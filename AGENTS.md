# AGENTS.md

## 语言

始终使用简体中文回复。

## 项目定位

本仓库用于开发羽毛球 MES 系统。后端主要使用 Spring Boot，PC 端前端使用 Vue，后续需要微信小程序端，数据库使用 MySQL。

AI 的目标是减少错误，而不是生成更多代码。所有改动都应保证结构清晰、接口一致、数据库和实体一致、前后端可联调、进度可追踪。

## 开始前必须做

1. 查看 `git status --short --branch`。
2. 阅读本次相关代码和文档，不要凭空生成。
3. 根据任务读取对应 skill：
   - 项目理解和任务拆分：`.agents/skills/mes-project-onboarding/SKILL.md`
   - 后端普通 CRUD：`.agents/skills/mes-backend-resource/SKILL.md`
   - 生产、库存、质量、追溯业务流：`.agents/skills/mes-backend-business-flow/SKILL.md`
   - 接口契约同步：`.agents/skills/mes-api-contract-sync/SKILL.md`
   - 数据库和实体检查：`.agents/skills/mes-mysql-schema-check/SKILL.md`
   - PC 前端页面：`.agents/skills/mes-pc-frontend-page/SKILL.md`
   - 微信小程序：`.agents/skills/mes-wechat-miniprogram/SKILL.md`
   - 前后端联调：`.agents/skills/mes-frontend-api-integration/SKILL.md`
   - 调试修复：`.agents/skills/mes-debug-fix/SKILL.md`
   - 测试验证：`.agents/skills/mes-test-verification/SKILL.md`
   - 提交前审查：`.agents/skills/mes-code-review/SKILL.md`
   - 更新协作进度：`.agents/skills/mes-progress-update/SKILL.md`

## 重要文档

优先读取实际存在的路径。文档路径变化时，用 `rg --files -g "*.md"` 查找。

- `COLLABORATION.md`
- `相关文档，图片/md/后端接口文档.md` 或 `md/后端接口文档.md`
- `相关文档，图片/md/数据库表结构设计文档.md`
- `相关文档，图片/md/数据库数据设计文档.md`
- `相关文档，图片/md/企业级MES最终基础数据说明.md`
- `workspace/idea/ems/docs/羽毛球MES开发约束框架.md`
- `workspace/idea/ems/src/main/resources/db/schema.sql`

## 通用规则

- 每次只做一个明确模块或一个明确功能。
- 不做无关重构。
- 不新增依赖，除非能说明现有依赖无法满足。
- 不回滚用户已有改动。
- 不把本机绝对路径写进业务代码。
- 不提交 `node_modules`、`dist`、`target`、日志、环境变量文件。
- 提交前更新并暂存 `COLLABORATION.md`。

## 后端规则

- 后端目录：`workspace/idea/ems`
- 接口统一以 `/api` 开头。
- 返回统一使用 `ResultData`。
- 分层保持 `controller`、`service`、`service.impl`、`mapper`、`entity`。
- 简单 CRUD 优先使用 MyBatis-Plus。
- 查询默认过滤 `deleted_flag = 0`。
- 删除默认软删除。
- 数据库以 `schema.sql` 为准，实体和 Mapper 必须对齐。

## 前端规则

- PC 端目录：`workspace/vscode/ems-forword`
- 页面按业务模块组织。
- 路由、组件导入、接口路径必须可构建。
- 不随意改全局样式。
- 调接口前先确认接口文档和后端实际路径。

## 数据和业务规则

- MySQL 表、Java 实体、接口字段必须一致。
- 生产、库存、质量、追溯数据不能随意物理删除。
- 库存变动必须考虑库存余额和流水。
- 状态字段优先使用英文编码，不在业务代码里临时写死中文状态。

## 验证要求

- 后端 Java 改动后运行 Maven 测试或说明不能运行的原因。
- 前端改动后运行 `npm run build` 或说明不能运行的原因。
- 数据库或实体改动后检查表名、主键、字段一致性。
- 只改文档时至少检查 Markdown 链接、表格和路径。

## 回复要求

完成任务后说明：改了什么、主要文件、验证命令和结果、风险或未完成事项。
