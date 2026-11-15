"""Servidor HTTP real em porta local temporária, com reinício de processo."""
import os
from pathlib import Path
import socket
import subprocess
import sys
import time

import httpx


def test_live_http_server_survives_restart(tmp_path):
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        port = sock.getsockname()[1]
    url = f"http://127.0.0.1:{port}"
    environment = {**os.environ, "DATABASE_URL": f"sqlite:///{(tmp_path / 'restart.db').as_posix()}"}

    def start():
        process = subprocess.Popen(
            [sys.executable, "-m", "uvicorn", "app.main:app", "--host", "127.0.0.1", "--port", str(port)],
            cwd=Path(__file__).resolve().parents[1], env=environment,
            stdout=subprocess.DEVNULL, stderr=subprocess.PIPE,
            creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0,
        )
        for _ in range(100):
            try:
                if httpx.get(f"{url}/health", timeout=0.5).status_code == 200:
                    return process
            except httpx.HTTPError:
                pass
            if process.poll() is not None:
                raise AssertionError(process.stderr.read().decode(errors="replace"))
            time.sleep(0.1)
        process.kill()
        process.wait()
        raise AssertionError("O servidor não iniciou.")

    def stop(process):
        process.terminate()
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)
        process.stderr.close()

    process = start()
    try:
        with httpx.Client(base_url=url, timeout=5) as client:
            auth = client.post("/auth/register", json={"name": "Persistência HTTP", "username": "persistencia", "password": "TesteSenha123!"}).json()
            uid = auth["user"]["id"]
            token = auth["access_token"]
            client.headers["Authorization"] = "Bearer " + token
            client.put(f"/users/{uid}/preferences", json={"genre_ids": [1, 8]}).raise_for_status()
            client.post("/evaluations", json={"user_id": uid, "movie_id": 1, "status": "GOSTEI"}).raise_for_status()
            history = client.get(f"/users/{uid}/evaluations").json()
            recommendations = client.get(f"/users/{uid}/recommendations").json()
    finally:
        stop(process)
    process = start()
    try:
        with httpx.Client(base_url=url, timeout=5) as client:
            client.headers["Authorization"] = "Bearer " + token
            assert client.get(f"/users/{uid}/evaluations").json() == history
            assert client.get(f"/users/{uid}/recommendations").json() == recommendations
            assert len(client.get(f"/users/{uid}/preferences").json()) == 2
            assert len(client.get("/movies").json()) == 20
    finally:
        stop(process)
