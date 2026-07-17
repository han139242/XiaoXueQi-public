const now = '2026-07-13T09:30:00'
const UNQUALIFIED_RATE_LIMIT = 0.005

function currentLocalTime() {
  const date = new Date()
  const pad = (value) => value < 10 ? '0' + value : String(value)
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate())
    + 'T' + pad(date.getHours()) + ':' + pad(date.getMinutes()) + ':' + pad(date.getSeconds())
}

const currentUser = {
  userId: 1,
  username: 'mock',
  realName: '现场测试员',
  employeeId: 1001,
  roles: ['生产操作员'],
  roleNames: ['生产操作员'],
  permissions: [
    'mfg:work-order:view', 'mfg:task:view', 'mfg:task:start', 'mfg:task:finish',
    'mfg:work-report:view', 'mfg:work-report:create', 'mfg:work-report:submit', 'trace:barcode:scan'
  ],
  lastLoginTime: now
}

const products = [
  { productId: 1001, productCode: 'BD-TRAIN-77', productName: '训练级 77 速羽毛球', status: 'ENABLE' },
  { productId: 1002, productCode: 'BD-MATCH-77', productName: '比赛级 77 速羽毛球', status: 'ENABLE' },
  { productId: 1003, productCode: 'BD-MATCH-76', productName: '比赛级 76 速羽毛球', status: 'ENABLE' }
]

