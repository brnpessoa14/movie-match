from test_api import api


def test_profile_update_is_atomic_and_keeps_identity_and_session(api):
    c, _ = api
    first = c.post('/auth/register', json={'name': 'Ana', 'username': 'ana', 'password': 'CinemaSenha123!'}).json()
    other = c.post('/auth/register', json={'name': 'Outra', 'username': 'ocupado', 'password': 'CinemaSenha123!'}).json()
    uid = first['user']['id']
    c.headers['Authorization'] = 'Bearer ' + first['access_token']
    c.put(f'/users/{uid}/preferences', json={'genre_ids': [1]})
    c.post('/evaluations', json={'user_id': uid, 'movie_id': 1, 'status': 'GOSTEI'})
    assert c.patch(f'/users/{other["user"]["id"]}', json={'name': 'Tentativa', 'username': 'invasao'}).status_code == 403
    assert c.patch(f'/users/{uid}', json={'name': 'Não salvar', 'username': 'ocupado'}).status_code == 409
    assert c.get(f'/users/{uid}').json()['name'] == 'Ana'
    for username in ['bad user', 'x', '', None]:
        result = c.patch(f'/users/{uid}', json={'name': 'Ana', 'username': username})
        assert result.status_code == (200 if username is None else 422)
    changed = c.patch(f'/users/{uid}', json={'name': ' Ana Cinema ', 'username': ' ANA.CINEMA '})
    assert changed.status_code == 200
    assert changed.json()['id'] == uid
    assert changed.json()['name'] == 'Ana Cinema'
    assert changed.json()['username'] == 'ana.cinema'
    assert len(c.get(f'/users/{uid}/evaluations').json()) == 1
    assert c.get(f'/users/{uid}/preferences').json()[0]['id'] == 1
    assert c.post('/auth/login', json={'username': 'ana', 'password': 'CinemaSenha123!'}).status_code == 401
    login = c.post('/auth/login', json={'username': 'ana.cinema', 'password': 'CinemaSenha123!'})
    assert login.status_code == 200 and login.json()['user']['id'] == uid
