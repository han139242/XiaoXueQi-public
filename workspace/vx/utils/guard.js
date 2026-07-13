const auth = require('./auth')
const env = require('../config/env')
const mockData = require('../mock/data')

function ensureMockAuth() {
  if (!env.useMockData()) return false
  if (!auth.getToken()) auth.setToken('mock-token')
  if (!auth.getUser()) auth.setUser(mockData.currentUser)
  return true
}

function requireAuth() {
  if (ensureMockAuth()) return true
  if (auth.getToken()) return true
  wx.reLaunch({ url: '/pages/login/index' })
  return false
}

function requireEmployee() {
  const user = auth.getUser()
  if (user && user.employeeId) return user.employeeId
  wx.showModal({
    title: '无法执行现场操作',
    content: '当前账号未关联员工档案，请联系管理员。',
    showCancel: false
  })
  return null
}

module.exports = {
  requireAuth,
  requireEmployee
}
