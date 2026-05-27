# Perform AI — Backend Service

> Athlete biomechanical analysis API built with Spring Boot.
> Accepts athlete data, processes it asynchronously, and returns computed motion metrics.

**Author:** Md Ehteshamul Haque Tamvir
**Email:** mtamvir@gmail.com

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [API Reference](#api-reference)
- [Request & Response Examples](#request--response-examples)
- [Async Processing Flow](#async-processing-flow)
- [Security — OWASP Top 10](#security--owasp-top-10)
- [Configuration](#configuration)
- [Getting Started](#getting-started)
- [Running the App](#running-the-app)
- [Error Handling](#error-handling)
- [Rate Limiting](#rate-limiting)
- [Production Checklist](#production-checklist)

---

## Overview

Perform AI Backend exposes a REST API for submitting athlete analysis jobs and polling their results.
Processing is simulated asynchronously using a dedicated thread pool — designed to be replaced with
real ML model inference or an external processing pipeline.

Key design goals:
- Clean layered architecture (controller → service → model)
- Standard async HTTP pattern (202 Accepted + Location header + polling)
- OWASP Top 10 security controls for unauthenticated public endpoints
- Structured logging throughout the request lifecycle
- Consistent error response shape across all failure scenarios

---

## Tech Stack

| Component        | Technology                        |
|------------------|-----------------------------------|
| Language         | Compiled to Java 21 (classfile target); runs on JDK 25 at runtime |
| Framework        | Spring Boot 3.4.0                 |
| Validation       | Jakarta Bean Validation           |
| Security         | Spring Security 6                 |
| Rate Limiting    | Bucket4j 8.10.1 (token bucket)    |
| Async Processing | Spring @Async + ThreadPoolTaskExecutor |
| Build Tool       | Maven 3.9.x                       |
| Server           | Embedded Apache Tomcat            |

---

## Project Structure

```
backend/
├── pom.xml
└── src/main/
    ├── java/com/performai/
    │   ├── PerformAiApplication.java          # Entry point, @EnableAsync
    │   ├── config/
    │   │   ├── AsyncConfig.java               # Named thread pool configuration
    │   │   └── SecurityConfig.java            # Security headers, CSRF, session policy
    │   ├── controller/
    │   │   └── AnalysisController.java        # POST /api/v1/analysis, GET /api/v1/analysis/{id}
    │   ├── service/
    │   │   └── AnalysisService.java           # Job lifecycle, async processing
    │   ├── model/
    │   │   ├── AnalysisJob.java               # Job domain model (thread-safe)
    │   │   ├── AnalysisResult.java            # Immutable result record
    │   │   └── AnalysisStatus.java            # PENDING / COMPLETED enum
    │   ├── filter/
    │   │   └── RateLimitFilter.java           # Per-IP rate limiting (OWASP A04)
    │   └── exception/
    │       ├── GlobalExceptionHandler.java    # Centralised error handling
    │       └── JobNotFoundException.java      # 404 domain exception
    └── resources/
        └── application.properties            # Server and security configuration
```

---

## Architecture

```
Client
  │
  │  POST /api/v1/analysis
  ▼
RateLimitFilter          ← per-IP token bucket (20 req/min)
  │
SecurityFilterChain      ← security headers, CSRF disabled, stateless session
  │
AnalysisController       ← validates input (@Valid, @NotBlank, @Size)
  │
AnalysisService.submit() ← creates job, stores in ConcurrentHashMap
  │                         returns job immediately (PENDING)
  ├──► AnalysisService.processAsync()   ← @Async on "analysisExecutor" thread pool
  │         │  sleeps 2s (mock)
  │         └► job.complete(result)     ← flips status to COMPLETED atomically
  │
  ▼
202 Accepted + Location: /api/v1/analysis/{id}

  ─────────────────────────────────────────────

Client polls GET /api/v1/analysis/{id}
  │
AnalysisController.getStatus()
  │
AnalysisService.getById()
  │
200 OK  →  status: PENDING  (metrics: null)
        →  status: COMPLETED (metrics: { foot_contact, foot_off, turning_point })
```

---

## API Reference

### Base URL

```
http://localhost:8080/api/v1
```

### Endpoints

| Method | Path                  | Description                        | Auth     |
|--------|-----------------------|------------------------------------|----------|
| POST   | `/analysis`           | Submit a new analysis job          | None     |
| GET    | `/analysis/{id}`      | Poll status and result of a job    | None     |

---

### POST /api/v1/analysis

Submits a new athlete analysis job. The job is processed asynchronously.

**Request Headers**

| Header         | Value              |
|----------------|--------------------|
| Content-Type   | application/json   |

**Request Body**

| Field    | Type   | Required | Constraints              |
|----------|--------|----------|--------------------------|
| athlete  | string | Yes      | Not blank, max 100 chars |

**Responses**

| Status | Description                                      |
|--------|--------------------------------------------------|
| 202    | Job accepted. Location header points to poll URL |
| 400    | Validation failed (blank or oversized input)     |
| 429    | Rate limit exceeded (max 20 req/min per IP)      |
| 500    | Unexpected server error                          |

**Response Headers**

| Header    | Example                                  |
|-----------|------------------------------------------|
| Location  | `http://localhost:8080/api/v1/analysis/{id}` |

---

### GET /api/v1/analysis/{id}

Polls the current status and result of an existing analysis job.

**Path Parameters**

| Parameter | Type   | Description        |
|-----------|--------|--------------------|
| id        | string | UUID of the job    |

**Responses**

| Status | Description                                      |
|--------|--------------------------------------------------|
| 200    | Job found. Returns current state and metrics     |
| 404    | No job found for the given ID                    |
| 429    | Rate limit exceeded                              |
| 500    | Unexpected server error                          |

---

## Request & Response Examples

### Submit a job

```bash
curl -X POST http://localhost:8080/api/v1/analysis \
  -H "Content-Type: application/json" \
  -d '{"athlete": "Demo Athlete"}'
```

**Response — 202 Accepted**

```json
{
  "id": "a3f1c2d4-87b2-4e10-9c3a-1f2e3d4c5b6a",
  "athlete": "Demo Athlete",
  "status": "PENDING",
  "submittedAt": "2025-09-01T10:00:00.000Z",
  "metrics": null
}
```

**Response Headers**
```
Location: http://localhost:8080/api/v1/analysis/a3f1c2d4-87b2-4e10-9c3a-1f2e3d4c5b6a
```

---

### Poll job status — PENDING

```bash
curl http://localhost:8080/api/v1/analysis/a3f1c2d4-87b2-4e10-9c3a-1f2e3d4c5b6a
```

```json
{
  "id": "a3f1c2d4-87b2-4e10-9c3a-1f2e3d4c5b6a",
  "athlete": "Demo Athlete",
  "status": "PENDING",
  "submittedAt": "2025-09-01T10:00:00.000Z",
  "metrics": null
}
```

---

### Poll job status — COMPLETED

```json
{
  "id": "a3f1c2d4-87b2-4e10-9c3a-1f2e3d4c5b6a",
  "athlete": "Demo Athlete",
  "status": "COMPLETED",
  "submittedAt": "2025-09-01T10:00:00.000Z",
  "metrics": {
    "foot_contact": 0.32,
    "foot_off": 1.08,
    "turning_point": 1.22
  }
}
```

---

### Validation error — 400

```bash
curl -X POST http://localhost:8080/api/v1/analysis \
  -H "Content-Type: application/json" \
  -d '{"athlete": ""}'
```

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "athlete: athlete name must not be blank",
  "timestamp": "2025-09-01T10:00:00.000Z"
}
```

---

### Job not found — 404

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Analysis job not found: unknown-id",
  "timestamp": "2025-09-01T10:00:00.000Z"
}
```

---

### Rate limit exceeded — 429

```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Max 20 requests per minute."
}
```

**Response Headers**
```
Retry-After: 60
```

---

## Async Processing Flow

```
1. Client sends POST /api/v1/analysis
2. Controller validates input
3. Service creates AnalysisJob (status: PENDING) and stores it
4. Service dispatches processAsync() to "analysisExecutor" thread pool
5. Controller immediately returns 202 + Location header
6. Background thread sleeps 2s (mock), then calls job.complete(result)
7. job.complete() writes result first, then atomically flips status to COMPLETED
8. Client polls GET /api/v1/analysis/{id} until status is COMPLETED
```

**Thread Pool Configuration**

| Setting       | Value                  |
|---------------|------------------------|
| Core threads  | 4                      |
| Max threads   | 10                     |
| Queue capacity| 100 jobs               |
| Thread prefix | `analysis-worker-`     |

---

## Security — OWASP Top 10

| #    | Risk                        | Status      | Implementation                                                                 |
|------|-----------------------------|-------------|--------------------------------------------------------------------------------|
| A03  | Injection                   | Mitigated   | `@NotBlank` + `@Size(max=100)` on all inputs. No DB or shell calls.            |
| A04  | Insecure Design             | Mitigated   | `RateLimitFilter` — 20 req/min per IP via token bucket (Bucket4j)              |
| A05  | Security Misconfiguration   | Mitigated   | Security headers enforced. Stack traces suppressed. Actuator disabled.         |
| A07  | Auth & Identification       | Placeholder | `SecurityConfig` in place. Add JWT/OAuth2 `authorizeHttpRequests()` rules.     |
| A08  | Software & Data Integrity   | Mitigated   | Input size cap + `max-http-form-post-size=1MB` at server level                 |
| A09  | Logging & Monitoring        | Mitigated   | Structured `log.info/warn/error` in controller, service, and exception handler |
| A01  | Broken Access Control       | Placeholder | Open for mock. Enforce with JWT/OAuth2 before production.                      |

**Security Headers applied on every response**

| Header                      | Value                                    | Purpose                    |
|-----------------------------|------------------------------------------|----------------------------|
| X-Content-Type-Options      | nosniff                                  | Prevents MIME sniffing     |
| X-Frame-Options             | DENY                                     | Prevents clickjacking      |
| Strict-Transport-Security   | max-age=31536000; includeSubDomains      | Enforces HTTPS             |
| Content-Security-Policy     | default-src 'none'; frame-ancestors 'none' | Restricts resource loading |

---

## Configuration

All configuration is in `src/main/resources/application.properties`.

```properties
# Server
server.port=8080

# OWASP A05 — suppress internal error details from responses
server.error.include-message=never
server.error.include-stacktrace=never
server.error.include-exception=false
server.error.include-binding-errors=never

# OWASP A05 — disable actuator endpoint exposure
management.endpoints.web.exposure.exclude=*

# OWASP A08 — limit request body size
spring.servlet.multipart.max-request-size=1MB
spring.servlet.multipart.max-file-size=1MB
server.tomcat.max-http-form-post-size=1MB
```

---

## Getting Started

### Prerequisites

| Tool  | Version  | Download                                      |
|-------|----------|-----------------------------------------------|
| JDK   | 25 (LTS) | https://adoptium.net/temurin/releases         |
| Maven | 3.9.x    | https://maven.apache.org/download.cgi         |

### Clone the repository

```bash
git clone https://github.com/<your-username>/perform-ai.git
cd perform-ai/backend
```

---

## Running the App

Note: this project is configured to compile to Java 21 bytecode (`maven.compiler.release=21`) so the build is compatible with older classfile readers while allowing you to run on a newer JDK (tested on JDK 25).

### With Maven (recommended for development)

```bash
# use a JDK 25 runtime but the project will be compiled to Java 21 classfiles
$env:JAVA_HOME='D:\jdk25\jdk-25.0.3+9'
$env:PATH='D:\jdk25\jdk-25.0.3+9\bin;D:\maven\apache-maven-3.9.6\bin;' + $env:PATH
mvn -DskipTests spring-boot:run
```

### Build and run the fat JAR

```bash
mvn -DskipTests clean package
java -jar target/perform-ai-backend-0.0.1-SNAPSHOT.jar
```

The server starts on http://localhost:8080 by default.

### Stopping the app (Windows)

If the process still holds port 8080, find and kill it:

```powershell
# find listeners on port 8080
netstat -ano | findstr :8080
# kill process by PID
taskkill /PID <pid> /F
```

If you'd like to switch to producing true Java 25 classfiles later, update `maven.compiler.release` and ensure your Spring Boot / Spring Framework version includes ASM support for Java 25.

---

## Deployment

### 1. Build the production artifact

```bash
mvn clean package -DskipTests
```

- This creates a runnable JAR in `target/`.
- Confirm the artifact exists: `target/perform-ai-backend-0.0.1-SNAPSHOT.jar`

### 2. Configure environment variables

Use environment variables to separate configuration from code.

For a production profile, set:

```bash
export SPRING_PROFILES_ACTIVE=prod
export SERVER_PORT=8080
```

On Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'prod'
$env:SERVER_PORT = '8080'
```

If you need a custom configuration file, use:

```bash
java -jar target/perform-ai-backend-0.0.1-SNAPSHOT.jar --spring.config.location=classpath:/application.properties,./config/application-prod.properties
```

### 3. Deploy as a standalone service

Run the JAR on a host with Java 25 installed:

```bash
nohup java -jar target/perform-ai-backend-0.0.1-SNAPSHOT.jar > perform-ai.log 2>&1 &
```

For Windows, use a service manager or PowerShell job.

### 4. Deploy with Docker (recommended for cloud and container platforms)

Create a lightweight `Dockerfile` in `backend/`:

```dockerfile
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY target/perform-ai-backend-0.0.1-SNAPSHOT.jar /app/app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Build and run the container:

```bash
docker build -t perform-ai-backend:latest .
docker run -d --name perform-ai-backend -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  perform-ai-backend:latest
```

### 5. Deploy to cloud infrastructure

Recommended deployment targets:

- Kubernetes / OpenShift
- AWS ECS / EKS
- Azure App Service / Azure Container Apps
- Google Cloud Run

Key production deployment steps:

1. Push the Docker image to a registry (Docker Hub, GitHub Container Registry, ECR, ACR, etc.).
2. Supply environment-specific configuration through secrets or config maps.
3. Ensure HTTPS is terminated at the load balancer or ingress.
4. Enable health checks against `/actuator/health` or a custom readiness endpoint if available.

### 6. Verify deployment

- Confirm the service responds: `curl http://<host>:8080/api/v1/analysis`
- Validate the rate limit and error handling by checking `429` / `400` / `404` flows.
- Review logs for startup success and thread pool initialization.

### 7. Production readiness checklist

Verify the following before directing traffic to the deployment:

- [ ] Use a persistent backing store for analysis jobs instead of in-memory storage.
- [ ] Secure the application with authentication and authorization.
- [ ] Serve the app behind HTTPS/TLS.
- [ ] Set appropriate resource limits and replicas for the environment.
- [ ] Configure centralized logging and monitoring.
- [ ] Configure a rollback plan and versioned deployment pipeline.

---

## Error Handling

All errors return a consistent JSON shape:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Analysis job not found: <id>",
  "timestamp": "2025-09-01T10:00:00.000Z"
}
```

| Scenario                  | HTTP Status | Handler                          |
|---------------------------|-------------|----------------------------------|
| Job ID not found          | 404         | `JobNotFoundException`           |
| Blank / oversized input   | 400         | `MethodArgumentNotValidException`|
| Rate limit exceeded       | 429         | `RateLimitFilter`                |
| Any unhandled exception   | 500         | `GlobalExceptionHandler`         |

Stack traces are **never** sent to the client — logged server-side only.

---

## Rate Limiting

- **Algorithm:** Token bucket (Bucket4j)
- **Limit:** 20 requests per minute per IP address
- **Scope:** All `/api/v1/` paths
- **Response on breach:** `429 Too Many Requests` + `Retry-After: 60` header
- **IP resolution:** Reads `X-Forwarded-For` header first (reverse proxy support), falls back to `RemoteAddr`

---

## Production Checklist

Before deploying to production, ensure the following:

- [ ] Replace in-memory `ConcurrentHashMap` job store with a persistent database (PostgreSQL, Redis, etc.)
- [ ] Replace mock `Thread.sleep()` in `processAsync()` with real ML model inference or pipeline call
- [ ] Add JWT or OAuth2 authentication in `SecurityConfig.authorizeHttpRequests()`
- [ ] Enable HTTPS / TLS termination at the load balancer or configure Spring SSL
- [ ] Tune thread pool settings (`corePoolSize`, `maxPoolSize`, `queueCapacity`) based on load
- [ ] Tune rate limit (`MAX_REQUESTS_PER_MINUTE`) based on expected traffic
- [ ] Replace in-memory rate limit buckets with distributed store (Redis + Bucket4j Redis integration)
- [ ] Configure centralised log aggregation (ELK, CloudWatch, Datadog, etc.)
- [ ] Set `spring.boot.admin` or equivalent for health monitoring
- [ ] Review and tighten `Content-Security-Policy` header for your frontend domain
