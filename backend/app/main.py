from contextlib import asynccontextmanager
from typing import Annotated

from fastapi import Depends, FastAPI, HTTPException, Query, Response
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from . import models as m, schemas as s
from .catalog import seed
from .database import Base, SessionLocal, engine, get_db
from .recommendations import recommend
from .auth import SessionAuth, authorize, DUMMY_HASH, hash_password, issue_session, verify_password


@asynccontextmanager
async def lifespan(app):
    Base.metadata.create_all(engine)
    with SessionLocal() as db:
        seed(db)
    yield


app = FastAPI(title="CineMatch API", version="2.0.0", lifespan=lifespan,
    description="Contas com nome de usuário e senha, sessões Bearer, catálogo e recomendações explicáveis.")
DB = Annotated[Session, Depends(get_db)]


def require(db, model, resource_id):
    item = db.get(model, resource_id)
    if item is None:
        raise HTTPException(404, "Registro não encontrado.")
    return item


@app.get("/health", tags=["Sistema"])
def health(db: DB):
    db.execute(select(1))
    return {"status": "ok"}


@app.post("/auth/register", response_model=s.AuthOut, status_code=201, tags=["Autenticação"])
def register(body: s.RegisterInput, db: DB):
    if db.scalar(select(m.Account.id).where(m.Account.username == body.username)):
        raise HTTPException(409, "Este nome de usuário já está em uso.")
    user = m.User(name=body.name)
    account = m.Account(user=user, username=body.username, password_hash=hash_password(body.password))
    db.add(account)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(409, "Este nome de usuário já está em uso.")
    return issue_session(db, account)


@app.post("/auth/login", response_model=s.AuthOut, tags=["Autenticação"])
def login(body: s.LoginInput, db: DB):
    account = db.scalar(select(m.Account).where(m.Account.username == body.username))
    valid = verify_password(body.password, account.password_hash if account else DUMMY_HASH)
    if not account or not valid:
        raise HTTPException(401, "Nome de usuário ou senha incorretos.")
    return issue_session(db, account)


@app.post("/auth/logout", status_code=204, tags=["Autenticação"])
def logout(session: SessionAuth, db: DB):
    db.delete(session)
    db.commit()
    return Response(status_code=204)


@app.get("/users/{user_id}", response_model=s.UserOut, tags=["Usuários"])
def get_user(user_id: int, db: DB, session: SessionAuth):
    authorize(session, user_id)
    return profile_result(require(db, m.User, user_id), session.account)


def profile_result(user, account):
    return {"id": user.id, "name": user.name, "created_at": user.created_at,
            "username": account.username}


@app.patch("/users/{user_id}", response_model=s.UserOut, tags=["Usuários"])
def update_user(user_id: int, body: s.ProfileInput, db: DB, session: SessionAuth):
    authorize(session, user_id)
    user = require(db, m.User, user_id)
    user.name = body.name
    if body.username is not None:
        session.account.username = body.username
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(409, "Este nome de usuário já está em uso.")
    return profile_result(user, session.account)


@app.get("/genres", response_model=list[s.GenreOut], tags=["Catálogo"])
def genres(db: DB):
    return db.scalars(select(m.Genre).order_by(m.Genre.id)).all()


@app.get("/users/{user_id}/preferences", response_model=list[s.GenreOut], tags=["Preferências"])
def preferences(user_id: int, db: DB, session: SessionAuth):
    authorize(session, user_id)
    return require(db, m.User, user_id).preferences


@app.put("/users/{user_id}/preferences", response_model=list[s.GenreOut], tags=["Preferências"])
def save_preferences(user_id: int, body: s.PreferencesInput, db: DB, session: SessionAuth):
    authorize(session, user_id)
    user = require(db, m.User, user_id)
    ids = set(body.genre_ids)
    selected = db.scalars(select(m.Genre).where(m.Genre.id.in_(ids)).order_by(m.Genre.id)).all()
    if len(selected) != len(ids):
        raise HTTPException(422, "Um ou mais gêneros não existem.")
    user.preferences = selected
    session.account.preferences_ready = True
    db.commit()
    return selected


