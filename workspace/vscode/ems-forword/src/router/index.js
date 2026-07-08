//路由
import { createRouter, createWebHashHistory } from 'vue-router'

// 路由规则
const routes = [
  {
    path: '/',
    redirect: '/login' // 重定向到登录页,自动跳转到登录页
  },  
  {
    path: '/login',
    component: Login
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
    document.title = `${to.meta.title} - ${import.meta.env.VITE_APP_TITLE}`
  }
  next()
})

export default router