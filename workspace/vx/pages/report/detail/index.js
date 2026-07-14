const guard = require('../../../utils/guard')
const reportApi = require('../../../api/work-report')
const lookupApi = require('../../../api/lookup')
const statusUtil = require('../../../utils/status')
const format = require('../../../utils/format')

Page({
  data: { id: null, report: null, materials: [], loading: true, error: '' },

  onLoad(options) {
    const id = Number(options.id)
    if (!Number.isInteger(id) || id <= 0) {
      this.setData({ loading: false, error: '报工 ID 无效' })
      return
    }
    this.setData({ id })
    this.loadData()
  },

  onShow() {
    if (!guard.requireAuth()) return
    if (this.data.id && !this.data.loading) this.loadData()
  },

  onPullDownRefresh() {
    this.loadData().finally(() => wx.stopPullDownRefresh())
  },

  async loadData() {
    this.setData({ loading: true, error: '' })
    try {
      const [report, materials, lookups] = await Promise.all([
        reportApi.detail(this.data.id),
        reportApi.materials(this.data.id),
        lookupApi.load(['station', 'employee'])
      ])
      if (!report) {
        this.setData({ report: null, materials: [], loading: false })
        return
      }
      this.setData({
        report: {
          ...report,
          statusView: statusUtil.getStatus('audit', report.auditStatus),
          operatorLabel: lookupApi.label(lookups.employee, report.operatorId, '员工'),
          stationLabel: lookupApi.label(lookups.station, report.stationId, '工位'),
          reportTimeText: format.formatDateTime(report.reportTime),
          createdTimeText: format.formatDateTime(report.createdTime)
        },
        materials: Array.isArray(materials) ? materials : [],
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false, error: error.message || '报工详情加载失败' })
    }
  }
})
