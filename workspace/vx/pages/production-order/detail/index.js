const guard = require('../../../utils/guard')
const productionOrderApi = require('../../../api/production-order')
const lookupApi = require('../../../api/lookup')
const statusUtil = require('../../../utils/status')
const format = require('../../../utils/format')

const sourceMap = {
  ERP: 'ERP 同步',
  MANUAL: '手工创建'
}

Page({
  data: {
    id: '',
    detail: null,
    loading: false,
    error: '',
    lookups: null
  },

  onLoad(options) {
    this.setData({ id: options && options.id ? decodeURIComponent(options.id) : '' })
    this.loadDetail()
  },

  onShow() {
    if (!guard.requireAuth()) return
    if (this.data.id && !this.data.loading) this.loadDetail()
  },

  onPullDownRefresh() {
    this.loadDetail().finally(() => wx.stopPullDownRefresh())
  },

  async ensureLookups() {
    const lookups = await lookupApi.load(['customer', 'product'])
    this.setData({ lookups })
    return lookups
  },

  decorate(item, lookups) {
    return {
      ...item,
      statusView: statusUtil.getStatus('productionOrder', item.status),
      customerLabel: lookupApi.label(lookups.customer, item.customerId, '客户'),
      productLabel: lookupApi.label(lookups.product, item.productId, '产品'),
      sourceText: sourceMap[item.sourceType] || item.sourceType || '-',
      deliveryDateText: format.display(item.deliveryDate),
      createdTimeText: format.formatDateTime(item.createdTime),
      updatedTimeText: format.formatDateTime(item.updatedTime),
      remarkText: format.display(item.remark)
    }
  },

  async loadDetail() {
    if (this.data.loading) return
    if (!this.data.id) {
      this.setData({ error: '缺少生产订单 ID' })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const lookups = this.data.lookups || await this.ensureLookups()
      const detail = await productionOrderApi.detail(this.data.id)
      this.setData({
        detail: detail ? this.decorate(detail, lookups) : null,
        error: detail ? '' : '生产订单不存在或已删除'
      })
    } catch (error) {
      this.setData({ error: error.message || '生产订单详情加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  }
})
