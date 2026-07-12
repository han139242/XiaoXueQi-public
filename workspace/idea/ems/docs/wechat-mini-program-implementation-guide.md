# 羽毛球 MES 微信小程序实施文档

本文档用于指导使用微信开发者工具直接制作本系统对应的小程序。功能范围以后端 `ems` 项目已经提供的 Spring Boot 接口为准，页面风格参考：

```text
D:\大二暑期小学期\git-soft\XiaoXueQi-main\相关文档，图片\image\WeChat xiaochengxu
```

## 1. 项目目标

小程序是羽毛球 MES 系统的移动端入口，重点服务车间现场和移动办公场景。第一版不追求完整搬运 PC 端全部管理功能，而是先打通生产现场最核心闭环：

1. 用户登录和当前用户信息获取。
2. 生产工单列表、筛选、详情查看。
3. 生产任务列表、筛选、详情查看、开始任务、完成任务。
4. 生产报工列表、报工详情、新增报工、提交报工。
5. 移动工作台入口：看板、质检、库存、出入库、设备维修、扫码追溯。
6. 我的页面：用户信息、角色权限、退出登录。

第二版再扩展质检处理、仓储确认、设备维修派工、扫码生成和 AI 助手。

## 2. 技术选型

推荐使用原生微信小程序。

原因：

1. 当前目标只发布微信小程序，不需要同步发布 App、H5 或支付宝小程序。
2. 原生小程序和微信开发者工具配合最直接，适合课程项目落地。
3. 本项目页面以列表、表单、详情、筛选为主，原生 `wxml + wxss + js` 成本低。
4. 微信扫码、缓存、上传、请求拦截、TabBar 配置都可以直接使用官方能力。

不建议第一版使用 uni-app。除非团队后续明确需要一套代码多端发布，或者已经有成熟 Vue 组件体系需要复用。

## 3. 后端联调约定

### 3.1 后端地址

开发环境：

```text
http://localhost:8088
```

接口统一前缀：

```text
/api
```

微信开发者工具访问本机后端时，需要在“详情 -> 本地设置”中勾选：

```text
不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书
```

真机预览时，手机不能访问电脑的 `localhost`。需要改成电脑局域网 IP，例如：

```text
http://192.168.1.10:8088
```

并确保手机和电脑在同一网络，后端允许跨域。

### 3.2 认证方式

登录接口：

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

后端返回格式：

```json
{
  "msg": "操作成功",
  "code": 200,
  "data": {
    "userId": 1,
    "username": "admin"
  },
  "token": "jwt-token"
}
```

后续请求必须携带：

```http
Authorization: Bearer <token>
```

退出登录：

```http
POST /api/auth/logout
```

当前用户信息：

```http
GET /api/auth/info
```

### 3.3 统一返回格式

普通接口：

```json
{
  "msg": "操作成功",
  "code": 200,
  "data": {}
}
```

分页接口：

```json
{
  "msg": "操作成功",
  "code": 200,
  "data": {
    "records": [],
    "total": 0,
    "size": 10,
    "current": 1,
    "pages": 0
  }
}
```

小程序统一判断：

1. `code === 200`：业务成功。
2. `code !== 200`：弹出 `msg`，必要时停留当前页面。
3. HTTP 401 或接口提示登录失效：清理本地 token，跳转登录页。

## 4. 微信开发者工具创建项目

### 4.1 创建步骤

1. 打开微信开发者工具。
2. 选择“小程序”。
3. 点击“新建项目”。
4. 项目目录建议放在：

```text
ems/miniprogram
```

如果当前仓库暂时不想放小程序代码，也可以放在同级：

```text
workspace/idea/miniprogram
```

5. AppID：

```text
wx596570b6ec2dd957
```

6. 开发模式：

```text
小程序
```

7. 后端服务：

```text
不使用云服务
```

8. 模板：

```text
JavaScript 基础模板
```

### 4.2 推荐目录结构

```text
miniprogram/
  app.js
  app.json
  app.wxss
  project.config.json
  sitemap.json
  assets/
    icons/
    images/
  config/
    env.js
  utils/
    request.js
    auth.js
    status.js
    format.js
  api/
    auth.js
    dashboard.js
    work-order.js
    task.js
    work-report.js
    qms.js
    wms.js
    eam.js
    trace.js
    mdm.js
    process.js
    factory.js
  components/
    status-tag/
      index.wxml
      index.wxss
      index.js
      index.json
    data-card/
      index.wxml
      index.wxss
      index.js
      index.json
    filter-bar/
      index.wxml
      index.wxss
      index.js
      index.json
    empty-state/
      index.wxml
      index.wxss
      index.js
      index.json
  pages/
    login/
      index.wxml
      index.wxss
      index.js
      index.json
    work-order/
      index/
      detail/
    task/
      index/
      detail/
      finish/
    report/
      index/
      detail/
      form/
    workbench/
      index/
    qms/
      inspection-list/
      inspection-detail/
    wms/
      inventory-list/
      inbound-list/
      outbound-list/
    eam/
      repair-list/
      repair-detail/
    trace/
      scan-result/
    profile/
      index/
```

第一版必须完成的页面：

```text
pages/login/index
pages/work-order/index
pages/work-order/detail
pages/task/index
pages/task/detail
pages/task/finish
pages/report/index
pages/report/detail
pages/report/form
pages/workbench/index
pages/profile/index
```

## 5. app.json 配置

推荐 `app.json`：

