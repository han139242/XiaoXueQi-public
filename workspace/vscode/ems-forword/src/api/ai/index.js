import request from '@/api/request'

export function getAiStatus() {
  return request.get('/api/ai/status')
}

export function chatWithAi(data, signal) {
  return request.post('/api/ai/chat', data, { signal })
}

export function getDashboardInsight(data, signal) {
  return request.post('/api/ai/dashboard-insight', data, { signal })
}

export function getModuleSuggestion(data, signal) {
  return request.post('/api/ai/module-suggestion', data, { signal })
}
