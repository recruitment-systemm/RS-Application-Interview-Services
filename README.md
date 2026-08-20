# Application & Interview Services

Spring Boot microservice for managing candidate job applications and interviews in the recruitment management platform.

## Key Features

- Submit job applications with resume upload
- List and retrieve applications, update application status
- Schedule, retrieve, complete, and cancel interviews
- List interviews by application or by interviewer
- Integrates with `job-service` to resolve job details
- JWT-based request authentication
- File uploads via Cloudinary

## Tech Stack

- Java 21, Spring Boot 4.1.0
- Spring Web, Spring Data JPA, Spring Security
- PostgreSQL + Flyway migrations
- JWT (jjwt 0.12.6)
- Cloudinary (file uploads)
- Lombok
- Maven

## Prerequisites

- Java 21
- Docker (for PostgreSQL)
- A running `job-service` instance (for job data lookups)

## Setup

Start the database:

```bash
docker compose up -d
```

This starts PostgreSQL on port **5435**. Credentials can be overridden with `DB_USERNAME` / `DB_PASSWORD` (default `postgres`/`postgres`).

Run the application with the `dev` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The service starts on port **8083**.

## Environment Variables

Set these before running with the `dev` profile (see `src/main/resources/application-dev.yaml`):

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | JWT signing secret, used to validate tokens issued by authentication-service |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret |

By default, this service calls `job-service` at `http://localhost:8082` (`services.job-service.url` in `application-dev.yaml`).

## API Documentation

Interactive Swagger UI is available at:

```
http://localhost:8083/docs
```

Raw OpenAPI spec: `http://localhost:8083/v3/api-docs`

## API Endpoints

### Applications (`/api/v1/applications`)

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/applications` | Submit an application (multipart, resume upload) |
| GET | `/api/v1/applications` | List applications |
| GET | `/api/v1/applications/{applicationId}` | Get application by ID |
| PATCH | `/api/v1/applications/{applicationId}/status` | Update application status |

### Interviews (`/api/v1/interviews`)

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/interviews` | Schedule an interview |
| GET | `/api/v1/interviews/{id}` | Get interview by ID |
| GET | `/api/v1/interviews/application/{applicationId}` | List interviews for an application |
| GET | `/api/v1/interviews` | List all interviews |
| GET | `/api/v1/interviews/interviewer/{interviewerId}` | List interviews for an interviewer |
| PATCH | `/api/v1/interviews/{id}/complete` | Mark interview as completed |
| PATCH | `/api/v1/interviews/{id}/cancel` | Cancel an interview |

## Database

PostgreSQL, managed with Flyway migrations in `src/main/resources/database/migrations`:

- `V1__create_applications_and_interviews.sql`
