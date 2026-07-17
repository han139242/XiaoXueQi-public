// main.js 是整个 Vue 前端的启动入口，index.html 会首先加载这个文件。
import { createApp } from 'vue'
// Element Plus 提供表格、表单、按钮、弹窗等后台管理组件。
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
// style.css 是全局样式；App.vue 是整个项目的根组件。
import './style.css'
import App from './App.vue'
// router 管理 URL 与 Vue 页面之间的对应关系。
import router from './router'
import { initTheme } from './utils/theme'
import liveColumnResize from './directives/live-column-resize'

// 先读取并应用用户保存的主题，避免页面出现主题闪烁。
initTheme()

// 以 App.vue 为根组件创建一个 Vue 应用实例。
const app = createApp(App)

// use() 用于安装全局插件：router 提供路由，ElementPlus 提供 UI 组件。
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.directive('live-column-resize', liveColumnResize)

// 把 Vue 应用挂载到 index.html 中 <div id="app"></div> 的位置。
// CSS 选择器中“#”表示按 id 查找，所以 #app 表示 id="app" 的元素。
app.mount('#app')
