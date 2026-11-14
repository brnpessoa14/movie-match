"""Execute `python -m app.enrich` no backend. Não expõe chaves na API."""
import os
import httpx
from sqlalchemy import select
from .database import Base, engine, SessionLocal
from .catalog import seed
from .models import CastMember, Movie


def apply_omdb(movie, data):
    if data.get("Response") != "True":
        return False
    # Preserva título e sinopse em português; OMDb retorna predominantemente inglês.
    poster = data.get("Poster", "")
    movie.poster_url = poster if poster.startswith("https://") else None
    rating = data.get("imdbRating", "N/A")
    movie.imdb_rating = float(rating) if rating != "N/A" else None
    rt = next((r["Value"] for r in data.get("Ratings", []) if r["Source"] == "Rotten Tomatoes"), None)
    movie.rotten_tomatoes_rating = int(rt.rstrip("%")) if rt and rt.endswith("%") else None
    runtime = data.get("Runtime", "").split(" ")[0]
    if runtime.isdigit():
        movie.runtime = int(runtime)
    actors = data.get("Actors", "N/A")
    if actors != "N/A":
        movie.cast = [CastMember(actor_name=a.strip(), cast_order=i) for i, a in enumerate(actors.split(","))]
    return True


def main():
    key = os.getenv("OMDB_API_KEY")
    if not key:
        raise SystemExit("Configure OMDB_API_KEY em backend/.env. O catálogo local continua funcionando sem chave.")
    Base.metadata.create_all(engine)
    with SessionLocal() as db, httpx.Client(timeout=15) as client:
        seed(db)
        for movie in db.scalars(select(Movie).order_by(Movie.id)).all():
            try:
                response = client.get("https://www.omdbapi.com/", params={"apikey": key, "i": movie.imdb_id, "plot": "full"})
                response.raise_for_status()
                if apply_omdb(movie, response.json()):
                    db.commit()
                    print(f"Atualizado: {movie.title}")
                else:
                    print(f"Sem dados: {movie.title}. Verifique chave e cota OMDb.")
            except (httpx.HTTPError, ValueError, KeyError):
                db.rollback()
                # Não imprime a exceção HTTP: a URL pode conter a chave.
                print("Falha na consulta OMDb; dados locais preservados.")


if __name__ == "__main__":
    main()
