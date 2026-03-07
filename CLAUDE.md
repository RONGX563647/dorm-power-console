# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DormPower (宿舍用电管理系统) - An IoT-based smart dormitory power management platform supporting device monitoring, real-time data collection, and intelligent analysis. Deployed on a 2-core 2GB server (117.72.210.10).

## Build and Development Commands

### Backend (Spring Boot)

```bash
cd backend

# Development (uses H2 in-memory database by default with dev profile)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Production build
mvn clean package -DskipTests

# Run tests
mvn test

# Run single test class
mvn test -Dtest=ClassName

# Run single test method
mvn test -Dtest=ClassName#methodName
```

### Frontend (Vue 3)

```bash
cd frontend

# Install dependencies
npm install

# Development server (http://localhost:3000)
npm run dev

# Production build
npm run build

# Type checking
npm run type-check

# Linting
npm run lint
```

### Docker Deployment

```bash
# Production deployment
docker compose -f docker-compose.production.yml --env-file .env.production up -d

# View logs
docker compose -f docker-compose.production.yml logs -f [service_name]

# Stop services
docker compose -f docker-compose.production.yml down
```

## Architecture

### Backend Structure

```
backend/src/main/java/com/dormpower/
├── config/          # Configuration classes (Security, MQTT, WebSocket, Cache, Async)
├── controller/      # REST API endpoints
├── service/         # Business logic layer
├── repository/      # Spring Data JPA repositories
├── model/           # JPA entities
├── dto/             # Data Transfer Objects
├── mqtt/            # MQTT bridge for IoT device communication
├── websocket/       # WebSocket handlers for real-time updates
├── scheduler/       # Scheduled tasks (metrics, device monitoring)
├── annotation/      # Custom annotations (RateLimit, AuditLog)
└── exception/       # Exception handling
```

### Frontend Structure

```
frontend/src/
├── api/             # API client (axios-based)
├── components/      # Reusable Vue components
├── views/           # Page components (route-based)
├── router/          # Vue Router configuration
├── stores/          # Pinia state management
├── types/           # TypeScript type definitions
└── utils/           # Utility functions
```

### Key Architectural Patterns

1. **Device Communication Flow**:
   - IoT devices → MQTT Broker (Mosquitto) → MqttBridge → Database → WebSocket → Frontend
   - Commands: Frontend → REST API → MqttBridge → MQTT Broker → IoT devices

2. **Real-time Updates**: WebSocketManager singleton manages subscriptions with device-specific channels using ConcurrentHashMap and CopyOnWriteArraySet for thread-safe operations.

3. **Authentication**: JWT-based with token blacklist support. Protected routes use `@PreAuthorize` annotations.

4. **Concurrency**: Heavy use of JUC components (ExecutorService, ConcurrentHashMap, Atomic classes, volatile) for handling 10,000+ concurrent device connections.

5. **Multi-Profile Configuration**:
   - `dev`: H2 database, debug logging
   - `local`: Local PostgreSQL
   - `prod`: Production PostgreSQL with optimized connection pool
   - `low-memory`: Reduced resource usage for small servers

### MQTT Topics

| Topic Pattern | Purpose |
|--------------|---------|
| `dorm/status/{deviceId}` | Device status updates |
| `dorm/telemetry/{deviceId}` | Telemetry data |
| `dorm/cmd/{deviceId}` | Commands to devices |
| `dorm/ack/{deviceId}` | Command acknowledgments |
| `dorm/event/{deviceId}` | Device events |

### API Endpoints

- Auth: `/api/auth/login`, `/api/auth/logout`, `/api/auth/me`
- Devices: `/api/devices`, `/api/devices/{id}/status`
- Commands: `/api/strips/{id}/cmd`, `/api/cmd/{cmdId}`
- Telemetry: `/api/telemetry`, `/api/strips/{id}/telemetry`
- AI Reports: `/api/rooms/{room_id}/ai_report`
- WebSocket: `/ws`

## Environment Variables

Backend requires these environment variables (see `.env.example`):
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRATION`
- `MQTT_BROKER_URL`, `MQTT_CLIENT_ID`, `MQTT_USERNAME`, `MQTT_PASSWORD`
- `SPRING_PROFILES_ACTIVE`

## Important Notes

- The project uses **Spring Boot 3.2.x** which requires **Jakarta EE** (not javax.*) for annotations
- Database migrations are handled by Hibernate's `ddl-auto=update` in production
- WebSocket uses raw Spring WebSocket (not STOMP) with custom JSON message protocol
- Rate limiting is implemented using Guava RateLimiter with `@RateLimit` annotation
- The server has limited resources (2-core 2GB), so JVM heap is configured to 384-512MB