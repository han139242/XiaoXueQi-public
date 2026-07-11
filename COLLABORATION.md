# 羽毛球 MES 协同开发说明

本文档给三位成员共同使用，用来记录项目进度、开发边界、交接信息和基础质量要求。目标不是增加流程负担，而是保证大家都能看懂项目现在做到哪里、代码为什么这样写、下一步应该做什么。

## 1. 项目目标

最终交付一个结构清晰、能演示核心业务闭环的羽毛球 MES 系统。

技术范围：

- 后端主要使用 Spring Boot。
- 前端需要包含 PC 端管理界面和微信小程序端。
- 数据库使用 MySQL。
- 三个人本地工具和依赖小版本可以略有差异，只要不影响启动、构建、接口联调和数据库一致性即可。

核心闭环优先级：

1. 登录认证和用户管理。
2. 主数据：单位、供应商、客户、物料、产品、BOM。
3. 工艺和工厂建模：工序、路线、车间、产线、工位。
4. 生产：订单、计划、工单、派工、任务、报工。
5. 仓储：库存、出入库、批次、盘点。
6. 质量：检验单、检验结果、不合格处理。
7. 追溯和看板：条码、批次追溯、首页统计。

## 2. 文档入口

开发前优先看这几份文档：

| 文档 | 用途 |
|---|---|
| `COLLABORATION.md` | 三人协同、进度、交接、AI 使用规则 |
| `相关文档/md/database_table_analysis（数据库）.md` | 数据库设计依据和表结构说明 |
| `相关文档/md/后端接口文档.md` | 后端接口路径、请求参数、返回格式 |
| `相关文档/md/MES系统设计文档.md` | MES 总体设计和阶段规划 |
| `相关文档/md/后端分层理解手册.md` | 解释 Controller、Service、Mapper、XML 分层和答辩说法 |
| `相关文档/md/后端ABC分工AI提示词规范.md` | 群发用 A/B/C 后端分工和 AI 执行提示词 |
| `相关文档/does/mes.pdf` | 原始/参考 PDF 文档 |
| `workspace/idea/ems/docs/羽毛球MES开发约束框架.md` | 后端落地约束和模块边界 |
| `workspace/idea/ems/src/main/resources/db/schema.sql` | 当前数据库建表脚本 |

如果文档之间有冲突，优先级按下面处理：

1. 已验证可运行的代码和数据库事实。
2. `schema.sql`、实体类、接口文档。
3. 本协作文档和其它说明文档。

冲突解决后，要在相关文档里补一句说明，避免下个人继续踩坑。

## 3. 当前进度总表

状态统一使用：`未开始`、`进行中`、`待联调`、`已完成`、`有风险`。

| 模块 | 当前状态 | 负责人 | 说明 |
|---|---|---|---|
| 项目基线 | 已完成 | 全员 | 后端、前端、数据库脚本、设计文档已入库 |
| 登录认证 | 待联调 | 待认领 | 已完成正式登录页、Token 存储、Bearer 请求头、路由守卫和登录失效处理；需准备后端测试账号完成真实登录联调 |
| 用户管理 | 待联调 | 统一开发 + AI 协助 | 后端 CRUD/重置密码/角色分配和前端资源页均已落地，待真实账号与权限联调 |
| 角色管理 | 待联调 | A + AI 协助 | 已补齐后端通用 CRUD 和导出接口，待前端页面联调 |
| 主数据/工艺/工厂 | 已完成 | B + AI 协助 | 已完成 B 分工全部后端接口，包含主数据、工艺、车间、产线、工位和工位工序 |
| 工艺管理 | 待联调 | B + AI 协助 | 已完成工序、工艺路线、路线明细、工艺参数和作业指导书后端接口 |
| 工厂建模 | 待联调 | B + AI 协助 | 已完成车间、产线、工位和工位工序后端接口 |
| 生产制造 | 待联调 | 统一开发 + AI 协助 | 后端资源与关键状态流、前端资源页和业务动作均已落地；待真实数据库跑通订单到报工主链路 |
| 仓储库存 | 待联调 | 统一开发 + AI 协助 | 后端资源、库存事务和前端主从/只读/确认页面已落地；待真实数据库验证出入库、退料、盘点和库存流水 |
| 质量管理 | 待联调 | 统一开发 + AI 协助 | 后端检验与不合格品流程、前端主从页面和状态动作已落地；待与报工、入库真实联调 |
| 设备管理 | 已完成 | C + AI 协助 | 已完成设备分类、设备台账、设备状态日志、保养、点检、维修后端接口 |
| 条码追溯 | 已完成 | C + AI 协助 | 已完成 `trace_barcode`、`trace_product_lot`、`trace_material_usage` 后端接口，支持条码生成/扫码、批次追溯、按原料批次查询物料追溯和通用 CRUD/导出 |
| 首页看板 | 已完成 | C + AI 协助 | 已完成 `/api/dashboard` 汇总、生产趋势、质量统计和设备状态统计接口 |
| AI 智能助手 | 待联调 | C + AI 协助 | 已完成 `/api/ai/status`、`chat`、`dashboard-insight`、`module-suggestion`；默认关闭，需配置后端模型密钥后完成真实调用验收 |
| PC 端前端 | 待联调 | 统一开发 + AI 协助 | 已完成登录鉴权、后台 Layout、响应式导航、工作台、生产大屏、AI 助手及全部 78 个后端资源的配置式业务页面；待真实后端、数据库和 AI 模型最终联调 |
| 微信小程序 | 未开始 | 待认领 | 后续根据演示需求补移动端功能 |

