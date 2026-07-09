//路由
import { createRouter, createWebHashHistory } from 'vue-router'
import Login from '../pages/login/index.vue'
import Regist from '../pages/regist/index.vue'

// 路由规则
const routes = [
  {
    path: '/',
    redirect: '/login' // 重定向到登录页,自动跳转到登录页
  },  
  {
    path: '/login',
    meta: {
      title: '登录'
    },
    component: Login
  },
  {
    path: '/regist',
    meta: {
      title: '注册'
    },
    component: Regist
  },
]

// 创建路由实例
const router = createRouter({
  history: createWebHashHistory(), // hash模式（兼容低版本浏览器）
  routes
})

// 路由守卫：修改页面标题
router.beforeEach((to, from, next) => {
  console.log(to.path,'========')
  if (to.meta.title) {
    const appTitle = import.meta.env.VITE_APP_TITLE || '羽毛球 MES'
    document.title = `${to.meta.title} - ${appTitle}`
  }
  next()
})

export default router
