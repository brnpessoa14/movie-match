# Arquitetura — CineMatch 2

**XML/Activity → Repository/Retrofit → FastAPI/Pydantic → SQLAlchemy → SQLite.** Imagens são carregadas pelo Coil usando URLs do catálogo, com fallback vetorial. O Android nunca acessa o SQLite diretamente.

## Autenticação e ciclo de vida

O cadastro cria `User` e `Account` na mesma transação. `auth.py` aplica PBKDF2-HMAC-SHA256 com salt aleatório e 600.000 iterações à senha. O login verifica o hash com comparação constante e devolve um token aleatório de 256 bits. O banco guarda apenas o hash desse token e a expiração; logout exclui a sessão correspondente.

`HTTPBearer` integra a autenticação ao Swagger. As rotas de preferências, perfil, descoberta, avaliações e recomendações dependem da sessão e conferem a correspondência entre a conta e o usuário solicitado. Saber um ID não autoriza acesso a ele. O catálogo e os gêneros são públicos.

No Android, `Profile` mantém a sessão somente em memória. O ID salvo pela versão antiga no SharedPreferences é ignorado. `MainActivity` sempre apresenta login. `CineMatchApplication` acompanha as Activities iniciadas; ao deixar o app em segundo plano, descarta a sessão. Ao retornar, `BaseActivity` abre o login e limpa a pilha das telas privadas. Rotação e navegação interna não são tratadas como uma nova entrada; há uma exceção transitória para o seletor de compartilhamento.

Senhas não são persistidas no dispositivo nem retornadas pela API. A duração de oito horas no servidor é um limite máximo da sessão, e não uma opção de login automático. O registro de conta permanece no banco, assim como suas preferências e avaliações.

Referências técnicas: [OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html) e [FastAPI Security](https://fastapi.tiangolo.com/tutorial/security/). A escolha de uma sessão opaca revogável permite evitar uma dependência de JWT neste projeto local.

## Responsabilidades

- `data/Api.kt`: contratos Retrofit e interceptor Bearer.
- `data/Models.kt`: payloads JSON de conta, filmes e avaliações.
- `data/Profile.kt`: sessão móvel em memória.
- `ui/BaseActivity.kt`: navegação, menu de conta, tratamento de erros e carregamento.
- `ui/MainActivity.kt`: validação, cadastro e login.
- Demais Activities: preferências, avaliação, recomendações e lista pessoal.
- `ui/MovieViews.kt`: cartaz, informações e Intent de compartilhamento reutilizáveis.
- `ui/MovieAdapter.kt`: cards de listas.
- `ui/MaxWidthLayout.kt`: limita e centraliza a largura dos conteúdos.
- `backend/app/auth.py`: derivação/verificação de senha, sessões e autorização.
- `database.py` e `models.py`: sessões SQL e restrições relacionais.
- `schemas.py` e `main.py`: validação e endpoints.
- `recommendations.py`: recomendação independente da UI e da OMDb.
- `catalog.py` e `enrich.py`: seed idempotente e enriquecimento manual.

## Tabelas

| Tabela | Integridade |
|---|---|
| users | ID do perfil e nome de exibição |
| accounts | Usuário único, FK única para users, hash da senha e conclusão de preferências |
| auth_sessions | FK para accounts, hash único do token e expiração |
| movies | ID, IMDb ID único, metadados e notas opcionais |
| genres | ID e nome único |
| movie_genres | PK composta e FKs |
| movie_cast | PK movie_id/cast_order e FK |
| user_preferences | PK user_id/genre_id e FKs |
| evaluations | FKs, unicidade user_id/movie_id e CHECK dos quatro status |

As tabelas novas são criadas sem alterar ou apagar as tabelas existentes. Perfis antigos sem credenciais são preservados e não podem ser assumidos apenas pelo ID. Um backup SQLite é feito antes da atualização do ambiente local.

## Interface adaptável

Formulários e descoberta têm largura máxima de 560 dp; listas usam até 1080 dp. Listas mostram duas colunas a partir de 720 dp. A descoberta possui layout próprio na horizontal, com filme e ações em painéis separados. Qualificadores de altura ajustam o cartaz em telas menores. Conteúdo longo e teclado usam rolagem; as quatro ações ficam acessíveis. Os recuos do sistema são aplicados uma única vez no shell.

## Persistência e recomendação

Cada operação de escrita confirma uma transação. A avaliação só avança depois da confirmação. Se o próximo carregamento falha após salvar, a nova tentativa não repete a avaliação confirmada. O banco também impede duplicações. As Activities consultam novamente os dados ao retomar a navegação.

A recomendação usa +3 para preferências; +5/+2/−4/−1 para avaliações; média por gênero e ID como desempate. Exclui todos os filmes já avaliados e calcula novamente em cada consulta. IMDb e Rotten Tomatoes são informações de exibição, não parte do peso de recomendação.


## Evolução Banner (3.0)

As cinco Activities originais foram preservadas; ProfileActivity acrescenta edição real da conta e configurações. Gêneros continuam no fluxo de cadastro e também são acessíveis pelo perfil. O shell possui avatar, quatro destinos, insets reais e erros contextuais. A navegação continua disponível durante consultas; a requisição de avaliação bloqueia toques duplicados.

DiscoveryActivity utiliza somente dois contextos: não assisti e já assisti. Mudar o seletor não envia HTTP. Depois de um POST confirmado, remove o filme da fila carregada, restaura o contexto inicial e atualiza a tela; falhas mantêm filme e contexto. As listas consultam novamente a API ao retomar. Detalhes e compartilhamento permanecem nativos.

O endpoint PATCH /users/{id} aceita nome e usuário opcional, normaliza o usuário, valida formato e unicidade e faz a atualização na mesma transação. O ID da conta e as relações existentes permanecem iguais. GET /users/{id} inclui o usuário da conta autenticada. A autorização existente permanece obrigatória.

Appearance salva somente a opção Sistema/Claro/Escuro em SharedPreferences e aplica AppCompatDelegate no início do processo. Cores claras e escuras ficam em values e values-night. A descoberta adapta cartaz, introdução e resumo à altura disponível, sem reduzir globalmente a tipografia. Textos completos permanecem no diálogo. Fontes ampliadas podem rolar a área do filme; ações e navegação permanecem acessíveis.