完成一个明确功能、模块状态变化、接口或数据库发生变化、出现需要交接的风险时，更新上表状态和下面的进度记录。普通小修、样式微调、同一功能内的连续提交，不强制每次更新。

## 4. 进度记录

新增记录时放在最上面，格式保持简单。

| 日期 | 成员 | 模块 | 完成内容 | 验证结果 | 下一步 |
|---|---|---|---|---|---|
| 2026-07-11 | AI 协助 | PC 前端完整业务面与视觉精修 | 配置式资源工作区覆盖全部 78 个后端资源，补齐分页筛选、CRUD、导出、主从明细、只读、状态动作、追溯及上传下载；按 iMES 信息组织重做登录、工作台和大屏，主色改为钴蓝/冷青蓝，绿色仅保留业务语义；顶部统一 AI 入口 | `npm run build` 通过；1366x768 登录/工作台/资源页、1920x1080 大屏及 390x844 资源页截图通过；无页面横向溢出、可见错误或控制台异常，3 个大屏 ECharts 画布均有实际像素 | 启动 MySQL 和后端，以真实账号跑通生产、质检、入库、追溯链路；配置 AI 模型做成功与失败降级验收 |
| 2026-07-11 | AI 协助 | 生产任务/设备维修状态流 | 新任务强制待开工，限制开始/完成迁移并锁定运行后普通修改删除；维修单强制提交，限制派工/关闭迁移并校验结果、费用和数量；状态更新增加原状态条件，避免并发重复流转和重复写日志，并同步接口文档 | Maven 全量测试通过：31 tests、0 failures/errors；生产任务 5 项、维修工单 6 项状态流测试，覆盖设备维修状态同步 | 真实数据库联调任务开工/完工和维修派工/关闭，确认状态日志与设备状态同步 |
| 2026-07-10 | AI 协助 | PC 前端重点能力与视觉基线 | 在公共底座上新增独立生产运营大屏、全局 AI 助手抽屉和专用 Dashboard 解读接口；工作台增加制造指挥区、真实接口状态、重点入口和移动端重排；以 iMES-Factory 为首要参考补充场景化 UI 基线，不新增依赖 | `npm run build` 通过；浏览器检查 1920x1080、1440x900、1366x768 大屏及 390x844 工作台/AI 抽屉，图表非空、无横向溢出；`git diff --check` 通过；仅有第三方 PURE 注释和主包体积警告 | 推送稳定底座；按统一开发路线实现全部系统、主数据、生产、仓储、质量、设备和追溯界面 |
| 2026-07-10 | AI 协助 | PC 前端公共底座 | 在 VS Code 前端目录完成正式登录、Axios/Token、路由守卫和分片、后台 Layout、侧栏、顶部搜索/通知、标签页、模块入口、404、响应式抽屉及 Dashboard 工作台；开发预览与生产鉴权隔离，未新增依赖 | `npm run build` 通过；浏览器实测 1440x900 工作台、390x844 登录/工作台/导航抽屉，3 个 ECharts 画布非空且无横向溢出；生产预览无开发入口且控制台无错误；仅保留 Element Plus 第三方 PURE 注释和主包体积警告 | 人工查看 `http://localhost:5173` 效果；确认后合并公共底座，再从同一提交启动业务页面分工和真实登录/Dashboard 联调 |
| 2026-07-10 | AI 协助 | PC 前端启动复核 | 复核前端 ABC 分工、文件所有权、四轮并行顺序和实际 AI/Dashboard Controller；补充当前启动状态，并清理 AI Controller 尚未实现时期的旧表述 | 前端 `npm run build` 通过；Markdown 表格、关键路径、过时 AI 表述和 `git diff --check` 检查通过；构建仅有第三方 PURE 注释和大包体积警告 | 三人从独立分支同时执行第 0 轮；先合并 A 的公共底座，再接入 B/C 独立目录并进入第 1 轮 |
| 2026-07-10 | AI 协助 | AI 后端/状态统一 | 新增 OpenAI 兼容 AI Controller/Service、状态查询、普通问答、真实看板解读和模块建议；过滤敏感上下文且禁止 AI 自动写业务；统一看板/质检/追溯状态为英文编码，修复不合格品审批状态混写并补齐审批/状态审计记录，新增幂等状态迁移脚本并移除 `schema.sql` 硬编码 `USE mes` | Maven 全量测试通过：20 tests、0 failures/errors；MySQL 8.0 临时库建表 78 张，迁移脚本连续执行两次后编码和审批状态正确；schema/entity/@TableId/Mapper 均为 78；`git diff --check` 通过 | 前端 C 先联调 `/api/ai/status` 和失败状态；后端配置真实 `AI_API_KEY` 后完成一次成功问答与超时/失败验收；旧业务库先备份再执行状态迁移 |
| 2026-07-10 | AI 协助 | PC 前端 ABC 分工复核 | 重写前端 ABC 分工、文件所有权、并行轮次、UI/大屏/AI 验收和三份 AI 执行提示词；调整为 A 略少、B/C 略多且接近，AI 全部归 C；核对实际 Controller/ResultData/package.json，记录认证、主从接口和状态值差异 | Markdown 代码块、表格、尾随空格和关键路径检查通过；官网链接可访问，14 个 GitHub 仓库均可解析 HEAD；只改文档，未运行前端构建 | 三人分别对 AI 说“我负责前端 A/B/C”启动第 0 轮；同时优先补 AI Controller 契约并统一后端状态值 |
| 2026-07-10 | AI 协助 | PC 前端开发规划 | 新增 `相关文档/md/前端ABC分工开发计划.md`，整理前端资源参考、视觉规范、目录约定、A/B/C 分工、并行轮次、可视化大屏、AI 接入、页面验收标准和联调规则 | 文档检查通过；未改业务代码，未运行构建 | 按第 0 轮先完成 Layout、请求封装、CRUD 模板、可视化大屏草案和 AI 助手入口 |
| 2026-07-10 | AI 协助 | 项目提交前整理 | 完成后端分业务域目录整理；修正 Swagger 扫描旧 `com.neuedu.health` 包、移除未使用的 DeepSeek 明文密钥配置、规范 `HelloController` 为 `/api/hello` + `ResultData`；保留并验证 PC 前端基础框架改动 | 后端 Maven `test` 通过；MySQL 8.0 临时库执行 `schema.sql` 生成 78 张表；schema/entity/主键/Mapper namespace 检查通过；前端 `npm run build` 通过；`git diff --check` 通过 | 推送后进入 PC 前端页面开发和前后端联调 |
| 2026-07-10 | AI 协助 | 后端包结构整理 | 将后端 Controller、Service、ServiceImpl、Mapper、Entity 按业务域整理到 sys/org/mdm/process/factory/mfg/wms/qms/eam/trace/dashboard/common 等子包；同步更新 Java package/import 与 Mapper XML namespace/type 引用，并移除批量改写产生的 UTF-8 BOM | 后端 Maven `test` 通过：Tests run: 1, Failures: 0, Errors: 0；静态检查未发现旧根包引用或 BOM | 后续前后端联调时按新包路径查找后端文件，接口路径保持不变 |
| 2026-07-10 | C + AI 协助 | 条码追溯/首页看板 | 新增 `trace_barcode`、`trace_product_lot`、`trace_material_usage` 后端接口和 `/api/dashboard` 看板接口；条码支持生成、扫码、通用 CRUD 和导出；产品批次支持按批次号追溯物料使用与检验摘要；物料使用支持按原料批次查询；看板提供汇总、生产趋势、质量统计和设备状态统计 | schema/entity 数量检查通过：78/78/78；Maven `test` 通过：Tests run: 1, Failures: 0, Errors: 0 | C 继续前后端联调或补充追溯看板细节 |
| 2026-07-10 | C + AI 协助 | 设备管理 | 新增 `eam_equipment_status_log`、`eam_maintenance_plan`、`eam_maintenance_record`、`eam_spot_check_plan`、`eam_spot_check_record`、`eam_repair_order` 后端接口；设备状态日志仅开放分页、详情和导出；保养计划/记录与点检计划/记录提供通用 CRUD 和导出；维修单补充派工 `/assign` 和关闭 `/close`，并同步设备状态、设备状态日志、状态变更日志和操作日志 | schema/entity 数量检查通过：78/78/78；Maven `test` 通过：Tests run: 1, Failures: 0, Errors: 0 | C 继续追溯、看板或前后端联调 |
| 2026-07-10 | C + AI 协助 | 补货/报工/质检/不合格品 | 新增 `wms_replenishment_suggestion`、`mfg_work_report`、`mfg_report_material`、`qms_inspection_order`、`qms_inspection_result`、`qms_nonconforming_order`、`qms_nonconforming_detail` 后端接口；补货建议支持按物料/仓库生成；报工支持提交、审核和用料明细 `/full`；检验单支持主从表 `/full`、提交检验结果和检验结果批量保存；不合格品处理单支持主从表 `/full` 和审批；已审核/已完成单据禁止通过主表或明细接口绕过修改 | schema/entity 数量检查通过：78/78/78；Maven `clean test` 通过：Tests run: 1, Failures: 0, Errors: 0；`git diff --check` 仅提示 `COLLABORATION.md` LF/CRLF | C 继续追溯、看板或前后端联调 |
| 2026-07-10 | C + AI 协助 | 仓储出库/退料/盘点 | 新增 `wms_outbound_order`、`wms_outbound_detail`、`wms_return_order`、`wms_return_detail`、`wms_stocktake_order`、`wms_stocktake_detail` 后端接口；主单支持分页、详情、新增、修改、软删除、导出、`/full` 主从表保存和业务确认；出库确认扣减库存并写出库流水，退料确认回加库存并写退料流水，盘点完成按差异调整库存并写盘点流水；已确认/已完成单据禁止重复确认、修改、删除及通过明细接口绕过修改 | schema/entity 数量检查通过：78/78/78；Maven `clean test` 通过：Tests run: 1, Failures: 0, Errors: 0；`git diff --check` 仅提示 `COLLABORATION.md` LF/CRLF | C 继续仓储补货建议、报工/质检联动或前后端联调 |
| 2026-07-10 | C + AI 协助 | 仓储批次/库存/入库 | 新增 `wms_material_batch`、`wms_inventory`、`wms_stock_movement`、`wms_inbound_order`、`wms_inbound_detail` 后端接口；库存余额和库存流水仅开放查询/详情/导出；入库单支持主从表 `/full` 和确认入库，确认时事务内更新库存余额、写入库存流水、状态日志和操作日志，已确认入库单禁止重复确认、修改和删除 | schema/entity 数量检查通过：78/78/78；Maven `test` 通过：Tests run: 1, Failures: 0, Errors: 0 | C 继续出库单/明细、退料或盘点模块 |
| 2026-07-10 | C + AI 协助 | 生产任务/人员/异常/安灯 | 新增 `mfg_task`、`mfg_task_operator`、`mfg_exception`、`mfg_andon_alert` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出；补充任务开始/完成、按任务查人员、异常处理、安灯触发/关闭接口；安灯触发字段以 `schema.sql` 实体为准，文档示例中不存在的字段不落库 | schema/entity 数量检查通过：78/78/78；Maven `test` 通过：Tests run: 1, Failures: 0, Errors: 0 | C 继续报工、报工用料或仓储库存模块 |
| 2026-07-10 | C + AI 协助 | C 后端注释和接口修复 | 补齐 `mfg_work_order_material`、`qms_inspection_standard`、`qms_inspection_scheme`、`qms_inspection_scheme_item` Controller/Service/ServiceImpl 方法注释；移除质检标准旧 `getPage/export` Service 方法；补充按工单查用料和按检验方案查明细接口；统一导出标题和 Mapper 中文说明 | schema/entity 数量检查通过：78/78/78；Maven `test` 通过：Tests run: 1, Failures: 0, Errors: 0 | C 继续后续生产、仓储、质量模块联调 |
| 2026-07-09 | C + AI 协助 | C 后端代码检查修复 | 检查并修复 `mfg_work_order_material`、`qms_inspection_standard`、`qms_inspection_scheme`、`qms_inspection_scheme_item` 后端代码；统一 QMS 接口 `/api/qms/...` 路径、`current/size` 分页和 `/excel` 导出，修复质检标准物理删除风险，查询过滤 `deleted_flag = 0`，删除改为软删除 | Maven `clean test` 通过：Tests run: 1, Failures: 0, Errors: 0 | C 继续后续生产/仓储/质量模块联调 |
| 2026-07-09 | A + AI 协助 | 审批记录管理 | 新增 `sys_approval_record` 后端通用 CRUD 和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；支持业务、审批节点、审批人、审批结果和时间范围筛选 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步复查 A 模块完整性 |
| 2026-07-09 | A + AI 协助 | 状态变更日志管理 | 新增 `sys_status_change_log` 只读后端接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；支持分页、详情、导出，以及业务类型、业务 ID、状态、变更人、时间范围筛选 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续审批记录 |
| 2026-07-09 | A + AI 协助 | 操作日志管理 | 新增 `sys_operation_log` 只读后端接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；支持分页、详情、导出，以及用户、操作类型、模块、业务 ID、时间范围筛选 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续状态变更日志 |
| 2026-07-09 | A + AI 协助 | 附件文件管理 | 新增 `sys_file` 上传、下载、通用 CRUD 和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；复用现有文件工具并补充通用上传方法 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续操作日志 |
| 2026-07-09 | A + AI 协助 | 系统参数管理 | 新增 `sys_system_param` 后端通用 CRUD 和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；支持参数键/名称关键字和模块编码筛选 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续附件文件 |
| 2026-07-09 | A + AI 协助 | 字典数据管理 | 新增 `sys_dict_data` 后端通用 CRUD、按字典类型查询和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；因表结构无 `status` 字段，接口不按 status 过滤 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续系统参数 |
| 2026-07-09 | A + AI 协助 | 字典类型管理 | 新增 `sys_dict_type` 后端通用 CRUD 和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；支持字典类型编码/名称关键字和状态筛选 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续字典数据 |
| 2026-07-09 | A + AI 协助 | 用户和菜单管理 | 将早期 `UserController` / `UserService` / `UserServiceImpl` 收敛为接口文档要求的 `SysUserController` / `SysUserService` / `SysUserServiceImpl`；保留 `/api/sys/user` 路径，补充分页、软删除、重置密码、分配角色接口；新增 `sys_menu` 后端 CRUD、菜单树和导出接口 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续字典类型 |
| 2026-07-09 | A + AI 协助 | 班次管理 | 新增 `org_shift` 后端通用 CRUD 和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；因表结构无 `status` 字段，接口不按 status 过滤 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续字典类型 |
| 2026-07-09 | A + AI 协助 | 班组成员管理 | 新增 `org_team_member` 后端通用 CRUD、按班组查询成员接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；查询默认过滤 `deleted_flag = 0`，删除为软删除 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续班次管理 |
| 2026-07-09 | C + AI 协助 | 库位管理 | 新增 `wms_location` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出；支持库位编码/名称搜索、仓库筛选、库位类型筛选、状态筛选 | Maven 测试通过 | C 继续仓储其他模块（库存、出入库等） |
| 2026-07-09 | C + AI 协助 | 仓库管理 | 新增 `wms_warehouse` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出；支持仓库编码/名称搜索、仓库类型筛选、状态筛选 | Maven 测试通过 | C 继续库位管理模块 |
| 2026-07-09 | C + AI 协助 | 生产工单 | 新增 `mfg_work_order` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出；支持工单编号搜索、状态筛选、生产计划筛选、产品筛选 | Maven 测试通过 | C 继续仓储模块或其他生产模块 |
| 2026-07-09 | A + AI 协助 | 班组管理 | 新增 `org_team` 后端通用 CRUD 和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；支持班组编码/名称、部门、班组长筛选；因表结构无 `status` 字段，接口不按 status 过滤 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续班组成员管理 |
| 2026-07-09 | A + AI 协助 | 员工管理 | 新增 `org_employee` 后端通用 CRUD 和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；支持工号/姓名/电话关键字、部门、状态筛选 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续班组管理 |
| 2026-07-09 | A + AI 协助 | 部门管理 | 新增 `org_department` 后端通用 CRUD、部门树和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；因表结构无 `status` 字段，接口不按 status 过滤 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续员工管理 |
| 2026-07-09 | C + AI 协助 | 生产计划 | 新增 `mfg_production_plan` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出；支持计划编号搜索、状态筛选、生产订单筛选、产线筛选 | Maven 测试通过 | C 继续生产工单或仓储模块 |
| 2026-07-09 | A + AI 协助 | 角色菜单关联 | 新增 `sys_role_menu` 后端通用 CRUD、按角色查询菜单关联接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；ServiceImpl 显式调用 `baseMapper`，查询默认过滤 `deleted_flag = 0`，删除为软删除 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续组织基础模块 |
| 2026-07-09 | A + AI 协助 | 用户角色关联 | 新增 `sys_user_role` 后端通用 CRUD、按用户查询角色关联接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；ServiceImpl 显式调用 `baseMapper`，查询默认过滤 `deleted_flag = 0`，删除为软删除 | 已使用 JDK 21 和本机 MySQL 环境变量运行 Maven test，通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续 `sys_role_menu` |
| 2026-07-09 | C + AI 协助 | 生产订单 | 新增 `mfg_production_order` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出；支持订单编号搜索、状态筛选、客户筛选、产品筛选 | Maven 测试通过 | C 继续生产计划或生产工单模块 |
| 2026-07-09 | C + AI 协助 | 设备台账 | 新增 `eam_equipment` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出；支持设备编码/名称搜索、状态筛选、分类筛选 | Maven 测试通过 | C 继续生产订单模块 |
| 2026-07-09 | C + AI 协助 | 设备分类 | 新增 `eam_equipment_category` 后端通用管理接口，包含分页、详情、新增、修改、软删除、树形结构查询、导出；支持分类编码/名称搜索、状态筛选 | Maven 测试通过 | C 继续设备台账模块 |
| 2026-07-09 | C + AI 协助 | 质检项目 | 新增 `qms_inspection_item` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出；支持项目编码/名称搜索、检验类型筛选 | Maven 测试通过 | C 继续设备分类或质检标准模块 |
| 2026-07-09 | B + AI 协助 | 工位工序 | 新增 `factory_station_operation` 后端通用管理接口，包含分页、详情、新增、修改、软删除、按工位查询可执行工序、导出和 Mapper XML | Maven 测试通过 | B 分工后端模块已完成，等待联调或补接口细节 |
| 2026-07-09 | B + AI 协助 | 工位 | 新增 `factory_station` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续工位工序模块 |
| 2026-07-09 | B + AI 协助 | 产线 | 新增 `factory_line` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续工位模块 |
| 2026-07-09 | B + AI 协助 | 车间 | 新增 `factory_workshop` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续产线模块 |
| 2026-07-09 | B + AI 协助 | 作业指导书 | 新增 `process_work_instruction` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续车间模块 |
| 2026-07-09 | B + AI 协助 | 工艺参数 | 新增 `process_parameter` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续作业指导书模块 |
| 2026-07-09 | B + AI 协助 | 工艺路线明细 | 新增 `process_route_item` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续工艺参数模块 |
| 2026-07-09 | B + AI 协助 | 工艺路线 | 新增 `process_route` 主表后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML；路线明细按文档拆到下一个模块单独实现 | Maven 测试通过 | B 继续工艺路线明细模块 |
| 2026-07-09 | B + AI 协助 | 工序 | 新增 `process_operation` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续工艺路线模块 |
| 2026-07-09 | B + AI 协助 | BOM 明细 | 新增 `mdm_bom_item` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续工序模块 |
| 2026-07-09 | B + AI 协助 | BOM | 新增 `mdm_bom` 主表后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML；BOM 明细按文档拆到下一个模块单独实现 | Maven 测试通过 | B 继续 BOM 明细模块 |
| 2026-07-09 | B + AI 协助 | 产品 | 新增 `mdm_product` 后端管理接口，按文档支持产品编码、名称、状态分页过滤，并包含详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续 BOM 或 BOM 明细模块 |
| 2026-07-09 | B + AI 协助 | 物料 | 新增 `mdm_material` 后端管理接口，按文档支持物料编码、名称、分类、状态分页过滤，并包含详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续产品或 BOM 模块 |
| 2026-07-09 | B + AI 协助 | 物料分类 | 新增 `mdm_material_category` 后端管理接口，包含分页、树形查询、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续物料或产品模块 |
| 2026-07-09 | B + AI 协助 | 客户 | 新增 `mdm_customer` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续物料分类或物料模块 |
| 2026-07-09 | B + AI 协助 | 供应商 | 新增 `mdm_supplier` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续客户或物料分类模块 |
| 2026-07-09 | B + AI 协助 | 计量单位 | 新增 `mdm_unit` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续供应商或客户模块 |
| 2026-07-09 | A + AI 协助 | 后端协作分工 | 新增 Markdown 版 A/B/C 后端分工 AI 提示词，补充分层风格、并行冲突处理、单模块流程和群内交接格式 | 文档入口和 Markdown 标题已检查 | 群内按 A/B/C 认领后每次只让 AI 做一个模块 |
| 2026-07-09 | A + AI 协助 | 角色管理 | 将 `SysRoleServiceImpl` 的通用 CRUD 调整为显式 `baseMapper` 调用，并同步分层手册说明 | Maven 测试通过 | 后续普通模块优先使用更直观的 `baseMapper` 风格 |
| 2026-07-09 | A + AI 协助 | 后端分层理解 | 新增后端分层理解手册，说明 `SysRole` 模块写法、Mapper 为空原因、Service 业务边界和答辩说明 | Maven 测试通过，文档入口已检查 | 后续成员按手册理解普通 CRUD 和复杂业务分层 |
| 2026-07-09 | A + AI 协助 | 角色管理 | 调整 `sys_role` 分层：Controller 只处理请求返回，查询、默认值、软删除等规则下沉到 Service | Maven 测试通过 | 后续模块按此分层模板继续实现 |
| 2026-07-09 | A + AI 协助 | 角色管理 | 新增 `sys_role` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | 继续补角色菜单分配或前端角色管理页面 |
| 2026-07-09 | 人工整理 + AI 协助 | 文档目录 | 将设计、数据库、接口和 PDF 统一整理到 `相关文档/`，并同步 README、协作文档和后端约束里的文档路径 | 已检查新目录文件和链接引用 | 后续统一从 `相关文档/` 查设计资料 |
| 2026-07-09 | AI 协助 | AI 开发规范 | 新增 `AGENTS.md`、Copilot 指令和 12 个 MES 专用 skills，覆盖项目进入、后端、数据库、前端、联调、调试、验证、审查和进度更新 | 12 个 skill 已通过结构校验 | 后续开发按任务读取对应 skill |
| 2026-07-09 | AI 协助 | 协作规范 | 新增 Git 提交提醒钩子，提交时检查是否同步更新 `COLLABORATION.md` | 已加入 `.githooks/pre-commit` 和安装脚本 | 成员拉取后执行一次 hooks 配置 |
| 2026-07-09 | AI 协助 | 协作规范 | 新增三人通用协同开发说明，补充 README 文档入口 | 已检查 Markdown 内容和链接路径 | 后续每次开发同步更新进度 |
| 2026-07-09 | AI 协助 | 项目基线 | 同步后端基线、数据库脚本、接口文档，修复前端构建入口 | 后端测试通过，前端构建通过 | 开始登录和用户管理联调 |

