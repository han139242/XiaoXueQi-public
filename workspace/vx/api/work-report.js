const request = require('../utils/request')

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
