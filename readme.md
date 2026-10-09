# Shortener Link

Encurtador de links feito com **Java 21** e **Spring Boot**, com roteamento dinâmico via **Traefik** e ambiente completo orquestrado por **Docker Compose**.

## Funcionalidades

- Criar links curtos a partir de uma URL (`http` ou `https`)
- Listar, consultar e remover links
- Redirecionar (HTTP 302) usando apenas o `short_code` na URL
- Contagem de cliques, com incremento atômico direto no banco
- Validação de entrada (rejeita URLs vazias, sem protocolo ou com esquemas perigosos como `javascript:`)
- Respostas de erro padronizadas (`ProblemDetail`)
- Health check via Spring Actuator

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot (Web, Data JPA, Validation, Actuator) |
| Banco (produção) | PostgreSQL 17 |
| Banco (dev/testes) | H2 em memória |
| Reverse proxy | Traefik v3.6 |
| Containers | Docker e Docker Compose |
| Testes | JUnit 5, Mockito, AssertJ, MockMvc |

## Arquitetura

```
                    ┌─────────────────────────────┐
  Cliente ──:80──►  │           Traefik           │
                    │  /api/**        ─┐          │
                    │  /{7 caracteres} ┴► app     │
                    └─────────────────────────────┘
                                   │ rede "web"
                              ┌────▼────┐
                              │   app   │  Spring Boot (:8080)
                              └────┬────┘
                                   │ rede "internal" (sem acesso externo)
                              ┌────▼────┐
                              │   db    │  PostgreSQL
                              └─────────┘
```

- O Traefik descobre a aplicação pelas **labels** do Docker, sem arquivo de configuração de rotas. Subir ou remover containers atualiza as rotas automaticamente.
- Existem dois routers apontando para o mesmo service:
    - `/api/**` para o gerenciamento dos links
    - `/{shortCode}` (7 caracteres alfanuméricos) para o redirecionamento
- O `/actuator` **não** é exposto pelo Traefik. O healthcheck do Docker acessa a aplicação por dentro do container.
- O banco fica numa rede `internal` sem acesso à internet.

## Estrutura do projeto

```
shortener_link/
├── src/
│   ├── main/
│   │   ├── java/com/nall/shortener_link/
│   │   │   ├── ShortenerLinkApplication.java
│   │   │   ├── controllers/     # LinkController, RedirectController
│   │   │   ├── services/        # LinkService, ShortCodeGenerator
│   │   │   ├── repositories/    # LinkRepository
│   │   │   ├── model/           # Link (entidade)
│   │   │   ├── dto/             # CreateLinkRequest, LinkResponse
│   │   │   └── exception/       # LinkNotFoundException, GlobalExceptionHandler
│   │   └── resources/
│   │       ├── application.properties         # H2 (padrão)
│   │       └── application-prod.properties    # PostgreSQL (Docker)
│   └── test/                    # testes unitários e de integração
├── Dockerfile
├── docker-compose.yml
├── .env
└── pom.xml
```

## Pré-requisitos

- **JDK 21** (para rodar localmente e executar os testes)
- **Maven 3.9+**
- **Docker** e **Docker Compose** (para o ambiente completo)

Confira com `java -version` e `mvn -version`: o Maven precisa estar usando o JDK 21.

## Executando

### Opção 1: ambiente completo com Docker Compose

1. Crie o arquivo `.env` na raiz do projeto:

   ```env
   DOMAIN=localhost
   POSTGRES_DB=encurtador
   POSTGRES_USER=encurtador
   POSTGRES_PASSWORD=troque_esta_senha
   ```

2. Suba tudo:

   ```bash
   docker compose up -d --build
   docker compose ps          # app e db devem aparecer como "healthy"
   docker compose logs -f app
   ```

3. A aplicação responde na porta **80**, via Traefik.

Para derrubar:

```bash
docker compose down        # mantém os dados do banco
docker compose down -v     # apaga também o volume do PostgreSQL
```

### Opção 2: local, sem Docker (H2 em memória)

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. O console do H2 fica em `http://localhost:8080/h2-console`.

> No modo local os links são perdidos ao reiniciar, pois o H2 roda em memória.

