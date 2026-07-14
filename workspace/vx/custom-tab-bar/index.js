const auth = require('../utils/auth')

const TAB_ITEMS = [
  { path: '/pages/work-order/index/index', label: '工单', icon: '/assets/icons/tab-work-order.png', activeIcon: '/assets/icons/tab-work-order-active.png', permission: 'mfg:work-order:view' },
  { path: '/pages/task/index/index', label: '任务', icon: '/assets/icons/tab-task.png', activeIcon: '/assets/icons/tab-task-active.png', permission: 'mfg:task:view' },
  { path: '/pages/workbench/index/index', label: '工作台', icon: '/assets/icons/tab-workbench.png', activeIcon: '/assets/icons/tab-workbench-active.png' },
  { path: '/pages/report/index/index', label: '报工', icon: '/assets/icons/tab-report.png', activeIcon: '/assets/icons/tab-report-active.png', permission: 'mfg:work-report:view' },
  { path: '/pages/profile/index/index', label: '我的', icon: '/assets/icons/tab-profile.png', activeIcon: '/assets/icons/tab-profile-active.png' }
]

Component({
  data: {
    currentPath: '',
    visibleTabs: []
  },

  lifetimes: {
    attached() {
      this.refreshTabs()
    }
  },

  pageLifetimes: {
    show() {
      this.refreshTabs()
    }
  },

  methods: {
    refreshTabs() {
      const pages = getCurrentPages()
      const currentPath = pages.length ? `/${pages[pages.length - 1].route}` : ''
      const visibleTabs = TAB_ITEMS.filter((item) => !item.permission || auth.hasPermission(item.permission))
      this.setData({ currentPath, visibleTabs })
    },

    switchTab(event) {
      const path = event.currentTarget.dataset.path
      if (!path || path === this.data.currentPath) return
      wx.switchTab({ url: path })
    }
  }
})