```json
{
  "pages": [
    "pages/login/index",
    "pages/work-order/index/index",
    "pages/work-order/detail/index",
    "pages/task/index/index",
    "pages/task/detail/index",
    "pages/task/finish/index",
    "pages/report/index/index",
    "pages/report/detail/index",
    "pages/report/form/index",
    "pages/workbench/index/index",
    "pages/profile/index/index"
  ],
  "window": {
    "navigationBarTitleText": "羽毛球MES",
    "navigationBarBackgroundColor": "#18aee8",
    "navigationBarTextStyle": "white",
    "backgroundColor": "#f5f5f5"
  },
  "tabBar": {
    "color": "#7a7a7a",
    "selectedColor": "#18aee8",
    "backgroundColor": "#ffffff",
    "borderStyle": "black",
    "list": [
      {
        "pagePath": "pages/work-order/index/index",
        "text": "工单",
        "iconPath": "assets/icons/work-order.png",
        "selectedIconPath": "assets/icons/work-order-active.png"
      },
      {
        "pagePath": "pages/task/index/index",
        "text": "任务",
        "iconPath": "assets/icons/task.png",
        "selectedIconPath": "assets/icons/task-active.png"
      },
      {
        "pagePath": "pages/workbench/index/index",
        "text": "iMES",
        "iconPath": "assets/icons/workbench.png",
        "selectedIconPath": "assets/icons/workbench-active.png"
      },
      {
        "pagePath": "pages/report/index/index",
        "text": "报工",
        "iconPath": "assets/icons/report.png",
        "selectedIconPath": "assets/icons/report-active.png"
      },
      {
        "pagePath": "pages/profile/index/index",
        "text": "我的",
        "iconPath": "assets/icons/profile.png",
        "selectedIconPath": "assets/icons/profile-active.png"
      }
    ]
  },
  "permission": {
    "scope.userLocation": {
      "desc": "用于记录现场操作位置，可第一版不启用"
    }
  },
  "style": "v2",
  "sitemapLocation": "sitemap.json"
}
```

图标可以先使用简单线性图标，尺寸建议：

```text
81px * 81px
```

如果没有图标资源，开发阶段可以临时使用微信开发者工具自动生成的默认图标，但最终展示应补齐。

## 6. 视觉风格规范

参考图片的主要视觉特征：

1. 顶部导航栏：亮蓝色，接近 `#18aee8`。
2. 页面背景：浅灰色，建议 `#f5f5f5`。
3. 内容卡片：白色，圆角 `8rpx` 到 `12rpx`。
4. 卡片间距：左右 `24rpx`，上下 `20rpx`。
5. 主要文字：黑色加粗，列表编号突出。
6. 次要字段：灰色，左右两列展示，左侧字段名，右侧字段值。
7. 状态标签：放卡片右上角，用颜色表达状态。
8. 筛选栏：固定在导航栏下方，三等分横向排列。
9. 底部 Tab：五栏，选中项蓝色。
10. 表单按钮：全宽蓝色按钮，文字白色，必要时带勾选图标。
11. 浮动按钮：圆形蓝色，固定右下角，用于扫码或新增。

推荐颜色：

```text
主色：#18aee8
主色深色：#1298cc
页面背景：#f5f5f5
卡片背景：#ffffff
主文字：#111111
普通文字：#666666
弱文字：#999999
分割线：#eeeeee
警告橙：#f6a400
成功绿：#22b573
危险红：#e84c3d
灰态：#999999
```

推荐全局 `app.wxss`：

```css
page {
  min-height: 100%;
  background: #f5f5f5;
  color: #111111;
  font-size: 30rpx;
}

.page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: calc(32rpx + env(safe-area-inset-bottom));
}

.card {
  margin: 20rpx 24rpx;
  padding: 28rpx 30rpx;
  background: #ffffff;
  border-radius: 10rpx;
  box-sizing: border-box;
}

.row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
  min-height: 44rpx;
  line-height: 44rpx;
}

.label {
  flex: 0 0 170rpx;
  color: #777777;
}

.value {
  flex: 1;
  color: #777777;
  text-align: right;
  word-break: break-all;
}

.primary-button {
  height: 88rpx;
  line-height: 88rpx;
  background: #18aee8;
  color: #ffffff;
  border-radius: 8rpx;
  text-align: center;
  font-size: 34rpx;
}
```

## 7. 公共请求封装

### 7.1 环境配置

`config/env.js`：

```js
const env = {
  dev: {
    baseUrl: 'http://localhost:8088'
  },
  lan: {
    baseUrl: 'http://192.168.1.10:8088'
  },
  prod: {
    baseUrl: 'https://your-domain.com'
  }
}

module.exports = env.dev
```

真机调试时，把导出改成 `env.lan`。

### 7.2 token 工具

`utils/auth.js`：

```js
const TOKEN_KEY = 'MES_TOKEN'
const USER_KEY = 'MES_USER'

function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || ''
}

function setToken(token) {
  wx.setStorageSync(TOKEN_KEY, token)
}

function removeToken() {
  wx.removeStorageSync(TOKEN_KEY)
}

function getUser() {
  return wx.getStorageSync(USER_KEY) || null
}

function setUser(user) {
  wx.setStorageSync(USER_KEY, user)
}

function clearAuth() {
  removeToken()
  wx.removeStorageSync(USER_KEY)
}

module.exports = {
  getToken,
  setToken,
  removeToken,
  getUser,
  setUser,
  clearAuth
}
```

### 7.3 request 封装

`utils/request.js`：

```js
const env = require('../config/env')
const auth = require('./auth')

function request(options) {
  const token = auth.getToken()
  const header = Object.assign({
    'Content-Type': 'application/json'
  }, options.header || {})

  if (token) {
    header.Authorization = `Bearer ${token}`
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: `${env.baseUrl}${options.url}`,
      method: options.method || 'GET',
      data: options.data || {},
      header,
      success(res) {
        if (res.statusCode === 401) {
          auth.clearAuth()
          wx.showToast({ title: '登录已失效', icon: 'none' })
          wx.reLaunch({ url: '/pages/login/index' })
          reject(res)
          return
        }

        const body = res.data || {}
        if (body.code !== 200) {
          wx.showToast({
            title: body.msg || '请求失败',
            icon: 'none'
          })
          reject(body)
          return
        }

        resolve(body.data !== undefined ? body.data : body)
      },
      fail(err) {
        wx.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = request
```

## 8. API 模块设计

### 8.1 登录 API

`api/auth.js`：

