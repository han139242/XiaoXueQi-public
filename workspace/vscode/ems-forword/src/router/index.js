// router/index.js 负责三件事：声明页面路径、控制页面跳转、执行登录与权限检查。
import { createRouter, createWebHashHistory } from 'vue-router'

// auth 工具负责读取登录会话和当前用户信息。
import { clearAuth, getCurrentUser, hasAuthenticatedSession, isFrameworkPreview, setCurrentUser } from '@/utils/auth'
// resourceRoutes 根据资源配置生成生产、仓储、质量、设备等通用管理页面路由。
import { getDomainResources, resourceRoutes } from '@/config/resources'
import { getCurrentUserInfo } from '@/api/auth'
import { hasPermission, resourcePermission } from '@/utils/permission'

// routes 是整个系统的“路径 -> 页面组件”映射表。
const routes = [
  {
    path: '/login',
    name: 'Login',
    // 动态 import 表示访问该页面时再加载代码，减少首次打开系统的资源体积。
    component: () => import('@/pages/login/index.vue'),
    // public 表示不登录也能访问；title 用于设置浏览器标签标题。
    meta: { public: true, title: '登录' }
  },
  {
    path: '/big-screen/production',
    name: 'ProductionBigScreen',
    component: () => import('@/pages/big-screen/production.vue'),
    meta: { title: '生产运营大屏', permission: 'dashboard:big-screen:view' }
  },
  {
    path: '/',
    // layout 是登录后的整体框架，包含侧边菜单、顶部导航和内部 RouterView。
    component: () => import('@/pages/layout/index.vue'),
    redirect: '/dashboard',
    // children 中的页面都显示在 layout 的内部路由出口中。
    children: [
      // 下面四个是独立实现的固定页面，数量较少，直接写在主路由表中更容易阅读。
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/pages/dashboard/index.vue'),
        meta: { title: '首页', icon: 'dashboard', affix: true, permission: 'dashboard:summary:view' }
      },
      {
        path: 'ai/workbench',
        name: 'AiWorkbench',
        component: () => import('@/pages/ai/workbench.vue'),
        meta: { title: 'AI 工作台', icon: 'bot' }
      },
      {
        path: 'mfg/schedule-workbench',
        name: 'MfgScheduleWorkbench',
        component: () => import('@/pages/mfg/scheduling-workbench.vue'),
        meta: { title: '智能排产工作台', icon: 'route', permission: 'mfg:schedule-scenario:view' }
      },
      {
        path: 'notifications',
        name: 'NotificationCenter',
        component: () => import('@/pages/sys/notification-center.vue'),
        meta: { title: '消息中心', icon: 'bell', permission: 'sys:notification:view' }
      },
      // 其余生产、仓储、质量等通用页面由资源配置统一生成。
      ...resourceRoutes,
      {
        path: 'module/:moduleKey',
        name: 'ModuleHub',
        redirect: (to) => getDomainResources(to.params.moduleKey)
          .find((resource) => hasPermission(resourcePermission(resource.key, 'view')))?.routePath || '/dashboard',
        meta: { title: '模块总览', icon: 'package' }
      }
    ]
  },
  {
    path: '/forbidden',
    name: 'Forbidden',
    component: () => import('@/pages/error/forbidden.vue'),
    meta: { title: '无访问权限', permissionExempt: true }
  },
  {
    // pathMatch 是兜底路由：前面的路径都匹配不到时显示 404 页面。
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/pages/error/not-found.vue'),
    meta: { public: true, title: '页面未找到' }
  }
]

const router = createRouter({
  // Hash 模式的地址形如 http://localhost:5173/#/dashboard，刷新时不依赖服务器配置路由回退。
  history: createWebHashHistory(),
  routes,
  // 每次进入新页面时让页面滚动条回到顶部。
  scrollBehavior: () => ({ top: 0 })
})

// beforeEach 是全局前置路由守卫，每一次页面跳转前都会执行。
router.beforeEach(async (to) => {
  // 登录页、404 等公共页面不要求登录。
  if (to.meta.public) {
    // 已登录用户再次访问登录页时，直接送回原目标页或首页。
    if (to.name === 'Login' && hasAuthenticatedSession()) {
      return to.query.redirect || '/dashboard'
    }
    return true
  }

  // 非公共页面没有有效登录会话时，跳转登录页，并记录原目标地址。
  if (!hasAuthenticatedSession()) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  if (!isFrameworkPreview()) {
    const currentUser = getCurrentUser()
    // 本地只有 Token、还没有角色和权限时，向后端 /api/auth/info 补齐当前用户信息。
    if (!Array.isArray(currentUser.permissions) || !Array.isArray(currentUser.roles)) {
      try {
        const result = await getCurrentUserInfo()
        setCurrentUser(result.data || {})
      } catch {
        // Token 失效或接口失败时清除旧会话，重新登录。
        clearAuth()
        return { path: '/login', query: { redirect: to.fullPath } }
      }
    }
  }

  const requiredPermission = to.meta.permission
    || (to.name === 'ModuleHub' ? `module:${to.params.moduleKey}:view` : '')
  // 页面声明了 permission，而当前账号不具备该权限时，进入 403 页面。
  if (!to.meta.permissionExempt && requiredPermission && !hasPermission(requiredPermission)) {
    return { path: '/forbidden', query: { from: to.fullPath } }
  }

  return true
})

// afterEach 是后置守卫：页面跳转完成后更新浏览器标签标题。
router.afterEach((to) => {
  const title = to.meta.title ? `${to.meta.title} - 羽球智造 MES` : '羽球智造 MES'
  document.title = title
})

// 默认导出路由实例，main.js通过app.use(router)把它安装到整个Vue应用。
export default router
