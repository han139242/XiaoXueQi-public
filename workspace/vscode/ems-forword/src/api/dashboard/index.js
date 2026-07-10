import request from '@/api/request'

export function getDashboardSummary(params) {
  return request.get('/api/dashboard/summary', { params })
}

export function getProductionTrend(params) {
  return request.get('/api/dashboard/production-trend', { params })
}

export function getQualityStatistics(params) {
  return request.get('/api/dashboard/quality-statistics', { params })
}

export function getEquipmentStatus() {
  return request.get('/api/dashboard/equipment-status')
}
