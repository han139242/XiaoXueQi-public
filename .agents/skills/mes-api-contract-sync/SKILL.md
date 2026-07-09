---
name: mes-api-contract-sync
description: Use when aligning backend API documents, Spring Boot controllers, request/response fields, frontend calls, route paths, status codes, or resolving API contract mismatches in the badminton MES project.
---

# MES API Contract Sync

## Purpose

Use this when paths, methods, request fields, response formats, or frontend calls do not match.

## Inputs to Compare

- API document under `相关文档/md` or `md`
- Controller annotations and method parameters
- frontend request code
- `ResultData` response shape
- database/entity field names when request data maps to persistent fields

## Workflow

1. Locate the documented API path, method, parameters, and response.
2. Locate the backend Controller and actual route.
3. Locate frontend caller if it exists.
4. Decide the source of truth:
   - working code and database reality first;
   - then `schema.sql`, entity, API document;
   - then collaboration notes.
5. Make the smallest change that aligns all active callers.
6. If the document is outdated, update or record the difference.
7. Validate affected side:
   - backend: Maven test or route-level smoke test;
   - frontend: build or request compile check.

## Contract Rules

- Formal backend APIs use `/api`.
- Pagination uses `GET /page` with `current` and `size`.
- Detail uses `GET /{id}`.
- Create uses `POST /`.
- Update uses `PUT /`.
- Delete uses `DELETE /{id}` and soft delete.
- Response should use `ResultData`.

## Must Not

- Do not change frontend paths without checking backend route.
- Do not change backend paths without checking API document and frontend callers.
- Do not create duplicate APIs for the same action unless compatibility is explicitly required.
