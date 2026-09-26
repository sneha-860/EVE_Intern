# EVE Healthcare — Backend Engineering Assignment

A production-grade backend service built with Java 21 & Spring Boot 3 for diagnostic test bookings and simulated payments with idempotent webhook handling.

---

## 🚀 Features

1. **Authentication (JWT-based)**
   - User signup (`POST /auth/signup`)
   - User login with JWT token issuance (`POST /auth/login`)
   - Spring Security stateless RBAC authentication filter

2. **Diagnostic Centres & Tests Management**
   - Create & retrieve Diagnostic Centres (`POST /centres`, `GET /centres`, `GET /centres/{id}`)
   - Create & retrieve Diagnostic Tests offered by centres (`POST /centres/{centreId}/tests`, `GET /centres/{centreId}/tests`)

3. **Booking System**
   - Book diagnostic tests (`POST /bookings`)
   - View user bookings (`GET /bookings`, `GET /bookings/{id}`)
   - Cancel bookings (`POST /bookings/{id}/cancel`)
   - Booking Lifecycle: `PENDING` ➔ `CONFIRMED` / `FAILED` / `CANCELLED`

4. **Simulated Payment Service**
   - Mock payment endpoint (`POST /payments`) supporting success & failure simulation
   - Automatically updates booking status to `CONFIRMED` or `FAILED`

5. **Idempotent Payment Webhook**
   - Webhook processing endpoint (`POST /payments/webhook/`)
   - Guaranteed **idempotency**: deduplicates repeated events via event ID tracking without corrupting database state or creating duplicate records

6. **Bonus Engineering**
   - **Docker & Docker Compose**: Full containerization setup for backend service & PostgreSQL database
   - **Swagger / OpenAPI Documentation**: Interactive API playground at `/swagger-ui.html`
   - **Comprehensive Unit & Integration Tests**: End-to-end integration test suite using H2 in-memory DB

---

## 🛠️ Tech Stack & Prerequisites

- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 3.2.4 (Spring Data JPA, Spring Security, Spring Validation)
- **Database:** PostgreSQL (Production / Docker) & H2 (In-memory Test Profile)
- **Containerization:** Docker & Docker Compose
- **API Documentation:** SpringDoc OpenAPI 3 / Swagger UI
- **Build Tool:** Apache Maven

---

## 💻 How to Run Locally

### Option 1: Docker Compose (Recommended)

1. Make sure Docker Desktop is running.
2. Clone repository & navigate to root directory:
   ```bash
   git clone <your-repo-url>
   cd EVE_Project
   ```
3. Launch services using Docker Compose:
   ```bash
   docker-compose up --build
   ```
4. Access the server & API docs:
   - Base URL: `http://localhost:8080`
   - Interactive Swagger API Docs: `http://localhost:8080/swagger-ui.html`

---

### Option 2: Local Java Environment (Maven)

1. Prerequisites: JDK 21 installed and PostgreSQL running on localhost:5432 (or H2 in-memory).
2. Run integration tests:
   ```bash
   mvn test
   ```
3. Start the application:
   ```bash
   mvn spring-boot:run
   ```

---

## 📚 API Endpoints Overview & Examples

### 1. Authentication

#### `POST /auth/signup`
**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "password": "securepassword123",
  "fullName": "John Doe"
}
```
**Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": 1,
  "email": "john.doe@example.com",
  "fullName": "John Doe",
  "role": "PATIENT"
}
```

