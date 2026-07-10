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

## 当前基础框架

- 正式登录页和 `POST /api/auth/login` 对接。
- Axios 请求封装、Bearer Token、业务错误码和登录失效处理。
- 路由守卫、懒加载路由分片、404 页面。
- 可折叠侧边栏、顶部栏、模块搜索、通知空状态、标签页和响应式抽屉。
- 运营工作台接入四个 `/api/dashboard` 接口。
- 独立的 `1920x1080` 生产运营大屏，支持等比例缩放、全屏、定时刷新和错误重试。
- 全局 AI 助手抽屉，对接 `/api/ai/status` 与 `/api/ai/chat`；模型未配置时明确展示不可用状态，不伪造回答。
- 统一设计变量、页面容器、面板、按钮、状态色和图表组件。
- 所有业务域的模块入口，业务页面后续按开发计划逐项接入。

开发环境登录页提供“查看基础框架”入口，只用于视觉评审。该入口依赖 `import.meta.env.DEV`，生产构建中不可用；真实联调必须使用后端账号和 JWT。

## 主要目录

```text
src/api                 接口封装和 Axios 实例
src/components/common   通用基础组件
src/components/layout   后台布局组件
src/components/chart    ECharts 基础组件
src/config              导航和模块配置
src/pages               页面
src/router/modules      分组路由
src/styles              设计变量和全局样式
src/utils               登录态等工具
```

## 视觉素材

登录页羽毛球照片来自 Pixabay：`badminton-6030862`，使用 Pixabay Content License。项目内文件为 `src/assets/login-badminton.jpg`。
