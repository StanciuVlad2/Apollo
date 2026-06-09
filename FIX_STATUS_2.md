# Fixes Update 2 — Documentare Remedieri

Acest document descrie rezolvarea completă a bug-urilor raportate în sprint-ul de review:
#5 (race condition cu regresie), #6 (rezervări anonime), M3 (ștergere masă), M4 (cocktail quota), M5 (stock negativ), M6 (corectitudine rezervări).

---

## #5 — Race condition: regresie partial index

### Problema originală
Constraint-ul `@UniqueConstraint(columnNames = {"table_id", "reservation_date", "start_time"})` adăugat anterior pe modelul `Reservation` ignora coloana `status`. `cancelReservation()` face soft-delete (setează `status=CANCELLED`, păstrează rândul). Consecință: un slot cancelat ocupa permanent cheia de unicitate → orice rezervare ulterioară pe același slot primea 409 permanent.

### Fix aplicat

**1. `Reservation.java`** — Eliminat `@UniqueConstraint` din adnotarea `@Table`. JPA nu mai creează constraint-ul greșit pe o bază de date fresh.

```java
// Înainte
@Table(name = "reservations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"table_id", "reservation_date", "start_time"},
            name = "uk_reservation_table_date_slot")
})

// După
@Table(name = "reservations")
```

**2. `DatabaseInitializer.java`** (fișier nou în `reservations-service/config/`) — Bean Spring care ascultă `ApplicationReadyEvent` (declanșat garantat după `ddl-auto: update`). Execută două operații idempotente:

```sql
-- Elimină constraint-ul flat dacă există (DB existente, upgrade)
ALTER TABLE reservations DROP CONSTRAINT IF EXISTS uk_reservation_table_date_slot;

-- Creează partial index — doar rândurile CONFIRMED ocupă un slot
CREATE UNIQUE INDEX IF NOT EXISTS uk_active_reservation_slot
  ON reservations (table_id, reservation_date, start_time)
  WHERE status = 'CONFIRMED';
```

**De ce `ApplicationReadyEvent`:** Spring publică acest eveniment după ce întregul context e inițializat, inclusiv după execuția `ddl-auto: update` de Hibernate. Operațiile SQL din initializer rulează pe un DB deja structurat.

**De ce partial index în loc de constraint JPA:** JPA nu suportă partial indexes. Un `UNIQUE INDEX ... WHERE status = 'CONFIRMED'` la nivel PostgreSQL permite reutilizarea unui slot după cancelarea rezervării, menținând protecția împotriva race conditions concurente.

**Compatibilitate:** `IF NOT EXISTS` și `IF EXISTS` fac ambele operații idempotente — restart-ul aplicației nu aruncă erori.

### Fișiere modificate
- `reservations-service/src/main/java/com/restaurant/reservations/model/Reservation.java`
- `reservations-service/src/main/java/com/restaurant/reservations/config/DatabaseInitializer.java` *(nou)*

---

## #6 — Rezervări anonime

### Problema originală
**Backend (gateway):** `AuthGatewayFilter` nu whitelista `GET /api/reservations/availability` și `POST /api/reservations`. Cererile anonime primeau HTTP 401 la nivelul gateway-ului, înainte să ajungă la serviciu (deși `SecurityConfig` din `reservations-service` permitea aceste rute).

**Frontend:** `useEffect` din `Reservations.tsx` apela `checkAvailability()` la fiecare modificare a `selectedDate` sau `partySize`, inclusiv la fiecare caracter tastat. Lipsea debounce și AbortController — se generau zeci de requesturi în zbor simultan.

### Fix aplicat

**1. `AuthGatewayFilter.java`** — Modificate listele de rute publice:

```java
// Înainte
private static final List<String> PUBLIC_POST = List.of(
    "/api/auth/login", "/api/auth/register", "/api/cocktails/generate"
);
private static final List<String> PUBLIC_GET_PREFIXES = List.of(
    "/api/auth/verify-email", "/api/menu-items", "/api/tables",
    "/api/settings", "/api/stock/events"
);

// După
private static final List<String> PUBLIC_POST = List.of(
    "/api/auth/login", "/api/auth/register", "/api/reservations"
);
private static final List<String> PUBLIC_GET_PREFIXES = List.of(
    "/api/auth/verify-email", "/api/menu-items", "/api/tables",
    "/api/settings", "/api/stock/events", "/api/reservations/availability"
);
```

