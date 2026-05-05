# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Build & Run Commands

Each service is an independent Maven project. Run commands from inside each service's inner directory (e.g., `booking-service/booking-service/`).

```bash
# Build (skip tests)
mvn clean package -DskipTests

# Run
mvn spring-boot:run

# Run a single test
mvn test -Dtest=ClassName#methodName
```

**Startup order matters** — services must start in this sequence:
1. `discovery-sevice` (Eureka, port 8761)
2. `config-server` (port 8888)
3. `user-service` (port 8081)
4. `flight-service` (port 8082)
5. `booking-service` (port 8083)
6. `payment-service` (port 8084)
7. `notification-service` (port 8085)

**External infrastructure required before starting any service:**
- PostgreSQL on `localhost:5432` (user: `postgres`, password: `1212`)
- RabbitMQ on `localhost:5672` (user: `guest`, password: `guest`)

**Databases to create manually:**
```sql
CREATE DATABASE airsync_users;
CREATE DATABASE airsync_flights;
CREATE DATABASE airsync_bookings;
CREATE DATABASE airsync_payments;
```
All services use `ddl-auto: update` — tables are created automatically on first run.

---

## Architecture Overview

AirSync is a **Spring Cloud microservices** airline booking platform (Spring Boot 4.0.6, Java 21, Spring Cloud 2025.1.1).

### Service Map

```
Client
  │
  ├── user-service       :8081  PostgreSQL (airsync_users)
  ├── flight-service     :8082  PostgreSQL (airsync_flights)
  ├── booking-service    :8083  PostgreSQL (airsync_bookings)  ──Feign──► flight-service
  ├── payment-service    :8084  PostgreSQL (airsync_payments)  ──Feign──► booking-service
  └── notification-service :8085  (no DB — stateless)

Infrastructure:
  discovery-sevice  :8761  (Eureka registry — note the typo in folder name)
  config-server     :8888  (Git-backed config: github.com/Thomson-dev/airsync-config)
  RabbitMQ          :5672  (exchange: airsync.exchange)
```

### Complete Booking Flow

```
1. POST /api/users/register  →  user-service
2. POST /api/users/login     →  JWT token returned

3. GET  /api/flights/search  →  flight-service
4. GET  /api/flights/{id}/seats

5. POST /api/bookings        →  booking-service
        • validates price against flight-service
        • marks seat unavailable via Feign
        • saves booking as PENDING
        • sets expiresAt = now + 10 minutes

        PATH A — user pays within 10 minutes:
6.      POST /api/payments/initiate   →  payment-service → Paystack link
7.      POST /api/payments/verify/{ref} → Paystack verify
              → calls booking-service PUT /api/bookings/{id}/confirm (Feign)
              → publishes PAYMENT_SUCCESS to RabbitMQ
8.      PUT  /api/bookings/{id}/confirm
              → status: CONFIRMED
              → publishes BOOKING_CONFIRMED to RabbitMQ
9.      notification-service receives event → sends confirmation email

        PATH B — 10 minutes elapse without payment:
        @Scheduled (every 60s) in booking-service finds expired PENDING bookings
              → releases seat via Feign
              → status: CANCELLED
              → publishes BOOKING_CANCELLED to RabbitMQ
        notification-service → sends cancellation email
```

### Inter-Service Communication

**Synchronous (OpenFeign):**
- `booking-service → flight-service`: `FlightServiceClient` — `getFlight()`, `bookSeat()`, `releaseSeat()`
- `payment-service → booking-service`: `BookingServiceClient` — `confirmBooking()`

**Asynchronous (RabbitMQ — TopicExchange `airsync.exchange`):**

| Publisher | Routing Key | Queue | Events |
|---|---|---|---|
| booking-service | `booking.events` | `booking.notification.queue` | `BOOKING_CONFIRMED`, `BOOKING_CANCELLED` |
| payment-service | `payment.events` | `payment.notification.queue` | `PAYMENT_SUCCESS`, `PAYMENT_FAILED` |

Both queues are consumed by `notification-service` via `@RabbitListener`.

### Booking Lifecycle

```
PENDING  ──(payment verified)──► CONFIRMED
PENDING  ──(10 min elapsed)───► CANCELLED
CONFIRMED/PENDING ──(manual)──► CANCELLED
```

The `expiresAt` field on `Booking` drives the scheduler. `BookingRepository.findByStatusAndExpiresAtBefore(PENDING, now)` is the query.

---

## Key Design Decisions

**Price validation on booking:** `BookingService.createBooking` calls `FlightServiceClient.getFlight()` and rejects the request if `|submittedPrice - actualPrice| > 0.01`. This prevents clients from submitting arbitrary prices.

**Seat state is owned by flight-service:** Booking and payment services never modify seat data directly — they always call `flight-service` via Feign. If a Feign call to `releaseSeat` or `bookSeat` fails, the `@Transactional` on the calling method rolls back the booking change too.

**Paystack integration:** `PaystackService` uses plain `RestTemplate`. Amount is converted to kobo (`price * 100`) before sending to Paystack. `verifyPayment` checks for `status == "success"` in the Paystack response body.

**RabbitMQ message format:** All events are serialized as JSON via `Jackson2JsonMessageConverter`. Each service declares its own `RabbitMQConfig` bean that creates the exchange, queue, and binding on startup — RabbitMQ will create them if they don't exist.

**Notification email resolution:** `NotificationListener.resolveEmail(userId)` is a placeholder. For booking events, the user's email isn't in the event payload — a Feign call to `user-service` should replace the placeholder before going to production.

---

## Credentials to Configure Before Running

| File | Key | What to set |
|---|---|---|
| `payment-service/.../application.yaml` | `paystack.secret-key` | Your Paystack secret key from the dashboard |
| `notification-service/.../application.yaml` | `spring.mail.username` | Your Gmail address |
| `notification-service/.../application.yaml` | `spring.mail.password` | Gmail App Password (not your login password) |
| `notification-service/.../application.yaml` | `notification.from-email` | Same Gmail address |
