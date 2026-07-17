// 设备详情页：展示设备基础信息和关联维修记录入口。
const guard = require('../../../utils/guard')
const auth = require('../../../utils/auth')
const equipmentApi = require('../../../api/equipment')
const lookupApi = require('../../../api/lookup')
const format = require('../../../utils/format')

const statusMap = {
  RUNNING: ['运行中', 'success'],
  IDLE: ['空闲', 'primary'],
  STOPPED: ['停机', 'warning'],
  FAULT: ['故障', 'danger'],
  MAINTENANCE: ['维修中', 'warning'],
  MAINTAINING: ['保养中', 'warning'],
  REPAIRING: ['维修中', 'warning'],
  SCRAPPED: ['已报废', 'neutral']
}

Page({
  data: {
    equipmentId: '',
    detail: null,
    rawDetail: null,
    canUpdate: false,
    loading: false,
    error: '',
    lookups: null
  },

  onLoad(options) {
    this.setData({ equipmentId: options && options.id ? decodeURIComponent(options.id) : '' })
    this.loadDetail()
  },

  onShow() {
    if (!guard.requireAuth()) return
    this.setData({ canUpdate: auth.hasPermission('eam:equipment:update') })
    if (this.data.equipmentId && !this.data.loading) this.loadDetail()
  },

  onPullDownRefresh() {
    this.loadDetail().finally(() => wx.stopPullDownRefresh())
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
      updatedTimeText: format.formatDateTime(item.updatedTime),
      remarkText: format.display(item.remark)
    }
  },

  async loadDetail() {
    if (this.data.loading) return
    if (!this.data.equipmentId) {
      this.setData({ error: '缺少设备 ID' })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const lookups = this.data.lookups || await this.ensureLookups()
      const detail = await equipmentApi.equipmentDetail(this.data.equipmentId)
      this.setData({
        rawDetail: detail || null,
        detail: detail ? this.decorate(detail, lookups) : null,
        error: detail ? '' : '设备不存在或已删除'
      })
    } catch (error) {
      this.setData({ error: error.message || '设备详情加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  updateStatus(event) {
    if (!guard.requirePermission('eam:equipment:update')) return
    const status = event.currentTarget.dataset.status
    const labelMap = {
      RUNNING: '标记运行',
      IDLE: '标记空闲',
      FAULT: '标记故障'
    }
    const label = labelMap[status] || '更新状态'
    wx.showModal({
      title: label,
      content: '确认更新设备状态？',
      success: async (result) => {
        if (!result.confirm) return
        try {
          await equipmentApi.updateEquipment({
            ...(this.data.rawDetail || {}),
            equipmentId: this.data.equipmentId,
            status,
            remark: '小程序' + label
          })
          wx.showToast({ title: '已更新', icon: 'success' })
          this.loadDetail()
        } catch (error) {
          wx.showToast({ title: error.message || '更新失败', icon: 'none' })
        }
      }
    })
  }
})
