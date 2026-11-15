from datetime import timedelta
from sqlalchemy import select
from sqlalchemy.orm import Session
from app.models import Account, AuthSession, User, now
from app.auth import hash_password, verify_password
from test_api import api

PAYLOAD = {"name": "Ana", "username": "ana.filmes", "password": "CinemaSenha123!"}


def test_register_login_logout_and_validation(api):
    c, engine = api
    created = c.post("/auth/register", json=PAYLOAD)
    assert created.status_code == 201
    auth = created.json()
    assert auth["preferences_ready"] is False
    assert "password" not in str(auth)
    assert c.post("/auth/register", json=PAYLOAD).status_code == 409
    assert c.post("/auth/login", json={**PAYLOAD, "password": "wrongpassword"}).status_code == 401
    assert c.post("/auth/login", json={"username": "nonexistent", "password": "wrongpassword"}).status_code == 401
    logged = c.post("/auth/login", json={"username": " ANA.FILMES ", "password": PAYLOAD["password"]})
    assert logged.status_code == 200
    assert logged.json()["user"]["id"] == auth["user"]["id"]
    c.headers["Authorization"] = "Bearer " + logged.json()["access_token"]
    assert c.get(f"/users/{auth['user']['id']}").status_code == 200
    assert c.post("/auth/logout").status_code == 204
    assert c.get(f"/users/{auth['user']['id']}").status_code == 401
    with Session(engine) as db:
        account = db.scalar(select(Account))
        assert account.password_hash != PAYLOAD["password"]
        assert verify_password(PAYLOAD["password"], account.password_hash)
        assert db.scalar(select(AuthSession)).token_hash != auth["access_token"]


def test_requires_identity_and_isolates_accounts(api):
    c, _ = api
    first = c.post("/auth/register", json=PAYLOAD).json()
    uid = first["user"]["id"]
    assert c.get(f"/users/{uid}/evaluations").status_code == 401
    assert c.post("/users", json={"name": "Bypass"}).status_code in (404, 405)
    second = c.post("/auth/register", json={**PAYLOAD, "username": "segunda"}).json()
    c.headers["Authorization"] = "Bearer " + second["access_token"]
    for path in [f"/users/{uid}", f"/users/{uid}/preferences", f"/users/{uid}/evaluations", f"/users/{uid}/recommendations", f"/movies/discovery?user_id={uid}"]:
        assert c.get(path).status_code == 403
    assert c.put(f"/users/{uid}/preferences", json={"genre_ids": [1]}).status_code == 403
    assert c.post("/evaluations", json={"user_id": uid, "movie_id": 1, "status": "GOSTEI"}).status_code == 403
    assert c.get(f"/users/{second['user']['id']}/evaluations").json() == []


def test_expiration_preferences_and_legacy_data_preserved(api):
    c, engine = api
    with Session(engine) as db:
        db.add(User(name="Perfil antigo sem senha"))
        db.commit()
    result = c.post("/auth/register", json=PAYLOAD).json()
    uid = result["user"]["id"]
    c.headers["Authorization"] = "Bearer " + result["access_token"]
    c.put(f"/users/{uid}/preferences", json={"genre_ids": []}).raise_for_status()
    logged = c.post("/auth/login", json=PAYLOAD).json()
    assert logged["preferences_ready"] is True
    with Session(engine) as db:
        assert len(db.scalars(select(User)).all()) == 2
        for session in db.scalars(select(AuthSession)):
            session.expires_at = now() - timedelta(seconds=1)
        db.commit()
    assert c.get(f"/users/{uid}").status_code == 401
    assert c.post("/auth/register", json={**PAYLOAD, "username": "bad user"}).status_code == 422
    assert c.post("/auth/register", json={**PAYLOAD, "password": "123"}).status_code == 422


def test_password_salts_and_invalid_hash():
    one, two = hash_password("same password"), hash_password("same password")
    assert one != two
    assert verify_password("same password", one)
    assert not verify_password("wrong password", one)
    assert not verify_password("same password", "invalid")
