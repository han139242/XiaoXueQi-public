const env = require('../config/env')
const auth = require('./auth')
const mockData = require('../mock/data')

let redirectingToLogin = false

function redirectToLogin() {
  // 多个接口同时返回 401 时，只跳转一次登录页，避免页面反复 reLaunch。
  if (redirectingToLogin) return
  redirectingToLogin = true
  auth.clearAuth()
  wx.showToast({ title: '登录已失效', icon: 'none' })
  setTimeout(() => {
    wx.reLaunch({
      url: '/pages/login/index',
      complete: () => {
        redirectingToLogin = false
      }
    })
  }, 300)
}

function createError(message, body, statusCode) {
  const error = new Error(message)
  error.code = body && body.code
  error.data = body && body.data
  error.statusCode = statusCode
  return error
}

function networkErrorMessage(error) {
  const detail = String(error && (error.errMsg || error.message) || '')
  // 这些错误发生在 wx.request 的 fail 阶段，说明请求没有正常进入后端业务接口。
  // 常见原因是微信合法域名、网络连通、证书或后端进程不可达。
  if (/url not in domain list|domain.*(invalid|not configured|not allowed)/i.test(detail)) {
    return '开发者工具请关闭合法域名校验；真机或体验版请配置 HTTPS request 合法域名'
  }
  if (/timeout/i.test(detail)) {
    return '连接后端超时，请确认电脑上的 MES 后端和 Tailscale Funnel 正常运行'
  }
  if (/connection refused|err_connection_refused|unable to connect|failed to connect/i.test(detail)) {
    return '无法连接 MES 后端，请确认电脑后端已启动'
  }
  if (/ssl|certificate|handshake/i.test(detail)) {
    return '后端 HTTPS 证书校验失败'
  }
  return detail ? '网络连接失败：' + detail.replace(/^request:fail\s*/i, '') : '网络连接失败'
}

function request(options) {
  const config = options || {}
  const token = auth.getToken()
  const header = Object.assign({
    'Content-Type': 'application/json'
  }, config.header || {})

  // 登录成功后，后端要求所有受保护接口都携带 Authorization: Bearer <token>。
  if (token) header.Authorization = 'Bearer ' + token

  if (env.useMockData()) {
    return mockData.handleRequest(config).catch((error) => {
      if (!config.silent) wx.showToast({ title: error.message || '本地测试数据请求失败', icon: 'none' })
      return Promise.reject(error)
    })
  }

  let baseUrl
  try {
    baseUrl = env.getBaseUrl()
  } catch (error) {
    if (!config.silent) wx.showToast({ title: error.message, icon: 'none' })
    return Promise.reject(error)
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: baseUrl + config.url,
      method: config.method || 'GET',
      data: config.data || {},
      header,
      timeout: config.timeout || 10000,
      success(res) {
        const body = res.data || {}
        // 后端统一返回 ResultData：{ code, msg, data }。
        // raw=true 用于登录接口拿完整响应；普通接口默认只返回 data。
        if (res.statusCode === 401 || Number(body.code) === 1002) {
          if (!config.skipAuthRedirect) redirectToLogin()
          reject(createError('登录已失效', body, res.statusCode))
          return
        }

        if (res.statusCode < 200 || res.statusCode >= 300) {
          const message = body.msg || '服务请求失败'
          if (!config.silent) wx.showToast({ title: message, icon: 'none' })
          reject(createError(message, body, res.statusCode))
          return
        }

        if (Number(body.code) !== 200) {
          const message = body.msg || '业务处理失败'
          if (!config.silent) wx.showToast({ title: message, icon: 'none' })
          reject(createError(message, body, res.statusCode))
          return
        }

        resolve(config.raw ? body : body.data)
      },
      fail(error) {
        const message = networkErrorMessage(error)
        if (!config.silent) wx.showToast({ title: message, icon: 'none', duration: 3500 })
        const requestError = createError(message, { detail: error && error.errMsg }, null)
        requestError.errMsg = error && error.errMsg
        reject(requestError)
      }
    })
  })
}

module.exports = request
