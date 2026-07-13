const now = '2026-07-13T09:30:00'

const currentUser = {
  userId: 1,
  username: 'mock',
  realName: '现场测试员',
  employeeId: 1001,
  roles: ['生产操作员'],
  roleNames: ['生产操作员'],
  permissions: ['mfg:task:start', 'mfg:task:finish', 'mfg:work-report:submit'],
  lastLoginTime: now
}

const products = [
  { productId: 1001, productCode: 'BD-TRAIN-77', productName: '训练级 77 速羽毛球', status: 'ENABLE' },
  { productId: 1002, productCode: 'BD-MATCH-77', productName: '比赛级 77 速羽毛球', status: 'ENABLE' },
  { productId: 1003, productCode: 'BD-MATCH-76', productName: '比赛级 76 速羽毛球', status: 'ENABLE' }
]

const operations = [
  { operationId: 2001, operationCode: 'OP-FEATHER', operationName: '羽片分选' },
  { operationId: 2002, operationCode: 'OP-GLUE', operationName: '植毛上胶' },
  { operationId: 2003, operationCode: 'OP-WINDING', operationName: '绕线加固' },
  { operationId: 2004, operationCode: 'OP-INSPECT', operationName: '飞行检验' }
]

const stations = [
  { stationId: 3001, stationCode: 'ST-FEATHER-01', stationName: '羽片分选工位 1' },
  { stationId: 3002, stationCode: 'ST-GLUE-01', stationName: '植毛上胶工位 1' },
  { stationId: 3003, stationCode: 'ST-WIND-01', stationName: '绕线加固工位 1' },
  { stationId: 3004, stationCode: 'ST-QA-01', stationName: '飞行检验工位 1' }
]

const employees = [
  { employeeId: 1001, employeeCode: 'EMP-MFG-101', employeeName: '张敏' },
  { employeeId: 1002, employeeCode: 'EMP-MFG-102', employeeName: '李娜' },
  { employeeId: 1003, employeeCode: 'EMP-QA-101', employeeName: '王磊' }
]

const workOrders = [
  {
    workOrderId: 5001,
    workOrderNo: 'WO-20260713-001',
    planId: 4101,
    productId: 1001,
    bomId: 6101,
    routeId: 7101,
    plannedQuantity: 1200,
    completedQuantity: 360,
    status: 'RUNNING',
    remark: '自动线 A 今日首批',
    createdTime: '2026-07-13T08:00:00',
    updatedTime: '2026-07-13T09:20:00'
  },
  {
    workOrderId: 5002,
    workOrderNo: 'WO-20260713-002',
    planId: 4102,
    productId: 1002,
    bomId: 6102,
    routeId: 7102,
    plannedQuantity: 900,
    completedQuantity: 0,
    status: 'RELEASED',
    remark: '待班组接收',
    createdTime: '2026-07-13T08:30:00',
    updatedTime: '2026-07-13T08:30:00'
  },
  {
    workOrderId: 5003,
    workOrderNo: 'WO-20260712-006',
    planId: 4103,
    productId: 1003,
    bomId: 6103,
    routeId: 7102,
    plannedQuantity: 1000,
    completedQuantity: 1000,
    status: 'FINISHED',
    remark: '昨日完成批次',
    createdTime: '2026-07-12T14:10:00',
    updatedTime: '2026-07-12T18:00:00'
  }
]

const tasks = [
  {
    taskId: 8001,
    taskNo: 'TASK-20260713-001',
    dispatchId: 7001,
    workOrderId: 5001,
    operationId: 2001,
    stationId: 3001,
    plannedQuantity: 1200,
    completedQuantity: 360,
    defectQuantity: 8,
    status: 'RUNNING',
    remark: '当前正在执行',
    createdTime: '2026-07-13T08:05:00',
    updatedTime: '2026-07-13T09:15:00'
  },
  {
    taskId: 8002,
    taskNo: 'TASK-20260713-002',
    dispatchId: 7001,
    workOrderId: 5001,
    operationId: 2002,
    stationId: 3002,
    plannedQuantity: 1200,
    completedQuantity: 0,
    defectQuantity: 0,
    status: 'WAIT_START',
    remark: '等待上胶',
    createdTime: '2026-07-13T08:10:00',
    updatedTime: '2026-07-13T08:10:00'
  },
  {
    taskId: 8003,
    taskNo: 'TASK-20260713-003',
    dispatchId: 7002,
    workOrderId: 5002,
    operationId: 2003,
    stationId: 3003,
    plannedQuantity: 900,
    completedQuantity: 0,
    defectQuantity: 0,
    status: 'WAIT_START',
    remark: '待开工',
    createdTime: '2026-07-13T08:35:00',
    updatedTime: '2026-07-13T08:35:00'
  },
  {
    taskId: 8004,
    taskNo: 'TASK-20260712-008',
    dispatchId: 7003,
    workOrderId: 5003,
    operationId: 2004,
    stationId: 3004,
    plannedQuantity: 1000,
    completedQuantity: 1000,
    defectQuantity: 12,
    status: 'FINISHED',
    remark: '检验完成',
    createdTime: '2026-07-12T15:00:00',
    updatedTime: '2026-07-12T17:55:00'
  }
]

