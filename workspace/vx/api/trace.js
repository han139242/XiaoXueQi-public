// 追溯扫码 API：扫码后按条码值查询工单、批次、报工等关联信息。
const request = require('../utils/request')

function scan(barcodeValue) {
  return request({ url: '/api/trace/barcode/scan/' + encodeURIComponent(barcodeValue) })
}

module.exports = {
  scan
}
