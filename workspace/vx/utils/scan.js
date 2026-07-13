function scanToResult() {
  wx.scanCode({
    scanType: ['barCode', 'qrCode'],
    success(result) {
      const value = String(result.result || '').trim()
      if (!value) {
        wx.showToast({ title: '未识别到条码内容', icon: 'none' })
        return
      }
      wx.navigateTo({
        url: '/pages/trace/scan-result/index?value=' + encodeURIComponent(value)
      })
    },
    fail(error) {
      if (!String(error.errMsg || '').includes('cancel')) {
        wx.showToast({ title: '扫码失败，请重试', icon: 'none' })
      }
    }
  })
}

module.exports = {
  scanToResult
}