@app.get("/movies", response_model=list[s.MovieOut], tags=["Catálogo"])
def movies(db: DB, offset: int = Query(0, ge=0), limit: int = Query(50, ge=1, le=100)):
    return db.scalars(select(m.Movie).order_by(m.Movie.id).offset(offset).limit(limit)).all()


# Rota estática antes de /movies/{movie_id}.
@app.get("/movies/discovery", response_model=list[s.MovieOut], tags=["Catálogo"])
def discovery(user_id: int, db: DB, session: SessionAuth, limit: int = Query(20, ge=1, le=100)):
    authorize(session, user_id)
    require(db, m.User, user_id)
    evaluated = select(m.Evaluation.movie_id).where(m.Evaluation.user_id == user_id)
    return db.scalars(select(m.Movie).where(m.Movie.id.not_in(evaluated)).order_by(m.Movie.id).limit(limit)).all()


@app.get("/movies/{movie_id}", response_model=s.MovieOut, tags=["Catálogo"])
def movie_detail(movie_id: int, db: DB):
    return require(db, m.Movie, movie_id)


@app.post("/evaluations", response_model=s.EvaluationOut, status_code=201, tags=["Avaliações"])
def evaluate(body: s.EvaluationInput, db: DB, session: SessionAuth):
    authorize(session, body.user_id)
    require(db, m.User, body.user_id)
    require(db, m.Movie, body.movie_id)
    evaluation = m.Evaluation(user_id=body.user_id, movie_id=body.movie_id, status=body.status.value)
    db.add(evaluation)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(409, "Filme já avaliado. Altere a avaliação no histórico.")
    return evaluation


@app.get("/users/{user_id}/evaluations", response_model=list[s.EvaluationOut], tags=["Avaliações"])
def history(user_id: int, db: DB, session: SessionAuth, status: s.Status | None = None):
    authorize(session, user_id)
    require(db, m.User, user_id)
    query = select(m.Evaluation).where(m.Evaluation.user_id == user_id)
    if status:
        query = query.where(m.Evaluation.status == status.value)
    return db.scalars(query.order_by(m.Evaluation.updated_at.desc(), m.Evaluation.id.desc())).all()


def owned_evaluation(db, evaluation_id, user_id):
    evaluation = require(db, m.Evaluation, evaluation_id)
    if evaluation.user_id != user_id:
        raise HTTPException(404, "Avaliação não encontrada neste perfil.")
    return evaluation


@app.patch("/evaluations/{evaluation_id}", response_model=s.EvaluationOut, tags=["Avaliações"])
def update_evaluation(evaluation_id: int, user_id: int, body: s.EvaluationPatch, db: DB, session: SessionAuth):
    authorize(session, user_id)
    evaluation = owned_evaluation(db, evaluation_id, user_id)
    evaluation.status = body.status.value
    db.commit()
    return evaluation


@app.delete("/evaluations/{evaluation_id}", status_code=204, tags=["Avaliações"])
def delete_evaluation(evaluation_id: int, user_id: int, db: DB, session: SessionAuth):
    authorize(session, user_id)
    db.delete(owned_evaluation(db, evaluation_id, user_id))
    db.commit()
    return Response(status_code=204)


@app.get("/users/{user_id}/watchlist", response_model=list[s.EvaluationOut], tags=["Avaliações"])
def watchlist(user_id: int, db: DB, session: SessionAuth):
    return history(user_id, db, session, s.Status.QUERO_ASSISTIR)


@app.get("/users/{user_id}/recommendations", response_model=list[s.RecommendationOut], tags=["Recomendações"])
def recommendations(user_id: int, db: DB, session: SessionAuth, limit: int = Query(20, ge=1, le=100)):
    authorize(session, user_id)
    return recommend(db, require(db, m.User, user_id), limit)
