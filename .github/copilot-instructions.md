# GitHub Copilot Instructions

本仓库是羽毛球 MES 系统。后端主要使用 Spring Boot，PC 前端使用 Vue，后续包含微信小程序端，数据库使用 MySQL。

## Before Coding

- Read `COLLABORATION.md`.
- Locate current documents with `rg --files -g "*.md"` because documents may be moved during collaboration.
- Prefer existing code and `schema.sql` over guesses.
- Keep changes scoped to one module or one feature.

## Backend

- Backend root: `workspace/idea/ems`.
- Use `/api` prefix for formal APIs.
- Use `ResultData` for API responses.
- Keep layers: `controller`, `service`, `service.impl`, `mapper`, `entity`.
- Use MyBatis-Plus for simple CRUD.
- Filter `deleted_flag = 0` by default.
- Use soft delete for delete APIs.
- Keep `schema.sql`, entity fields, mapper SQL, and API fields aligned.

## Frontend

- PC frontend root: `workspace/vscode/ems-forword`.
- Keep pages organized by business module.
- Verify route imports and component imports.
- Match frontend requests to backend API contracts.
- Run `npm run build` after meaningful frontend changes.

## Database and MES Rules

- MySQL is the database baseline.
- Do not physically delete production, inventory, quality, or traceability data.
- Inventory changes must preserve balance and movement records.
- Status values should use stable English codes.

## Project Skills

For complex tasks, read the matching project skill under `.agents/skills/<skill-name>/SKILL.md` before suggesting or editing code.

Common skills: `mes-backend-resource`, `mes-backend-business-flow`, `mes-api-contract-sync`, `mes-mysql-schema-check`, `mes-pc-frontend-page`, `mes-wechat-miniprogram`, `mes-frontend-api-integration`, `mes-debug-fix`, `mes-test-verification`, `mes-code-review`, `mes-progress-update`.

## Output

When finishing a task, summarize changed files, validation commands, validation results, and remaining risks.
