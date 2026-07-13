const request = require('../utils/request')

function summary(data) {
  return request({ url: '/api/dashboard/summary', data: data || {} })
}

module.exports = {
  summary
}
