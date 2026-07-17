import request from '@/api/request'

// AI生成可能耗时较长，单独把超时提高到125秒；普通请求仍使用request.js的默认值。
const AI_ADVICE_TIMEOUT = 125000
// 同一组排产接口共享前缀，修改接口根路径时只需调整一处。
const BASE_URL = '/api/mfg/schedule-scenario'

// 分页查询历史排产方案，params保存页码和筛选条件。
export function getScheduleScenarioPage(params) {
  return request.get(`${BASE_URL}/page`, { params })
}

// 根据方案ID查询方案头信息。
export function getScheduleScenarioDetail(scenarioId) {
  return request.get(`${BASE_URL}/${scenarioId}`)
}

// 查询指定方案包含的所有排产明细。
export function getScheduleScenarioItems(scenarioId) {
  return request.get(`${BASE_URL}/${scenarioId}/items`)
}

// 打开或创建当前人工排产草稿，后端保证用户能够继续上一次编辑。
export function openManualScheduleDraft() {
  return request.post(`${BASE_URL}/manual-draft/open`)
}

// 查询人工排产工作台整块看板数据，包括待排订单、产线和已放置任务。
export function getManualScheduleBoard() {
  return request.get(`${BASE_URL}/manual-board`)
}

// 针对整张人工排产看板生成AI建议。
export function getManualBoardAiAdvice() {
  return request.post(`${BASE_URL}/manual-board/ai-advice`, undefined, { timeout: AI_ADVICE_TIMEOUT })
}

// 对当前真正未排产的DRAFT订单生成一批可撤回的AI排产草案。
export function generateManualAiBatch() {
  return request.post(`${BASE_URL}/manual-board/ai-batch`, undefined, { suppressErrorMessage: true })
}

// 撤回本次AI一键排产草案；只撤回未确认时间块。
export function withdrawManualAiBatch(scenarioId, data) {
  return request.delete(`${BASE_URL}/manual-board/ai-batch/${scenarioId}`, { data, suppressErrorMessage: true })
}

// 将本次AI一键排产草案整体确认成正式生产计划。
export function confirmManualAiBatch(scenarioId, data) {
  return request.put(`${BASE_URL}/manual-board/ai-batch/${scenarioId}/confirm`, data, { suppressErrorMessage: true })
}

// 查询某订单可放置的产线；禁止通用错误弹窗，由工作台在具体位置展示错误。
export function getManualEligibleLines(scenarioId, productionOrderId) {
  return request.get(`${BASE_URL}/${scenarioId}/orders/${productionOrderId}/eligible-lines`, { suppressErrorMessage: true })
}

// 向草稿新增一条排产项，data通过POST请求体提交。
export function addManualScheduleItem(scenarioId, data) {
  return request.post(`${BASE_URL}/${scenarioId}/items`, data, { suppressErrorMessage: true })
}

// 修改排产项所在产线和起止时间等位置数据。
export function updateManualSchedulePlacement(scenarioId, scheduleItemId, data) {
  return request.put(`${BASE_URL}/${scenarioId}/items/${scheduleItemId}/placement`, data, { suppressErrorMessage: true })
}

// 删除草稿排产项；Axios的DELETE请求体必须放在配置对象的data属性中。
export function removeManualScheduleItem(scenarioId, scheduleItemId, data) {
  return request.delete(`${BASE_URL}/${scenarioId}/items/${scheduleItemId}`, { data, suppressErrorMessage: true })
}

// 单独确认一条人工排产结果，使该项进入后续业务阶段。
export function confirmManualScheduleItem(scenarioId, scheduleItemId, data) {
  return request.put(`${BASE_URL}/${scenarioId}/items/${scheduleItemId}/confirm`, data, { suppressErrorMessage: true })
}

// 提交整个草稿给后端做产能冲突、时间重叠和数据完整性校验。
export function validateManualScheduleDraft(scenarioId, data) {
  return request.post(`${BASE_URL}/${scenarioId}/validate`, data)
}

// 只针对某一张生产订单生成AI排产建议。
export function getManualOrderAiAdvice(scenarioId, productionOrderId, data) {
  return request.post(`${BASE_URL}/${scenarioId}/orders/${productionOrderId}/ai-advice`, data, { timeout: AI_ADVICE_TIMEOUT })
}

// 由后端算法根据订单、产线和产能自动计算一个排产方案。
export function calculateScheduleScenario(data) {
  return request.post(`${BASE_URL}/calculate`, data)
}

// 对已经计算的方案生成解释、风险和调整建议。
export function generateScheduleAiAdvice(scenarioId, data) {
  return request.post(`${BASE_URL}/${scenarioId}/ai-advice`, data, { timeout: AI_ADVICE_TIMEOUT })
}

// 确认整个排产方案，后端负责状态流转和后续生产数据生成。
export function confirmScheduleScenario(scenarioId, data) {
  return request.put(`${BASE_URL}/${scenarioId}/confirm`, data)
}

// 工作台只加载已提交排产的订单，因此在调用方参数上固定追加RELEASED状态。
export function getDraftProductionOrderPage(params) {
  return request.get('/api/mfg/production-order/page', { params: { ...params, status: 'RELEASED' } })
}
