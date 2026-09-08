# Trokr

Plataforma de trocas colaborativas: usuários cadastram itens ou habilidades
que oferecem e propõem trocas entre si, sem dinheiro envolvido.

> **"Trokr" é um nome de trabalho.** A turma vai batizar o projeto na Aula 1.
> O nome só aparece em lugares fáceis de trocar depois (pacote Java, nome do
> projeto Maven, nomes de containers/banco no Docker) — não está espalhado
> pela lógica de negócio.

> **Este repositório cobre apenas as Aulas 1 a 3.** Ele é intencionalmente
> simples: setup do projeto, arquitetura em camadas básica e CRUD de duas
> entidades. Padrões de projeto, tratamento de erros robusto, autenticação,
> eventos, créditos/reputação, MongoDB etc. ainda **não existem de propósito**
> — vão aparecer aula a aula ao longo do semestre, cada um resolvendo um
> problema que a turma vai sentir primeiro no código como está hoje.

## Stack e decisões da Aula 1

- **Java 21** + **Spring Boot 4.1.0**
- **Maven** (com Maven Wrapper, `mvnw`/`mvnw.cmd`) em vez de Gradle — escolhido
  por ter XML mais verboso porém mais fácil de ler linha a linha numa aula
  expositiva do que o DSL Groovy/Kotlin do Gradle, e por ser o build tool que
  a maioria dos tutoriais e da documentação oficial do Spring usa como
  exemplo padrão.
- **Spring Web, Spring Data JPA, PostgreSQL Driver, Spring Boot Actuator**
- **Bean Validation** (`spring-boot-starter-validation`) para validar os DTOs
  de entrada
- **Lombok** (opcional): usado só para reduzir boilerplate de getters/setters/
  construtores nas entidades (`@Getter`, `@Setter`, `@NoArgsConstructor`,
  `@AllArgsConstructor`). Não é foco da disciplina — se preferir, pode remover
  a dependência e escrever os métodos manualmente sem mudar nada da
  arquitetura.
