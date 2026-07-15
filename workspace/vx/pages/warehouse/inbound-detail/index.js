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

Page({
  data: {
    inboundId: '',
    detail: null,
    lines: [],
    canConfirm: false,
    loading: false,
    error: '',
    lookups: null
  },

  onLoad(options) {
    this.setData({ inboundId: options && options.id ? decodeURIComponent(options.id) : '' })
    this.loadDetail()
  },

  onShow() {
    if (!guard.requireAuth()) return
    this.setData({ canConfirm: auth.hasPermission('wms:inbound-order:confirm') })
    if (this.data.inboundId && !this.data.loading) this.loadDetail()
  },

  onPullDownRefresh() {
    this.loadDetail().finally(() => wx.stopPullDownRefresh())
  },

  async ensureLookups() {
    const lookups = await lookupApi.load(['warehouse', 'material', 'location', 'batch'])
    this.setData({ lookups })
    return lookups
  },

  decorateDetail(item, lookups) {
    const statusView = statusMap[item.auditStatus] || [item.auditStatus || '-', 'neutral']
    return {
      ...item,
      statusText: statusView[0],
      statusTone: statusView[1],
      warehouseLabel: lookupApi.label(lookups.warehouse, item.warehouseId, '仓库'),
      inboundTimeText: format.formatDateTime(item.inboundTime),
      createdTimeText: format.formatDateTime(item.createdTime),
      remarkText: format.display(item.remark)
    }
  },

  decorateLine(item, lookups) {
    return {
      ...item,
      materialLabel: lookupApi.label(lookups.material, item.materialId, '物料'),
      locationLabel: lookupApi.label(lookups.location, item.locationId, '库位'),
      batchLabel: lookupApi.label(lookups.batch, item.batchId, '批次')
    }
  },

  async loadDetail() {
    if (this.data.loading) return
    if (!this.data.inboundId) {
      this.setData({ error: '缺少入库单 ID' })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const lookups = this.data.lookups || await this.ensureLookups()
      const detail = await warehouseApi.inboundDetail(this.data.inboundId)
      const lines = await warehouseApi.inboundDetails(this.data.inboundId)
      this.setData({
        detail: detail ? this.decorateDetail(detail, lookups) : null,
        lines: Array.isArray(lines) ? lines.map((item) => this.decorateLine(item, lookups)) : [],
        error: detail ? '' : '入库单不存在或已删除'
      })
    } catch (error) {
      this.setData({ error: error.message || '入库详情加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  confirmOrder() {
    if (!guard.requirePermission('wms:inbound-order:confirm')) return
    wx.showModal({
      title: '确认入库',
      content: '确认该入库单已完成实物入库？',
      success: async (result) => {
        if (!result.confirm) return
        try {
          const user = auth.getUser() || {}
          await warehouseApi.confirmInbound(this.data.inboundId, {
            operatorId: user.employeeId || user.userId,
            remark: '小程序确认入库'
          })
          wx.showToast({ title: '已确认', icon: 'success' })
          this.loadDetail()
        } catch (error) {
          wx.showToast({ title: error.message || '确认失败', icon: 'none' })
        }
      }
    })
  }
})
