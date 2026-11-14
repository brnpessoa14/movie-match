import os
from pathlib import Path

from dotenv import load_dotenv
from sqlalchemy import create_engine, event
from sqlalchemy.orm import DeclarativeBase, sessionmaker

load_dotenv(Path(__file__).resolve().parents[1] / ".env")


class Base(DeclarativeBase):
    pass


def make_engine(url: str):
    engine = create_engine(url, connect_args={"check_same_thread": False} if url.startswith("sqlite") else {})
    if url.startswith("sqlite"):
        @event.listens_for(engine, "connect")
        def enable_constraints(connection, _):
            connection.execute("PRAGMA foreign_keys=ON")
    return engine


engine = make_engine(os.getenv("DATABASE_URL", "sqlite:///./cinematch.db"))
SessionLocal = sessionmaker(bind=engine, expire_on_commit=False)


def get_db():
    with SessionLocal() as session:
        yield session