- **Spring Boot DevTools** (opcional, escopo `runtime`): reinicia a aplicação
  sozinho quando as classes são recompiladas, agilizando o ciclo de
  desenvolvimento (ver ["Ciclo de desenvolvimento"](#ciclo-de-desenvolvimento)).
  Com `optional=true` no `pom.xml`, não entra no `.jar` final gerado pelo
  Docker, então não tem efeito em produção.
- **PostgreSQL** via Docker Compose

## Como subir o ambiente

Pré-requisito: Docker e Docker Compose instalados. Não é necessário ter Java
ou Maven instalados na máquina — tudo é compilado dentro do container.

```bash
docker compose up
```

Isso sobe dois containers:

- `trokr-db`: PostgreSQL 16, com um volume nomeado para persistir os dados
  entre reinicializações
- `trokr-app`: a aplicação Spring Boot, compilada a partir do código-fonte
  via Dockerfile multi-stage, na porta `8080`

Para derrubar o ambiente:

```bash
docker compose down
```

Para derrubar o ambiente **e apagar os dados do banco**:

```bash
docker compose down -v
```

## Ciclo de desenvolvimento

Como o código Java é compilado dentro do container, uma mudança no fonte só
aparece depois que a imagem é reconstruída. Para não ter que ficar rodando
`docker compose down` + `docker compose up --build` a cada alteração, use uma
das opções abaixo.

### Opção A — `docker compose watch` (recomendada, sem instalar Java)

```bash
docker compose watch
```

Não precisa rodar `docker compose up` antes: o `watch` já sobe `db` + `app` e
só então começa a observar `src/` e o `pom.xml` (bloco `develop.watch` no
`docker-compose.yml`). Ao salvar um arquivo, ele reconstrói a imagem do `app`
e recria o container automaticamente. O cache de camadas do Docker é
aproveitado — as dependências Maven já baixadas **não** são rebaixadas, então
sobra basicamente o `mvn package` (alguns segundos a algumas dezenas de
segundos). Requer Docker Compose 2.22 ou mais novo.

É só salvar o arquivo e esperar o container voltar.

> **Opcional — `docker compose up --watch`.** Faz o mesmo rebuild automático,
> mas com o comportamento do `up` de sempre: mostra os logs de **todos** os
> serviços (`db` e `app`, não só o `app`) e o `Ctrl+C` **derruba** os
> containers. Já o `docker compose watch` "cru" mostra só os logs dos serviços
> observados e, ao sair com `Ctrl+C`, **deixa os containers rodando** — nesse
> caso é preciso `docker compose down` depois. Use o que preferir; o resultado
> do hot reload é idêntico.

### Opção B — rodar a aplicação localmente com DevTools (mais rápido)

Se você tem Java 21 instalado (ou usa a IDE para rodar), suba só o banco no
Docker e a aplicação na máquina:

```bash
docker compose up db -d
./mvnw spring-boot:run
```

Com o **Spring Boot DevTools** no classpath, a aplicação reinicia sozinha
(1–2s) sempre que as classes são recompiladas — o `mvnw spring-boot:run`
recompila ao detectar mudança, e no IntelliJ basta acionar "Build Project"
(Ctrl+F9 / Cmd+F9).

Nesse caso, as variáveis de ambiente de conexão com o banco (`DB_HOST`,
`DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`) já têm valores padrão em
`application.yml` compatíveis com o `docker-compose.yml` rodando em
`localhost`.

## Estrutura de pastas

O projeto segue uma arquitetura em camadas simples — cada pacote tem uma
responsabilidade e conversa só com o pacote "abaixo" dele:

```
src/main/java/com/trokr/
├── controller/   # Recebe requisições HTTP, valida entrada, devolve DTOs
├── service/      # Regras de negócio e orquestração das operações
├── repository/   # Acesso a dados via Spring Data JPA (sem lógica própria)
├── model/        # Entidades JPA (representam as tabelas do banco)
├── dto/          # Objetos de entrada/saída da API (não expõem as entidades)
└── exception/    # Exceções da aplicação e tratamento delas
```

O fluxo de uma requisição é sempre:
`Controller → Service → Repository → Banco de dados`, e a resposta volta
pelo mesmo caminho, sendo convertida de entidade para DTO no Controller.

Decisões desta fase que valem explicar:

- **Sem interfaces para os Services** (`UsuarioService`, `ItemService` são
  classes concretas): como só existe uma implementação de cada, uma
  interface por cima só adicionaria indireção sem benefício real agora.
- **Repositories são só `extends JpaRepository`**, sem nenhuma abstração
  genérica por cima — o Spring Data JPA já resolve isso.
- **DTOs são `record`s do Java**, convertidos manualmente de/para entidade
  (métodos `fromEntity` nos DTOs de resposta). Sem MapStruct ou biblioteca
  de mapeamento.
- **`Item` é uma entidade genérica** — ainda não existe distinção entre
  produto físico, serviço ou aula, e não existe conceito de "Proposta de
  Troca" ligando dois itens. Isso é conteúdo de aulas futuras.
- **Relação `Item → Usuario` é unidirecional**: o item conhece seu dono, mas
  `Usuario` não guarda uma lista de itens. Evita decisões de cascade/fetch
  type que ainda não fazem sentido discutir nesta fase.
- **Tratamento de erro é só um `@ExceptionHandler` genérico** para "não
  encontrado", devolvendo 404. Erros de validação (`@Valid`) já viram 400
  automaticamente pelo Spring, sem código extra. Um tratamento de erros
  robusto e padronizado é conteúdo de uma aula futura.
- **`ddl-auto: update`** no Hibernate cria/ajusta as tabelas automaticamente
  a partir das entidades — é uma simplificação temporária para o início do
  curso, documentada como tal no `application.yml`. Será substituído por
  migrations versionadas (Flyway/Liquibase) quando controle de schema passar
  a importar.

## Endpoints disponíveis

Base URL local: `http://localhost:8080`

### Hello World

Endpoint mais simples possível, sem tocar em banco de dados — bom primeiro
teste no Postman/Insomnia para confirmar que a aplicação está no ar.

```
GET /hello
```

```
Hello, Trokr!
```

### Health check

```
GET /actuator/health
```

```json
{ "status": "UP" }
```

### Usuarios

| Método | Rota             | Descrição               |
|--------|------------------|--------------------------|
| GET    | `/usuarios`      | Lista todos os usuários |
| GET    | `/usuarios/{id}` | Busca um usuário por id |
| POST   | `/usuarios`      | Cria um usuário         |
| PUT    | `/usuarios/{id}` | Atualiza um usuário     |
| DELETE | `/usuarios/{id}` | Remove um usuário       |

**Request — `POST /usuarios`**

```json
{
  "nome": "Maria Silva",
  "email": "maria@example.com"
}
```

**Response — `201 Created`**

```json
{
  "id": 1,
  "nome": "Maria Silva",
  "email": "maria@example.com",
  "dataCriacao": "2026-08-02T21:18:54.056766"
}
```

### Itens

| Método | Rota          | Descrição            |
|--------|---------------|------------------------|
| GET    | `/itens`      | Lista todos os itens |
| GET    | `/itens/{id}` | Busca um item por id |
| POST   | `/itens`      | Cria um item          |
| PUT    | `/itens/{id}` | Atualiza um item      |
| DELETE | `/itens/{id}` | Remove um item        |

**Request — `POST /itens`**

```json
{
  "titulo": "Aula de violão",
  "descricao": "Aula básica de 1 hora, para iniciantes",
  "usuarioId": 1
}
```

**Response — `201 Created`**

```json
{
  "id": 1,
  "titulo": "Aula de violão",
  "descricao": "Aula básica de 1 hora, para iniciantes",
  "usuarioId": 1,
  "usuarioNome": "Maria Silva",
  "dataCriacao": "2026-08-02T21:19:06.836283"
}
```

**Erros comuns**

- `400 Bad Request`: dados de entrada inválidos (ex.: `email` fora do
  formato, campo obrigatório em branco)
- `404 Not Found`: id de `usuario`/`item` inexistente, incluindo quando
  `usuarioId` informado ao criar/atualizar um item não existe

```json
{ "erro": "Usuario não encontrado com id 999" }
```

## Testando a API

O arquivo `COLLECTION_INSOMNIA.json` na raiz do projeto tem as requisições
de exemplo acima prontas para importar no Insomnia (ou no Postman, que
também importa esse formato). Ele já vem com uma variável de ambiente
`base_url` apontando para `http://localhost:8080`.

## Testes automatizados

Nesta fase, só existe um teste de contexto do Spring Boot (confirma que a
aplicação sobe sem erros de configuração). Como ainda não configuramos um
banco em memória para os testes (fora de escopo por enquanto), o Postgres
precisa estar no ar para o contexto carregar:

```bash
docker compose up db -d
./mvnw test
```
