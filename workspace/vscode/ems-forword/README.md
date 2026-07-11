# 羽球智造 MES PC 管理端

PC 前端位于 `workspace/vscode/ems-forword`，使用 Vue 3、Vite、Vue Router、Element Plus、Axios 和 ECharts。

## 本地运行

```powershell
npm install
npm run dev
```

默认地址为 `http://localhost:5173`。开发服务器将 `/api` 代理到 `http://localhost:8088`；需要修改后端地址时，在本机环境中设置 `VITE_API_PROXY_TARGET`，不要把本机地址或密钥提交到仓库。

生产构建：

```powershell
npm run build
```

## 当前能力

- 正式登录页和 `POST /api/auth/login` 对接。
- Axios 请求封装、Bearer Token、业务错误码和登录失效处理。
- 路由守卫、懒加载路由分片、404 页面。
- 可折叠侧边栏、顶部栏、模块搜索、通知空状态、标签页和响应式抽屉。
- 提供经典蓝、曜石黑、制造绿、工业橙、企业红 5 套完整界面皮肤，主题联动顶栏、侧栏、标签页、内容背景和 Dashboard 图表，并通过右侧主题抽屉切换和本地持久化。
- 首页接入四个 `/api/dashboard` 接口；开发环境“查看基础框架”预览使用 `src/config/dashboard-preview.js` 的内置演示数据，不代表真实数据库数据。
- 独立的 `1920x1080` 生产运营大屏，支持等比例缩放、全屏、定时刷新和错误重试。
- 全局 AI 助手抽屉，对接 `/api/ai/status` 与 `/api/ai/chat`；模型未配置时明确展示不可用状态，不伪造回答。
- 统一设计变量、页面容器、面板、按钮、状态色和图表组件。
- 配置式业务工作区覆盖全部 `78` 个后端资源 Controller，包含系统、组织、主数据、工艺、工厂、生产、仓储、质量、设备和追溯模块。
- 资源页支持分页筛选、CRUD、导出、字段显示设置、主从明细、只读资源、业务状态动作、附件上传下载和批次追溯。
- 生产任务、质检、不合格品、入库确认和维修工单按后端英文状态编码控制操作，不允许从普通编辑入口绕过状态流转。

开发环境登录页提供“查看基础框架”入口，只用于视觉和交互评审。该入口依赖 `import.meta.env.DEV`，生产构建中不可用；正式验收仍需启动 MySQL 和后端，使用真实账号、JWT 与业务数据跑通主链路。

## AI 后端配置

前端不会直连第三方模型，也不会保存模型密钥。真实 AI 调用由 Spring Boot 后端代理，默认关闭。启动后端前可在本机设置：

```powershell
$env:AI_ENABLED='true'
$env:AI_API_URL='https://api.deepseek.com/chat/completions'
$env:AI_API_KEY='本机密钥'
$env:AI_MODEL='deepseek-chat'
```

未设置密钥或 `AI_ENABLED=false` 时，`GET /api/ai/status` 会返回不可用状态，前端将禁用提问入口并展示实际原因。不要把 `.env`、密钥或本机配置提交到仓库。

## 主要目录

```text
src/api                 接口封装和 Axios 实例
src/components/common   通用基础组件
src/components/layout   后台布局组件
src/components/chart    ECharts 基础组件
src/config/resources    78 个业务资源、字段、关系和状态动作配置
src/config              导航、预览数据和公共配置
src/pages               页面
src/router/modules      分组路由
src/styles              设计变量和全局样式
src/utils               登录态等工具
```

## 设计参考与素材

- [iMES-Factory](https://github.com/zmrid/iMES-Factory) 采用 MIT License。本项目参考其蓝色顶栏、白/黑侧栏主题、border-card 标签页、紧凑列表工作流、彩色快捷菜单和信息密度，并按本项目接口、羽毛球业务和设计变量重新实现。
- 登录页羽毛球成品照片为 Wikimedia Commons 的 [Badminton-Ball](https://commons.wikimedia.org/wiki/File:Badminton-Ball_(13550266595).jpg)，作者 Tim Reckmann，使用 CC BY 2.0 许可。项目内文件为 `src/assets/login-shuttlecock.jpg`。
- 生产运营大屏使用 Pixabay 羽毛球照片 `badminton-6030862`，遵循 Pixabay Content License。项目内文件为 `src/assets/login-badminton.jpg`。
