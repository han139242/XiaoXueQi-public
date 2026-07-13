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
| `相关文档，图片/md/GitHub仓库操作审计.md` | 按北京时间记录所有分支的 Push、Create、Delete 事件和实际操作账号 |
| `相关文档，图片/md/database_table_analysis（数据库）.md` | 数据库设计依据和表结构说明 |
| `相关文档，图片/md/后端接口文档.md` | 后端接口路径、请求参数、返回格式 |
| `相关文档，图片/md/MES系统设计文档.md` | MES 总体设计和阶段规划 |
| `相关文档，图片/md/羽毛球MES微信小程序实施文档.md` | 微信开发者工具制作小程序的页面、接口、目录和联调实施说明 |
| `相关文档，图片/md/后端分层理解手册.md` | 解释 Controller、Service、Mapper、XML 分层和答辩说法 |
| `相关文档，图片/md/后端ABC分工AI提示词规范.md` | 群发用 A/B/C 后端分工和 AI 执行提示词 |
| `相关文档，图片/does/mes.pdf` | 原始/参考 PDF 文档 |
| `workspace/idea/ems/docs/羽毛球MES开发约束框架.md` | 后端落地约束和模块边界 |
| `workspace/idea/ems/src/main/resources/db/schema.sql` | 当前数据库建表脚本 |
| `相关文档，图片/md/数据库共享操作文档.md` | 三人共享 MySQL/Tailscale 配置、Navicat 连接、服务器身份核验和操作规则；包含共享凭据，仅限当前私人仓库成员使用 |

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
| 登录认证 | 已完成 | han139242 / zengjingye / AnHC-00 | 已完成 PBKDF2 密码哈希与旧明文自动升级、连续失败 5 次自动锁定、`/api/auth/info`、`/logout` 和注销 Token 黑名单；共享库 `admin` 真实登录、JWT 鉴权、注销失效均已通过 |
| 用户管理 | 待联调 | han139242 / zengjingye / AnHC-00 | 后端 CRUD/重置密码/角色分配和前端资源页均已落地，待真实账号与权限联调 |
| 角色管理 | 待联调 | zengjingye | 已补齐后端通用 CRUD 和导出接口，待前端页面联调 |
| 主数据/工艺/工厂 | 已完成 | han139242 | 已完成 B 分工全部后端接口，包含主数据、工艺、车间、产线、工位和工位工序 |
| 工艺管理 | 待联调 | han139242 | 已完成工序、工艺路线、路线明细、工艺参数和作业指导书后端接口 |
| 工厂建模 | 待联调 | han139242 | 已完成车间、产线、工位和工位工序后端接口 |
| 生产制造 | 待联调 | han139242 / zengjingye / AnHC-00 | 后端资源与关键状态流、前端资源页和业务动作均已落地；待真实数据库跑通订单到报工主链路 |
| 仓储库存 | 待联调 | han139242 / zengjingye / AnHC-00 | 后端资源、库存事务和前端主从/只读/确认页面已落地；待真实数据库验证出入库、退料、盘点和库存流水 |
| 质量管理 | 待联调 | han139242 / zengjingye / AnHC-00 | 后端检验与不合格品流程、前端主从页面和状态动作已落地；待与报工、入库真实联调 |
| 设备管理 | 已完成 | AnHC-00 | 已完成设备分类、设备台账、设备状态日志、保养、点检、维修后端接口 |
| 条码追溯 | 已完成 | AnHC-00 | 已完成 `trace_barcode`、`trace_product_lot`、`trace_material_usage` 后端接口，支持条码生成/扫码、批次追溯、按原料批次查询物料追溯和通用 CRUD/导出 |
| 首页看板 | 已完成 | AnHC-00 | 已完成 `/api/dashboard` 汇总、生产趋势、质量统计和设备状态统计接口 |
| AI 智能助手 | 待联调 | AnHC-00 | 已切换 DeepSeek 官方接口和 `deepseek-chat`，完成 `/api/ai/status`、`chat`、`dashboard-insight`、`module-suggestion`；项目默认配置与主机用户环境变量均使用团队确认的 DeepSeek Key，真实问答已通过，尚需人工复核 AI 抽屉交互和输出质量 |
| PC 端前端 | 待联调 | han139242 / zengjingye / AnHC-00 | 已完成登录鉴权、后台 Layout、响应式导航、工作台、真实数据生产大屏、AI 助手及全部 78 个后端资源的配置式业务页面；生产构建、真实登录和看板代理烟测通过，核心业务写操作与自动视觉复核仍待完成 |
| 微信小程序 | 进行中 | han139242 | 已建立 `workspace/vx` 微信开发者工具原生模板，补充界面参考图和实施文档；尚未接入登录、后端 `/api` 和 MES 业务页面 |

完成一个明确功能、模块状态变化、接口或数据库发生变化、出现需要交接的风险时，更新上表状态和下面的进度记录。普通小修、样式微调、同一功能内的连续提交，不强制每次更新。

## 4. 进度记录

新增记录时放在最上面，格式保持简单。

“成员”列统一填写 GitHub 实际推送账号。历史记录依据 GitHub PushEvent、CreateEvent、DeleteEvent 和对应提交校正；同一内容经过多个分支时，以首次上传该内容的账号为准，后续合并和删除操作单独记录在 `相关文档，图片/md/GitHub仓库操作审计.md`。该字段不用于判断电脑前的实际操作人。

