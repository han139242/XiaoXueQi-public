---
name: mes-code-review
description: Use before committing or when reviewing badminton MES changes for bugs, contract mismatches, schema drift, risky business logic, missing validation, unrelated changes, or missing progress updates.
---

# MES Code Review

## Review Order

1. Check `git status --short --branch`.
2. Review staged and unstaged diff separately if both exist.
3. Identify touched layers: docs, backend, database, PC frontend, mini program, hooks.
4. Check the highest-risk layer first:
   - inventory and production quantity changes;
   - schema/entity changes;
   - authentication and JWT;
   - API path changes;
   - frontend build entry and router.
5. Confirm appropriate validation was run.
6. Confirm `COLLABORATION.md` was updated and staged.

## Findings to Prioritize

- data loss or physical delete risk;
- inventory balance without movement record;
- missing transaction on multi-table mutation;
- API path/method mismatch;
- entity primary key mismatch;
- query missing `deleted_flag = 0`;
- frontend import/route/build failure;
- hidden unrelated refactor.

## Review Output

Lead with issues. Use file references when possible. If no issues are found, say so and mention remaining validation gaps.

## Must Not

- Do not focus on style before correctness.
- Do not approve broad unexplained changes.
- Do not ignore dirty worktree entries that may affect the commit.
