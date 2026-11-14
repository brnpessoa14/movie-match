# Validação e entrega — 14/11/2026

Diretório final: **D:\movie-match**. A pasta `.idea` que já existia foi preservada. O projeto foi implementado e inicialmente testado em uma pasta temporária do workspace autorizado; os fontes finais foram copiados e o backend, a compilação e o Lint foram verificados também no diretório final. O APK final está em `android/app/build/outputs/apk/debug/app-debug.apk`.

## Implementado e verificado

- Cinco Activities nativas Kotlin com XML: início, preferências, descoberta, recomendações e histórico.
- Intents explícitas para navegação; `ACTION_SEND`/`createChooser` para compartilhamento, com captura da Intent pelo teste. Nenhum conteúdo foi enviado a um aplicativo externo.
- Views variadas, interface em português, tema escuro, carregamento, estados vazios, feedback, botões desabilitados durante requisições e nova tentativa em erros.
- Cadastro e recuperação do perfil salvo; consulta e edição posterior de gêneros.
- Catálogo persistido com 20 títulos reais e 11 gêneros, duração, elenco e sinopses próprias. Ausência de cartazes/notas identificada com ilustração original e N/D.
- Quatro avaliações com significados distintos, unicidade usuário/filme e avanço somente após confirmação da API.
- Recomendações personalizadas, determinísticas e explicáveis; exclusão de todos os títulos já avaliados.
- Histórico, filtro “quero assistir”, alteração e exclusão de avaliações.
- FastAPI com CRUD, JSON/Pydantic, Swagger/OpenAPI e SQLite/SQLAlchemy, FKs e restrições de integridade.
- API real → arquivo SQLite → API real → Android no emulador Android 17/API 37.
- README, arquitetura, roteiro oral, fontes do catálogo e seis capturas reais, sem imagens simuladas apresentadas como screenshots.

### Resultados efetivos

| Verificação | Resultado |
|---|---|
| Pytest no diretório final | **11 passed**, 1 aviso de depreciação do cliente de teste Starlette/httpx |
| `assembleDebug` em D:\movie-match\android | **BUILD SUCCESSFUL** |
| `lintDebug` no diretório final | **BUILD SUCCESSFUL**, zero erros, 29 avisos não bloqueantes |
| Teste Android `fullFlowWithRealBackend` | **1 teste aprovado**, zero falhas, em emulador API 37 |
| Swagger no navegador | `/docs` carregou e GET `/health` executado com HTTP 200, `{"status":"ok"}` |
| Reinício real do processo HTTP | Aprovado no teste `test_live_http_server_survives_restart`: mesmo histórico, recomendações, preferências e catálogo após novo processo |
| Reinício real do processo Android | `am force-stop`, relançamento de MainActivity e retorno a DiscoveryActivity; ID 7 e 3 avaliações preservados |
| Banco da pasta final | Dados demonstrativos preservados via backup SQLite; API iniciada a partir de `D:\movie-match\backend` |

O teste instrumentado percorre cadastro, seleção de gênero, quatro status, exclusão dos avaliados das recomendações, detalhes, compartilhamento, filtro, alteração, exclusão, edição posterior de preferências e relançamento do início com o mesmo perfil. Há verificações diretas na API além das interações da interface. O teste não utiliza uma API simulada.

Os 29 avisos de Lint incluem sugestões de atualização de dependências/target, internacionalização adicional, otimização do adaptador e recursos/tema. Não foram suprimidos por uma baseline. O aviso Python diz respeito ao adaptador de teste Starlette/httpx e não impediu os testes. Não se afirma ausência de avisos.

### Evidências incluídas

- `docs/backend-tests.xml`: relatório JUnit dos 11 testes Python.
- `docs/android-tests.xml`: relatório do teste instrumentado.
- `docs/restart-check.json`: verificação de recuperação do perfil após encerrar o processo Android.
- `docs/lint-results-debug.xml`: relatório final do Lint.
- `docs/screenshots/01-inicio.png` a `06-compartilhar.png`: capturas reais das cinco Activities e do diálogo com o botão de compartilhamento. A sexta captura mostra os detalhes do filme; não é apresentada como captura do seletor do sistema.

## Implementado, mas ainda não executado com serviço externo

O comando `python -m app.enrich` implementa enriquecimento por OMDb. Foram testados o parser de notas ausentes/presentes e o tratamento de resposta sem dados. **A consulta real à OMDb não foi executada**, pois não foi fornecida uma chave. Para habilitar cartazes e notas externas, configure `OMDB_API_KEY` em `backend/.env` e execute o comando. A chave não pertence ao Android nem ao repositório.

O build de entrega é **debug para desenvolvimento local**, com HTTP habilitado somente nesse tipo de build. Release assinado/HTTPS, publicação em loja e dispositivo físico não foram validados e não são apresentados como concluídos.

## Pendente

- **GitHub público:** projeto preparado em Git local. Falta criar/selecionar o repositório remoto e autorizar/publicar. Nenhum repositório público foi criado nem anunciado.
- **Apresentação oral:** o roteiro foi entregue, mas domínio e apresentação precisam ser realizados pelo estudante.
- **Bônus:** dispositivo físico, Play Store e nuvem não foram executados. A demonstração local foi priorizada.

O perfil instalado no emulador é demonstrativo, criado pelos testes. O banco contém registros desses testes de integração e está ignorado pelo Git. A API não implementa autenticação; use esta configuração para demonstração local.

## Checklist dos critérios AP2

| Critério | Peso | Evidência / situação |
|---|---:|---|
| Funcionamento geral | 1,0 | Fluxo real aprovado no emulador com a API |
| Pelo menos 5 Activities | 1,5 | Cinco classes registradas no manifesto e percorridas no teste |
| Intents explícitas e implícita | 1,0 | Navegação explícita e compartilhamento ACTION_SEND |
| Interface e UX | 1,0 | Tema, diferentes Views, feedback e capturas reais; avaliação qualitativa cabe ao professor |
| API REST | 1,0 | Retrofit/FastAPI, CRUD e Swagger, integração testada |
| Banco relacional | 1,0 | SQLite/SQLAlchemy, FKs, unicidade e reinício verificado |
| Organização e qualidade | 1,0 | Pacotes/módulos separados, testes e documentação |
| Apresentação e domínio | 2,5 | Roteiro entregue; apresentação pelo estudante pendente |
| Pelo menos 6 Views | Obrigatório | TextView, EditText, Button, ImageView, CheckBox, RecyclerView, ProgressBar, CardView, Spinner |
| Swagger/OpenAPI | Obrigatório | /docs e /openapi.json verificados |
| README e screenshots | Obrigatório | README e seis PNGs reais incluídos |
| Repositório GitHub público | Obrigatório | **Pendente de publicação** |

Os pesos representam os critérios da atividade, não uma nota atribuída ao projeto.

## Comandos de execução

```powershell
cd D:\movie-match
.\start-backend.ps1
```

Se precisar recriar o ambiente Python: `.\setup.ps1 -Python 'C:\caminho\python.exe'` com Python 3.12+. No momento da entrega, o ambiente `.venv` já está instalado e a API está em execução na porta 8000.

```powershell
cd D:\movie-match\android
.\build.ps1
```

Abra `D:\movie-match\android` no Android Studio e execute `app` no emulador. URL padrão: `http://10.0.2.2:8000/`. Para os comandos de testes, USB, OMDb e publicação, consulte o README.
