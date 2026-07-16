import request from '@/api/request'

const AI_ADVICE_TIMEOUT = 125000
const BASE_URL = '/api/mfg/schedule-scenario'

export function getScheduleScenarioPage(params) {
  return request.get(`${BASE_URL}/page`, { params })
}

export function getScheduleScenarioDetail(scenarioId) {
  return request.get(`${BASE_URL}/${scenarioId}`)
}

export function getScheduleScenarioItems(scenarioId) {
  return request.get(`${BASE_URL}/${scenarioId}/items`)
}

export function openManualScheduleDraft() {
  return request.post(`${BASE_URL}/manual-draft/open`)
}

export function getManualEligibleLines(scenarioId, productionOrderId) {
  return request.get(`${BASE_URL}/${scenarioId}/orders/${productionOrderId}/eligible-lines`)
}

export function addManualScheduleItem(scenarioId, data) {
  return request.post(`${BASE_URL}/${scenarioId}/items`, data)
}

export function updateManualSchedulePlacement(scenarioId, scheduleItemId, data) {
  return request.put(`${BASE_URL}/${scenarioId}/items/${scheduleItemId}/placement`, data)
}

export function removeManualScheduleItem(scenarioId, scheduleItemId, data) {
  return request.delete(`${BASE_URL}/${scenarioId}/items/${scheduleItemId}`, { data })
}

export function validateManualScheduleDraft(scenarioId, data) {
  return request.post(`${BASE_URL}/${scenarioId}/validate`, data)
}

export function getManualOrderAiAdvice(scenarioId, productionOrderId, data) {
  return request.post(`${BASE_URL}/${scenarioId}/orders/${productionOrderId}/ai-advice`, data, { timeout: AI_ADVICE_TIMEOUT })
}

export function calculateScheduleScenario(data) {
  return request.post(`${BASE_URL}/calculate`, data)
}

export function generateScheduleAiAdvice(scenarioId, data) {
  return request.post(`${BASE_URL}/${scenarioId}/ai-advice`, data, { timeout: AI_ADVICE_TIMEOUT })
}

export function confirmScheduleScenario(scenarioId, data) {
  return request.put(`${BASE_URL}/${scenarioId}/confirm`, data)
}

export function getDraftProductionOrderPage(params) {
  return request.get('/api/mfg/production-order/page', { params: { ...params, status: 'RELEASED' } })
}
