const authApi = require('../../api/auth')
const auth = require('../../utils/auth')

Page({
  data: {
    username: '',
    password: '',
    checking: true,
    submitting: false,
    error: ''
  },

  onLoad() {
    this.restoreSession()
  },

  async restoreSession() {
    if (!auth.getToken()) {
      this.setData({ checking: false })
      return
    }
    try {
      const user = await authApi.info()
      auth.setUser(user)
      wx.switchTab({ url: '/pages/workbench/index/index' })
    } catch (error) {
      auth.clearAuth()
      this.setData({
        checking: false,
        error: error.message || '会话校验失败，请重新登录'
      })
    }
  },

  handleInput(event) {
    const field = event.currentTarget.dataset.field
    this.setData({ [field]: event.detail.value, error: '' })
  },

  async handleSubmit() {
    if (this.data.submitting) return
    const username = this.data.username.trim()
    const password = this.data.password
    if (!username || !password) {
      this.setData({ error: '请输入用户名和密码' })
      return
    }

    this.setData({ submitting: true, error: '' })
    try {
      const response = await authApi.login({ username, password })
      if (!response.token || typeof response.token !== 'string') {
        throw new Error('登录响应缺少有效 Token')
      }
      auth.setToken(response.token)
      const user = await authApi.info()
      auth.setUser(user)
      this.setData({ password: '' })
      wx.switchTab({ url: '/pages/workbench/index/index' })
    } catch (error) {
      auth.clearAuth()
      this.setData({
        submitting: false,
        password: '',
        error: error.message || '登录失败，请重试'
      })
    }
  }
})
