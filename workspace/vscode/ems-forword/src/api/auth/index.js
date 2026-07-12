import request from '@/api/request'

export function login(data) {
  return request({
    url: '/api/auth/login',
    method: 'post',
    data
  })
}

export function getCurrentUserInfo() {
  return request({
    url: '/api/auth/info',
    method: 'get'
  })
}

export function logout() {
  return request({
    url: '/api/auth/logout',
    method: 'post'
  })
}
