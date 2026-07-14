const request = require('../utils/request')
const env = require('../config/env')

const now = '2026-07-13T09:30:00'
const UNQUALIFIED_RATE_LIMIT = 0.005

function currentLocalTime() {
  const date = new Date()
  const pad = (value) => value < 10 ? '0' + value : String(value)
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate())
    + 'T' + pad(date.getHours()) + ':' + pad(date.getMinutes()) + ':' + pad(date.getSeconds())
}

let fallbackInspectionOrders = [
  {
    inspectionId: 10001,
    inspectionNo: 'IQC-20260713-001',
    inspectionType: 'final',
    workOrderId: 5001,
    taskId: 8001,
    taskNo: 'TASK-20260713-001',
    reportId: 9001,
    reportNo: 'REP-20260713-001',
    inspectorId: 1003,
    sampleQuantity: 0,
    qualifiedQuantity: 0,
    unqualifiedQuantity: 0,
    inspectionTime: null,
    status: 'WAIT_INSPECTION',
    inspectionResult: '',
    remark: '自动线 A 首批终检',
    createdTime: '2026-07-13T09:20:00',
    updatedTime: '2026-07-13T09:20:00'
  },
  {
    inspectionId: 10002,
    inspectionNo: 'PQC-20260713-002',
    inspectionType: 'process',
    workOrderId: 5002,
    taskId: 8003,
    taskNo: 'TASK-20260713-003',
    reportId: null,
    reportNo: '',
    inspectorId: 1003,
    sampleQuantity: 0,
    qualifiedQuantity: 0,
    unqualifiedQuantity: 0,
    inspectionTime: null,
    status: 'WAIT_INSPECTION',
    inspectionResult: '',
    remark: '绕线工序巡检',
    createdTime: '2026-07-13T09:26:00',
    updatedTime: '2026-07-13T09:26:00'
  },
  {
    inspectionId: 10003,
    inspectionNo: 'FQC-20260712-003',
    inspectionType: 'final',
    workOrderId: 5003,
    taskId: 8004,
    taskNo: 'TASK-20260712-004',
    reportId: 9002,
    reportNo: 'REP-20260712-002',
    inspectorId: 1003,
    sampleQuantity: 100,
    qualifiedQuantity: 100,
    unqualifiedQuantity: 0,
    inspectionTime: '2026-07-12T18:25:00',
    status: 'FINISHED',
    inspectionResult: 'PASS',
    remark: '成品抽检合格',
    createdTime: '2026-07-12T18:10:00',
    updatedTime: '2026-07-12T18:25:00'
  },
  {
    inspectionId: 10004,
    inspectionNo: 'PQC-20260712-004',
    inspectionType: 'process',
    workOrderId: 5002,
    taskId: 8003,
    taskNo: 'TASK-20260713-003',
    reportId: 9002,
    reportNo: 'REP-20260712-002',
    inspectorId: 1003,
    sampleQuantity: 100,
    qualifiedQuantity: 92,
    unqualifiedQuantity: 8,
    inspectionTime: '2026-07-12T17:58:00',
    status: 'FINISHED',
    inspectionResult: 'FAIL',
    remark: '绕线外观复检不合格',
    createdTime: '2026-07-12T17:40:00',
    updatedTime: '2026-07-12T17:58:00'
  }
]

let fallbackNonconformingOrders = [
  {
    ncrId: 11001,
    ncrNo: 'BL202212152037000001',
    inspectionId: 10004,
    inspectionNo: 'PQC-20260712-004',
    productLotId: 0,
    defectReason: '测试',
    dispositionType: 'REWORK',
    approvalStatus: 'PENDING',
    status: 'OPEN',
    createdByName: '超级管理员',
    imageUrl: '',
    remark: '抽检发现局部胶量不足',
    createdTime: '2022-12-15T20:37:00',
    updatedTime: '2022-12-15T20:37:00'
  },
  {
    ncrId: 11002,
    ncrNo: '111',
    inspectionId: 10002,
    inspectionNo: 'PQC-20260713-002',
    productLotId: 0,
    defectReason: '111',
    dispositionType: 'rework',
    approvalStatus: 'PENDING',
    status: 'OPEN',
    createdByName: '超级管理员',
    imageUrl: '',
    remark: '',
    createdTime: '2022-12-15T19:40:11',
    updatedTime: '2022-12-15T19:40:11'
  },
  {
    ncrId: 11003,
    ncrNo: '202208231513080001',
    inspectionId: 10002,
    inspectionNo: 'PQC-20260713-002',
    productLotId: 0,
    defectReason: '涂料不均匀',
    dispositionType: 'rework',
    approvalStatus: 'PENDING',
    status: 'OPEN',
    createdByName: '超级管理员',
    imageUrl: '',
    remark: '',
    createdTime: '2022-08-23T15:13:08',
    updatedTime: '2022-08-23T15:13:08'
  },
  {
    ncrId: 11004,
    ncrNo: '202207281421250001',
    inspectionId: 10001,
    inspectionNo: 'IQC-20260713-001',
    productLotId: 0,
    defectReason: '气压不良',
    dispositionType: 'rework',
    approvalStatus: 'PENDING',
    status: 'OPEN',
    createdByName: '超级管理员',
    imageUrl: '',
    remark: '',
    createdTime: '2022-07-28T14:21:25',
    updatedTime: '2022-07-28T14:21:25'
  }
]

