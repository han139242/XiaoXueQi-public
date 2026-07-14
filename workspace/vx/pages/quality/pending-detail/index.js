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
  incoming: '来料检验'
}

const resultMap = {
  PASS: '合格',
  FAIL: '不合格',
  CONCESSION: '让步接收'
}

Page({
  data: {
    inspectionId: '',
    detail: null,
    recordMode: false,
    readonly: false,
    loading: false,
    error: '',
    lookups: null
  },

  onLoad(options) {
    const inspectionId = options && options.inspectionId ? decodeURIComponent(options.inspectionId) : ''
    const recordMode = Boolean(options && options.mode === 'record')
    this.setData({ inspectionId, recordMode })
    wx.setNavigationBarTitle({ title: recordMode ? '质检记录详情' : '待检详情' })
    this.loadDetail()
  },

  onShow() {
    if (!guard.requireAuth()) return
    if (this.data.inspectionId && !this.data.loading) this.loadDetail()
  },

  onPullDownRefresh() {
    this.loadDetail().finally(() => wx.stopPullDownRefresh())
  },

  async ensureLookups() {
    const lookups = await lookupApi.load(['workOrder', 'employee'])
    this.setData({ lookups })
    return lookups
  },

  decorate(item, lookups) {
    const statusView = statusMap[item.status] || [item.status || '-', 'neutral']
    return {
      ...item,
      statusText: statusView[0],
      statusTone: statusView[1],
      typeText: typeMap[item.inspectionType] || item.inspectionType || '-',
      resultText: resultMap[item.inspectionResult] || item.inspectionResult || '未判定',
      workOrderLabel: lookupApi.label(lookups.workOrder, item.workOrderId, '工单'),
      taskLabel: item.taskNo || (item.taskId ? '任务 ID ' + item.taskId : '-'),
      reportLabel: item.reportNo || (item.reportId ? '报工 ID ' + item.reportId : '-'),
      batchLabel: item.batchNo || item.lotNo || (item.batchId ? '批次 ID ' + item.batchId : '-'),
      schemeLabel: item.schemeName || (item.schemeId ? '方案 ID ' + item.schemeId : '-'),
      inspectorLabel: lookupApi.label(lookups.employee, item.inspectorId, '员工'),
      createdTimeText: format.formatDateTime(item.createdTime),
      updatedTimeText: format.formatDateTime(item.updatedTime),
      remarkText: format.display(item.remark)
    }
  },

  async loadDetail() {
    if (this.data.loading) return
    if (!this.data.inspectionId) {
      this.setData({ error: '缺少检验单 ID' })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const lookups = this.data.lookups || await this.ensureLookups()
      const detail = await qualityApi.inspectionDetail(this.data.inspectionId)
      if (!detail) {
        this.setData({ detail: null, error: '检验单不存在或已删除' })
        return
      }
      this.setData({
        detail: this.decorate(detail, lookups),
        readonly: this.data.recordMode || detail.status === 'FINISHED' || detail.status === 'CLOSED',
        error: ''
      })
    } catch (error) {
      this.setData({ error: error.message || '检验单详情加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  openDefectForm() {
    if (!this.data.inspectionId) return
    wx.navigateTo({
      url: '/pages/quality/defect-form/index?inspectionId=' + encodeURIComponent(this.data.inspectionId)
    })
  },

  goBack() {
    wx.navigateBack()
  }
})
