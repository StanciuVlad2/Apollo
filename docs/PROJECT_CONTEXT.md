# Apollo — Project Context

Restaurant management system "Odin Restaurant".
Two repos in one workspace: **Apollo** (Spring Boot backend) + **Odin** (`../Odin/frontend/` — React 19 + Vite).

---

## Branch

`feature/migrateToMicroservices` — monolith migrated to microservices. Main branch is `main`.

---

## Repo Structure (Apollo/)

```
Apollo/
├── shared-lib/              ← Gradle library, published to mavenLocal, used by all services
├── eureka-server/           ← Spring Cloud Eureka, port 8761
├── api-gateway/             ← Spring Cloud Gateway, port 8080 (single entry point for frontend)
├── auth-service/            ← Auth + UserManagement, port 8081
├── operations-service/      ← Menu + Orders + Stock, port 8082
├── reservations-service/    ← Reservations + RestaurantTable, port 8083
├── feedback-service/        ← Feedback, port 8084
├── cocktails-service/       ← AI Cocktail generation (Gemini), port 8085
├── src/                     ← Original monolith (DO NOT DELETE — kept for reference)
├── docker-compose.yml       ← Full stack (all services + infra)
└── settings.gradle          ← Includes all 8 subprojects
```

---

## Port Map

| Service              | Port |
|----------------------|------|
| api-gateway          | 8080 |
| auth-service         | 8081 |
| operations-service   | 8082 |
| reservations-service | 8083 |
| feedback-service     | 8084 |
| cocktails-service    | 8085 |
| eureka-server        | 8761 |
| postgres             | 5432 |
| elasticsearch        | 9200 |
| kafka                | 9092 |

---

## Tech Stack

- **Java 21**, Spring Boot 3.5.6, Gradle 8 (multi-project)
- **Spring Cloud 2024.0.1**: Gateway, Eureka, OpenFeign
- **PostgreSQL 16** — single instance, one schema per service (`auth`, `operations`, `reservations`, `feedback`, `cocktails`)
- **Elasticsearch 8** — used by operations-service (menu_items, stock_items indices)
- **Kafka** (Confluent) — `order.completed` topic: operations-service → feedback-service
- **JJWT 0.12.6** — HMAC-SHA256 JWT, stateless
- **Lombok**, Spring Security, Spring Data JPA, Spring Mail, Thumbnailator, Vertex AI (Gemini)

---

## shared-lib (com.restaurant:shared-lib:1.0.0)

Published with: `./gradlew shared-lib:publishToMavenLocal`

| Class | Purpose |
|---|---|
| `com.restaurant.shared.security.TokenService` | JWT issue/validate. `issue(Long userId, String email, Set<String> roles)` |
| `com.restaurant.shared.security.TokenAuthFilter` | Servlet filter — validates Bearer token, sets `UserPrincipal` in `SecurityContextHolder` |
| `com.restaurant.shared.security.UserPrincipal` | `record(Long userId, String email, Set<String> roles)` — JWT principal |
| `com.restaurant.shared.security.UserHolder` | `static UserPrincipal getCurrentUser()` |
| `com.restaurant.shared.feign.JwtFeignInterceptor` | Feign interceptor — propagates `Authorization: Bearer` on inter-service calls |
| `com.restaurant.shared.dto.PageResponse<T>` | Generic paginated response |
| `com.restaurant.shared.dto.ErrorResponse` | Standardized error record with `of(status, error, message)` |
| `com.restaurant.shared.exception.GlobalExceptionHandler` | `@RestControllerAdvice` for all services |
| `com.restaurant.shared.config.BaseSecurityConfig` | Abstract security config — services extend and call `super.configure(http)` |

---

## Architecture Decisions

### JWT Propagation
- Frontend sends `Authorization: Bearer <jwt>` to api-gateway (:8080)
- Gateway validates JWT (using JJWT directly — WebFlux, not servlet) and forwards header unchanged
- Each service has `TokenAuthFilter` (from shared-lib) that re-validates JWT and sets `UserPrincipal` in `SecurityContextHolder`
- Inter-service Feign calls use `JwtFeignInterceptor` which copies the Bearer token from the current request
- `@PreAuthorize` works unchanged on all controllers

### api-gateway
- Uses WebFlux (reactive) — does NOT use shared-lib's servlet-based `TokenAuthFilter`
- Has inline `AuthGatewayFilter implements GlobalFilter` that validates JWT and returns 401 if invalid
- Public endpoints: `POST /api/auth/login`, `POST /api/auth/register`, `GET /api/auth/verify-email`, `GET /api/menu-items/**`, `GET /api/tables`, `POST /api/cocktails/generate`
- Routes via Eureka: `lb://auth-service`, `lb://operations-service`, etc.