function clone(value) {
  return JSON.parse(JSON.stringify(value))
}

function compact(value) {
  return value !== '' && value !== null && value !== undefined
}

function contains(value, keyword) {
  if (!compact(keyword)) return true
  return String(value || '').toLowerCase().includes(String(keyword).toLowerCase())
}

function paginate(records, query) {
  const current = Math.max(1, Number(query.current || 1))
  const size = Math.max(1, Number(query.size || 10))
  const start = (current - 1) * size
  return {
    records: clone(records.slice(start, start + size)),
    total: records.length,
    size,
    current,
    pages: Math.ceil(records.length / size)
  }
}

function withMockFallback(remoteRequest, fallback) {
  return remoteRequest().catch((error) => {
    if (env.useMockData()) return Promise.resolve().then(fallback)
    throw error
  })
}

function fallbackInspectionPage(data) {
  const query = data || {}
  const records = fallbackInspectionOrders.filter((item) => {
    if (compact(query.status) && item.status !== query.status) return false
    if (compact(query.inspectionType) && item.inspectionType !== query.inspectionType) return false
    if (compact(query.inspectionResult) && item.inspectionResult !== query.inspectionResult) return false
    if (compact(query.workOrderId) && String(item.workOrderId) !== String(query.workOrderId)) return false
    return contains(item.inspectionNo, query.keyword) || contains(item.taskNo, query.keyword) || contains(item.reportNo, query.keyword)
  })
  return paginate(records, query)
}

function fallbackInspectionDetail(inspectionId) {
  const inspection = fallbackInspectionOrders.find((item) => String(item.inspectionId) === String(inspectionId))
  if (!inspection) throw new Error('检验单不存在')
  return clone(inspection)
}

function fallbackNonconformingPage(data) {
  const query = data || {}
  const records = fallbackNonconformingOrders.filter((item) => {
    if (compact(query.inspectionId) && String(item.inspectionId) !== String(query.inspectionId)) return false
    if (compact(query.approvalStatus) && item.approvalStatus !== query.approvalStatus) return false
    if (compact(query.status) && item.status !== query.status) return false
    return true
  })
  return paginate(records, query)
}

function fallbackNonconformingDetail(ncrId) {
  const order = fallbackNonconformingOrders.find((item) => String(item.ncrId) === String(ncrId))
  if (!order) throw new Error('不良品项不存在')
  return clone(order)
}

function fallbackSubmitInspection(inspectionId, data) {
  const inspection = fallbackInspectionOrders.find((item) => String(item.inspectionId) === String(inspectionId))
  if (!inspection) throw new Error('检验单不存在')
  const quantitiesProvided = data && (compact(data.sampleQuantity) || compact(data.qualifiedQuantity) || compact(data.unqualifiedQuantity))
  if (quantitiesProvided) {
    const sampleQuantity = Number(data.sampleQuantity)
    const qualifiedQuantity = Number(data.qualifiedQuantity)
    const unqualifiedQuantity = Number(data.unqualifiedQuantity)
    if (!Number.isFinite(sampleQuantity) || sampleQuantity <= 0
      || !Number.isFinite(qualifiedQuantity) || qualifiedQuantity < 0
      || !Number.isFinite(unqualifiedQuantity) || unqualifiedQuantity < 0
      || Math.abs(qualifiedQuantity + unqualifiedQuantity - sampleQuantity) > 0.000001) {
      throw new Error('检验数量不符合要求')
    }
    inspection.sampleQuantity = sampleQuantity
    inspection.qualifiedQuantity = qualifiedQuantity
    inspection.unqualifiedQuantity = unqualifiedQuantity
    inspection.inspectionResult = unqualifiedQuantity / sampleQuantity <= UNQUALIFIED_RATE_LIMIT ? 'PASS' : 'FAIL'
  } else {
    inspection.inspectionResult = data && data.inspectionResult ? data.inspectionResult : 'PASS'
  }
  inspection.status = 'FINISHED'
  inspection.remark = data && data.remark ? data.remark : inspection.remark
  inspection.inspectionTime = currentLocalTime()
  inspection.updatedTime = inspection.inspectionTime
  return clone(inspection)
}

