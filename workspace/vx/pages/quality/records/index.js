const guard = require('../../../utils/guard')
const qualityApi = require('../../../api/quality')
const lookupApi = require('../../../api/lookup')
const format = require('../../../utils/format')

const statusMap = {
  WAIT_INSPECTION: ['待检', 'warning'],
  INSPECTING: ['检验中', 'primary'],
  FINISHED: ['已完成', 'success'],
  CLOSED: ['已关闭', 'neutral']
}

const typeMap = {
  first: '首检',
  process: '巡检',
  final: '终检',
  incoming: '来料检验',
  FIRST: '首检',
  PROCESS: '巡检',
  FINAL: '终检',
  INCOMING: '来料检验'
}

const resultMap = {
  PASS: ['合格', 'success'],
  FAIL: ['不合格', 'danger'],
  CONCESSION: ['让步接收', 'warning']
}

const resultOptions = [
  { value: '', label: '全部结果' },
  { value: 'PASS', label: '合格' },
  { value: 'FAIL', label: '不合格' },
  { value: 'CONCESSION', label: '让步接收' }
]

const dispositionMap = {
  REWORK: '返工',
  SCRAP: '报废',
  DOWNGRADE: '降级',
  CONCESSION: '让步接收'
}

Page({
  data: {
    list: [],
    current: 1,
    size: 10,
    pages: 0,
    total: 0,
    loading: false,
    finished: false,
    error: '',
    keyword: '',
    filters: { inspectionResult: '' },
    resultOptions,
    resultIndex: 0,
    activeFilter: '',
    dropdownOptions: [],
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
    this.setData({ keyword: '', activeFilter: '' })
    this.loadPage(true)
  },

  toggleFilter(event) {
    const field = event.currentTarget.dataset.field
    if (this.data.activeFilter === field) {
      this.setData({ activeFilter: '', dropdownOptions: [] })
      return
    }
    const selectedValue = this.data.filters.inspectionResult
    this.setData({
      activeFilter: field,
      dropdownOptions: field === 'keyword' ? [] : resultOptions.map((item, index) => ({
        ...item,
        index,
        selected: item.value === selectedValue
      }))
    })
  },

  selectFilter(event) {
    const field = this.data.activeFilter
    const index = Number(event.currentTarget.dataset.index)
    if (field === 'result') {
      this.setData({ resultIndex: index, 'filters.inspectionResult': resultOptions[index].value })
    }
    this.setData({ activeFilter: '', dropdownOptions: [] })
    this.loadPage(true)
  },

  submitKeyword() {
    this.setData({ activeFilter: '' })
    this.loadPage(true)
  },

  handleResult(event) {
    const index = Number(event.detail.value)
    this.setData({
      resultIndex: index,
      'filters.inspectionResult': resultOptions[index].value
    })
    this.loadPage(true)
  },

  async ensureLookups() {
    const lookups = await lookupApi.load(['workOrder', 'employee'])
    this.setData({ lookups })
    return lookups
  },

  decorate(item, lookups) {
    const statusView = statusMap[item.status] || [item.status || '-', 'neutral']
    const hasResult = item.inspectionResult !== null && item.inspectionResult !== undefined && item.inspectionResult !== ''
    const resultView = hasResult ? (resultMap[item.inspectionResult] || [item.inspectionResult, 'neutral']) : ['未判定', 'neutral']
    const sampleQuantity = Number(item.sampleQuantity || 0)
    const unqualifiedQuantity = Number(item.unqualifiedQuantity || 0)
    return {
      ...item,
      statusText: statusView[0],
      statusTone: statusView[1],
      resultText: resultView[0],
      resultTone: resultView[1],
      typeText: typeMap[item.inspectionType] || item.inspectionType || '-',
      workOrderLabel: lookupApi.label(lookups.workOrder, item.workOrderId, '工单'),
      inspectorLabel: lookupApi.label(lookups.employee, item.inspectorId, '员工'),
      taskLabel: item.taskNo || (item.taskId ? '任务 ID ' + item.taskId : '-'),
      reportLabel: item.reportNo || (item.reportId ? '报工 ID ' + item.reportId : '-'),
      hasQuantity: sampleQuantity > 0,
      unqualifiedRateText: sampleQuantity > 0 ? (unqualifiedQuantity / sampleQuantity * 100).toFixed(4) + '%' : '-',
      inspectionTimeText: format.formatDateTime(item.inspectionTime),
      updatedTimeText: format.formatDateTime(item.updatedTime || item.createdTime)
    }
  },

  async loadNonconformingSummary(item) {
    if (item.inspectionResult !== 'FAIL') return {}
    try {
      const page = await qualityApi.nonconformingPage({
        current: 1,
        size: 20,
        inspectionId: item.inspectionId
      })
      const orders = page && Array.isArray(page.records) ? page.records : []
      const dispositions = Array.from(new Set(orders.map((order) => {
        return dispositionMap[String(order.dispositionType || '').toUpperCase()] || order.dispositionType
      }).filter(Boolean)))
      return {
        dispositionText: dispositions.length ? dispositions.join('、') : '待录入',
        nonconformingNoText: orders.length
          ? orders.map((order) => order.ncrNo || ('不良品项 ' + order.ncrId)).join('、')
          : '未关联不良品项'
      }
    } catch (error) {
      return { dispositionText: '暂无法获取', nonconformingNoText: '暂无法获取' }
    }
  },

  async loadPage(reset) {
    if (this.data.loading) return
    if (!reset && this.data.finished) return
    const current = reset ? 1 : this.data.current
    this.setData({ loading: true, error: reset ? '' : this.data.error })
    try {
      const lookups = this.data.lookups || await this.ensureLookups()
      const resultFilter = this.data.filters.inspectionResult
      const page = await qualityApi.inspectionPage(format.compactQuery({
        current,
        size: this.data.size,
        keyword: this.data.keyword.trim(),
        status: 'FINISHED',
        inspectionResult: resultFilter
      }))
      const records = page && Array.isArray(page.records) ? page.records : []
      const decorated = await Promise.all(records.map(async (item) => ({
        ...this.decorate(item, lookups),
        ...await this.loadNonconformingSummary(item)
      })))
      const pages = Number(page && page.pages) || 0
      this.setData({
        list: reset ? decorated : this.data.list.concat(decorated),
        current: current + 1,
        pages,
        total: Number(page && page.total) || (reset ? decorated.length : this.data.total),
        finished: pages === 0 || current >= pages,
        error: ''
      })
    } catch (error) {
      this.setData({ error: error.message || '质检记录加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  openDetail(event) {
    const inspectionId = event.currentTarget.dataset.id
    if (!inspectionId) return
    wx.navigateTo({
      url: '/pages/quality/pending-detail/index?mode=record&inspectionId=' + encodeURIComponent(inspectionId)
    })
  }
})
