# Arquitetura - CineMatch 3.1

**Activity/XML -> Repository/Retrofit/OkHttp -> FastAPI/Pydantic -> SQLAlchemy -> SQLite.** Coil carrega URLs de imagens. O Android não acessa o banco de contas/avaliações diretamente.

## Android

Há sete Activities: Main, Preferences, Discovery, Recommendations, History, Profile e Catalog. BaseActivity compartilha navegação, insets, carregamento, erro e retry. data/Api.kt define o contrato REST e o interceptor Bearer; Models.kt contém DTOs. MovieAdapter reutiliza itens de RecyclerView. MovieViews cuida de dados, diálogo de detalhes e ACTION_SEND. MoviePresentation mede a área disponível e adapta a apresentação.

Profile mantém AuthResult somente em memória. CineMatchApplication descarta essa sessão ao entrar em segundo plano, preservando mudanças de configuração e a exceção transitória do compartilhamento. BaseActivity redireciona telas privadas sem sessão ao login e limpa a pilha. Appearance persiste somente Sistema/Claro/Escuro em SharedPreferences.

## Backend e dados

main.py define rotas, dependências e transações. schemas.py valida os contratos. auth.py gera/verifica PBKDF2 e sessões opacas, exige Bearer e compara a identidade da sessão com o ID solicitado. database.py cria sessões por requisição e ativa FKs no SQLite. create_all cria tabelas ausentes; não substitui migrações de alterações de colunas.

| Tabela | Função e integridade |
|---|---|
| users | Identidade do perfil, nome e criação |
| accounts | Username único, user_id único, hash e preferences_ready |
| auth_sessions | FK account_id, hash único do token e expiração |
| movies | IMDb ID único e metadados |
| genres | Nome único |
| movie_genres | Associação N:M; PK composta e FKs |
| movie_cast | Elenco por filme; PK movie_id/cast_order |
| user_preferences | Associação N:M; PK composta e FKs |
| evaluations | FKs, UNIQUE user_id/movie_id e CHECK dos quatro status |
| catalog_searches | Cache auxiliar por consulta/página; IDs em JSON, total e data |
| provider_usage | Contador por dia UTC |

User e Account têm relação 1:1. Account possui N sessões. Usuários e filmes têm N gêneros por tabelas associativas. Cada avaliação liga um usuário a um filme e guarda status e datas. O JSON de IDs de catalog_searches é uma simplificação de cache auxiliar, não o modelo do histórico.

## Avaliação e recomendação

O seletor muda apenas o contexto. O clique de ação envia POST. busy bloqueia repetições; saved conserva a confirmação entre tentativas. Depois do POST, pendingRefresh mantém os controles de avaliação bloqueados até o GET da nova fila. Se o POST falha, o filme não avança; se apenas o GET falha, o retry não repete o POST. UNIQUE protege também no banco.

recommendations.py soma +3 por preferência e +5/+2/-4/-1 por avaliação em cada gênero, calcula a média do candidato, exclui todos os filmes já avaliados e desempata por ID. Descoberta e recomendações usam essa regra; não há modelo de aprendizado de máquina. A descoberta usa cabeçalhos para informar o total completo e os candidatos restantes, independentemente do lote de 20.

## OMDb e cache

O seed idempotente contém 20 filmes. expand.py faz importação opcional e omdb_catalog.py busca títulos/páginas e fichas reais. Filmes existentes são preservados. Gêneros importados são mapeados; notas e cartazes ausentes têm fallback. Novas sinopses podem estar em inglês.

Uma busca normalizada por texto/página reutiliza o cache por sete dias. A reserva de quota usa operação condicional atômica específica de SQLite; workers fazem HTTP, não compartilham a Session SQL. Consultas à descoberta usam os dados salvos, sem consultar a OMDb por candidato. .env e o banco não são publicados.

## Validação e limites

O relatório docs/validation-3.1.md registra os resultados executados. O ambiente validado é Android API 37 em emulador com FastAPI local. Deploy, Android físico e Play Store não foram validados. O banco local ampliado não acompanha um clone; o seed e a importação documentada permitem preparar outro ambiente.
