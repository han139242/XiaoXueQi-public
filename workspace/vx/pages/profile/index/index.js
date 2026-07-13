const guard = require('../../../utils/guard')
const authApi = require('../../../api/auth')
const auth = require('../../../utils/auth')
const env = require('../../../config/env')
const format = require('../../../utils/format')

Page({
  data: {
    user: null,
    loading: true,
    error: '',
    envVersion: 'develop',
    baseUrl: ''
  },

  onShow() {
    if (!guard.requireAuth()) return
    this.refreshEnvironment()
    this.loadUser()
  },

  refreshEnvironment() {
    const envVersion = env.getEnvVersion()
    let baseUrl = ''
    try {
      baseUrl = env.getBaseUrl()
    } catch (error) {
      baseUrl = '未配置'
    }
    this.setData({ envVersion, baseUrl })
  },

  async loadUser() {
    this.setData({ loading: true, error: '' })
    try {
      const user = await authApi.info()
      auth.setUser(user)
      this.setData({
        user: {
          ...user,
          avatarText: user.username ? String(user.username).slice(0, 1).toUpperCase() : 'M',
          lastLoginTimeText: format.formatDateTime(user.lastLoginTime)
        },
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false, error: error.message || '用户信息加载失败' })
    }
  },

  setBaseUrl() {
    if (this.data.envVersion === 'release') return
    wx.showModal({
      title: '设置后端地址',
      editable: true,
      placeholderText: 'http://局域网地址:8088',
      content: this.data.baseUrl === '未配置' ? '' : this.data.baseUrl,
      success: (result) => {
        if (!result.confirm) return
        const value = String(result.content || '').trim()
        if (value && !/^https?:\/\/[^\s]+$/i.test(value)) {
          wx.showToast({ title: '请输入有效的 HTTP(S) 地址', icon: 'none' })
          return
        }
        env.setDebugBaseUrl(value)
        this.refreshEnvironment()
        wx.showToast({ title: value ? '地址已更新' : '已恢复默认地址', icon: 'success' })
      }
    })
  },

  clearBusinessCache() {
    const app = getApp()
    app.globalData.lookups = {}
    wx.showToast({ title: '业务缓存已清理', icon: 'success' })
  },

  logout() {
    wx.showModal({
      title: '退出登录',
      content: '确认退出当前 MES 账号？',
      success: async (result) => {
        if (!result.confirm) return
        try {
          await authApi.logout()
        } catch (error) {
          // 本地会话仍必须清理。
        } finally {
          auth.clearAuth()
          wx.reLaunch({ url: '/pages/login/index' })
        }
      }
    })
  }
})
