import request from '@/api/request'

const AI_GENERATION_TIMEOUT = 125000

export function getAiStatus() {
  return request.get('/api/ai/status')
}

export function chatWithAi(data, signal) {
  return request.post('/api/ai/chat', data, { signal, timeout: AI_GENERATION_TIMEOUT })
}

export function getDashboardInsight(data, signal) {
  return request.post('/api/ai/dashboard-insight', data, { signal, timeout: AI_GENERATION_TIMEOUT })
}

export function getModuleSuggestion(data, signal) {
  return request.post('/api/ai/module-suggestion', data, { signal, timeout: AI_GENERATION_TIMEOUT })
}
