const request = require('../utils/request')

function page(data) {
  return request({ url: '/api/mfg/task/page', data })
}

function detail(taskId) {
  return request({ url: '/api/mfg/task/' + encodeURIComponent(taskId) })
}

function start(taskId, data) {
  return request({
    url: '/api/mfg/task/' + encodeURIComponent(taskId) + '/start',
    method: 'PUT',
    data: data || {}
  })
}

function finish(taskId, data) {
  return request({
    url: '/api/mfg/task/' + encodeURIComponent(taskId) + '/finish',
    method: 'PUT',
    data
  })
}

module.exports = {
  page,
  detail,
  start,
  finish
}
