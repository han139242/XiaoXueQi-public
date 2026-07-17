// 生产计划 API：用于工单、任务页面补充计划开始/结束时间。
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
