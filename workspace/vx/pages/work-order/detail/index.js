const guard = require('../../../utils/guard')
const auth = require('../../../utils/auth')
const workOrderApi = require('../../../api/work-order')
const taskApi = require('../../../api/task')
const reportApi = require('../../../api/work-report')
const productionPlanApi = require('../../../api/production-plan')
const lookupApi = require('../../../api/lookup')
const statusUtil = require('../../../utils/status')
const format = require('../../../utils/format')

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

function compareTasks(left, right) {
  const leftTime = new Date(left.createdTime || 0).getTime()
  const rightTime = new Date(right.createdTime || 0).getTime()
  if (leftTime !== rightTime) return leftTime - rightTime
  return Number(left.taskId || 0) - Number(right.taskId || 0)
}

Page({
  data: {
    id: null,
    workOrder: null,
    tasks: [],
    history: [],
    lookups: null,
    activeTab: 'progress',
    historyLoading: false,
    historyLoaded: false,
    historyError: '',
    canReport: false,
    canViewHistory: false,
    loading: true,
    error: ''
  },

  onLoad(options) {
    const id = Number(options.id)
    if (!Number.isInteger(id) || id <= 0) {
      this.setData({ loading: false, error: '工单 ID 无效' })
      return
    }
    this.syncPermissions()
    this.setData({ id })
    this.loadData()
  },

  onShow() {
    if (!guard.requireAuth()) return
    this.syncPermissions()
    if (this.data.id && !this.data.loading) this.loadData()
  },

  onPullDownRefresh() {
    this.loadData().finally(() => wx.stopPullDownRefresh())
  },

  syncPermissions() {
    this.setData({
      canReport: auth.hasPermission('mfg:work-report:create') && auth.hasPermission('mfg:work-report:submit'),
      canViewHistory: auth.hasPermission('mfg:work-report:view')
    })
  },

  switchTab(event) {
    const activeTab = event.currentTarget.dataset.tab
    this.setData({ activeTab })
    if (activeTab === 'history') this.loadHistory()
  },

  openTask(event) {
    wx.navigateTo({ url: '/pages/task/detail/index?id=' + encodeURIComponent(event.currentTarget.dataset.id) })
  },

  openReportForm(event) {
    if (!guard.requirePermission('mfg:work-report:create') || !guard.requirePermission('mfg:work-report:submit')) return
    wx.navigateTo({ url: '/pages/report/form/index?taskId=' + encodeURIComponent(event.currentTarget.dataset.id) })
  },

  openReport(event) {
    wx.navigateTo({ url: '/pages/report/detail/index?id=' + encodeURIComponent(event.currentTarget.dataset.id) })
  },

  decorateTasks(records, lookups) {
    const tasks = records.slice().sort(compareTasks).map((item) => {
      const planned = Number(item.plannedQuantity || 0)
      const qualified = Number(item.completedQuantity || 0)
      const defect = Number(item.defectQuantity || 0)
      const progress = planned > 0 ? Math.min(100, qualified / planned * 100) : 0
      return {
        ...item,
        statusView: statusUtil.getStatus('task', item.status),
        operationLabel: lookupApi.label(lookups.operation, item.operationId, '工序'),
        stationLabel: lookupApi.label(lookups.station, item.stationId, '工位'),
        qualifiedQuantity: qualified,
        defectQuantity: defect,
        progressText: progress.toFixed(2) + '%'
      }
    })
    let currentIndex = tasks.findIndex((item) => item.status !== 'FINISHED')
    if (currentIndex < 0) currentIndex = Math.max(0, tasks.length - 1)
    return tasks.map((item, index) => ({
      ...item,
      stageState: item.status === 'FINISHED' ? 'done' : index === currentIndex ? 'active' : 'pending'
    }))
  },

  async fetchHistory(tasks, lookups) {
    if (!this.data.canViewHistory || tasks.length === 0) return []
    const pages = await Promise.all(tasks.map((task) => {
      return reportApi.page({ current: 1, size: 100, taskId: task.taskId })
    }))
    const taskMap = {}
    tasks.forEach((task) => {
      taskMap[String(task.taskId)] = task
    })
    const reports = []
    pages.forEach((page) => {
      if (page && Array.isArray(page.records)) reports.push(...page.records)
    })
    return reports.sort((left, right) => {
      return new Date(right.reportTime || right.createdTime || 0).getTime()
        - new Date(left.reportTime || left.createdTime || 0).getTime()
    }).map((item) => {
      const task = taskMap[String(item.taskId)] || {}
      return {
        ...item,
        operationLabel: task.operationLabel || '未知工序',
        taskLabel: task.taskNo || ('任务 ID ' + item.taskId),
        operatorLabel: lookupApi.label(lookups.employee, item.operatorId, '员工'),
        reportTimeText: format.formatDateTime(item.reportTime || item.createdTime),
        statusView: statusUtil.getStatus('audit', item.auditStatus)
      }
    })
  },

  async loadHistory() {
    if (this.data.historyLoading || this.data.historyLoaded) return
    this.setData({ historyLoading: true, historyError: '' })
    try {
      const history = await this.fetchHistory(this.data.tasks, this.data.lookups)
      this.setData({ history, historyLoaded: true })
    } catch (error) {
      this.setData({ history: [], historyLoaded: true, historyError: error.message || '生产履历加载失败' })
    } finally {
      this.setData({ historyLoading: false })
    }
  },

  retryHistory() {
    this.setData({ historyLoaded: false })
    this.loadHistory()
  },

  async loadData() {
    this.setData({ loading: true, error: '' })
    try {
      const workOrder = await workOrderApi.detail(this.data.id)
      if (!workOrder) {
        this.setData({ workOrder: null, tasks: [], history: [], loading: false })
        return
      }
      const [taskPage, lookups, plan] = await Promise.all([
        taskApi.page({ current: 1, size: 50, workOrderId: this.data.id, sort: 'oldest' }),
        lookupApi.load(['product', 'operation', 'station', 'employee']),
        workOrder.planId ? productionPlanApi.detail(workOrder.planId).catch(() => null) : Promise.resolve(null)
      ])
      const records = taskPage && Array.isArray(taskPage.records) ? taskPage.records : []
      const tasks = this.decorateTasks(records, lookups)
      const product = findItem(lookups.product, workOrder.productId) || {}
      const completedQuantity = Number(workOrder.completedQuantity || 0)
      const defectQuantity = tasks.reduce((sum, item) => sum + Number(item.defectQuantity || 0), 0)
      this.setData({
        workOrder: {
          ...workOrder,
          statusView: statusUtil.getStatus('workOrder', workOrder.status),
          productText: [product.productCode, product.productName].filter(Boolean).join(' | ')
            || lookupApi.label(lookups.product, workOrder.productId, '产品'),
          planTimeText: dateRange(plan && plan.plannedStartTime, plan && plan.plannedEndTime),
          completedQuantity,
          defectQuantity,
          actualQuantity: completedQuantity + defectQuantity
        },
        tasks,
        history: [],
        lookups,
        historyLoaded: false,
        historyError: '',
        loading: false
      })
      if (this.data.activeTab === 'history') this.loadHistory()
    } catch (error) {
      this.setData({ loading: false, error: error.message || '工单详情加载失败' })
    }
  }
})
