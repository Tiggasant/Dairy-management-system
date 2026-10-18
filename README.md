# Ghunsai Dairy Management – Backend API

Spring Boot 4 (Java 17) · PostgreSQL · Flyway · stateless JWT auth with two roles: **OWNER** and **WORKER**.

## Who can do what

| Area | Endpoint | OWNER | WORKER |
|---|---|:-:|:-:|
| **Auth** | `POST /api/auth/login` | public | public |
| | `GET /api/auth/me`, `POST /api/auth/change-password` | ✅ | ✅ |
| **Users** | `POST/GET /api/users`, `PUT /api/users/{id}/active`, `PUT /api/users/{id}/password` | ✅ | ❌ |
| **Billing** | `POST /api/sales` (create bill) | ✅ | ✅ |
| | `GET /api/sales/{id}` (receipt) | any bill | own bills only |
| | `GET /api/sales/reports/my-daily?date=` (my totals / cash to hand over) | ✅ | ✅ |
| | `GET /api/sales?from=&to=&page=&size=` (bill history) | ✅ | ❌ |
| | `GET /api/sales/reports/daily?date=` (whole shop) | ✅ | ❌ |
| **Dashboard** | `GET /api/dashboard/daily` | ✅ | ❌ |
| **Products** | `GET /api/products`, `GET /api/products/{id}` | all products | active only |
| | `POST /api/products`, `PUT /api/products/{id}`, `PUT /api/products/{id}/price`, `PUT /api/products/{id}/active`, `GET /api/products/{id}/price-history` | ✅ | ❌ |
| **Stock** | `GET /api/stock/{productId}`, `GET /api/stock/report` | ✅ | ✅ |
| | `POST /api/stock/purchase`, `POST /api/stock/adjust` (wastage / correction), `GET /api/stock/movements` | ✅ | ❌ |
| **Customers** | `GET /api/customers`, `POST /api/customers`, `GET /api/customers/{id}/balance` | ✅ | ✅ (active only) |
| | `PUT /api/customers/{id}`, `PUT /api/customers/{id}/active`, `GET /api/customers/balances` (who owes what) | ✅ | ❌ |
| **Udhaar payments** | `POST/GET /api/customer-payments/{customerId}` | ✅ | ✅ |
| **Ops** | `GET /actuator/health`, `GET /api/home` | public | public |

The whole matrix lives in one place: `config/SecurityConfig.java`. Anything not listed there is denied (`anyRequest().denyAll()`).
To give workers another permission, move one line from the OWNER block to the shared block.

## Run locally

Requirements: JDK 17+, PostgreSQL 14+.

```bash
createdb dairy_management                      # empty database – Flyway creates all tables
export SPRING_PROFILES_ACTIVE=dev              # local defaults (DB postgres/postgres on :5432, owner / Owner@12345)
./mvnw spring-boot:run
```

Override the dev defaults with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` if your Postgres differs.

## Deploy with Docker

```bash
cp .env.example .env     # set DB_PASSWORD, JWT_SECRET (openssl rand -base64 48), BOOTSTRAP_OWNER_* 
docker compose up -d --build
curl http://localhost:8080/actuator/health
```

Put it behind HTTPS (nginx / Caddy / a cloud load balancer) and set `FORWARD_HEADERS_STRATEGY=native`.
**Never expose the API over plain HTTP on the internet – tokens travel in the `Authorization` header.**

### Configuration (environment variables)

| Variable | Required | Meaning |
|---|:-:|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | ✅ | PostgreSQL connection |
| `JWT_SECRET` | ✅ | ≥ 32 chars. Changing it logs everybody out |
| `JWT_EXPIRATION_MINUTES` | | default 720 (12 h) |
| `BOOTSTRAP_OWNER_USERNAME` / `_PASSWORD` / `_NAME` | first start | creates the first OWNER if none exists. Remove the password afterwards |
| `CORS_ALLOWED_ORIGINS` | | web frontend origins, comma separated (not needed for a mobile app) |
| `APP_TIMEZONE` | | default `Asia/Kolkata` – decides what "today" means in reports |
| `FORWARD_HEADERS_STRATEGY` | | `native` behind a reverse proxy |
| `DB_POOL_SIZE`, `PORT` | | defaults 10 / 8080 |

The app refuses to start if a required value is missing or the JWT secret is too short – there are no hidden default passwords.

## Typical flow

```bash
# 1. owner logs in
TOKEN=$(curl -s localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"owner","password":"Owner@12345"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')

# 2. owner creates a worker
curl -s localhost:8080/api/users -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"username":"ramu","fullName":"Ramu","password":"Worker@123","role":"WORKER"}'

