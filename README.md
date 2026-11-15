# CineMatch

Aplicativo Android nativo para descobrir, avaliar e guardar filmes, com contas por usuário e senha e recomendações explicáveis. Projeto da AP2 de Desenvolvimento Mobile.

**Versão Android: 3.1 (versionCode 4).** Kotlin/XML, sete Activities, API REST FastAPI documentada com Swagger e persistência relacional em SQLite.

## Funcionalidades

- Cadastro/login, isolamento entre contas e histórico persistido no backend.
- Preferências de gêneros no cadastro e no perfil.
- Descoberta ordenada por preferências e avaliações, atualizada após cada resposta confirmada.
- Avaliações contextuais: **Não assisti** oferece Quero assistir / Não me interessa; **Já assisti** oferece Gostei / Não gostei. Trocar o seletor não grava avaliação.
- Cartaz, metadados, sinopse e elenco adaptados ao espaço disponível; detalhes completos em diálogo nativo.
- Para você com motivos reais da recomendação; Minha lista com filtro, alteração e remoção de avaliações.
- Busca de títulos na OMDb, resultados paginados e cache persistente no backend.
- Perfil com edição real de nome e usuário, gêneros, logout e aparência Sistema/Claro/Escuro.
- Compartilhamento com Intent implícita ACTION_SEND e seletor do Android.

## Executar no Windows

Requisitos: Python 3.12+, Android Studio, SDK Android e JDK compatíveis com o projeto. A configuração atual usa compileSdk 37, targetSdk 36, minSdk 26, Gradle 9.6.0 e Android Gradle Plugin 9.4.0. O toolchain de build configurado é JDK 25, com bytecode JVM 17.

### Backend

Na raiz do clone:

```powershell
.\setup.ps1 -Python 'C:\caminho\python.exe'
.\start-backend.ps1
```

Substitua o caminho de exemplo pelo Python instalado. O setup cria `.venv` e instala `backend/requirements.lock.txt`. Se o ambiente já estiver preparado, basta iniciar o backend. Mantenha o servidor ativo enquanto usa o app.

Alternativa de inicialização:

```powershell
cd backend
..\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

- [Swagger local](http://localhost:8000/docs)
- [OpenAPI local](http://localhost:8000/openapi.json)
- [Health local](http://localhost:8000/health)

Esses endereços só funcionam onde a API está executando. O banco padrão é `backend/cinematch.db`. O script raiz fixa a pasta de trabalho para manter o mesmo arquivo; não apague o banco para demonstrar persistência.

### Android

1. Abra a pasta **android** no Android Studio (`D:\movie-match\android` neste computador).
2. Aguarde o Gradle Sync, escolha a configuração **app** e um emulador.
3. Pressione **Run** para compilar e instalar o código atual. Abrir apenas o dispositivo pelo Device Manager não reinstala o APK.

O emulador validado neste computador é **CineMatch_Banner_API_37**. O endereço padrão do backend no Android é `http://10.0.2.2:8000/`; `localhost` no emulador representa o próprio Android, não o Windows.

```powershell
cd android
.\build.ps1
# APK: app\build\outputs\apk\debug\app-debug.apk
```

Na primeira abertura, crie uma conta. O usuário aceita de 3 a 32 letras sem acento, números, ponto, hífen ou sublinhado; a senha precisa ter pelo menos 8 caracteres. Depois escolha seus gêneros.

**O app pede novo login ao retornar do segundo plano.** Senha e token não são persistidos no dispositivo. Rotação, navegação interna e o seletor de compartilhamento preservam a sessão em uso. Preferências e avaliações continuam no banco da conta. A aparência é salva localmente em SharedPreferences.

### Alternativa USB (não validada nesta entrega)

Com depuração USB autorizada e platform-tools no PATH:

```powershell
.\build.ps1 -ApiBaseUrl 'http://127.0.0.1:8000/'
adb reverse tcp:8000 tcp:8000
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

HTTP local é permitido somente no debug. Uma distribuição externa exige HTTPS, assinatura e URL configurada com `-PAPI_BASE_URL=https://.../`.

## Catálogo: o que vem no clone

O banco local observado na validação tinha **511 filmes**, mas o banco, as contas e a chave OMDb **não são versionados**. Um clone novo cria um seed idempotente de **20 filmes** com sinopses editoriais em português. O catálogo cresce por importação e busca.

Para habilitar buscas, copie `backend/.env.example` para `backend/.env` e preencha `OMDB_API_KEY` localmente. Sem chave, o seed e os fluxos internos continuam funcionando; novas buscas externas exigem chave válida e cota disponível.

Expansão opcional, a partir da raiz:

```powershell
cd backend
..\.venv\Scripts\python.exe -m app.expand --target 500 --requests 650
```

Esse comando usa consultas reais da OMDb. Não é executado automaticamente ao abrir o app. `app.enrich` enriquece filmes já existentes e também consome consultas; não precisa ser repetido para demonstrar o sistema.

