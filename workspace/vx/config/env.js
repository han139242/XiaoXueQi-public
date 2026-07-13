const BASE_URL_KEY = 'MES_BASE_URL'
const MOCK_DATA_KEY = 'MES_USE_MOCK_DATA'

const versionUrls = {
  develop: 'http://127.0.0.1:8088',
  trial: '',
  release: ''
}

const mockDataDefaults = {
  develop: true,
  trial: false,
  release: false
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

function useMockData() {
  const envVersion = getEnvVersion()
  if (envVersion === 'develop') return true
  if (envVersion === 'release') return false
  const override = wx.getStorageSync(MOCK_DATA_KEY)
  if (override === true || override === 'true') return true
  if (override === false || override === 'false') return false
  return Boolean(mockDataDefaults[envVersion])
}

function setMockDataEnabled(enabled) {
  if (getEnvVersion() === 'release') return false
  wx.setStorageSync(MOCK_DATA_KEY, Boolean(enabled))
  return true
}

function clearMockDataOverride() {
  wx.removeStorageSync(MOCK_DATA_KEY)
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
  useMockData,
  setMockDataEnabled,
  clearMockDataOverride,
  setDebugBaseUrl,
  clearDebugBaseUrl
}