## 5. 每次开发的最小流程

1. 开始前先看 `git status`，确认当前工作区是否干净。
2. 确认本次只做一个模块或一个清晰功能，例如“用户分页查询”。
3. 先读已有代码、接口文档和表结构，再让 AI 写代码。
4. 改动尽量小，不顺手重构无关文件。
5. 写完后运行最小验证。
6. 更新本文件的进度记录。提交钩子会检查 `COLLABORATION.md` 是否已暂存，防止忘记记录。
7. 提交到 GitHub，提交信息写清楚模块和动作。

一个功能可以标为完成，至少满足：

- 代码位置清楚，能从模块名找到 Controller、Service、Mapper、Entity 或前端页面。
- 接口路径、字段名、返回格式和文档基本一致。
- 本地能通过对应的最小验证。
- 已在进度记录里写清完成内容、验证结果和下一步。

## 6. 提交提醒机制

仓库提供了 `.githooks/pre-commit`，用于提醒每次提交同步更新 `COLLABORATION.md`。

每位成员拉取仓库后执行一次：

```powershell
.\scripts\setup-git-hooks.ps1
```

生效后，如果提交时没有暂存 `COLLABORATION.md`，Git 会中断提交并提示先更新文档。

特殊情况，例如只改错别字或临时修复冲突，可以临时跳过：

