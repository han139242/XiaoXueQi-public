const guard = require('../../../utils/guard')
const auth = require('../../../utils/auth')
const equipmentApi = require('../../../api/equipment')
const lookupApi = require('../../../api/lookup')
const format = require('../../../utils/format')

const statusMap = {
  SUBMITTED: ['待派工', 'warning'],
  ASSIGNED: ['已派工', 'primary'],
  PROCESSING: ['维修中', 'warning'],
  CLOSED: ['已关闭', 'success'],
  CANCELLED: ['已取消', 'neutral']
}

Page({
  data: {
    repairId: '',
    detail: null,
    loading: false,
    error: '',
    lookups: null
  },

  onLoad(options) {
    this.setData({ repairId: options && options.id ? decodeURIComponent(options.id) : '' })
    this.loadDetail()
  },

  onShow() {
    if (!guard.requireAuth()) return
    if (this.data.repairId && !this.data.loading) this.loadDetail()
  },

  onPullDownRefresh() {
    this.loadDetail().finally(() => wx.stopPullDownRefresh())
  },

  async ensureLookups() {
    const lookups = await lookupApi.load(['equipment', 'employee'])
    this.setData({ lookups })
    return lookups
  },

  decorate(item, lookups) {
    const statusView = statusMap[item.status] || [item.status || '-', 'neutral']
    return {
      ...item,
      statusText: statusView[0],
      statusTone: statusView[1],
      equipmentLabel: lookupApi.label(lookups.equipment, item.equipmentId, '设备'),
      applicantLabel: lookupApi.label(lookups.employee, item.applicantId, '员工'),
      repairerLabel: item.repairerId ? lookupApi.label(lookups.employee, item.repairerId, '员工') : '未派工',
      reportedTimeText: format.formatDateTime(item.reportedTime),
      assignedTimeText: format.formatDateTime(item.assignedTime),
      closedTimeText: format.formatDateTime(item.closedTime),
      faultText: format.display(item.faultDescription),
      resultText: format.display(item.repairResult, '未填写'),
      remarkText: format.display(item.remark)
    }
  },

  async loadDetail() {
    if (this.data.loading) return
    if (!this.data.repairId) {
      this.setData({ error: '缺少维修单 ID' })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const lookups = this.data.lookups || await this.ensureLookups()
      const detail = await equipmentApi.repairDetail(this.data.repairId)
      this.setData({
        detail: detail ? this.decorate(detail, lookups) : null,
        error: detail ? '' : '维修单不存在或已删除'
      })
    } catch (error) {
      this.setData({ error: error.message || '维修详情加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  assignRepair() {
    wx.showModal({
      title: '派工维修',
      content: '确认将该维修单派给当前账号对应人员？',
      success: async (result) => {
        if (!result.confirm) return
        try {
          const user = auth.getUser() || {}
          await equipmentApi.assignRepair(this.data.repairId, {
            repairerId: user.employeeId || 1002,
            remark: '小程序派工'
          })
          wx.showToast({ title: '已派工', icon: 'success' })
          this.loadDetail()
        } catch (error) {
          wx.showToast({ title: error.message || '派工失败', icon: 'none' })
        }
      }
    })
  },

  closeRepair() {
    wx.showModal({
      title: '关闭维修单',
      content: '确认维修已完成并关闭该维修单？',
      success: async (result) => {
        if (!result.confirm) return
        try {
          await equipmentApi.closeRepair(this.data.repairId, {
            repairResult: '小程序确认维修完成',
            repairCost: 0
          })
          wx.showToast({ title: '已关闭', icon: 'success' })
          this.loadDetail()
        } catch (error) {
          wx.showToast({ title: error.message || '关闭失败', icon: 'none' })
        }
      }
    })
  }
})
