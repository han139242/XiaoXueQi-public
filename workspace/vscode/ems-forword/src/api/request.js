import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearAuth, getToken } from '@/utils/auth'

let lastMessage = ''
let lastMessageAt = 0

function showError(message) {
  const now = Date.now()
  if (message === lastMessage && now - lastMessageAt < 1500) {
    return
  }
  lastMessage = message
  lastMessageAt = now
  ElMessage.error(message)
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response
    }

    const result = response.data
    if (!result || typeof result.code === 'undefined') {
      return result
    }

    if (Number(result.code) === 200) {
      return result
    }

    if (Number(result.code) === 1002 || Number(result.code) === 401) {
      clearAuth()
      const redirect = encodeURIComponent(window.location.hash.slice(1) || '/dashboard')
      window.location.hash = `#/login?redirect=${redirect}`
    }

    const error = new Error(result.msg || '请求处理失败')
    error.code = result.code
    error.data = result.data
    showError(error.message)
    return Promise.reject(error)
  },
  (error) => {
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
    showError(message)
    return Promise.reject(error)
  }
)

export default request
