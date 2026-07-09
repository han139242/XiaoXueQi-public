---
name: mes-pc-frontend-page
description: Use when creating or modifying PC-side Vue pages, routes, forms, tables, dialogs, API calls, or Element Plus UI for the badminton MES management frontend.
---

# MES PC Frontend Page

## Scope

Use this for `workspace/vscode/ems-forword` PC management UI work.

## Required Reading

- related backend API document and Controller
- existing `src/router`, `src/pages`, `src/style.css`
- existing component and Element Plus usage

## Workflow

1. Confirm target module and API path.
2. Place page under a module-oriented folder in `src/pages`.
3. Register route in router without breaking existing routes.
4. Build a practical management UI: table, search/filter, create/edit dialog or form, delete confirmation, loading/error states where useful.
5. Keep labels aligned with MES business terms.
6. Use existing dependencies and Element Plus components.
7. Keep global style changes minimal.
8. Run `npm run build`.
9. Update `COLLABORATION.md` with page status and integration notes.

## UI Quality Bar

- The first screen should be usable, not a marketing page.
- Tables and forms should be dense, readable, and business-focused.
- Buttons should use clear action labels or icons when available.
- Text must not overflow or overlap on common desktop widths.
- Route imports and component imports must resolve.

## Must Not

- Do not create placeholder-only pages and mark them complete.
- Do not hardcode backend responses when an API exists.
- Do not introduce a new UI library without explicit approval.