# 3. owner adds a product and stock
curl -s localhost:8080/api/products -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Milk","category":"Dairy","unit":"litre","sellingPrice":50,"lowStockThreshold":5}'
curl -s localhost:8080/api/stock/purchase -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"productId":1,"quantity":20,"notes":"Morning delivery"}'

# 4. worker creates a bill (prices come from the server, never from the client)
curl -s localhost:8080/api/sales -H "Authorization: Bearer $WORKER_TOKEN" -H 'Content-Type: application/json' \
  -d '{"items":[{"productId":1,"quantity":2}],"cashAmount":60,"upiAmount":40,"udhaarAmount":0,"clientRequestId":"<uuid-per-bill>"}'
```

Login response: `{ "accessToken": "...", "tokenType": "Bearer", "expiresInSeconds": 43200, "user": { "id", "username", "fullName", "role", ... } }`.
Send it as `Authorization: Bearer <accessToken>`; the app should read `user.role` to decide which screens to show.

Every error has the same shape: `{ "timestamp", "status", "error", "message", "fieldErrors": { "field": "reason" } }`.
Status codes: `400` validation / business rule, `401` not logged in or token revoked/expired, `403` role not allowed, `404`, `409` conflict, `429` too many login attempts.

## Business rules enforced by the server

* **Prices** are always taken from the product, never from the request. Price changes are kept in `product_price_history`; a bill stores the price it was sold at.
* **Stock** is never stored as a number – it is derived from the movement ledger (purchase / adjustment-in add; sale / wastage / adjustment-out subtract). A sale is refused when stock is insufficient.
* **Concurrency**: product rows are locked (always in id order, so no deadlocks) while a bill is created – two counters can never sell the last litre twice. Udhaar payments lock the customer row.
* **Bill split**: `cash + upi + udhaar` must equal the bill total (also enforced by a DB constraint). Udhaar needs a customer.
* **Duplicate protection**: send a unique `clientRequestId` with each bill; a retry after a network failure returns the same bill.
* **Udhaar ledger**: outstanding = total udhaar − total payments. A payment larger than the outstanding amount is rejected. A customer who still owes money cannot be deactivated.
* **Audit**: every bill, stock movement and payment records which user created it.
* **Login safety**: BCrypt passwords, 5 wrong passwords → 15 min lock (per client address + username), generic "invalid username or password" message.
* **Instant revoke**: deactivating a user, resetting a password or changing a password invalidates all of that user's tokens immediately.

## Database

Flyway migrations live in `src/main/resources/db/migration`. Hibernate only *validates* the schema (`ddl-auto=validate`).
Use a **fresh database** for production (the old `ddl-auto=update` development database is not compatible with `V1`).
Take regular `pg_dump` backups. Never edit an applied migration – add `V2__...sql`.

## Tests

```bash
./mvnw test      # unit tests, no database needed
# end-to-end test (real PostgreSQL, Flyway, security rules) – use an EMPTY throwaway database:
TEST_DB_URL=jdbc:postgresql://localhost:5432/dairy_test TEST_DB_USER=postgres TEST_DB_PASSWORD=postgres ./mvnw test
```
