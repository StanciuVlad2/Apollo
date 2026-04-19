# Apollo — Backend Spring Boot

Backend-ul sistemului de management al restaurantului "Odin Restaurant".

**Stack:** Java 21, Spring Boot 3.5.6, Gradle, PostgreSQL 16, Elasticsearch 8, Google Gemini AI

---

## Pornire rapida

### Cu Docker (recomandat)

```bash
docker-compose up -d
```

## Comenzi Gradle

```bash
gradle clean build                                          # Build complet cu teste
gradle bootRun                                              # Pornire server
```

---

## Configurare

Fișierul principal: `src/main/resources/application.yml`

| Proprietate | Valoare implicită |
|---|---|
| PostgreSQL | `localhost:5432/restaurant` (user/pass: `restaurant`) |
| Elasticsearch | `http://localhost:9200` |
| Token TTL | 30 minute |
| Upload imagini | `uploads/menu-images/` |
| Gemini API Key | variabila de mediu `GEMINI_API_KEY` |

---

## Module

| Modul | Descriere |
|---|---|
| `Auth` | JWT (HMAC-SHA256) stateless via JJWT. Token-urile nu sunt stocate în DB. Verificare email prin UUID token cu expirare 24h. |
| `Menu` | Articole din meniu stocate în Elasticsearch, upload imagini |
| `Reservations` | Rezervări cu sloturi fixe de 2 ore (10-22) |
| `Orders` | Procesare comenzi (PENDING / COMPLETED / CANCELLED) |
| `Stock` | Gestiune stocuri (SOLID, LIQUID, PORTION) |
| `Cocktails` | Generare rețete AI via Google Gemini |
| `Feedback` | Recenzii și rating-uri clienți |
| `UserManagement` | CRUD utilizatori și roluri |

---

## API Documentation

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
