---
name: mes-backend-resource
description: Use when implementing or modifying a normal Spring Boot backend resource module for the badminton MES system, especially CRUD APIs, pagination, soft delete, mapper/service/controller/entity alignment, or MyBatis-Plus resource endpoints.
---

# MES Backend Resource

## Scope

Use this for ordinary management resources such as users, units, suppliers, customers, materials, products, workshops, lines, equipment categories, dictionaries, and similar CRUD-like modules.

For production execution, inventory deduction, quality handling, or traceability workflows, use `mes-backend-business-flow`.

## Required Reading

- `COLLABORATION.md`
- API document for the module
- `schema.sql` table definition
- Existing entity, mapper, service, controller patterns in `workspace/idea/ems`

## Implementation Steps

1. Confirm table name, primary key, fields, `deleted_flag`, and status fields.
2. Check whether Entity, Mapper, Service, ServiceImpl, Controller already exist.
3. Reuse existing naming and package structure.
4. Use MyBatis-Plus for simple CRUD and pagination.
5. Implement standard endpoints only when needed:
   - `GET /page`
   - `GET /{id}`
   - `POST /`
   - `PUT /`
   - `DELETE /{id}` as soft delete
   - `GET /excel` only if export is requested or already part of the module pattern
6. Filter `deleted_flag = 0` for list/page queries.
7. Return `ResultData` consistently.
8. Validate with Maven test or a smaller compile/test command if appropriate.
9. Update `COLLABORATION.md` before commit.

## Quality Bar

- Controller only handles request/response and parameter basics.
- Service owns business decisions.
- Mapper names express business meaning.
- No sample SQL or placeholder endpoint remains.
- API path matches document or documented project convention.

## Must Not

- Do not bypass Service by wiring multiple Mappers into Controller.
- Do not physically delete business data.
- Do not add new dependencies for routine CRUD.
- Do not invent fields not present in `schema.sql`.
