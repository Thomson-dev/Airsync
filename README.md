# AirSync ✈️

A microservices-based airline booking system built with Spring Boot, Spring Cloud, RabbitMQ, and Paystack.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Services](#services)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Setup](#setup)
- [Complete Usage Walkthrough](#complete-usage-walkthrough)
- [API Reference](#api-reference)
- [Booking Lifecycle](#booking-lifecycle)
- [Project Structure](#project-structure)

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

## Architecture

```
[Client]
  │
  ├─ 1. Register / Login ─────────────────► user-service:8081
  │
  ├─ 2. Search flights / seats ───────────► flight-service:8082
  │
  ├─ 3. POST /api/bookings ───────────────► booking-service:8083
  │                                              │
  │                                              ├──Feign──► flight-service (validate price)
  │                                              ├──Feign──► flight-service (book seat)
  │                                              └── saved as PENDING ⏱️ 10 min
  │
  ├─ 4. POST /api/payments/initiate ─────► payment-service:8084
  │                                              └──► Paystack API → checkout URL
  │
  ├─ 5. User pays on Paystack
  │
  ├─ 6. POST /api/payments/verify ───────► payment-service:8084
  │                                              ├──Feign──► booking-service (confirm)
  │                                              └──RabbitMQ──► notification-service:8085
  │                                                                   └──► Gmail ✅
  │
  └─ (if no payment in 10 min)
       @Scheduled ─────────────────────────► booking-service:8083
                                                  ├──Feign──► flight-service (release seat)
                                                  └──RabbitMQ──► notification-service:8085
                                                                       └──► Gmail ❌
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
- PostgreSQL running on `localhost:5432` (user: `postgres`, password: `1212`)
- RabbitMQ running on `localhost:5672` (user: `guest`, password: `guest`)

---

## Setup

### 1. Create PostgreSQL databases

```sql
CREATE DATABASE airsync_users;
CREATE DATABASE airsync_flights;
CREATE DATABASE airsync_bookings;
CREATE DATABASE airsync_payments;
```

Tables are created automatically on first run (`ddl-auto: update`).

### 2. Configure credentials

**Payment service** — `payment-service/payment-service/src/main/resources/application.yaml`:
```yaml
paystack:
  secret-key: sk_test_xxxxxxxxxxxxxxxx   # from Paystack dashboard
```

**Notification service** — `notification-service/notification-service/src/main/resources/application.yaml`:
```yaml
spring:
  mail:
    username: your@gmail.com
    password: xxxx xxxx xxxx xxxx        # Gmail App Password (not your login password)

notification:
  from-email: your@gmail.com
```

> Gmail App Passwords: Google Account → Security → 2-Step Verification → App passwords

### 3. Start services in order

```bash
# From each service's inner directory, e.g. discovery-sevice/discovery-sevice/
mvn spring-boot:run
```

**Start order** (each must be healthy before starting the next):

1. `discovery-sevice` → http://localhost:8761
2. `config-server` → http://localhost:8888
3. `user-service` → http://localhost:8081
4. `flight-service` → http://localhost:8082
5. `booking-service` → http://localhost:8083
6. `payment-service` → http://localhost:8084
7. `notification-service` → http://localhost:8085

---

## Complete Usage Walkthrough

This section walks through the full booking flow end-to-end using curl examples.

---

### Step 1 — Register a User

**Request:**
```bash
curl -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "John Doe",
    "email": "john.doe@example.com",
    "password": "secret123",
    "phoneNumber": "08012345678"
  }'
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john.doe@example.com",
  "fullName": "John Doe"
}
```

---

### Step 2 — Login

**Request:**
```bash
curl -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "secret123"
  }'
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john.doe@example.com",
  "fullName": "John Doe"
}
```

Save the `token` — all subsequent requests require:
```
Authorization: Bearer <token>
```

---

### Step 3 — Add a Flight (Admin)

**Request:**
```bash
curl -X POST http://localhost:8082/api/flights \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "flightNumber": "AS101",
    "origin": "LOS",
    "destination": "ABJ",
    "departureDate": "2026-06-15",
    "departureTime": "08:00:00",
    "arrivalTime": "09:10:00",
    "price": 45000.00,
    "totalSeats": 12
  }'
