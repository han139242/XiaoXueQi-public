const guard = require('../../../utils/guard')
const qualityApi = require('../../../api/quality')
const format = require('../../../utils/format')

const dispositionMap = {
  rework: '返工',
  scrap: '报废',
  downgrade: '降级',
  concession: '让步接收'
}

const approvalMap = {
  PENDING: ['待审批', 'warning'],
  APPROVED: ['已通过', 'success'],
  REJECTED: ['已驳回', 'danger']
}

Page({
  data: {
    list: [],
    current: 1,
    size: 10,
    pages: 0,
    loading: false,
    finished: false,
    error: ''
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

  decorate(item) {
    const approval = approvalMap[item.approvalStatus] || [item.approvalStatus || '-', 'neutral']
    return {
      ...item,
      displayName: item.defectName || item.defectReason || '未填写不良品名称',
      displayNo: item.ncrNo || ('NCR-' + item.ncrId),
      createdByText: item.createdByName || item.creatorName || item.createByName || '超级管理员',
      imageUrl: item.imageUrl || item.pictureUrl || item.photoUrl || '',
      inspectionLabel: item.inspectionNo || (item.inspectionId ? '检验单 ID ' + item.inspectionId : '-'),
      dispositionText: dispositionMap[item.dispositionType] || item.dispositionType || '-',
      approvalText: approval[0],
      approvalTone: approval[1],
      createdTimeText: format.formatDateTime(item.createdTime)
    }
  },

  async loadPage(reset) {
    if (this.data.loading) return
    if (!reset && this.data.finished) return
    const current = reset ? 1 : this.data.current
    this.setData({ loading: true, error: reset ? '' : this.data.error })
    try {
      const page = await qualityApi.nonconformingPage({ current, size: this.data.size })
      const records = page && Array.isArray(page.records) ? page.records : []
      const pages = Number(page && page.pages) || 0
      this.setData({
        list: reset ? records.map((item) => this.decorate(item)) : this.data.list.concat(records.map((item) => this.decorate(item))),
        current: current + 1,
        pages,
        finished: pages === 0 || current >= pages,
        error: ''
      })
    } catch (error) {
      this.setData({ error: error.message || '不良品项加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  openForm() {
    wx.navigateTo({ url: '/pages/quality/defect-form/index' })
  },

  openDetail(event) {
    const ncrId = event.currentTarget.dataset.id
    if (!ncrId) return
    wx.navigateTo({
      url: '/pages/quality/defect-form/index?ncrId=' + encodeURIComponent(ncrId)
    })
  },

  refreshList() {
    this.loadPage(true)
  },

  showSearchTip() {
    wx.showToast({ title: '当前支持下拉刷新查看最新数据', icon: 'none' })
  }
})
