# 羽毛球制造 MES 微信小程序需求设计文档

## 1. 项目定位

本小程序是现有羽毛球制造 MES 系统的移动端延伸，面向车间现场和移动办公场景。小程序复用现有 Spring Boot 后端接口，以账号密码登录为主，提供工单查看、任务执行、生产报工、移动工作台和个人中心能力。

小程序界面风格参考现有 iMES 客户端：顶部蓝色导航栏、浅灰页面背景、白色圆角业务卡片、底部五栏导航。业务内容需要结合羽毛球制造系统，不直接照搬通用 iMES 模板。

## 2. 适用人群

- 车间工人：查看个人任务、开始任务、提交报工、查看工序要求。
- 班组长：查看班组工单和任务进度，跟踪异常和报工情况。
- 质检员：查看待检任务、检验报工结果、登记质检结果。
- 仓库人员：查看物料库存、出入库、齐套状态和生产领料需求。
- 设备维修人员：查看设备异常、维修工单、处理维修任务。
- 管理员：查看全局数据、账号信息、系统入口和基础管理能力。

## 3. 技术路线

### 3.1 推荐方案：原生微信小程序

原生微信小程序是微信官方提供的开发方式，使用微信开发者工具创建项目，主要文件类型为：

- `.wxml`：页面结构，类似 HTML。
- `.wxss`：页面样式，类似 CSS。
- `.js`：页面逻辑和接口请求。
- `.json`：页面配置、导航栏、权限等配置。

推荐原生小程序的原因：

- 你当前已经使用微信开发者工具。
- 本项目第一版只发布微信小程序，不需要同时发布 App、H5、支付宝小程序。
- 原生方式对微信登录、扫码、缓存、上传、订阅消息等微信能力支持最直接。
- 项目结构简单，适合课程设计和团队协作说明。

### 3.2 uni-app 是什么

uni-app 是一个跨端框架，通常用 Vue 语法写一套代码，然后编译到微信小程序、H5、App、支付宝小程序等多个平台。

uni-app 适合以下场景：

- 后续明确要同时做微信小程序、H5、Android App、iOS App。
- 团队更熟悉 Vue。
- 需要复用已有 Vue 前端的大量组件或页面逻辑。

本项目暂不建议第一版使用 uni-app。原因是当前需求以微信小程序为目标，优先把 MES 现场业务跑通更重要，原生小程序实现成本更低，和微信开发者工具配合更直接。

## 4. 后端复用方案

小程序复用当前后端服务。

基础地址：

```text
开发环境：http://localhost:8088
接口前缀：/api
```

登录接口：

```text
POST /api/auth/login
GET  /api/auth/info
POST /api/auth/logout
```

认证方式：

- 用户输入账号和密码。
- 登录成功后后端返回 `token`。
- 小程序将 token 存入本地缓存。
- 后续请求在 Header 中携带：

```text
Authorization: Bearer <token>
```

## 5. 底部导航

底部导航采用五个入口：

| Tab | 页面 | 说明 |
| --- | --- | --- |
| 工单 | `/pages/work-order/index` | 生产工单列表、筛选、详情入口 |
| 任务 | `/pages/task/index` | 生产任务列表、开始任务、进入报工 |
| iMES | `/pages/workbench/index` | 移动工作台，聚合质检、仓库、设备、看板入口 |
| 报工 | `/pages/report/index` | 报工记录列表、报工详情 |
| 我的 | `/pages/profile/index` | 当前用户、修改密码、退出登录、系统信息 |

## 6. 页面设计

### 6.1 登录页

页面参考客户端账号登录样式，但去掉桌面端、小程序、Android、H5 跳转入口。小程序本身就是移动端入口。

字段：

| 字段 | 说明 |
| --- | --- |
| username | 用户名 |
| password | 密码 |
| captcha | 第一版可不做；如后端未来支持验证码再接入 |

按钮：

- 登录
- 清空输入

登录成功后：

- 保存 token。
- 保存当前用户信息。
- 跳转到工单页或上次访问页。

### 6.2 工单页

对应后端：

```text
GET /api/mfg/work-order/page
GET /api/mfg/work-order/{workOrderId}
```

列表筛选：

