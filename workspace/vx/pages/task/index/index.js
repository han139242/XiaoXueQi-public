const guard = require('../../../utils/guard')
const auth = require('../../../utils/auth')
const taskApi = require('../../../api/task')
const productionPlanApi = require('../../../api/production-plan')
const lookupApi = require('../../../api/lookup')
const statusUtil = require('../../../utils/status')
const format = require('../../../utils/format')

const sortOptions = [
  { value: '', label: '默认排序' },
  { value: 'newest', label: '最新创建' },
  { value: 'oldest', label: '最早创建' }
]

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'WAIT_START', label: '待开工' },
  { value: 'RUNNING', label: '生产中' },
  { value: 'FINISHED', label: '已完工' }
]

function findItem(lookup, id) {
  return lookup && Array.isArray(lookup.items)
    ? lookup.items.find((item) => String(item.id) === String(id))
    : null
}

function dateText(value) {
  return value ? String(value).slice(0, 10) : '-'
}

function dateRange(start, end) {
  const startText = dateText(start)
  const endText = dateText(end || start)
  return startText === '-' ? '-' : startText + '~' + endText
}

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
    filters: { employeeId: '', operationId: '', status: '', sort: '' },
    statusOptions,
    statusIndex: 0,
    employeeOptions: [{ id: '', label: '全部人员' }],
    employeeIndex: 0,
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
    if (!guard.requireAuth()) return
    const tabBar = typeof this.getTabBar === 'function' ? this.getTabBar() : null
    if (tabBar) tabBar.setData({ currentPath: '/pages/task/index/index' })
    if (this.data.list.length && !this.data.loading) this.loadPage(true)
  },

  onPullDownRefresh() {
    this.loadPage(true).finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    this.loadPage(false)
  },

  toggleDropdown(event) {
    const field = event.currentTarget.dataset.field
    if (this.data.activeDropdown === field) {
      this.closeDropdown()
      return
    }
    this.setData({ activeDropdown: field, dropdownOptions: this.buildDropdownOptions(field) })
  },

  closeDropdown() {
    this.setData({ activeDropdown: '', dropdownOptions: [] })
  },

  buildDropdownOptions(field) {
    const indexMap = { status: 'statusIndex', employee: 'employeeIndex', sort: 'sortIndex', operation: 'operationIndex' }
    const optionsMap = { status: this.data.statusOptions, employee: this.data.employeeOptions, sort: this.data.sortOptions, operation: this.data.operationOptions }
    const selectedIndex = this.data[indexMap[field]]
    return (optionsMap[field] || []).map((item, index) => ({ ...item, index, selected: index === selectedIndex }))
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
    if (field === 'employee') {
      const option = this.data.employeeOptions[index]
      this.setData({ employeeIndex: index, 'filters.employeeId': option.id })
      this.loadPage(true)
      return
    }
    if (field === 'sort') {
      this.setData({ sortIndex: index, 'filters.sort': sortOptions[index].value })
      this.loadPage(true)
      return
    }
    if (field === 'operation') {
      const option = this.data.operationOptions[index]
      this.setData({ operationIndex: index, 'filters.operationId': option.id })
      this.loadPage(true)
    }
  },

  openDetail(event) {
    wx.navigateTo({ url: '/pages/task/detail/index?id=' + encodeURIComponent(event.currentTarget.dataset.id) })
  },

  async ensureLookups() {
    const [lookups, planPage] = await Promise.all([
      lookupApi.load(['workOrder', 'operation', 'station', 'employee', 'product']),
      productionPlanApi.page({ current: 1, size: 500 }).catch(() => ({ records: [] }))
    ])
    const plans = planPage && Array.isArray(planPage.records) ? planPage.records : []
    lookups.productionPlan = { items: plans.map((item) => ({ id: item.planId, ...item })) }
    const user = auth.getUser() || {}
    const employeeOptions = [{ id: '', label: '全部人员' }]
    if (user.employeeId) employeeOptions.push({ id: user.employeeId, label: '我的任务' })
    lookups.employee.items.forEach((item) => {
      if (String(item.id) !== String(user.employeeId || '')) employeeOptions.push(item)
    })
    this.setData({
      lookups,
      employeeOptions,
      operationOptions: [{ id: '', label: '全部工序' }].concat(lookups.operation.items)
    })
    return lookups
  },

  decorateTask(item, lookups) {
    const workOrder = findItem(lookups.workOrder, item.workOrderId) || {}
    const product = findItem(lookups.product, workOrder.productId) || {}
    const plan = findItem(lookups.productionPlan, workOrder.planId) || {}
    return {
      ...item,
      statusView: statusUtil.getStatus('task', item.status),
      workOrderLabel: workOrder.label || ('工单 ID ' + item.workOrderId),
      operationLabel: lookupApi.label(lookups.operation, item.operationId, '工序'),
      stationLabel: lookupApi.label(lookups.station, item.stationId, '工位'),
      productCode: product.productCode || '-',
      productName: product.productName || '-',
      planTimeText: dateRange(plan.plannedStartTime, plan.plannedEndTime),
      createdTimeText: format.formatDateTime(item.createdTime)
    }
  },

  async loadPage(reset) {
    if (this.data.loading || (!reset && this.data.finished)) return
    const current = reset ? 1 : this.data.current
    this.setData({ loading: true, error: reset ? '' : this.data.error })
    try {
      const lookups = this.data.lookups || await this.ensureLookups()
      const page = await taskApi.page(format.compactQuery({ current, size: this.data.size, ...this.data.filters }))
      const records = page && Array.isArray(page.records) ? page.records : []
      const decorated = records.map((item) => this.decorateTask(item, lookups))
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
