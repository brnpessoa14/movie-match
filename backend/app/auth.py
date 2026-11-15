"""Contas locais com PBKDF2 e sessões opacas revogáveis, sem senhas em texto."""
from datetime import timedelta, timezone
import hashlib
import hmac
import secrets
from typing import Annotated

from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from .database import get_db
from .models import Account, AuthSession, now

ITERATIONS = 600_000
bearer = HTTPBearer(auto_error=False)


def hash_password(password):
    salt = secrets.token_hex(16)
    digest = hashlib.pbkdf2_hmac("sha256", password.encode(), bytes.fromhex(salt), ITERATIONS).hex()
    return f"pbkdf2_sha256${ITERATIONS}${salt}${digest}"


def verify_password(password, encoded):
    try:
        algorithm, rounds, salt, expected = encoded.split("$")
        if algorithm != "pbkdf2_sha256":
            return False
        actual = hashlib.pbkdf2_hmac("sha256", password.encode(), bytes.fromhex(salt), int(rounds)).hex()
        return hmac.compare_digest(actual, expected)
    except (ValueError, TypeError):
        return False


DUMMY_HASH = hash_password("CineMatch-login-placeholder")


def issue_session(db, account):
    db.execute(delete(AuthSession).where(AuthSession.expires_at < now()))
    token = secrets.token_urlsafe(32)
    db.add(AuthSession(account_id=account.id, token_hash=hashlib.sha256(token.encode()).hexdigest(),
                       expires_at=now() + timedelta(hours=8)))
    db.commit()
    return {"access_token": token, "user": account.user, "username": account.username,
            "preferences_ready": account.preferences_ready}


def current_session(db: Annotated[Session, Depends(get_db)],
                    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(bearer)]):
    if credentials is None:
        raise HTTPException(401, "Entre na sua conta para continuar.", headers={"WWW-Authenticate": "Bearer"})
    digest = hashlib.sha256(credentials.credentials.encode()).hexdigest()
    session = db.scalar(select(AuthSession).where(AuthSession.token_hash == digest))
    if session is None or session.expires_at.replace(tzinfo=timezone.utc) <= now():
        raise HTTPException(401, "Sua sessão expirou. Entre novamente.", headers={"WWW-Authenticate": "Bearer"})
    return session


SessionAuth = Annotated[AuthSession, Depends(current_session)]


def authorize(session, user_id):
    if session.account.user_id != user_id:
        raise HTTPException(403, "Você só pode acessar os dados da sua conta.")