function fallbackCreateNonconforming(data) {
  if (!data || !compact(data.inspectionId)) throw new Error('检验单不能为空')
  if (!compact(data.defectReason)) throw new Error('不良原因不能为空')
  const inspection = fallbackInspectionOrders.find((item) => String(item.inspectionId) === String(data.inspectionId))
  if (!inspection) throw new Error('检验单不存在')
  if (inspection.status !== 'FINISHED' || inspection.inspectionResult !== 'FAIL') {
    throw new Error('只能关联已完成且不合格的检验单')
  }
  const dispositionType = String(data.dispositionType || '').toUpperCase()
  if (!['REWORK', 'SCRAP', 'DOWNGRADE', 'CONCESSION'].includes(dispositionType)) {
    throw new Error('请选择有效的处理方式')
  }
  const ncrId = fallbackNonconformingOrders.length
    ? Math.max.apply(null, fallbackNonconformingOrders.map((item) => item.ncrId)) + 1
    : 11001
  const createdTime = currentLocalTime()
  const order = {
    ncrId,
    ncrNo: 'NCR-20260713-' + String(ncrId).slice(-3),
    inspectionId: Number(data.inspectionId),
    inspectionNo: inspection.inspectionNo,
    productLotId: data.productLotId ? Number(data.productLotId) : 0,
    defectReason: String(data.defectReason || '').trim(),
    dispositionType,
    approvalStatus: 'PENDING',
    status: 'OPEN',
    createdByName: '超级管理员',
    imageUrl: data.imageUrl || '',
    remark: data.remark || '',
    createdTime,
    updatedTime: createdTime
  }
  fallbackNonconformingOrders = [order].concat(fallbackNonconformingOrders)
  inspection.inspectionResult = 'FAIL'
  inspection.status = 'FINISHED'
  inspection.updatedTime = now
  return clone(order)
}

function fallbackUpdateNonconforming(ncrId, data) {
  const order = fallbackNonconformingOrders.find((item) => String(item.ncrId) === String(ncrId))
  if (!order) throw new Error('不良品项不存在')
  order.inspectionId = data.inspectionId ? Number(data.inspectionId) : order.inspectionId
  order.productLotId = data.productLotId ? Number(data.productLotId) : order.productLotId
  order.defectReason = String(data.defectReason || order.defectReason || '').trim()
  order.dispositionType = data.dispositionType ? String(data.dispositionType).toUpperCase() : order.dispositionType
  order.imageUrl = data.imageUrl || order.imageUrl || ''
  order.remark = data.remark || ''
  order.updatedTime = now
  return clone(order)
}

function inspectionPage(data) {
  return withMockFallback(
    () => request({ url: '/api/qms/inspection-order/page', data, silent: env.useMockData() }),
    () => fallbackInspectionPage(data)
  )
}

function inspectionDetail(inspectionId) {
  return withMockFallback(
    () => request({
      url: '/api/qms/inspection-order/' + encodeURIComponent(inspectionId),
      silent: env.useMockData()
    }),
    () => fallbackInspectionDetail(inspectionId)
  )
}

function submitInspection(inspectionId, data) {
  return withMockFallback(
    () => request({
      url: '/api/qms/inspection-order/' + encodeURIComponent(inspectionId) + '/submit',
      method: 'PUT',
      data,
      silent: env.useMockData()
    }),
    () => fallbackSubmitInspection(inspectionId, data)
  )
}

function nonconformingPage(data) {
  return withMockFallback(
    () => request({ url: '/api/qms/nonconforming-order/page', data, silent: env.useMockData() }),
    () => fallbackNonconformingPage(data)
  )
}

function nonconformingDetail(ncrId) {
  return withMockFallback(
    () => request({
      url: '/api/qms/nonconforming-order/' + encodeURIComponent(ncrId),
      silent: env.useMockData()
    }),
    () => fallbackNonconformingDetail(ncrId)
  )
}

function createNonconforming(data) {
  return withMockFallback(
    () => request({
      url: '/api/qms/nonconforming-order',
      method: 'POST',
      data,
      silent: env.useMockData()
    }),
    () => fallbackCreateNonconforming(data)
  )
}

function updateNonconforming(ncrId, data) {
  return withMockFallback(
    () => request({
      url: '/api/qms/nonconforming-order/' + encodeURIComponent(ncrId),
      method: 'PUT',
      data,
      silent: env.useMockData()
    }),
    () => fallbackUpdateNonconforming(ncrId, data)
  )
}

module.exports = {
  inspectionPage,
  inspectionDetail,
  submitInspection,
  nonconformingPage,
  nonconformingDetail,
  createNonconforming,
  updateNonconforming
}
