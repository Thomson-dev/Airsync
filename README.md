# AirSync ✈️

A microservices-based airline booking system built with Spring Boot, Spring Cloud, RabbitMQ, and Paystack.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.0.6 / Spring Cloud 2025.1.1 |
| Language | Java 21 |
| Service Discovery | Netflix Eureka |
| Config Management | Spring Cloud Config Server |
| Inter-service Calls | OpenFeign |
| Message Broker | RabbitMQ |
| Database | PostgreSQL |
| Authentication | JWT (Spring Security) |
| Payment Gateway | Paystack |
| Email | JavaMail (Gmail SMTP) |
| Build | Maven |

---

## Services

| Service | Port | Database | Description |
|---|---|---|---|
| discovery-service | 8761 | — | Eureka service registry |
| config-server | 8888 | — | Centralised Git-backed configuration |
| user-service | 8081 | airsync_users | Registration, login, JWT |
| flight-service | 8082 | airsync_flights | Flight catalog and seat management |
| booking-service | 8083 | airsync_bookings | Booking lifecycle + expiry scheduler |
| payment-service | 8084 | airsync_payments | Paystack payment initiation and verification |
| notification-service | 8085 | — | Email notifications via RabbitMQ events |

---

## How It Works

### Full Booking Flow

```
1. Register / Login          →  user-service  (returns JWT)

2. Search flights            →  flight-service
3. View available seats      →  flight-service

4. Create booking            →  booking-service
   ├── Validates price against flight-service
   ├── Marks seat unavailable (Feign → flight-service)
   ├── Saves booking as PENDING
   └── Starts 10-minute payment countdown

      ┌─────── PATH A: User pays within 10 minutes ───────┐
5.    Initiate payment        →  payment-service
                                 └── Returns Paystack checkout URL
6.    Verify payment          →  payment-service
                                 ├── Confirms with Paystack API
                                 ├── Calls booking-service to CONFIRM (Feign)
                                 └── Publishes PAYMENT_SUCCESS → RabbitMQ
7.    Booking confirmed       →  booking-service
                                 └── Publishes BOOKING_CONFIRMED → RabbitMQ
8.    Email sent              →  notification-service
      └─────────────────────────────────────────────────────┘

      ┌─────── PATH B: No payment within 10 minutes ──────┐
5.    Scheduler (every 60s)   →  booking-service
                                 ├── Finds expired PENDING bookings
                                 ├── Releases seat (Feign → flight-service)
                                 ├── Sets status to CANCELLED
                                 └── Publishes BOOKING_CANCELLED → RabbitMQ
6.    Cancellation email      →  notification-service
      └─────────────────────────────────────────────────────┘
```

### Service-to-Service Communication

**Synchronous (Feign):**
- `booking-service` → `flight-service` — price validation, seat booking/release
- `payment-service` → `booking-service` — confirm booking after payment

**Asynchronous (RabbitMQ — exchange: `airsync.exchange`):**

| Publisher | Routing Key | Queue | Events |
|---|---|---|---|
| booking-service | `booking.events` | `booking.notification.queue` | `BOOKING_CONFIRMED`, `BOOKING_CANCELLED` |
| payment-service | `payment.events` | `payment.notification.queue` | `PAYMENT_SUCCESS`, `PAYMENT_FAILED` |

---

## Prerequisites

- Java 21
- Maven 3.8+
- PostgreSQL running on `localhost:5432`
- RabbitMQ running on `localhost:5672`

---

## Setup

### 1. Create PostgreSQL databases

```sql
CREATE DATABASE airsync_users;
CREATE DATABASE airsync_flights;
CREATE DATABASE airsync_bookings;
CREATE DATABASE airsync_payments;
```

### 2. Configure credentials

**Payment service** — `payment-service/payment-service/src/main/resources/application.yaml`:
```yaml
paystack:
  secret-key: sk_live_xxxxxxxxxxxxxxxx   # from Paystack dashboard
```

**Notification service** — `notification-service/notification-service/src/main/resources/application.yaml`:
```yaml
spring:
  mail:
    username: your@gmail.com
    password: xxxx xxxx xxxx xxxx        # Gmail App Password

notification:
  from-email: your@gmail.com
```

> Gmail App Passwords: Google Account → Security → 2-Step Verification → App passwords

### 3. Start services (in order)

```bash
# From each service's inner directory, e.g. discovery-sevice/discovery-sevice/
mvn spring-boot:run
```

Start order: **discovery → config-server → user → flight → booking → payment → notification**

---

## API Reference

### User Service `:8081`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/users/register` | Create account |
| POST | `/api/users/login` | Login, returns JWT |
| GET | `/api/users/profile/{email}` | Get user profile |

### Flight Service `:8082`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/flights` | Add a flight |
| GET | `/api/flights/search?from=X&to=Y&date=YYYY-MM-DD` | Search flights |
| GET | `/api/flights/{id}` | Get flight details |
| GET | `/api/flights/{id}/seats` | List available seats |

### Booking Service `:8083`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/bookings` | Create booking (PENDING) |
| PUT | `/api/bookings/{id}/confirm` | Confirm booking (called by payment-service) |
| PUT | `/api/bookings/{id}/cancel` | Cancel booking |
| GET | `/api/bookings/{id}` | Get booking |
| GET | `/api/bookings/user/{userId}` | Get all bookings for a user |

**Create booking request:**
```json
{
  "userId": 1,
  "flightId": 1,
  "seatNumber": "3A",
  "totalPrice": 299.99
}
```

> `totalPrice` must match the flight's actual price (±0.01 tolerance). Any mismatch returns `400 Bad Request`.

### Payment Service `:8084`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/payments/initiate` | Start payment, returns Paystack URL |
| POST | `/api/payments/verify/{reference}` | Verify payment with Paystack |

**Initiate payment request:**
```json
{
  "bookingId": 1,
  "userId": 1,
  "amount": 299.99,
  "email": "user@example.com"
}
```

---

## Booking States

```
PENDING ──► CONFIRMED    (payment verified within 10 minutes)
PENDING ──► CANCELLED    (10-minute window expired — automatic)
CONFIRMED ──► CANCELLED  (manual cancellation)
```

---

## Project Structure

```
AirSync/
├── discovery-sevice/         Eureka server
├── config-server/            Spring Cloud Config Server
├── user-service/             Auth + JWT
├── flight-service/           Flights + seats
├── booking-service/          Bookings + scheduler + RabbitMQ publisher
├── payment-service/          Paystack + RabbitMQ publisher
└── notification-service/     RabbitMQ listener + email sender
```

Each service follows the same internal structure:
```
src/main/java/com/thomson/
  ├── <ServiceName>Application.java
  └── <servicename>/
        ├── config/          RabbitMQConfig (where applicable)
        ├── controller/
        ├── dto/             Request, Response, Event DTOs
        ├── entity/
        ├── exception/       GlobalExceptionHandler
        ├── feign/           Feign clients (where applicable)
        ├── publisher/       RabbitMQ publishers (where applicable)
        ├── repository/
        └── service/
```
