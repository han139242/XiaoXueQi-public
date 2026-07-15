const guard = require('../../../utils/guard')
const auth = require('../../../utils/auth')
const qualityApi = require('../../../api/quality')
const lookupApi = require('../../../api/lookup')
const format = require('../../../utils/format')

const statusMap = {
  WAIT_INSPECTION: ['待检', 'warning'],
  INSPECTING: ['检验中', 'primary'],
  DRAFT: ['待检', 'warning'],
  RUNNING: ['检验中', 'primary'],
  FINISHED: ['已完成', 'success'],
  CLOSED: ['已关闭', 'neutral']
}

const typeMap = {
  first: '首检',
  process: '巡检',
  final: '终检',
  incoming: '来料检验',
  FIRST: '首检',
  PROCESS: '巡检',
  FINAL: '终检',
  INCOMING: '来料检验'
}

Page({
  data: {
    list: [],
    current: 1,
    size: 10,
    pages: 0,
    loading: false,
    finished: false,
    error: '',
    lookups: null,
    canSubmit: false
  },

  onLoad() {
    this.loadPage(true)
  },

  onShow() {
    if (!guard.requireAuth()) return
    this.setData({ canSubmit: auth.hasPermission('qms:inspection-order:submit') })
    if (!this.data.loading) this.loadPage(true)
  },

  onPullDownRefresh() {
    this.loadPage(true).finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    this.loadPage(false)
  },

  async ensureLookups() {
    const lookups = await lookupApi.load(['workOrder', 'task', 'report'])
    this.setData({ lookups })
    return lookups
  },

  decorate(item, lookups) {
    const statusView = statusMap[item.status] || [item.status || '-', 'neutral']
    return {
      ...item,
      statusText: statusView[0],
      statusTone: statusView[1],
      typeText: typeMap[item.inspectionType] || item.inspectionType || '-',
      workOrderLabel: lookupApi.label(lookups.workOrder, item.workOrderId, '工单'),
      taskLabel: lookupApi.label(lookups.task, item.taskId, '任务'),
      reportLabel: lookupApi.label(lookups.report, item.reportId, '报工'),
      createdTimeText: format.formatDateTime(item.createdTime)
    }
  },

  async loadPage(reset) {
    if (this.data.loading) return
    if (!reset && this.data.finished) return
    const current = reset ? 1 : this.data.current
    this.setData({ loading: true, error: reset ? '' : this.data.error })
    try {
      const lookups = this.data.lookups || await this.ensureLookups()
      const page = await qualityApi.inspectionPage({
        current,
        size: this.data.size,
        status: 'DRAFT'
      })
      const records = page && Array.isArray(page.records) ? page.records : []
      const decorated = records.map((item) => this.decorate(item, lookups))
      const pages = Number(page && page.pages) || 0
      this.setData({
        list: reset ? decorated : this.data.list.concat(decorated),
        current: current + 1,
        pages,
        finished: pages === 0 || current >= pages,
        error: ''
      })
    } catch (error) {
      this.setData({ error: error.message || '待检任务加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  openDetail(event) {
    const inspectionId = event.currentTarget.dataset.id
    if (!inspectionId) return
    wx.navigateTo({
      url: '/pages/quality/pending-detail/index?inspectionId=' + encodeURIComponent(inspectionId)
    })
  },

  openInspectionForm(event) {
    if (!guard.requirePermission('qms:inspection-order:submit')) return
    wx.navigateTo({
      url: '/pages/quality/inspection-form/index?inspectionId=' + encodeURIComponent(event.currentTarget.dataset.id)
    })
  }
})
