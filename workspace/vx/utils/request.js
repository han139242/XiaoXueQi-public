const env = require('../config/env')
const auth = require('./auth')

let redirectingToLogin = false

function redirectToLogin() {
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

function request(options) {
  const config = options || {}
  const token = auth.getToken()
  const header = Object.assign({
    'Content-Type': 'application/json'
  }, config.header || {})

  if (token) header.Authorization = 'Bearer ' + token

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
        if (res.statusCode === 401 || Number(body.code) === 1002) {
          redirectToLogin()
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
        if (!config.silent) wx.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
        reject(error)
      }
    })
  })
}

module.exports = request
