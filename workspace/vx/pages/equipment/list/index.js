const guard = require('../../../utils/guard')
const equipmentApi = require('../../../api/equipment')
const lookupApi = require('../../../api/lookup')
const format = require('../../../utils/format')

const statusMap = {
  RUNNING: ['运行中', 'success'],
  IDLE: ['空闲', 'primary'],
  STOPPED: ['停机', 'warning'],
  FAULT: ['故障', 'danger'],
  MAINTENANCE: ['维修中', 'warning'],
  SCRAPPED: ['已报废', 'neutral']
}

Page({
  data: {
    list: [],
    current: 1,
    size: 10,
    loading: false,
    finished: false,
    error: '',
    keyword: '',
    lookups: null
  },

  onLoad() {
    this.loadPage(true)
  },

  onShow() {
    if (!guard.requireAuth()) return
    if (!this.data.loading) this.loadPage(true)
  },

  onPullDownRefresh() {
    this.loadPage(true).finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    this.loadPage(false)
  },

  handleKeyword(event) {
    this.setData({ keyword: event.detail.value })
  },

  search() {
    this.loadPage(true)
  },

  clearKeyword() {
    if (!this.data.keyword) return
    this.setData({ keyword: '' })
    this.loadPage(true)
  },

  async ensureLookups() {
    const lookups = await lookupApi.load(['station'])
    this.setData({ lookups })
    return lookups
  },

  decorate(item, lookups) {
    const statusView = statusMap[item.status] || [item.status || '-', 'neutral']
    return {
      ...item,
      statusText: statusView[0],
      statusTone: statusView[1],
      stationLabel: lookupApi.label(lookups.station, item.stationId, '工位'),
      updatedTimeText: format.formatDateTime(item.updatedTime)
    }
  },

  async loadPage(reset) {
    if (this.data.loading) return
    if (!reset && this.data.finished) return
    const current = reset ? 1 : this.data.current
    this.setData({ loading: true, error: reset ? '' : this.data.error })
    try {
      const lookups = this.data.lookups || await this.ensureLookups()
      const page = await equipmentApi.equipmentPage(format.compactQuery({
        current,
        size: this.data.size,
        keyword: this.data.keyword.trim()
      }))
      const records = page && Array.isArray(page.records) ? page.records : []
      const decorated = records.map((item) => this.decorate(item, lookups))
      const pages = Number(page && page.pages) || 0
      this.setData({
        list: reset ? decorated : this.data.list.concat(decorated),
        current: current + 1,
        finished: pages === 0 || current >= pages,
        error: ''
      })
    } catch (error) {
      this.setData({ error: error.message || '设备台账加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  openDetail(event) {
    wx.navigateTo({ url: '/pages/equipment/detail/index?id=' + encodeURIComponent(event.currentTarget.dataset.id) })
  }
})
