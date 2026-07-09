---
name: mes-debug-fix
description: Use when diagnosing and fixing build failures, runtime exceptions, API errors, frontend route/import issues, database connection problems, MyBatis mapper errors, or failing tests in the badminton MES project.
---

# MES Debug Fix

## Workflow

1. Reproduce or read the exact error output.
2. Identify the failing layer: frontend build, backend compile/test, runtime API, database, mapper, authentication, or Git workflow.
3. Read the directly related files before editing.
4. Make the smallest fix that addresses the root cause.
5. Re-run the command that failed.
6. If the fix reveals a contract or schema mismatch, use the matching skill for that layer.
7. Record non-obvious causes or remaining risk in `COLLABORATION.md`.

## Evidence to Collect

- command run and working directory;
- exact error message;
- changed file names;
- before/after route or field name when relevant;
- verification result.

## Typical MES Issues

- missing Vue import or unregistered route;
- Controller path mismatch with frontend;
- MyBatis mapper namespace mismatch;
- `@TableId` not matching SQL primary key;
- missing `deleted_flag` condition;
- database URL or local MySQL version mismatch;
- JWT filter blocking login or docs path.

## Must Not

- Do not make broad rewrites while fixing one bug.
- Do not suppress exceptions without understanding the cause.
- Do not mark a fix done before rerunning the failing command when it is feasible.
