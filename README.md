# Odin Restaurant — Sistem de Management al Restaurantului

Aplicație full-stack pentru gestionarea integrată a unui restaurant, cu arhitectură de microservicii backend și interfață modernă React.

---

## Cuprins

1. [Descrierea proiectului](#1-descrierea-proiectului)
2. [Tehnologii utilizate](#2-tehnologii-utilizate)
3. [Arhitectura sistemului](#3-arhitectura-sistemului)
4. [Model de date](#4-model-de-date)
5. [Diagrame use case](#5-diagrame-use-case)
6. [Diagrame de secvență — flow-uri importante](#6-diagrame-de-secventa--flow-uri-importante)
7. [Diagrama claselor principale](#7-diagrama-claselor-principale)
8. [Pornirea aplicației](#8-pornirea-aplicatiei)
9. [Roluri și acces](#9-roluri-si-acces)
10. [Funcționalități implementate și roadmap](#10-functionalitatii-implementate-si-roadmap)

---

## 1. Descrierea proiectului

**Odin Restaurant** este un sistem complet de management pentru restaurante, care acoperă întregul flux operațional: de la autentificarea angajaților și gestionarea meniului, până la preluarea comenzilor, rezervări, administrarea stocurilor, generarea rapoartelor și recenzii post-comandă.

Proiectul este compus din două aplicații separate:

- **Apollo** — backend bazat pe microservicii Spring Boot (Java 21)
- **Odin** — frontend SPA React 19 + TypeScript

Elementul distinctiv al proiectului este integrarea AI pentru generarea de cocktail-uri personalizate folosind Google Gemini 2.5 Flash, și sistemul de voucher-e automat generat la finalizarea comenzilor.

---

## 2. Tehnologii utilizate

### Backend (Apollo)
| Categorie | Tehnologie |
|-----------|-----------|
| Limbaj | Java 21 |
| Framework | Spring Boot 3.4.x, Spring Cloud 2024.0.x |
| Build | Gradle (multi-project) |
| Service discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway |
| Baze de date | PostgreSQL 16 (date relaționale), Elasticsearch 8.x (meniu, stocuri) |
| Mesagerie asincronă | Apache Kafka + Zookeeper |
| Autentificare | JWT (HMAC-SHA256), stateless |
| Email | SMTP (Spring Mail) |
| AI | Google Gemini 2.5 Flash API |
| Containerizare | Docker, Docker Compose |
| Documentație API | SpringDoc OpenAPI (Swagger) |

### Frontend (Odin)
| Categorie | Tehnologie |
|-----------|-----------|
| Limbaj | TypeScript |
| Framework | React 19 |
| Build tool | Vite |
| Routing | React Router 6 |
| Stilizare | CSS modules |
| HTTP client | Fetch API (singleton ApiService) |

---

## 3. Arhitectura sistemului

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT                                  │
│                 React 19 SPA (port 5173)                        │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTP / REST
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    API GATEWAY (port 8080)                       │
│              Spring Cloud Gateway — rutare + JWT forward         │
└──┬────────┬────────┬────────┬────────┬────────┬────────┬────────┘
   │        │        │        │        │        │        │
   ▼        ▼        ▼        ▼        ▼        ▼        ▼
auth    operations reserv. feedback cocktails reports  settings
:8081    :8082     :8083    :8084    :8085     :8086    :8088
   │        │                                          vouchers
   │        │                                          :8089(*)
   │        │ Kafka (async)
   │        ▼
   │  notifications-service :8087
   │  (trimite email-uri SMTP)
   │
   └──► Eureka Server :8761
        (service discovery)

Storage:
  PostgreSQL 16 :5432   ← auth, reservations, orders, feedback, vouchers
  Elasticsearch 8 :9200 ← menu items, recipe ingredients, stock items
  Kafka :9092           ← order events → notifications
```

Toate microserviciile se înregistrează în **Eureka Server**. Comunicarea sincronă inter-servicii se realizează prin **Feign Clients** definiți în `shared-lib`. Comunicarea asincronă (ex: comandă plasată → email de confirmare) se face prin **Kafka**.

---

## 4. Model de date

### Diagrama entitate-relație (PostgreSQL)

```
┌──────────────────┐         ┌───────────────────────┐
│      users       │         │  email_verification_  │
├──────────────────┤         │       tokens          │
│ id (PK)          │◄────────├───────────────────────┤
│ email (UNIQUE)   │         │ id (PK)               │
│ password         │         │ user_id (FK→users)    │
│ email_verified   │         │ token (UUID)          │
│ created_at       │         │ expires_at            │
└────────┬─────────┘         └───────────────────────┘
         │ user_roles (join)
         │ ┌─────────┐
         └─┤  role   │
           └─────────┘

┌──────────────────┐         ┌──────────────────────┐
│ restaurant_tables│         │    reservations      │
├──────────────────┤         ├──────────────────────┤
│ id (PK)          │◄────────│ id (PK)              │
│ table_number     │  1    N │ table_id (FK)        │
│ capacity         │         │ user_id              │
│ x, y (position)  │         │ customer_name        │
│ width, height    │         │ customer_phone       │
│ active           │         │ customer_email       │
└──────────────────┘         │ party_size           │
                             │ reservation_date     │
                             │ start_time           │
                             │ end_time             │
                             │ status (CONFIRMED /  │
                             │  CANCELLED /         │
                             │  COMPLETED / NO_SHOW)│
                             │ notes                │
                             │ cancel_reason        │
                             │ created_at           │
                             └──────────────────────┘

┌──────────────────┐         ┌──────────────────────┐
│      orders      │         │    order_items       │
├──────────────────┤         ├──────────────────────┤
│ id (PK)          │◄────────│ id (PK)              │
│ table_id         │  1    N │ order_id (FK)        │
│ user_id          │         │ menu_item_id         │
│ customer_email   │         │ menu_item_name       │
│ reservation_id   │         │ quantity             │
│ status (PENDING/ │         │ unit_price           │
│  READY / BILLED /│         └──────────────────────┘
│  COMPLETED /     │
│  CANCELLED)      │
│ notes            │
│ created_at       │
│ updated_at       │
└──────────────────┘

┌──────────────────┐         ┌──────────────────────┐
│     feedback     │         │  completable_orders  │
├──────────────────┤         ├──────────────────────┤
│ id (PK)          │         │ order_id (PK)        │
│ order_id (UNIQUE)│         │ user_id              │
│ user_id          │         │ completed_at         │
│ food_quality_    │         └──────────────────────┘
│   rating         │
│ service_speed_   │
│   rating         │
│ would_recommend  │
│ comment          │
│ created_at       │
└──────────────────┘

┌──────────────────┐
│    vouchers      │
├──────────────────┤
│ id (PK)          │
│ code (UNIQUE,12) │
│ user_id          │
│ value (decimal)  │
│ expiry_date      │
│ used (boolean)   │
│ order_id (UNIQUE)│
│ created_at       │
└──────────────────┘
```

### Elasticsearch (non-relational)

```
Index: menu_items          Index: stock_items
───────────────────        ──────────────────
id                         id
name                       name
description                quantity (Double)
price                      unit
category                   minimum_threshold
image_url                  type (SOLID/LIQUID/PORTION)
available
recipe_ingredients[]
  └── stock_item_id
  └── quantity_required
  └── unit
```

---

## 5. Diagrame use case

### UC1 — Client (GUEST)

```
                        ┌─────────────────────────────────┐
                        │          Sistem Odin             │
                        │                                  │
         ┌──────┐       │  ╔══════════════════════╗       │
         │      │──────────║  Înregistrare cont   ║       │
         │      │       │  ╚══════════════════════╝       │
         │      │──────────║  Verificare email    ║       │
         │      │       │  ╚══════════════════════╝       │
         │GUEST │──────────║  Autentificare (JWT) ║       │
         │      │       │  ╚══════════════════════╝       │
         │      │──────────║  Vizualizare meniu   ║       │
         │      │       │  ╚══════════════════════╝       │
         │      │──────────║  Plasare comandă     ║       │
         │      │       │  ╚══════════════════════╝       │
         │      │──────────║  Rezervare masă      ║       │
         │      │       │  ╚══════════════════════╝       │
         │      │──────────║  Lăsare feedback     ║       │
         │      │       │  ╚══════════════════════╝       │
         │      │──────────║  Vizualizare voucher ║       │
         └──────┘       │  ╚══════════════════════╝       │
                        │  ╔══════════════════════╗       │
                        │  ║  Cocktail AI (Gemini)║       │
                        │  ╚══════════════════════╝       │
                        └─────────────────────────────────┘
```

### UC2 — Angajați (WAITER / CHEF / MANAGER / ADMIN)

```
                        ┌──────────────────────────────────────────┐
                        │               Sistem Odin                │
                        │                                          │
  ┌────────┐            │  ╔══════════════════════════╗           │
  │ WAITER │────────────║  Gestionare comenzi active  ║           │
  └────────┘            │  ╚══════════════════════════╝           │
  ┌────────┐            │  ╔══════════════════════════╗           │
  │  CHEF  │────────────║  Vizualizare comenzi/rețete ║           │
  └────────┘            │  ╚══════════════════════════╝           │
  ┌─────────┐           │  ╔══════════════════════════╗           │
  │ MANAGER │───────────║  Gestionare mese (floor map)║           │
  └─────────┘           │  ╚══════════════════════════╝           │
       │                │  ╔══════════════════════════╗           │
       └────────────────║  Administrare stocuri       ║           │
                        │  ╚══════════════════════════╝           │
                        │  ╔══════════════════════════╗           │
       ┌────────────────║  Rapoarte & analize          ║           │
       │                │  ╚══════════════════════════╝           │
       │                │  ╔══════════════════════════╗           │
  ┌─────────┐           │  ║  Setări restaurant       ║           │
  │  ADMIN  │───────────║  ╚══════════════════════════╝           │
  └─────────┘           │  ╔══════════════════════════╗           │
       └────────────────║  Gestionare utilizatori     ║           │
                        │  ╚══════════════════════════╝           │
                        └──────────────────────────────────────────┘
```

---

## 6. Diagrame de secventa — flow-uri importante

### SD1 — Înregistrare și verificare email

```
Client       Frontend      API Gateway    auth-service    notifications-service
  │              │               │              │                │
  │──POST /register──────────────►              │                │
  │              │               │──────────────►                │
  │              │               │   creare User (GUEST,         │
  │              │               │   emailVerified=false)        │
  │              │               │   creare EmailVerificationToken│
  │              │               │              │──Kafka topic──►│
  │              │               │              │  "send-verification-email"│
  │              │               │◄─────────────│                │
  │◄─201 Created─│               │              │   trimite email SMTP
  │              │               │              │   cu link UUID │
  │              │               │              │                │
  │──click link email────────────────────────────────────────────│
  │──GET /verify-email?token=UUID────────────────►               │
  │              │               │   token valid?│               │
  │              │               │   emailVerified=true          │
  │              │               │◄─────────────│                │
  │◄─200 OK──────│               │              │                │
```

### SD2 — Autentificare și acces la resurse protejate

```
Client         Frontend       API Gateway     auth-service    orice-service
  │                │               │               │               │
  │──POST /api/auth/login──────────►               │               │
  │                │               │───────────────►               │
  │                │               │   validare credențiale        │
  │                │               │   generare JWT (sub,roles,    │
  │                │               │   userId, exp)                │
  │                │               │◄──────────────│               │
  │◄──JWT token────│               │               │               │
  │  (localStorage)│               │               │               │
  │                │               │               │               │
  │──GET /api/... (Bearer JWT)──────►               │               │
  │                │               │  TokenAuthFilter               │
  │                │               │  validare semnătură HMAC      │
  │                │               │  extrage claims               │
  │                │               │───────────────────────────────►
  │                │               │   request cu X-User-Id header  │
  │◄──200 + date───│               │◄──────────────────────────────│
```

### SD3 — Plasare comandă și notificare asincronă

```
Client      Frontend    API Gateway  operations-service   Kafka   notifications-service
  │             │            │              │               │              │
  │─POST /orders────────────►              │               │              │
  │             │            │─────────────►               │              │
  │             │            │  creare Order (PENDING)     │              │
  │             │            │  scădere stoc automat       │              │
  │             │            │  publish OrderPlacedEvent──►│              │
  │             │            │◄─────────────│              │──────────────►
  │◄─201 Order──│            │              │              │  trimite email│
  │             │            │              │              │  confirmare   │
  │             │            │              │              │  comandă      │
  │             │            │              │              │              │
  │─PUT /orders/{id}/status (READY)────────►               │              │
  │             │            │  status→READY               │              │
  │             │            │  publish OrderReadyEvent───►│              │
  │             │            │◄─────────────│              │──────────────►
  │             │            │              │              │  notificare   │
  │             │            │              │              │  client email │
```

### SD4 — Finalizare comandă și generare voucher

```
Client(WAITER)   operations-service   vouchers-service   Client(GUEST)
     │                  │                   │                  │
     │──PUT /orders/{id}/status (COMPLETED)─►                  │
     │                  │  status→COMPLETED  │                  │
     │                  │  publish OrderCompletedEvent         │
     │                  │───────────────────►│                  │
     │                  │                   │ creare Voucher    │
     │                  │                   │ (cod unic 12 car, │
     │                  │                   │  valoare, expiry) │
     │                  │                   │──email voucher───►│
     │◄─200──────────────│                   │                  │
```

### SD5 — Rezervare masă

```
Client       Frontend      API Gateway   reservations-service
  │              │               │              │
  │──GET /tables/available?date=X&partySize=N───►
  │              │               │──────────────►
  │              │               │  caută mese disponibile
  │              │               │  pentru data și slot
  │◄─lista mese──│               │◄─────────────│
  │              │               │              │
  │──POST /reservations──────────►              │
  │    {tableId, date, startTime, partySize}    │
  │              │               │──────────────►
  │              │               │  rezolvă TimeSlot enum
  │              │               │  setează endTime (startTime+2h)
  │              │               │  verifică conflict
  │              │               │  creare Reservation(CONFIRMED)
  │◄─201 rezervare│              │◄─────────────│
```

### SD6 — Generare cocktail cu AI

```
Client       Frontend     API Gateway    cocktails-service    Gemini API
  │              │              │              │                   │
  │──POST /cocktails/generate───►             │                   │
  │   {ingredients[], preferences}            │                   │
  │              │              │─────────────►                   │
  │              │              │  construiește prompt structurat  │
  │              │              │──────────────────────────────────►
  │              │              │                   Gemini 2.5 Flash
  │              │              │                   generează rețetă
  │              │              │◄─────────────────────────────────│
  │              │              │  parsează răspuns JSON           │
  │◄─rețetă AI───│              │◄─────────────│                   │
```

### SD7 — Import stocuri din CSV

```
Manager(Frontend)   API Gateway    operations-service         Elasticsearch
       │                 │               │                         │
       │ selectează fișier CSV           │                         │
       │ (coloane: name, quantity,       │                         │
       │  unit, type, minimumThreshold)  │                         │
       │                 │               │                         │
       │──POST /api/stock/import─────────►                         │
       │   multipart/form-data           │                         │
       │   (Bearer JWT — MANAGER/ADMIN)  │                         │
       │                 │               │                         │
       │                 │  StockItemService.importCsv()           │
       │                 │               │                         │
       │                 │               │  parsare CSV (Apache Commons CSV)
       │                 │               │  skip header row        │
       │                 │               │                         │
       │                 │               │  pentru fiecare rând:   │
       │                 │               │  ┌─────────────────────────────────┐
       │                 │               │  │ validare câmpuri:               │
       │                 │               │  │  • name nenul                   │
       │                 │               │  │  • type ∈ {SOLID,LIQUID,PORTION}│
       │                 │               │  │  • unit compatibil cu type:     │
       │                 │               │  │    SOLID  → g / kg              │
       │                 │               │  │    LIQUID → ml / liters         │
       │                 │               │  │    PORTION→ pieces / portions   │
       │                 │               │  │  • quantity > 0                 │
       │                 │               │  │  • minimumThreshold > 0 (opț.)  │
       │                 │               │  └─────────────────────────────────┘
       │                 │               │         │
       │                 │               │  eroare validare?
       │                 │               │    da → RowError(rowNumber, mesaj)
       │                 │               │         failed++; skip rând
       │                 │               │         │
       │                 │               │  caută name în Elasticsearch ──────►
       │                 │               │◄────────────────────────────────────│
       │                 │               │  există?
       │                 │               │    da  → UPDATE (quantity, unit,    │
       │                 │               │           type, threshold)          │
       │                 │               │           updated++       ──────────►
       │                 │               │    nu  → INSERT nou item  ──────────►
       │                 │               │           created++                 │
       │                 │               │◄───────────────────────────────────│
       │                 │               │                         │
       │◄─200 StockImportResult──────────│                         │
       │  { created, updated,            │                         │
       │    failed,                      │                         │
       │    errors: [{row, message}] }   │                         │
       │                 │               │                         │
       │  frontend afișează raport:      │                         │
       │  "X create, Y actualizate,      │                         │
       │   Z eșuate" + lista erorilor    │                         │
       │  pe număr de rând               │                         │
```

**Format CSV acceptat:**

```csv
name,quantity,unit,type,minimumThreshold
rosii,10,kg,SOLID,2
ulei masline,5,liters,LIQUID,1
oua,48,pieces,PORTION,12
```

---

## 7. Diagrama claselor principale

```
┌─────────────────────────────────────────────────────────────────┐
│                         auth-service                            │
│                                                                 │
│  ┌──────────┐      ┌────────────────────┐                      │
│  │   User   │      │EmailVerification   │                      │
│  ├──────────┤      │Token               │                      │
│  │id: Long  │◄─────├────────────────────┤                      │
│  │email     │      │id: Long            │                      │
│  │password  │      │user: User          │                      │
│  │roles:Set │      │token: String(UUID) │                      │
│  │emailVer. │      │expiresAt: LDT      │                      │
│  │createdAt │      └────────────────────┘                      │
│  └──────────┘                                                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      reservations-service                       │
│                                                                 │
│  ┌────────────────┐         ┌─────────────────────┐           │
│  │RestaurantTable │         │     Reservation     │           │
│  ├────────────────┤         ├─────────────────────┤           │
│  │id: Long        │◄────────│id: Long             │           │
│  │tableNumber     │  1    N │table: RestaurantTable│          │
│  │capacity: Int   │         │userId: Long         │           │
│  │x, y: Double    │         │customerName         │           │
│  │width, height   │         │customerPhone        │           │
│  │active: boolean │         │partySize: Int       │           │
│  └────────────────┘         │reservationDate      │           │
│                             │startTime / endTime  │           │
│                             │status: Enum         │           │
│                             │  CONFIRMED          │           │
│                             │  CANCELLED          │           │
│                             │  COMPLETED          │           │
│                             │  NO_SHOW            │           │
│                             └─────────────────────┘           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       operations-service                        │
│                                                                 │
│  ┌──────────┐  1  N  ┌───────────┐                            │
│  │  Order   │◄───────│ OrderItem │                            │
│  ├──────────┤        ├───────────┤                            │
│  │id: Long  │        │id: Long   │                            │
│  │tableId   │        │order: Order│                           │
│  │userId    │        │menuItemId │                            │
│  │status:   │        │menuItemName│                           │
│  │  PENDING │        │quantity   │                            │
│  │  READY   │        │unitPrice  │                            │
│  │  BILLED  │        └───────────┘                            │
│  │  COMPLETED│                                                 │
│  │  CANCELLED│       ┌───────────┐  (Elasticsearch)           │
│  │reservId  │        │ MenuItem  │                            │
│  │createdAt │        ├───────────┤                            │
│  └──────────┘        │id: String │                            │
│                      │name       │                            │
│  ┌──────────┐        │price      │                            │
│  │StockItem │        │category   │                            │
│  ├──────────┤        │available  │                            │
│  │id: String│        │imageUrl   │                            │
│  │name      │        └───────────┘                            │
│  │quantity  │                                                  │
│  │unit      │                                                  │
│  │minThresh.│                                                  │
│  │type: Enum│                                                  │
│  │  SOLID   │                                                  │
│  │  LIQUID  │                                                  │
│  │  PORTION │                                                  │
│  └──────────┘                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       feedback-service                          │
│                                                                 │
│  ┌──────────────────────┐                                      │
│  │       Feedback       │                                      │
│  ├──────────────────────┤                                      │
│  │id: Long              │                                      │
│  │orderId: Long (UNIQUE)│                                      │
│  │userId: Long          │                                      │
│  │foodQualityRating:    │                                      │
│  │  POOR / BELOW_AVG /  │                                      │
│  │  AVERAGE / GOOD /    │                                      │
│  │  EXCELLENT           │                                      │
│  │serviceSpeedRating:   │                                      │
│  │  SLOW/ADEQUATE/FAST  │                                      │
│  │wouldRecommend: bool  │                                      │
│  │comment: Text         │                                      │
│  └──────────────────────┘                                      │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       vouchers-service                          │
│                                                                 │
│  ┌──────────────────────┐                                      │
│  │        Voucher       │                                      │
│  ├──────────────────────┤                                      │
│  │id: Long              │                                      │
│  │code: String(12,UNIQ) │                                      │
│  │userId: Long          │                                      │
│  │value: BigDecimal     │                                      │
│  │expiryDate: LocalDate │                                      │
│  │used: boolean         │                                      │
│  │orderId: Long (UNIQUE)│                                      │
│  └──────────────────────┘                                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 8. Pornirea aplicatiei

### Varianta 1 — deploy.sh (recomandat)

Script-ul `Apollo/deploy.sh` gestionează build-ul și deployment-ul Docker. Rulat fără argumente afișează comenzile disponibile:

```bash
cd Apollo
./deploy.sh
# Usage: ./deploy.sh [all | <service-name> | clean]
#   all              — build și redeploy toate serviciile
#   <service-name>   — build și redeploy un singur serviciu
#   clean            — oprește containerele, șterge imaginile și cache-ul proiectului
#
# Services: auth-service operations-service reservations-service feedback-service
#           cocktails-service reports-service notifications-service settings-service
#           vouchers-service api-gateway eureka-server
```

**Deploy complet (prima rulare sau după modificări majore):**
```bash
cd Apollo
./deploy.sh all
```

**Redeploy un singur microserviciu (după o modificare punctuală):**
```bash
cd Apollo
./deploy.sh auth-service
./deploy.sh operations-service
# etc.
```

**Curățare completă (imagini + cache Gradle + containere):**
```bash
cd Apollo
./deploy.sh clean
```

**Pornire frontend (terminal separat):**
```bash
cd Odin/frontend
npm install
npm run dev
```

Aplicația este disponibilă la **http://localhost:5173**

Eureka dashboard: **http://localhost:8761**  
Swagger UI: **http://localhost:8080/swagger-ui.html**

---

### Varianta 2 — Mod dezvoltare (servicii individuale)

**Prerequisite:** Java 21, Node.js 18+, PostgreSQL 16, Elasticsearch 8.x, Kafka pe porturile implicite.

```bash
# Backend — build complet
cd Apollo
./gradlew clean build

# Pornire servicii (în ordinea dependențelor):
./gradlew :eureka-server:bootRun
./gradlew :auth-service:bootRun
./gradlew :operations-service:bootRun
./gradlew :reservations-service:bootRun
./gradlew :feedback-service:bootRun
./gradlew :cocktails-service:bootRun
./gradlew :reports-service:bootRun
./gradlew :notifications-service:bootRun
./gradlew :settings-service:bootRun
./gradlew :vouchers-service:bootRun
./gradlew :api-gateway:bootRun

# Frontend
cd Odin/frontend
npm install
npm run dev
```

### Varianta 3 — Script automat

```bash
./start-all.sh
```

---

### Variabile de configurare

| Variabilă | Descriere | Implicit |
|-----------|-----------|---------|
| `GEMINI_API_KEY` | Cheie API Google Gemini (cocktails-service) | — |
| `MAIL_USER` | Adresă email SMTP pentru notificări | `odin.dining@gmail.com` |
| `MAIL_PASS` | Parolă aplicație Gmail (App Password) | — |
| `APP_TOKEN_SECRET` | Cheie secretă pentru semnare JWT | cheie demo inclusă |
| `VITE_API_URL` | URL API Gateway din frontend | `http://localhost:8080` |

---

## 9. Roluri si acces

| Rol | Acces |
|-----|-------|
| **GUEST** | Dashboard personal, meniu, coș cumpărături, plasare comenzi, rezervări, feedback post-comandă, vizualizare voucher-e, generare cocktail-uri AI |
| **WAITER** | Dashboard lucrători, gestionare comenzi active (actualizare status), emitere notă de plată |
| **CHEF** | Dashboard lucrători, vizualizare comenzi și rețete asociate articolelor din meniu |
| **MANAGER** | Tot ce are WAITER + gestionare mese (floor plan drag-and-drop), administrare stocuri, rapoarte & analize, setări restaurant |
| **ADMIN** | Acces complet + gestionare utilizatori (creare, editare roluri, dezactivare) |

Noul utilizator înregistrat primește automat rolul **GUEST** și trebuie să verifice adresa de email înainte de primul login.

---

## 10. Functionalitatii implementate si roadmap

### Implementat

- **Autentificare & Autorizare** — JWT stateless, HMAC-SHA256, refresh implicit; verificare email prin token UUID; roluri ierarhice (GUEST→ADMIN); protecție rute frontend
- **Meniu** — CRUD complet cu Elasticsearch; categorii; upload imagini; căutare full-text; vizualizare rețete cu ingrediente
- **Comenzi** — plasare din coș, gestionare status (PENDING→READY→BILLED→COMPLETED), filtrare per masă/utilizator, note comandă
- **Rezervări** — sloturi fixe de 2 ore (10-22), verificare disponibilitate mese, gestionare status (CONFIRMED/CANCELLED/NO_SHOW), anulare cu motiv
- **Gestiune mese** — plan interactiv drag-and-drop (poziție x/y, dimensiuni), creare/editare/ștergere mese
- **Stocuri** — CRUD articole (SOLID/LIQUID/PORTION), scădere automată la plasarea comenzii, alerte prag minim, import CSV, scheduler reaprovizionare
- **Feedback** — recenzie post-comandă (un feedback per comandă), rating calitate mâncare, viteză serviciu, recomandare, comentariu liber
- **Voucher-e** — generare automată la finalizarea comenzii, cod unic de 12 caractere, valabilitate configurabilă, aplicare la comandă
- **Cocktail-uri AI** — generare rețete personalizate cu Google Gemini 2.5 Flash pe baza ingredientelor și preferințelor
- **Rapoarte** — dashboard cu analize vânzări, articole populare, rating feedback, comenzi per perioadă (zilnic/săptămânal/lunar)
- **Notificări email** — confirmare înregistrare, verificare email, confirmare comandă, notificare comandă gata, distribuire voucher
- **Setări restaurant** — configurare date restaurant, ore funcționare, număr maxim rezervări per slot
- **Gestionare utilizatori** (ADMIN) — lista utilizatori, editare roluri, activare/dezactivare cont

### In lucru / Planificat

- Dashboard Kibana integrat pentru monitorizare Elasticsearch
- CI/CD pipeline (GitHub Actions → Docker Hub)
- Adaugare noi rapoarte
- Flyway pentru migrări

---

## API Documentation

Swagger UI disponibil când backend-ul rulează:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