```

> `totalSeats` must be a multiple of 6. Seats are auto-generated (1A, 1B, 1C... in Business, Economy, First Class).

**Response `200 OK`:**
```json
{
  "id": 1,
  "flightNumber": "AS101",
  "origin": "LOS",
  "destination": "ABJ",
  "departureDate": "2026-06-15",
  "departureTime": "08:00:00",
  "arrivalTime": "09:10:00",
  "price": 45000.00,
  "availableSeats": 12,
  "status": "SCHEDULED"
}
```

---

### Step 4 — Search for Flights

**Request:**
```bash
curl "http://localhost:8082/api/flights/search?from=LOS&to=ABJ&date=2026-06-15" \
  -H "Authorization: Bearer <token>"
```

**Response `200 OK`:**
```json
[
  {
    "id": 1,
    "flightNumber": "AS101",
    "origin": "LOS",
    "destination": "ABJ",
    "departureDate": "2026-06-15",
    "departureTime": "08:00:00",
    "arrivalTime": "09:10:00",
    "price": 45000.00,
    "availableSeats": 12,
    "status": "SCHEDULED"
  }
]
```

---

### Step 5 — View Available Seats

**Request:**
```bash
curl http://localhost:8082/api/flights/1/seats \
  -H "Authorization: Bearer <token>"
```

**Response `200 OK`:**
```json
[
  { "id": 1, "seatNumber": "1A", "seatClass": "BUSINESS", "available": true },
  { "id": 2, "seatNumber": "1B", "seatClass": "BUSINESS", "available": true },
  { "id": 3, "seatNumber": "2A", "seatClass": "ECONOMY",  "available": true },
  { "id": 4, "seatNumber": "2B", "seatClass": "ECONOMY",  "available": false }
]
```

---

### Step 6 — Create a Booking

**Request:**
```bash
curl -X POST http://localhost:8083/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "userId": 1,
    "flightId": 1,
    "seatNumber": "1A",
    "totalPrice": 45000.00
  }'
```

> `totalPrice` must match the flight's actual price within ±0.01. Any larger mismatch returns `400 Bad Request`.

**Response `200 OK`:**
```json
{
  "id": 1,
  "userId": 1,
  "flightId": 1,
  "seatNumber": "1A",
  "totalPrice": 45000.00,
  "status": "PENDING",
  "bookingTime": "2026-06-15T08:30:00"
}
```

The booking is now `PENDING` with a **10-minute window** to complete payment before it auto-cancels and releases the seat.

---

### Step 7 — Initiate Payment

**Request:**
```bash
curl -X POST http://localhost:8084/api/payments/initiate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "bookingId": 1,
    "userId": 1,
    "amount": 45000.00,
    "email": "john.doe@example.com"
  }'
```

**Response `200 OK`:**
```json
{
  "id": 1,
  "bookingId": 1,
  "reference": "xk7ab29qmz",
  "authorizationUrl": "https://checkout.paystack.com/xk7ab29qmz",
  "status": "PENDING"
}
```

Open the `authorizationUrl` in a browser to complete payment on Paystack.

**Paystack test card:**
| Field | Value |
|---|---|
| Card number | `4084 0840 8408 4081` |
| Expiry | Any future date |
| CVV | `408` |
| PIN | `0000` |
| OTP | `123456` |

---

### Step 8 — Verify Payment

After completing payment on Paystack, verify using the `reference` from Step 7:

**Request:**
```bash
curl -X POST http://localhost:8084/api/payments/verify/xk7ab29qmz \
  -H "Authorization: Bearer <token>"
```

**Response `200 OK`:**
```json
{
  "id": 1,
  "bookingId": 1,
  "reference": "xk7ab29qmz",
  "status": "SUCCESS"
}
```

What happens internally:
1. Paystack is called to confirm the transaction
2. `booking-service` is called via Feign → booking status → `CONFIRMED`
3. `PAYMENT_SUCCESS` event published to RabbitMQ
4. `BOOKING_CONFIRMED` event published to RabbitMQ
5. `notification-service` sends confirmation email to the user

---

### What Happens If You Don't Pay in Time

A scheduler in `booking-service` runs every 60 seconds and automatically cancels expired bookings:

```
PENDING booking + expiresAt < now
  └── flight-service: seat released (Feign)
  └── booking status → CANCELLED
  └── BOOKING_CANCELLED event → RabbitMQ → cancellation email sent
```

---

## API Reference

### User Service `:8081`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/users/register` | Register a new account |
| POST | `/api/users/login` | Login, returns JWT token |
| GET | `/api/users/profile/{email}` | Get user profile by email |