```js
const request = require('../utils/request')

function login(data) {
  return request({
    url: '/api/auth/login',
    method: 'POST',
    data
  })
}

function getInfo() {
  return request({
    url: '/api/auth/info'
  })
}

function logout() {
  return request({
    url: '/api/auth/logout',
    method: 'POST'
  })
}

module.exports = {
  login,
  getInfo,
  logout
}
```

注意：当前后端登录接口的 `token` 在返回体顶层，不在 `data` 内。登录页可以直接使用 `wx.request`，或在 `request` 封装中为登录接口特殊处理。推荐登录页直接调用 `wx.request`，成功后保存 `res.data.token` 和 `res.data.data`。

### 8.2 工单 API

`api/work-order.js`：

```js
const request = require('../utils/request')

function page(params) {
  return request({
    url: '/api/mfg/work-order/page',
    data: params
  })
}

function detail(workOrderId) {
  return request({
    url: `/api/mfg/work-order/${workOrderId}`
  })
}

module.exports = {
  page,
  detail
}
```

### 8.3 任务 API

`api/task.js`：

```js
const request = require('../utils/request')

function page(params) {
  return request({
    url: '/api/mfg/task/page',
    data: params
  })
}

function detail(taskId) {
  return request({
    url: `/api/mfg/task/${taskId}`
  })
}

function start(taskId, data) {
  return request({
    url: `/api/mfg/task/${taskId}/start`,
    method: 'PUT',
    data
  })
}

function finish(taskId, data) {
  return request({
    url: `/api/mfg/task/${taskId}/finish`,
    method: 'PUT',
    data
  })
}

module.exports = {
  page,
  detail,
  start,
  finish
}
```

### 8.4 报工 API

`api/work-report.js`：

```js
const request = require('../utils/request')

function page(params) {
  return request({
    url: '/api/mfg/work-report/page',
    data: params
  })
}

function detail(reportId) {
  return request({
    url: `/api/mfg/work-report/${reportId}`
  })
}

function submit(data) {
  return request({
    url: '/api/mfg/work-report/submit',
    method: 'POST',
    data
  })
}

function save(data) {
  return request({
    url: '/api/mfg/work-report',
    method: 'POST',
    data
  })
}

module.exports = {
  page,
  detail,
  submit,
  save
}
```

### 8.5 工作台扩展 API

`api/dashboard.js`：

```js
const request = require('../utils/request')

function summary(params) {
  return request({ url: '/api/dashboard/summary', data: params })
}

function productionTrend(params) {
  return request({ url: '/api/dashboard/production-trend', data: params })
}

function qualityStatistics(params) {
  return request({ url: '/api/dashboard/quality-statistics', data: params })
}

function equipmentStatus() {
  return request({ url: '/api/dashboard/equipment-status' })
}

module.exports = {
  summary,
  productionTrend,
  qualityStatistics,
  equipmentStatus
}
```

工作台第一版可以只调用 `summary`，其它接口作为后续图表扩展。

## 9. 状态字典

`utils/status.js`：

```js
const WORK_ORDER_STATUS = {
  DRAFT: { text: '新建', color: 'gray' },
  APPROVED: { text: '已审核', color: 'blue' },
  RELEASED: { text: '已下达', color: 'blue' },
  WAIT_START: { text: '未开始', color: 'orange' },
  RUNNING: { text: '进行中', color: 'blue' },
  FINISHED: { text: '已完成', color: 'green' },
  CLOSED: { text: '已关闭', color: 'gray' },
  CANCELLED: { text: '已取消', color: 'gray' },
  REVOKED: { text: '已撤回', color: 'gray' }
}

const TASK_STATUS = {
  WAIT_START: { text: '未开始', color: 'orange' },
  RUNNING: { text: '进行中', color: 'blue' },
  PAUSED: { text: '暂停', color: 'orange' },
  FINISHED: { text: '已完成', color: 'green' },
  CLOSED: { text: '已关闭', color: 'gray' }
}

const AUDIT_STATUS = {
  DRAFT: { text: '草稿', color: 'gray' },
  SUBMITTED: { text: '待审核', color: 'orange' },
  APPROVED: { text: '已通过', color: 'green' },
  REJECTED: { text: '已驳回', color: 'red' }
}

function getStatus(map, value) {
  return map[value] || { text: value || '-', color: 'gray' }
}

module.exports = {
  WORK_ORDER_STATUS,
  TASK_STATUS,
  AUDIT_STATUS,
  getStatus
}
```

如果后端返回的状态不是以上值，前端先按原值显示，不要提交中文状态给后端。

## 10. 页面导航结构

底部五栏：

| Tab | 页面 | 功能 |
|---|---|---|
| 工单 | `/pages/work-order/index/index` | 生产工单列表、筛选、详情入口 |
| 任务 | `/pages/task/index/index` | 生产任务列表、开始任务、完成任务、报工入口 |
| iMES | `/pages/workbench/index/index` | 移动工作台，聚合看板、质检、仓储、设备、扫码 |
| 报工 | `/pages/report/index/index` | 报工记录列表、报工详情 |
| 我的 | `/pages/profile/index/index` | 当前用户、角色权限、退出登录 |

非 Tab 页面使用 `wx.navigateTo`：

```text
工单详情：/pages/work-order/detail/index?workOrderId=xxx
任务详情：/pages/task/detail/index?taskId=xxx
完成任务：/pages/task/finish/index?taskId=xxx
报工表单：/pages/report/form/index?taskId=xxx
报工详情：/pages/report/detail/index?reportId=xxx
```

## 11. 登录页设计

### 11.1 页面目标

用户输入账号密码，登录成功后保存 token 和用户信息，跳转工单页。

### 11.2 页面结构

1. 顶部蓝色区域，展示系统名称：`羽毛球 MES`。
2. 白色登录卡片。
3. 用户名输入框。
4. 密码输入框。
5. 登录按钮。
6. 可选：记住账号开关。

### 11.3 字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | input | 是 | 后端账号 |
| password | password input | 是 | 后端密码 |

### 11.4 交互

1. 点击登录前校验账号、密码不能为空。
2. 调用 `POST /api/auth/login`。
3. 成功后保存：