### Database
- Single PostgreSQL instance, schema per service
- Each service's `application.yml`: `url: jdbc:postgresql://...?currentSchema=<schema>`
- `ddl-auto: update` in dev
- No JPA cross-service foreign keys — replaced with plain `Long userId`, `Long orderId` columns

### Kafka
- Topic: `order.completed`
- Producer: `operations-service` → publishes when order status → COMPLETED
- Consumer: `feedback-service` → stores in local `completable_orders(orderId, userId)` table
- This avoids feedback-service making synchronous HTTP call to operations-service to validate orders

### User references across services
- `Reservation.user` → replaced with `Long userId` (plain column)
- `Feedback.user` + `Feedback.order` → replaced with `Long userId`, `Long orderId`
- Services use `UserHolder.getCurrentUser().userId()` instead of querying UserRepository

---

## Key Package Paths

| Service | Main package |
|---|---|
| auth-service | `com.restaurant.auth` |
| operations-service | `com.restaurant.operations.{menu,orders,stock,kafka,config}` |
| reservations-service | `com.restaurant.reservations` |
| feedback-service | `com.restaurant.feedback` |
| cocktails-service | `com.restaurant.cocktails` |
| api-gateway | `com.restaurant.gateway` |
| eureka-server | `com.restaurant.eureka` |
| shared-lib | `com.restaurant.shared.{security,feign,dto,exception,config}` |

---

## Commands

```bash
# Build shared-lib (must run before building services)
./gradlew shared-lib:publishToMavenLocal

# Build all service jars
./gradlew bootJar

# Build a single service
./gradlew auth-service:bootJar

# Start full stack (Docker)
docker compose up --build -d -p odin-restaurant

# Start a single service independently
cd auth-service && docker compose up -d

# Eureka dashboard
open http://localhost:8761

# Frontend (Odin)
cd ../Odin/frontend && npm run dev   # localhost:5173
```

---

## Routing Table (api-gateway)

| Path prefix | Service |
|---|---|
| `/api/auth/**`, `/api/admin/users/**` | auth-service |
| `/api/menu-items/**`, `/api/orders/**`, `/api/stock/**` | operations-service |
| `/api/reservations/**`, `/api/tables/**` | reservations-service |
| `/api/feedback/**` | feedback-service |
| `/api/cocktails/**` | cocktails-service |

---

## Environment Variables (common to all services)

| Variable | Default | Purpose |
|---|---|---|
| `APP_TOKEN_SECRET` | `MySuperSecretJWTKeyThatIsAtLeast256BitsLong!!` | JWT signing key |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_USER` / `DB_PASS` | `restaurant` | DB credentials |
| `EUREKA_HOST` | `localhost` | Eureka server host |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Kafka (operations + feedback only) |
| `ES_HOST` | `localhost` | Elasticsearch (operations only) |
| `GEMINI_API_KEY` | — | Gemini API (cocktails only) |
| `MAIL_USER` / `MAIL_PASS` | Gmail credentials | Email (auth only) |

---

## Git Log (migration commits)

```
0f94da7 feat: add root docker-compose for full microservices stack
f9c9e08 feat(feedback-service): migrate Feedback with Kafka consumer and completable_orders
d1631b1 feat(reservations-service): migrate Reservations, denormalize User to userId
c2fafe2 feat(operations-service): migrate Menu+Orders+Stock with Kafka producer
926db18 feat(cocktails-service): migrate Cocktails module
f6bccbc feat(auth-service): migrate Auth + UserManagement modules
d60a93c feat(api-gateway): add Spring Cloud Gateway with JWT validation and routing
99babbc chore: fix .gitignore for Dockerfiles in subdirectories
c538302 feat(eureka-server): add Dockerfile and docker-compose
35cc517 feat(eureka-server): add Eureka service discovery server
3eeab9d feat(shared-lib): add JwtFeignInterceptor, DTOs, GlobalExceptionHandler, BaseSecurityConfig
d801ec8 feat(shared-lib): add TokenService, TokenAuthFilter, UserPrincipal, UserHolder
e7de71e chore: set up Gradle multi-project monorepo structure
```

---

## Spec & Plan (full details)

- Architecture spec: `docs/superpowers/specs/2026-04-26-microservices-migration-design.md`
- Implementation plan: `docs/superpowers/plans/2026-04-26-microservices-migration.md`
