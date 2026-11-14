# Roteiro de apresentação — CineMatch

## Antes da aula

1. Iniciar a API com `D:\movie-match\start-backend.ps1` e conferir `/health` e `/docs`.
2. Abrir `D:\movie-match\android` no Android Studio e iniciar o emulador. Para celular USB, compilar com URL `http://127.0.0.1:8000/` e usar `adb reverse tcp:8000 tcp:8000`.
3. Conferir navegação e conexão. Não apagar o banco da demonstração.
4. Ter README, Swagger, código e link GitHub publicados disponíveis. Estudar as classes citadas abaixo.

## Fala e demonstração (6–8 minutos)

“O CineMatch ajuda a escolher o próximo filme. Ele usa preferências e quatro tipos de avaliação para recomendar títulos de forma simples e explicável.”

1. **Início:** cadastrar um perfil. Explicar POST `/users` e ID em SharedPreferences. Não existe senha nesta versão.
2. **Preferências:** selecionar ficção científica e aventura. Mostrar CheckBoxes, seleção múltipla e PUT de preferências.
3. **Descoberta:** percorrer um filme, mostrar duração, elenco e sinopse expansível. Explicar N/D para notas ausentes. Usar os quatro botões em filmes diferentes. Explicar que o app só avança depois de salvar.
4. **Compartilhar:** abrir detalhes, tocar “Compartilhar filme” e mostrar o seletor Android. Explicar `ACTION_SEND`, MIME `text/plain` e `createChooser`; cancelar sem enviar a terceiros.
5. **Para você:** mostrar recomendação e motivo. Explicar pesos +3/+5/+2/−4/−1, média por gênero e exclusão dos filmes avaliados.
6. **Minha lista:** filtrar “quero assistir”, mudar para “gostei”, excluir outra avaliação. Voltar às recomendações para consultar o resultado recalculado.
7. **Swagger:** mostrar schemas, validação, GET de histórico, PATCH e DELETE. Ressaltar JSON e códigos HTTP.
8. **Persistência:** reiniciar API, relançar app e demonstrar mesmos dados/ID. Mostrar as tabelas e as FKs no código.

## Onde explicar no código

- Activities e Intents explícitas: `android/app/src/main/java/br/com/cinematch/ui/`.
- Intent implícita e detalhes: `ui/MovieViews.kt`.
- Retrofit: `data/Api.kt`; modelos de request/response: `data/Models.kt`.
- Integridade: `backend/app/models.py`; validação: `schemas.py`.
- Cálculo: `backend/app/recommendations.py`.
- Testes: `backend/tests/test_api.py` e `android/app/src/androidTest/.../CineMatchFlowTest.kt`.

## Perguntas possíveis

**Por que cinco Activities?** A AP2 exige cinco Activities reais. Cada uma representa uma responsabilidade funcional e usa layout XML, com navegação por Intents explícitas.

**Qual é a Intent implícita?** Compartilhamento de texto com `ACTION_SEND`; o Android resolve quais apps aceitam esse conteúdo. Ela não define uma classe de Activity de destino.

**Onde ficam os dados?** No SQLite do backend, acessado pelo SQLAlchemy. SharedPreferences guarda apenas dados mínimos do perfil local. Avaliações e preferências são lidas da API.

**Como evitar duplicação?** Botões bloqueados durante a operação, flag de confirmação para não reenviar em retry após sucesso e restrição SQL única `(user_id, movie_id)`.

**Por que Retrofit com Coroutines?** Para declarar endpoints e converter JSON sem bloquear a UI. O ciclo de vida cancela tarefas de Activities destruídas.

**É inteligência artificial?** Não. É um algoritmo determinístico por afinidade de gêneros. É simples de explicar, testar e reproduzir; não apresenta precisão científica ou porcentagens inventadas.

**O que ocorre sem OMDb?** O catálogo local continua disponível na própria API. Cartazes usam ilustração original e notas ficam N/D; os dados podem ser enriquecidos por comando quando uma chave válida for configurada.

**Por que 10.0.2.2?** É o endereço especial do emulador padrão para o loopback do computador. Localhost dentro do Android é o Android, não o Windows.

**Pode publicar o backend desta forma?** É uma demonstração local sem autenticação. Para produção, seria necessário autenticar usuários, autorizar operações por identidade validada, usar HTTPS, migrar esquema e configurar observabilidade/segredos.

**Quais são as limitações?** Catálogo pequeno, API necessária para uso, perfil vinculado ao dispositivo e dependência opcional da OMDb para cartazes/notas. Consulte a validação para diferenciar os testes executados dos pendentes.
