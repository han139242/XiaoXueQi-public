// 生产工单 API：工单列表、详情和开工操作。
const request = require('../utils/request')

function page(data) {
  return request({ url: '/api/mfg/work-order/page', data })
}

function detail(workOrderId) {
  return request({ url: '/api/mfg/work-order/' + encodeURIComponent(workOrderId) })
}

function start(workOrderId) {
  return request({
    url: '/api/mfg/work-order/' + encodeURIComponent(workOrderId) + '/start',
    method: 'PUT'
  })
}

module.exports = {
  page,
  detail,
  start
}
