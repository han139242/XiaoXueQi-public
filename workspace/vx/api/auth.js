const request = require('../utils/request')

function login(data) {
  return request({
    url: '/api/auth/login',
    method: 'POST',
    data,
    raw: true,
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
