// 设备模块 API：设备列表、设备详情、维修单查询和维修提交。
const request = require('../utils/request')

function equipmentPage(data) {
  return request({ url: '/api/eam/equipment/page', data })
}

function equipmentDetail(equipmentId) {
  return request({ url: '/api/eam/equipment/' + encodeURIComponent(equipmentId) })
}

function updateEquipment(data) {
  return request({
    url: '/api/eam/equipment',
    method: 'PUT',
    data
  })
}

function repairPage(data) {
  return request({ url: '/api/eam/repair-order/page', data })
}

function repairDetail(repairId) {
  return request({ url: '/api/eam/repair-order/' + encodeURIComponent(repairId) })
}

function assignRepair(repairId, data) {
  return request({
    url: '/api/eam/repair-order/' + encodeURIComponent(repairId) + '/assign',
    method: 'PUT',
    data
  })
}

function closeRepair(repairId, data) {
  return request({
    url: '/api/eam/repair-order/' + encodeURIComponent(repairId) + '/close',
    method: 'PUT',
    data
  })
}

module.exports = {
  equipmentPage,
  equipmentDetail,
  updateEquipment,
  repairPage,
  repairDetail,
  assignRepair,
  closeRepair
}
