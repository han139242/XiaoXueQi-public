const guard = require('../../../utils/guard')
const auth = require('../../../utils/auth')
const taskApi = require('../../../api/task')
const workOrderApi = require('../../../api/work-order')
const productionPlanApi = require('../../../api/production-plan')
const reportApi = require('../../../api/work-report')
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
  return Number(left.taskId || 0) - Number(right.taskId || 0)
}

function progressTaskScore(task) {
  const statusScore = { RUNNING: 50, PAUSED: 45, FINISHED: 40, WAIT_START: 30, CLOSED: 10 }
  const normalizedNumberBonus = /-\d{3}$/.test(String(task.taskNo || '')) ? 5 : 0
  return (statusScore[task.status] || 0) + normalizedNumberBonus
}

function selectProgressTasks(records) {
  // 一个工单理论上对应七道工序任务。
  // 如果历史数据里同一道工序出现重复任务，这里优先选生产中/已完成的有效任务展示进度。
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
    task: null,
    workOrder: null,
    product: null,
    progressTasks: [],
    reports: [],
    showDetails: false,
    loading: true,
    error: '',
    submitting: false,
    canStart: false,
    canReport: false,
    canFinish: false,
    isSuperAdmin: false
  },

  onLoad(options) {
    const id = Number(options.id)
    if (!Number.isInteger(id) || id <= 0) {
      this.setData({ loading: false, error: '任务 ID 无效' })
      return
    }
    this.setData({ id })
    this.loadData()
  },

  onShow() {
    if (!guard.requireAuth()) return
    this.setData({
      isSuperAdmin: auth.isSuperAdmin(),
      canStart: auth.hasPermission('mfg:task:start'),
      canReport: auth.hasPermission('mfg:work-report:create') && auth.hasPermission('mfg:work-report:submit'),
      canFinish: auth.hasPermission('mfg:task:finish')
    })
    if (this.data.id && !this.data.loading) this.loadData()
  },

  onPullDownRefresh() {
    this.loadData().finally(() => wx.stopPullDownRefresh())
  },

  copyTaskNo() {
    // 复制任务编号后，可在报工记录列表按任务编号/任务 ID 追溯对应报工。
    const taskNo = this.data.task && this.data.task.taskNo
    if (!taskNo) {
      wx.showToast({ title: '暂无任务编号', icon: 'none' })
      return
    }
    wx.setClipboardData({
      data: String(taskNo),
      success: () => wx.showToast({ title: '任务编号已复制', icon: 'success' }),
      fail: () => wx.showToast({ title: '复制失败，请重试', icon: 'none' })
    })
  },

  async startTask(event) {
    if (!guard.requirePermission('mfg:task:start')) return
    const taskId = Number(event.currentTarget.dataset.id || this.data.id)
    const target = this.data.progressTasks.find((item) => Number(item.taskId) === taskId) || this.data.task
    if (this.data.submitting || !target || (!auth.isSuperAdmin() && target.assignedToCurrentUser !== true)
      || target.status !== 'WAIT_START') return
    const confirmed = await new Promise((resolve) => {
      wx.showModal({
        title: '开始任务',
        content: [target.taskNo, target.operationLabel, target.stationLabel].filter(Boolean).join('\n'),
        success: (result) => resolve(result.confirm),
        fail: () => resolve(false)
      })
    })
    if (!confirmed) return
    this.setData({ submitting: true })
    try {
      // 开工会调用后端状态流转接口：WAIT_START -> RUNNING。
      // 后端还会校验父工单必须已经生产中。
      await taskApi.start(taskId, target.stationId ? { stationId: target.stationId } : {})
      wx.showToast({ title: '任务已开始', icon: 'success' })
      await this.loadData()
    } catch (error) {
      if (Number(error.code) === 1007) await this.loadData()
    } finally {
      this.setData({ submitting: false })
    }
  },

  openReportForm(event) {
    if (!guard.requirePermission('mfg:work-report:create') || !guard.requirePermission('mfg:work-report:submit')) return
    const taskId = Number(event.currentTarget.dataset.id || this.data.id)
    const target = this.data.progressTasks.find((item) => Number(item.taskId) === taskId) || this.data.task
    // 只有“派给当前用户 + 生产中 + 未达标”的任务才允许进入报工。
    if (!target || (!auth.isSuperAdmin() && target.assignedToCurrentUser !== true)
      || target.status !== 'RUNNING' || target.reportingComplete) return
    wx.navigateTo({ url: '/pages/report/form/index?taskId=' + encodeURIComponent(taskId) })
  },

  openFinish(event) {
    if (!guard.requirePermission('mfg:task:finish')) return
    const taskId = Number(event.currentTarget.dataset.id || this.data.id)
    const target = this.data.progressTasks.find((item) => Number(item.taskId) === taskId) || this.data.task
    if (!target || (!auth.isSuperAdmin() && target.assignedToCurrentUser !== true)
      || !['RUNNING', 'PAUSED'].includes(target.status)) return
    wx.navigateTo({ url: '/pages/task/finish/index?taskId=' + encodeURIComponent(taskId) })
  },

  openProgressDetail(event) {
    const taskId = Number(event.currentTarget.dataset.id)
    if (taskId === Number(this.data.id)) {
      this.setData({ showDetails: !this.data.showDetails })
      return
    }
    wx.navigateTo({ url: '/pages/task/detail/index?id=' + encodeURIComponent(taskId) })
  },

  openReport(event) {
    wx.navigateTo({ url: '/pages/report/detail/index?id=' + encodeURIComponent(event.currentTarget.dataset.id) })
  },

  decorateTask(item, lookups, isSuperAdmin) {
    // 把后端原始任务记录转换成页面可直接展示的数据：
    // 状态文案、工序/工位名称、累计数量、是否达标等。
    const completed = Math.max(Number(item.completedQuantity || 0), Number(item.reportedQualifiedQuantity || 0))
    const defects = Math.max(Number(item.defectQuantity || 0), Number(item.reportedDefectQuantity || 0))
    const planned = Number(item.plannedQuantity || 0)
    return {
      ...item,
      statusView: statusUtil.getStatus('task', item.status),
      operationLabel: lookupApi.label(lookups.operation, item.operationId, '工序'),
      stationLabel: lookupApi.label(lookups.station, item.stationId, '工位'),
      qualifiedQuantity: completed,
      defectQuantity: defects,
      canOperate: isSuperAdmin || item.assignedToCurrentUser === true,
      finishable: item.status === 'RUNNING' || item.status === 'PAUSED',
      reportingComplete: planned > 0 && completed + defects >= planned,
      actualQuantity: completed + defects,
      remark: ''
    }
  },

  async loadData() {
    this.setData({ loading: true, error: '' })
    try {
      const task = await taskApi.detailForView(this.data.id)
      if (!task) {
        this.setData({ task: null, progressTasks: [], reports: [], loading: false })
        return
      }
      // 任务详情页同时加载：当前任务、所属工单、同工单七道工序、当前任务报工记录和字典。
      // Promise.all 用于并发请求，减少手机端等待时间。
      const [workOrder, taskPage, reportPage, lookups, planPage] = await Promise.all([
        workOrderApi.detail(task.workOrderId),
        taskApi.pageForView({ current: 1, size: 100, workOrderId: task.workOrderId }),
        reportApi.page({ current: 1, size: 20, taskId: this.data.id }),
        lookupApi.load(['operation', 'station', 'employee', 'product']),
        productionPlanApi.page({ current: 1, size: 500 })
      ])
      const product = workOrder ? findItem(lookups.product, workOrder.productId) : null
      const plans = planPage && Array.isArray(planPage.records) ? planPage.records : []
      const plan = workOrder ? plans.find((item) => String(item.planId) === String(workOrder.planId)) : null
      const reports = reportPage && Array.isArray(reportPage.records) ? reportPage.records : []
      const relatedTasks = selectProgressTasks(
        taskPage && Array.isArray(taskPage.records) ? taskPage.records : [task]
      )
      const isSuperAdmin = auth.isSuperAdmin()
      const decoratedTask = this.decorateTask(task, lookups, isSuperAdmin)
      this.setData({
        task: {
          ...decoratedTask,
          workOrderLabel: workOrder && workOrder.workOrderNo ? workOrder.workOrderNo : '工单 ID ' + task.workOrderId,
          planTimeText: dateRange(plan && plan.plannedStartTime, plan && plan.plannedEndTime),
          createdTimeText: format.formatDateTime(task.createdTime),
          updatedTimeText: format.formatDateTime(task.updatedTime)
        },
        workOrder,
        product: product || { productCode: '-', productName: '-' },
        progressTasks: relatedTasks.slice().sort(compareTasks).map((item) => this.decorateTask(item, lookups, isSuperAdmin)),
        reports: reports.map((item) => ({
          ...item,
          statusView: statusUtil.getStatus('audit', item.auditStatus),
          operatorLabel: lookupApi.label(lookups.employee, item.operatorId, '员工'),
          reportTimeText: format.formatDateTime(item.reportTime)
        })),
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false, error: error.message || '任务详情加载失败' })
    }
  }
})
