const guard = require('../../../utils/guard')
const productionOrderApi = require('../../../api/production-order')
const lookupApi = require('../../../api/lookup')
const statusUtil = require('../../../utils/status')
const format = require('../../../utils/format')

Page({
  data: {
    list: [],
    current: 1,
    size: 10,
    loading: false,
    finished: false,
    error: '',
    keyword: '',
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

  handleKeyword(event) {
    this.setData({ keyword: event.detail.value })
  },

  search() {
    this.loadPage(true)
  },

  clearKeyword() {
    if (!this.data.keyword) return
    this.setData({ keyword: '' })
    this.loadPage(true)
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
      createdTimeText: format.formatDateTime(item.createdTime),
      deliveryDateText: format.display(item.deliveryDate)
    }
  },

  async loadPage(reset) {
    if (this.data.loading) return
    if (!reset && this.data.finished) return
    const current = reset ? 1 : this.data.current
    this.setData({ loading: true, error: reset ? '' : this.data.error })
    try {
      const lookups = this.data.lookups || await this.ensureLookups()
      const page = await productionOrderApi.page(format.compactQuery({
        current,
        size: this.data.size,
        keyword: this.data.keyword.trim()
      }))
      const records = page && Array.isArray(page.records) ? page.records : []
      const pages = Number(page && page.pages) || 0
      this.setData({
        list: reset ? records.map((item) => this.decorate(item, lookups)) : this.data.list.concat(records.map((item) => this.decorate(item, lookups))),
        current: current + 1,
        finished: pages === 0 || current >= pages,
        error: ''
      })
    } catch (error) {
      this.setData({ error: error.message || '生产订单加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  openDetail(event) {
    wx.navigateTo({ url: '/pages/production-order/detail/index?id=' + encodeURIComponent(event.currentTarget.dataset.id) })
  }
})
