const request = require('../utils/request')

function inboundPage(data) {
  return request({ url: '/api/wms/inbound-order/page', data })
}

function inboundDetail(inboundId) {
  return request({ url: '/api/wms/inbound-order/' + encodeURIComponent(inboundId) })
}

function inboundDetails(inboundId) {
  return request({ url: '/api/wms/inbound-detail/inbound/' + encodeURIComponent(inboundId) })
}

function confirmInbound(inboundId, data) {
  return request({
    url: '/api/wms/inbound-order/' + encodeURIComponent(inboundId) + '/confirm',
    method: 'PUT',
    data
  })
}

function outboundPage(data) {
  return request({ url: '/api/wms/outbound-order/page', data })
}

function outboundDetail(outboundId) {
  return request({ url: '/api/wms/outbound-order/' + encodeURIComponent(outboundId) })
}

function outboundDetails(outboundId) {
  return request({ url: '/api/wms/outbound-detail/outbound/' + encodeURIComponent(outboundId) })
}

function confirmOutbound(outboundId, data) {
  return request({
    url: '/api/wms/outbound-order/' + encodeURIComponent(outboundId) + '/confirm',
    method: 'PUT',
    data
  })
}

function inventoryPage(data) {
  return request({ url: '/api/wms/inventory/page', data })
}

function inventoryDetail(inventoryId) {
  return request({ url: '/api/wms/inventory/' + encodeURIComponent(inventoryId) })
}

module.exports = {
  inboundPage,
  inboundDetail,
  inboundDetails,
  confirmInbound,
  outboundPage,
  outboundDetail,
  outboundDetails,
  confirmOutbound,
  inventoryPage,
  inventoryDetail
}