let reports = [
  {
    reportId: 9001,
    reportNo: 'REP-20260713-001',
    taskId: 8001,
    operatorId: 1001,
    stationId: 3001,
    qualifiedQuantity: 360,
    defectQuantity: 8,
    actualWorkHours: 1.5,
    reportTime: '2026-07-13T09:10:00',
    auditStatus: 'SUBMITTED',
    remark: '首段报工',
    createdTime: '2026-07-13T09:10:00'
  },
  {
    reportId: 9002,
    reportNo: 'REP-20260712-012',
    taskId: 8004,
    operatorId: 1003,
    stationId: 3004,
    qualifiedQuantity: 988,
    defectQuantity: 12,
    actualWorkHours: 2,
    reportTime: '2026-07-12T17:50:00',
    auditStatus: 'APPROVED',
    remark: '昨日检验报工',
    createdTime: '2026-07-12T17:50:00'
  }
]

const reportMaterials = {
  9001: [
    { id: 1, reportId: 9001, materialId: 1101, batchId: 1201, usedQuantity: 360, scrapQuantity: 8, remark: '羽片消耗' }
  ],
  9002: [
    { id: 2, reportId: 9002, materialId: 1102, batchId: 1202, usedQuantity: 1000, scrapQuantity: 12, remark: '检验抽样' }
  ]
}

const barcodes = [
  {
    barcodeId: 1,
    barcodeValue: 'WO-20260713-001',
    barcodeType: 'WORK_ORDER',
    bizType: 'MFG_WORK_ORDER',
    bizId: 5001,
    status: 'ENABLE',
    remark: '生产工单测试条码',
    createdTime: '2026-07-13T08:00:00',
    updatedTime: '2026-07-13T08:00:00'
  },
  {
    barcodeId: 2,
    barcodeValue: 'TASK-20260713-001',
    barcodeType: 'TASK',
    bizType: 'MFG_TASK',
    bizId: 8001,
    status: 'ENABLE',
    remark: '生产任务测试条码',
    createdTime: '2026-07-13T08:05:00',
    updatedTime: '2026-07-13T08:05:00'
  }
]

function compact(value) {
  return value !== '' && value !== null && value !== undefined
}

function contains(source, keyword) {
  if (!compact(keyword)) return true
  return String(source || '').toLowerCase().includes(String(keyword).toLowerCase())
}

function paginate(records, query) {
  const current = Math.max(1, Number(query.current || 1))
  const size = Math.max(1, Number(query.size || 10))
  const total = records.length
  const pages = Math.ceil(total / size)
  const start = (current - 1) * size
  return {
    records: records.slice(start, start + size),
    total,
    size,
    current,
    pages
  }
}

function pageProducts(query) {
  return paginate(products, query)
}

function pageOperations(query) {
  return paginate(operations, query)
}

function pageStations(query) {
  return paginate(stations, query)
}

function pageEmployees(query) {
  return paginate(employees, query)
}

function pageWorkOrders(query) {
  const filtered = workOrders.filter((item) => {
    if (compact(query.status) && item.status !== query.status) return false
    if (compact(query.productId) && String(item.productId) !== String(query.productId)) return false
    return contains(item.workOrderNo, query.keyword)
  })
  return paginate(filtered, query)
}

function pageTasks(query) {
  const filtered = tasks.filter((item) => {
    if (compact(query.status) && item.status !== query.status) return false
    if (compact(query.workOrderId) && String(item.workOrderId) !== String(query.workOrderId)) return false
    if (compact(query.operationId) && String(item.operationId) !== String(query.operationId)) return false
    if (compact(query.stationId) && String(item.stationId) !== String(query.stationId)) return false
    return contains(item.taskNo, query.keyword)
  })
  return paginate(filtered, query)
}

function pageReports(query) {
  const filtered = reports.filter((item) => {
    if (compact(query.taskId) && String(item.taskId) !== String(query.taskId)) return false
    if (compact(query.operatorId) && String(item.operatorId) !== String(query.operatorId)) return false
    if (compact(query.stationId) && String(item.stationId) !== String(query.stationId)) return false
    if (compact(query.auditStatus) && item.auditStatus !== query.auditStatus) return false
    return contains(item.reportNo, query.keyword)
  })
  return paginate(filtered, query)
}

function findById(records, field, id) {
  return records.find((item) => String(item[field]) === String(id)) || null
}

function summary() {
  return {
    productionOrderCount: 3,
    workOrderCount: workOrders.length,
    runningTaskCount: tasks.filter((item) => item.status === 'RUNNING').length,
    todayQualifiedQuantity: reports
      .filter((item) => String(item.reportTime).startsWith('2026-07-13'))
      .reduce((sum, item) => sum + Number(item.qualifiedQuantity || 0), 0),
    todayDefectQuantity: reports
      .filter((item) => String(item.reportTime).startsWith('2026-07-13'))
      .reduce((sum, item) => sum + Number(item.defectQuantity || 0), 0),
    inventoryWarningCount: 2,
    equipmentFaultCount: 1
  }
}