```text
MES_TOKEN = res.data.token
MES_USER = res.data.data
```

4. 跳转：

```js
wx.switchTab({ url: '/pages/work-order/index/index' })
```

5. 失败时显示后端 `msg`。

### 11.5 登录页注意点

当前公共 `request` 封装默认只返回 `data`，但登录接口 token 在顶层。因此登录页建议单独写请求：

```js
wx.request({
  url: `${baseUrl}/api/auth/login`,
  method: 'POST',
  data: {
    username: this.data.username,
    password: this.data.password
  },
  success: (res) => {
    const body = res.data || {}
    if (body.code === 200 && body.token) {
      auth.setToken(body.token)
      auth.setUser(body.data)
      wx.switchTab({ url: '/pages/work-order/index/index' })
    } else {
      wx.showToast({ title: body.msg || '登录失败', icon: 'none' })
    }
  }
})
```

## 12. 工单列表页

### 12.1 对应接口

```http
GET /api/mfg/work-order/page
```

参数：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| current | number | 否 | 当前页，默认 1 |
| size | number | 否 | 每页数量，默认 10 |
| keyword | string | 否 | 工单编号关键字 |
| status | string | 否 | 工单状态 |
| planId | number | 否 | 生产计划 ID |
| productId | number | 否 | 产品 ID |

### 12.2 页面布局

参照截图 `1.jpg`、`2.jpg`：

1. 导航标题：`工单`。
2. 顶部筛选栏三列：
   - 状态
   - 排序
   - 选择产品
3. 下方工单卡片列表。
4. 右下角悬浮扫码按钮。
5. 底部 TabBar。

### 12.3 卡片字段

后端实体：`MfgWorkOrder`

| 展示字段 | 后端字段 | 展示方式 |
|---|---|---|
| 卡片标题 | workOrderNo | 黑色加粗 |
| 状态 | status | 右上角状态标签 |
| 工单编号 | workOrderNo | 右侧灰色 |
| 产品信息 | productId | 第一版显示产品 ID；后续关联产品编码/名称 |
| BOM | bomId | 可选显示 |
| 工艺路线 | routeId | 可选显示 |
| 计划数 | plannedQuantity | 数字 |
| 实际数 | completedQuantity | 数字 |
| 良品数 | 暂无直接字段 | 第一版可不显示或显示 `-` |
| 不良品数 | 暂无直接字段 | 第一版可不显示或从任务汇总 |
| 备注 | remark | 详情页显示 |

### 12.4 筛选状态

筛选菜单显示中文，提交给后端英文编码：

| 显示 | 提交值 |
|---|---|
| 全部 | 空 |
| 新建 | DRAFT |
| 已审核 | APPROVED |
| 已下达 | RELEASED |
| 进行中 | RUNNING |
| 已完成 | FINISHED |
| 已关闭 | CLOSED |

如果截图风格需要“未开始”，可以映射为：

```text
WAIT_START 或 RELEASED
```

实际以后端数据为准。

### 12.5 排序

后端工单分页接口目前没有排序参数。第一版排序在前端完成：

| 显示 | 前端处理 |
|---|---|
| 默认排序 | 保持后端返回顺序 |
| 工单编号倒序 | records 按 workOrderNo 降序 |
| 工单编号升序 | records 按 workOrderNo 升序 |
| 创建时间倒序 | records 按 createdTime 降序 |

### 12.6 操作

1. 点击卡片：进入工单详情。
2. 点击右下角更多：弹出操作菜单：
   - 查看详情
   - 查看任务
   - 发起报工
3. 点击扫码按钮：调用 `wx.scanCode`，扫码后跳转追溯结果页。

## 13. 工单详情页

### 13.1 对应接口

```http
GET /api/mfg/work-order/{workOrderId}
GET /api/mfg/task/page?workOrderId={workOrderId}
GET /api/mfg/work-order-material/work-order/{workOrderId}
```

其中第三个接口用于工单用料，后端已提供 `MfgWorkOrderMaterialController`，如果第一版不做物料详情，可以先不调用。

### 13.2 页面布局

参照截图 `10.jpg`：

1. 顶部导航标题：`工单详情`。
2. 第一张白色卡片显示工单基本信息。
3. 中间横条标题：`生产进度`。
4. 下方任务/工序卡片列表。

### 13.3 工单信息卡

展示：

| 展示 | 字段 |
|---|---|
| 工单号 | workOrderNo |
| 状态 | status |
| 产品 | productId |
| BOM | bomId |
| 工艺路线 | routeId |
| 计划数 | plannedQuantity |
| 实际数 | completedQuantity |
| 备注 | remark |

### 13.4 生产进度

任务列表接口：

```http
GET /api/mfg/task/page?current=1&size=50&workOrderId=xxx
```

每张任务卡：

| 展示 | 字段 |
|---|---|
| 工序名称 | operationId，第一版显示 ID；后续通过工序接口映射 |
| 计划数 | plannedQuantity |
| 良品数 | completedQuantity - defectQuantity |
| 不良品数 | defectQuantity |
| 按钮 | 详情、报工 |

### 13.5 操作

1. `详情`：跳转任务详情。
2. `报工`：跳转报工表单，并携带 `taskId`。

## 14. 任务列表页

### 14.1 对应接口

```http
GET /api/mfg/task/page
```

参数：

| 参数 | 类型 | 说明 |
|---|---|---|
| current | number | 当前页 |
| size | number | 每页数量 |
| keyword | string | 任务编号关键字 |
| status | string | 任务状态 |
| dispatchId | number | 派工单 ID |
| workOrderId | number | 工单 ID |
| operationId | number | 工序 ID |
| stationId | number | 工位 ID |

### 14.2 页面布局

参考工单列表：

1. 导航标题：`任务`。
2. 筛选栏：
   - 状态
   - 排序
   - 选择工序
3. 任务卡片列表。

### 14.3 卡片字段

后端实体：`MfgTask`