```powershell
$env:SKIP_COLLAB_DOC='1'; git commit -m "fix: typo"
```

跳过只用于确实不影响协作进度的改动，不作为常规做法。

建议提交信息格式：

```text
feat: add sys user page api
fix: repair frontend router build
docs: update collaboration progress
chore: sync database schema
```

## 7. AI 开发使用规则

三个人都可以使用 AI，但要遵守下面几条，防止项目失控：

- 使用 AI 前先看 `AGENTS.md`。
- 任务复杂时，让 AI 读取 `.agents/skills` 下对应的 MES 专用 skill。
- 让 AI 改代码前，先让它阅读相关文件，不要直接让它凭空生成。
- 给 AI 的任务要具体，例如“按接口文档实现 mdm_unit 的 CRUD”，不要只说“帮我完善主数据”。
- AI 新增接口、字段、状态值、依赖时，要先核对接口文档、数据库文档和 `pom.xml`。
- AI 生成的 SQL、Mapper、Service、Controller 必须自己快速看一遍，确认名称和表字段对得上。
- AI 修改完成后，要让它说明改了哪些文件、怎么验证、还有什么风险。
- 不确定的地方先记录在本文件，不要偷偷写死临时规则。

推荐提示词模板：

```text
请先阅读相关代码和文档，不要直接修改。
本次只实现【模块/功能】。
要求遵守现有包结构、接口路径、ResultData 返回格式和 schema.sql 字段。
修改后运行最小验证，并总结改动、验证和风险。
```

