import request from '@/api/request'

// 通用资源接口层：把各业务页面重复使用的 CRUD 请求集中封装起来。
// baseUrl 由资源配置提供，例如 /api/mfg/production-order；真正发送请求、添加 Token
// 和处理 ResultData 的工作仍由 request.js 完成。
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

// release、withdraw、audit 等非普通 CRUD 操作的请求方法和地址不固定，统一走此入口。
export function executeResourceAction({ method = 'put', url, data, params }) {
  return request({ method, url, data, params })
}

// blob 表示后端返回的是 Excel、图片等二进制文件，而不是普通 JSON。
export function exportResource(baseUrl, params) {
  return request.get(`${baseUrl}/excel`, { params, responseType: 'blob' })
}

export function downloadResource(url) {
  return request.get(url, { responseType: 'blob' })
}

export function uploadResource(url, formData) {
  // multipart/form-data 用来同时提交文件内容和业务字段。
  return request.post(url, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