| 展示字段 | 后端字段 |
|---|---|
| 标题 | taskNo |
| 状态 | status |
| 任务编号 | taskNo |
| 工单 | workOrderId |
| 派工单 | dispatchId |
| 工序 | operationId |
| 工位 | stationId |
| 计划数 | plannedQuantity |
| 已完成数 | completedQuantity |
| 不良数 | defectQuantity |

### 14.4 状态筛选

| 显示 | 提交值 |
|---|---|
| 全部 | 空 |
| 未开始 | WAIT_START |
| 进行中 | RUNNING |
| 暂停 | PAUSED |
| 已完成 | FINISHED |
| 已关闭 | CLOSED |

### 14.5 操作

卡片点击进入任务详情。更多操作：

| 操作 | 条件 | 行为 |
|---|---|---|
| 开始任务 | `status === WAIT_START` | 调用开始任务接口 |
| 报工 | `status === RUNNING` 或 `WAIT_START` | 跳转报工表单 |
| 完成任务 | `status === RUNNING` | 跳转完成任务页 |
| 查看详情 | 全部 | 跳转详情 |

## 15. 任务详情页

### 15.1 对应接口

```http
GET /api/mfg/task/{taskId}
GET /api/mfg/work-report/page?taskId={taskId}
```

### 15.2 页面布局

1. 顶部导航标题：`任务详情`。
2. 任务基本信息卡。
3. 生产进度区。
4. 报工记录列表。
5. 底部操作按钮：
   - 开始
   - 报工
   - 完成

### 15.3 基本信息

| 展示 | 字段 |
|---|---|
| 任务编号 | taskNo |
| 状态 | status |
| 工单 | workOrderId |
| 派工单 | dispatchId |
| 工序 | operationId |
| 工位 | stationId |
| 计划数 | plannedQuantity |
| 完成数 | completedQuantity |
| 不良数 | defectQuantity |
| 备注 | remark |

### 15.4 开始任务

接口：

```http
PUT /api/mfg/task/{taskId}/start
Content-Type: application/json

{
  "stationId": 1
}
```

`stationId` 可以从当前任务字段带出；如果为空，第一版可以传空对象：

```json
{}
```

成功后刷新任务详情。

### 15.5 完成任务

跳转：

```text
/pages/task/finish/index?taskId=xxx
```

完成页字段：

| 字段 | 类型 | 必填 |
|---|---|---|
| completedQuantity | number input | 是 |
| defectQuantity | number input | 是 |
| remark | textarea | 否 |

提交接口：

```http
PUT /api/mfg/task/{taskId}/finish
Content-Type: application/json

{
  "completedQuantity": 10,
  "defectQuantity": 1,
  "remark": "现场完成"
}
```

## 16. 报工列表页

### 16.1 对应接口

```http
GET /api/mfg/work-report/page
```

参数：

| 参数 | 类型 | 说明 |
|---|---|---|
| current | number | 当前页 |
| size | number | 每页数量 |
| keyword | string | 报工编号关键字 |
| taskId | number | 任务 ID |
| operatorId | number | 操作工 ID |
| stationId | number | 工位 ID |
| auditStatus | string | 审核状态 |

### 16.2 页面布局

参考截图 `28.jpg`：

1. 导航标题：`报工`。
2. 筛选栏：
   - 审核状态
   - 排序
   - 工位/人员
3. 报工卡片列表。
4. 右下角悬浮新增按钮。

### 16.3 卡片字段

后端实体：`MfgWorkReport`

| 展示字段 | 后端字段 |
|---|---|
| 标题 | reportNo 或 reportTime |
| 报工编号 | reportNo |
| 任务 | taskId |
| 报工人 | operatorId |
| 工位 | stationId |
| 良品数 | qualifiedQuantity |
| 不良数 | defectQuantity |
| 实际工时 | actualWorkHours |
| 报工时间 | reportTime |
| 审核状态 | auditStatus |
| 备注 | remark |

### 16.4 操作

1. 点击卡片：进入报工详情。
2. 点击新增：如果没有 `taskId`，先进入任务选择页或提示从任务页发起报工。第一版推荐从任务详情发起报工，报工 Tab 的新增按钮跳转任务列表。

## 17. 报工表单页

### 17.1 对应接口

推荐使用提交接口：

```http
POST /api/mfg/work-report/submit
```

请求体支持主表字段和 `materials` 用料明细。

### 17.2 页面入口

从任务详情或工单详情的任务卡进入：

```text
/pages/report/form/index?taskId=xxx
```

进入后先调用：

```http
GET /api/mfg/task/{taskId}
```

用于展示任务信息和默认工位。

### 17.3 表单字段

| 字段 | 后端字段 | 类型 | 必填 | 默认值 |
|---|---|---|---|---|
| 任务 ID | taskId | hidden | 是 | 页面参数 |
| 报工人 | operatorId | hidden/select | 是 | 当前用户对应员工 ID，若没有则临时使用用户 ID |
| 工位 | stationId | hidden/select | 否 | 任务 stationId |
| 良品数 | qualifiedQuantity | number input | 是 | 0 |
| 不良数 | defectQuantity | number input | 是 | 0 |
| 实际工时 | actualWorkHours | number input | 否 | 空 |
| 报工时间 | reportTime | datetime | 是 | 当前时间 |
| 备注 | remark | textarea | 否 | 空 |

### 17.4 用料明细

第一版可以先不填写 `materials`，请求体传空数组：

```json
"materials": []
```

第二版再增加用料行：

| 字段 | 后端字段 |
|---|---|
| 物料 | materialId |
| 批次 | batchId |
| 使用数量 | usedQuantity |
| 报废数量 | scrapQuantity |
| 备注 | remark |

### 17.5 提交请求示例

```json
{
  "taskId": 1001,
  "operatorId": 1,
  "stationId": 2,
  "qualifiedQuantity": 10,
  "defectQuantity": 1,
  "actualWorkHours": 2.5,
  "reportTime": "2026-07-12T16:30:00",
  "remark": "正常报工",
  "materials": []
}
```

### 17.6 校验规则

