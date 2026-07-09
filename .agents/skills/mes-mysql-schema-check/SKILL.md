---
name: mes-mysql-schema-check
description: Use when modifying or validating MySQL schema.sql, Java entities, MyBatis mappers, table names, primary keys, field types, or database consistency for the badminton MES project.
---

# MES MySQL Schema Check

## Purpose

Use this to prevent schema/entity drift. It is required when touching `schema.sql`, entity classes, mapper SQL, table names, primary keys, or core database fields.

## Checks

1. Count `CREATE TABLE` entries in `schema.sql`.
2. Count entity classes with `@TableName`.
3. Compare table names between SQL and entities.
4. Compare SQL primary keys with `@TableId`.
5. Spot-check important type mapping:
   - `BIGINT` -> `Long`
   - `DECIMAL` -> `BigDecimal`
   - `DATE` -> `LocalDate`
   - `DATETIME` -> `LocalDateTime`
   - `TINYINT` -> `Integer` unless Boolean is explicitly designed
6. Confirm `deleted_flag` handling for business tables.

## Useful Commands

Run from repository root:

```powershell
(Select-String -Path workspace\idea\ems\src\main\resources\db\schema.sql -Pattern '^CREATE TABLE `' | Measure-Object).Count
(rg -n "@TableName\(" workspace\idea\ems\src\main\java\com\neuedu\ems\entity | Measure-Object).Count
(rg -n "@TableId\(" workspace\idea\ems\src\main\java\com\neuedu\ems\entity | Measure-Object).Count
```

## Fix Workflow

1. Determine whether SQL or entity is wrong.
2. Prefer `schema.sql` and verified database design as baseline.
3. Fix entity annotations and fields before mapper/controller code.
4. Re-run the counts and Maven test.
5. Record any intentional schema change in `COLLABORATION.md`.

## Must Not

- Do not silently add fields only in Java or only in SQL.
- Do not change primary keys without checking all affected entities and mappers.
- Do not use ad hoc string mapping for structured SQL inspection when normal tooling is available.
