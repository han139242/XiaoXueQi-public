
// 项目的入口文件：js

import { createApp } from 'vue'
import router from './router'
import ElementPlus from 'element-plus'


import 'element-plus/dist/index.css'

// import './style.css'
import App from './App.vue'
// 创建一个vue对象，创建一个个vue对象的时候需要一个页面的模版
const app = createApp(App)

app.use(router)
app.use(ElementPlus)
app.mount("#app")
