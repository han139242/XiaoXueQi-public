const guard = require('../../../utils/guard')
const equipmentApi = require('../../../api/equipment')
const lookupApi = require('../../../api/lookup')
const format = require('../../../utils/format')

const statusMap = {
  SUBMITTED: ['待派工', 'warning'],
  ASSIGNED: ['已派工', 'primary'],
  PROCESSING: ['维修中', 'warning'],
  CLOSED: ['已关闭', 'success'],
  CANCELLED: ['已取消', 'neutral']
}

Page({
  data: {
    list: [],
    current: 1,
    size: 10,
    loading: false,
    finished: false,
    error: '',
    lookups: null
  },

  onLoad() {
    this.loadPage(true)
  },

  onShow() {
    if (!guard.requireAuth()) return
    if (!this.data.loading) this.loadPage(true)
  },

  onPullDownRefresh() {
    this.loadPage(true).finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    this.loadPage(false)
  },

  async ensureLookups() {
    const lookups = await lookupApi.load(['equipment', 'employee'])
    this.setData({ lookups })
    return lookups
  },

  decorate(item, lookups) {
    const statusView = statusMap[item.status] || [item.status || '-', 'neutral']
    return {
      ...item,
      statusText: statusView[0],
      statusTone: statusView[1],
      equipmentLabel: lookupApi.label(lookups.equipment, item.equipmentId, '设备'),
      applicantLabel: lookupApi.label(lookups.employee, item.applicantId, '员工'),
      repairerLabel: item.repairerId ? lookupApi.label(lookups.employee, item.repairerId, '员工') : '未派工',
      reportedTimeText: format.formatDateTime(item.reportedTime)
    }
  },

  async loadPage(reset) {
    if (this.data.loading) return
    if (!reset && this.data.finished) return
    const current = reset ? 1 : this.data.current
    this.setData({ loading: true, error: reset ? '' : this.data.error })
    try {
      const lookups = this.data.lookups || await this.ensureLookups()
      const page = await equipmentApi.repairPage({ current, size: this.data.size })
      const records = page && Array.isArray(page.records) ? page.records : []
      const decorated = records.map((item) => this.decorate(item, lookups))
      const pages = Number(page && page.pages) || 0
      this.setData({
        list: reset ? decorated : this.data.list.concat(decorated),
        current: current + 1,
        finished: pages === 0 || current >= pages,
        error: ''
      })
    } catch (error) {
      this.setData({ error: error.message || '维修单加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  openDetail(event) {
    wx.navigateTo({ url: '/pages/equipment/repair-detail/index?id=' + encodeURIComponent(event.currentTarget.dataset.id) })
  }
})
