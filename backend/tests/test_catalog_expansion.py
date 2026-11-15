import httpx
from sqlalchemy import select, func
from sqlalchemy.orm import Session
from app import omdb_catalog
from app.models import Movie
from test_api import api, new_user

DETAIL = {'Response':'True','Type':'movie','imdbID':'tt9999001','Title':'Filme de teste',
          'Year':'2022','Plot':'Synopsis returned by the provider.','Genre':'Action, Sci-Fi',
          'Runtime':'110 min','Actors':'Test Actor','Poster':'https://example.com/poster.jpg',
          'imdbRating':'7.2','Ratings':[{'Source':'Rotten Tomatoes','Value':'80%'}]}


def test_search_imports_once_caches_across_sessions_and_preserves_existing(api, monkeypatch):
    c, engine = api
    new_user(c)
    calls=[]
    def request(url, params, timeout):
        calls.append(params)
        data = {'Response':'True','Search':[{'imdbID':'tt0133093'},{'imdbID':'tt9999001'}],'totalResults':'2'} if 's' in params else DETAIL
        return httpx.Response(200,json=data,request=httpx.Request('GET','https://www.omdbapi.com/'))
    monkeypatch.setattr(omdb_catalog.httpx,'get',request)
    original=c.get('/movies/1').json()
    result=c.get('/movies/search',params={'q':'test','page':1})
    assert result.status_code==200
    assert result.json()['total_results']==2 and len(result.json()['movies'])==2
    assert len(calls)==2
    again=c.get('/movies/search',params={'q':' TEST ','page':1}).json()
    assert again['cached'] is True and len(calls)==2
    assert c.get('/movies/1').json()==original
    with Session(engine) as db:
        assert db.scalar(select(func.count()).select_from(Movie))==21
    assert c.get('/movies/search',params={'q':'x'}).status_code==422
    assert c.get('/movies/search',params={'q':'test','page':101}).status_code==422
    c.headers.pop('Authorization')
    assert c.get('/movies/search',params={'q':'test'}).status_code==401


def test_provider_failure_is_real_error_and_does_not_remove_catalog(api, monkeypatch):
    c,_=api
    new_user(c)
    monkeypatch.setattr(omdb_catalog.httpx,'get',lambda *args,**kwargs: httpx.Response(200,json={'Response':'False','Error':'Request limit reached!'},request=httpx.Request('GET','https://www.omdbapi.com/')))
    result=c.get('/movies/search',params={'q':'anything'})
    assert result.status_code==503
    assert 'apikey' not in result.text.lower() and 'http' not in result.text.lower()
    assert len(c.get('/movies').json())==20


def test_discovery_reorders_from_preferences_and_evaluations_and_counts_total(api):
    c,_=api
    uid=new_user(c)
    assert c.get('/movies/discovery',params={'user_id':uid}).json()[0]['id']==1
    animation=next(g['id'] for g in c.get('/genres').json() if g['name']=='Animação')
    c.put(f'/users/{uid}/preferences',json={'genre_ids':[animation]})
    first=c.get('/movies/discovery',params={'user_id':uid})
    assert first.json()[0]['id']==6
    assert 'Animação' in first.json()[0]['discovery_reason']
    c.post('/evaluations',json={'user_id':uid,'movie_id':6,'status':'GOSTEI'}).raise_for_status()
    second=c.get('/movies/discovery',params={'user_id':uid})
    assert second.json()[0]['id']==7
    assert second.headers['X-Catalog-Total']=='20' and second.headers['X-Remaining-Count']=='19'
    c.post('/evaluations',json={'user_id':uid,'movie_id':7,'status':'NAO_GOSTEI'}).raise_for_status()
    assert all(m['id'] not in {6,7} for m in c.get('/movies/discovery',params={'user_id':uid}).json())


def test_import_reopens_discovery_for_account_that_exhausted_original_catalog(api):
    c,engine=api
    uid=new_user(c)
    for movie in c.get('/movies').json():
        c.post('/evaluations',json={'user_id':uid,'movie_id':movie['id'],'status':'SEM_INTERESSE'}).raise_for_status()
    assert c.get('/movies/discovery',params={'user_id':uid}).json()==[]
    with Session(engine) as db:
        omdb_catalog.import_movie(db,DETAIL)
        db.commit()
    result=c.get('/movies/discovery',params={'user_id':uid})
    assert len(result.json())==1 and result.json()[0]['imdb_id']=='tt9999001'
    assert result.headers['X-Remaining-Count']=='1'
    assert len(c.get(f'/users/{uid}/evaluations').json())==20