| 筛选项 | 对应参数 | 说明 |
| --- | --- | --- |
| 状态 | status | 工单状态 |
| 排序 | 前端参数 | 默认按创建时间或工单号倒序 |
| 选择产品 | productId | 按产品筛选 |
| 搜索 | keyword | 工单编号搜索 |

卡片字段：

| 展示名 | 后端字段 |
| --- | --- |
| 工单编号 | workOrderNo |
| 产品信息 | productId，需关联产品编码/名称 |
| 计划数 | plannedQuantity |
| 实际数 | completedQuantity |
| 良品数 | 第一版可由任务/报工汇总 |
| 不良品数 | 第一版可由任务/报工汇总 |
| 状态 | status |

状态字典：

| 后端值 | 小程序展示 |
| --- | --- |
| DRAFT | 新建 |
| APPROVED | 已审核 |
| RELEASED | 已下达 |
| RUNNING | 生产中 |
| FINISHED | 已完成 |
| CLOSED | 已关闭 |

卡片操作：

- 查看详情
- 查看任务
- 发起报工

### 6.3 工单详情页

对应后端：

```text
GET /api/mfg/work-order/{workOrderId}
GET /api/mfg/task/page?workOrderId=<workOrderId>
GET /api/mfg/work-order-material/work-order/{workOrderId}
```

页面结构：

- 工单信息卡片。
- 生产进度区域。
- 任务/工序列表。
- 物料需求入口。
- 质检记录入口。

生产进度建议：

- 计划数：`plannedQuantity`
- 已完成数：`completedQuantity`
- 不良数：任务或报工汇总 `defectQuantity`
- 进度百分比：`completedQuantity / plannedQuantity`

### 6.4 任务页

对应后端：

```text
GET /api/mfg/task/page
GET /api/mfg/task/{taskId}
PUT /api/mfg/task/{taskId}/start
PUT /api/mfg/task/{taskId}/finish
```

筛选项：

| 筛选项 | 对应参数 |
| --- | --- |
| 人员筛选 | 第一版可按当前用户过滤，后端后续可扩展 operatorId |
| 排序 | 前端参数 |
| 选择工序 | operationId |
| 状态 | status |

卡片字段：

| 展示名 | 后端字段 |
| --- | --- |
| 任务编号 | taskNo |
| 工单 | workOrderId，需关联工单编号 |
| 工序 | operationId，需关联工序名称 |
| 工位 | stationId |
| 计划数 | plannedQuantity |
| 已完成数 | completedQuantity |
| 不良数 | defectQuantity |
| 状态 | status |

任务状态：

| 后端值 | 小程序展示 |
| --- | --- |
| WAIT_START | 待开工 |
| RUNNING | 生产中 |
| PAUSED | 暂停 |
| FINISHED | 已完工 |
| CLOSED | 已关闭 |

任务操作：

- 查看详情
- 开始任务
- 报工
- 完成任务

### 6.5 任务详情页

对应后端：

```text
GET /api/mfg/task/{taskId}
GET /api/mfg/work-report/page?taskId=<taskId>
```

页面结构：

- 任务基本信息。
- 所属工单信息。
- 工序信息。
- 生产进度。
- 报工记录。

按钮：

- 开始任务
- 报工
- 完成任务

### 6.6 报工页

对应后端：

```text
GET  /api/mfg/work-report/page
POST /api/mfg/work-report
POST /api/mfg/work-report/full
POST /api/mfg/work-report/submit
GET  /api/mfg/work-report/{reportId}
```

报工列表字段：

| 展示名 | 后端字段 |
| --- | --- |
| 报工编号 | reportNo |
| 任务 | taskId |
| 报工人 | operatorId |
| 工位 | stationId |
| 良品数 | qualifiedQuantity |
| 不良数 | defectQuantity |
| 实际工时 | actualWorkHours |
| 报工时间 | reportTime |
| 审核状态 | auditStatus |

报工表单字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| taskId | 是 | 当前任务 |
| operatorId | 是 | 当前登录人或选择员工 |
| stationId | 否 | 当前工位 |
| qualifiedQuantity | 是 | 良品数 |
| defectQuantity | 是 | 不良数 |
| actualWorkHours | 否 | 实际工时 |
| reportTime | 是 | 默认当前时间 |
| remark | 否 | 备注 |
| materials | 否 | 报工用料明细 |

报工状态：

