const request = require('../utils/request')

function login(data) {
  return request({
    url: '/api/auth/login',
    method: 'POST',
    data,
    raw: true
  })
}

function info() {
  return request({ url: '/api/auth/info' })
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
