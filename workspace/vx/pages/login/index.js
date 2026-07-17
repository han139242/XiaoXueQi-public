const authApi = require('../../api/auth')
const auth = require('../../utils/auth')

function switchToWorkbench() {
  // 登录成功后进入工作台。这里单独封装，是为了把跳转失败也变成可读错误。
  return new Promise((resolve, reject) => {
    wx.switchTab({
      url: '/pages/workbench/index/index',
      success: resolve,
      fail(error) {
        const detail = String(error && error.errMsg || '').replace(/^switchTab:fail\s*/i, '')
        const message = detail ? '工作台打开失败：' + detail : '工作台打开失败，请重新编译小程序'
        const navigationError = new Error(message)
        navigationError.stage = 'navigation'
        reject(navigationError)
      }
    })
  })
}

Page({
  data: {
    username: '',
    password: '',
    checking: true,
    submitting: false,
    error: ''
  },

  onLoad() {
    // 打开登录页时先尝试恢复旧 token。
    // 如果 token 还有效，就直接进入工作台；无效则清理登录态。
    this.restoreSession()
  },

  async restoreSession() {
    if (!auth.getToken()) {
      this.setData({ checking: false })
      return
    }
    try {
      const user = await authApi.info({ skipAuthRedirect: true, silent: true })
      auth.setUser(user)
      await switchToWorkbench()
    } catch (error) {
      if (error.stage !== 'navigation') auth.clearAuth()
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
    let authEstablished = false
    try {
      // 登录分两步：先用账号密码换 token，再用 token 请求 /api/auth/info 获取用户、角色、权限。
      const response = await authApi.login({ username, password })
      if (!response.token || typeof response.token !== 'string') {
        throw new Error('登录响应缺少有效 Token')
      }
      const token = response.token.trim()
      auth.setToken(token)
      if (auth.getToken() !== token) {
        throw new Error('登录凭证保存失败，请清理小程序缓存后重试')
      }

      let user
      try {
        user = await authApi.info({ skipAuthRedirect: true, silent: true })
      } catch (error) {
        throw new Error('登录成功，但用户信息加载失败：' + (error.message || '未知错误'))
      }
      auth.setUser(user)
      authEstablished = true
      this.setData({ submitting: false, password: '' })
      await switchToWorkbench()
    } catch (error) {
      if (!authEstablished || error.stage !== 'navigation') auth.clearAuth()
      this.setData({
        submitting: false,
        password: '',
        error: error.message || '登录失败，请重试'
      })
    }
  }
})
