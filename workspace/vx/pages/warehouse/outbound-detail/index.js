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
    outboundId: '',
    detail: null,
    lines: [],
    canConfirm: false,
    loading: false,
    error: '',
    lookups: null
  },

  onLoad(options) {
    this.setData({ outboundId: options && options.id ? decodeURIComponent(options.id) : '' })
    this.loadDetail()
  },

  onShow() {
    if (!guard.requireAuth()) return
    this.setData({ canConfirm: auth.hasPermission('wms:outbound-order:confirm') })
    if (this.data.outboundId && !this.data.loading) this.loadDetail()
  },

  onPullDownRefresh() {
    this.loadDetail().finally(() => wx.stopPullDownRefresh())
  },

  async ensureLookups() {
    const lookups = await lookupApi.load(['warehouse', 'material', 'location', 'workOrder'])
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
      workOrderLabel: item.workOrderId ? lookupApi.label(lookups.workOrder, item.workOrderId, '工单') : '-',
      outboundTimeText: format.formatDateTime(item.outboundTime),
      createdTimeText: format.formatDateTime(item.createdTime),
      remarkText: format.display(item.remark)
    }
  },

  decorateLine(item, lookups) {
    return {
      ...item,
      materialLabel: lookupApi.label(lookups.material, item.materialId, '物料'),
      locationLabel: lookupApi.label(lookups.location, item.locationId, '库位')
    }
  },

  async loadDetail() {
    if (this.data.loading) return
    if (!this.data.outboundId) {
      this.setData({ error: '缺少出库单 ID' })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const lookups = this.data.lookups || await this.ensureLookups()
      const detail = await warehouseApi.outboundDetail(this.data.outboundId)
      const lines = await warehouseApi.outboundDetails(this.data.outboundId)
      this.setData({
        detail: detail ? this.decorateDetail(detail, lookups) : null,
        lines: Array.isArray(lines) ? lines.map((item) => this.decorateLine(item, lookups)) : [],
        error: detail ? '' : '出库单不存在或已删除'
      })
    } catch (error) {
      this.setData({ error: error.message || '出库详情加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  confirmOrder() {
    if (!guard.requirePermission('wms:outbound-order:confirm')) return
    wx.showModal({
      title: '确认出库',
      content: '确认该出库单已完成实物出库？',
      success: async (result) => {
        if (!result.confirm) return
        try {
          const user = auth.getUser() || {}
          await warehouseApi.confirmOutbound(this.data.outboundId, {
            operatorId: user.employeeId || user.userId,
            remark: '小程序确认出库'
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