1. 良品数不能为空且不能小于 0。
2. 不良数不能为空且不能小于 0。
3. 良品数和不良数不能同时为 0。
4. 实际工时如果填写，必须大于 0。
5. 报工数量建议不超过任务剩余数量：

```text
剩余数量 = plannedQuantity - completedQuantity
```

如果后端没有限制，前端先提醒但不强拦。

## 18. 报工详情页

### 18.1 对应接口

```http
GET /api/mfg/work-report/{reportId}
```

如需展示用料明细：

```http
GET /api/mfg/report-material/report/{reportId}
```

### 18.2 页面布局

参考截图 `28.jpg`：

1. 顶部标题：`报工详情`。
2. 报工信息卡。
3. 生产进度标题条。
4. 第一版下方可留空或展示关联任务进度。

### 18.3 展示字段

| 展示 | 字段 |
|---|---|
| 报工编号 | reportNo |
| 任务 | taskId |
| 报工人 | operatorId |
| 工位 | stationId |
| 报工时间 | reportTime |
| 报工数量 | qualifiedQuantity + defectQuantity |
| 良品数 | qualifiedQuantity |
| 不良数 | defectQuantity |
| 实际工时 | actualWorkHours |
| 审核状态 | auditStatus |
| 备注 | remark |

## 19. iMES 工作台页

### 19.1 页面目标

工作台不是空页面，而是移动端业务入口聚合。第一版以“看数据 + 进模块”为主。

### 19.2 页面布局

1. 顶部标题：`iMES`。
2. 今日统计卡片：
   - 工单数
   - 任务数
   - 待报工
   - 异常数
3. 常用功能宫格：
   - 生产看板
   - 质检单
   - 不合格品
   - 库存查询
   - 入库单
   - 出库单
   - 维修单
   - 扫码追溯
4. 可选：AI 助手入口。

### 19.3 对应接口

看板：

```http
GET /api/dashboard/summary
GET /api/dashboard/production-trend
GET /api/dashboard/quality-statistics
GET /api/dashboard/equipment-status
```

质检：

```http
GET /api/qms/inspection-order/page
GET /api/qms/inspection-order/{inspectionId}
PUT /api/qms/inspection-order/{inspectionId}/submit
```

不合格品：

```http
GET /api/qms/nonconforming-order/page
GET /api/qms/nonconforming-order/{ncrId}
PUT /api/qms/nonconforming-order/{ncrId}/approve
```

库存：

```http
GET /api/wms/inventory/page
GET /api/wms/inventory/{inventoryId}
```

出入库：

```http
GET /api/wms/inbound-order/page
GET /api/wms/inbound-order/{inboundId}
PUT /api/wms/inbound-order/{inboundId}/confirm

GET /api/wms/outbound-order/page
GET /api/wms/outbound-order/{outboundId}
PUT /api/wms/outbound-order/{outboundId}/confirm
```

设备维修：

```http
GET /api/eam/repair-order/page
GET /api/eam/repair-order/{repairId}
PUT /api/eam/repair-order/{repairId}/assign
PUT /api/eam/repair-order/{repairId}/close
```

扫码追溯：

```http
GET /api/trace/barcode/scan/{barcodeValue}
```

AI：

```http
GET /api/ai/status
POST /api/ai/chat
POST /api/ai/dashboard-insight
POST /api/ai/module-suggestion
```

AI 后端默认可能关闭，第一版入口可以隐藏或显示“暂未启用”。

## 20. 我的页面

### 20.1 对应接口

```http
GET /api/auth/info
POST /api/auth/logout
```

### 20.2 页面结构

1. 顶部用户卡片：
   - 默认头像
   - 用户名
   - 用户 ID
2. 信息列表：
   - 角色
   - 权限数量
   - 登录状态
   - 系统版本
3. 操作列表：
   - 刷新用户信息
   - 清理缓存
   - 退出登录

### 20.3 退出登录流程

1. 弹出确认框。
2. 调用 `POST /api/auth/logout`。
3. 无论接口是否成功，都清理本地 token。
4. 跳转登录页：

```js
wx.reLaunch({ url: '/pages/login/index' })
```

## 21. 公共组件

### 21.1 status-tag

用途：统一显示状态颜色。

属性：

| 属性 | 类型 | 说明 |
|---|---|---|
| text | string | 状态文字 |
| color | string | blue/orange/green/red/gray |

样式：

```css
.tag {
  display: inline-flex;
  align-items: center;
  height: 40rpx;
  padding: 0 14rpx;
  border-radius: 20rpx;
  font-size: 26rpx;
  font-weight: 600;
}

.blue { color: #18aee8; }
.orange { color: #f6a400; }
.green { color: #22b573; }
.red { color: #e84c3d; }
.gray { color: #999999; }
```

### 21.2 data-card

用途：工单、任务、报工、库存、维修单通用信息卡。

建议属性：

| 属性 | 类型 | 说明 |
|---|---|---|
| title | string | 卡片标题 |
| statusText | string | 状态文字 |
| statusColor | string | 状态颜色 |
| rows | array | `{ label, value }` 数组 |
| showMore | boolean | 是否显示右下角更多 |

### 21.3 filter-bar

用途：三段筛选栏。

建议属性：

| 属性 | 类型 |
|---|---|
| items | array |

每项：

```js
{
  key: 'status',
  label: '状态',
  activeLabel: '未开始'
}
```

点击后用 `wx.showActionSheet` 或自定义下拉层展示选项。参考图中是下拉面板盖住列表，背景变暗。

### 21.4 empty-state

用途：列表为空时显示。

文案：

```text
暂无数据
```

按钮：

```text
刷新
```

## 22. 数据关联策略

当前后端很多列表只返回 ID，例如 `productId`、`operationId`、`stationId`。第一版为保证可实现性，允许先显示 ID。为了体验更好，建议增加前端缓存映射：

### 22.1 产品映射

接口：

```http
GET /api/mdm/product/page?current=1&size=500
```

缓存结构：

```js
productMap[productId] = `${productCode} | ${productName}`
```

### 22.2 工序映射

接口：

```http
GET /api/process/operation/page?current=1&size=500
```

