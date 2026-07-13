const guard = require('../../../utils/guard')
const auth = require('../../../utils/auth')
const reportApi = require('../../../api/work-report')
const lookupApi = require('../../../api/lookup')
const statusUtil = require('../../../utils/status')
const format = require('../../../utils/format')

const auditOptions = [
  { value: '', label: '全部审核状态' },
  { value: 'SUBMITTED', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' }
]

Page({
  data: {
    list: [],
    current: 1,
    size: 10,
    pages: 0,
    loading: false,
    finished: false,
    error: '',
    keyword: '',
    auditStatus: '',
    auditOptions,
    auditIndex: 0,
    employeeId: null,
    myOnly: false,
    lookups: null
  },

  onLoad() {
    const user = auth.getUser()
    const employeeId = user && user.employeeId
    this.setData({ employeeId: employeeId || null, myOnly: Boolean(employeeId) })
    this.loadPage(true)
  },

  onShow() {
    guard.requireAuth()
  },

  onPullDownRefresh() {
    this.loadPage(true).finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    this.loadPage(false)
  },

  handleKeyword(event) {
    this.setData({ keyword: event.detail.value })
  },

  handleAudit(event) {
    const index = Number(event.detail.value)
    this.setData({ auditIndex: index, auditStatus: auditOptions[index].value })
    this.loadPage(true)
  },

  toggleMyOnly(event) {
    this.setData({ myOnly: event.detail.value })
    this.loadPage(true)
  },

  search() {
    this.loadPage(true)
  },

  openDetail(event) {
    wx.navigateTo({ url: '/pages/report/detail/index?id=' + encodeURIComponent(event.currentTarget.dataset.id) })
  },

  async loadPage(reset) {
    if (this.data.loading) return
    if (!reset && this.data.finished) return
    const current = reset ? 1 : this.data.current
    this.setData({ loading: true, error: reset ? '' : this.data.error })
    try {
      const lookups = this.data.lookups || await lookupApi.load(['workOrder', 'station', 'employee'])
      if (!this.data.lookups) this.setData({ lookups })
      const query = format.compactQuery({
        current,
        size: this.data.size,
        keyword: this.data.keyword.trim(),
        auditStatus: this.data.auditStatus,
        operatorId: this.data.myOnly ? this.data.employeeId : ''
      })
      const page = await reportApi.page(query)
      const records = page && Array.isArray(page.records) ? page.records : []
      const decorated = records.map((item) => ({
        ...item,
        statusView: statusUtil.getStatus('audit', item.auditStatus),
        operatorLabel: lookupApi.label(lookups.employee, item.operatorId, '员工'),
        stationLabel: lookupApi.label(lookups.station, item.stationId, '工位'),
        reportTimeText: format.formatDateTime(item.reportTime)
      }))
      const pages = Number(page && page.pages) || 0
      this.setData({
        list: reset ? decorated : this.data.list.concat(decorated),
        current: current + 1,
        pages,
        finished: pages === 0 || current >= pages,
        error: ''
      })
    } catch (error) {
      this.setData({ error: error.message || '报工记录加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  }
})
