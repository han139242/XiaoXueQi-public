const guard = require('../../../utils/guard')
const warehouseApi = require('../../../api/warehouse')
const lookupApi = require('../../../api/lookup')
const format = require('../../../utils/format')

Page({
  data: {
    list: [],
    current: 1,
    size: 10,
    loading: false,
    finished: false,
    error: '',
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

  async ensureLookups() {
    const lookups = await lookupApi.load(['material', 'warehouse', 'location', 'batch'])
    this.setData({ lookups })
    return lookups
  },

  decorate(item, lookups) {
    const availableQuantity = Number(item.availableQuantity || 0)
    const lockedQuantity = Number(item.lockedQuantity || 0)
    const inspectionQuantity = Number(item.inspectionQuantity || 0)
    const totalQuantity = Number((availableQuantity + lockedQuantity + inspectionQuantity).toFixed(4))
    const statusView = inspectionQuantity > 0
      ? ['存在待检库存', 'warning']
      : lockedQuantity > 0
        ? ['存在锁定库存', 'primary']
        : ['库存可用', 'success']
    return {
      ...item,
      materialLabel: lookupApi.label(lookups.material, item.materialId, '物料'),
      warehouseLabel: lookupApi.label(lookups.warehouse, item.warehouseId, '仓库'),
      locationLabel: lookupApi.label(lookups.location, item.locationId, '库位'),
      batchLabel: lookupApi.label(lookups.batch, item.batchId, '批次'),
      totalQuantity,
      updatedTimeText: format.formatDateTime(item.updatedTime),
      statusText: statusView[0],
      statusTone: statusView[1]
    }
  },

  async loadPage(reset) {
    if (this.data.loading) return
    if (!reset && this.data.finished) return
    const current = reset ? 1 : this.data.current
    this.setData({ loading: true, error: reset ? '' : this.data.error })
    try {
      const lookups = this.data.lookups || await this.ensureLookups()
      const page = await warehouseApi.inventoryPage({ current, size: this.data.size })
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
      this.setData({ error: error.message || '库存余额加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  }
})
