const request = require('../utils/request')

function page(data) {
  return request({ url: '/api/mfg/production-plan/page', data })
}

function detail(planId) {
  return request({ url: '/api/mfg/production-plan/' + encodeURIComponent(planId) })
}

module.exports = {
  page,
  detail
}
