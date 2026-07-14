const guard = require('../../../utils/guard')
const scanUtil = require('../../../utils/scan')

Page({
  onShow() {
    guard.requireAuth()
  },

  openTab(event) {
    const url = event.currentTarget.dataset.url
    if (!url) return
    wx.switchTab({
      url,
      fail: () => {
        wx.showToast({ title: '页面跳转失败', icon: 'none' })
      }
    })
  },

  openPage(event) {
    const url = event.currentTarget.dataset.url
    if (!url) return
    wx.navigateTo({
      url,
      fail: () => {
        wx.showToast({ title: '页面打开失败', icon: 'none' })
      }
    })
  },

  startScan() {
    scanUtil.scanToResult()
  }
})
