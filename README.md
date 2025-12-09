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

Clean Architecture (Uncle Bob) com separação clara entre camadas.

```
src/main/java/br/com/labs/
├── domain/              # Entities e regras de negócio puras
├── application/         # Use Cases (Application Business Rules)
└── infrastructure/      # Frameworks & Drivers
    ├── config/          # Configurações Spring
    ├── persistence/     # Adapters de saída (JPA, Redis)
    │   ├── jpa/
    │   └── redis/
    ├── email/           # Adapter de saída (SMTP)
    ├── security/        # JWT, BCrypt, Filters
    └── web/             # Adapter de entrada (HTTP)
        ├── controller/
        ├── dto/
        └── exception/
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
- [x] Registro de usuários
- [x] Login com username/password
- [x] Geração e envio de código MFA por email
- [x] Código MFA com TTL de 5 minutos (Redis)
- [x] Verificação do código MFA
- [x] Geração de JWT (Access Token + Refresh Token)
- [x] Refresh Token armazenado no Redis (whitelist)
- [x] Rate limiting: bloqueio após 3 tentativas erradas de MFA (15 min)
- [x] Logout com invalidação de tokens
- [x] Blacklist de Access Tokens revogados

---

## Roadmap

### Fase 1: Setup Inicial ✅
- [x] Configurar `pom.xml` com dependências
- [x] Criar `docker-compose.yaml` (PostgreSQL, Redis, Mailhog)
- [x] Configurar `application.yaml`
- [x] Configurar Virtual Threads para async

### Fase 2: Domain Layer ✅
- [x] Criar Value Objects (`UserId`, `Email`, `Username`, `Password`, `MfaCode`, `TokenPair`, `MfaToken`)
- [x] Criar Entity `User`
- [x] Criar Sealed Class `DomainException` e exceções específicas
- [x] Criar Ports (interfaces dos repositories)

### Fase 3: Infrastructure - Persistência ✅
- [x] Configurar Flyway e criar migration da tabela `users`
- [x] Implementar `UserJpaEntity` e `UserJpaRepository`
- [x] Implementar `UserRepositoryAdapter`
- [x] Configurar Redis e implementar `MfaRedisRepository`
- [x] Implementar `TokenRedisRepository`

### Fase 4: Infrastructure - Security ✅
- [x] Implementar `JwtTokenProvider`
- [x] Implementar `BCryptPasswordEncoder`
- [x] Configurar `SecurityConfig`
- [x] Implementar `JwtAuthenticationFilter`

### Fase 5: Infrastructure - Email ✅
- [x] Implementar `SmtpEmailSender`
- [x] Criar template de email para código MFA

### Fase 6: Application Layer - Use Cases ✅
- [x] Implementar `RegisterUserUseCase`
- [x] Implementar `AuthenticateUserUseCase`
- [x] Implementar `VerifyMfaCodeUseCase`
- [x] Implementar `RefreshTokenUseCase`
- [x] Implementar `LogoutUseCase`

### Fase 7: Web Layer ✅
- [x] Criar Request DTOs com validação
- [x] Implementar `AuthController`
- [x] Implementar `GlobalExceptionHandler` (RFC 7807)

### Fase 8: Testes ✅
- [x] Testes unitários dos Use Cases
- [x] Testes de integração com Testcontainers
- [x] Testes dos endpoints

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
