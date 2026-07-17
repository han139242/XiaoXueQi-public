// Axios 是真正发送 HTTP 请求的工具；这里创建全项目共用的 Axios 实例。
import axios from 'axios'
// ElMessage 用于把后端错误以消息提示的形式展示给用户。
import { ElMessage } from 'element-plus'
// 每次请求前读取 Token；登录失效时统一清理登录信息。
import { clearAuth, getToken } from '@/utils/auth'

// 记录上一次错误，避免同一个接口错误在短时间内连续弹出多次。
let lastMessage = ''
let lastMessageAt = 0

const GENERIC_FAILURE_MESSAGES = new Set(['操作失败', '业务处理异常', '请求处理失败'])

// 弹出错误消息，并在1.5秒内拦截完全相同的重复提示。
function showError(message) {
  const now = Date.now()
  if (message === lastMessage && now - lastMessageAt < 1500) {
    return
  }
  lastMessage = message
  lastMessageAt = now
  ElMessage.error(message)
}

// 后端只返回通用错误时，根据请求方法和接口动作补充更具体的操作建议。
function resolveOperationFallback(config) {
  const method = String(config?.method || '').toLowerCase()
  const url = String(config?.url || '').split('?', 1)[0]
  if (method === 'delete') {
    return '删除失败：记录可能不存在或已删除，也可能处于不可删除状态、已被其他业务引用；请刷新后检查关联数据'
  }
  if (url.endsWith('/withdraw')) {
    return '撤回失败：当前单据状态不允许撤回，或已产生不能直接回退的下游数据；请刷新后查看最新状态'
  }
  return ''
}

// 按“后端详细信息 -> ResultData.msg -> 前端操作兜底”的优先级选出最终错误文案。
function resolveResultMessage(result, config) {
  const detail = result?.data?.message
  const detailMessage = typeof detail === 'string' ? detail.trim() : ''
  if (detailMessage && !GENERIC_FAILURE_MESSAGES.has(detailMessage)) {
    return detailMessage
  }
  const message = typeof result?.msg === 'string' ? result.msg.trim() : ''
  if (message && !GENERIC_FAILURE_MESSAGES.has(message)) {
    return message
  }
  return resolveOperationFallback(config) || detailMessage || message || '请求处理失败'
}

// 创建统一请求实例。业务页面不直接使用原始 axios，而是统一导入这个 request。
const request = axios.create({
  // 开发时通常为空，请求使用 /api 相对路径并由 Vite 代理到 8088。
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  // 请求超过15秒仍未完成时按超时处理。
  timeout: 15000
})

// 请求拦截器：每个请求发出前自动执行，为已登录请求添加 JWT Token。
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    // 后端从 Authorization: Bearer <token> 中识别当前登录用户。
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：后端返回后统一解析 ResultData，业务页面不必重复判断code。
request.interceptors.response.use(
  (response) => {
    // 下载Excel或文件时返回的是二进制，不按普通JSON处理。
    if (response.config.responseType === 'blob') {
      return response
    }

    // 普通接口约定返回 { code, msg, data }。
    const result = response.data
    if (!result || typeof result.code === 'undefined') {
      return result
    }

    if (Number(result.code) === 200) {
      // code=200表示业务成功，直接把ResultData交给调用页面。
      return result
    }

    if (Number(result.code) === 1002 || Number(result.code) === 401) {
      // Token失效：清理本地登录信息，并跳回登录页，同时保留原目标地址。
      clearAuth()
      const redirect = encodeURIComponent(window.location.hash.slice(1) || '/dashboard')
      window.location.hash = `#/login?redirect=${redirect}`
    }

    const message = resolveResultMessage(result, response.config)
    const error = new Error(message)
    error.code = result.code
    error.data = result.data
    error.displayMessage = message
    if (!response.config.suppressErrorMessage) showError(message)
    return Promise.reject(error)
  },
  (error) => {
    // 第二个回调处理网络层错误，例如401、403、服务器异常和超时。
    if (axios.isCancel(error)) {
      return Promise.reject(error)
    }

    const status = error.response?.status
    let message = '网络连接失败，请检查后端服务'
    if (status === 401) {
      clearAuth()
      message = '登录状态已失效，请重新登录'
      window.location.hash = '#/login'
    } else if (status === 403) {
      message = '当前账号没有操作权限'
    } else if (status >= 500) {
      message = '服务暂时不可用，请稍后重试'
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时，请稍后重试'
    }
    error.displayMessage = message
    if (!error.config?.suppressErrorMessage) showError(message)
    return Promise.reject(error)
  }
)

// 默认导出统一Axios实例，其他API文件可以用任意本地名称import它。
export default request