const customers = [
  { customerId: 2101, customerCode: 'CUS-SCHOOL-01', customerName: '青训体育用品' },
  { customerId: 2102, customerCode: 'CUS-CLUB-02', customerName: '城市羽毛球俱乐部' },
  { customerId: 2103, customerCode: 'CUS-EXPORT-03', customerName: '海外训练器材客户' }
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

const materials = [
  { materialId: 1101, materialCode: 'MAT-FEATHER-A', materialName: '鹅毛毛片 A 级', unitName: '片', status: 'ENABLE' },
  { materialId: 1102, materialCode: 'MAT-CORK-77', materialName: '复合球头 77 速', unitName: '只', status: 'ENABLE' },
  { materialId: 1103, materialCode: 'MAT-GLUE-01', materialName: '羽毛球专用胶水', unitName: 'ml', status: 'ENABLE' },
  { materialId: 1104, materialCode: 'MAT-THREAD-W', materialName: '白色加固线', unitName: 'm', status: 'ENABLE' }
]

const warehouses = [
  { warehouseId: 1, warehouseCode: 'WH-RAW', warehouseName: '原料仓', warehouseType: 'RAW', managerId: 1002, status: 'ENABLE' },
  { warehouseId: 2, warehouseCode: 'WH-FG', warehouseName: '成品仓', warehouseType: 'FINISHED', managerId: 1002, status: 'ENABLE' }
]

const locations = [
  { locationId: 101, warehouseId: 1, locationCode: 'RAW-A-01', locationName: '原料 A 区 01', locationType: 'NORMAL', status: 'ENABLE' },
  { locationId: 102, warehouseId: 1, locationCode: 'RAW-B-02', locationName: '原料 B 区 02', locationType: 'NORMAL', status: 'ENABLE' },
  { locationId: 201, warehouseId: 2, locationCode: 'FG-A-01', locationName: '成品 A 区 01', locationType: 'NORMAL', status: 'ENABLE' }
]

let inventories = [
  {
    inventoryId: 12001,
    materialId: 1101,
    batchId: 13001,
    batchNo: 'BATCH-FEATHER-0713',
    warehouseId: 1,
    locationId: 101,
    quantity: 48000,
    lockedQuantity: 1200,
    availableQuantity: 46800,
    warningQuantity: 20000,
    updatedTime: '2026-07-13T08:40:00',
    remark: '今日生产主用羽片'
  },
  {
    inventoryId: 12002,
    materialId: 1102,
    batchId: 13002,
    batchNo: 'BATCH-CORK-0712',
    warehouseId: 1,
    locationId: 102,
    quantity: 16000,
    lockedQuantity: 900,
    availableQuantity: 15100,
    warningQuantity: 8000,
    updatedTime: '2026-07-13T08:44:00',
    remark: ''
  },
  {
    inventoryId: 12003,
    materialId: 1103,
    batchId: 13003,
    batchNo: 'BATCH-GLUE-0709',
    warehouseId: 1,
    locationId: 101,
    quantity: 9800,
    lockedQuantity: 300,
    availableQuantity: 9500,
    warningQuantity: 12000,
    updatedTime: '2026-07-13T08:46:00',
    remark: '低于预警'
  }
]

let inboundOrders = [
  {
    inboundId: 14001,
    inboundNo: 'IN-20260713-001',
    inboundType: 'PURCHASE',
    warehouseId: 1,
    sourceOrderType: 'PURCHASE_ORDER',
    sourceOrderId: 0,
    auditStatus: 'SUBMITTED',
    inboundTime: '',
    remark: '供应商送货待确认',
    createdTime: '2026-07-13T08:50:00',
    updatedTime: '2026-07-13T08:50:00'
  },
  {
    inboundId: 14002,
    inboundNo: 'IN-20260712-004',
    inboundType: 'PRODUCTION',
    warehouseId: 2,
    sourceOrderType: 'MFG_WORK_ORDER',
    sourceOrderId: 5003,
    auditStatus: 'APPROVED',
    inboundTime: '2026-07-12T18:20:00',
    remark: '昨日成品入库',
    createdTime: '2026-07-12T18:05:00',
    updatedTime: '2026-07-12T18:20:00'
  }
]

let inboundDetails = {
  14001: [
    { detailId: 14101, inboundId: 14001, materialId: 1101, batchId: 13004, batchNo: 'BATCH-FEATHER-0714', locationId: 101, inboundQuantity: 30000, remark: '鹅毛毛片' },
    { detailId: 14102, inboundId: 14001, materialId: 1103, batchId: 13005, batchNo: 'BATCH-GLUE-0714', locationId: 102, inboundQuantity: 15000, remark: '胶水补货' }
  ],
  14002: [
    { detailId: 14103, inboundId: 14002, materialId: 1102, batchId: 13002, batchNo: 'BATCH-CORK-0712', locationId: 201, inboundQuantity: 1000, remark: '成品周转' }
  ]
}

let outboundOrders = [
  {
    outboundId: 15001,
    outboundNo: 'OUT-20260713-001',
    outboundType: 'MATERIAL_ISSUE',
    workOrderId: 5001,
    warehouseId: 1,
    auditStatus: 'SUBMITTED',
    outboundTime: '',
    remark: '自动线 A 领料',
    createdTime: '2026-07-13T08:55:00',
    updatedTime: '2026-07-13T08:55:00'
  },
  {
    outboundId: 15002,
    outboundNo: 'OUT-20260712-002',
    outboundType: 'SALE',
    workOrderId: 0,
    warehouseId: 2,
    auditStatus: 'APPROVED',
    outboundTime: '2026-07-12T19:10:00',
    remark: '成品发货',
    createdTime: '2026-07-12T18:40:00',
    updatedTime: '2026-07-12T19:10:00'
  }
]

let outboundDetails = {
  15001: [
    { detailId: 15101, outboundId: 15001, materialId: 1101, batchId: 13001, batchNo: 'BATCH-FEATHER-0713', locationId: 101, outboundQuantity: 12000, remark: '羽片领料' },
    { detailId: 15102, outboundId: 15001, materialId: 1103, batchId: 13003, batchNo: 'BATCH-GLUE-0709', locationId: 101, outboundQuantity: 1200, remark: '胶水领料' }
  ],
  15002: [
    { detailId: 15103, outboundId: 15002, materialId: 1102, batchId: 13002, batchNo: 'BATCH-CORK-0712', locationId: 201, outboundQuantity: 600, remark: '发货' }
  ]
}

let equipments = [
  {
    equipmentId: 16001,
    equipmentCode: 'EQ-GLUE-01',
    equipmentName: '自动植毛上胶机 1',
    categoryId: 1,
    model: 'GL-600',
    manufacturer: '青岛智造装备',
    purchaseDate: '2025-03-12',
    stationId: 3002,
    status: 'RUNNING',
    remark: '自动线 A 主设备',
    createdTime: '2026-07-10T09:00:00',
    updatedTime: '2026-07-13T08:20:00'
  },
  {
    equipmentId: 16002,
    equipmentCode: 'EQ-WIND-01',
    equipmentName: '绕线加固机 1',
    categoryId: 2,
    model: 'WD-300',
    manufacturer: '沈阳精工',
    purchaseDate: '2024-11-05',
    stationId: 3003,
    status: 'FAULT',
    remark: '张力波动，待维修',
    createdTime: '2026-07-10T09:20:00',
    updatedTime: '2026-07-13T09:00:00'
  },
  {
    equipmentId: 16003,
    equipmentCode: 'EQ-QA-01',
    equipmentName: '飞行轨迹检测台',
    categoryId: 3,
    model: 'QA-200',
    manufacturer: '苏州检测仪器',
    purchaseDate: '2025-01-18',
    stationId: 3004,
    status: 'IDLE',
    remark: '',
    createdTime: '2026-07-10T09:40:00',
    updatedTime: '2026-07-13T08:10:00'
  }
]

let repairOrders = [
  {
    repairId: 17001,
    repairNo: 'REP-EQ-20260713-001',
    equipmentId: 16002,
    exceptionId: 0,
    applicantId: 1001,
    repairerId: null,
    faultDescription: '绕线张力传感器波动，影响线圈紧固度',
    repairResult: '',
    repairCost: 0,
    status: 'SUBMITTED',
    reportedTime: '2026-07-13T09:05:00',
    assignedTime: '',
    closedTime: '',
    remark: '现场已停机等待维修',
    createdTime: '2026-07-13T09:05:00',
    updatedTime: '2026-07-13T09:05:00'
  },
  {
    repairId: 17002,
    repairNo: 'REP-EQ-20260712-003',
    equipmentId: 16001,
    exceptionId: 0,
    applicantId: 1002,
    repairerId: 1002,
    faultDescription: '胶量泵头堵塞',
    repairResult: '清洗泵头后恢复',
    repairCost: 80,
    status: 'CLOSED',
    reportedTime: '2026-07-12T15:30:00',
    assignedTime: '2026-07-12T15:40:00',
    closedTime: '2026-07-12T16:10:00',
    remark: '',
    createdTime: '2026-07-12T15:30:00',
    updatedTime: '2026-07-12T16:10:00'
  }
]

const productionOrders = [
  {
    productionOrderId: 4001,
    productionOrderNo: 'PO-20260713-001',
    customerId: 2101,
    productId: 1001,
    orderQuantity: 1200,
    deliveryDate: '2026-07-16',
    sourceType: 'ERP',
    status: 'RUNNING',
    remark: '学校训练球补货订单',
    createdTime: '2026-07-13T07:45:00',
    updatedTime: '2026-07-13T09:20:00'
  },
  {
    productionOrderId: 4002,
    productionOrderNo: 'PO-20260713-002',
    customerId: 2102,
    productId: 1002,
    orderQuantity: 900,
    deliveryDate: '2026-07-17',
    sourceType: 'MANUAL',
    status: 'SCHEDULED',
    remark: '俱乐部比赛级用球',
    createdTime: '2026-07-13T08:20:00',
    updatedTime: '2026-07-13T08:45:00'
  },
  {
    productionOrderId: 4003,
    productionOrderNo: 'PO-20260712-006',
    customerId: 2103,
    productId: 1003,
    orderQuantity: 1000,
    deliveryDate: '2026-07-15',
    sourceType: 'ERP',
    status: 'FINISHED',
    remark: '昨日已完成批次',
    createdTime: '2026-07-12T13:50:00',
    updatedTime: '2026-07-12T18:10:00'
  }
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

const productionPlans = [
  { planId: 4101, planNo: 'PLAN-20260713-001', plannedStartTime: '2026-07-13T08:00:00', plannedEndTime: '2026-07-13T18:00:00', plannedQuantity: 1200, status: 'ACTIVE' },
  { planId: 4102, planNo: 'PLAN-20260713-002', plannedStartTime: '2026-07-13T08:30:00', plannedEndTime: '2026-07-14T18:00:00', plannedQuantity: 900, status: 'ACTIVE' },
  { planId: 4103, planNo: 'PLAN-20260712-006', plannedStartTime: '2026-07-12T14:00:00', plannedEndTime: '2026-07-12T18:00:00', plannedQuantity: 1000, status: 'FINISHED' }
]

const taskOperators = [
  { id: 1, taskId: 8001, employeeId: 1001, roleInTask: 'PRIMARY' },
  { id: 2, taskId: 8002, employeeId: 1001, roleInTask: 'PRIMARY' },
  { id: 3, taskId: 8003, employeeId: 1002, roleInTask: 'PRIMARY' },
  { id: 4, taskId: 8004, employeeId: 1003, roleInTask: 'PRIMARY' }
]

let inspectionOrders = [
  {
    inspectionId: 10001,
    inspectionNo: 'IQC-20260713-001',
    inspectionType: 'final',
    schemeId: 1,
    workOrderId: 5001,
    taskId: 8001,
    taskNo: 'TASK-20260713-001',
    reportId: 9001,
    reportNo: 'REP-20260713-001',
    batchId: 0,
    inspectorId: 1003,
    sampleQuantity: 0,
    qualifiedQuantity: 0,
    unqualifiedQuantity: 0,
    inspectionTime: null,
    inspectionResult: '',
    status: 'WAIT_INSPECTION',
    remark: '自动线 A 首批终检',
    createdTime: '2026-07-13T09:20:00',
    updatedTime: '2026-07-13T09:20:00'
  },
  {
    inspectionId: 10002,
    inspectionNo: 'PQC-20260713-002',
    inspectionType: 'process',
    schemeId: 2,
    workOrderId: 5002,
    taskId: 8003,
    taskNo: 'TASK-20260713-003',
    reportId: null,
    reportNo: '',
    batchId: 0,
    inspectorId: 1003,
    sampleQuantity: 0,
    qualifiedQuantity: 0,
    unqualifiedQuantity: 0,
    inspectionTime: null,
    inspectionResult: '',
    status: 'WAIT_INSPECTION',
    remark: '绕线工序巡检',
    createdTime: '2026-07-13T09:26:00',
    updatedTime: '2026-07-13T09:26:00'
  },
  {
    inspectionId: 10003,
    inspectionNo: 'FQC-20260712-003',
    inspectionType: 'final',
    schemeId: 1,
    workOrderId: 5003,
    taskId: 8004,
    taskNo: 'TASK-20260712-004',
    reportId: 9002,
    reportNo: 'REP-20260712-002',
    batchId: 0,
    inspectorId: 1003,
    sampleQuantity: 100,
    qualifiedQuantity: 100,
    unqualifiedQuantity: 0,
    inspectionTime: '2026-07-12T18:25:00',
    inspectionResult: 'PASS',
    status: 'FINISHED',
    remark: '成品抽检合格',
    createdTime: '2026-07-12T18:10:00',
    updatedTime: '2026-07-12T18:25:00'
  },
  {
    inspectionId: 10004,
    inspectionNo: 'PQC-20260712-004',
    inspectionType: 'process',
    schemeId: 2,
    workOrderId: 5002,
    taskId: 8003,
    taskNo: 'TASK-20260713-003',
    reportId: 9002,
    reportNo: 'REP-20260712-002',
    batchId: 0,
    inspectorId: 1003,
    sampleQuantity: 100,
    qualifiedQuantity: 92,
    unqualifiedQuantity: 8,
    inspectionTime: '2026-07-12T17:58:00',
    inspectionResult: 'FAIL',
    status: 'FINISHED',
    remark: '绕线外观复检不合格',
    createdTime: '2026-07-12T17:40:00',
    updatedTime: '2026-07-12T17:58:00'
  }
]

let nonconformingOrders = [
  {
    ncrId: 11001,
    ncrNo: 'NCR-20260713-001',
    inspectionId: 10004,
    inspectionNo: 'PQC-20260712-004',
    productLotId: 0,
    defectReason: '涂胶不均匀',
    dispositionType: 'REWORK',
    approvalStatus: 'PENDING',
    status: 'OPEN',
    remark: '抽检发现局部胶量不足',
    createdTime: '2026-07-13T09:28:00',
    updatedTime: '2026-07-13T09:28:00'
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

function pageCustomers(query) {
  const filtered = customers.filter((item) => contains(item.customerCode, query.keyword) || contains(item.customerName, query.keyword))
  return paginate(filtered, query)
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

function pageMaterials(query) {
  const filtered = materials.filter((item) => contains(item.materialCode, query.keyword) || contains(item.materialName, query.keyword))
  return paginate(filtered, query)
}

function pageWarehouses(query) {
  const filtered = warehouses.filter((item) => {
    if (compact(query.status) && item.status !== query.status) return false
    return contains(item.warehouseCode, query.keyword) || contains(item.warehouseName, query.keyword)
  })
  return paginate(filtered, query)
}

function pageLocations(query) {
  const filtered = locations.filter((item) => {
    if (compact(query.warehouseId) && String(item.warehouseId) !== String(query.warehouseId)) return false
    return contains(item.locationCode, query.keyword) || contains(item.locationName, query.keyword)
  })
  return paginate(filtered, query)
}

function pageProductionOrders(query) {
  const filtered = productionOrders.filter((item) => {
    if (compact(query.status) && item.status !== query.status) return false
    if (compact(query.customerId) && String(item.customerId) !== String(query.customerId)) return false
    if (compact(query.productId) && String(item.productId) !== String(query.productId)) return false
    return contains(item.productionOrderNo, query.keyword)
  })
  return paginate(filtered, query)
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
    if (compact(query.employeeId) && !taskOperators.some((operator) => String(operator.taskId) === String(item.taskId) && String(operator.employeeId) === String(query.employeeId))) return false
    return contains(item.taskNo, query.keyword)
  })
  const sorted = filtered.slice().sort((left, right) => {
    const a = new Date(left.createdTime || 0).getTime()
    const b = new Date(right.createdTime || 0).getTime()
    return query.sort === 'oldest' ? a - b : b - a
  })
  return paginate(sorted, query)
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

function pageProductionPlans(query) {
  const filtered = productionPlans.filter((item) => contains(item.planNo, query.keyword))
  return paginate(filtered, query)
}

function pageInspectionOrders(query) {
  const filtered = inspectionOrders.filter((item) => {
    if (compact(query.status) && item.status !== query.status) return false
    if (compact(query.inspectionType) && item.inspectionType !== query.inspectionType) return false
    if (compact(query.inspectionResult) && item.inspectionResult !== query.inspectionResult) return false
    if (compact(query.workOrderId) && String(item.workOrderId) !== String(query.workOrderId)) return false
    return contains(item.inspectionNo, query.keyword) || contains(item.taskNo, query.keyword) || contains(item.reportNo, query.keyword)
  })
  return paginate(filtered, query)
}

function pageNonconformingOrders(query) {
  const filtered = nonconformingOrders.filter((item) => {
    if (compact(query.inspectionId) && String(item.inspectionId) !== String(query.inspectionId)) return false
    if (compact(query.approvalStatus) && item.approvalStatus !== query.approvalStatus) return false
    if (compact(query.status) && item.status !== query.status) return false
    return contains(item.ncrNo, query.keyword) || contains(item.defectReason, query.keyword)
  })
  return paginate(filtered, query)
}

function pageInventories(query) {
  const filtered = inventories.filter((item) => {
    if (compact(query.materialId) && String(item.materialId) !== String(query.materialId)) return false
    if (compact(query.warehouseId) && String(item.warehouseId) !== String(query.warehouseId)) return false
    return contains(item.batchNo, query.keyword)
  })
  return paginate(filtered, query)
}

function pageInboundOrders(query) {
  const filtered = inboundOrders.filter((item) => {
    if (compact(query.auditStatus) && item.auditStatus !== query.auditStatus) return false
    if (compact(query.warehouseId) && String(item.warehouseId) !== String(query.warehouseId)) return false
    return contains(item.inboundNo, query.keyword)
  })
  return paginate(filtered, query)
}

function pageOutboundOrders(query) {
  const filtered = outboundOrders.filter((item) => {
    if (compact(query.auditStatus) && item.auditStatus !== query.auditStatus) return false
    if (compact(query.warehouseId) && String(item.warehouseId) !== String(query.warehouseId)) return false
    return contains(item.outboundNo, query.keyword)
  })
  return paginate(filtered, query)
}

function pageEquipments(query) {
  const filtered = equipments.filter((item) => {
    if (compact(query.status) && item.status !== query.status) return false
    if (compact(query.categoryId) && String(item.categoryId) !== String(query.categoryId)) return false
    return contains(item.equipmentCode, query.keyword) || contains(item.equipmentName, query.keyword)
  })
  return paginate(filtered, query)
}

function pageRepairOrders(query) {
  const filtered = repairOrders.filter((item) => {
    if (compact(query.status) && item.status !== query.status) return false
    if (compact(query.equipmentId) && String(item.equipmentId) !== String(query.equipmentId)) return false
    if (compact(query.repairerId) && String(item.repairerId) !== String(query.repairerId)) return false
    return contains(item.repairNo, query.keyword) || contains(item.faultDescription, query.keyword)
  })
  return paginate(filtered, query)
}

function findById(records, field, id) {
  return records.find((item) => String(item[field]) === String(id)) || null
}

function createInspectionOrderFromReport(report, task) {
  if (!report || !task) return null
  const existing = inspectionOrders.find((item) => String(item.reportId) === String(report.reportId))
  if (existing) return existing
  const inspectionId = inspectionOrders.length
    ? Math.max.apply(null, inspectionOrders.map((item) => item.inspectionId)) + 1
    : 10001
  const inspection = {
    inspectionId,
    inspectionNo: 'PQC-20260713-' + String(inspectionId).slice(-3),
    inspectionType: 'process',
    schemeId: 2,
    workOrderId: task.workOrderId,
    taskId: task.taskId,
    taskNo: task.taskNo || '',
    reportId: report.reportId,
    reportNo: report.reportNo,
    batchId: 0,
    inspectorId: 1003,
    sampleQuantity: 0,
    qualifiedQuantity: 0,
    unqualifiedQuantity: 0,
    inspectionTime: null,
    inspectionResult: '',
    status: 'WAIT_INSPECTION',
    remark: '报工提交后自动生成待检任务',
    createdTime: now,
    updatedTime: now
  }
  inspectionOrders = [inspection].concat(inspectionOrders)
  return inspection
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
  createInspectionOrderFromReport(report, task)
  return report
}

function submitInspectionOrder(id, data) {
  const inspection = findById(inspectionOrders, 'inspectionId', id)
  if (!inspection) throw new Error('检验单不存在')
  if (!['WAIT_INSPECTION', 'INSPECTING'].includes(inspection.status)) throw new Error('当前检验单状态不允许提交')
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
    const report = inspection.reportId ? findById(reports, 'reportId', inspection.reportId) : null
    if (report && sampleQuantity > Number(report.qualifiedQuantity || 0) + Number(report.defectQuantity || 0)) {
      throw new Error('抽检数量不能超过关联报工数量')
    }
    inspection.sampleQuantity = sampleQuantity
    inspection.qualifiedQuantity = qualifiedQuantity
    inspection.unqualifiedQuantity = unqualifiedQuantity
    inspection.inspectionResult = unqualifiedQuantity / sampleQuantity <= UNQUALIFIED_RATE_LIMIT ? 'PASS' : 'FAIL'
  } else {
    inspection.inspectionResult = data && data.inspectionResult ? data.inspectionResult : 'PASS'
  }
  inspection.remark = data && data.remark ? data.remark : inspection.remark
  inspection.status = 'FINISHED'
  inspection.inspectionTime = currentLocalTime()
  inspection.updatedTime = inspection.inspectionTime
  return inspection
}

function createNonconformingOrder(data) {
  if (!data || !compact(data.inspectionId)) throw new Error('检验单不能为空')
  if (!compact(data.defectReason)) throw new Error('不良原因不能为空')
  const inspection = findById(inspectionOrders, 'inspectionId', data.inspectionId)
  if (!inspection) throw new Error('检验单不存在')
  if (inspection.status !== 'FINISHED' || inspection.inspectionResult !== 'FAIL') {
    throw new Error('只能关联已完成且不合格的检验单')
  }
  const dispositionType = String(data.dispositionType || '').toUpperCase()
  if (!['REWORK', 'SCRAP', 'DOWNGRADE', 'CONCESSION'].includes(dispositionType)) {
    throw new Error('请选择有效的处理方式')
  }

  const ncrId = nonconformingOrders.length
    ? Math.max.apply(null, nonconformingOrders.map((item) => item.ncrId)) + 1
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
    remark: data.remark || '',
    createdTime,
    updatedTime: createdTime
  }
  nonconformingOrders = [order].concat(nonconformingOrders)
  inspection.inspectionResult = 'FAIL'
  inspection.status = 'FINISHED'
  inspection.updatedTime = now
  return order
}

function updateNonconformingOrder(id, data) {
  const order = findById(nonconformingOrders, 'ncrId', id)
  if (!order) throw new Error('不良品项不存在')
  if (data && compact(data.inspectionId)) order.inspectionId = Number(data.inspectionId)
  if (data && compact(data.productLotId)) order.productLotId = Number(data.productLotId)
  if (data && compact(data.defectReason)) order.defectReason = String(data.defectReason).trim()
  if (data && compact(data.dispositionType)) order.dispositionType = String(data.dispositionType).toUpperCase()
  order.imageUrl = data && data.imageUrl || order.imageUrl || ''
  order.remark = data && data.remark || ''
  order.updatedTime = now
  return order
}

function confirmInboundOrder(id, data) {
  const order = findById(inboundOrders, 'inboundId', id)
  if (!order) throw new Error('入库单不存在')
  if (order.auditStatus !== 'SUBMITTED') throw new Error('当前入库单不允许确认')
  order.auditStatus = 'APPROVED'
  order.inboundTime = now
  order.remark = data && data.remark ? data.remark : order.remark
  order.updatedTime = now
  return order
}

function confirmOutboundOrder(id, data) {
  const order = findById(outboundOrders, 'outboundId', id)
  if (!order) throw new Error('出库单不存在')
  if (order.auditStatus !== 'SUBMITTED') throw new Error('当前出库单不允许确认')
  order.auditStatus = 'APPROVED'
  order.outboundTime = now
  order.remark = data && data.remark ? data.remark : order.remark
  order.updatedTime = now
  return order
}

function updateEquipment(data) {
  if (!data || !compact(data.equipmentId)) throw new Error('设备不能为空')
  const equipment = findById(equipments, 'equipmentId', data.equipmentId)
  if (!equipment) throw new Error('设备不存在')
  Object.assign(equipment, data, { updatedTime: now })
  return equipment
}

function assignRepairOrder(id, data) {
  const order = findById(repairOrders, 'repairId', id)
  if (!order) throw new Error('维修单不存在')
  if (order.status !== 'SUBMITTED') throw new Error('只有待派工维修单可以派工')
  order.repairerId = Number(data && data.repairerId || 1002)
  order.status = 'ASSIGNED'
  order.assignedTime = now
  order.remark = data && data.remark ? data.remark : order.remark
  order.updatedTime = now
  const equipment = findById(equipments, 'equipmentId', order.equipmentId)
  if (equipment) {
    equipment.status = 'MAINTENANCE'
    equipment.updatedTime = now
  }
  return order
}

function closeRepairOrder(id, data) {
  const order = findById(repairOrders, 'repairId', id)
  if (!order) throw new Error('维修单不存在')
  if (!['ASSIGNED', 'PROCESSING'].includes(order.status)) throw new Error('当前维修单不允许关闭')
  order.status = 'CLOSED'
  order.repairResult = data && data.repairResult ? data.repairResult : '小程序确认维修完成'
  order.repairCost = Number(data && data.repairCost || order.repairCost || 0)
  order.closedTime = now
  order.updatedTime = now
  const equipment = findById(equipments, 'equipmentId', order.equipmentId)
  if (equipment) {
    equipment.status = 'RUNNING'
    equipment.updatedTime = now
  }
  return order
}

function scanBarcode(value) {
  return barcodes.find((item) => item.barcodeValue === value) || null
}

function clone(value) {
  if (value === undefined || value === null) return value
  return JSON.parse(JSON.stringify(value))
}

function normalizeUrl(value) {
  let url = String(value || '')
  const apiIndex = url.indexOf('/api/')
  if (apiIndex >= 0) url = url.slice(apiIndex)
  url = url.split('?')[0]
  return url.length > 1 ? url.replace(/\/+$/, '') : url
}

function route(config) {
  const url = normalizeUrl(config.url)
  const data = config.data || {}
  const method = String(config.method || 'GET').toUpperCase()

  if (url === '/api/auth/login') return { token: 'mock-token', data: currentUser, ...currentUser }
  if (url === '/api/auth/info') return currentUser
  if (url === '/api/auth/logout') return true
  if (url === '/api/mdm/product/page') return pageProducts(data)
  if (url === '/api/mdm/customer/page') return pageCustomers(data)
  if (url === '/api/mdm/material/page') return pageMaterials(data)
  if (url === '/api/process/operation/page') return pageOperations(data)
  if (url === '/api/factory/station/page') return pageStations(data)
  if (url === '/api/org/employee/page') return pageEmployees(data)
  if (url === '/api/wms/warehouse/page') return pageWarehouses(data)
  if (url === '/api/wms/location/page') return pageLocations(data)
  if (url === '/api/mfg/production-order/page') return pageProductionOrders(data)
  if (url === '/api/mfg/work-order/page') return pageWorkOrders(data)
  if (url === '/api/mfg/production-plan/page') return pageProductionPlans(data)
  if (url === '/api/mfg/task/page') return pageTasks(data)
  if (url === '/api/mfg/work-report/page') return pageReports(data)
  if (url === '/api/mfg/work-report/submit') return submitReport(data)
  if (url === '/api/qms/inspection-order/page') return pageInspectionOrders(data)
  if (url === '/api/qms/nonconforming-order/page') return pageNonconformingOrders(data)
  if (url === '/api/qms/nonconforming-order') return createNonconformingOrder(data)
  if (url === '/api/wms/inventory/page') return pageInventories(data)
  if (url === '/api/wms/inbound-order/page') return pageInboundOrders(data)
  if (url === '/api/wms/outbound-order/page') return pageOutboundOrders(data)
  if (url === '/api/eam/equipment/page') return pageEquipments(data)
  if (url === '/api/eam/equipment' && method === 'PUT') return updateEquipment(data)
  if (url === '/api/eam/repair-order/page') return pageRepairOrders(data)

  let matched = url.match(/^\/api\/mfg\/production-order\/(\d+)$/)
  if (matched) return findById(productionOrders, 'productionOrderId', matched[1])

  matched = url.match(/^\/api\/mfg\/work-order\/(\d+)$/)
  if (matched) return findById(workOrders, 'workOrderId', matched[1])

  matched = url.match(/^\/api\/mfg\/production-plan\/(\d+)$/)
  if (matched) return findById(productionPlans, 'planId', matched[1])

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

  matched = url.match(/^\/api\/qms\/inspection-order\/(\d+)\/submit$/)
  if (matched) return submitInspectionOrder(matched[1], data)

  matched = url.match(/^\/api\/qms\/inspection-order\/(\d+)$/)
  if (matched) return findById(inspectionOrders, 'inspectionId', matched[1])

  matched = url.match(/^\/api\/qms\/nonconforming-order\/(\d+)$/)
  if (matched && method === 'PUT') return updateNonconformingOrder(matched[1], data)
  if (matched) return findById(nonconformingOrders, 'ncrId', matched[1])

  matched = url.match(/^\/api\/wms\/inventory\/(\d+)$/)
  if (matched) return findById(inventories, 'inventoryId', matched[1])

  matched = url.match(/^\/api\/wms\/inbound-order\/(\d+)\/confirm$/)
  if (matched) return confirmInboundOrder(matched[1], data)

  matched = url.match(/^\/api\/wms\/inbound-order\/(\d+)$/)
  if (matched) return findById(inboundOrders, 'inboundId', matched[1])

  matched = url.match(/^\/api\/wms\/inbound-detail\/inbound\/(\d+)$/)
  if (matched) return inboundDetails[matched[1]] || []

  matched = url.match(/^\/api\/wms\/outbound-order\/(\d+)\/confirm$/)
  if (matched) return confirmOutboundOrder(matched[1], data)

  matched = url.match(/^\/api\/wms\/outbound-order\/(\d+)$/)
  if (matched) return findById(outboundOrders, 'outboundId', matched[1])

  matched = url.match(/^\/api\/wms\/outbound-detail\/outbound\/(\d+)$/)
  if (matched) return outboundDetails[matched[1]] || []

  matched = url.match(/^\/api\/eam\/equipment\/(\d+)$/)
  if (matched) return findById(equipments, 'equipmentId', matched[1])

  matched = url.match(/^\/api\/eam\/repair-order\/(\d+)\/assign$/)
  if (matched) return assignRepairOrder(matched[1], data)

  matched = url.match(/^\/api\/eam\/repair-order\/(\d+)\/close$/)
  if (matched) return closeRepairOrder(matched[1], data)

  matched = url.match(/^\/api\/eam\/repair-order\/(\d+)$/)
  if (matched) return findById(repairOrders, 'repairId', matched[1])

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
        resolve(clone(result))
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