## 8. 代码结构约定

后端目录：

```text
workspace/idea/ems
src/main/java/com/neuedu/ems/controller
src/main/java/com/neuedu/ems/service
src/main/java/com/neuedu/ems/service/impl
src/main/java/com/neuedu/ems/mapper
src/main/java/com/neuedu/ems/entity
src/main/resources/mappers
src/main/resources/db/schema.sql
```

前端目录：

```text
workspace/vscode/ems-forword
src/pages
src/router
src/style.css
```

微信小程序目录后续创建后再补充，建议独立放在 `workspace` 下，不和 PC 端目录混在一起。

基本要求：

- 后端接口统一 `/api` 开头。
- 查询默认过滤 `deleted_flag = 0`。
- 删除默认做软删除，不物理删除业务数据。
- 实体字段和 `schema.sql` 保持一致。
- 前端页面路径和后端模块路径尽量对应，方便查找。
- 不新增依赖，除非能说明为什么现有依赖不够用。

## 9. 版本和环境

版本原则保持简单：

- Spring Boot、Node、npm、MySQL 等工具允许小版本不完全一致。
- 如果代码能正常启动、测试、构建、联调，就不因为小版本差异强制统一。
- 如果某个版本差异导致报错，要把实际版本、报错现象和解决方式记录到进度记录或相关文档里。
- 不把本机绝对路径写进业务代码；确实需要本地路径时，优先使用配置项或环境变量。

