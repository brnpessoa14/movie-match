from datetime import datetime
from enum import Enum
from pydantic import BaseModel, ConfigDict, Field, field_validator


class Status(str, Enum):
    GOSTEI = "GOSTEI"
    QUERO_ASSISTIR = "QUERO_ASSISTIR"
    NAO_GOSTEI = "NAO_GOSTEI"
    SEM_INTERESSE = "SEM_INTERESSE"


class OrmSchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)


class UserInput(BaseModel):
    name: str = Field(min_length=1, max_length=80, examples=["Bruno"])

    @field_validator("name")
    @classmethod
    def clean_name(cls, value):
        value = value.strip()
        if not value:
            raise ValueError("Informe seu nome.")
        return value


class UserOut(OrmSchema):
    id: int
    name: str
    created_at: datetime


class GenreOut(OrmSchema):
    id: int
    name: str


class PreferencesInput(BaseModel):
    genre_ids: list[int] = Field(max_length=50)


class CastOut(OrmSchema):
    actor_name: str
    cast_order: int


class MovieOut(OrmSchema):
    id: int
    imdb_id: str | None
    title: str
    year: int
    runtime: int | None
    plot: str
    poster_url: str | None
    imdb_rating: float | None
    rotten_tomatoes_rating: int | None
    genres: list[GenreOut]
    cast: list[CastOut]


class EvaluationInput(BaseModel):
    user_id: int = Field(gt=0)
    movie_id: int = Field(gt=0)
    status: Status


class EvaluationPatch(BaseModel):
    status: Status


class EvaluationOut(OrmSchema):
    id: int
    user_id: int
    movie_id: int
    status: Status
    created_at: datetime
    updated_at: datetime
    movie: MovieOut


class RecommendationOut(BaseModel):
    movie: MovieOut
    score: float
    reason: str
