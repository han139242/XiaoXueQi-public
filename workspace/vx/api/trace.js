const request = require('../utils/request')

function scan(barcodeValue) {
  return request({ url: '/api/trace/barcode/scan/' + encodeURIComponent(barcodeValue) })
}

module.exports = {
  scan
}