#### `POST /auth/login`
**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "password": "securepassword123"
}
```

---

### 2. Diagnostic Centres & Tests

#### `POST /centres`
**Request Body:**
```json
{
  "name": "Apollo Diagnostic Lab",
  "location": "Indiranagar, Bangalore"
}
```

#### `POST /centres/{centreId}/tests`
**Request Body:**
```json
{
  "name": "Full Body Lipid Profile",
  "description": "Comprehensive cholesterol and triglyceride test",
  "price": 1250.00
}
```

#### `GET /centres`
Retrieves list of centres along with available diagnostic tests and prices.

---

### 3. Booking System (Requires `Authorization: Bearer <JWT>`)

#### `POST /bookings`
**Request Body:**
```json
{
  "centreId": 1,
  "testId": 1,
  "appointmentDateTime": "2026-10-15T10:30:00"
}
```
**Response (201 Created):**
```json
{
  "id": 101,
  "userId": 1,
  "userName": "John Doe",
  "centreId": 1,
  "centreName": "Apollo Diagnostic Lab",
  "testId": 1,
  "testName": "Full Body Lipid Profile",
  "appointmentDateTime": "2026-10-15T10:30:00",
  "amount": 1250.00,
  "status": "PENDING",
  "createdAt": "2026-09-26T21:00:00"
}
```

---

### 4. Simulated Payment Service

#### `POST /payments`
**Request Body (Success):**
```json
{
  "bookingId": 101,
  "simulateFailure": false,
  "paymentMethod": "CREDIT_CARD"
}
```

**Request Body (Failure Simulation):**
```json
{
  "bookingId": 101,
  "simulateFailure": true
}
```

---

### 5. Idempotent Payment Webhook

#### `POST /payments/webhook/`
**Request Body:**
```json
{
  "eventId": "evt_wh_9988776611",
  "eventType": "payment.succeeded",
  "bookingId": 101,
  "transactionId": "TXN_77665544",
  "amount": 1250.00,
  "status": "SUCCESS"
}
```
**Response:**
```json
{
  "success": true,
  "message": "Webhook processed successfully",
  "eventId": "evt_wh_9988776611"
}
```
*If sent a second time with the same `eventId`, returns idempotent response without creating duplicate payments or re-modifying booking state.*

---

## 📊 Database Schema Design

```
+----------------+       +------------------+       +-------------------+
|     users      |       |  diagnostic_     |       |  diagnostic_      |
+----------------+       |  centres         |       |  tests            |
| id (PK)        |       +------------------+       +-------------------+
| email (UNIQUE) |       | id (PK)          |   +---| id (PK)           |
| password       |       | name             |   |   | name              |
| full_name      |       | location         |   |   | price             |
| role           |       +------------------+   |   | centre_id (FK)    |
+----------------+                ^             |   +-------------------+
        ^                         |             |             ^
        |                         +---------+   |             |
        +-----------------------+           |   |             |
                                |           |   |             |
                         +------+-----------+---+--+          |
                         |        bookings         |          |
                         +-------------------------+          |
                         | id (PK)                 |          |
                         | user_id (FK) -----------+          |
                         | centre_id (FK)                     |
                         | test_id (FK) ----------------------+
                         | appointment_date_time   |
                         | amount                  |
                         | status (PENDING, etc.)  |
                         +-------------------------+
                                      ^
                                      |
                         +------------+------------+
                         |        payments         |
                         +-------------------------+
                         | id (PK)                 |
                         | transaction_id (UNIQUE) |
                         | booking_id (FK)         |
                         | amount                  |
                         | status (SUCCESS/FAILED) |
                         +-------------------------+

                         +-------------------------+
                         |   processed_webhooks    |
                         +-------------------------+
                         | id (PK)                 |
                         | event_id (UNIQUE)       |
                         | event_type              |
                         | processed_at            |
                         +-------------------------+
```

---

## 💡 Key Architectural Assumptions & Edge Case Handling

1. **Idempotency Strategy:**
   - Webhook requests record the unique `eventId` in `processed_webhooks`.
   - Subsequent calls with identical `eventId`s bypass transactional updates and safely return an idempotent acknowledgement.

2. **Authorization & Security:**
   - Only authenticated patients can view/cancel their own bookings.
   - Resource access attempt violations return HTTP `403 Forbidden`.

3. **Booking Validation:**
   - Ensures a selected diagnostic test explicitly belongs to the specified diagnostic centre before creating a booking.

---

## 🔮 What I Would Improve With More Time

1. **Redis Caching:** Add Redis cache layer for popular centre & test search queries.
2. **RabbitMQ / Kafka Queue:** Asynchronously process webhooks for heavy traffic workloads.
3. **Advanced Rate Limiting:** Implement token bucket rate-limiting via Bucket4j or Redis.
