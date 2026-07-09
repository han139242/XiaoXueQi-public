---
name: mes-progress-update
description: Use before committing or handing off badminton MES work to update COLLABORATION.md with concise progress, module status, validation results, next step, risks, or AI-assisted development notes.
---

# MES Progress Update

## Purpose

Keep project progress readable for three AI-assisted developers. Updates should be short, useful, and tied to actual work.

## When to Update

Update when:

- a feature or slice is completed;
- module status changes;
- API or database contract changes;
- a meaningful bug is fixed;
- validation result matters to others;
- a risk or handoff note exists.

## How to Update

1. Add newest row at the top of the progress table.
2. Use the existing columns: date, member, module, completed content, validation result, next step.
3. Keep each cell concise.
4. If a module status changed, update the status table too.
5. Mention if validation was not run and why.

## Good Row

```text
2026-07-09 | AI 协助 | 用户管理 | 对齐用户分页接口路径并修复前端路由构建 | 后端测试通过，前端构建通过 | 登录联调
```

## Must Not

- Do not write vague rows like "优化代码" without module and result.
- Do not mark "已完成" without validation.
- Do not remove previous progress history unless explicitly requested.