**2. `api.ts`** — Adăugat parametru `signal?: AbortSignal` la `checkAvailability()`:

```typescript
async checkAvailability(date: string, partySize?: number, signal?: AbortSignal) {
  const response = await fetch(url, {
    method: "GET",
    headers: this.getHeaders(this.isAuthenticated()),
    signal,   // ← AbortController signal
  });
}
```

De asemenea schimbat `getHeaders(true)` → `getHeaders(this.isAuthenticated())`: dacă tokenul e expirat, `isAuthenticated()` îl șterge din localStorage și nu mai trimite un header invalid.

**3. `Reservations.tsx`** — Adăugat debounce de 300ms cu AbortController:

```tsx
useEffect(() => {
  if (!selectedDate || !/^\d{4}-\d{2}-\d{2}$/.test(selectedDate)) return

  const controller = new AbortController()
  const timer = setTimeout(() => {
    checkAvailability(controller.signal)
  }, 300)

  return () => {
    clearTimeout(timer)    // anulează timerul dacă input-ul se schimbă înainte de 300ms
    controller.abort()     // anulează requestul dacă era deja în zbor
  }
}, [selectedDate, partySize])
```

`checkAvailability` prinde `AbortError` explicit și returnează fără a seta state pe o componentă în curs de re-render.

### Fișiere modificate
- `api-gateway/src/main/java/com/restaurant/gateway/filter/AuthGatewayFilter.java`
- `Odin/frontend/src/services/api.ts`
- `Odin/frontend/src/pages/Reservations/Reservations.tsx`

---

## M3 — Ștergere masă cu rezervări istorice

### Problema originală
`deleteTable()` în `RestaurantTableService` folosea `reservationRepository.findAll()` (încărca toate rezervările din toate mesele în memorie) și verifica doar statusul `CONFIRMED`. O masă cu rezervări `CANCELLED`, `COMPLETED` sau `NO_SHOW` trecea de verificare și ajungea la `tableRepository.deleteById(id)` — PostgreSQL arunca `ConstraintViolationException` (FK violation) → HTTP 500 generic.

### Fix aplicat

**1. `ReservationRepository.java`** — Adăugate două metode derivate Spring Data:

```java
boolean existsByTableId(Long tableId);
boolean existsByTableIdAndStatus(Long tableId, ReservationStatus status);
```

Spring Data generează automat `SELECT COUNT(*) > 0 FROM reservations WHERE table_id = ?` și varianta cu filtru pe `status`. Nu se mai încarcă niciun rând în memorie.

**2. `RestaurantTableService.deleteTable()`** — Logică nouă în 3 ramuri:

```java
public void deleteTable(Long id) {
    RestaurantTable table = tableRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Table not found: " + id));

    // Rezervări active → 409
    if (reservationRepository.existsByTableIdAndStatus(id, ReservationStatus.CONFIRMED)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT,
            "Cannot delete table with active reservations");
    }

    // Rezervări istorice (FK activ) → soft-delete
    if (reservationRepository.existsByTableId(id)) {
        table.setIsActive(false);
        tableRepository.save(table);
        return;
    }

    // Nicio rezervare → hard-delete normal
    tableRepository.deleteById(id);
}
```

**De ce soft-delete pentru istorice:** FK-ul din `reservations.table_id` → `restaurant_tables.id` nu are `ON DELETE CASCADE`. Ștergerea hard ar viola constrângerea. Dezactivarea mesei (`isActive=false`) o elimină din toate endpoint-urile active fără a viola integritatea referențială.

### Fișiere modificate
- `reservations-service/src/main/java/com/restaurant/reservations/repository/ReservationRepository.java`
- `reservations-service/src/main/java/com/restaurant/reservations/service/RestaurantTableService.java`

---

## M4 — Cocktail quota (gateway)

### Problema originală
`/api/cocktails/generate` era listat în `PUBLIC_POST` în `AuthGatewayFilter`, deci gateway-ul lăsa requesturile fără JWT să treacă. Serviciul de cocktailuri fusese configurat cu `anyRequest().authenticated()`, dar gateway-ul le lăsa să ajungă acolo fără verificare prealabilă.

### Fix aplicat

