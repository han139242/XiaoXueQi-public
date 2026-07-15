export default [
  {
    path: 'notifications',
    name: 'NotificationCenter',
    component: () => import('@/pages/sys/notification-center.vue'),
    meta: { title: '消息中心', icon: 'bell', permission: 'sys:notification:view' }
  }
]
