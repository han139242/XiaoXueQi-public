---
name: mes-frontend-api-integration
description: Use when connecting PC frontend or WeChat mini program pages to Spring Boot APIs, debugging request paths, token headers, ResultData parsing, CORS, pagination, or frontend-backend data mismatches.
---

# MES Frontend API Integration

## Purpose

Use this after a backend endpoint or frontend page exists and they need to work together.

## Workflow

1. Confirm backend base URL, route, HTTP method, request body/query, and response shape.
2. Confirm frontend caller uses the same path and method.
3. Confirm token header convention: `Authorization: Bearer <token>` when authentication is required.
4. Normalize `ResultData` parsing: code, msg, data.
5. Check pagination naming: `current`, `size`, `records`, `total`.
6. Handle empty data, auth failure, validation failure, and server error.
7. Build frontend and run backend test or smoke test if possible.
8. Record integration status in `COLLABORATION.md`.

## Common Failure Points

- `/auth/login` vs `/api/auth/login`
- `/user/list` vs `/api/sys/user/page`
- token stored but not sent with Bearer prefix
- frontend expects array while backend returns Page object
- CORS or preflight blocked
- backend returns `passwordHash` or sensitive fields that frontend should not display

## Must Not

- Do not fix integration by adding duplicate backend paths unless compatibility is required.
- Do not hardcode local API URLs in many pages; centralize when a request helper exists.
- Do not hide backend errors without showing useful UI feedback.