function startTask(id, data) {
  const task = findById(tasks, 'taskId', id)
  if (!task) throw new Error('任务不存在')
  if (task.status !== 'WAIT_START') throw new Error('只有待开工任务可以开始')
  task.status = 'RUNNING'
  if (data && compact(data.stationId)) task.stationId = Number(data.stationId)
  task.updatedTime = now
  const workOrder = findById(workOrders, 'workOrderId', task.workOrderId)
  if (workOrder && workOrder.status === 'RELEASED') workOrder.status = 'RUNNING'
  return task
}

function finishTask(id, data) {
  const task = findById(tasks, 'taskId', id)
  if (!task) throw new Error('任务不存在')
  if (!['RUNNING', 'PAUSED'].includes(task.status)) throw new Error('当前任务状态不允许完成')
  task.completedQuantity = Number(data && data.completedQuantity || 0)
  task.defectQuantity = Number(data && data.defectQuantity || 0)
  task.remark = data && data.remark || task.remark
  task.status = 'FINISHED'
  task.updatedTime = now
  const workOrder = findById(workOrders, 'workOrderId', task.workOrderId)
  if (workOrder) {
    workOrder.completedQuantity = Math.max(Number(workOrder.completedQuantity || 0), Number(task.completedQuantity || 0))
    workOrder.updatedTime = now
  }
  return task
}

function submitReport(data) {
  const task = findById(tasks, 'taskId', data && data.taskId)
  if (!task) throw new Error('任务不存在')
  if (task.status !== 'RUNNING') throw new Error('只有生产中任务可以报工')
  const reportId = Math.max.apply(null, reports.map((item) => item.reportId)) + 1
  const report = {
    reportId,
    reportNo: 'REP-20260713-' + String(reportId).slice(-3),
    taskId: task.taskId,
    operatorId: Number(data.operatorId),
    stationId: Number(data.stationId || task.stationId),
    qualifiedQuantity: Number(data.qualifiedQuantity || 0),
    defectQuantity: Number(data.defectQuantity || 0),
    actualWorkHours: Number(data.actualWorkHours || 0),
    reportTime: now,
    auditStatus: 'SUBMITTED',
    remark: data.remark || '',
    createdTime: now
  }
  reports = [report].concat(reports)
  task.completedQuantity = Number(task.completedQuantity || 0) + report.qualifiedQuantity
  task.defectQuantity = Number(task.defectQuantity || 0) + report.defectQuantity
  task.updatedTime = now
  reportMaterials[reportId] = Array.isArray(data.materials) ? data.materials.map((item, index) => ({
    id: reportId * 10 + index,
    reportId,
    ...item
  })) : []
  return report
}

function scanBarcode(value) {
  return barcodes.find((item) => item.barcodeValue === value) || null
}

function route(config) {
  const url = config.url
  const data = config.data || {}

  if (url === '/api/auth/login') return { token: 'mock-token', data: currentUser, ...currentUser }
  if (url === '/api/auth/info') return currentUser
  if (url === '/api/auth/logout') return true
  if (url === '/api/dashboard/summary') return summary()
  if (url === '/api/mdm/product/page') return pageProducts(data)
  if (url === '/api/process/operation/page') return pageOperations(data)
  if (url === '/api/factory/station/page') return pageStations(data)
  if (url === '/api/org/employee/page') return pageEmployees(data)
  if (url === '/api/mfg/work-order/page') return pageWorkOrders(data)
  if (url === '/api/mfg/task/page') return pageTasks(data)
  if (url === '/api/mfg/work-report/page') return pageReports(data)
  if (url === '/api/mfg/work-report/submit') return submitReport(data)

  let matched = url.match(/^\/api\/mfg\/work-order\/(\d+)$/)
  if (matched) return findById(workOrders, 'workOrderId', matched[1])

  matched = url.match(/^\/api\/mfg\/task\/(\d+)$/)
  if (matched) return findById(tasks, 'taskId', matched[1])

  matched = url.match(/^\/api\/mfg\/task\/(\d+)\/start$/)
  if (matched) return startTask(matched[1], data)

  matched = url.match(/^\/api\/mfg\/task\/(\d+)\/finish$/)
  if (matched) return finishTask(matched[1], data)

  matched = url.match(/^\/api\/mfg\/work-report\/(\d+)$/)
  if (matched) return findById(reports, 'reportId', matched[1])

  matched = url.match(/^\/api\/mfg\/report-material\/report\/(\d+)$/)
  if (matched) return reportMaterials[matched[1]] || []

  matched = url.match(/^\/api\/trace\/barcode\/scan\/(.+)$/)
  if (matched) return scanBarcode(decodeURIComponent(matched[1]))

  return undefined
}

function handleRequest(config) {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      try {
        const result = route(config || {})
        if (result === undefined) {
          reject(new Error('本地测试数据未覆盖该接口：' + ((config && config.url) || '')))
          return
        }
        resolve(result)
      } catch (error) {
        reject(error)
      }
    }, 120)
  })
}

module.exports = {
  handleRequest,
  currentUser
}