## 10. 验证要求

按改动范围选择最小验证，不要求每次都跑全部。

| 改动范围 | 至少验证 |
|---|---|
| 后端 Java 代码 | 在 `workspace/idea/ems` 运行 Maven 测试 |
| 数据库脚本或实体 | 检查 `schema.sql`、实体 `@TableName`、`@TableId` 是否一致 |
| 前端代码 | 在 `workspace/vscode/ems-forword` 运行 `npm run build` |
| 只改文档 | 检查文档路径、链接、表格格式 |

当前可用的后端测试命令：

```powershell
java "-Dmaven.multiModuleProjectDirectory=D:\soft\XiaoXueQiworkspace\workspace\idea\ems" -cp .mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain test
```

当前可用的前端验证命令：

```powershell
npm run build
```

## 11. 交接模板

每次提交或交给别人继续做时，按这个模板写到聊天记录、提交说明或本文件进度记录里：

```text
本次模块：
完成内容：
主要文件：
验证命令：
验证结果：
未完成事项：
风险或注意点：
下一步建议：
```

## 12. 风险处理

常见风险和处理方式：

| 风险 | 处理方式 |
|---|---|
| 接口路径和前端请求不一致 | 以 `相关文档/md/后端接口文档.md` 为准，同时修正前端调用 |
| 实体字段和数据库字段不一致 | 以 `schema.sql` 为准，修实体并跑测试 |
| AI 生成重复代码 | 优先复用现有 Service、Mapper、工具类 |
| 一个提交改太多模块 | 拆成小提交，至少在提交说明里写清影响范围 |
| 不知道谁改了什么 | 看 `git log --oneline` 和本文件进度记录 |
| 本地能跑别人不能跑 | 检查配置是否写死本机路径，必要时改成环境变量 |

## 13. 简单原则

- 能看懂，比写得复杂更重要。
- 能运行，比功能堆得多更重要。
- 每次只推进一个清楚的点。
- 文档只记录对协作有帮助的信息，每次提交至少补一条简短、有用的进度或风险记录。
- 遇到不确定的地方，先写下来，再一起决定。