| 日期 | 成员 | 模块 | 完成内容 | 验证结果 | 下一步 |
|---|---|---|---|---|---|
| 2026-07-12 | AI 协助 | 微信小程序第一版静态交付审查 | 完成实施文档阶段 0 至 4：登录认证、工单/任务只读、开始/报工/完成、真实工作台、扫码追溯和我的页面；审查确认未修改后端/PC 前端，未提交、推送或发布 | 全量 JSON 解析、JavaScript `node --check`、页面/组件路径、敏感信息扫描、API/身份/伪状态专项扫描及 `git diff --check` 通过；10 个受保护接口均返回 401，后端可达且鉴权生效；开发者工具 CLI 不可用 | 阻塞于微信开发者工具编译、关联 `employeeId` 专用账号的真实闭环烟测、真机/合法 HTTPS 域名和已授权 TabBar PNG；完成这些验收前不标记第一版完成，也不进入阶段 5 |
| 2026-07-12 | AI 协助 | 微信小程序阶段 4 | 完成只读真实 summary 工作台、工单页与工作台扫码入口、条码结果/未知条码状态、我的账号信息/角色/权限/开发地址/缓存清理/退出；仅展示后端七个现有汇总字段，无伪统计或死入口 | 全部 JSON 解析、JavaScript `node --check`、页面路径、敏感信息扫描和 `git diff --check` 通过；本机 8088 看板接口返回 401，确认服务可达且鉴权生效；未发现开发者工具 CLI，未执行工具编译 | 在微信开发者工具完成编译与参考图并排复核；使用关联 `employeeId` 的专用测试账号完成登录、任务、报工、完成和注销烟测；补齐已授权 TabBar PNG 与正式 HTTPS 域名 |
| 2026-07-12 | AI 协助 | 微信小程序阶段 3 | 按任务真实状态补齐开始、报工和完成操作；报工仅调用 `/api/mfg/work-report/submit`，操作人只取 `/auth/info.employeeId`，完成数量按最终值提交；完成报工列表/详情、用料只读明细、防重复提交和操作后重载 | 全部 JSON 解析、JavaScript `node --check`、页面路径、写接口/状态伪造专项扫描和 `git diff --check` 通过；缺少关联员工测试账号与可控任务，未执行真实写操作烟测 | 进入阶段 4，实现真实 summary 工作台、扫码追溯和我的页面；最终使用专用测试数据联调生产闭环 |
| 2026-07-12 | AI 协助 | 微信小程序阶段 2 | 完成工单和任务真实分页、关键字/状态/产品/工单/工序/工位筛选、下拉刷新、上拉加载及工单/任务详情；集中加载产品、工单、工序、工位、员工名称映射，失败时显示明确 ID | 全部 JSON 解析、JavaScript `node --check`、`app.json` 页面路径和 `git diff --check` 通过；未连接真实账号与数据库，接口数据态待联调 | 进入阶段 3，实现开始任务、报工提交/查询和完成任务，并严格按 `WAIT_START/RUNNING/PAUSED` 控制按钮 |
| 2026-07-12 | AI 协助 | 微信小程序阶段 1 | 接入开发/体验/正式环境地址策略、JWT 认证存储、统一 `ResultData` 请求处理、401/1002 回登录、页面守卫和真实账号登录；建立五个受保护 Tab 并移除默认 `index/logs` 与 `wx.login` 示例 | 全部 JSON 解析、JavaScript `node --check`、`app.json` 页面路径、默认示例残留扫描和 `git diff --check` 通过；未提供账号，尚未执行真实登录烟测 | 进入阶段 2，实现真实工单/任务分页、筛选、详情和名称映射；后续补微信开发者工具编译与账号联调 |
| 2026-07-12 | AI 协助 | 微信小程序阶段 0 | 固定共享基础库为 `3.16.2`，建立蓝色设计令牌、状态字典、格式工具和加载/空状态/状态标签/数据卡公共组件；默认页面按迁移顺序暂时保留 | `workspace/vx` 全部 JSON 解析、JavaScript `node --check`、`git diff --check -- workspace/vx` 通过 | 进入阶段 1，接入请求封装、账号登录、Token 校验和受保护 Tab；Iconfont 因缺少授权先使用纯文字 TabBar |
| 2026-07-12 | han139242 | 微信小程序实施文档 | 新增微信小程序实施文档，覆盖 AppID `wx596570b6ec2dd957`、微信开发者工具创建步骤、目录结构、公共请求封装、工单/任务/报工/工作台页面、后端接口清单和验收标准；同步参考截图素材目录 | 已检查 Markdown 文件存在、目标路径正确、AppID 已写入；本次仅文档和图片，无运行测试 | 按文档在 `workspace/vx` 接入登录、Token、工单、任务和报工主流程 |
| 2026-07-12 | han139242 | 微信小程序开发模板 | 纳入 `workspace/vx` 原生小程序初始工程，包含首页、日志页、工具函数和公共项目配置；补充 `微信小程序.jpg` 业务界面参考图，并忽略本机 `project.private.config.json` | 6 个 JSON 配置解析通过、4 个 JavaScript 文件 `node --check` 通过，敏感信息扫描未发现密钥或 Token；未执行微信开发者工具自动编译 | 基于模板设计登录与 Token 策略，优先实现任务查看、扫码、报工和质检流程 |
| 2026-07-12 | han139242 | 生产运营大屏信息重构 | 参考 Power BI、Tableau、Grafana 官方规范和 GitHub DataV/制造 SPC 项目，将密集大屏收敛为 4 个大 KPI 与 5 个业务区；移除写死排程、环比、伪 OEE 和模拟告警；二次优化生产趋势双口径、今日/台账边界、风险分类、明细跳转、刷新异常与全屏交互，仅展示 4 个 Dashboard 接口数据 | 前端 `npm run build` 通过；登录、注销及 4 个 Dashboard 接口此前烟测均为 200；本轮大屏路由 HTTP 200、`git diff --check` 通过；浏览器通道无可用实例且 Edge 停留在 GitHub 页面，尚未完成最终截图复核 | 切回生产大屏标签后复核 1536×864 窗口和 1920×1080 全屏的完整性、字号、图表与空数据状态 |
| 2026-07-12 | han139242 | 协作归属与仓库审计校正 | 依据 GitHub PushEvent 将 `a97dc6f` 对应的 11 条生产大屏进度从提交作者校正为实际推送账号 `AnHC-00`，并增量补齐 3 条仓库 Push 事件 | 核对 Event ID `14794188214`、`14797315825`、`14803082664`，确认账号、分支和前后提交；审计累计 75 条 | 后续成员归属只以 GitHub Event 的 `actor.login` 为准，不再使用 Git 提交作者代替 |
| 2026-07-12 | han139242 | 认证、审计、乐观锁与 DeepSeek | 登录密码改为 PBKDF2 哈希并兼容旧明文自动升级，补齐失败锁定、用户信息、注销和 Token 黑名单；JWT 自动填充审计字段，11 个高并发表启用乐观锁；共享库执行可重复迁移；AI 切换 DeepSeek 官方接口 | 后端 55 项测试、前端构建、共享 MySQL 审计/乐观锁事务测试均通过；真实烟测通过登录、`/info`、DeepSeek 问答、`/logout`、旧 Token 失效和密码哈希持久化；schema/实体/主键均保持 78 | 三人拉取后继续生产、库存和质量真实写操作联调；遇到 `1007` 时刷新数据后重试 |
| 2026-07-12 | AnHC-00 | 生产运营大屏 | 移除刷新按钮误用的静态告警状态，红点不再常驻；刷新中的旋转反馈保留 | `npm.cmd run build` 通过；`git diff --check` 通过；开发路由 HTTP 200 | 后端补充真实 OEE 所需的可用性、性能和质量统计字段 |
| 2026-07-12 | AnHC-00 | 生产运营大屏 | 顶部第四项替换为设备 OEE；当前以设备可动率和质量合格率计算估算值，并显示可动率，待后端补充节拍/计划产能后替换为完整 OEE | `npm.cmd run build` 通过；`git diff --check` 通过；开发路由 HTTP 200 | 后端补充真实 OEE 所需的可用性、性能和质量统计字段 |
| 2026-07-12 | AnHC-00 | 生产运营大屏 | 顶部 KPI 移除订单总数，将生产工单移至首项；原生产工单位置替换为设备总数及运行设备数，复用现有设备状态接口数据 | `npm.cmd run build` 通过；`git diff --check` 通过；开发路由 HTTP 200 | 在真实设备状态数据下核对指标数值 |
| 2026-07-12 | AnHC-00 | 生产运营大屏 | 将订单排期的日期与日/周/月作为整体置于标题栏中间；已完成状态由灰色改为低饱和琥珀色，与计划蓝、进行中绿、延迟紫明确对应 | `npm.cmd run build` 通过；`git diff --check` 通过；开发路由 HTTP 200 | 在 1920×1080 全屏下人工确认居中控制组与图例间距 |
| 2026-07-12 | AnHC-00 | 生产运营大屏 | 将订单排期日期置于标题栏中间；统一任务条为计划、进行中、已完成、延迟四种可与图例一一对应的状态色，移除未说明的颜色 | `npm.cmd run build` 通过；`git diff --check` 通过；开发路由 HTTP 200 | 在 1920×1080 全屏下人工确认日期与图例间距 |
| 2026-07-12 | AnHC-00 | 生产运营大屏 | 丰富生产进度总览的工厂蓝图背景，补充厂区网格、主厂房、设备岛、输送线和状态节点，保持在数据图层之下 | `npm.cmd run build` 通过；`git diff --check` 通过；开发路由 HTTP 200 | 在 1920×1080 全屏下人工确认背景密度是否合适 |
| 2026-07-12 | AnHC-00 | 生产运营大屏 | 为生产进度总览补充低透明等距工厂蓝图背景；收紧图表高度、缩小并居中环形进度，避免圆环被面板裁切 | `npm.cmd run build` 通过；`git diff --check` 通过；开发路由 HTTP 200 | 在 1920×1080 全屏下人工确认环图与右侧图例间距 |
| 2026-07-12 | AnHC-00 | 生产运营大屏 | 将生产订单排期任务条调亮，并复用顶部 KPI 的蓝、橙、青、紫基调；状态色以有限比例混入深蓝底，兼顾可读性与低饱和层次 | `npm.cmd run build` 通过；`git diff --check` 通过；开发路由 HTTP 200 | 在 1920×1080 全屏下人工确认亮度与顶部 KPI 是否一致 |
| 2026-07-12 | AnHC-00 | 生产运营大屏 | 将生产订单排期的任务条和图例收敛为低饱和中性灰蓝；保留轻微灰绿、灰紫、灰棕色偏区分状态，去除鲜艳色块感 | `npm.cmd run build` 通过；`git diff --check` 通过；开发路由 HTTP 200 | 在 1920×1080 全屏下人工确认明度是否还需再降 |
| 2026-07-12 | AnHC-00 | 生产运营大屏 | 按参考图重构为顶部 6 项指标、左侧产线、中部工单甘特排程、右侧进度/产量趋势和底部设备/效率/预警/质量四区；补齐退出大屏入口，上调全页文字，强化 KPI 图标/折线背景，以角部渐变边框和右下加粗直线强化开放式分区，移除大屏内开发预览标识，并将排期任务条改为浅色渐变 | `npm.cmd run build` 通过；`git diff --check` 通过；未安装可用浏览器自动化，待人工视觉复核 | 用真实账号在 1920×1080 全屏下核对实际数据与视觉细节 |
| 2026-07-12 | AnHC-00 | 生产运营大屏 | 参考工业生产看板重构顶部指标、产线状态、工序排程、生产进度、趋势、设备和风险布局，保留真实接口与预览分支 | `npm.cmd run build` 通过；开发路由 HTTP 200；未安装 Playwright，未完成自动截图复核 | 退出预览并真实登录后复核接口数据和 1920×1080 全屏视觉 |
| 2026-07-12 | han139242 | GitHub 分支操作审计 | 新增独立审计表，按北京时间回溯仓库创建以来所有分支的 Push、Create、Delete 事件；纠正首次通过功能分支上传的两条 `c16a128` 进度归属 | GitHub Events API 共核对 72 条：Push 67、Create 3、Delete 2，覆盖 `main` 和 `frontend/framework-foundation`；Event ID 无重复 | 后续以 Event ID 增量同步，所有分支创建、推送和删除均不得遗漏 |
| 2026-07-12 | han139242 | 协作记录推送归属校正 | 对照 GitHub 三位协作者、仓库 PushEvent 和进度行对应提交，将历史成员列统一改为实际推送账号并清理旧成员标记 | 当时仅按 PushEvent 匹配 83 条历史进度；后续完整审计发现遗漏 1 条 CreateEvent，已由上方记录纠正 | 后续同时核对 Push、Create、Delete 和所有分支 |
| 2026-07-12 | han139242 | 审计字段与乐观锁规范 | 在数据库分析文档中明确审计字段、业务操作日志和 MySQL binlog 的职责差异，记录当前自动填充与乐观锁尚未启用，并约定后续按表并发风险实施，禁止页面手工填写审计字段 | `schema.sql`、实体和主键均为 78；`@Version`、乐观锁插件、`MetaObjectHandler` 当前均为 0；Markdown 章节结构检查通过 | 后续单独实施 JWT 操作人自动填充、登录时间更新和高并发表乐观锁，并补并发测试 |
| 2026-07-12 | han139242 | 登录账号状态校验 | 登录密码匹配后校验 `sys_user.status`：仅 `ENABLE` 可登录，`DISABLE`、`LOCKED` 和未知状态返回不同提示；受限账号不会写入 Redis 或生成 JWT；同步登录错误码接口文档 | 认证定向测试 6 项通过；后端完整 Maven 测试 43 项通过、0 失败；Markdown 结构检查通过 | 如需自动锁定，再补登录失败次数和解锁策略 |
| 2026-07-11 | han139242 | PC 登录页与默认主题 | 使用用户确认的蓝色羽毛球背景，仅调整登录页为左侧品牌图与右侧浅色登录工作区；将主题默认值从深色改为蓝色，保留主题切换与其他主题选择 | 前端 `npm run build` 通过；主题迁移模拟覆盖首次访问、旧默认深色和用户选择其他主题；Vite `5173` 返回 HTTP 200；`git diff --check` 通过 | 继续核心业务页面联调 |
| 2026-07-11 | han139242 | 登录页输入框视觉修复 | 根据实际页面截图，将 Chrome/Edge 自动填充覆盖的绿色输入背景改为中性炭灰，并统一普通、悬停、聚焦和自动填充状态，避免输入区域与右侧图标区域颜色断层 | 前端 `npm run build` 通过；Vite 首页与登录模块均返回 HTTP 200；旧绿色 `#486861` 已清零；刷新后的实际截图确认用户名、密码、图标区和外框颜色连续；保留第三方 PURE 注释和现有大包体积警告 | 继续登录后的核心业务页面联调 |
| 2026-07-11 | han139242 | 项目整体检查与安全收口 | 修复登录缓存不可用时失败、JWT 错误响应、密码字段外泄、Redis 反序列化、文件下载/临时文件路径越界和上传扩展名误判；静态文件目录改为读取配置；数据库和 AI 凭据改为本机环境变量；同步私人仓库凭据规则、共享库状态和全部文档路径 | 后端 `clean test` 39 项全过；前端生产构建通过、`npm audit` 0 漏洞；共享 MySQL 身份、78 张表、两位队友各 3 个连接核验通过；schema/entity/主键/实际库均为 78 且无差异；真实登录、JWT、看板和 AI 问答通过；25 份 Markdown 无坏链；Git hooks 正反场景通过；无可用浏览器实例，未完成自动截图复核；构建仍有第三方 PURE 注释和 1.08 MB 主包警告 | 三人拉取后重新运行 hooks 安装脚本并重启 IDE 读取环境变量；人工复核登录、首页、大屏和 AI 抽屉；继续核心生产/库存/质量写操作联调，公开仓库前轮换历史密钥并完成密码哈希改造 |
| 2026-07-11 | AnHC-00 | PC 登录页视觉优化 | 使用用户提供的羽毛球背景图；精简左侧品牌与标语，右侧加入轻度毛玻璃、完整焦点描边、按钮悬停反馈及页面入场动画；恢复看板预览配置文件，保留原有登录接口、校验、记住登录和开发预览逻辑 | 分支内 `npm.cmd run build` 通过且登录页开发服务返回 HTTP 200；合入 `main` 后再次生产构建通过；仅有第三方 PURE 注释和现有大包体积警告 | 人工复核登录页视觉与背景图构图，后续继续核心业务页面联调 |
| 2026-07-11 | AnHC-00 | 共享数据库环境 | 队友安装并登录 Tailscale，接受 `mes-db-host` 设备共享；将数据库连接信息写入当前用户环境变量，不改项目配置文件 | Tailscale 私网 ping 正常；`100.102.94.37:3306` 连通；共享账号只读验证 MySQL 8.0.44、`mes` 数据库和 78 张表 | 完全重启 IntelliJ 后启动后端，使用环境变量完成登录与业务接口联调 |
| 2026-07-11 | han139242 | Git 协作钩子强化 | 移除 `SKIP_COLLAB_DOC` 普通跳过入口，提交成功时显示明确检查结果，并新增 `pre-push` 校验所有待推送非合并提交是否更新 `COLLABORATION.md` | 已覆盖无协作文档时拒绝提交、协作文档暂存时允许提交、待推送提交缺少协作文档时拒绝推送三类场景 | 三名成员拉取后重新执行 `scripts/setup-git-hooks.ps1`，以后每次提交和推送都同步记录协作进展 |
| 2026-07-11 | han139242 | 三人共享数据库 | 将主机 MySQL `mes` 确认为项目唯一共享操作库，配置 Tailscale 私网、防火墙、共享账号、完整库级开发权限、Navicat/JDBC 连接和服务器 UUID 核验，并新增共享操作文档 | 两位队友分别从 `100.127.29.52`、`100.126.194.17` 接入，共建立 6 个正常连接池连接；主机、端口、库名和服务器 UUID 核验通过 | 三人统一使用共享库；结构变更先备份并提交迁移 SQL，仓库转为公开或成员变化时立即轮换共享凭据 |
| 2026-07-11 | han139242 | 登录页/生产大屏视觉收敛 | 根据用户反馈，将登录页从花哨中控风格收敛为左侧深色品牌区加右侧浅色登录卡，减少圆环、强发光、抽象信号等装饰；生产大屏移除中间羽毛球图片，改为制造现场数据拓扑卡片，展示订单、任务、质检、库存节点和采集状态 | 前端 `npm run build` 通过；`git diff --check` 仅提示相关文件 LF/CRLF 换行；浏览器工具当前无可用实例，未完成自动截图复核 | 人工打开 `/login` 和 `/big-screen/production` 复核视觉，如仍不满意再按具体截图继续微调配色、密度和视觉重心 |
| 2026-07-11 | han139242 | 登录页/生产大屏重做 | 登录页去掉旧大图方案，重做为黑色工业中控登录页，包含制造链路、羽毛球抽象信号、演示账号提示和统一深色登录卡；生产大屏重做为指挥中心布局，统一顶部状态条、KPI 条、中央制造链路、趋势图、质量/设备/风险/AI/系统面板，整体贴合当前黑色主题和工业蓝质感 | `npm run build` 通过；静态检查确认两页仅保留单一 template/style 且无旧登录样式残留；`git diff --check` 仅有 Windows 换行提示；内置浏览器不可用，未完成自动截图复核 | 人工打开 `/login` 和 `/big-screen/production` 检查视觉；如还不满意，按截图标注继续针对间距、配色、密度和视觉重心微调 |
| 2026-07-11 | han139242 | AI 登录态联调 | 在本机真实 MySQL `mes.sys_user` 补充 `admin/123456` 演示账号；启动后端 8088，完成 JWT 登录、AI 状态、普通问答、首页看板解读和模块建议真实调用；发现首页解读耗时超过前端通用 15s 超时后，将 AI 生成接口前端超时调整为 60s | 后端真实接口联调通过：登录、`/api/ai/status`、`chat`、`dashboard-insight`、`module-suggestion` 均返回成功；Vite 前端代理 `/api` 登录、AI 状态和 AI 问答通过；前端 `npm run build` 通过；内置浏览器不可用，未完成自动点击 AI 抽屉截图 | 人工在浏览器用 `admin/123456` 登录，点击顶部 AI 助手验证抽屉交互；真实业务数据补齐后再验看板解读质量 |
| 2026-07-11 | han139242 | AI 中转站配置 | 将后端 AI 默认配置切到 `https://blackaicoding.com/`、演示 API key、`gpt-5.5` 和 `reasoning_effort=high`；基础域名自动拼接 `/v1/chat/completions`；状态接口返回思考深度，前端 AI 抽屉展示模型与 high reasoning | 后端 Maven 全量测试通过：32 tests、0 failures/errors；前端 `npm run build` 通过；中转站最小真实调用成功，模型返回内容 | 启动后端并用真实账号在 AI 抽屉完成问答、看板解读和模块建议验收；如额度或模型不可用，用环境变量覆盖 key 或模型 |
| 2026-07-11 | han139242 | PC 前端 iMES 质感复刻 | 参考 iMES 截图和源码继续收口后台壳层：默认主题切为黑色并加一次性默认主题迁移；主题切换联动顶栏、侧栏、标签页、内容背景和图表；Dashboard 显示名统一为“首页”；首页改为白色工具区、彩色快捷菜单、彩色数量统计和紧凑运营关注；主题抽屉改为大矩形主题预览 | `npm run build` 通过；`git diff --check` 通过，仅有 Windows 换行提示；本地 Vite `http://127.0.0.1:5175/` 返回 200；内置浏览器不可用且 Chrome 远程调试端口未连通，未完成自动截图复核 | 人工在桌面端和移动端查看黑色默认主题、主题抽屉、首页彩色入口和业务页标签；真实账号联调时确认首页接口展示真实数据库数据 |
| 2026-07-11 | han139242 | PC 前端视觉与主题升级 | 参考 iMES 紧凑后台结构重做登录页单图主视觉，引入有明确 CC BY 2.0 许可的羽毛球成品素材；新增 5 套可持久化品牌主题，联动 Header、侧栏、标签页、工作台图表和资源页；工作台补充真实路由快捷作业与基于 Dashboard 字段的运营关注区，并优化中等宽度导航与工具栏 | `npm run build` 通过；最新开发服务 `http://127.0.0.1:5174` 的 `/login`、`/dashboard` 均返回 200；`git diff --check` 通过；内置浏览器无可用实例，本轮未完成自动截图复核 | 人工查看桌面端与移动端主题切换效果；真实环境联调时继续验证主题持久化、工作台入口和 Dashboard 数据 |
| 2026-07-11 | han139242 | PC 前端完整业务面与视觉精修 | 配置式资源工作区覆盖全部 78 个后端资源，补齐分页筛选、CRUD、导出、主从明细、只读、状态动作、追溯及上传下载；按 iMES 信息组织重做登录、工作台和大屏，主色改为钴蓝/冷青蓝，绿色仅保留业务语义；顶部统一 AI 入口 | `npm run build` 通过；1366x768 登录/工作台/资源页、1920x1080 大屏及 390x844 资源页截图通过；无页面横向溢出、可见错误或控制台异常，3 个大屏 ECharts 画布均有实际像素 | 启动 MySQL 和后端，以真实账号跑通生产、质检、入库、追溯链路；配置 AI 模型做成功与失败降级验收 |
| 2026-07-11 | han139242 | 生产任务/设备维修状态流 | 新任务强制待开工，限制开始/完成迁移并锁定运行后普通修改删除；维修单强制提交，限制派工/关闭迁移并校验结果、费用和数量；状态更新增加原状态条件，避免并发重复流转和重复写日志，并同步接口文档 | Maven 全量测试通过：31 tests、0 failures/errors；生产任务 5 项、维修工单 6 项状态流测试，覆盖设备维修状态同步 | 真实数据库联调任务开工/完工和维修派工/关闭，确认状态日志与设备状态同步 |
| 2026-07-10 | han139242 | PC 前端重点能力与视觉基线 | 在公共底座上新增独立生产运营大屏、全局 AI 助手抽屉和专用 Dashboard 解读接口；工作台增加制造指挥区、真实接口状态、重点入口和移动端重排；以 iMES-Factory 为首要参考补充场景化 UI 基线，不新增依赖 | `npm run build` 通过；浏览器检查 1920x1080、1440x900、1366x768 大屏及 390x844 工作台/AI 抽屉，图表非空、无横向溢出；`git diff --check` 通过；仅有第三方 PURE 注释和主包体积警告 | 推送稳定底座；按统一开发路线实现全部系统、主数据、生产、仓储、质量、设备和追溯界面 |
| 2026-07-10 | han139242 | PC 前端公共底座 | 在 VS Code 前端目录完成正式登录、Axios/Token、路由守卫和分片、后台 Layout、侧栏、顶部搜索/通知、标签页、模块入口、404、响应式抽屉及 Dashboard 工作台；开发预览与生产鉴权隔离，未新增依赖 | `npm run build` 通过；浏览器实测 1440x900 工作台、390x844 登录/工作台/导航抽屉，3 个 ECharts 画布非空且无横向溢出；生产预览无开发入口且控制台无错误；仅保留 Element Plus 第三方 PURE 注释和主包体积警告 | 人工查看 `http://localhost:5173` 效果；确认后合并公共底座，再从同一提交启动业务页面分工和真实登录/Dashboard 联调 |
| 2026-07-10 | han139242 | PC 前端启动复核 | 复核前端 ABC 分工、文件所有权、四轮并行顺序和实际 AI/Dashboard Controller；补充当前启动状态，并清理 AI Controller 尚未实现时期的旧表述 | 前端 `npm run build` 通过；Markdown 表格、关键路径、过时 AI 表述和 `git diff --check` 检查通过；构建仅有第三方 PURE 注释和大包体积警告 | 三人从独立分支同时执行第 0 轮；先合并 A 的公共底座，再接入 B/C 独立目录并进入第 1 轮 |
| 2026-07-10 | han139242 | AI 后端/状态统一 | 新增 OpenAI 兼容 AI Controller/Service、状态查询、普通问答、真实看板解读和模块建议；过滤敏感上下文且禁止 AI 自动写业务；统一看板/质检/追溯状态为英文编码，修复不合格品审批状态混写并补齐审批/状态审计记录，新增幂等状态迁移脚本并移除 `schema.sql` 硬编码 `USE mes` | Maven 全量测试通过：20 tests、0 failures/errors；MySQL 8.0 临时库建表 78 张，迁移脚本连续执行两次后编码和审批状态正确；schema/entity/@TableId/Mapper 均为 78；`git diff --check` 通过 | 前端 C 先联调 `/api/ai/status` 和失败状态；后端配置真实 `AI_API_KEY` 后完成一次成功问答与超时/失败验收；旧业务库先备份再执行状态迁移 |
| 2026-07-10 | han139242 | PC 前端 ABC 分工复核 | 重写前端 ABC 分工、文件所有权、并行轮次、UI/大屏/AI 验收和三份 AI 执行提示词；调整为 A 略少、B/C 略多且接近，AI 全部归 C；核对实际 Controller/ResultData/package.json，记录认证、主从接口和状态值差异 | Markdown 代码块、表格、尾随空格和关键路径检查通过；官网链接可访问，14 个 GitHub 仓库均可解析 HEAD；只改文档，未运行前端构建 | 三人分别对 AI 说“我负责前端 A/B/C”启动第 0 轮；同时优先补 AI Controller 契约并统一后端状态值 |
| 2026-07-10 | han139242 | PC 前端开发规划 | 新增 `相关文档，图片/md/前端ABC分工开发计划.md`，整理前端资源参考、视觉规范、目录约定、A/B/C 分工、并行轮次、可视化大屏、AI 接入、页面验收标准和联调规则 | 文档检查通过；未改业务代码，未运行构建 | 按第 0 轮先完成 Layout、请求封装、CRUD 模板、可视化大屏草案和 AI 助手入口 |
| 2026-07-10 | han139242 | 项目提交前整理 | 完成后端分业务域目录整理；修正 Swagger 扫描旧 `com.neuedu.health` 包、移除未使用的 DeepSeek 明文密钥配置、规范 `HelloController` 为 `/api/hello` + `ResultData`；保留并验证 PC 前端基础框架改动 | 后端 Maven `test` 通过；MySQL 8.0 临时库执行 `schema.sql` 生成 78 张表；schema/entity/主键/Mapper namespace 检查通过；前端 `npm run build` 通过；`git diff --check` 通过 | 推送后进入 PC 前端页面开发和前后端联调 |
| 2026-07-10 | han139242 | 后端包结构整理 | 将后端 Controller、Service、ServiceImpl、Mapper、Entity 按业务域整理到 sys/org/mdm/process/factory/mfg/wms/qms/eam/trace/dashboard/common 等子包；同步更新 Java package/import 与 Mapper XML namespace/type 引用，并移除批量改写产生的 UTF-8 BOM | 后端 Maven `test` 通过：Tests run: 1, Failures: 0, Errors: 0；静态检查未发现旧根包引用或 BOM | 后续前后端联调时按新包路径查找后端文件，接口路径保持不变 |
| 2026-07-10 | AnHC-00 | 条码追溯/首页看板 | 新增 `trace_barcode`、`trace_product_lot`、`trace_material_usage` 后端接口和 `/api/dashboard` 看板接口；条码支持生成、扫码、通用 CRUD 和导出；产品批次支持按批次号追溯物料使用与检验摘要；物料使用支持按原料批次查询；看板提供汇总、生产趋势、质量统计和设备状态统计 | schema/entity 数量检查通过：78/78/78；Maven `test` 通过：Tests run: 1, Failures: 0, Errors: 0 | C 继续前后端联调或补充追溯看板细节 |
| 2026-07-10 | AnHC-00 | 设备管理 | 新增 `eam_equipment_status_log`、`eam_maintenance_plan`、`eam_maintenance_record`、`eam_spot_check_plan`、`eam_spot_check_record`、`eam_repair_order` 后端接口；设备状态日志仅开放分页、详情和导出；保养计划/记录与点检计划/记录提供通用 CRUD 和导出；维修单补充派工 `/assign` 和关闭 `/close`，并同步设备状态、设备状态日志、状态变更日志和操作日志 | schema/entity 数量检查通过：78/78/78；Maven `test` 通过：Tests run: 1, Failures: 0, Errors: 0 | C 继续追溯、看板或前后端联调 |
| 2026-07-10 | AnHC-00 | 补货/报工/质检/不合格品 | 新增 `wms_replenishment_suggestion`、`mfg_work_report`、`mfg_report_material`、`qms_inspection_order`、`qms_inspection_result`、`qms_nonconforming_order`、`qms_nonconforming_detail` 后端接口；补货建议支持按物料/仓库生成；报工支持提交、审核和用料明细 `/full`；检验单支持主从表 `/full`、提交检验结果和检验结果批量保存；不合格品处理单支持主从表 `/full` 和审批；已审核/已完成单据禁止通过主表或明细接口绕过修改 | schema/entity 数量检查通过：78/78/78；Maven `clean test` 通过：Tests run: 1, Failures: 0, Errors: 0；`git diff --check` 仅提示 `COLLABORATION.md` LF/CRLF | C 继续追溯、看板或前后端联调 |
| 2026-07-10 | AnHC-00 | 仓储出库/退料/盘点 | 新增 `wms_outbound_order`、`wms_outbound_detail`、`wms_return_order`、`wms_return_detail`、`wms_stocktake_order`、`wms_stocktake_detail` 后端接口；主单支持分页、详情、新增、修改、软删除、导出、`/full` 主从表保存和业务确认；出库确认扣减库存并写出库流水，退料确认回加库存并写退料流水，盘点完成按差异调整库存并写盘点流水；已确认/已完成单据禁止重复确认、修改、删除及通过明细接口绕过修改 | schema/entity 数量检查通过：78/78/78；Maven `clean test` 通过：Tests run: 1, Failures: 0, Errors: 0；`git diff --check` 仅提示 `COLLABORATION.md` LF/CRLF | C 继续仓储补货建议、报工/质检联动或前后端联调 |
| 2026-07-10 | AnHC-00 | 仓储批次/库存/入库 | 新增 `wms_material_batch`、`wms_inventory`、`wms_stock_movement`、`wms_inbound_order`、`wms_inbound_detail` 后端接口；库存余额和库存流水仅开放查询/详情/导出；入库单支持主从表 `/full` 和确认入库，确认时事务内更新库存余额、写入库存流水、状态日志和操作日志，已确认入库单禁止重复确认、修改和删除 | schema/entity 数量检查通过：78/78/78；Maven `test` 通过：Tests run: 1, Failures: 0, Errors: 0 | C 继续出库单/明细、退料或盘点模块 |
| 2026-07-10 | AnHC-00 | 生产任务/人员/异常/安灯 | 新增 `mfg_task`、`mfg_task_operator`、`mfg_exception`、`mfg_andon_alert` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出；补充任务开始/完成、按任务查人员、异常处理、安灯触发/关闭接口；安灯触发字段以 `schema.sql` 实体为准，文档示例中不存在的字段不落库 | schema/entity 数量检查通过：78/78/78；Maven `test` 通过：Tests run: 1, Failures: 0, Errors: 0 | C 继续报工、报工用料或仓储库存模块 |
| 2026-07-10 | AnHC-00 | C 后端注释和接口修复 | 补齐 `mfg_work_order_material`、`qms_inspection_standard`、`qms_inspection_scheme`、`qms_inspection_scheme_item` Controller/Service/ServiceImpl 方法注释；移除质检标准旧 `getPage/export` Service 方法；补充按工单查用料和按检验方案查明细接口；统一导出标题和 Mapper 中文说明 | schema/entity 数量检查通过：78/78/78；Maven `test` 通过：Tests run: 1, Failures: 0, Errors: 0 | C 继续后续生产、仓储、质量模块联调 |
| 2026-07-09 | AnHC-00 | C 后端代码检查修复 | 检查并修复 `mfg_work_order_material`、`qms_inspection_standard`、`qms_inspection_scheme`、`qms_inspection_scheme_item` 后端代码；统一 QMS 接口 `/api/qms/...` 路径、`current/size` 分页和 `/excel` 导出，修复质检标准物理删除风险，查询过滤 `deleted_flag = 0`，删除改为软删除 | Maven `clean test` 通过：Tests run: 1, Failures: 0, Errors: 0 | C 继续后续生产/仓储/质量模块联调 |
| 2026-07-09 | zengjingye | 审批记录管理 | 新增 `sys_approval_record` 后端通用 CRUD 和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；支持业务、审批节点、审批人、审批结果和时间范围筛选 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步复查 A 模块完整性 |
| 2026-07-09 | zengjingye | 状态变更日志管理 | 新增 `sys_status_change_log` 只读后端接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；支持分页、详情、导出，以及业务类型、业务 ID、状态、变更人、时间范围筛选 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续审批记录 |
| 2026-07-09 | zengjingye | 操作日志管理 | 新增 `sys_operation_log` 只读后端接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；支持分页、详情、导出，以及用户、操作类型、模块、业务 ID、时间范围筛选 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续状态变更日志 |
| 2026-07-09 | zengjingye | 附件文件管理 | 新增 `sys_file` 上传、下载、通用 CRUD 和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；复用现有文件工具并补充通用上传方法 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续操作日志 |
| 2026-07-09 | zengjingye | 系统参数管理 | 新增 `sys_system_param` 后端通用 CRUD 和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；支持参数键/名称关键字和模块编码筛选 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续附件文件 |
| 2026-07-09 | zengjingye | 字典数据管理 | 新增 `sys_dict_data` 后端通用 CRUD、按字典类型查询和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；因表结构无 `status` 字段，接口不按 status 过滤 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续系统参数 |
| 2026-07-09 | zengjingye | 字典类型管理 | 新增 `sys_dict_type` 后端通用 CRUD 和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；支持字典类型编码/名称关键字和状态筛选 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续字典数据 |
| 2026-07-09 | zengjingye | 用户和菜单管理 | 将早期 `UserController` / `UserService` / `UserServiceImpl` 收敛为接口文档要求的 `SysUserController` / `SysUserService` / `SysUserServiceImpl`；保留 `/api/sys/user` 路径，补充分页、软删除、重置密码、分配角色接口；新增 `sys_menu` 后端 CRUD、菜单树和导出接口 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续字典类型 |
| 2026-07-09 | zengjingye | 班次管理 | 新增 `org_shift` 后端通用 CRUD 和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；因表结构无 `status` 字段，接口不按 status 过滤 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续字典类型 |
| 2026-07-09 | zengjingye | 班组成员管理 | 新增 `org_team_member` 后端通用 CRUD、按班组查询成员接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；查询默认过滤 `deleted_flag = 0`，删除为软删除 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续班次管理 |
| 2026-07-09 | AnHC-00 | 库位管理 | 新增 `wms_location` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出；支持库位编码/名称搜索、仓库筛选、库位类型筛选、状态筛选 | Maven 测试通过 | C 继续仓储其他模块（库存、出入库等） |
| 2026-07-09 | AnHC-00 | 仓库管理 | 新增 `wms_warehouse` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出；支持仓库编码/名称搜索、仓库类型筛选、状态筛选 | Maven 测试通过 | C 继续库位管理模块 |
| 2026-07-09 | AnHC-00 | 生产工单 | 新增 `mfg_work_order` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出；支持工单编号搜索、状态筛选、生产计划筛选、产品筛选 | Maven 测试通过 | C 继续仓储模块或其他生产模块 |
| 2026-07-09 | zengjingye | 班组管理 | 新增 `org_team` 后端通用 CRUD 和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；支持班组编码/名称、部门、班组长筛选；因表结构无 `status` 字段，接口不按 status 过滤 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续班组成员管理 |
| 2026-07-09 | zengjingye | 员工管理 | 新增 `org_employee` 后端通用 CRUD 和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；支持工号/姓名/电话关键字、部门、状态筛选 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续班组管理 |
| 2026-07-09 | zengjingye | 部门管理 | 新增 `org_department` 后端通用 CRUD、部门树和导出接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；因表结构无 `status` 字段，接口不按 status 过滤 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续员工管理 |
| 2026-07-09 | zengjingye | 生产计划 | 新增 `mfg_production_plan` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出；支持计划编号搜索、状态筛选、生产订单筛选、产线筛选 | Maven 测试通过 | C 继续生产工单或仓储模块 |
| 2026-07-09 | zengjingye | 角色菜单关联 | 新增 `sys_role_menu` 后端通用 CRUD、按角色查询菜单关联接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；ServiceImpl 显式调用 `baseMapper`，查询默认过滤 `deleted_flag = 0`，删除为软删除 | Maven test 通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续组织基础模块 |
| 2026-07-09 | zengjingye | 用户角色关联 | 新增 `sys_user_role` 后端通用 CRUD、按用户查询角色关联接口，补齐 Controller / Service / ServiceImpl / Mapper / XML；ServiceImpl 显式调用 `baseMapper`，查询默认过滤 `deleted_flag = 0`，删除为软删除 | 已使用 JDK 21 和本机 MySQL 环境变量运行 Maven test，通过：Tests run: 1, Failures: 0, Errors: 0 | 下一步继续 `sys_role_menu` |
| 2026-07-09 | AnHC-00 | 生产订单 | 新增 `mfg_production_order` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出；支持订单编号搜索、状态筛选、客户筛选、产品筛选 | Maven 测试通过 | C 继续生产计划或生产工单模块 |
| 2026-07-09 | AnHC-00 | 设备台账 | 新增 `eam_equipment` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出；支持设备编码/名称搜索、状态筛选、分类筛选 | Maven 测试通过 | C 继续生产订单模块 |
| 2026-07-09 | AnHC-00 | 设备分类 | 新增 `eam_equipment_category` 后端通用管理接口，包含分页、详情、新增、修改、软删除、树形结构查询、导出；支持分类编码/名称搜索、状态筛选 | Maven 测试通过 | C 继续设备台账模块 |
| 2026-07-09 | AnHC-00 | 质检项目 | 新增 `qms_inspection_item` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出；支持项目编码/名称搜索、检验类型筛选 | Maven 测试通过 | C 继续设备分类或质检标准模块 |
| 2026-07-09 | han139242 | 工位工序 | 新增 `factory_station_operation` 后端通用管理接口，包含分页、详情、新增、修改、软删除、按工位查询可执行工序、导出和 Mapper XML | Maven 测试通过 | B 分工后端模块已完成，等待联调或补接口细节 |
| 2026-07-09 | han139242 | 工位 | 新增 `factory_station` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续工位工序模块 |
| 2026-07-09 | han139242 | 产线 | 新增 `factory_line` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续工位模块 |
| 2026-07-09 | han139242 | 车间 | 新增 `factory_workshop` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续产线模块 |
| 2026-07-09 | han139242 | 作业指导书 | 新增 `process_work_instruction` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续车间模块 |
| 2026-07-09 | han139242 | 工艺参数 | 新增 `process_parameter` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续作业指导书模块 |
| 2026-07-09 | han139242 | 工艺路线明细 | 新增 `process_route_item` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续工艺参数模块 |
| 2026-07-09 | han139242 | 工艺路线 | 新增 `process_route` 主表后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML；路线明细按文档拆到下一个模块单独实现 | Maven 测试通过 | B 继续工艺路线明细模块 |
| 2026-07-09 | han139242 | 工序 | 新增 `process_operation` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续工艺路线模块 |
| 2026-07-09 | han139242 | BOM 明细 | 新增 `mdm_bom_item` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续工序模块 |
| 2026-07-09 | han139242 | BOM | 新增 `mdm_bom` 主表后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML；BOM 明细按文档拆到下一个模块单独实现 | Maven 测试通过 | B 继续 BOM 明细模块 |
| 2026-07-09 | han139242 | 产品 | 新增 `mdm_product` 后端管理接口，按文档支持产品编码、名称、状态分页过滤，并包含详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续 BOM 或 BOM 明细模块 |
| 2026-07-09 | han139242 | 物料 | 新增 `mdm_material` 后端管理接口，按文档支持物料编码、名称、分类、状态分页过滤，并包含详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续产品或 BOM 模块 |
| 2026-07-09 | han139242 | 物料分类 | 新增 `mdm_material_category` 后端管理接口，包含分页、树形查询、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续物料或产品模块 |
| 2026-07-09 | han139242 | 客户 | 新增 `mdm_customer` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续物料分类或物料模块 |
| 2026-07-09 | han139242 | 供应商 | 新增 `mdm_supplier` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续客户或物料分类模块 |
| 2026-07-09 | han139242 | 计量单位 | 新增 `mdm_unit` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | B 继续供应商或客户模块 |
| 2026-07-09 | han139242 | 后端协作分工 | 新增 Markdown 版 A/B/C 后端分工 AI 提示词，补充分层风格、并行冲突处理、单模块流程和群内交接格式 | 文档入口和 Markdown 标题已检查 | 群内按 A/B/C 认领后每次只让 AI 做一个模块 |
| 2026-07-09 | han139242 | 角色管理 | 将 `SysRoleServiceImpl` 的通用 CRUD 调整为显式 `baseMapper` 调用，并同步分层手册说明 | Maven 测试通过 | 后续普通模块优先使用更直观的 `baseMapper` 风格 |
| 2026-07-09 | han139242 | 后端分层理解 | 新增后端分层理解手册，说明 `SysRole` 模块写法、Mapper 为空原因、Service 业务边界和答辩说明 | Maven 测试通过，文档入口已检查 | 后续成员按手册理解普通 CRUD 和复杂业务分层 |
| 2026-07-09 | han139242 | 角色管理 | 调整 `sys_role` 分层：Controller 只处理请求返回，查询、默认值、软删除等规则下沉到 Service | Maven 测试通过 | 后续模块按此分层模板继续实现 |
| 2026-07-09 | han139242 | 角色管理 | 新增 `sys_role` 后端通用管理接口，包含分页、详情、新增、修改、软删除、导出和 Mapper XML | Maven 测试通过 | 继续补角色菜单分配或前端角色管理页面 |
| 2026-07-09 | han139242 | 文档目录 | 将设计、数据库、接口和 PDF 统一整理到 `相关文档/`，并同步 README、协作文档和后端约束里的文档路径 | 已检查新目录文件和链接引用 | 后续统一从 `相关文档/` 查设计资料 |
| 2026-07-09 | han139242 | AI 开发规范 | 新增 `AGENTS.md`、Copilot 指令和 12 个 MES 专用 skills，覆盖项目进入、后端、数据库、前端、联调、调试、验证、审查和进度更新 | 12 个 skill 已通过结构校验 | 后续开发按任务读取对应 skill |
| 2026-07-09 | han139242 | 协作规范 | 新增 Git 提交提醒钩子，提交时检查是否同步更新 `COLLABORATION.md` | 已加入 `.githooks/pre-commit` 和安装脚本 | 成员拉取后执行一次 hooks 配置 |
| 2026-07-09 | han139242 | 协作规范 | 新增三人通用协同开发说明，补充 README 文档入口 | 已检查 Markdown 内容和链接路径 | 后续每次开发同步更新进度 |
| 2026-07-09 | han139242 | 项目基线 | 同步后端基线、数据库脚本、接口文档，修复前端构建入口 | 后端测试通过，前端构建通过 | 开始登录和用户管理联调 |

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

仓库提供 `.githooks/pre-commit` 和 `.githooks/pre-push`：提交时强制检查本次提交是否更新 `COLLABORATION.md`，推送时再次检查所有待推送的非合并提交。

每位成员拉取仓库后执行一次：

```powershell
.\scripts\setup-git-hooks.ps1
```

生效后，如果提交时没有暂存 `COLLABORATION.md`，Git 会中断提交；如果待推送提交中存在未更新协作文档的提交，Git 会拒绝推送。即使是错别字、冲突修复或文档小改，也要增加一条简短、真实的协作记录，不再提供普通跳过入口。

推送完成后，下一次开发开始时按 GitHub Event ID 增量同步 `相关文档，图片/md/GitHub仓库操作审计.md`。Push、Create、Delete 和非 `main` 分支同样需要记录；进度表描述功能内容，审计表只描述仓库操作。

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
| 接口路径和前端请求不一致 | 以 `相关文档，图片/md/后端接口文档.md` 为准，同时修正前端调用 |
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
