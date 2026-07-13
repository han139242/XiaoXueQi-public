const guard = require('../../../utils/guard')
const taskApi = require('../../../api/task')
const reportApi = require('../../../api/work-report')
const lookupApi = require('../../../api/lookup')
const statusUtil = require('../../../utils/status')
const format = require('../../../utils/format')

Page({
  data: {
    id: null,
    task: null,
    reports: [],
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

  startTask() {
    if (this.data.submitting || !this.data.task || this.data.task.status !== 'WAIT_START') return
    wx.showModal({
      title: '开始任务',
      content: '确认开始当前生产任务？',
      success: async (result) => {
        if (!result.confirm) return
        this.setData({ submitting: true })
        try {
          await taskApi.start(this.data.id, this.data.task.stationId ? { stationId: this.data.task.stationId } : {})
          wx.showToast({ title: '任务已开始', icon: 'success' })
          await this.loadData()
        } catch (error) {
          if (Number(error.code) === 1007) await this.loadData()
        } finally {
          this.setData({ submitting: false })
        }
      }
    })
  },

  openReportForm() {
    if (!this.data.task || this.data.task.status !== 'RUNNING') return
    wx.navigateTo({ url: '/pages/report/form/index?taskId=' + encodeURIComponent(this.data.id) })
  },

  openFinish() {
    if (!this.data.task || !['RUNNING', 'PAUSED'].includes(this.data.task.status)) return
    wx.navigateTo({ url: '/pages/task/finish/index?taskId=' + encodeURIComponent(this.data.id) })
  },

  openReport(event) {
    wx.navigateTo({ url: '/pages/report/detail/index?id=' + encodeURIComponent(event.currentTarget.dataset.id) })
  },

  async loadData() {
    this.setData({ loading: true, error: '' })
    try {
      const [task, reportPage, lookups] = await Promise.all([
        taskApi.detail(this.data.id),
        reportApi.page({ current: 1, size: 20, taskId: this.data.id }),
        lookupApi.load(['workOrder', 'operation', 'station', 'employee'])
      ])
      if (!task) {
        this.setData({ task: null, reports: [], loading: false })
        return
      }
      const reports = reportPage && Array.isArray(reportPage.records) ? reportPage.records : []
      this.setData({
        task: {
          ...task,
          statusView: statusUtil.getStatus('task', task.status),
          workOrderLabel: lookupApi.label(lookups.workOrder, task.workOrderId, '工单'),
          operationLabel: lookupApi.label(lookups.operation, task.operationId, '工序'),
          stationLabel: lookupApi.label(lookups.station, task.stationId, '工位'),
          createdTimeText: format.formatDateTime(task.createdTime),
          updatedTimeText: format.formatDateTime(task.updatedTime)
        },
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
