# Validação — CineMatch 2.0

Atualização de 14/11/2026. Diretório final: **D:\movie-match**. APK: `android/app/build/outputs/apk/debug/app-debug.apk`, versionCode 2/versionName 2.0.

## Implementado e verificado

- Nova identidade visual: azul escuro, coral, tipografia consistente, cartazes reais, fundos cinematográficos, cards de gêneros e navegação inferior arredondada.
- Cinco Activities Kotlin/XML preservadas, com Intents explícitas e compartilhamento ACTION_SEND/createChooser.
- Login com nome de usuário e senha, cadastro, confirmação da senha, validação, menu de conta e logout.
- Abertura no login; IDs gravados pelo protótipo antigo são ignorados. Ao sair para o segundo plano e voltar, são exigidas credenciais novamente. Rotação, navegação interna e seletor de compartilhamento mantêm a sessão em uso.
- Senha derivada com PBKDF2-HMAC-SHA256/salt individual, sessão móvel somente em memória e tokens do backend armazenados apenas como hash.
- Autorização nas rotas privadas: uma conta não consegue acessar dados de outra alterando o ID da requisição.
- Preferências, quatro avaliações, recomendações, histórico, filtro, alteração e exclusão continuam integrados à API/SQLite.
- Cadastro e login de uma segunda conta não mostram o histórico da primeira. Entrar novamente na primeira recupera suas avaliações.
- Layout compacto, layout horizontal com painel lateral e recomendações em duas colunas em telas largas.
- Os 20 filmes, cartazes e notas importados anteriormente da OMDb continuam no banco. Esta atualização não fez novas consultas à OMDb.
- Processo Android encerrado e relançado: abriu MainActivity/login, sem entrar no perfil demonstrativo.

## Resultados efetivos

| Verificação | Resultado |
|---|---|
| Pytest no diretório final | **15 testes aprovados**, 1 aviso de depreciação Starlette/httpx |
| `assembleDebug` em D:\movie-match\android | **BUILD SUCCESSFUL**, APK 2.0 gerado e instalado |
| `lintDebug` | **Zero erros**, 32 avisos não bloqueantes, sem baseline para ocultá-los |
| Android — fluxo completo + layout normal | **2 testes aprovados**, zero falhas |
| Android — 360 × 640 dp | **1 execução do teste de layout aprovada**; login rolável, título e quatro ações acessíveis |
| Android — 853 × 533 dp horizontal | **1 execução do teste de layout aprovada**; painel lateral e lista em duas colunas |
| Reinício HTTP real | Aprovado pelo Pytest; dados e sessão continuam no banco após reiniciar o processo |
| Reinício do processo Android | Volta ao login, conforme a nova regra |
| Comparação do banco com o backup | Todas as linhas originais preservadas nas sete tabelas anteriores |

O teste completo inclui cadastro, gêneros, quatro avaliações, recomendações sem títulos já avaliados, detalhes, compartilhamento, filtro, alteração/exclusão, edição das preferências, logout, senha incorreta, novo login, segunda conta e retorno ao login depois de sair para a tela inicial do Android. O teste de layout também confere a quantidade de colunas das recomendações conforme a largura disponível.

As execuções em tamanhos diferentes são repetições do teste de responsividade, e não quatro testes distintos de negócio. O tamanho normal do emulador foi restaurado ao concluir.

## Evidências

- `docs/backend-tests.xml`: 15 testes Python.
- `docs/android-tests.xml`: 2 testes Android da versão final.
- `docs/android-tests-compact.xml` e `docs/android-tests-wide.xml`: execuções específicas de responsividade.
- `docs/lint-results-debug.xml`: Lint final, sem erros e com avisos registrados.
- `docs/data-preservation.json`: comparação por linhas com o backup anterior à criação das contas.
- `docs/restart-check.json`: nova política de login após reinício.
- `docs/screenshots/01-inicio.png` a `09-login-processo-reiniciado.png`: telas reais de login, gêneros, descoberta, recomendações, lista, compartilhamento, cadastro e reabertura.
- `docs/screenshots/responsive-compact-*`, `responsive-normal-*` e `responsive-wide-*`: capturas reais das adaptações.

A sexta captura corresponde ao fluxo de detalhes/compartilhamento. Nenhum compartilhamento foi enviado a um aplicativo externo. As capturas não são mockups.

## Preservação e uso das contas

Antes de atualizar, foi criado o backup local `.tools/backups/before-accounts.db`, ignorado pelo Git. A comparação confirmou a preservação de **7 perfis antigos, 20 filmes, 11 gêneros, 52 associações de gêneros, 60 registros de elenco, 8 preferências e 18 avaliações**. A chave OMDb em `.env` foi mantida e não faz parte do código ou do commit.

Perfis antigos sem senha continuam no banco, mas não oferecem entrada automática nem são assumidos por uma conta recém-criada. Na primeira utilização da versão 2.0, toque em **Criar conta**. A conta cadastrada pode ser acessada novamente com o mesmo usuário e senha; suas próprias avaliações ficam persistidas. As contas criadas pelos testes são apenas demonstrativas.

## Limites e pendências

- A API deve estar ligada e acessível para usar catálogo, conta e histórico. O build debug usa HTTP local; distribuição externa exige HTTPS e configuração de produção.
- Não há recuperação de senha por e-mail ou autenticação social nesta versão.
- Dispositivo físico, release assinado, Play Store e deploy em nuvem não foram validados nesta atualização.
- Publicação do repositório no GitHub continua pendente de autorização e configuração do remoto. O projeto está em Git local.
- A apresentação oral e o domínio do código precisam ser realizados pelo estudante; há um roteiro atualizado em `docs/presentation.md`.

## Critérios AP2

| Critério | Evidência |
|---|---|
| Cinco Activities | Main, Preferences, Discovery, Recommendations e History |
| Intents explícitas e implícita | Navegação e ACTION_SEND/createChooser |
| Pelo menos seis Views | TextView, EditText, Button, ImageView, CheckBox, RecyclerView, ProgressBar, CardView, Spinner, ScrollView, GridLayout |
| Interface organizada | Capturas reais e testes em três configurações de tela |
| API REST/OpenAPI | FastAPI, Retrofit, autenticação Bearer, CRUD e Swagger |
| Banco relacional | SQLite/SQLAlchemy, nove tabelas, FKs, unicidade e persistência verificada |
| Organização e qualidade | Módulos separados, testes e documentação atualizada |
| README/screenshots | Incluídos |
| GitHub público | Pendente de publicação |
| Apresentação oral | Roteiro entregue; apresentação pendente |

## Executar

Abra **D:\movie-match\android** no Android Studio e execute `app` com ▶. Na primeira abertura, use Criar conta. Se a API não estiver em execução:

```powershell
cd D:\movie-match
.\start-backend.ps1
```

O emulador usa `http://10.0.2.2:8000/`; o Swagger no computador fica em `http://localhost:8000/docs`. Consulte o README para comandos de build, testes e conexão USB.
