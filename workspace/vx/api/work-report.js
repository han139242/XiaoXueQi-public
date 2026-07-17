const request = require('../utils/request')

// 报工 API，对应后端 /api/mfg/work-report。
// 任务达标、报工记录查询和报工提交都围绕 taskId/reportId 展开。
function page(data) {
  return request({ url: '/api/mfg/work-report/page', data })
}

function detail(reportId) {
  return request({ url: '/api/mfg/work-report/' + encodeURIComponent(reportId) })
}

function materials(reportId) {
  return request({ url: '/api/mfg/report-material/report/' + encodeURIComponent(reportId) })
}

function submit(data) {
  return request({
    url: '/api/mfg/work-report/submit',
    method: 'POST',
    data,
    timeout: 30000
  })
}

module.exports = {
  page,
  detail,
  materials,
  submit
}
