const guard = require('../../../utils/guard')
const auth = require('../../../utils/auth')
const reportApi = require('../../../api/work-report')
const lookupApi = require('../../../api/lookup')
const statusUtil = require('../../../utils/status')
const format = require('../../../utils/format')

const auditOptions = [
  { value: '', label: '全部审核状态' },
  { value: 'SUBMITTED', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' }
]

const sortOptions = [
  { value: 'default', label: '排序' },
  { value: 'newest', label: '最新报工' },
  { value: 'oldest', label: '最早报工' }
]

const scopeOptions = [
  { value: 'all', label: '全部报工' },
  { value: 'mine', label: '我的报工' }
]

Page({
  data: {
    list: [],
    current: 1,
    size: 10,
    pages: 0,
    loading: false,
    finished: false,
    error: '',
    keyword: '',
    auditStatus: '',
    auditOptions,
    auditIndex: 0,
    sortOptions,
    sortIndex: 0,
    scopeOptions,
    scopeIndex: 0,
    employeeId: null,
    myOnly: false,
    lookups: null,
    activeDropdown: '',
    dropdownOptions: []
  },

  onLoad() {
    const user = auth.getUser()
    const employeeId = user && user.employeeId
    this.setData({ employeeId: employeeId || null, myOnly: Boolean(employeeId), scopeIndex: employeeId ? 1 : 0 })
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

  handleAudit(event) {
    const index = Number(event.detail.value)
    this.setData({ auditIndex: index, auditStatus: auditOptions[index].value })
    this.loadPage(true)
  },

  toggleMyOnly() {
    if (!this.data.employeeId) return
    this.setData({ myOnly: !this.data.myOnly })
    this.loadPage(true)
  },

  handleSort(event) {
    const sortIndex = Number(event.detail.value)
    this.setData({ sortIndex, list: this.sortRecords(this.data.list, sortOptions[sortIndex].value) })
  },

  toggleDropdown(event) {
    const field = event.currentTarget.dataset.field
    if (this.data.activeDropdown === field) {
      this.closeDropdown()
      return
    }
    this.setData({
      activeDropdown: field,
      dropdownOptions: this.buildDropdownOptions(field)
    })
  },

  closeDropdown() {
    this.setData({ activeDropdown: '', dropdownOptions: [] })
  },

  buildDropdownOptions(field) {
    const indexMap = {
      audit: 'auditIndex',
      sort: 'sortIndex',
      scope: 'scopeIndex'
    }
    const optionsMap = {
      audit: this.data.auditOptions,
      sort: this.data.sortOptions,
      scope: this.data.scopeOptions
    }
    const selectedIndex = this.data[indexMap[field]]
    return (optionsMap[field] || []).map((item, index) => ({
      ...item,
      index,
      selected: index === selectedIndex
    }))
  },

  selectDropdown(event) {
    const field = event.currentTarget.dataset.field
    const index = Number(event.currentTarget.dataset.index)
    this.closeDropdown()
    if (field === 'audit') {
      this.setData({ auditIndex: index, auditStatus: auditOptions[index].value })
      this.loadPage(true)
      return
    }
    if (field === 'sort') {
      this.setData({ sortIndex: index, list: this.sortRecords(this.data.list, sortOptions[index].value) })
      return
    }
    if (field === 'scope') {
      if (scopeOptions[index].value === 'mine' && !this.data.employeeId) {
        wx.showToast({ title: '当前账号未关联员工', icon: 'none' })
        return
      }
      this.setData({ scopeIndex: index, myOnly: scopeOptions[index].value === 'mine' })
      this.loadPage(true)
    }
  },

  search() {
    this.loadPage(true)
  },

  openDetail(event) {
    wx.navigateTo({ url: '/pages/report/detail/index?id=' + encodeURIComponent(event.currentTarget.dataset.id) })
  },

  sortRecords(records, direction) {
    if (direction === 'default') return records
    return records.slice().sort((left, right) => {
      const a = new Date(left.reportTime || 0).getTime()
      const b = new Date(right.reportTime || 0).getTime()
      return direction === 'oldest' ? a - b : b - a
    })
  },

  async loadPage(reset) {
    if (this.data.loading) return
    if (!reset && this.data.finished) return
    const current = reset ? 1 : this.data.current
    this.setData({ loading: true, error: reset ? '' : this.data.error })
    try {
      const lookups = this.data.lookups || await lookupApi.load(['workOrder', 'station', 'employee'])
      if (!this.data.lookups) this.setData({ lookups })
      const query = format.compactQuery({
        current,
        size: this.data.size,
        keyword: this.data.keyword.trim(),
        auditStatus: this.data.auditStatus,
        operatorId: this.data.myOnly ? this.data.employeeId : ''
      })
      const page = await reportApi.page(query)
      const records = page && Array.isArray(page.records) ? page.records : []
      const decorated = records.map((item) => ({
        ...item,
        statusView: statusUtil.getStatus('audit', item.auditStatus),
        operatorLabel: lookupApi.label(lookups.employee, item.operatorId, '员工'),
        stationLabel: lookupApi.label(lookups.station, item.stationId, '工位'),
        reportTimeText: format.formatDateTime(item.reportTime)
      }))
      const pages = Number(page && page.pages) || 0
      this.setData({
        list: this.sortRecords(reset ? decorated : this.data.list.concat(decorated), sortOptions[this.data.sortIndex].value),
        current: current + 1,
        pages,
        finished: pages === 0 || current >= pages,
        error: ''
      })
    } catch (error) {
      this.setData({ error: error.message || '报工记录加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  }
})
