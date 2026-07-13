const BASE_URL_KEY = 'MES_BASE_URL'

const versionUrls = {
  develop: 'http://127.0.0.1:8088',
  trial: '',
  release: ''
}

function getEnvVersion() {
  try {
    return wx.getAccountInfoSync().miniProgram.envVersion || 'develop'
  } catch (error) {
    return 'develop'
  }
}

function getBaseUrl() {
  const override = wx.getStorageSync(BASE_URL_KEY)
  const baseUrl = override || versionUrls[getEnvVersion()]
  if (!baseUrl) {
    throw new Error('当前环境尚未配置 MES 后端地址')
  }
  return String(baseUrl).replace(/\/+$/, '')
}

function setDebugBaseUrl(baseUrl) {
  if (getEnvVersion() === 'release') return false
  const value = String(baseUrl || '').trim()
  if (value) {
    wx.setStorageSync(BASE_URL_KEY, value)
  } else {
    wx.removeStorageSync(BASE_URL_KEY)
  }
  return true
}

function clearDebugBaseUrl() {
  wx.removeStorageSync(BASE_URL_KEY)
}

module.exports = {
  getEnvVersion,
  getBaseUrl,
  setDebugBaseUrl,
  clearDebugBaseUrl
}
