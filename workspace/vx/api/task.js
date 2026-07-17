const request = require('../utils/request')

// 生产任务 API，对应后端 /api/mfg/task。
// 小程序任务列表、任务详情、开工和完工都从这里发起。
function page(data) {
  return request({ url: '/api/mfg/task/page', data })
}

function pageForView(data) {
  // fullView=true 表示请求后端返回适合移动端展示的扩展字段，
  // 例如当前用户是否被派工、累计报工数量等。
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
