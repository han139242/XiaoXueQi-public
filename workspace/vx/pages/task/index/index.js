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
    filters: { keyword: '', status: '', workOrderId: '', operationId: '', stationId: '' },
    statusOptions,
    statusIndex: 0,
    workOrderOptions: [{ id: '', label: '全部工单' }],
    operationOptions: [{ id: '', label: '全部工序' }],
    stationOptions: [{ id: '', label: '全部工位' }],
    workOrderIndex: 0,
    operationIndex: 0,
    stationIndex: 0,
    lookups: null
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

  clearFilters() {
    this.setData({
      filters: { keyword: '', status: '', workOrderId: '', operationId: '', stationId: '' },
      statusIndex: 0,
      workOrderIndex: 0,
      operationIndex: 0,
      stationIndex: 0
    })
    this.loadPage(true)
  },

  openDetail(event) {
    wx.navigateTo({ url: '/pages/task/detail/index?id=' + encodeURIComponent(event.currentTarget.dataset.id) })
  },

  async ensureLookups() {
    const lookups = await lookupApi.load(['workOrder', 'operation', 'station'])
    this.setData({
      lookups,
      workOrderOptions: [{ id: '', label: '全部工单' }].concat(lookups.workOrder.items),
      operationOptions: [{ id: '', label: '全部工序' }].concat(lookups.operation.items),
      stationOptions: [{ id: '', label: '全部工位' }].concat(lookups.station.items)
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
        list: reset ? decorated : this.data.list.concat(decorated),
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