缓存结构：

```js
operationMap[operationId] = operationName
```

### 22.3 工位映射

接口：

```http
GET /api/factory/station/page?current=1&size=500
```

缓存结构：

```js
stationMap[stationId] = stationName
```

### 22.4 员工映射

接口：

```http
GET /api/org/employee/page?current=1&size=500
```

缓存结构：

```js
employeeMap[employeeId] = employeeName
```

第一版如果时间紧，不做映射也可以上线，但文案要写成：

```text
产品ID：1001
工序ID：2001
```

不要伪造不存在的名称。

## 23. 分页和刷新

所有列表页统一逻辑：

```js
data: {
  list: [],
  current: 1,
  size: 10,
  total: 0,
  pages: 0,
  loading: false,
  finished: false,
  filters: {}
}
```

加载第一页：

```js
this.setData({
  current: 1,
  list: [],
  finished: false
})
this.loadData()
```

下拉刷新：

```js
onPullDownRefresh() {
  this.reload().finally(() => wx.stopPullDownRefresh())
}
```

上拉加载：

```js
onReachBottom() {
  if (!this.data.finished && !this.data.loading) {
    this.setData({ current: this.data.current + 1 })
    this.loadData()
  }
}
```

接口返回后：

```js
const page = await api.page({
  current: this.data.current,
  size: this.data.size,
  ...this.data.filters
})

const records = page.records || []
this.setData({
  list: this.data.current === 1 ? records : this.data.list.concat(records),
  total: page.total || 0,
  pages: page.pages || 0,
  finished: this.data.current >= page.pages
})
```

## 24. 扫码追溯

### 24.1 入口

1. 工单列表右下角扫码按钮。
2. 工作台“扫码追溯”宫格。

### 24.2 流程

```js
wx.scanCode({
  onlyFromCamera: false,
  success: (res) => {
    const value = encodeURIComponent(res.result)
    wx.navigateTo({
      url: `/pages/trace/scan-result/index?barcodeValue=${value}`
    })
  }
})
```

### 24.3 结果页接口

```http
GET /api/trace/barcode/scan/{barcodeValue}
```

展示字段：

| 展示 | 字段 |
|---|---|
| 条码值 | barcodeValue |
| 条码类型 | barcodeType |
| 业务类型 | bizType |
| 业务 ID | bizId |
| 状态 | status |
| 备注 | remark |

## 25. 权限控制

第一版不需要做复杂动态路由，但要根据 `/api/auth/info` 返回的角色或权限控制入口显示。

建议规则：

| 角色 | 默认入口 |
|---|---|
| 车间工人 | 工单、任务、报工、我的 |
| 班组长 | 工单、任务、报工、iMES、我的 |
| 质检员 | 任务、报工、iMES、我的；工作台显示质检 |
| 仓库人员 | iMES、我的；工作台显示库存、出入库 |
| 设备维修人员 | iMES、我的；工作台显示维修 |
| 管理员 | 全部入口 |

如果后端角色名不稳定，第一版可以全部显示入口，点击无权限时由后端返回错误。

## 26. 第一版开发顺序

严格按以下顺序实现，确保每一步都能独立运行：

1. 创建小程序项目，配置 `app.json`、`app.wxss`、TabBar。
2. 编写 `env.js`、`auth.js`、`request.js`。
3. 完成登录页，能拿到 token。
4. 完成工单列表页，能展示分页数据。
5. 完成工单详情页，能展示关联任务。
6. 完成任务列表页。
7. 完成任务详情页和开始任务操作。
8. 完成报工表单页，能提交 `POST /api/mfg/work-report/submit`。
9. 完成报工列表和详情。
10. 完成我的页面和退出登录。
11. 完成工作台入口和看板汇总。
12. 补充扫码追溯。
13. 做真机联调和异常处理。

不要先做大量二期模块，否则主生产闭环容易联调不完。

## 27. 后端接口清单

### 27.1 认证

| 功能 | 方法 | 接口 |
|---|---|---|
| 登录 | POST | `/api/auth/login` |
| 当前用户 | GET | `/api/auth/info` |
| 退出 | POST | `/api/auth/logout` |

### 27.2 工单

| 功能 | 方法 | 接口 |
|---|---|---|
| 分页 | GET | `/api/mfg/work-order/page` |
| 详情 | GET | `/api/mfg/work-order/{workOrderId}` |

### 27.3 任务

| 功能 | 方法 | 接口 |
|---|---|---|
| 分页 | GET | `/api/mfg/task/page` |
| 详情 | GET | `/api/mfg/task/{taskId}` |
| 开始 | PUT | `/api/mfg/task/{taskId}/start` |
| 完成 | PUT | `/api/mfg/task/{taskId}/finish` |

### 27.4 报工

| 功能 | 方法 | 接口 |
|---|---|---|
| 分页 | GET | `/api/mfg/work-report/page` |
| 详情 | GET | `/api/mfg/work-report/{reportId}` |
| 新增 | POST | `/api/mfg/work-report` |
| 新增主从 | POST | `/api/mfg/work-report/full` |
| 提交 | POST | `/api/mfg/work-report/submit` |
| 修改 | PUT | `/api/mfg/work-report` |
| 修改主从 | PUT | `/api/mfg/work-report/full` |
| 审核 | PUT | `/api/mfg/work-report/{reportId}/audit` |

### 27.5 看板

| 功能 | 方法 | 接口 |
|---|---|---|
| 汇总 | GET | `/api/dashboard/summary` |
| 生产趋势 | GET | `/api/dashboard/production-trend` |
| 质量统计 | GET | `/api/dashboard/quality-statistics` |
| 设备状态 | GET | `/api/dashboard/equipment-status` |

### 27.6 质检

