# Team Task Manager - Full Stack Assignment

This project contains:
- `backend`: Java Spring Boot REST API with MySQL
- `frontend`: React dashboard UI
- `docker-compose.yml`: Run whole stack in Docker

## Features
- Signup/Login with JWT token
- Role based access (`ADMIN`, `MEMBER`)
- Project management and team member assignment
- Task creation, assignment, full update and status tracking
- Dashboard API with overdue and status counts

## Run with Docker
```bash
docker compose up --build
```

## API Endpoints
- `POST /api/auth/signup`
- `POST /api/auth/login`
- `GET, POST /api/projects`
- `POST /api/projects/{projectId}/members/{userId}`
- `GET /api/projects/{projectId}/members`
- `GET, POST /api/tasks`
- `PUT /api/tasks/{id}`
- `PATCH /api/tasks/{id}/status`
- `GET /api/dashboard`

For protected APIs, pass:
`Authorization: Bearer <token>`
