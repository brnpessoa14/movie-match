from collections import defaultdict
from sqlalchemy import select
from .models import Evaluation, Movie

WEIGHTS = {"GOSTEI": 5, "QUERO_ASSISTIR": 2, "NAO_GOSTEI": -4, "SEM_INTERESSE": -1}


def recommend(db, user, limit):
    weights = defaultdict(int)
    for genre in user.preferences:
        weights[genre.id] += 3
    evaluations = db.scalars(select(Evaluation).where(Evaluation.user_id == user.id)).all()
    evaluated = {e.movie_id for e in evaluations}
    for evaluation in evaluations:
        for genre in evaluation.movie.genres:
            weights[genre.id] += WEIGHTS[evaluation.status]
    results = []
    for movie in db.scalars(select(Movie)).all():
        if movie.id in evaluated:
            continue
        score = sum(weights[g.id] for g in movie.genres) / max(len(movie.genres), 1)
        positive = [g.name for g in movie.genres if weights[g.id] > 0]
        if positive:
            reason = "Combina com o seu gosto por " + ", ".join(positive) + "."
        elif not user.preferences and not evaluations:
            reason = "Um primeiro encontro com o catálogo. Suas escolhas deixam as próximas sugestões mais pessoais."
        else:
            reason = "Um convite para explorar algo diferente dos seus gêneros favoritos."
        results.append({"movie": movie, "score": round(score, 4), "reason": reason})
    # Média evita favorecer filmes só por terem mais gêneros; ID desempata.
    return sorted(results, key=lambda r: (-r["score"], r["movie"].id))[:limit]
