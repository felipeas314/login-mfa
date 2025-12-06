# Login MFA

Projeto de estudo para implementação de autenticação com Multi-Factor Authentication (MFA) usando Spring Boot 3.4, Java 21, PostgreSQL e Redis.

## Tecnologias

| Tecnologia | Versão | Uso |
|------------|--------|-----|
| Java | 21 | Records, Sealed Classes, Pattern Matching, Virtual Threads |
| Spring Boot | 3.4.0 | Framework principal |
| PostgreSQL | 17 | Persistência de usuários |
| Redis | 7 | MFA codes (TTL), Refresh tokens, Blacklist |
| Flyway | - | Migrations |
| Docker Compose | - | Ambiente de desenvolvimento |

## Arquitetura

Hexagonal Architecture (Ports & Adapters) com separação clara entre domínio, aplicação e infraestrutura.

```
src/main/java/br/com/labs/
├── domain/           # Regras de negócio puras
├── application/      # Casos de uso
├── infrastructure/   # Adapters (JPA, Redis, Email, JWT)
└── web/              # Controllers e DTOs da API
```

## Fluxo de Autenticação

```
1. POST /auth/register    → Cadastra usuário
2. POST /auth/login       → Valida credenciais → Envia código MFA por email
3. POST /auth/verify      → Valida código MFA → Retorna Access + Refresh tokens
4. POST /auth/refresh     → Renova tokens
5. POST /auth/logout      → Invalida tokens
```

## Funcionalidades

- [x] Modelagem da arquitetura
- [ ] Registro de usuários
- [ ] Login com username/password
- [ ] Geração e envio de código MFA por email
- [ ] Código MFA com TTL de 5 minutos (Redis)
- [ ] Verificação do código MFA
- [ ] Geração de JWT (Access Token + Refresh Token)
- [ ] Refresh Token armazenado no Redis (whitelist)
- [ ] Rate limiting: bloqueio após 3 tentativas erradas de MFA (15 min)
- [ ] Logout com invalidação de tokens
- [ ] Blacklist de Access Tokens revogados

---

## Roadmap

### Fase 1: Setup Inicial
- [ ] Configurar `pom.xml` com dependências
- [ ] Criar `docker-compose.yaml` (PostgreSQL, Redis, Mailhog)
- [ ] Configurar `application.yaml`
- [ ] Configurar Virtual Threads para async

### Fase 2: Domain Layer
- [ ] Criar Value Objects (`UserId`, `Email`, `Password`, `MfaCode`)
- [ ] Criar Entity `User`
- [ ] Criar Sealed Class `DomainException` e exceções específicas
- [ ] Criar Ports (interfaces dos repositories)

### Fase 3: Infrastructure - Persistência
- [ ] Configurar Flyway e criar migration da tabela `users`
- [ ] Implementar `UserJpaEntity` e `UserJpaRepository`
- [ ] Implementar `UserRepositoryAdapter`
- [ ] Configurar Redis e implementar `MfaRedisRepository`
- [ ] Implementar `TokenRedisRepository`

### Fase 4: Infrastructure - Security
- [ ] Implementar `JwtTokenProvider`
- [ ] Implementar `BCryptPasswordEncoder`
- [ ] Configurar `SecurityConfig`
- [ ] Implementar `JwtAuthenticationFilter`

### Fase 5: Infrastructure - Email
- [ ] Implementar `SmtpEmailSender`
- [ ] Criar template de email para código MFA

### Fase 6: Application Layer - Use Cases
- [ ] Implementar `RegisterUserUseCase`
- [ ] Implementar `AuthenticateUserUseCase`
- [ ] Implementar `VerifyMfaCodeUseCase`
- [ ] Implementar `RefreshTokenUseCase`
- [ ] Implementar `LogoutUseCase`

### Fase 7: Web Layer
- [ ] Criar Request DTOs com validação
- [ ] Implementar `AuthController`
- [ ] Implementar `GlobalExceptionHandler` (RFC 7807)

### Fase 8: Testes
- [ ] Testes unitários dos Use Cases
- [ ] Testes de integração com Testcontainers
- [ ] Testes dos endpoints

### Fase 9: Melhorias (opcional)
- [ ] Documentação com OpenAPI/Swagger
- [ ] Health checks
- [ ] Métricas com Micrometer
- [ ] Rate limiting global com Bucket4j

---

## Como Executar

### Pré-requisitos
- Java 21
- Docker e Docker Compose

### Subir infraestrutura
```bash
docker compose up -d
```

### Executar aplicação
```bash
./mvnw spring-boot:run
```

### Acessar Mailhog (emails de teste)
```
http://localhost:8025
```

---

## Endpoints

| Método | Endpoint | Descrição | Auth |
|--------|----------|-----------|------|
| POST | `/api/v1/auth/register` | Cadastro de usuário | Não |
| POST | `/api/v1/auth/login` | Login inicial | Não |
| POST | `/api/v1/auth/verify` | Verifica código MFA | Não |
| POST | `/api/v1/auth/refresh` | Renova tokens | Não |
| POST | `/api/v1/auth/logout` | Invalida tokens | Sim |

---

## Redis Keys

| Key Pattern | Descrição | TTL |
|-------------|-----------|-----|
| `mfa:code:{userId}` | Código MFA pendente | 5 min |
| `mfa:block:{userId}` | Bloqueio após tentativas | 15 min |
| `refresh:{tokenId}` | Refresh token válido | 7 dias |
| `blacklist:{jti}` | Access token revogado | Tempo restante do token |

---

## Licença

Projeto de estudo - uso livre.
