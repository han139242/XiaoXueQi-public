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
  const leftMatch = String(left.taskNo || '').match(/-(\d{3})$/)
  const rightMatch = String(right.taskNo || '').match(/-(\d{3})$/)
  const leftSequence = leftMatch ? Number(leftMatch[1]) : Number(left.operationId || Number.MAX_SAFE_INTEGER)
  const rightSequence = rightMatch ? Number(rightMatch[1]) : Number(right.operationId || Number.MAX_SAFE_INTEGER)
  if (leftSequence !== rightSequence) return leftSequence - rightSequence
  const leftTime = new Date(left.createdTime || 0).getTime()
  const rightTime = new Date(right.createdTime || 0).getTime()
  if (leftTime !== rightTime) return leftTime - rightTime
  return Number(left.taskId || 0) - Number(right.taskId || 0)
}

function progressTaskScore(task) {
  const statusScore = { RUNNING: 50, PAUSED: 45, FINISHED: 40, WAIT_START: 30, CLOSED: 10 }
  const normalizedNumberBonus = /-\d{3}$/.test(String(task.taskNo || '')) ? 5 : 0
  return (statusScore[task.status] || 0) + normalizedNumberBonus
}

function selectProgressTasks(records) {
  const selected = {}
  records.forEach((task) => {
    const key = task.operationId == null ? 'task-' + task.taskId : 'operation-' + task.operationId
    const current = selected[key]
    if (!current || progressTaskScore(task) > progressTaskScore(current)
      || (progressTaskScore(task) === progressTaskScore(current) && Number(task.taskId) < Number(current.taskId))) {
      selected[key] = task
    }
  })
  return Object.keys(selected).map((key) => selected[key])
}

Page({
  data: {
    id: null,
    workOrder: null,
    tasks: [],
    activeStageAnchor: '',
    history: [],
    lookups: null,
    activeTab: 'progress',
    historyLoading: false,
    historyLoaded: false,
    historyError: '',
    canStartWorkOrder: false,
    startingWorkOrder: false,
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
      canStartWorkOrder: auth.hasPermission('mfg:work-order:start'),
      canReport: auth.hasPermission('mfg:work-report:create') && auth.hasPermission('mfg:work-report:submit'),
      canViewHistory: auth.hasPermission('mfg:work-report:view')
    })
  },

  async startWorkOrder() {
    if (!guard.requirePermission('mfg:work-order:start')) return
    if (this.data.startingWorkOrder || !this.data.workOrder || this.data.workOrder.status !== 'RELEASED') return
    const confirmed = await new Promise((resolve) => {
      wx.showModal({
        title: '开始生产',
        content: '确认将工单 ' + this.data.workOrder.workOrderNo + ' 更改为生产中？',
        success: (result) => resolve(result.confirm),
        fail: () => resolve(false)
      })
    })
    if (!confirmed) return
    this.setData({ startingWorkOrder: true })
    try {
      await workOrderApi.start(this.data.id)
      wx.showToast({ title: '工单已开始生产', icon: 'success' })
      await this.loadData()
    } finally {
      this.setData({ startingWorkOrder: false })
    }
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
    const taskId = Number(event.currentTarget.dataset.id)
    const task = this.data.tasks.find((item) => Number(item.taskId) === taskId)
    if (!task || task.assignedToCurrentUser !== true || task.status !== 'RUNNING' || task.reportingComplete) return
    wx.navigateTo({ url: '/pages/report/form/index?taskId=' + encodeURIComponent(taskId) })
  },

  openReport(event) {
    wx.navigateTo({ url: '/pages/report/detail/index?id=' + encodeURIComponent(event.currentTarget.dataset.id) })
  },

  decorateTasks(records, lookups) {
    const tasks = selectProgressTasks(records).sort(compareTasks).map((item) => {
      const planned = Number(item.plannedQuantity || 0)
      const qualified = Math.max(Number(item.completedQuantity || 0), Number(item.reportedQualifiedQuantity || 0))
      const defect = Math.max(Number(item.defectQuantity || 0), Number(item.reportedDefectQuantity || 0))
      const reported = qualified + defect
      const progress = item.status === 'FINISHED'
        ? 100
        : planned > 0 ? Math.min(100, reported / planned * 100) : 0
      return {
        ...item,
        statusView: statusUtil.getStatus('task', item.status),
        operationLabel: lookupApi.label(lookups.operation, item.operationId, '工序'),
        stationLabel: lookupApi.label(lookups.station, item.stationId, '工位'),
        qualifiedQuantity: qualified,
        defectQuantity: defect,
        reportingComplete: planned > 0 && reported >= planned,
        progressText: progress.toFixed(2) + '%'
      }
    })
    return tasks.map((item) => ({
      ...item,
      stageState: item.status === 'FINISHED' || item.reportingComplete
        ? 'done'
        : item.status === 'RUNNING' ? 'active' : 'pending'
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
        taskApi.pageForView({ current: 1, size: 50, workOrderId: this.data.id, sort: 'oldest' }),
        lookupApi.load(['product', 'operation', 'station', 'employee']),
        workOrder.planId ? productionPlanApi.detail(workOrder.planId).catch(() => null) : Promise.resolve(null)
      ])
      const records = taskPage && Array.isArray(taskPage.records) ? taskPage.records : []
      const tasks = this.decorateTasks(records, lookups)
      const product = findItem(lookups.product, workOrder.productId) || {}
      const activeTask = tasks.find((item) => item.stageState === 'active')
      const finalTask = tasks.length ? tasks[tasks.length - 1] : null
      const reportedCompleted = workOrder.status === 'RUNNING' && finalTask
        ? Number(finalTask.qualifiedQuantity || 0)
        : 0
      const completedQuantity = Math.max(Number(workOrder.completedQuantity || 0), reportedCompleted)
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
        activeStageAnchor: activeTask ? 'stage-task-' + activeTask.taskId : '',
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
