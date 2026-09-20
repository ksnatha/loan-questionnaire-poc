# Loan Questionnaire POC

Dynamic Questionnaire Engine for loan applications — 200+ questions, versioned without redeployment.

## Components

| Component | Type | Port | Purpose |
|---|---|---|---|
| `lookup-stub-service` | Spring Boot | 8081 | Reference data (code sets) and external lookup stub |
| `questionnaire-service` | Spring Boot | 8082 | Authors, versions, and publishes section/tab templates |
| `application-service` | Spring Boot | 8083 | Loan applications, answer storage, risk rating |
| `application-ui` | React (Vite) | 5173 | Renders tab sections generically from template JSON |
| `questionnaire-admin-ui` | React (Vite) | 5174 | Business UI for editing and publishing templates |
| `common` | Java library | — | Shared template JSON model classes and DTOs |

## Prerequisites

- Java 21 JDK
- Node.js 20+ and npm
- Gradle (or use the included `gradlew` wrapper once Java is available)

## Running

```bash
# Bootstrap Gradle wrapper (once, requires Gradle installed globally)
gradle wrapper

# Build all backend modules
./gradlew build

# Run a single service
./gradlew :lookup-stub-service:bootRun
./gradlew :questionnaire-service:bootRun
./gradlew :application-service:bootRun

# Run React apps (from their directories)
cd application-ui && npm install && npm run dev
cd questionnaire-admin-ui && npm install && npm run dev
```

## Health endpoints

Each service exposes `/actuator/health` once running:
- http://localhost:8081/actuator/health
- http://localhost:8082/actuator/health
- http://localhost:8083/actuator/health

## Architecture

See `docs/questionnaire-poc-architecture.md` for design decisions and ERD.  
See `docs/questionnaire-poc-ai-agent-instructions.md` for the phase-by-phase build plan.
