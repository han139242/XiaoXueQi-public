// 登录相关接口也复用统一request，因此会自动获得错误处理和Token拦截能力。
import request from '@/api/request'

// 提交用户名和密码，后端验证成功后返回JWT Token及登录信息。
export function login(data) {
  return request({
    url: '/api/auth/login',
    method: 'post',
    data
  })
}

// 根据当前请求携带的Token，从后端获取用户、角色和权限列表。
export function getCurrentUserInfo() {
  return request({
    url: '/api/auth/info',
    method: 'get'
  })
}

// 通知后端注销当前Token；前端还会通过auth.js清理Cookie和用户缓存。
export function logout() {
  return request({
    url: '/api/auth/logout',
    method: 'post'
  })
}
