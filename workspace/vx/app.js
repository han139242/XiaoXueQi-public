const auth = require('./utils/auth')

App({
  // 小程序冷启动时执行一次。这里把本地缓存中的用户信息恢复到全局数据，
  // 这样页面进入后可以直接判断当前账号、角色和权限。
  onLaunch() {
    this.globalData.user = auth.getUser()
  },
  // globalData 是小程序级共享状态，适合放登录用户和基础字典缓存。
  // 页面自己的业务数据仍放在 Page({ data }) 中，避免互相污染。
  globalData: {
    user: null,
    lookups: {}
  }
})