**Register request body:**
```json
{
  "fullName": "John Doe",
  "email": "john.doe@example.com",
  "password": "secret123",
  "phoneNumber": "08012345678"
}
```

**Login request body:**
```json
{
  "email": "john.doe@example.com",
  "password": "secret123"
}
```

**Auth response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john.doe@example.com",
  "fullName": "John Doe"
}
```

---

### Flight Service `:8082`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/flights` | Add a flight |
| GET | `/api/flights/search?from=X&to=Y&date=YYYY-MM-DD` | Search flights |
| GET | `/api/flights/{id}` | Get flight details |
| GET | `/api/flights/{id}/seats` | List all seats and availability |
| DELETE | `/api/flights/{id}` | Delete a flight |

**Add flight request body:**
```json
{
  "flightNumber": "AS101",
  "origin": "LOS",
  "destination": "ABJ",
  "departureDate": "2026-06-15",
  "departureTime": "08:00:00",
  "arrivalTime": "09:10:00",
  "price": 45000.00,
  "totalSeats": 12
}
```

**Flight response:**
```json
{
  "id": 1,
  "flightNumber": "AS101",
  "origin": "LOS",
  "destination": "ABJ",
  "departureDate": "2026-06-15",
  "departureTime": "08:00:00",
  "arrivalTime": "09:10:00",
  "price": 45000.00,
  "availableSeats": 12,
  "status": "SCHEDULED"
}
```

**Seat response:**
```json
[
  { "id": 1, "seatNumber": "1A", "seatClass": "BUSINESS", "available": true },
  { "id": 2, "seatNumber": "2A", "seatClass": "ECONOMY",  "available": false }
]
```

---

### Booking Service `:8083`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/bookings` | Create a booking (status: PENDING) |
| GET | `/api/bookings/{id}` | Get booking by ID |
| GET | `/api/bookings/user/{userId}` | Get all bookings for a user |
| PUT | `/api/bookings/{id}/confirm` | Confirm booking (called internally by payment-service) |
| PUT | `/api/bookings/{id}/cancel` | Cancel a booking manually |

**Create booking request body:**
```json
{
  "userId": 1,
  "flightId": 1,
  "seatNumber": "1A",
  "totalPrice": 45000.00
}
```

**Booking response:**
```json
{
  "id": 1,
  "userId": 1,
  "flightId": 1,
  "seatNumber": "1A",
  "totalPrice": 45000.00,
  "status": "PENDING",
  "bookingTime": "2026-06-15T08:30:00"
}
```

> `totalPrice` must match the flight's actual price within **±0.01**. Larger deviation returns `400 Bad Request`.

---

### Payment Service `:8084`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/payments/initiate` | Create Paystack transaction, returns checkout URL |
| POST | `/api/payments/verify/{reference}` | Verify payment and confirm booking |

**Initiate request body:**
```json
{
  "bookingId": 1,
  "userId": 1,
  "amount": 45000.00,
  "email": "john.doe@example.com"
}
```

**Initiate response:**
```json
{
  "id": 1,
  "bookingId": 1,
  "reference": "xk7ab29qmz",
  "authorizationUrl": "https://checkout.paystack.com/xk7ab29qmz",
  "status": "PENDING"
}
```

**Verify response (success):**
```json
{
  "id": 1,
  "bookingId": 1,
  "reference": "xk7ab29qmz",
  "status": "SUCCESS"
}
```

**Verify response (failed):**
```json
{
  "id": 1,
  "bookingId": 1,
  "reference": "xk7ab29qmz",
  "status": "FAILED"
}
```

---

## Booking Lifecycle

```
PENDING ──(payment verified within 10 min)──► CONFIRMED
PENDING ──(10-minute window expired)────────► CANCELLED  (automatic, seat released)
CONFIRMED / PENDING ──(manual)──────────────► CANCELLED
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

Each service follows the same internal layout:
```
src/main/java/com/thomson/<service>/
  ├── <Service>Application.java
  ├── config/          RabbitMQConfig, SecurityConfig
  ├── controller/
  ├── dto/             Request, Response, Event DTOs
  ├── entity/
  ├── exception/       GlobalExceptionHandler
  ├── feign/           Feign clients (where applicable)
  ├── publisher/       RabbitMQ publishers (where applicable)
  ├── repository/
  └── service/
```
