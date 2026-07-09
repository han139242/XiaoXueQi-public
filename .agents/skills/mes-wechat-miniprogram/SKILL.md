---
name: mes-wechat-miniprogram
description: Use when creating or modifying the WeChat mini program side of the badminton MES project, including mobile pages, API integration, login/session handling, scanning workflows, task/report views, or lightweight shop-floor operations.
---

# MES WeChat Mini Program

## Scope

Use this for future mobile scenarios such as task viewing,扫码追溯, production reporting, inspection entry, warehouse confirmation, equipment repair reporting, or dashboard snippets.

## Planning Rules

1. Keep mini program functions smaller than PC management pages.
2. Prefer shop-floor operations: scan, confirm, report, inspect, view task.
3. Reuse backend `/api` contracts where possible.
4. Confirm authentication/token strategy before coding login-dependent pages.
5. Keep directory independent from the PC frontend.

## Page Quality Bar

- One primary task per page.
- Clear loading, empty, and error states.
- Avoid dense admin tables on mobile.
- Use stable business identifiers: task no, work order no, batch no, barcode.
- Treat scanning and network failures as normal states.

## Integration Checklist

- API path matches backend.
- Token/header behavior matches PC and backend.
- Request/response field names match `ResultData`.
- No hardcoded user or demo token remains.
- Mobile workflow records required business state or trace data.

## Must Not

- Do not duplicate backend rules in mini program code.
- Do not store sensitive tokens in unsafe long-term storage without a clear reason.
- Do not build mobile-only APIs that conflict with documented backend APIs.
