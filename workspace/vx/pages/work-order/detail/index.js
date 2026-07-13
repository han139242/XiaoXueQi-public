const guard = require('../../../utils/guard')
const workOrderApi = require('../../../api/work-order')
const taskApi = require('../../../api/task')
const lookupApi = require('../../../api/lookup')
const statusUtil = require('../../../utils/status')
const format = require('../../../utils/format')

Page({
  data: {
    id: null,
    workOrder: null,
    tasks: [],
    loading: true,
    error: ''
  },

  onLoad(options) {
    const id = Number(options.id)
    if (!Number.isInteger(id) || id <= 0) {
      this.setData({ loading: false, error: '工单 ID 无效' })
      return
    }
    this.setData({ id })
    this.loadData()
  },

  onShow() {
    guard.requireAuth()
  },

  openTask(event) {
    wx.navigateTo({ url: '/pages/task/detail/index?id=' + encodeURIComponent(event.currentTarget.dataset.id) })
  },

  openReportForm(event) {
    wx.navigateTo({ url: '/pages/report/form/index?taskId=' + encodeURIComponent(event.currentTarget.dataset.id) })
  },

  async loadData() {
    this.setData({ loading: true, error: '' })
    try {
      const [workOrder, taskPage, lookups] = await Promise.all([
        workOrderApi.detail(this.data.id),
        taskApi.page({ current: 1, size: 50, workOrderId: this.data.id }),
        lookupApi.load(['product', 'operation', 'station'])
      ])
      if (!workOrder) {
        this.setData({ workOrder: null, tasks: [], loading: false })
        return
      }
      const tasks = taskPage && Array.isArray(taskPage.records) ? taskPage.records : []
      this.setData({
        workOrder: {
          ...workOrder,
          statusView: statusUtil.getStatus('workOrder', workOrder.status),
          productLabel: lookupApi.label(lookups.product, workOrder.productId, '产品'),
          createdTimeText: format.formatDateTime(workOrder.createdTime),
          updatedTimeText: format.formatDateTime(workOrder.updatedTime)
        },
        tasks: tasks.map((item) => ({
          ...item,
          statusView: statusUtil.getStatus('task', item.status),
          operationLabel: lookupApi.label(lookups.operation, item.operationId, '工序'),
          stationLabel: lookupApi.label(lookups.station, item.stationId, '工位')
        })),
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false, error: error.message || '工单详情加载失败' })
    }
  }
})
