const auth = require('./auth')
const env = require('../config/env')
const mockData = require('../mock/data')

const PAGE_PERMISSIONS = {
  'pages/production-order/index/index': 'mfg:production-order:view',
  'pages/production-order/detail/index': 'mfg:production-order:view',
  'pages/work-order/index/index': 'mfg:work-order:view',
  'pages/work-order/detail/index': 'mfg:work-order:view',
  'pages/task/index/index': 'mfg:task:view',
  'pages/task/detail/index': 'mfg:task:view',
  'pages/task/finish/index': 'mfg:task:finish',
  'pages/report/index/index': 'mfg:work-report:view',
  'pages/report/detail/index': 'mfg:work-report:view',
  'pages/report/form/index': 'mfg:work-report:create',
  'pages/quality/pending/index': 'qms:inspection-order:view',
  'pages/quality/pending-detail/index': 'qms:inspection-order:view',
  'pages/quality/records/index': 'qms:inspection-order:view',
  'pages/quality/defect/index': 'qms:nonconforming-order:view',
  'pages/quality/defect-form/index': 'qms:nonconforming-order:view',
  'pages/warehouse/inbound/index': 'wms:inbound-order:view',
  'pages/warehouse/inbound-detail/index': 'wms:inbound-order:view',
  'pages/warehouse/outbound/index': 'wms:outbound-order:view',
  'pages/warehouse/outbound-detail/index': 'wms:outbound-order:view',
  'pages/warehouse/inventory/index': 'wms:inventory:view',
  'pages/equipment/list/index': 'eam:equipment:view',
  'pages/equipment/detail/index': 'eam:equipment:view',
  'pages/equipment/repair/index': 'eam:repair-order:view',
  'pages/equipment/repair-detail/index': 'eam:repair-order:view',
  'pages/trace/scan-result/index': 'trace:barcode:scan'
}

function ensureMockAuth() {
  if (!env.useMockData()) return false
  if (!auth.getToken()) auth.setToken('mock-token')
  if (!auth.getUser()) auth.setUser(mockData.currentUser)
  return true
}

function requireAuth() {
  ensureMockAuth()
  if (auth.getToken()) {
    // 每个页面 onShow 时会调用 requireAuth。
    // 这里除了判断是否登录，还根据当前 route 做页面级权限拦截。
    const pages = getCurrentPages()
    const route = pages.length ? pages[pages.length - 1].route : ''
    const permissionCode = PAGE_PERMISSIONS[route]
    if (!permissionCode || auth.hasPermission(permissionCode)) return true
    wx.showToast({ title: '当前账号无访问权限', icon: 'none' })
    setTimeout(() => {
      wx.switchTab({ url: '/pages/workbench/index/index' })
    }, 300)
    return false
  }
  wx.reLaunch({ url: '/pages/login/index' })
  return false
}

function requirePermission(permissionCode, message = '当前账号无操作权限') {
  // 按钮级权限校验，例如“开工”“报工”“提交质检”。
  // 页面能看不代表所有按钮都能操作。
  if (auth.hasPermission(permissionCode)) return true
  wx.showToast({ title: message, icon: 'none' })
  return false
}

function requireEmployee() {
  // 现场操作类接口需要 operatorId/employeeId。
  // 如果账号没有关联员工档案，就不能提交报工、质检等业务记录。
  const user = auth.getUser()
  if (user && user.employeeId) return user.employeeId
  if (auth.isSuperAdmin(user) && user && user.userId) return user.userId
  wx.showModal({
    title: '无法执行现场操作',
    content: '当前账号未关联员工档案，请联系管理员。',
    showCancel: false
  })
  return null
}

module.exports = {
  requireAuth,
  requireEmployee,
  requirePermission
}