| 功能 | 方法 | 接口 |
|---|---|---|
| 检验单分页 | GET | `/api/qms/inspection-order/page` |
| 检验单详情 | GET | `/api/qms/inspection-order/{inspectionId}` |
| 新增检验单 | POST | `/api/qms/inspection-order` |
| 新增主从 | POST | `/api/qms/inspection-order/full` |
| 修改主从 | PUT | `/api/qms/inspection-order/full` |
| 提交检验 | PUT | `/api/qms/inspection-order/{inspectionId}/submit` |
| 不合格品分页 | GET | `/api/qms/nonconforming-order/page` |
| 不合格品详情 | GET | `/api/qms/nonconforming-order/{ncrId}` |
| 不合格审批 | PUT | `/api/qms/nonconforming-order/{ncrId}/approve` |

### 27.7 仓储

| 功能 | 方法 | 接口 |
|---|---|---|
| 库存分页 | GET | `/api/wms/inventory/page` |
| 库存详情 | GET | `/api/wms/inventory/{inventoryId}` |
| 入库单分页 | GET | `/api/wms/inbound-order/page` |
| 入库单详情 | GET | `/api/wms/inbound-order/{inboundId}` |
| 入库确认 | PUT | `/api/wms/inbound-order/{inboundId}/confirm` |
| 出库单分页 | GET | `/api/wms/outbound-order/page` |
| 出库单详情 | GET | `/api/wms/outbound-order/{outboundId}` |
| 出库确认 | PUT | `/api/wms/outbound-order/{outboundId}/confirm` |

### 27.8 设备

| 功能 | 方法 | 接口 |
|---|---|---|
| 维修单分页 | GET | `/api/eam/repair-order/page` |
| 维修单详情 | GET | `/api/eam/repair-order/{repairId}` |
| 新增维修单 | POST | `/api/eam/repair-order` |
| 派工 | PUT | `/api/eam/repair-order/{repairId}/assign` |
| 关闭 | PUT | `/api/eam/repair-order/{repairId}/close` |

### 27.9 条码

| 功能 | 方法 | 接口 |
|---|---|---|
| 条码分页 | GET | `/api/trace/barcode/page` |
| 条码详情 | GET | `/api/trace/barcode/{barcodeId}` |
| 生成条码 | POST | `/api/trace/barcode/generate` |
| 扫码查询 | GET | `/api/trace/barcode/scan/{barcodeValue}` |

## 28. 后端需要补强的点

以下不是第一版必须完成，但会明显提升小程序体验：

1. 工单列表直接返回产品编码、产品名称，而不是只返回 `productId`。
2. 任务列表直接返回工单编号、工序名称、工位名称。
3. 报工列表直接返回任务编号、工单编号、工序名称、操作工姓名。
4. 当前登录用户和员工档案建立关联，明确 `operatorId` 应该取用户 ID 还是员工 ID。
5. 增加“我的任务”接口，按当前用户过滤任务。
6. 增加移动端首页汇总接口：

```text
今日待开工任务数
今日进行中任务数
今日待报工任务数
今日待质检数
今日异常数
```

7. 增加工单详情聚合接口，一次返回工单、任务、报工汇总、物料齐套、质检结果。

## 29. 联调验收清单

### 29.1 基础验收

| 项目 | 标准 |
|---|---|
| 登录 | 正确账号可登录，错误账号提示错误 |
| token | 登录后所有接口携带 Authorization |
| 退出 | 退出后清理 token，不能继续访问业务页 |
| 分页 | 上拉能加载下一页 |
| 刷新 | 下拉刷新能重新加载第一页 |
| 空数据 | 无数据时显示空状态 |
| 网络异常 | 后端关闭时显示网络异常 |

### 29.2 生产闭环验收

| 流程 | 标准 |
|---|---|
| 工单列表 | 能看到后端工单 |
| 工单详情 | 能看到工单信息和关联任务 |
| 任务列表 | 能筛选状态 |
| 开始任务 | WAIT_START 任务可以切换到 RUNNING |
| 报工 | RUNNING 任务可以提交良品数和不良数 |
| 报工列表 | 提交后能看到记录 |
| 任务完成 | 能提交完成数、不良数、备注 |

### 29.3 视觉验收

| 项目 | 标准 |
|---|---|
| 顶部导航 | 蓝色，白色标题 |
| 列表卡片 | 白底圆角，字段左右对齐 |
| 状态颜色 | 未开始橙、进行中蓝、完成绿、异常红、关闭灰 |
| TabBar | 五栏：工单、任务、iMES、报工、我的 |
| 表单按钮 | 全宽蓝色按钮 |
| 真机显示 | 文字不溢出，不遮挡按钮和 TabBar |

## 30. 常见问题

### 30.1 微信开发者工具请求不到后端

检查：

1. 后端是否启动在 `8088`。
2. `env.js` 的 `baseUrl` 是否正确。
3. 开发者工具是否勾选“不校验合法域名”。
4. 真机调试是否使用电脑局域网 IP，而不是 `localhost`。
5. 电脑防火墙是否放行 `8088`。

### 30.2 登录成功但业务接口 401

检查：

1. 是否保存了 `token`。
2. Header 是否是：

```text
Authorization: Bearer <token>
```

3. token 前面是否多了空格或少了 `Bearer`。
4. 后端 token 是否过期或已退出。

### 30.3 报工提交失败

检查：

1. `taskId` 是否存在。
2. `operatorId` 是否为空。
3. `qualifiedQuantity` 和 `defectQuantity` 是否是数字。
4. `reportTime` 是否是后端可解析的时间格式，例如：

```text
2026-07-12T16:30:00
```

5. 如果传了 `materials`，每一行是否包含合法 `materialId`、`batchId`、`usedQuantity`。

### 30.4 页面显示一堆 ID

这是因为当前后端列表接口返回的是业务主表字段，没有聚合名称。第一版可以接受；要优化就按“数据关联策略”加载产品、工序、工位、员工映射。

## 31. 推荐交付标准

小程序第一版完成后，应能演示以下路径：

```text
登录
-> 查看工单列表
-> 进入工单详情
-> 查看任务
-> 开始任务
-> 提交报工
-> 查看报工记录
-> 完成任务
-> 工作台查看统计入口
-> 我的页面退出登录
```

只要这条路径稳定，就说明小程序已经和羽毛球 MES 后端形成可用的移动端主流程。