A OMDb oferece busca por título e fichas. O CineMatch calcula a ordenação por gêneros. Resultados de busca são cacheados por sete dias; novas fichas são importadas sem sobrescrever os títulos e sinopses dos filmes já existentes. Sinopses novas podem estar em inglês. Cartazes ausentes usam fallback e notas ausentes mostram N/D. Consulte a [OMDb](https://www.omdbapi.com/) e as [referências do catálogo inicial](docs/catalog-sources.md).

Consultar descoberta ou recomendações usa o SQLite e não uma consulta OMDb por candidato. A API interna e o carregamento de imagens continuam necessários; o aplicativo não é totalmente offline.

## Activities, Intents e Views

| Activity | Responsabilidade |
|---|---|
| MainActivity | Login e cadastro |
| PreferencesActivity | Seleção e persistência dos gêneros |
| DiscoveryActivity | Candidato atual e quatro avaliações contextuais |
| RecommendationsActivity | Recomendações com motivos e detalhes |
| HistoryActivity | Minha lista, filtro, alteração e exclusão |
| ProfileActivity | Dados da conta, gêneros, aparência e logout |
| CatalogActivity | Busca OMDb, páginas e detalhes |

Todas são Activities reais com XML e navegação por Intents explícitas. O compartilhamento em `ui/MovieViews.kt` usa `ACTION_SEND`, `text/plain` e `Intent.createChooser`; nenhum conteúdo é enviado automaticamente.

Views usadas: TextView, EditText/TextInputEditText, MaterialButton, ImageView/ImageButton, MaterialCheckBox, Spinner, RecyclerView, ProgressBar, MaterialCardView, ScrollView e GridLayout.

## Capturas reais da versão 3.1

| Descoberta | Busca |
|---|---|
| ![Descoberta](docs/screenshots/expanded/responsive-normal-discovery.png) | ![Busca](docs/screenshots/expanded/catalog-search.png) |

Também há capturas de temas, tela compacta, horizontal e fonte em 200% em `docs/screenshots/expanded/`. As imagens usam contas de teste.

## API

| Método | Caminho | Função |
|---|---|---|
| GET | `/health` | Saúde da API/banco |
| POST | `/auth/register`, `/auth/login` | Conta e sessão Bearer |
| POST | `/auth/logout` | Revoga a sessão atual |
| GET / PATCH | `/users/{id}` | Consulta/altera a própria conta |
| GET | `/genres` | Gêneros existentes |
| GET / PUT | `/users/{id}/preferences` | Consulta/substitui preferências |
| GET | `/movies`, `/movies/{id}` | Catálogo e ficha |
| GET | `/movies/discovery?user_id=...` | Candidatos personalizados, lote padrão 20 |
| GET | `/movies/search?q=...&page=...` | Busca OMDb autenticada e cache |
| POST | `/evaluations` | Cria avaliação única por conta/filme |
| GET | `/users/{id}/evaluations` | Histórico e filtro opcional de status |
| PATCH / DELETE | `/evaluations/{id}?user_id=...` | Altera/remove a própria avaliação |
| GET | `/users/{id}/watchlist` | Filmes que deseja assistir |
| GET | `/users/{id}/recommendations` | Filme, score e motivo |

No Swagger, cadastre/entre, copie `access_token` e use **Authorize**. Os IDs nas rotas privadas precisam corresponder à identidade do token. A descoberta informa `X-Catalog-Total` e `X-Remaining-Count`, para distinguir lote recebido e total disponível.

Senhas usam PBKDF2-HMAC-SHA256 com salt individual e 600.000 iterações. Tokens opacos são aleatórios, guardados no banco apenas como hash SHA-256 e expiram em oito horas. O interceptor OkHttp envia Bearer. A sessão móvel é somente em memória; não há recuperação por e-mail nem login social.

## Recomendação

Peso por gênero: +3 para preferência, +5 para GOSTEI, +2 para QUERO_ASSISTIR, -4 para NAO_GOSTEI e -1 para SEM_INTERESSE. O score do candidato é a média dos pesos dos seus gêneros. Ordenação decrescente por score e crescente por ID no empate.

Todos os filmes já avaliados são excluídos, inclusive os desejados. Descoberta e Para você usam essa regra. Após cada avaliação confirmada, a descoberta consulta a fila recalculada. IMDb/Rotten Tomatoes não participam do score. Não há treinamento de modelo nem percentuais de compatibilidade inventados.

## Organização, persistência e testes

```text
android/app/src/main/java/br/com/cinematch/
  data/                 Retrofit, DTOs, sessão e aparência
  ui/                   Activities, adapter, detalhes e layout adaptável
android/app/src/main/res/ Layouts XML, temas e recursos
backend/app/main.py     Rotas FastAPI e dependências
backend/app/schemas.py  Validação Pydantic e contratos
backend/app/models.py  Onze tabelas, relacionamentos e restrições
backend/app/auth.py     Senhas, sessões e autorização
backend/app/recommendations.py  Regra de recomendação
backend/app/omdb_catalog.py     Busca, importação, cache e cota
backend/tests/          Testes em bancos temporários
docs/                   Arquitetura, apresentação, relatórios e capturas
```

```powershell
cd backend
..\.venv\Scripts\python.exe -m pytest -q
cd ..\android
.\gradlew.bat assembleDebug assembleDebugAndroidTest lintDebug
# Com API e emulador ativos:
.\gradlew.bat connectedDebugAndroidTest
```

Resultados registrados na versão 3.1: **20 testes do backend e 8 verificações Android passaram** (5 testes principais + 3 execuções de responsividade). Build concluído; lint com 0 erros e 71 avisos. Testes Android criam contas adicionais na API em execução. Consulte o [relatório 3.1](docs/validation-3.1.md), a [arquitetura](docs/architecture.md) e o [roteiro de apresentação](docs/presentation.md).

## Limites da entrega

Validação em emulador API 37 e API local. Android físico, Play Store e nuvem não foram validados. O GitHub contém o código e a documentação; ele não hospeda a API. SQLite e HTTP debug atendem à demonstração local; uma implantação de produção exige configuração, HTTPS e operação próprios. O arquivo de banco e os registros de conta permanecem locais.
