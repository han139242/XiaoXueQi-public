// 出库单列表页：仓库人员查看生产领料、销售或其他出库记录。
const guard = require('../../../utils/guard')
const auth = require('../../../utils/auth')
const warehouseApi = require('../../../api/warehouse')
const lookupApi = require('../../../api/lookup')
const format = require('../../../utils/format')

const statusMap = {
  SUBMITTED: ['待确认', 'warning'],
  APPROVED: ['已确认', 'success'],
  REJECTED: ['已驳回', 'danger'],
  CLOSED: ['已关闭', 'neutral']
}

const typeMap = {
  MATERIAL_ISSUE: '生产领料',
  SALE: '销售出库',
  SCRAP: '报废出库',
  OTHER: '其他出库'
}

Page({
  data: {
    list: [],
    current: 1,
    size: 10,
    loading: false,
    finished: false,
    canConfirm: false,
    error: '',
    lookups: null
  },

  onLoad() {
    this.loadPage(true)
  },

  onShow() {
    if (!guard.requireAuth()) return
    this.setData({ canConfirm: auth.hasPermission('wms:outbound-order:confirm') })
    if (!this.data.loading) this.loadPage(true)
  },

  onPullDownRefresh() {
    this.loadPage(true).finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    this.loadPage(false)
  },

  async ensureLookups() {
    const lookups = await lookupApi.load(['warehouse', 'workOrder'])
    this.setData({ lookups })
    return lookups
  },

  decorate(item, lookups) {
    const statusView = statusMap[item.auditStatus] || [item.auditStatus || '-', 'neutral']
    return {
      ...item,
      typeText: typeMap[item.outboundType] || item.outboundType || '-',
      statusText: statusView[0],
      statusTone: statusView[1],
      warehouseLabel: lookupApi.label(lookups.warehouse, item.warehouseId, '仓库'),
      workOrderLabel: item.workOrderId ? lookupApi.label(lookups.workOrder, item.workOrderId, '工单') : '-',
      timeText: format.formatDateTime(item.outboundTime || item.createdTime)
    }
  },

  async loadPage(reset) {
    if (this.data.loading) return
    if (!reset && this.data.finished) return
    const current = reset ? 1 : this.data.current
    this.setData({ loading: true, error: reset ? '' : this.data.error })
    try {
      const lookups = this.data.lookups || await this.ensureLookups()
      const page = await warehouseApi.outboundPage({ current, size: this.data.size })
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
      this.setData({ error: error.message || '出库单加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  openDetail(event) {
    wx.navigateTo({ url: '/pages/warehouse/outbound-detail/index?id=' + encodeURIComponent(event.currentTarget.dataset.id) })
  },

  confirmOrder(event) {
    if (!guard.requirePermission('wms:outbound-order:confirm')) return
    const outboundId = event.currentTarget.dataset.id
    wx.showModal({
      title: '确认出库',
      content: '确认该出库单已完成实物出库？',
      success: async (result) => {
        if (!result.confirm) return
        try {
          const user = auth.getUser() || {}
          await warehouseApi.confirmOutbound(outboundId, {
            operatorId: user.employeeId || user.userId,
            remark: '小程序确认出库'
          })
          wx.showToast({ title: '已确认', icon: 'success' })
          this.loadPage(true)
        } catch (error) {
          wx.showToast({ title: error.message || '确认失败', icon: 'none' })
        }
      }
    })
  }
})
