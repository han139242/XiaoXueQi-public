const guard = require('../../../utils/guard')
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
    submitting: false
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
    if (this.data.id && !this.data.loading) this.loadData()
  },

  onPullDownRefresh() {
    this.loadData().finally(() => wx.stopPullDownRefresh())
  },

  async startTask(event) {
    const taskId = Number(event.currentTarget.dataset.id || this.data.id)
    const target = this.data.progressTasks.find((item) => Number(item.taskId) === taskId) || this.data.task
    if (this.data.submitting || !target || target.status !== 'WAIT_START') return
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
    const taskId = Number(event.currentTarget.dataset.id || this.data.id)
    const target = this.data.progressTasks.find((item) => Number(item.taskId) === taskId) || this.data.task
    if (!target || target.status !== 'RUNNING') return
    wx.navigateTo({ url: '/pages/report/form/index?taskId=' + encodeURIComponent(taskId) })
  },

  openFinish(event) {
    const taskId = Number(event.currentTarget.dataset.id || this.data.id)
    const target = this.data.progressTasks.find((item) => Number(item.taskId) === taskId) || this.data.task
    if (!target || !['RUNNING', 'PAUSED'].includes(target.status)) return
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

  decorateTask(item, lookups) {
    const completed = Number(item.completedQuantity || 0)
    const defects = Number(item.defectQuantity || 0)
    return {
      ...item,
      statusView: statusUtil.getStatus('task', item.status),
      operationLabel: lookupApi.label(lookups.operation, item.operationId, '工序'),
      stationLabel: lookupApi.label(lookups.station, item.stationId, '工位'),
      qualifiedQuantity: completed,
      actualQuantity: completed + defects
    }
  },

  async loadData() {
    this.setData({ loading: true, error: '' })
    try {
      const task = await taskApi.detail(this.data.id)
      if (!task) {
        this.setData({ task: null, progressTasks: [], reports: [], loading: false })
        return
      }
      const [workOrder, taskPage, reportPage, lookups, planPage] = await Promise.all([
        workOrderApi.detail(task.workOrderId),
        taskApi.page({ current: 1, size: 100, workOrderId: task.workOrderId }),
        reportApi.page({ current: 1, size: 20, taskId: this.data.id }),
        lookupApi.load(['operation', 'station', 'employee', 'product']),
        productionPlanApi.page({ current: 1, size: 500 })
      ])
      const product = workOrder ? findItem(lookups.product, workOrder.productId) : null
      const plans = planPage && Array.isArray(planPage.records) ? planPage.records : []
      const plan = workOrder ? plans.find((item) => String(item.planId) === String(workOrder.planId)) : null
      const reports = reportPage && Array.isArray(reportPage.records) ? reportPage.records : []
      const relatedTasks = taskPage && Array.isArray(taskPage.records) ? taskPage.records : [task]
      const decoratedTask = this.decorateTask(task, lookups)
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
        progressTasks: relatedTasks.map((item) => this.decorateTask(item, lookups)),
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
