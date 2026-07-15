const guard = require('../../../utils/guard')
const auth = require('../../../utils/auth')
const qualityApi = require('../../../api/quality')
const lookupApi = require('../../../api/lookup')
const format = require('../../../utils/format')

const statusMap = {
  WAIT_INSPECTION: ['待检', 'warning'],
  INSPECTING: ['检验中', 'primary'],
  DRAFT: ['待检', 'warning'],
  RUNNING: ['检验中', 'primary'],
  FINISHED: ['已完成', 'success'],
  CLOSED: ['已关闭', 'neutral']
}

const typeMap = {
  first: '首检',
  process: '巡检',
  final: '终检',
  incoming: '来料检验',
  FIRST: '首检',
  PROCESS: '巡检',
  FINAL: '终检',
  INCOMING: '来料检验'
}

const resultMap = {
  PASS: '合格',
  FAIL: '不合格',
  CONCESSION: '让步接收'
}

const dispositionMap = {
  REWORK: '返工',
  SCRAP: '报废',
  DOWNGRADE: '降级',
  CONCESSION: '让步接收'
}

Page({
  data: {
    inspectionId: '',
    detail: null,
    recordMode: false,
    readonly: false,
    canSubmit: false,
    canCreateDefect: false,
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
    this.setData({
      canSubmit: auth.hasPermission('qms:inspection-order:submit'),
      canCreateDefect: auth.hasPermission('qms:nonconforming-order:create')
    })
    if (this.data.inspectionId && !this.data.loading) this.loadDetail()
  },

  onPullDownRefresh() {
    this.loadDetail().finally(() => wx.stopPullDownRefresh())
  },

  async ensureLookups() {
    const lookups = await lookupApi.load([
      'workOrder', 'task', 'report', 'employee', 'batch', 'inspectionScheme'
    ])
    this.setData({ lookups })
    return lookups
  },

  decorate(item, lookups) {
    const statusView = statusMap[item.status] || [item.status || '-', 'neutral']
    const sampleQuantity = Number(item.sampleQuantity || 0)
    const unqualifiedQuantity = Number(item.unqualifiedQuantity || 0)
    return {
      ...item,
      statusText: statusView[0],
      statusTone: statusView[1],
      typeText: typeMap[item.inspectionType] || item.inspectionType || '-',
      resultText: resultMap[item.inspectionResult] || item.inspectionResult || '未判定',
      workOrderLabel: lookupApi.label(lookups.workOrder, item.workOrderId, '工单'),
      taskLabel: lookupApi.label(lookups.task, item.taskId, '任务'),
      reportLabel: lookupApi.label(lookups.report, item.reportId, '报工'),
      batchLabel: lookupApi.label(lookups.batch, item.batchId, '批次'),
      schemeLabel: lookupApi.label(lookups.inspectionScheme, item.schemeId, '方案'),
      inspectorLabel: lookupApi.label(lookups.employee, item.inspectorId, '员工'),
      hasQuantity: sampleQuantity > 0,
      unqualifiedRateText: sampleQuantity > 0 ? (unqualifiedQuantity / sampleQuantity * 100).toFixed(4) + '%' : '-',
      inspectionTimeText: format.formatDateTime(item.inspectionTime || item.updatedTime),
      createdTimeText: format.formatDateTime(item.createdTime),
      updatedTimeText: format.formatDateTime(item.updatedTime),
      remarkText: format.display(item.remark)
    }
  },

  decorateNonconforming(item) {
    return {
      ...item,
      displayNo: item.ncrNo || ('不良品项 ' + item.ncrId),
      defectReasonText: item.defectName || item.defectReason || '-',
      dispositionText: dispositionMap[String(item.dispositionType || '').toUpperCase()] || item.dispositionType || '-'
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
      let nonconformingItems = []
      if (detail.inspectionResult === 'FAIL') {
        try {
          const page = await qualityApi.nonconformingPage({
            current: 1,
            size: 20,
            inspectionId: detail.inspectionId
          })
          const records = page && Array.isArray(page.records) ? page.records : []
          nonconformingItems = records.map((item) => this.decorateNonconforming(item))
        } catch (error) {
          nonconformingItems = []
        }
      }
      this.setData({
        detail: { ...this.decorate(detail, lookups), nonconformingItems },
        readonly: this.data.recordMode || detail.status === 'FINISHED' || detail.status === 'CLOSED',
        error: ''
      })
    } catch (error) {
      this.setData({ error: error.message || '检验单详情加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  openInspectionForm() {
    if (!guard.requirePermission('qms:inspection-order:submit')) return
    if (!this.data.inspectionId) return
    wx.navigateTo({
      url: '/pages/quality/inspection-form/index?inspectionId=' + encodeURIComponent(this.data.inspectionId)
    })
  },

  goBack() {
    wx.navigateBack()
  }
})
