# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Appointment System (预约系统) is a merchant-to-customer appointment booking platform where merchants create appointment tasks and users book via signed links. It's a full-stack application with Spring Boot backend and Vue 3 frontend.

## Common Commands

### Backend (Java 21 + Spring Boot 4.0.2)

```bash
# Build the project
./mvnw clean compile -DskipTests

# Run development server
./mvnw spring-boot:run

# Run tests
./mvnw test

# Run single test class
./mvnw test -Dtest=ClassNameTest

# Package for production
./mvnw clean package -DskipTests
```

### Frontend (Vue 3 + TypeScript + Vite)

```bash
cd frontend

# Install dependencies
npm install

# Development server (http://localhost:5173)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint

# Type check
npm run type-check
```

### Development Environment

```bash
# Initialize and start development environment (runs backend + waits for health check)
./init.sh
```

The backend runs on http://localhost:8080 and frontend on http://localhost:5173. API proxy is configured in Vite to forward `/api` requests to the backend.

## Architecture

### Backend (Spring Boot Monolithic)

```
org.example.appointment_system/
├── config/          # Configuration classes (Security, Redis, RabbitMQ, OpenAPI)
├── controller/     # REST controllers (Auth, User, Merchant, Booking, Admin)
├── service/        # Business logic services
├── repository/     # Spring Data JPA repositories
├── entity/         # JPA entities (User, MerchantProfile, ServiceItem, AppointmentTask, Booking)
├── dto/            # Request/Response DTOs
├── enums/          # Enums (UserRole, BookingStatus, ServiceCategory)
├── exception/      # Global exception handling
├── filter/         # Request filters (RateLimitFilter)
└── util/           # Utilities (SignedLinkUtils)
```

### Frontend (Vue 3 SPA)

```
frontend/src/
├── api/           # Axios API modules (auth, merchant, service, booking)
├── assets/        # Styles and images
├── components/    # Reusable components (common/, business/)
├── composables/   # Vue composables (useAuth, useNotification)
├── layouts/       # Layout wrappers (Default, Merchant, Admin)
├── router/        # Vue Router configuration with guards
├── stores/        # Pinia stores (auth, merchant, booking)
├── types/         # TypeScript interfaces
├── utils/         # Utilities (request wrapper, date helpers)
└── views/         # Page components (auth/, merchant/, user/, admin/)
```

### Infrastructure

- **MySQL 8.0** (port 3306): Main database, managed by Flyway migrations in `src/main/resources/db/migration`
- **Redis 7.x** (port 6379): Session storage, caching, rate limiting
- **RabbitMQ 3.12** (port 5672): Async booking queue
- **Spring Cloud Gateway** (port 8080): API gateway with rate limiting and routing

### Authentication

- Uses Spring Session with Redis (`spring-session-data-redis`)
- Session cookie: `SESSION`
- Roles: `USER`, `MERCHANT`, `ADMIN`
- External booking links use HMAC-SHA256 signed URLs: `/book/{taskId}?token={signature}&exp={expiry}`

## Key Files to Know

- `pom.xml`: Maven dependencies (Spring Boot 4.0.2, Spring Cloud Gateway, Spring Data JPA/Redis/RabbitMQ)
- `frontend/package.json`: NPM dependencies (Vue 3.4+, Element Plus 2.x, Pinia 2.x)
- `src/main/resources/application.yml`: Backend configuration (database, redis, rabbitmq, security)
- `docs/feature_list.json`: Feature tracking with pass/fail status
- `docs/api.md`: API documentation
- `docs/database-design.md`: Database schema and design

## Development Workflow

1. Run `./init.sh` to start the backend
2. Run `npm run dev` in `frontend/` for the frontend
3. Check `docs/feature_list.json` for pending features to implement
4. Write tests for new functionality
5. Commit with descriptive messages following: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`

## API Base

- Backend: http://localhost:8080
- API prefix: `/api`
- Health check: http://localhost:8080/actuator/health
- API Docs: http://localhost:8080/swagger-ui.html (when enabled)
