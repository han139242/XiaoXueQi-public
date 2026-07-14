export default [
  {
    path: 'dashboard',
    name: 'Dashboard',
    component: () => import('@/pages/dashboard/index.vue'),
    meta: { title: '首页', icon: 'dashboard', affix: true, permission: 'dashboard:summary:view' }
  }
]
