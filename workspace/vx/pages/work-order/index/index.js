// 工单列表页：查看生产工单，并进入工单详情或执行开工。
const guard = require('../../../utils/guard')
const workOrderApi = require('../../../api/work-order')
const lookupApi = require('../../../api/lookup')
const statusUtil = require('../../../utils/status')
const format = require('../../../utils/format')

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'WAIT_START', label: '待开始' },
  { value: 'RUNNING', label: '生产中' },
  { value: 'FINISHED', label: '已完成' },
  { value: 'CLOSED', label: '已关闭' }
]

const sortOptions = [
  { value: 'default', label: '排序' },
  { value: 'newest', label: '最新创建' },
  { value: 'oldest', label: '最早创建' }
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
    sortOptions,
    sortIndex: 0,
    productOptions: [{ id: '', label: '全部产品' }],
    productIndex: 0,
    productLookup: null,
    activeDropdown: '',
    dropdownOptions: [],
    scanFabLeft: 0,
    scanFabTop: 0,
    scanFabSize: 42
  },

  onLoad() {
    this.initScanFab()
    this.loadPage(true)
  },

  onShow() {
    if (!guard.requireAuth()) return
    const tabBar = typeof this.getTabBar === 'function' ? this.getTabBar() : null
    if (tabBar) tabBar.setData({ currentPath: '/pages/work-order/index/index' })
    if (!this.data.loading) this.loadPage(true)
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

  handleSort(event) {
    const sortIndex = Number(event.detail.value)
    this.setData({ sortIndex, list: this.sortRecords(this.data.list, sortOptions[sortIndex].value) })
  },

  toggleDropdown(event) {
    const field = event.currentTarget.dataset.field
    if (this.data.activeDropdown === field) {
      this.closeDropdown()
      return
    }
    this.setData({
      activeDropdown: field,
      dropdownOptions: this.buildDropdownOptions(field)
    })
  },

  closeDropdown() {
    this.setData({ activeDropdown: '', dropdownOptions: [] })
  },

  buildDropdownOptions(field) {
    const indexMap = {
      status: 'statusIndex',
      sort: 'sortIndex',
      product: 'productIndex'
    }
    const optionsMap = {
      status: this.data.statusOptions,
      sort: this.data.sortOptions,
      product: this.data.productOptions
    }
    const selectedIndex = this.data[indexMap[field]]
    return (optionsMap[field] || []).map((item, index) => ({
      ...item,
      index,
      selected: index === selectedIndex
    }))
  },

  selectDropdown(event) {
    const field = event.currentTarget.dataset.field
    const index = Number(event.currentTarget.dataset.index)
    this.closeDropdown()
    if (field === 'status') {
      this.setData({ statusIndex: index, 'filters.status': statusOptions[index].value })
      this.loadPage(true)
      return
    }
    if (field === 'sort') {
      this.setData({ sortIndex: index, list: this.sortRecords(this.data.list, sortOptions[index].value) })
      return
    }
    if (field === 'product') {
      const option = this.data.productOptions[index]
      this.setData({ productIndex: index, 'filters.productId': option.id })
      this.loadPage(true)
    }
  },

  handleSearch() {
    this.loadPage(true)
  },

  initScanFab() {
    const info = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync()
    const size = this.data.scanFabSize
    this.setData({
      scanFabLeft: Math.max(8, info.windowWidth - size - 18),
      scanFabTop: Math.max(8, info.windowHeight - size - 112)
    })
  },

  handleScanFabTouchStart(event) {
    const touch = event.touches && event.touches[0]
    if (!touch) return
    this.scanFabStart = {
      x: touch.clientX,
      y: touch.clientY,
      left: this.data.scanFabLeft,
      top: this.data.scanFabTop
    }
    this.scanFabMoved = false
  },

  handleScanFabTouchMove(event) {
    const touch = event.touches && event.touches[0]
    if (!touch || !this.scanFabStart) return
    const info = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync()
    const size = this.data.scanFabSize
    const dx = touch.clientX - this.scanFabStart.x
    const dy = touch.clientY - this.scanFabStart.y
    if (Math.abs(dx) > 3 || Math.abs(dy) > 3) this.scanFabMoved = true
    this.setData({
      scanFabLeft: Math.max(8, Math.min(this.scanFabStart.left + dx, info.windowWidth - size - 8)),
      scanFabTop: Math.max(8, Math.min(this.scanFabStart.top + dy, info.windowHeight - size - 72))
    })
  },

  handleScanFabTouchEnd() {
    this.scanFabIgnoreTap = this.scanFabMoved
    setTimeout(() => {
      this.scanFabMoved = false
      this.scanFabIgnoreTap = false
    }, 350)
  },

  startScan() {
    if (this.scanFabMoved || this.scanFabIgnoreTap) return
    wx.scanCode({
      scanType: ['barCode', 'qrCode'],
      success: (result) => {
        const value = String(result.result || '').trim()
        if (!value) {
          wx.showToast({ title: '未识别到工单信息', icon: 'none' })
          return
        }
        this.setData({ 'filters.keyword': value })
        this.loadPage(true)
        wx.showToast({ title: '已录入工单信息', icon: 'success' })
      },
      fail(error) {
        if (!String(error.errMsg || '').includes('cancel')) {
          wx.showToast({ title: '扫码失败，请重试', icon: 'none' })
        }
      }
    })
  },

  clearFilters() {
    this.setData({
      filters: { keyword: '', status: '', productId: '' },
      statusIndex: 0,
      productIndex: 0,
      sortIndex: 0,
      activeDropdown: '',
      dropdownOptions: []
    })
    this.loadPage(true)
  },

  openDetail(event) {
    const id = event.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/work-order/detail/index?id=' + encodeURIComponent(id) })
  },

  sortRecords(records, direction) {
    if (direction === 'default') return records
    return records.slice().sort((left, right) => {
      const a = new Date(left.createdTime || 0).getTime()
      const b = new Date(right.createdTime || 0).getTime()
      return direction === 'oldest' ? a - b : b - a
    })
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
        list: this.sortRecords(reset ? decorated : this.data.list.concat(decorated), sortOptions[this.data.sortIndex].value),
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
