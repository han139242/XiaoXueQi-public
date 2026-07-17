// 不良品登记页：录入不良原因、处理方式，并支持拍照上传。
const guard = require('../../../utils/guard')
const auth = require('../../../utils/auth')
const qualityApi = require('../../../api/quality')
const fileApi = require('../../../api/file')
const format = require('../../../utils/format')

const PHOTO_BIZ_TYPE = 'QMS_NONCONFORMING_ORDER'

function currentTimeText() {
  const date = new Date()
  const pad = (value) => value < 10 ? '0' + value : String(value)
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate())
    + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes()) + ':' + pad(date.getSeconds())
}

const dispositionOptions = [
  { value: '', label: '请选择处理方式' },
  { value: 'REWORK', label: '返工' },
  { value: 'SCRAP', label: '报废' },
  { value: 'DOWNGRADE', label: '降级' },
  { value: 'CONCESSION', label: '让步接收' }
]

const defaultForm = {
  ncrId: '',
  ncrNo: '',
  inspectionId: '',
  productLotId: '',
  defectReason: '',
  dispositionType: '',
  remark: '',
  imageUrl: ''
}

Page({
  data: {
    form: { ...defaultForm },
    dispositionOptions,
    dispositionIndex: 0,
    failedInspectionOptions: [{ value: '', label: '请选择检验单' }],
    inspectionIndex: 0,
    inspectionNo: '',
    entryTimeText: currentTimeText(),
    associationLocked: false,
    submitting: false,
    loading: false,
    mode: 'create',
    canSave: false,
    photoPath: '',
    photoIsLocal: false,
    photoUploading: false
  },

  onLoad(options) {
    if (options && options.ncrId) {
      this.loadDetail(decodeURIComponent(options.ncrId))
      return
    }
    if (options && options.inspectionId) {
      const inspectionId = decodeURIComponent(options.inspectionId)
      this.setData({ 'form.inspectionId': inspectionId, associationLocked: true })
      this.loadInspection(inspectionId).catch((error) => {
        wx.showToast({ title: error.message || '检验单加载失败', icon: 'none' })
      })
      return
    }
    this.loadFailedInspections()
  },

  onShow() {
    if (!guard.requireAuth()) return
    const permission = this.data.mode === 'detail'
      ? 'qms:nonconforming-order:update'
      : 'qms:nonconforming-order:generate'
    const canSave = auth.hasPermission(permission)
    this.setData({ canSave })
    if (!canSave && this.data.mode !== 'detail') {
      wx.showToast({ title: '当前账号无发起处置权限', icon: 'none' })
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

  handleInspection(event) {
    const inspectionIndex = Number(event.detail.value)
    const selected = this.data.failedInspectionOptions[inspectionIndex]
    this.setData({
      inspectionIndex,
      inspectionNo: selected.inspectionNo || '',
      'form.inspectionId': selected.value
    })
  },

  async loadFailedInspections() {
    try {
      const page = await qualityApi.inspectionPage({
        current: 1,
        size: 100,
        status: 'FINISHED',
        inspectionResult: 'FAIL'
      })
      const records = page && Array.isArray(page.records) ? page.records : []
      this.setData({
        failedInspectionOptions: [{ value: '', label: '请选择检验单' }].concat(records.map((item) => ({
          value: String(item.inspectionId),
          inspectionNo: item.inspectionNo || '',
          label: item.inspectionNo || ('检验单 ' + item.inspectionId)
        })))
      })
    } catch (error) {
      wx.showToast({ title: error.message || '检验单加载失败', icon: 'none' })
    }
  },

  async loadInspection(inspectionId) {
    const inspection = await qualityApi.inspectionDetail(inspectionId)
    if (!inspection || inspection.status !== 'FINISHED' || inspection.inspectionResult !== 'FAIL') {
      throw new Error('只能关联已完成且不合格的检验单')
    }
    this.setData({ inspectionNo: inspection.inspectionNo || ('检验单 ' + inspection.inspectionId) })
    return inspection
  },

  async loadDetail(ncrId) {
    this.setData({ loading: true, mode: 'detail' })
    try {
      const detail = await qualityApi.nonconformingDetail(ncrId)
      let inspection = null
      if (detail.inspectionId) {
        try {
          inspection = await qualityApi.inspectionDetail(detail.inspectionId)
        } catch (error) {
          inspection = null
        }
      }
      const dispositionCode = String(detail.dispositionType || '').toUpperCase()
      const dispositionIndex = Math.max(0, dispositionOptions.findIndex((item) => item.value === dispositionCode))
      this.setData({
        form: {
          ncrId: detail.ncrId || '',
          ncrNo: detail.ncrNo || '',
          inspectionId: detail.inspectionId || '',
          productLotId: detail.productLotId || '',
          defectReason: detail.defectName || detail.defectReason || '',
          dispositionType: dispositionCode || dispositionOptions[dispositionIndex].value,
          remark: '',
          imageUrl: detail.imageUrl || detail.pictureUrl || detail.photoUrl || ''
        },
        dispositionIndex,
        inspectionNo: inspection && inspection.inspectionNo
          ? inspection.inspectionNo
          : (detail.inspectionId ? '检验单 ' + detail.inspectionId : '-'),
        entryTimeText: format.formatDateTime(detail.createdTime),
        associationLocked: true
      })
      await this.loadPhoto(ncrId)
    } catch (error) {
      wx.showToast({ title: error.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadPhoto(ncrId) {
    try {
      const page = await fileApi.list({
        current: 1,
        size: 1,
        bizType: PHOTO_BIZ_TYPE,
        bizId: Number(ncrId)
      })
      const records = page && Array.isArray(page.records) ? page.records : []
      if (records.length) {
        this.setData({
          photoPath: fileApi.absoluteUrl(records[0].fileUrl),
          photoIsLocal: false,
          'form.imageUrl': fileApi.absoluteUrl(records[0].fileUrl)
        })
      }
    } catch (error) {
      wx.showToast({ title: error.message || '现场照片加载失败', icon: 'none' })
    }
  },

  choosePhoto() {
    if (this.data.submitting || this.data.photoUploading) return
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['camera', 'album'],
      sizeType: ['compressed'],
      success: (result) => {
        const file = result.tempFiles && result.tempFiles[0]
        if (file && file.tempFilePath) {
          this.setData({
            photoPath: file.tempFilePath,
            photoIsLocal: true,
            'form.imageUrl': file.tempFilePath
          })
        }
      }
    })
  },

  previewPhoto() {
    const photoPath = this.data.photoPath || this.data.form.imageUrl
    if (photoPath) wx.previewImage({ current: photoPath, urls: [photoPath] })
  },

  removePhoto() {
    if (this.data.submitting || this.data.photoUploading) return
    this.setData({ photoPath: '', photoIsLocal: false, 'form.imageUrl': '' })
  },

  resetForm() {
    const inspectionId = this.data.form.inspectionId
    const ncrId = this.data.form.ncrId
    const ncrNo = this.data.form.ncrNo
    this.setData({
      form: { ...defaultForm, inspectionId, ncrId, ncrNo },
      dispositionIndex: 0,
      inspectionIndex: this.data.inspectionIndex,
      photoPath: '',
      photoIsLocal: false
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
    if (!String(this.data.form.inspectionId || '').trim()) return '请选择关联检验单'
    if (!String(this.data.form.defectReason || '').trim()) return '请输入不良原因'
    if (!String(this.data.form.dispositionType || '').trim()) return '请选择处理方式'
    const remark = format.validateRemark(this.data.form.remark, 200)
    if (remark.error) return remark.error
    return ''
  },

  async submit() {
    const permission = this.data.form.ncrId ? 'qms:nonconforming-order:update' : 'qms:nonconforming-order:generate'
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
      if (!form.ncrId) await this.loadInspection(form.inspectionId)
      const payload = {
        defectReason: String(form.defectReason || '').trim(),
        dispositionType: form.dispositionType,
        remark: format.validateRemark(form.remark, 200).value
      }
      let ncrId = form.ncrId
      if (form.ncrId) {
        await qualityApi.updateNonconforming(form.ncrId, payload)
      } else {
        const created = await qualityApi.generateNonconforming(Number(form.inspectionId), payload)
        ncrId = created && created.ncrId ? created.ncrId : created && created.ncrId === 0 ? 0 : null
        if (!ncrId) throw new Error('不良品记录已提交，但未返回记录编号')
        this.setData({ 'form.ncrId': ncrId, associationLocked: true })
      }
      const photoPath = this.data.photoPath
      if (photoPath && !/^https?:/i.test(photoPath)) {
        this.setData({ photoUploading: true })
        const uploaded = await fileApi.upload(photoPath, PHOTO_BIZ_TYPE, ncrId)
        this.setData({
          photoPath: fileApi.absoluteUrl(uploaded.fileUrl),
          photoIsLocal: false,
          'form.imageUrl': fileApi.absoluteUrl(uploaded.fileUrl)
        })
      }
      wx.showToast({ title: form.ncrId ? '已保存' : '已录入', icon: 'success' })
      setTimeout(() => {
        if (form.ncrId) {
          wx.navigateBack()
          return
        }
        wx.redirectTo({
          url: '/pages/quality/pending-detail/index?mode=record&inspectionId=' + encodeURIComponent(form.inspectionId)
        })
      }, 500)
    } catch (error) {
      wx.showToast({ title: error.message || '提交失败', icon: 'none' })
    } finally {
      this.setData({ submitting: false, photoUploading: false })
    }
  }
})
