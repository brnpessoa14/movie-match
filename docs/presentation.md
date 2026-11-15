# Apresentação - CineMatch 3.1

## Preparar

1. Iniciar a API com start-backend.ps1 e conferir http://localhost:8000/health.
2. Abrir a pasta android no Android Studio e executar app no emulador.
3. Ter uma conta de apresentação com credenciais conhecidas e dados de teste que podem ser alterados.
4. Abrir Swagger em http://localhost:8000/docs e o repositório GitHub. Os links da API são locais.

## Demonstração sugerida (6-8 minutos)

1. Apresentar a proposta: coletar preferências/avaliações e sugerir filmes por uma regra explicável.
2. Mostrar login/cadastro e gêneros; explicar que os dados pertencem à conta no backend.
3. Na descoberta, mudar o contexto sem gravar, avaliar e mostrar a fila recalculada.
4. Abrir Para você e explicar um motivo real e os pesos por gênero.
5. Em Minha lista, filtrar, alterar status e excluir uma avaliação de teste.
6. Abrir detalhes e o compartilhamento ACTION_SEND, cancelando sem enviar conteúdo.
7. Mostrar perfil, aparência e busca por um título na OMDb; consultas novas dependem de cota/internet.
8. Demonstrar uma rota autenticada e o CRUD no Swagger; abrir os arquivos centrais.
9. Reabrir o app, entrar e mostrar que o histórico persiste. Não alegar bônus não validado.

## Pontos do código

- Sete Activities e navegação: AndroidManifest.xml e ui/BaseActivity.kt.
- Login e sessão: MainActivity, data/Profile.kt, CineMatchApplication e backend/app/auth.py.
- Intent implícita: ui/MovieViews.kt, ACTION_SEND e createChooser.
- Avaliação e retry: ui/DiscoveryActivity.kt, saved e pendingRefresh.
- Contratos e JSON: data/Api.kt, Models.kt e backend/app/schemas.py.
- Banco/FKs/restrições: backend/app/models.py e database.py.
- Regra de pontuação: backend/app/recommendations.py.
- Busca/cache: backend/app/omdb_catalog.py.

O número de filmes do ambiente é variável. Um clone novo começa com os 20 filmes do seed, pois banco e chave não são versionados. Consulte README.md para preparar o ambiente e docs/validation-3.1.md para as verificações registradas.
