export default [
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
    meta: { title: '智能排产工作台', icon: 'route' }
  }
]