| 后端值 | 小程序展示 |
| --- | --- |
| SUBMITTED | 待审核 |
| APPROVED | 通过 |
| REJECTED | 驳回 |

### 6.7 iMES 工作台

iMES 页不是简单空页面，建议作为移动工作台，放常用业务入口。

第一版入口：

- 生产看板：`/api/dashboard/summary`
- 质检单：`/api/qms/inspection-order/page`
- 不合格品：`/api/qms/nonconforming-order/page`
- 库存查询：`/api/wms/inventory/page`
- 出入库单：`/api/wms/inbound-order/page`、`/api/wms/outbound-order/page`
- 设备维修：`/api/eam/repair-order/page`
- AI 助手：`/api/ai/chat`，可作为后续增强

### 6.8 我的页

对应后端：

```text
GET  /api/auth/info
POST /api/auth/logout
PUT  /api/sys/user/{userId}/reset-password
```

页面内容：

- 头像：默认使用 iMES/MES 图标。
- 用户名：`username`
- 角色：`roles`
- 权限：`permissions`
- 上次登录时间：`lastLoginTime`

功能入口：

- 关于 iMES / 羽毛球 MES
- 帮助中心
- 修改密码
- 切换登录
- 退出系统

## 7. 权限设计

第一版建议使用后端返回的角色和权限控制页面入口。

| 角色 | 可见模块 | 主要权限 |
| --- | --- | --- |
| 车间工人 | 工单、任务、报工、我的 | 查看任务、提交报工 |
| 班组长 | 工单、任务、报工、iMES、我的 | 查看班组进度、任务处理 |
| 质检员 | 任务、报工、iMES、我的 | 质检单处理、不合格品记录 |
| 仓库人员 | iMES、我的 | 库存、出入库、齐套查询 |
| 设备维修人员 | iMES、我的 | 维修单查看和处理 |
| 管理员 | 全部 | 全部模块入口 |

## 8. 接口封装规范

小程序建议统一封装 `request` 方法。

请求处理：

- 自动拼接 `baseUrl`。
- 自动携带 token。
- 统一处理 `code !== 200` 的错误。
- token 失效时清理缓存并跳转登录页。

返回格式沿用现有后端：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

## 9. 小程序目录建议

```text
miniprogram/
  app.js
  app.json
  app.wxss
  utils/
    request.js
    auth.js
    status.js
  api/
    auth.js
    work-order.js
    task.js
    report.js
    dashboard.js
    qms.js
    wms.js
    eam.js
  pages/
    login/
    work-order/
      index
      detail
    task/
      index
      detail
    report/
      index
      detail
      form
    workbench/
      index
    profile/
      index
      password
  components/
    business-card/
    status-tag/
    filter-bar/
    empty-state/
```

## 10. 第一版开发范围

第一阶段先完成闭环：

1. 账号密码登录。
2. 工单列表和工单详情。
3. 任务列表和任务详情。
4. 任务开始。
5. 生产报工提交。
6. 报工记录查看。
7. 我的页面和退出登录。

第二阶段再补充：

1. 质检处理。
2. 仓库库存和齐套。
3. 设备维修。
4. AI 助手。
5. 消息提醒。
6. 扫码查工单、扫码查物料、扫码查设备。

## 11. 需要后端补强的点

当前后端已有基础接口，但小程序体验更好需要补充聚合接口：

- 工单列表返回产品编码、产品名称，而不是只返回 `productId`。
- 任务列表返回工单编号、工序名称、工位名称，而不是只返回 ID。
- 报工列表返回任务编号、工单编号、工序名称、操作人姓名。
- 工单详情返回任务列表、报工汇总、物料齐套、质检结果。
- 当前用户任务接口：按登录用户过滤任务。
- 移动端首页统计接口：今日任务数、待报工数、待质检数、异常数。

## 12. 视觉风格

参考截图风格：

- 主色：亮蓝色，接近 `#17aee8`。
- 页面背景：浅灰色。
- 内容容器：白色卡片。
- 卡片圆角：8px 左右。
- 状态色：
  - 待开工/未开始：橙色。
  - 生产中：蓝色。
  - 已完成：绿色。
  - 异常/驳回：红色。
  - 已关闭：灰色。

小程序要避免过度装饰，页面以车间现场可读性为主：字体清晰、按钮大、关键数量明显、操作路径短。

 