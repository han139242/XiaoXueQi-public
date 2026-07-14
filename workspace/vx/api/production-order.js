const request = require('../utils/request')

function page(data) {
  return request({ url: '/api/mfg/production-order/page', data })
}

function detail(productionOrderId) {
  return request({ url: '/api/mfg/production-order/' + encodeURIComponent(productionOrderId) })
}

module.exports = {
  page,
  detail
}
