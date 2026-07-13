const guard = require('../../../utils/guard')
const authApi = require('../../../api/auth')
const auth = require('../../../utils/auth')
const format = require('../../../utils/format')

Page({
  data: {
    user: null,
    loading: true,
    error: '',
    showAccount: false
  },

  onShow() {
    if (!guard.requireAuth()) return
    this.loadUser()
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

  toggleAccount() {
    this.setData({ showAccount: !this.data.showAccount })
  },

  showAbout() {
    wx.showModal({ title: '关于 MES', content: '羽球智造 MES 为生产现场提供工单、任务与报工协同。', showCancel: false })
  },

  showHelp() {
    wx.showModal({ title: '帮助中心', content: '可在工单、任务和报工页面查看生产执行信息；下拉页面可刷新数据。', showCancel: false })
  },

  showCompany() {
    wx.showModal({ title: '关于我们', content: '羽球智造 MES，面向羽毛球制造现场的生产管理系统。', showCancel: false })
  },

  showPasswordHint() {
    wx.showModal({ title: '修改密码', content: '请联系系统管理员通过 PC 管理端完成密码重置。', showCancel: false })
  },

  switchAccount() {
    wx.showModal({
      title: '切换登录',
      content: '切换账号将退出当前会话，是否继续？',
      success: (result) => {
        if (!result.confirm) return
        auth.clearAuth()
        wx.reLaunch({ url: '/pages/login/index' })
      }
    })
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
