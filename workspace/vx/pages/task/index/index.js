const guard = require('../../../utils/guard')
const taskApi = require('../../../api/task')
const lookupApi = require('../../../api/lookup')
const statusUtil = require('../../../utils/status')
const format = require('../../../utils/format')

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'WAIT_START', label: '待开工' },
  { value: 'RUNNING', label: '生产中' },
  { value: 'PAUSED', label: '暂停' },
  { value: 'FINISHED', label: '已完工' },
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
    filters: { keyword: '', status: '', operationId: '' },
    statusOptions,
    statusIndex: 0,
    sortOptions,
    sortIndex: 0,
    operationOptions: [{ id: '', label: '全部工序' }],
    operationIndex: 0,
    lookups: null,
    activeDropdown: '',
    dropdownOptions: []
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

  handleSearch() {
    this.loadPage(true)
  },

  handlePicker(event) {
    const field = event.currentTarget.dataset.field
    const optionsName = field + 'Options'
    const indexName = field + 'Index'
    const filterName = field === 'status' ? 'status' : field + 'Id'
    const index = Number(event.detail.value)
    const option = this.data[optionsName][index]
    this.setData({ [indexName]: index, ['filters.' + filterName]: option.value !== undefined ? option.value : option.id })
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
      operation: 'operationIndex'
    }
    const optionsMap = {
      status: this.data.statusOptions,
      sort: this.data.sortOptions,
      operation: this.data.operationOptions
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
    if (field === 'operation') {
      const option = this.data.operationOptions[index]
      this.setData({ operationIndex: index, 'filters.operationId': option.id })
      this.loadPage(true)
    }
  },

  clearFilters() {
    this.setData({
      filters: { keyword: '', status: '', operationId: '' },
      statusIndex: 0,
      operationIndex: 0,
      sortIndex: 0,
      activeDropdown: '',
      dropdownOptions: []
    })
    this.loadPage(true)
  },

  openDetail(event) {
    wx.navigateTo({ url: '/pages/task/detail/index?id=' + encodeURIComponent(event.currentTarget.dataset.id) })
  },

  sortRecords(records, direction) {
    if (direction === 'default') return records
    return records.slice().sort((left, right) => {
      const a = new Date(left.createdTime || 0).getTime()
      const b = new Date(right.createdTime || 0).getTime()
      return direction === 'oldest' ? a - b : b - a
    })
  },

  async ensureLookups() {
    const lookups = await lookupApi.load(['workOrder', 'operation', 'station'])
    this.setData({
      lookups,
      operationOptions: [{ id: '', label: '全部工序' }].concat(lookups.operation.items)
    })
    return lookups
  },

  async loadPage(reset) {
    if (this.data.loading) return
    if (!reset && this.data.finished) return
    const current = reset ? 1 : this.data.current
    this.setData({ loading: true, error: reset ? '' : this.data.error })
    try {
      const lookups = this.data.lookups || await this.ensureLookups()
      const page = await taskApi.page(format.compactQuery(Object.assign({ current, size: this.data.size }, this.data.filters)))
      const records = page && Array.isArray(page.records) ? page.records : []
      const decorated = records.map((item) => ({
        ...item,
        statusView: statusUtil.getStatus('task', item.status),
        workOrderLabel: lookupApi.label(lookups.workOrder, item.workOrderId, '工单'),
        operationLabel: lookupApi.label(lookups.operation, item.operationId, '工序'),
        stationLabel: lookupApi.label(lookups.station, item.stationId, '工位'),
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
      this.setData({ error: error.message || '任务加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  }
})
