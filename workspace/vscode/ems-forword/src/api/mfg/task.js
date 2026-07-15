import request from '@/api/request'

export function getTaskReminderSummary() {
  return request.get('/api/mfg/task/reminder-summary')
}
