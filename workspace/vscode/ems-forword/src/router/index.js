import { createRouter, createWebHashHistory } from 'vue-router'
import aRoutes from './modules/a'
import bRoutes from './modules/b'
import cRoutes from './modules/c'
import { hasAuthenticatedSession } from '@/utils/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/login/index.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/big-screen/production',
    name: 'ProductionBigScreen',
    component: () => import('@/pages/big-screen/production.vue'),
    meta: { title: '生产运营大屏' }
  },
  {
    path: '/',
    component: () => import('@/pages/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      ...aRoutes,
      ...bRoutes,
      ...cRoutes,
      {
        path: 'module/:moduleKey',
        name: 'ModuleHub',
        component: () => import('@/pages/module/index.vue'),
        meta: { title: '模块总览', icon: 'package' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/pages/error/not-found.vue'),
    meta: { public: true, title: '页面未找到' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  if (to.meta.public) {
    if (to.name === 'Login' && hasAuthenticatedSession()) {
      return to.query.redirect || '/dashboard'
    }
    return true
  }

  if (!hasAuthenticatedSession()) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  return true
})

router.afterEach((to) => {
  const title = to.meta.title ? `${to.meta.title} - 羽球智造 MES` : '羽球智造 MES'
  document.title = title
})

export default router
