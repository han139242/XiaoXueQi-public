const request = require('../utils/request')

// 登录相关接口。页面不直接写 wx.request，而是调用这里的方法；
// 这样接口路径、超时时间和返回格式都集中维护。
function login(data) {
  return request({
    url: '/api/auth/login',
    method: 'POST',
    data,
    raw: true,
    silent: true,
    timeout: 30000
  })
}

function info(options) {
  return request(Object.assign({
    url: '/api/auth/info',
    timeout: 30000
  }, options || {}))
}

function logout() {
  return request({
    url: '/api/auth/logout',
    method: 'POST',
    silent: true
  })
}

module.exports = {
  login,
  info,
  logout
}
