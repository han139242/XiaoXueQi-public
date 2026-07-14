const guard = require('../../../utils/guard')
const auth = require('../../../utils/auth')
const qualityApi = require('../../../api/quality')

const dispositionOptions = [
  { value: 'rework', label: '返工' },
  { value: 'scrap', label: '报废' },
  { value: 'downgrade', label: '降级' },
  { value: 'concession', label: '让步接收' }
]

const defaultForm = {
  ncrId: '',
  ncrNo: '',
  inspectionId: '',
  productLotId: '',
  defectReason: '',
  dispositionType: 'rework',
  remark: '',
  imageUrl: ''
}

Page({
  data: {
    form: { ...defaultForm },
    dispositionOptions,
    dispositionIndex: 0,
    submitting: false,
    loading: false,
    mode: 'create',
    canSave: false
  },

  onLoad(options) {
    if (options && options.ncrId) {
      this.loadDetail(decodeURIComponent(options.ncrId))
      return
    }
    if (options && options.inspectionId) {
      this.setData({ 'form.inspectionId': decodeURIComponent(options.inspectionId) })
    }
  },

  onShow() {
    if (!guard.requireAuth()) return
    const permission = this.data.mode === 'detail'
      ? 'qms:nonconforming-order:update'
      : 'qms:nonconforming-order:create'
    const canSave = auth.hasPermission(permission)
    this.setData({ canSave })
    if (!canSave && this.data.mode !== 'detail') {
      wx.showToast({ title: '当前账号无新增权限', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 300)
    }
  },

  handleInput(event) {
    const field = event.currentTarget.dataset.field
    this.setData({ ['form.' + field]: event.detail.value })
  },

  handleDisposition(event) {
    const index = Number(event.detail.value)
    this.setData({
      dispositionIndex: index,
      'form.dispositionType': dispositionOptions[index].value
    })
  },

  async loadDetail(ncrId) {
    this.setData({ loading: true, mode: 'detail' })
    try {
      const detail = await qualityApi.nonconformingDetail(ncrId)
      const dispositionIndex = Math.max(0, dispositionOptions.findIndex((item) => item.value === detail.dispositionType))
      this.setData({
        form: {
          ncrId: detail.ncrId || '',
          ncrNo: detail.ncrNo || '',
          inspectionId: detail.inspectionId || '',
          productLotId: detail.productLotId || '',
          defectReason: detail.defectName || detail.defectReason || '',
          dispositionType: detail.dispositionType || dispositionOptions[dispositionIndex].value,
          remark: detail.remark || '',
          imageUrl: detail.imageUrl || detail.pictureUrl || detail.photoUrl || ''
        },
        dispositionIndex
      })
    } catch (error) {
      wx.showToast({ title: error.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  resetForm() {
    const inspectionId = this.data.form.inspectionId
    const ncrId = this.data.form.ncrId
    const ncrNo = this.data.form.ncrNo
    this.setData({
      form: { ...defaultForm, inspectionId, ncrId, ncrNo },
      dispositionIndex: 0
    })
  },

  removeDraft() {
    wx.showModal({
      title: '删除草稿',
      content: '确认放弃当前录入内容？',
      success: (result) => {
        if (result.confirm) wx.navigateBack()
      }
    })
  },

  cancel() {
    wx.navigateBack()
  },

  validate() {
    if (!String(this.data.form.inspectionId || '').trim()) return '请输入检验单 ID'
    if (!String(this.data.form.defectReason || '').trim()) return '请输入不良原因'
    return ''
  },

  async submit() {
    const permission = this.data.form.ncrId ? 'qms:nonconforming-order:update' : 'qms:nonconforming-order:create'
    if (!guard.requirePermission(permission)) return
    if (this.data.submitting) return
    const message = this.validate()
    if (message) {
      wx.showToast({ title: message, icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      const form = this.data.form
      const payload = {
        inspectionId: Number(form.inspectionId),
        productLotId: form.productLotId ? Number(form.productLotId) : null,
        defectReason: String(form.defectReason || '').trim(),
        dispositionType: form.dispositionType,
        imageUrl: form.imageUrl || '',
        remark: String(form.remark || '').trim()
      }
      if (form.ncrId) {
        await qualityApi.updateNonconforming(form.ncrId, payload)
      } else {
        await qualityApi.createNonconforming(payload)
      }
      wx.showToast({ title: form.ncrId ? '已保存' : '已录入', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 500)
    } catch (error) {
      wx.showToast({ title: error.message || '提交失败', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  }
})
