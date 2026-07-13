const auth = require('./utils/auth')

App({
  onLaunch() {
    this.globalData.user = auth.getUser()
  },
  globalData: {
    user: null,
    lookups: {}
  }
})
