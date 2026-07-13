const guard = require('../../../utils/guard')
const taskApi = require('../../../api/task')
const reportApi = require('../../../api/work-report')
const lookupApi = require('../../../api/lookup')
const statusUtil = require('../../../utils/status')
const format = require('../../../utils/format')

Page({
  data: {
    taskId: null,
    task: null,
    operatorId: null,
    stationId: '',
    stationOptions: [{ id: '', label: '不指定工位' }],
    stationIndex: 0,
    qualifiedQuantity: '',
    defectQuantity: '',
    actualWorkHours: '',
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
    const operatorId = guard.requireEmployee()
    this.setData({ taskId, operatorId })
    if (operatorId) this.loadTask()
    else this.setData({ loading: false, error: '当前账号未关联员工档案，请联系管理员' })
  },

  onShow() {
    guard.requireAuth()
  },

  handleInput(event) {
    this.setData({ [event.currentTarget.dataset.field]: event.detail.value, error: '' })
  },

  handleStation(event) {
    const stationIndex = Number(event.detail.value)
    const station = this.data.stationOptions[stationIndex]
    this.setData({ stationIndex, stationId: station.id, error: '' })
  },

  async loadTask() {
    if (!this.data.operatorId) {
      this.setData({ loading: false, task: null, error: '当前账号未关联员工档案，请联系管理员' })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const [task, lookups] = await Promise.all([
        taskApi.detail(this.data.taskId),
        lookupApi.load(['station'])
      ])
      if (!task) {
        this.setData({ task: null, loading: false })
        return
      }
      const stationOptions = [{ id: '', label: '不指定工位' }].concat(lookups.station.items)
      let stationIndex = stationOptions.findIndex((item) => String(item.id) === String(task.stationId))
      if (task.stationId != null && stationIndex < 0) {
        stationOptions.push({ id: task.stationId, label: '工位 ID ' + task.stationId })
        stationIndex = stationOptions.length - 1
      }
      if (stationIndex < 0) stationIndex = 0
      this.setData({
        task: { ...task, statusView: statusUtil.getStatus('task', task.status) },
        stationId: stationOptions[stationIndex].id,
        stationOptions,
        stationIndex,
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false, error: error.message || '任务加载失败' })
    }
  },

  async submit() {
    if (this.data.submitting || !this.data.task) return
    const operatorId = guard.requireEmployee()
    if (!operatorId) return
    const qualifiedQuantity = format.toNumber(this.data.qualifiedQuantity)
    const defectQuantity = format.toNumber(this.data.defectQuantity)
    const actualWorkHours = format.toNumber(this.data.actualWorkHours)
    const stationId = format.toNumber(this.data.stationId)
    if (qualifiedQuantity === null || qualifiedQuantity < 0 || defectQuantity === null || defectQuantity < 0) {
      this.setData({ error: '合格数量和不良数量必须是非负数字' })
      return
    }
    if (qualifiedQuantity === 0 && defectQuantity === 0) {
      this.setData({ error: '合格数量和不良数量不能同时为 0' })
      return
    }
    if (this.data.actualWorkHours !== '' && (actualWorkHours === null || actualWorkHours <= 0)) {
      this.setData({ error: '实际工时必须大于 0' })
      return
    }
    this.setData({ submitting: true, error: '' })
    try {
      const latest = await taskApi.detail(this.data.taskId)
      if (!latest || latest.status !== 'RUNNING') throw new Error('任务状态已变化，只有生产中任务可以报工')
      const payload = {
        taskId: this.data.taskId,
        operatorId,
        qualifiedQuantity,
        defectQuantity,
        remark: this.data.remark.trim(),
        materials: []
      }
      if (stationId !== null) payload.stationId = stationId
      if (actualWorkHours !== null) payload.actualWorkHours = actualWorkHours
      const result = await reportApi.submit(payload)
      if (!result || !result.reportId) throw new Error('报工成功但响应缺少报工 ID')
      wx.showToast({ title: '报工已提交', icon: 'success' })
      wx.redirectTo({ url: '/pages/report/detail/index?id=' + encodeURIComponent(result.reportId) })
    } catch (error) {
      this.setData({ error: error.message || '报工提交失败' })
    } finally {
      this.setData({ submitting: false })
    }
  }
})
