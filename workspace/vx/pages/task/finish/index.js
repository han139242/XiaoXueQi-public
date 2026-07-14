const guard = require('../../../utils/guard')
const taskApi = require('../../../api/task')
const statusUtil = require('../../../utils/status')
const format = require('../../../utils/format')

Page({
  data: {
    taskId: null,
    task: null,
    completedQuantity: '',
    defectQuantity: '',
    remark: '',
    loading: true,
    submitting: false,
    error: ''
  },

  onLoad(options) {
    const taskId = Number(options.taskId)
    if (!Number.isInteger(taskId) || taskId <= 0) {
      this.setData({ loading: false, error: '任务 ID 无效' })
      return
    }
    this.setData({ taskId })
    this.loadTask()
  },

  onShow() {
    guard.requireAuth()
  },

  handleInput(event) {
    this.setData({ [event.currentTarget.dataset.field]: event.detail.value, error: '' })
  },

  async loadTask() {
    this.setData({ loading: true, error: '' })
    try {
      const task = await taskApi.detail(this.data.taskId)
      if (!task) {
        this.setData({ task: null, loading: false })
        return
      }
      this.setData({
        task: { ...task, statusView: statusUtil.getStatus('task', task.status) },
        completedQuantity: format.display(task.completedQuantity, '0'),
        defectQuantity: format.display(task.defectQuantity, '0'),
        remark: task.remark || '',
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false, error: error.message || '任务加载失败' })
    }
  },

  submit() {
    if (!guard.requirePermission('mfg:task:finish')) return
    if (this.data.submitting || !this.data.task) return
    if (!['RUNNING', 'PAUSED'].includes(this.data.task.status)) {
      this.setData({ error: '当前任务状态不允许完成' })
      return
    }
    const completedQuantity = format.toNumber(this.data.completedQuantity)
    const defectQuantity = format.toNumber(this.data.defectQuantity)
    if (completedQuantity === null || completedQuantity < 0 || defectQuantity === null || defectQuantity < 0) {
      this.setData({ error: '完成数量和不良数量必须是非负数字' })
      return
    }
    wx.showModal({
      title: '确认完成任务',
      content: '提交后任务将进入已完工状态，确认继续？',
      success: async (result) => {
        if (!result.confirm) return
        this.setData({ submitting: true, error: '' })
        try {
          const latest = await taskApi.detail(this.data.taskId)
          if (!latest || !['RUNNING', 'PAUSED'].includes(latest.status)) throw new Error('任务状态已变化，请返回刷新')
          await taskApi.finish(this.data.taskId, { completedQuantity, defectQuantity, remark: this.data.remark.trim() })
          await taskApi.detail(this.data.taskId)
          wx.showToast({ title: '任务已完成', icon: 'success' })
          setTimeout(() => wx.navigateBack(), 500)
        } catch (error) {
          this.setData({ error: error.message || '完成任务失败' })
          if (Number(error.code) === 1007) await this.loadTask()
        } finally {
          this.setData({ submitting: false })
        }
      }
    })
  }
})
