export default [
  {
    path: 'dashboard',
    name: 'Dashboard',
    component: () => import('@/pages/dashboard/index.vue'),
    meta: { title: '运营工作台', icon: 'dashboard', affix: true }
  }
]
