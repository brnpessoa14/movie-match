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


@asynccontextmanager
async def lifespan(app):
    Base.metadata.create_all(engine)
    with SessionLocal() as db:
        seed(db)
    yield


app = FastAPI(title="CineMatch API", version="1.0.0", lifespan=lifespan,
    description="API acadêmica local: perfis sem senha, catálogo, avaliações e recomendações explicáveis. Não expor publicamente sem autenticação.")
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


@app.post("/users", response_model=s.UserOut, status_code=201, tags=["Usuários"])
def create_user(body: s.UserInput, db: DB):
    user = m.User(name=body.name)
    db.add(user)
    db.commit()
    return user


@app.get("/users/{user_id}", response_model=s.UserOut, tags=["Usuários"])
def get_user(user_id: int, db: DB):
    return require(db, m.User, user_id)


@app.patch("/users/{user_id}", response_model=s.UserOut, tags=["Usuários"])
def update_user(user_id: int, body: s.UserInput, db: DB):
    user = require(db, m.User, user_id)
    user.name = body.name
    db.commit()
    return user


@app.get("/genres", response_model=list[s.GenreOut], tags=["Catálogo"])
def genres(db: DB):
    return db.scalars(select(m.Genre).order_by(m.Genre.id)).all()


@app.get("/users/{user_id}/preferences", response_model=list[s.GenreOut], tags=["Preferências"])
def preferences(user_id: int, db: DB):
    return require(db, m.User, user_id).preferences


@app.put("/users/{user_id}/preferences", response_model=list[s.GenreOut], tags=["Preferências"])
def save_preferences(user_id: int, body: s.PreferencesInput, db: DB):
    user = require(db, m.User, user_id)
    ids = set(body.genre_ids)
    selected = db.scalars(select(m.Genre).where(m.Genre.id.in_(ids)).order_by(m.Genre.id)).all()
    if len(selected) != len(ids):
        raise HTTPException(422, "Um ou mais gêneros não existem.")
    user.preferences = selected
    db.commit()
    return selected


@app.get("/movies", response_model=list[s.MovieOut], tags=["Catálogo"])
def movies(db: DB, offset: int = Query(0, ge=0), limit: int = Query(50, ge=1, le=100)):
    return db.scalars(select(m.Movie).order_by(m.Movie.id).offset(offset).limit(limit)).all()


# Rota estática antes de /movies/{movie_id}.
@app.get("/movies/discovery", response_model=list[s.MovieOut], tags=["Catálogo"])
def discovery(user_id: int, db: DB, limit: int = Query(20, ge=1, le=100)):
    require(db, m.User, user_id)
    evaluated = select(m.Evaluation.movie_id).where(m.Evaluation.user_id == user_id)
    return db.scalars(select(m.Movie).where(m.Movie.id.not_in(evaluated)).order_by(m.Movie.id).limit(limit)).all()


@app.get("/movies/{movie_id}", response_model=s.MovieOut, tags=["Catálogo"])
def movie_detail(movie_id: int, db: DB):
    return require(db, m.Movie, movie_id)


@app.post("/evaluations", response_model=s.EvaluationOut, status_code=201, tags=["Avaliações"])
def evaluate(body: s.EvaluationInput, db: DB):
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
def history(user_id: int, db: DB, status: s.Status | None = None):
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
def update_evaluation(evaluation_id: int, user_id: int, body: s.EvaluationPatch, db: DB):
    evaluation = owned_evaluation(db, evaluation_id, user_id)
    evaluation.status = body.status.value
    db.commit()
    return evaluation


@app.delete("/evaluations/{evaluation_id}", status_code=204, tags=["Avaliações"])
def delete_evaluation(evaluation_id: int, user_id: int, db: DB):
    db.delete(owned_evaluation(db, evaluation_id, user_id))
    db.commit()
    return Response(status_code=204)


@app.get("/users/{user_id}/watchlist", response_model=list[s.EvaluationOut], tags=["Avaliações"])
def watchlist(user_id: int, db: DB):
    return history(user_id, db, s.Status.QUERO_ASSISTIR)


@app.get("/users/{user_id}/recommendations", response_model=list[s.RecommendationOut], tags=["Recomendações"])
def recommendations(user_id: int, db: DB, limit: int = Query(20, ge=1, le=100)):
    return recommend(db, require(db, m.User, user_id), limit)
