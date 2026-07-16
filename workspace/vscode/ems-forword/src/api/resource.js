import request from '@/api/request'

// 通用资源接口层：把各业务页面重复使用的 CRUD 请求集中封装起来。
// baseUrl 由资源配置提供，例如 /api/mfg/production-order；真正发送请求、添加 Token
// 和处理 ResultData 的工作仍由 request.js 完成。
// 分页查询：自动在资源根路径后拼接/page，params会被Axios转换成?current=1&size=20等查询参数。
export function getResourcePage(baseUrl, params, options = {}) {
  return request.get(`${baseUrl}/page`, { params, ...options })
}

// 列表查询：url由调用方提供，适合树结构、主从明细等不一定带/page的接口。
export function getResourceList(url, params) {
  return request.get(url, { params })
}

// 详情查询：把主键id拼到baseUrl后，例如GET /api/sys/role/10。
export function getResourceDetail(baseUrl, id) {
  return request.get(`${baseUrl}/${id}`)
}

// 新增资源：POST负责创建数据，data作为JSON请求体发送；path可覆盖/full等特殊新增地址。
export function createResource(baseUrl, data, path = '') {
  return request.post(`${baseUrl}${path}`, data)
}

// 修改资源：PUT表示更新已有记录，data中通常包含主键、修改字段和乐观锁version。
export function updateResource(baseUrl, data, path = '') {
  return request.put(`${baseUrl}${path}`, data)
}

// 删除资源：默认请求baseUrl/id；少数特殊接口可以直接传入完整path覆盖默认地址。
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

// 下载文件：responseType设为blob，阻止Axios把二进制内容当作ResultData JSON解析。
export function downloadResource(url) {
  return request.get(url, { responseType: 'blob' })
}

// 上传文件：formData中保存文件和关联业务字段，后端通过MultipartFile接收。
export function uploadResource(url, formData) {
  // multipart/form-data 用来同时提交文件内容和业务字段。
  return request.post(url, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
