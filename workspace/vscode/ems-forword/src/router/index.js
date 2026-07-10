// 路由： 根据不同的访问路径，显示不同的页面

// 1： 创建一个路由对象，给这个路由对象匹配路由规则（路径和页面的映射）


import { createRouter, createWebHashHistory } from 'vue-router'
import Login from '@/pages/login/index.vue'
import Regist from '@/pages/regist/index.vue'

import Layout from '@/pages/layout/index.vue'

const routes = [
    {
        path: '/',
        redirect: '/layout'
    },
    {
        path: '/login',
        component: Login
    },
    {
        path: '/regist',
        component: Regist
    },
    {
        path: '/layout',
        component: Layout
    }
]


// 创建路由实例对象
const router = createRouter({
    // 路由的模式
    history: createWebHashHistory(),
    routes: routes
})


// 将这个路由对象进行导出
export default router


















// // 路由守卫：修改页面标题
// router.beforeEach((to, from, next) => {
//   console.log(to.path,'========')
//   if (to.meta.title) {
//     document.title = `${to.meta.title} - ${import.meta.env.VITE_APP_TITLE}`
//   }
//   next()
// })
