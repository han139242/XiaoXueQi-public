const BASE_URL_KEY = 'MES_BASE_URL'
const MOCK_DATA_KEY = 'MES_USE_MOCK_DATA'

const versionUrls = {
  trial: '',
  release: ''
}

const developUrls = {
  // 微信开发者工具里访问的是本机后端。
  // 注意：127.0.0.1 对每台电脑都只代表“自己这台电脑”。
  devtools: 'http://127.0.0.1:8088',
  // 真机端不能访问电脑自己的 127.0.0.1，所以使用 HTTPS 可访问地址。
  // 预览/体验版还要求该域名配置到微信公众平台 request 合法域名。
  device: 'https://zjy.tail8d376f.ts.net'
}

const mockDataDefaults = {
  develop: false,
  trial: false,
  release: false
}

function getEnvVersion() {
  try {
    // envVersion: develop=开发版，trial=体验版，release=正式版。
    // 本项目根据环境决定使用哪个后端地址和是否允许 mock。
    return wx.getAccountInfoSync().miniProgram.envVersion || 'develop'
  } catch (error) {
    return 'develop'
  }
}

function isDevTools() {
  try {
    // 微信开发者工具的 platform 是 devtools；真机一般是 ios/android。
    // 这里用于区分“电脑模拟器”和“手机真机”的默认后端地址。
    const device = typeof wx.getDeviceInfo === 'function'
      ? wx.getDeviceInfo()
      : wx.getSystemInfoSync()
    return device.platform === 'devtools'
  } catch (error) {
    return false
  }
}

function getDefaultBaseUrl(envVersion) {
  if (envVersion === 'develop') {
    return isDevTools() ? developUrls.devtools : developUrls.device
  }
  return versionUrls[envVersion]
}

function getBaseUrl() {
  const envVersion = getEnvVersion()
  const override = wx.getStorageSync(BASE_URL_KEY)
  // 手机端不能可靠访问 http://127.0.0.1 这类本机地址。
  // 如果缓存里误存了 http 地址，真机开发版回退到 device 默认 HTTPS 地址。
  const invalidDeviceHttpOverride = envVersion === 'develop' && !isDevTools()
    && /^http:\/\//i.test(String(override || ''))
  const defaultBaseUrl = getDefaultBaseUrl(envVersion)
  const baseUrl = invalidDeviceHttpOverride ? defaultBaseUrl : (override || defaultBaseUrl)
  if (!baseUrl) {
    throw new Error('当前环境尚未配置 MES 后端地址')
  }
  return String(baseUrl).replace(/\/+$/, '')
}

function useMockData() {
  const envVersion = getEnvVersion()
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
  // 调试入口可临时覆盖后端地址，写入微信本地缓存；
  // 清缓存或调用 clearDebugBaseUrl 后会恢复默认地址。
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
