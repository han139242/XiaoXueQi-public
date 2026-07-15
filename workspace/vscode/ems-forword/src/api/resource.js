import request from '@/api/request'

export function getResourcePage(baseUrl, params, options = {}) {
  return request.get(`${baseUrl}/page`, { params, ...options })
}

export function getResourceList(url, params) {
  return request.get(url, { params })
}

export function getResourceDetail(baseUrl, id) {
  return request.get(`${baseUrl}/${id}`)
}

export function createResource(baseUrl, data, path = '') {
  return request.post(`${baseUrl}${path}`, data)
}

export function updateResource(baseUrl, data, path = '') {
  return request.put(`${baseUrl}${path}`, data)
}

export function deleteResource(baseUrl, id, path) {
  return request.delete(path || `${baseUrl}/${id}`)
}

export function executeResourceAction({ method = 'put', url, data, params }) {
  return request({ method, url, data, params })
}

export function exportResource(baseUrl, params) {
  return request.get(`${baseUrl}/excel`, { params, responseType: 'blob' })
}

export function downloadResource(url) {
  return request.get(url, { responseType: 'blob' })
}

export function uploadResource(url, formData) {
  return request.post(url, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
