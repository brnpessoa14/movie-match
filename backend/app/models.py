from datetime import datetime, timezone
from sqlalchemy import CheckConstraint, Column, DateTime, Float, ForeignKey, Integer, String, Table, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship
from .database import Base


def now():
    return datetime.now(timezone.utc)


movie_genres = Table("movie_genres", Base.metadata,
    Column("movie_id", ForeignKey("movies.id", ondelete="CASCADE"), primary_key=True),
    Column("genre_id", ForeignKey("genres.id"), primary_key=True))
user_preferences = Table("user_preferences", Base.metadata,
    Column("user_id", ForeignKey("users.id", ondelete="CASCADE"), primary_key=True),
    Column("genre_id", ForeignKey("genres.id"), primary_key=True))


class User(Base):
    __tablename__ = "users"
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(80))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=now)
    preferences: Mapped[list["Genre"]] = relationship(secondary=user_preferences, order_by="Genre.id", lazy="selectin")


class Genre(Base):
    __tablename__ = "genres"
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(60), unique=True)


class Movie(Base):
    __tablename__ = "movies"
    id: Mapped[int] = mapped_column(primary_key=True)
    imdb_id: Mapped[str | None] = mapped_column(String(20), unique=True)
    title: Mapped[str] = mapped_column(String(180))
    year: Mapped[int] = mapped_column(Integer)
    runtime: Mapped[int | None] = mapped_column(Integer)
    plot: Mapped[str] = mapped_column(Text)
    poster_url: Mapped[str | None] = mapped_column(Text)
    imdb_rating: Mapped[float | None] = mapped_column(Float)
    rotten_tomatoes_rating: Mapped[int | None] = mapped_column(Integer)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=now)
    genres: Mapped[list[Genre]] = relationship(secondary=movie_genres, lazy="selectin", order_by="Genre.id")
    cast: Mapped[list["CastMember"]] = relationship(cascade="all, delete-orphan", lazy="selectin", order_by="CastMember.cast_order")


class CastMember(Base):
    __tablename__ = "movie_cast"
    movie_id: Mapped[int] = mapped_column(ForeignKey("movies.id", ondelete="CASCADE"), primary_key=True)
    cast_order: Mapped[int] = mapped_column(primary_key=True)
    actor_name: Mapped[str] = mapped_column(String(120))


class Evaluation(Base):
    __tablename__ = "evaluations"
    __table_args__ = (
        UniqueConstraint("user_id", "movie_id", name="uq_user_movie"),
        CheckConstraint("status IN ('GOSTEI','QUERO_ASSISTIR','NAO_GOSTEI','SEM_INTERESSE')", name="valid_status"),
    )
    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    movie_id: Mapped[int] = mapped_column(ForeignKey("movies.id"))
    status: Mapped[str] = mapped_column(String(20))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=now)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=now, onupdate=now)
    movie: Mapped[Movie] = relationship(lazy="selectin")
