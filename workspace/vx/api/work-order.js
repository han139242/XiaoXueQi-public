const request = require('../utils/request')

function page(data) {
  return request({ url: '/api/mfg/work-order/page', data })
}

function detail(workOrderId) {
  return request({ url: '/api/mfg/work-order/' + encodeURIComponent(workOrderId) })
}

module.exports = {
  page,
  detail
}
