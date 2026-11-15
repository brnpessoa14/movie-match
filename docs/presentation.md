# Apresentação — CineMatch 2

## Preparar

1. Iniciar a API com `D:\movie-match\start-backend.ps1` se ela não estiver ligada.
2. Abrir `D:\movie-match\android` no Android Studio e executar `app` no emulador.
3. Criar sua própria conta, escolher gêneros e estudar o código. Não depender de um perfil automático.
4. Abrir o Swagger em `http://localhost:8000/docs`. Ter README e link GitHub disponíveis após publicar.

## Demonstração (6–8 minutos)

“O CineMatch transforma preferências e avaliações em sugestões de filmes explicáveis. Cada pessoa entra na própria conta e mantém sua lista.”

1. **Login/cadastro:** mostrar as duas opções, validação da senha e login de uma conta existente. Explicar que reabrir o app exige entrar novamente.
2. **Gêneros:** selecionar vários cards; explicar PUT de preferências e persistência no backend.
3. **Descoberta:** mostrar cartaz, título, notas, duração e sinopse expansível. Avaliar quatro filmes usando os quatro status.
4. **Compartilhamento:** abrir o seletor com ACTION_SEND e cancelar sem enviar.
5. **Para você:** apresentar uma recomendação e seu motivo. Explicar os pesos por gênero e a exclusão dos títulos avaliados.
6. **Minha lista:** filtrar “quero assistir”, mudar para “gostei” e excluir uma avaliação. Mostrar atualização nas consultas seguintes.
7. **Contas:** sair pelo ícone de pessoa e entrar novamente; o histórico continua no banco. Outra conta tem sua própria lista.
8. **Swagger e código:** mostrar cadastro/login, Authorize com o token, uma rota privada e os modelos relacionais.

## Código para explicar

- Cinco Activities e Intents explícitas: `android/app/src/main/java/br/com/cinematch/ui/`.
- Intent implícita: `ui/MovieViews.kt`, ACTION_SEND e createChooser.
- Cliente REST e token: `data/Api.kt`.
- Sessão móvel: `data/Profile.kt` e `CineMatchApplication.kt`.
- Senhas e autorização: `backend/app/auth.py`.
- Tabelas/FKs: `backend/app/models.py`.
- Recomendações: `backend/app/recommendations.py`.
- Testes: `backend/tests` e `android/app/src/androidTest`.

## Perguntas e respostas

**Por que cinco Activities?** A atividade exige cinco Activities reais. Cada uma tem uma responsabilidade e layout XML; a navegação utiliza Intents explícitas.

**Qual é a Intent implícita?** ACTION_SEND para compartilhar texto. O Android escolhe os aplicativos compatíveis; não definimos uma classe de destino.

**Onde fica a senha?** O backend guarda uma derivação PBKDF2 com salt individual, nunca a senha em texto. O Android mantém somente o token em memória durante o uso.

**Por que o histórico permanece se preciso entrar novamente?** Os registros pertencem à conta no banco. Descartar a sessão móvel não apaga o usuário nem suas avaliações.

**Outro usuário pode trocar o ID da URL e ver meus dados?** As rotas privadas comparam o ID com a identidade da sessão autenticada e recusam o acesso a outra conta.

**Onde os dados são armazenados?** No SQLite do backend, via SQLAlchemy. Retrofit faz a comunicação HTTP JSON; não há banco de avaliações no Android.

**É machine learning?** Não. É um algoritmo determinístico por gêneros, com pesos explicáveis, média e desempate por ID. Não apresenta uma porcentagem de precisão.

**Por que 10.0.2.2?** No emulador padrão, esse endereço acessa o computador host. Localhost dentro do dispositivo não representa o Windows.

**E se a OMDb estiver indisponível?** Os filmes já estão persistidos. A recomendação não depende de uma consulta externa. Imagens podem usar fallback e notas ausentes aparecem como N/D.

**Por que não publicar diretamente esta API em produção?** O ambiente usa HTTP de desenvolvimento local. Uma implantação externa precisa de HTTPS, operação de produção e políticas adicionais, como recuperação de conta e limitação de tentativas.