**`AuthGatewayFilter.java`** — Eliminat `/api/cocktails/generate` din `PUBLIC_POST` (inclus în fix-ul #6 de mai sus). Acum gateway-ul blochează cu 401 orice request fără JWT valid pe această rută, consistent cu configurarea `anyRequest().authenticated()` din `cocktails-service/SecurityConfig`.

### Fișiere modificate
- `api-gateway/src/main/java/com/restaurant/gateway/filter/AuthGatewayFilter.java`

---

## M5 — Stock negativ (verificare cumulativă)

### Problema originală
**Edge case cumulativ:** `deductStock()` în `OrderService` verifica stocul per-item, nu cumulativ. Dacă o comandă conținea două produse care foloseau același ingredient (ex: Pizza 300g făină + Pasta 250g făină), fiecare verificare trecea individual (300g ≤ stoc ✓, 250g ≤ stoc ✓), dar suma (550g) putea depăși stocul disponibil. Deducțiile se executau oricum → stoc negativ.

**Lipsa guard în `deduct()`:** `deduct()` nu verifica dacă rezultatul era negativ — scădea oricum.

### Fix aplicat

**1. `OrderService.deductStock()`** — Restructurată în 3 pași:

```java
// Pasul 1: agregare cantități necesare per ingredient
Map<String, double[]> cumulative = new LinkedHashMap<>();
Map<String, MenuItem> menuItemCache = new LinkedHashMap<>();

for (OrderItem item : order.getItems()) {
    MenuItem menuItem = menuItemService.getRawById(item.getMenuItemId());
    menuItemCache.put(item.getMenuItemId(), menuItem);
    for (RecipeIngredient ingredient : menuItem.getRecipe()) {
        double needed = ingredient.getQuantity() * item.getQuantity();
        String key = ingredient.getIngredientName().toLowerCase() + "|" + ingredient.getUnit();
        cumulative.computeIfAbsent(key, k -> new double[]{0})[0] += needed;
    }
}

// Pasul 2: preflight cumulativ
for (Map.Entry<String, double[]> entry : cumulative.entrySet()) {
    String[] parts = entry.getKey().split("\\|", 2);
    if (!stockItemService.hasSufficientStock(parts[0], entry.getValue()[0], parts[1])) {
        throw new IllegalStateException("Insufficient stock for: " + parts[0]);
    }
}

// Pasul 3: deducții individuale
```

Cache-ul de `MenuItem` evită fetch-ul dublu din Elasticsearch.

**2. `StockItemService.deduct()`** — Adăugat guard explicit:

```java
double newQty = Math.round((item.getQuantity() - converted) * 100.0) / 100.0;
if (newQty < 0) {
    throw new IllegalStateException("Insufficient stock for: " + ingredientName);
}
item.setQuantity(newQty);
```

Guard-ul acționează ca backstop de siguranță chiar dacă preflight-ul este ocolit (ex: concurență).

### Fișiere modificate
- `operations-service/src/main/java/com/restaurant/operations/orders/service/OrderService.java`
- `operations-service/src/main/java/com/restaurant/operations/stock/service/StockItemService.java`

---

## M6 — Corectitudine rezervări (cancelled-as-conflict)

### Cauza
Problema era legată direct de regresia de la #5. Query-ul `findConflictingReservations` din `ReservationRepository` filtra deja corect pe `status = 'CONFIRMED'`, deci la nivel logic nu confunda rezervările CANCELLED cu conflicte. Problema era că partial index-ul lipsea — un slot CANCELLED era inutilizabil din cauza constraint-ului flat, nu din cauza logicii de conflict.

### Rezolvare
M6 este rezolvat complet de fix-ul #5: eliminarea `@UniqueConstraint` + crearea partial index-ului `WHERE status = 'CONFIRMED'`. Ambele query-uri relevante (`findConflictingReservations` și `findReservedTablesForTimeSlot`) filtrează deja pe CONFIRMED — nu era nevoie de modificări suplimentare în logică.

---

## Sumar fișiere modificate

| Serviciu | Fișier | Bug |
|----------|--------|-----|
| reservations-service | `model/Reservation.java` | #5 |
| reservations-service | `config/DatabaseInitializer.java` *(nou)* | #5 |
| reservations-service | `repository/ReservationRepository.java` | M3 |
| reservations-service | `service/RestaurantTableService.java` | M3 |
| api-gateway | `filter/AuthGatewayFilter.java` | #6, M4 |
| operations-service | `orders/service/OrderService.java` | M5 |
| operations-service | `stock/service/StockItemService.java` | M5 |
| frontend (Odin) | `services/api.ts` | #6 |
| frontend (Odin) | `pages/Reservations/Reservations.tsx` | #6 |
