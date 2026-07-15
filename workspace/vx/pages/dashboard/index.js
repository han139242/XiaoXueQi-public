const guard = require('../../utils/guard')
const dashboardApi = require('../../api/dashboard')

const metricDefinitions = [
  ['productionOrderCount', '当前生产订单', 'primary', 'factory', '/pages/production-order/index/index'],
  ['workOrderCount', '当前生产工单', 'data', 'clipboard-list'],
  ['runningTaskCount', '运行中任务', 'success', 'circle-play'],
  ['todayQualifiedQuantity', '今日合格报工', 'primary', 'badge-check'],
  ['todayDefectQuantity', '今日不良报工', 'danger', 'triangle-alert'],
  ['inventoryWarningCount', '补货预警', 'warning', 'warehouse'],
  ['equipmentFaultCount', '故障设备', 'danger', 'wrench']
]

Page({
  data: {
    metrics: [],
    loading: false,
    error: ''
  },

  onShow() {
    if (guard.requireAuth()) this.loadSummary()
  },

  onPullDownRefresh() {
    this.loadSummary().finally(() => wx.stopPullDownRefresh())
  },

  openMetric(event) {
    const url = event.currentTarget.dataset.url
    if (!url) return
    wx.navigateTo({
      url,
      fail: () => wx.showToast({ title: '页面打开失败', icon: 'none' })
    })
  },

  async loadSummary() {
    if (this.data.loading) return
    this.setData({ loading: true, error: '' })
    try {
      const summary = await dashboardApi.summary() || {}
      const metrics = metricDefinitions.map(([key, label, tone, icon, url]) => ({
        key,
        label,
        tone,
        icon,
        url: url || '',
        value: summary[key] !== null && summary[key] !== undefined ? summary[key] : '-'
      }))
      this.setData({ metrics, loading: false })
    } catch (error) {
      this.setData({ loading: false, error: error.message || '汇总数据加载失败' })
    }
  }
})
