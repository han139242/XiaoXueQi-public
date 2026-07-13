const guard = require('../../../utils/guard')
const traceApi = require('../../../api/trace')
const format = require('../../../utils/format')

Page({
  data: {
    value: '',
    barcode: null,
    loading: false,
    searched: false,
    error: ''
  },

  onLoad(options) {
    const value = this.decodeValue(options.value)
    if (value) {
      this.setData({ value })
      this.query()
    }
  },

  onShow() {
    guard.requireAuth()
  },

  decodeValue(value) {
    if (!value) return ''
    try {
      return decodeURIComponent(value)
    } catch (error) {
      return String(value)
    }
  },

  handleInput(event) {
    this.setData({ value: event.detail.value, error: '' })
  },

  rescan() {
    wx.scanCode({
      scanType: ['barCode', 'qrCode'],
      success: (result) => {
        const value = String(result.result || '').trim()
        if (!value) {
          wx.showToast({ title: '未识别到条码内容', icon: 'none' })
          return
        }
        this.setData({ value, searched: false, barcode: null, error: '' })
        this.query()
      },
      fail(error) {
        if (!String(error.errMsg || '').includes('cancel')) {
          wx.showToast({ title: '扫码失败，请重试', icon: 'none' })
        }
      }
    })
  },

  async query() {
    const value = this.data.value.trim()
    if (!value || this.data.loading) {
      if (!value) this.setData({ error: '请输入或扫描条码' })
      return
    }
    this.setData({ loading: true, searched: true, error: '', barcode: null })
    try {
      const barcode = await traceApi.scan(value)
      this.setData({
        barcode: barcode ? {
          ...barcode,
          createdTimeText: format.formatDateTime(barcode.createdTime),
          updatedTimeText: format.formatDateTime(barcode.updatedTime)
        } : null,
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false, error: error.message || '条码查询失败' })
    }
  }
})
