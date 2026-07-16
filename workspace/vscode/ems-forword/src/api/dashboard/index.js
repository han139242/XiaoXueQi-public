import request from '@/api/request'

// 仪表盘接口按展示区域拆分，页面可以并行请求；params通常包含日期范围等统计口径。
export function getDashboardSummary(params) {
  return request.get('/api/dashboard/summary', { params })
}

// 查询近几天的合格产量和不良产量趋势。
export function getProductionTrend(params) {
  return request.get('/api/dashboard/production-trend', { params })
}

// 查询检验数量、合格数量和合格率等质量统计。
export function getQualityStatistics(params) {
  return request.get('/api/dashboard/quality-statistics', { params })
}

// 查询运行、停机、故障和保养中的设备数量。
export function getEquipmentStatus() {
  return request.get('/api/dashboard/equipment-status')
}