## Uso da API

Nos exemplos abaixo, use `http://localhost` (Docker/Traefik) ou `http://localhost:8080` (execução local).

| Método | Rota | Descrição | Resposta |
|---|---|---|---|
| `POST` | `/api/links` | Cria um link curto | `201` |
| `GET` | `/api/links` | Lista todos os links | `200` |
| `GET` | `/api/links/{shortCode}` | Consulta um link | `200` / `404` |
| `DELETE` | `/api/links/{shortCode}` | Remove um link | `204` / `404` |
| `GET` | `/{shortCode}` | Redireciona para a URL original | `302` / `404` |

### Exemplos

```bash
# Criar
curl -X POST http://localhost/api/links \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.google.com"}'
```

```json
{
  "shortCode": "ZUBXL6o",
  "shortUrl": "http://localhost/ZUBXL6o",
  "originalUrl": "https://www.google.com",
  "createdAt": "2026-10-09T12:44:07.035391422Z",
  "clickCount": 0
}
```

```bash
# Redirecionar (-i mostra o 302 e o header Location)
curl -i http://localhost/ZUBXL6o

# Listar
curl http://localhost/api/links

# Consultar (clickCount aumenta a cada redirecionamento)
curl http://localhost/api/links/ZUBXL6o

# Remover
curl -X DELETE http://localhost/api/links/ZUBXL6o
```

### Erros

| Situação | Status |
|---|---|
| URL vazia, sem `http://`/`https://` ou maior que 2048 caracteres | `400` |
| `short_code` inexistente | `404` |
| Caminho fora do padrão de 7 caracteres (ex.: `/abc`) | `404` |

## Decisões de projeto

- **`short_code` com 7 caracteres em Base62**: cerca de 3,5 trilhões de combinações, gerado com `SecureRandom`. Em caso de colisão, o service tenta de novo (até 5 vezes). O tamanho fixo também evita conflito com rotas como `/api` e `/actuator`.
- **302 em vez de 301**: o 301 é cacheado pelo navegador, e os cliques deixariam de passar pela aplicação e de ser contados.
- **Incremento atômico de cliques** (`click_count = click_count + 1` no banco): evita perder contagens em acessos simultâneos.
- **Regex `{7}` na rota de redirecionamento**: requisições como `/favicon.ico` ou `/robots.txt` não são tratadas como links.
- **Apenas `http` e `https`** são aceitos, o que impede cadastrar `javascript:` e similares.

## Testes

```bash
mvn test                           # todos os testes
mvn test -Dtest=LinkServiceTest    # uma classe específica
```

| Classe | Tipo | O que valida |
|---|---|---|
| `ShortCodeGeneratorTest` | Unitário | Formato e unicidade dos códigos |
| `LinkServiceTest` | Unitário (Mockito) | Criação, colisão, busca, exclusão e contagem |
| `LinkRepositoryTest` | `@DataJpaTest` | Queries, incremento de cliques e constraint única |
| `LinkApiIntegrationTest` | `@SpringBootTest` + MockMvc | Endpoints de ponta a ponta |

Os testes usam H2, então rodam sem Docker. O `Dockerfile` compila com `-DskipTests`, então rode `mvn test` antes de gerar a imagem.

## Acessando o PostgreSQL pelo pgAdmin

Por padrão o banco não tem porta publicada. Para acessá-lo pela sua máquina, publique a porta **apenas no localhost** e adicione uma rede sem `internal: true`, já que redes internas não permitem publicar portas:

```yaml
  db:
    # ...
    ports:
      - "127.0.0.1:5432:5432"
    networks:
      - internal
      - dbaccess

networks:
  dbaccess:
    name: encurtador_dbaccess
```

No pgAdmin (**Register → Server → Connection**):

- Host: `localhost`
- Port: `5432` (use outra, como `5433`, se já houver um Postgres local)
- Database, usuário e senha: os valores do `.env`

> Nunca publique a 5432 em `0.0.0.0` num servidor real. Para acesso remoto, use um túnel SSH.

## Escalando a aplicação

O Traefik balanceia a carga entre as réplicas automaticamente:

```bash
docker compose up -d --scale app=3
```