// 生产订单 API：用于移动端查看生产订单列表和详情。
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
