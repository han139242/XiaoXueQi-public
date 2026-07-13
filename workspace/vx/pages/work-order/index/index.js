const guard = require('../../../utils/guard')
const workOrderApi = require('../../../api/work-order')
const lookupApi = require('../../../api/lookup')
const statusUtil = require('../../../utils/status')
const format = require('../../../utils/format')
const scanUtil = require('../../../utils/scan')

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'DRAFT', label: '新建' },
  { value: 'APPROVED', label: '已审核' },
  { value: 'RELEASED', label: '已下达' },
  { value: 'RUNNING', label: '生产中' },
  { value: 'FINISHED', label: '已完成' },
  { value: 'CLOSED', label: '已关闭' }
]

Page({
  data: {
    list: [],
    current: 1,
    size: 10,
    total: 0,
    pages: 0,
    loading: false,
    finished: false,
    error: '',
    filters: { keyword: '', status: '', productId: '' },
    statusOptions,
    statusIndex: 0,
    productOptions: [{ id: '', label: '全部产品' }],
    productIndex: 0,
    productLookup: null
  },

  onLoad() {
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
    this.setData({ 'filters.keyword': event.detail.value })
  },

  handleStatus(event) {
    const index = Number(event.detail.value)
    this.setData({ statusIndex: index, 'filters.status': statusOptions[index].value })
    this.loadPage(true)
  },

  handleProduct(event) {
    const index = Number(event.detail.value)
    const option = this.data.productOptions[index]
    this.setData({ productIndex: index, 'filters.productId': option.id })
    this.loadPage(true)
  },

  handleSearch() {
    this.loadPage(true)
  },

  startScan() {
    scanUtil.scanToResult()
  },

  clearFilters() {
    this.setData({
      filters: { keyword: '', status: '', productId: '' },
      statusIndex: 0,
      productIndex: 0
    })
    this.loadPage(true)
  },

  openDetail(event) {
    const id = event.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/work-order/detail/index?id=' + encodeURIComponent(id) })
  },

  async ensureLookup() {
    const lookups = await lookupApi.load(['product'])
    const productLookup = lookups.product
    this.setData({
      productLookup,
      productOptions: [{ id: '', label: '全部产品' }].concat(productLookup.items)
    })
    return productLookup
  },

  async loadPage(reset) {
    if (this.data.loading) return
    if (!reset && this.data.finished) return
    const current = reset ? 1 : this.data.current
    this.setData({ loading: true, error: reset ? '' : this.data.error })
    try {
      const productLookup = this.data.productLookup || await this.ensureLookup()
      const page = await workOrderApi.page(format.compactQuery(Object.assign({
        current,
        size: this.data.size
      }, this.data.filters)))
      const records = page && Array.isArray(page.records) ? page.records : []
      const decorated = records.map((item) => ({
        ...item,
        statusView: statusUtil.getStatus('workOrder', item.status),
        productLabel: lookupApi.label(productLookup, item.productId, '产品'),
        createdTimeText: format.formatDateTime(item.createdTime)
      }))
      const pages = Number(page && page.pages) || 0
      this.setData({
        list: reset ? decorated : this.data.list.concat(decorated),
        current: current + 1,
        total: Number(page && page.total) || 0,
        pages,
        finished: pages === 0 || current >= pages,
        error: ''
      })
    } catch (error) {
      this.setData({ error: error.message || '工单加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  }
})
