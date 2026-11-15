import pytest
from uuid import uuid4
from fastapi.testclient import TestClient
from sqlalchemy import select
from sqlalchemy.orm import sessionmaker
from app import main
from app.catalog import seed
from app.database import Base, get_db, make_engine
from app.models import Evaluation, Movie
from app.enrich import apply_omdb


@pytest.fixture
def api(tmp_path, monkeypatch):
    engine = make_engine(f"sqlite:///{tmp_path / 'test.db'}")
    factory = sessionmaker(bind=engine, expire_on_commit=False)
    monkeypatch.setattr(main, "engine", engine)
    monkeypatch.setattr(main, "SessionLocal", factory)
    def override():
        with factory() as db:
            yield db
    main.app.dependency_overrides[get_db] = override
    with TestClient(main.app) as client:
        yield client, engine
    main.app.dependency_overrides.clear()
    engine.dispose()


def new_user(client):
    response = client.post("/auth/register", json={"name": "  Bruno  ", "username": "user_" + uuid4().hex[:10], "password": "TesteSenha123!"})
    assert response.status_code == 201
    assert response.json()["user"]["name"] == "Bruno"
    client.headers["Authorization"] = "Bearer " + response.json()["access_token"]
    return response.json()["user"]["id"]


def test_profile_catalog_and_preferences(api):
    c, _ = api
    uid = new_user(c)
    assert c.get("/health").json() == {"status": "ok"}
    assert c.get(f"/users/{uid}").json()["id"] == uid
    assert c.patch(f"/users/{uid}", json={"name": "Ana"}).json()["name"] == "Ana"
    genres = c.get("/genres").json()
    assert len(genres) == 11
    ids = [g["id"] for g in genres[:2]]
    assert len(c.put(f"/users/{uid}/preferences", json={"genre_ids": ids + ids}).json()) == 2
    assert [g["id"] for g in c.get(f"/users/{uid}/preferences").json()] == ids
    movies = c.get("/movies").json()
    assert len(movies) >= 20
    assert movies[0]["imdb_rating"] is None
    assert c.get(f"/movies/{movies[0]['id']}").json() == movies[0]
    assert len(c.get("/movies", params={"offset": 2, "limit": 3}).json()) == 3


@pytest.mark.parametrize("status", ["GOSTEI", "QUERO_ASSISTIR", "NAO_GOSTEI", "SEM_INTERESSE"])
def test_evaluation_lifecycle(api, status):
    c, _ = api
    uid = new_user(c)
    payload = {"user_id": uid, "movie_id": 1, "status": status}
    result = c.post("/evaluations", json=payload)
    assert result.status_code == 201
    eid = result.json()["id"]
    assert c.post("/evaluations", json=payload).status_code == 409
    assert c.get(f"/users/{uid}/evaluations").json()[0]["status"] == status
    assert all(m["id"] != 1 for m in c.get("/movies/discovery", params={"user_id": uid}).json())
    assert all(r["movie"]["id"] != 1 for r in c.get(f"/users/{uid}/recommendations").json())
    changed = c.patch(f"/evaluations/{eid}", params={"user_id": uid}, json={"status": "QUERO_ASSISTIR"})
    assert changed.status_code == 200
    assert len(c.get(f"/users/{uid}/watchlist").json()) == 1
    assert c.delete(f"/evaluations/{eid}", params={"user_id": uid}).status_code == 204
    assert c.get(f"/users/{uid}/evaluations").json() == []
    assert c.get("/movies/discovery", params={"user_id": uid}).json()[0]["id"] == 1


def test_recommendation_scores_recompute(api):
    c, _ = api
    uid = new_user(c)
    scifi = next(g["id"] for g in c.get("/genres").json() if g["name"] == "Ficção científica")
    c.put(f"/users/{uid}/preferences", json={"genre_ids": [scifi]})
    def scores():
        return {r["movie"]["id"]: r["score"] for r in c.get(f"/users/{uid}/recommendations").json()}
    before = scores()
    assert before[1] == 1.5  # +3 / dois gêneros
    eid = c.post("/evaluations", json={"user_id": uid, "movie_id": 3, "status": "GOSTEI"}).json()["id"]
    assert scores()[1] > before[1]
    c.patch(f"/evaluations/{eid}", params={"user_id": uid}, json={"status": "NAO_GOSTEI"})
    assert scores()[1] < before[1]
    c.delete(f"/evaluations/{eid}", params={"user_id": uid})
    assert scores() == before
    assert c.get(f"/users/{uid}/recommendations").json() == c.get(f"/users/{uid}/recommendations").json()


def test_validation_and_ownership(api):
    c, _ = api
    uid = new_user(c)
    for name in [" ", "x" * 81]:
        assert c.post("/auth/register", json={"name": name, "username": "another", "password": "TesteSenha123!"}).status_code == 422
    assert c.get("/users/99999").status_code == 403
    assert c.get("/movies/99999").status_code == 404
    assert c.put(f"/users/{uid}/preferences", json={"genre_ids": [999]}).status_code == 422
    assert c.post("/evaluations", json={"user_id": uid, "movie_id": 9999, "status": "GOSTEI"}).status_code == 404
    assert c.post("/evaluations", json={"user_id": uid, "movie_id": 1, "status": "BAD"}).status_code == 422
    eid = c.post("/evaluations", json={"user_id": uid, "movie_id": 1, "status": "GOSTEI"}).json()["id"]
    other = new_user(c)
    assert c.delete(f"/evaluations/{eid}", params={"user_id": other}).status_code == 404
    assert c.get("/movies", params={"limit": 0}).status_code == 422


def test_persistence_after_restart_and_seed(api):
    c, engine = api
    uid = new_user(c)
    c.post("/evaluations", json={"user_id": uid, "movie_id": 1, "status": "GOSTEI"})
    # Novo pool e nova sessão; dados vêm do arquivo, não de objetos em memória.
    url = str(engine.url)
    fresh = make_engine(url)
    with sessionmaker(bind=fresh)() as db:
        seed(db)
        assert len(db.scalars(select(Movie)).all()) == 20
        assert db.scalars(select(Evaluation).where(Evaluation.user_id == uid)).one().status == "GOSTEI"
    fresh.dispose()
    with TestClient(main.app) as restarted:
        restarted.headers["Authorization"] = c.headers["Authorization"]
        assert len(restarted.get(f"/users/{uid}/evaluations").json()) == 1


def test_empty_catalog_and_openapi(api):
    c, _ = api
    uid = new_user(c)
    for movie in c.get("/movies").json():
        c.post("/evaluations", json={"user_id": uid, "movie_id": movie["id"], "status": "SEM_INTERESSE"})
    assert c.get(f"/users/{uid}/recommendations").json() == []
    assert c.get("/movies/discovery", params={"user_id": uid}).json() == []
    assert c.get("/docs").status_code == 200
    assert "/evaluations/{evaluation_id}" in c.get("/openapi.json").json()["paths"]


def test_omdb_missing_ratings_and_offline():
    movie = Movie(title="Teste", year=2000, plot="Teste")
    assert not apply_omdb(movie, {"Response": "False"})
    assert apply_omdb(movie, {"Response": "True", "Poster": "N/A", "imdbRating": "N/A"})
    assert movie.imdb_rating is None and movie.poster_url is None
    assert movie.rotten_tomatoes_rating is None
    apply_omdb(movie, {"Response": "True", "imdbRating": "8.1", "Ratings": [{"Source": "Rotten Tomatoes", "Value": "92%"}]})
    assert movie.imdb_rating == 8.1 and movie.rotten_tomatoes_rating == 92
