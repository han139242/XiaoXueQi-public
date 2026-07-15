import request from '@/api/request'

export function getNotificationUnreadCount() {
  return request.get('/api/sys/notification/unread-count')
}

export function getNotificationPage(params = {}) {
  return request.get('/api/sys/notification/page', { params })
}

export function getNotificationDetail(recipientId) {
  return request.get(`/api/sys/notification/${recipientId}`)
}

export function markNotificationRead(recipientId) {
  return request.put(`/api/sys/notification/${recipientId}/read`)
}

export function markAllNotificationsRead(category) {
  return request.put('/api/sys/notification/read-all', null, { params: category ? { category } : {} })
}

export function acknowledgeNotification(recipientId) {
  return request.put(`/api/sys/notification/${recipientId}/ack`)
}
