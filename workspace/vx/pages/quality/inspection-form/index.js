const guard = require('../../../utils/guard')
const auth = require('../../../utils/auth')
const qualityApi = require('../../../api/quality')
const reportApi = require('../../../api/work-report')
const lookupApi = require('../../../api/lookup')
const format = require('../../../utils/format')

const UNQUALIFIED_RATE_LIMIT = 0.005

function currentTimeText() {
  const now = new Date()
  const pad = (value) => value < 10 ? '0' + value : String(value)
  return now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate())
    + ' ' + pad(now.getHours()) + ':' + pad(now.getMinutes()) + ':' + pad(now.getSeconds())
}

function calculateResult(sampleQuantity, unqualifiedQuantity) {
  const sample = format.toNumber(sampleQuantity)
  const unqualified = format.toNumber(unqualifiedQuantity)
  if (sample === null || sample <= 0 || unqualified === null || unqualified < 0) {
    return {
      unqualifiedRate: null,
      unqualifiedRateText: '-',
      inspectionResult: '',
      inspectionResultText: '待计算',
      inspectionResultTone: 'neutral'
    }
  }
  const unqualifiedRate = unqualified / sample
  const passed = unqualifiedRate <= UNQUALIFIED_RATE_LIMIT
  return {
    unqualifiedRate,
    unqualifiedRateText: (unqualifiedRate * 100).toFixed(4) + '%',
    inspectionResult: passed ? 'PASS' : 'FAIL',
    inspectionResultText: passed ? '合格' : '不合格',
    inspectionResultTone: passed ? 'success' : 'danger'
  }
}

Page({
  data: {
    inspectionId: '',
    detail: null,
    report: null,
    sampleQuantity: '',
    qualifiedQuantity: '',
    unqualifiedQuantity: '',
    unqualifiedRate: null,
    unqualifiedRateText: '-',
    inspectionResult: '',
    inspectionResultText: '待计算',
    inspectionResultTone: 'neutral',
    inspectionTimeText: '',
    remark: '',
    loading: true,
    submitting: false,
    error: ''
  },

  onLoad(options) {
    const inspectionId = options && options.inspectionId ? decodeURIComponent(options.inspectionId) : ''
    this.setData({ inspectionId, inspectionTimeText: currentTimeText() })
    this.loadData()
  },

  onShow() {
    guard.requireAuth()
  },

  handleInput(event) {
    const field = event.currentTarget.dataset.field
    const value = event.detail.value
    const nextData = { [field]: value, error: '' }
    if (field === 'sampleQuantity' || field === 'unqualifiedQuantity') {
      Object.assign(nextData, calculateResult(
        field === 'sampleQuantity' ? value : this.data.sampleQuantity,
        field === 'unqualifiedQuantity' ? value : this.data.unqualifiedQuantity
      ))
    }
    this.setData(nextData)
  },

  async loadData() {
    if (!this.data.inspectionId) {
      this.setData({ loading: false, error: '缺少检验单 ID' })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const [detail, lookups] = await Promise.all([
        qualityApi.inspectionDetail(this.data.inspectionId),
        lookupApi.load(['task', 'workOrder'])
      ])
      if (!detail) {
        this.setData({ loading: false, error: '检验单不存在或已删除' })
        return
      }
      if (detail.status === 'FINISHED' || detail.status === 'CLOSED') {
        this.setData({ loading: false, error: '当前检验单已完成，不能重复录入' })
        return
      }
      let report = null
      if (detail.reportId) {
        try {
          report = await reportApi.detail(detail.reportId)
        } catch (error) {
          report = null
        }
      }
      this.setData({
        detail: {
          ...detail,
          taskLabel: detail.taskNo || lookupApi.label(lookups.task, detail.taskId, '任务'),
          workOrderLabel: lookupApi.label(lookups.workOrder, detail.workOrderId, '工单'),
          reportLabel: detail.reportNo || (detail.reportId ? '报工 ID ' + detail.reportId : '-'),
          availableQuantity: report
            ? Number(report.qualifiedQuantity || 0) + Number(report.defectQuantity || 0)
            : null
        },
        report,
        remark: detail.remark || '',
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false, error: error.message || '检验单加载失败' })
    }
  },

  validate() {
    const sampleQuantity = format.toNumber(this.data.sampleQuantity)
    const qualifiedQuantity = format.toNumber(this.data.qualifiedQuantity)
    const unqualifiedQuantity = format.toNumber(this.data.unqualifiedQuantity)
    const calculated = calculateResult(sampleQuantity, unqualifiedQuantity)
    if (sampleQuantity === null || sampleQuantity <= 0) return { error: '抽检数量必须大于 0' }
    if (qualifiedQuantity === null || qualifiedQuantity < 0 || unqualifiedQuantity === null || unqualifiedQuantity < 0) {
      return { error: '合格数量和不合格数量必须是非负数字' }
    }
    if (Math.abs(qualifiedQuantity + unqualifiedQuantity - sampleQuantity) > 0.000001) {
      return { error: '合格数量与不合格数量之和必须等于抽检数量' }
    }
    if (this.data.detail.availableQuantity !== null && sampleQuantity > this.data.detail.availableQuantity) {
      return { error: '抽检数量不能超过关联报工数量 ' + this.data.detail.availableQuantity }
    }
    return {
      sampleQuantity,
      qualifiedQuantity,
      unqualifiedQuantity,
      inspectionResult: calculated.inspectionResult
    }
  },

  submit() {
    if (this.data.submitting || !this.data.detail) return
    const values = this.validate()
    if (values.error) {
      this.setData({ error: values.error })
      return
    }
    wx.showModal({
      title: '提交检验记录',
      content: '抽检 ' + values.sampleQuantity + '，合格 ' + values.qualifiedQuantity + '，不合格 ' + values.unqualifiedQuantity + '，确认提交？',
      success: async (result) => {
        if (!result.confirm) return
        const operatorId = guard.requireEmployee()
        if (!operatorId) return
        this.setData({ submitting: true, error: '' })
        try {
          const latest = await qualityApi.inspectionDetail(this.data.inspectionId)
          if (!latest || latest.status === 'FINISHED' || latest.status === 'CLOSED') {
            throw new Error('检验单状态已变化，请返回刷新')
          }
          await qualityApi.submitInspection(this.data.inspectionId, {
            ...values,
            operatorId,
            remark: this.data.remark.trim()
          })
          wx.showToast({ title: '检验记录已提交', icon: 'success' })
          if (values.inspectionResult === 'FAIL'
            && auth.hasPermission('qms:nonconforming-order:generate')) {
            wx.redirectTo({
              url: '/pages/quality/defect-form/index?inspectionId=' + encodeURIComponent(this.data.inspectionId)
            })
          } else {
            this.openRecordDetail()
          }
        } catch (error) {
          this.setData({ error: error.message || '检验记录提交失败' })
        } finally {
          this.setData({ submitting: false })
        }
      }
    })
  },

  openRecordDetail() {
    wx.redirectTo({
      url: '/pages/quality/pending-detail/index?mode=record&inspectionId=' + encodeURIComponent(this.data.inspectionId)
    })
  }
})
