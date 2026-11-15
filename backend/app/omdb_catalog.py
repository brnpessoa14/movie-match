"""Busca real na OMDb, importação idempotente e cache persistente. Nunca expõe a chave."""
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone, timedelta
import json
import os
import re
import threading

import httpx
from sqlalchemy import select, update
from sqlalchemy.dialects.sqlite import insert

from .models import CatalogSearch, Genre, Movie, ProviderUsage
from .enrich import apply_omdb

_quota_lock = threading.Lock()
GENRE_NAMES = {
    'Action': 'Ação', 'Adventure': 'Aventura', 'Comedy': 'Comédia', 'Drama': 'Drama',
    'Romance': 'Romance', 'Horror': 'Terror', 'Thriller': 'Suspense', 'Sci-Fi': 'Ficção científica',
    'Fantasy': 'Fantasia', 'Animation': 'Animação', 'Documentary': 'Documentário',
    'Crime': 'Crime', 'Mystery': 'Mistério', 'Biography': 'Biografia', 'History': 'História',
    'War': 'Guerra', 'Family': 'Família', 'Music': 'Música', 'Musical': 'Musical',
    'Sport': 'Esporte', 'Western': 'Faroeste', 'Film-Noir': 'Cinema noir', 'Short': 'Curta-metragem',
}


class CatalogUnavailable(Exception):
    pass


def fetch_many(db, parameters, fetch=None):
    """Reserva a cota antes dos pedidos paralelos; sessões SQL não cruzam threads."""
    if not parameters:
        return []
    key = os.getenv('OMDB_API_KEY')
    if not key and fetch is None:
        raise CatalogUnavailable('A busca OMDb ainda não foi configurada no servidor.')
    with _quota_lock:
        day = datetime.now(timezone.utc).date().isoformat()
        db.execute(insert(ProviderUsage).values(day=day, requests=0).on_conflict_do_nothing())
        reserved = db.execute(update(ProviderUsage).where(ProviderUsage.day == day, ProviderUsage.requests + len(parameters) <= 900).values(requests=ProviderUsage.requests + len(parameters)))
        if reserved.rowcount != 1:
            db.rollback()
            raise CatalogUnavailable('A cota de consultas de hoje foi atingida. O catálogo salvo continua disponível.')
        db.commit()
    def request(params):
        try:
            if fetch is not None:
                data = fetch(params)
            else:
                response = httpx.get('https://www.omdbapi.com/', params={'apikey': key, **params}, timeout=12)
                response.raise_for_status()
                data = response.json()
            if data.get('Response') == 'False':
                error = str(data.get('Error', '')).lower()
                if 'limit' in error or 'key' in error:
                    raise CatalogUnavailable('A OMDb recusou a consulta. Verifique a chave e a cota no servidor; seus dados foram preservados.')
            return data
        except (httpx.HTTPError, ValueError, TypeError):
            raise CatalogUnavailable('Não foi possível consultar a OMDb agora. Tente novamente.') from None
    with ThreadPoolExecutor(max_workers=4) as pool:
        return list(pool.map(request, parameters))


def import_movie(db, data):
    if data.get('Response') != 'True' or data.get('Type') != 'movie':
        return None
    imdb = data.get('imdbID', '')
    title = str(data.get('Title', '')).strip()
    year = re.search(r'\d{4}', str(data.get('Year', '')))
    if not re.fullmatch(r'tt\d{7,9}', imdb) or not title or not year:
        return None
    existing = db.scalar(select(Movie).where(Movie.imdb_id == imdb))
    if existing:
        return existing
    genres = []
    for raw in data.get('Genre', '').split(','):
        if not raw.strip() or raw.strip() == 'N/A':
            continue
        name = GENRE_NAMES.get(raw.strip(), raw.strip())[:60]
        genre = db.scalar(select(Genre).where(Genre.name == name))
        if genre is None:
            genre = Genre(name=name)
            db.add(genre)
            db.flush()
        genres.append(genre)
    plot = data.get('Plot', '')
    movie = Movie(imdb_id=imdb, title=title[:180], year=int(year.group()),
                  plot=plot if plot and plot != 'N/A' else 'Sinopse não disponibilizada pela OMDb.', genres=genres)
    try:
        apply_omdb(movie, data)
    except (ValueError, KeyError, TypeError):
        return None
    db.add(movie)
    db.flush()
    return movie


def search_catalog(db, query, page=1, fetch=None):
    key = f'{query.strip().casefold()}:{page}'
    cached = db.get(CatalogSearch, key)
    if cached and cached.created_at.replace(tzinfo=timezone.utc) > datetime.now(timezone.utc) - timedelta(days=7):
        ids = json.loads(cached.movie_ids)
        movies = {m.id: m for m in db.scalars(select(Movie).where(Movie.id.in_(ids))).all()}
        return {'movies': [movies[i] for i in ids if i in movies], 'total_results': cached.total_results, 'page': page, 'cached': True}
    data = fetch_many(db, [{'s': query.strip(), 'type': 'movie', 'page': page}], fetch)[0]
    hits = data.get('Search', [])
    imdbs = list(dict.fromkeys(hit.get('imdbID') for hit in hits if re.fullmatch(r'tt\d{7,9}', str(hit.get('imdbID', '')))))
    existing = {m.imdb_id: m for m in db.scalars(select(Movie).where(Movie.imdb_id.in_(imdbs))).all()}
    missing = [imdb for imdb in imdbs if imdb not in existing]
    details = fetch_many(db, [{'i': imdb, 'type': 'movie', 'plot': 'full'} for imdb in missing], fetch)
    for item in details:
        movie = import_movie(db, item)
        if movie:
            existing[movie.imdb_id] = movie
    movies = [existing[imdb] for imdb in imdbs if imdb in existing]
    db.merge(CatalogSearch(key=key, movie_ids=json.dumps([m.id for m in movies]),
                          total_results=int(data.get('totalResults', 0)), created_at=datetime.now(timezone.utc)))
    db.commit()
    return {'movies': movies, 'total_results': int(data.get('totalResults', 0)), 'page': page, 'cached': False}
