const guard = require('../../../utils/guard')
const scanUtil = require('../../../utils/scan')
const auth = require('../../../utils/auth')

Page({
  data: {
    canDashboard: false,
    canWorkOrder: false,
    canTask: false,
    canReport: false,
    canTrace: false,
    canQualityInspection: false,
    canQualityDefect: false,
    showQuality: false,
    canInbound: false,
    canOutbound: false,
    canInventory: false,
    showWarehouse: false,
    canEquipment: false,
    canRepair: false,
    showEquipment: false
  },

  onShow() {
    if (!guard.requireAuth()) return
    const canQualityInspection = auth.hasPermission('qms:inspection-order:view')
    const canQualityDefect = auth.hasPermission('qms:nonconforming-order:view')
    const canInbound = auth.hasPermission('wms:inbound-order:view')
    const canOutbound = auth.hasPermission('wms:outbound-order:view')
    const canInventory = auth.hasPermission('wms:inventory:view')
    const canEquipment = auth.hasPermission('eam:equipment:view')
    const canRepair = auth.hasPermission('eam:repair-order:view')
    this.setData({
      canDashboard: auth.hasPermission('dashboard:summary:view'),
      canWorkOrder: auth.hasPermission('mfg:work-order:view'),
      canTask: auth.hasPermission('mfg:task:view'),
      canReport: auth.hasPermission('mfg:work-report:view'),
      canTrace: auth.hasPermission('trace:barcode:scan'),
      canQualityInspection,
      canQualityDefect,
      showQuality: canQualityInspection || canQualityDefect,
      canInbound,
      canOutbound,
      canInventory,
      showWarehouse: canInbound || canOutbound || canInventory,
      canEquipment,
      canRepair,
      showEquipment: canEquipment || canRepair
    })
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
    if (!guard.requirePermission('trace:barcode:scan')) return
    scanUtil.scanToResult()
  }
})
