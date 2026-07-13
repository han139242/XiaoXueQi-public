const TOKEN_KEY = 'MES_TOKEN'
const USER_KEY = 'MES_USER'

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
  const app = getApp({ allowDefault: true })
  if (app && app.globalData) app.globalData.user = user
}

function clearAuth() {
  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync(USER_KEY)
  const app = getApp({ allowDefault: true })
  if (app && app.globalData) app.globalData.user = null
}

module.exports = {
  getToken,
  setToken,
  getUser,
  setUser,
  clearAuth
}
