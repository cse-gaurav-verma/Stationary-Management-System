# 📦 Stationery Management System

A full-stack **microservices-based** Stationery Management System built with **Spring Boot**, **Spring Cloud**, and **React**. The system enables organizations to manage stationery inventory, process purchase/issue requests, and maintain complete audit trails — all through a modern, responsive web interface.

---

## 📐 Architecture Overview

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                              FRONTEND (React)                               │
│                            http://localhost:3000                            │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         API GATEWAY (Spring Cloud)                          │
│                            http://localhost:8090                            │
│               Route: /api/auth/** → AUTH-SERVICE                            │
│               Route: /api/inventory/** → INVENTORY-SERVICE                  │
│               Route: /api/requests/** → REQUEST-SERVICE                     │
│               Cross-Cutting: JWT Validation, CORS, Rate Limiting            │
└──────┬──────────────────────┬──────────────────────┬────────────────────────┘
       │                      │                      │
       ▼                      ▼                      ▼
┌──────────────┐   ┌───────────────────┐   ┌──────────────────┐
│ AUTH SERVICE │   │ INVENTORY SERVICE │   │ REQUEST SERVICE  │
│  :8081       │   │  :8082            │   │  :8083           │
└──────┬───────┘   └────────┬──────────┘   └────────┬─────────┘
       │                    │                       │
       ▼                    ▼                       ▼
┌──────────────┐   ┌───────────────────┐   ┌──────────────────┐
│   auth_db    │   │  inventory_db     │   │   request_db     │
│   (MySQL)    │   │   (MySQL)         │   │   (MySQL)        │
└──────────────┘   └───────────────────┘   └──────────────────┘

                    ┌───────────────────┐
                    │  CONFIG SERVER    │◄── Centralized Configuration
                    │  :8888            │
                    └───────────────────┘

                    ┌───────────────────┐
                    │  EUREKA SERVER    │◄── Service Discovery
                    │  :8761            │
                    └───────────────────┘
```

---

## 🛠️ Technology Stack

- **Frontend:** React 18, React Router, Axios
- **API Gateway:** Spring Cloud Gateway (Reactive)
- **Backend:** Java 17, Spring Boot 3.2.x, Spring Cloud 2023.x
- **Security:** Spring Security, JWT
- **Service Comm.:** OpenFeign
- **Discovery:** Netflix Eureka
- **Configuration:** Spring Cloud Config Server
- **Database:** MySQL 8.0
- **CI/CD:** Jenkins, Docker, Docker Compose

---

## 🚀 Running Locally

### 1. Start MySQL & Databases

Create the required databases in MySQL:
```sql
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS inventory_db;
CREATE DATABASE IF NOT EXISTS request_db;
```

### 2. Start Services (Run in Order)

1. **Config Server** (Port 8888): `cd backend/config-server && mvn spring-boot:run`
2. **Eureka Server** (Port 8761): `cd backend/eureka-server && mvn spring-boot:run`
3. **API Gateway** (Port 8090): `cd backend/api-gateway && mvn spring-boot:run`
4. **Auth Service** (Port 8081): `cd backend/auth-service && mvn spring-boot:run`
5. **Inventory Service** (Port 8082): `cd backend/inventory-service && mvn spring-boot:run`
6. **Request Service** (Port 8083): `cd backend/request-service && mvn spring-boot:run`

### 3. Start Frontend

```bash
cd frontend
npm install
npm start
```
The frontend opens at: [http://localhost:3000](http://localhost:3000)

---

## 🐳 Running with Docker Compose

```bash
# Build and start all services
docker-compose -f ci-cd/docker-compose.yml up --build -d
```

| Service          | URL                                    |
|------------------|----------------------------------------|
| Config Server    | http://localhost:8888/actuator/health  |
| Eureka Dashboard | http://localhost:8761                  |
| API Gateway      | http://localhost:8090/actuator/health  |
| Frontend         | http://localhost:3000                  |

---

## 📚 API Documentation

All API calls must go through the **API Gateway** at `http://localhost:8090`.

### 🔐 Auth Service (`/api/auth`)
- **POST** `/api/auth/register` - Register a new user (`ADMIN` or `STUDENT`).
- **POST** `/api/auth/login` - Login and receive JWT.
- **GET** `/api/auth/validate` - Validate JWT.

### 📦 Inventory Service (`/api/inventory`)
- **GET** `/api/inventory` - Get all inventory items (All roles).
- **POST** `/api/inventory` - Create item (`ADMIN` only).
- **GET** `/api/inventory/low-stock` - Get low-stock items (`ADMIN` only).

### 📝 Request Service (`/api/requests`)
- **POST** `/api/requests` - Create request (`STUDENT` only).
- **GET** `/api/requests/my` - Get current user's requests.
- **PUT** `/api/requests/{id}/approve` - Approve request (`ADMIN` only).

#### Create Request Example
```http
POST /api/requests
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "items": [
    {
      "itemId": 5,
      "itemName": "Blue Ballpoint Pen",
      "quantity": 10
    }
  ]
}
```

---

## 🗄️ Database Schema

### Auth DB (`auth_db`)
- **users:** `id`, `username`, `password`, `email`, `full_name`, `role` (`ADMIN`, `STUDENT`), `enabled`, `created_at`, `updated_at`

### Inventory DB (`inventory_db`)
- **inventory_items:** `id`, `name`, `description`, `category`, `quantity`, `minimum_stock_level`, `unit_price`, `supplier`, `created_at`, `updated_at`

### Request DB (`request_db`)
- **stationery_requests:** `id`, `request_id`, `student_username`, `status`, `rejection_reason`, `admin_username`, `created_at`, `updated_at`
- **request_items:** `id`, `item_id`, `item_name`, `quantity`, `request_id`

---

## 🔄 CI/CD Pipeline

The project includes a `Jenkinsfile` for automated builds and deployment using Docker Compose. The pipeline covers: Checkout, Parallel Maven Builds, Tests, Docker Build & Push, and Deployment.

### Jenkins Prerequisites
- **Maven 3.9+** configured as `Maven` in Global Tool Configuration
- **JDK 21** configured as `JDK21` in Global Tool Configuration

## 🧪 Running Tests
- **Backend:** `cd backend/<service> && mvn test`
- **Frontend:** `cd frontend && npm test`
