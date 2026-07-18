const TOKEN_KEY = 'MES_TOKEN'
const USER_KEY = 'MES_USER'

// auth.js 只负责保存和判断登录态，不直接请求后端。
// 后端接口调用统一放在 api/auth.js，页面只通过这里读取 token/user。
function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || ''
}

function setToken(token) {
  wx.setStorageSync(TOKEN_KEY, token)
}

function getUser() {
  return wx.getStorageSync(USER_KEY) || null
}

function setUser(user) {
  wx.setStorageSync(USER_KEY, user)
  // 登录成功后同步到 App.globalData，便于工作台、TabBar 等组件读取。
  const app = getApp({ allowDefault: true })
  if (app && app.globalData) app.globalData.user = user
}

function clearAuth() {
  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync(USER_KEY)
  const app = getApp({ allowDefault: true })
  if (app && app.globalData) app.globalData.user = null
}

function isAdminAccount(user) {
  return String(user && user.username || '').toLowerCase() === 'admin'
}

function isSuperAdmin(user = getUser()) {
  return Boolean(user && (user.superAdmin || isAdminAccount(user)
    || (Array.isArray(user.roles) && user.roles.includes('SUPER_ADMIN'))))
}

function hasPermission(permissionCode, user = getUser()) {
  if (!permissionCode) return false
  // 超级管理员在后端和小程序端都视为全权限。
  if (isSuperAdmin(user)) return true
  return Boolean(user && Array.isArray(user.permissions) && user.permissions.includes(permissionCode))
}

function hasAnyPermission(permissionCodes, user = getUser()) {
  return Array.isArray(permissionCodes) && permissionCodes.some((code) => hasPermission(code, user))
}

module.exports = {
  getToken,
  setToken,
  getUser,
  setUser,
  clearAuth,
  isSuperAdmin,
  hasPermission,
  hasAnyPermission
}
