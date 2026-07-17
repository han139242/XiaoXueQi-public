const request = require('../utils/request')

function page(data) {
  return request({ url: '/api/mfg/task/page', data })
}

function pageForView(data) {
  return page({ ...(data || {}), fullView: true })
}

function detail(taskId) {
  return request({ url: '/api/mfg/task/' + encodeURIComponent(taskId) })
}

function detailForView(taskId) {
  return request({
    url: '/api/mfg/task/' + encodeURIComponent(taskId),
    data: { fullView: true }
  })
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
  pageForView,
  detail,
  detailForView,
  start,
  finish
}
