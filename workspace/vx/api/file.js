const request = require('../utils/request')
const auth = require('../utils/auth')
const env = require('../config/env')

function list(data) {
  return request({ url: '/api/sys/file/page', data, silent: true })
}

function upload(filePath, bizType, bizId) {
  if (env.useMockData()) {
    return Promise.resolve({ fileId: Date.now(), fileUrl: filePath, bizType, bizId })
  }
  const token = auth.getToken()
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: env.getBaseUrl() + '/api/sys/file/upload',
      filePath,
      name: 'file',
      formData: {
        bizType: String(bizType),
        bizId: String(bizId)
      },
      header: token ? { Authorization: 'Bearer ' + token } : {},
      timeout: 30000,
      success(response) {
        let body
        try {
          body = JSON.parse(response.data || '{}')
        } catch (error) {
          reject(new Error('图片上传响应格式异常'))
          return
        }
        if (response.statusCode < 200 || response.statusCode >= 300 || Number(body.code) !== 200) {
          reject(new Error(body.msg || '图片上传失败'))
          return
        }
        resolve(body.data)
      },
      fail(error) {
        reject(new Error(error.errMsg || '图片上传失败'))
      }
    })
  })
}

function absoluteUrl(fileUrl) {
  const value = String(fileUrl || '')
  if (!value || /^(https?:|wxfile:|file:)/i.test(value)) return value
  return env.getBaseUrl() + (value.startsWith('/') ? value : '/' + value)
}

module.exports = {
  list,
  upload,
  absoluteUrl
}
