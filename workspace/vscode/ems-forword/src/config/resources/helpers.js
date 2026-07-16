const option = (value, label, type = 'info') => ({ value, label, type })

export const optionSets = {
  enable: [option('ENABLE', '启用', 'success'), option('DISABLE', '停用', 'info')],
  userStatus: [
    option('ENABLE', '正常', 'success'),
    option('DISABLE', '停用', 'info'),
    option('LOCKED', '锁定', 'danger')
  ],
  employeeStatus: [
    option('ENABLE', '在职', 'success'),
    option('RESIGNED', '离职', 'info'),
    option('DISABLE', '停用', 'info')
  ],
  release: [
    option('DRAFT', '草稿', 'info'),
    option('RELEASED', '已发布', 'success'),
    option('DISABLE', '停用', 'info')
  ],
  productionOrder: [
    option('DRAFT', '新建', 'info'),
    option('RELEASED', '已排产', 'primary'),
    option('RUNNING', '生产中', 'warning'),
    option('FINISHED', '已完成', 'success'),
    option('CLOSED', '已关闭', 'info')
  ],
  productionPlan: [
    option('DRAFT', '待生效', 'info'),
    option('RELEASED', '已生效', 'primary'),
    option('FINISHED', '已完成', 'success'),
    option('CLOSED', '已取消', 'info')
  ],
  workOrder: [
    option('DRAFT', '新建', 'info'),
    option('APPROVED', '已审核', 'primary'),
    option('RELEASED', '已下达', 'primary'),
    option('RUNNING', '生产中', 'warning'),
    option('FINISHED', '已完成', 'success'),
    option('CLOSED', '已关闭', 'info')
  ],
  readiness: [
    option('WAIT_READY', '待齐套', 'info'),
    option('PARTIAL_READY', '部分齐套', 'warning'),
    option('READY', '已齐套', 'success')
  ],
  dispatch: [
    option('WAIT_DISPATCH', '待派发', 'info'),
    option('RELEASED', '已派发', 'primary'),
    option('ACCEPTED', '已接受', 'primary'),
    option('RUNNING', '生产中', 'warning'),
    option('FINISHED', '已完成', 'success'),
    option('REJECTED', '已驳回', 'danger')
  ],
  task: [
    option('WAIT_START', '待开工', 'info'),
    option('RUNNING', '生产中', 'warning'),
    option('PAUSED', '暂停', 'warning'),
    option('FINISHED', '已完工', 'success'),
    option('CLOSED', '已关闭', 'info')
  ],
  audit: [
    option('SUBMITTED', '待审核', 'warning'),
    option('APPROVED', '已通过', 'success'),
    option('REJECTED', '已驳回', 'danger'),
    option('WITHDRAWN', '已撤回', 'info'),
    option('CLOSED', '已关闭', 'info')
  ],
  approval: [
    option('SUBMITTED', '待审批', 'warning'),
    option('APPROVED', '已通过', 'success'),
    option('REJECTED', '已驳回', 'danger')
  ],
  exception: [
    option('SUBMITTED', '待处理', 'warning'),
    option('PROCESSING', '处理中', 'primary'),
    option('RESOLVED', '已解决', 'success'),
    option('CLOSED', '已关闭', 'info')
  ],
  andon: [
    option('ACTIVE', '告警中', 'danger'),
    option('PROCESSING', '处理中', 'warning'),
    option('CLOSED', '已解除', 'success')
  ],
  warehouse: [option('ENABLE', '启用', 'success'), option('DISABLE', '停用', 'info')],
  location: [
    option('IDLE', '空闲', 'success'),
    option('OCCUPIED', '占用', 'primary'),
    option('LOCKED', '锁定', 'danger'),
    option('MAINTAINING', '维护', 'warning')
  ],
  batchQuality: [
    option('WAIT_INSPECTION', '待检', 'warning'),
    option('PASS', '合格', 'success'),
    option('FAIL', '不合格', 'danger'),
    option('CONCESSION', '让步接收', 'warning')
  ],
  batchStatus: [
    option('AVAILABLE', '可用', 'success'),
    option('FROZEN', '冻结', 'warning'),
    option('EXHAUSTED', '耗尽', 'info'),
    option('SCRAPPED', '报废', 'danger')
  ],
  stocktake: [
    option('WAIT_START', '待盘', 'info'),
    option('RUNNING', '盘点中', 'warning'),
    option('FINISHED', '已完成', 'success'),
    option('CLOSED', '已关闭', 'info')
  ],
  inspectionResult: [
    option('PASS', '合格', 'success'),
    option('FAIL', '不合格', 'danger'),
    option('CONCESSION', '让步接收', 'warning')
  ],
  inspectionOrder: [
    option('DRAFT', '待检', 'info'),
    option('RUNNING', '检验中', 'warning'),
    option('FINISHED', '已完成', 'success'),
    option('CLOSED', '已撤回', 'info')
  ],
  ncrStatus: [
    option('SUBMITTED', '待处理', 'warning'),
    option('PROCESSING', '处理中', 'primary'),
    option('CLOSED', '已关闭', 'success')
  ],
  equipment: [
    option('IDLE', '空闲', 'info'),
    option('RUNNING', '运行', 'success'),
    option('MAINTAINING', '保养中', 'warning'),
    option('REPAIRING', '维修中', 'warning'),
    option('STOPPED', '停机', 'info'),
    option('FAULT', '故障', 'danger'),
    option('SCRAPPED', '报废', 'danger')
  ],
  repair: [
    option('SUBMITTED', '待派工', 'warning'),
    option('REPAIRING', '维修中', 'primary'),
    option('WAIT_ACCEPTANCE', '待验收', 'warning'),
    option('CLOSED', '已关闭', 'success')
  ],
  barcode: [
    option('ENABLE', '启用', 'success'),
    option('DISABLE', '停用', 'info'),
    option('INVALIDATED', '已作废', 'danger')
  ],
  productLot: [
    option('RUNNING', '生产中', 'primary'),
    option('WAIT_INSPECTION', '待检', 'warning'),
    option('PASS', '合格', 'success'),
    option('CONCESSION', '让步放行', 'warning'),
    option('FAIL', '不合格隔离', 'danger'),
    option('STORED', '已入库', 'success'),
    option('CLOSED', '已关闭', 'info')
  ],
  yesNo: [option(1, '是', 'success'), option(0, '否', 'info')]
}

const labels = {
  keyword: '关键字', username: '用户名', passwordHash: '初始密码', employeeId: '员工', status: '状态',
  lastLoginTime: '最后登录时间', roleCode: '角色编码', roleName: '角色名称', dataScope: '数据范围',
  menuName: '菜单名称', menuType: '菜单类型', permissionCode: '权限标识', routePath: '路由地址', parentId: '上级节点',
  dictTypeCode: '字典类型编码', dictTypeName: '字典类型名称', dictValue: '字典值', dictLabel: '字典标签', sortNo: '排序号',
  paramKey: '参数键', paramValue: '参数值', paramName: '参数名称', moduleCode: '模块编码',
  fileName: '文件名称', fileUrl: '文件地址', fileType: '文件类型', bizType: '业务类型', bizId: '业务 ID',
  userId: '用户', operationType: '操作类型', requestIp: '请求 IP', operationTime: '操作时间',
  oldStatus: '原状态', newStatus: '新状态', changeReason: '变更原因', changedBy: '变更人', changedTime: '变更时间',
  approvalNode: '审批节点', approverId: '审批人', approvalResult: '审批结果', approvalComment: '审批意见', approvalTime: '审批时间',
  departmentCode: '部门编码', departmentName: '部门名称', managerId: '负责人', employeeNo: '员工工号', employeeName: '员工姓名',
  positionName: '岗位名称', skillLevel: '技能等级', phone: '联系电话', teamCode: '班组编码', teamName: '班组名称', shiftId: '班次',
  leaderId: '班组长', teamId: '班组', joinDate: '加入日期', leaveDate: '离开日期', shiftCode: '班次编码', shiftName: '班次名称',
  startTime: '开始时间', endTime: '结束时间', unitCode: '单位编码', unitName: '单位名称', precisionScale: '精度位数',
  supplierCode: '供应商编码', supplierName: '供应商名称', customerCode: '客户编码', customerName: '客户名称',
  contactName: '联系人', contactPhone: '联系电话', categoryCode: '分类编码', categoryName: '分类名称', categoryId: '所属分类',
  materialCode: '物料编码', materialName: '物料名称', specModel: '规格型号', unitId: '计量单位', materialType: '物料类型',
  productCode: '产品编码', productName: '产品名称', productModel: '产品型号', materialId: '物料',
  bomCode: '物料清单编码', bomVersion: '物料清单版本', effectiveDate: '生效日期', bomId: '物料清单', quantity: '数量', lossRate: '损耗率', sequenceNo: '顺序号',
  operationCode: '工序编码', operationName: '工序名称', operationId: '工序', standardTime: '标准工时',
  parameterName: '参数名称', standardValue: '标准值', upperLimit: '上限', lowerLimit: '下限',
  routeCode: '路线编码', routeName: '路线名称', routeId: '工艺路线', version: '版本', qcRequired: '需要质检',
  instructionId: '作业指导书', title: '标题', fileId: '附件', effectiveTime: '生效时间',
  workshopCode: '车间编码', workshopName: '车间名称', workshopId: '车间', lineCode: '产线编码', lineName: '产线名称', lineId: '产线',
  capabilityId: '产线产品能力', calendarRuleId: '产线周历规则', priorityLevel: '适配优先级', capacityFactor: '产能系数',
  dayOfWeek: '星期', effectiveFrom: '生效日期', effectiveTo: '失效日期',
  dailyCapacity: '日产能', stationCode: '工位编码', stationName: '工位名称', stationId: '工位', stationType: '工位类型', isDefault: '默认工序',
  productionOrderNo: '生产订单号', productionOrderId: '生产订单', customerId: '客户', productId: '产品', orderQuantity: '订单数量', deliveryDate: '交付日期', sourceType: '来源类型',
  planNo: '计划编号', planId: '生产计划', plannedStartTime: '计划开始', plannedEndTime: '计划结束', plannedQuantity: '计划数量',
  workOrderNo: '工单编号', workOrderId: '生产工单', completedQuantity: '完成数量', requiredQuantity: '需求数量', issuedQuantity: '已发数量', shortageQuantity: '缺料数量',
  dispatchNo: '派工单号', dispatchId: '派工单', dueTime: '要求完成时间', materialReadyStatus: '齐套状态',
  taskNo: '任务编号', taskId: '生产任务', defectQuantity: '不良数量', roleInTask: '任务角色', assignedTime: '分配时间',
  reportNo: '报工单号', reportId: '报工单', operatorId: '操作员工', qualifiedQuantity: '合格数量', actualWorkHours: '实际工时', reportTime: '报工时间', auditStatus: '审核状态',
  usedQuantity: '使用数量', scrapQuantity: '报废数量', batchId: '物料批次',
  exceptionNo: '异常编号', exceptionId: '生产异常', exceptionType: '异常类型', sourceAlertId: '来源安灯', reportedBy: '报告人', description: '异常描述', handleResult: '处理结果',
  alertNo: '安灯编号', alertType: '告警类型', triggeredBy: '触发人', triggerTime: '触发时间', responseTime: '响应时间', resolvedTime: '解除时间',
  warehouseCode: '仓库编码', warehouseName: '仓库名称', warehouseId: '仓库', warehouseType: '仓库类型',
  locationCode: '库位编码', locationName: '库位名称', locationId: '库位', locationType: '库位类型',
  batchNo: '批次号', supplierId: '供应商', supplierBatchNo: '供应商批次', productionDate: '生产日期', receivedDate: '收货日期', qualityStatus: '质量状态', sourceOrderType: '来源单据类型', sourceOrderId: '来源单据 ID', sourceProductLotId: '来源产品批次',
  availableQuantity: '可用数量', lockedQuantity: '锁定数量', inspectionQuantity: '待检数量',
  movementNo: '流水编号', movementType: '变动类型', direction: '方向', operationTime: '操作时间',
  inboundNo: '入库单号', inboundId: '入库单', inboundType: '入库类型', inboundTime: '入库时间', inboundQuantity: '入库数量',
  outboundNo: '出库单号', outboundId: '出库单', outboundType: '出库类型', applicantId: '申请人', outboundTime: '出库时间', outboundQuantity: '出库数量',
  returnNo: '退料单号', returnId: '退料单', returnType: '退料类型', returnTime: '退料时间', returnQuantity: '退料数量', returnReason: '退料原因',
  stocktakeNo: '盘点单号', stocktakeId: '盘点单', stocktakeType: '盘点类型', stocktakeStatus: '盘点状态', startedTime: '开始盘点时间', finishedTime: '完成盘点时间', bookQuantity: '账面数量', actualQuantity: '实盘数量', differenceQuantity: '差异数量',
  suggestionNo: '建议编号', suggestionId: '补货建议', suggestedQuantity: '建议数量',
  itemCode: '检验项目编码', itemName: '检验项目名称', itemId: '检验项目', itemType: '项目类型', inspectionMethod: '检验方法',
  schemeCode: '方案编码', schemeName: '方案名称', schemeId: '检验方案', inspectionType: '检验类型', standardId: '检验标准', sampleQuantity: '抽样数量', judgementRule: '判定规则',
  inspectionNo: '检验单号', inspectionId: '检验单', inspectorId: '检验员', inspectionResult: '检验结果',
  measuredValue: '实测值', result: '判定结果', defectDescription: '缺陷描述', photoFileId: '检验照片',
  ncrNo: '不合格品单号', ncrId: '不合格品单', productLotId: '产品批次', sourceReportId: '来源报工', defectReason: '不合格原因', dispositionType: '处置方式', approvalStatus: '审批状态', barcodeId: '条码', inspectionResultId: '检验结果', finalDisposition: '最终处置', recheckResult: '复检结果',
  equipmentCode: '设备编码', equipmentName: '设备名称', equipmentId: '设备', model: '设备型号', manufacturer: '制造商', purchaseDate: '购置日期',
  runningStatus: '运行状态', downtimeType: '停机类型', alertLevel: '告警等级', collectedTime: '采集时间',
  maintenanceType: '保养类型', cycleType: '周期类型', cycleValue: '周期值', maintenanceContent: '保养内容', responsibleId: '负责人',
  recordId: '记录', maintainerId: '保养人', maintenanceTime: '保养时间', maintenanceResult: '保养结果', nextMaintenanceTime: '下次保养时间',
  checkCycle: '点检周期', checkItems: '点检项目', responsibleRole: '负责角色', checkerId: '点检人', checkTime: '点检时间', checkResult: '点检结果', abnormalDescription: '异常描述',
  repairNo: '维修单号', repairId: '维修单', reportedTime: '报修时间', faultDescription: '故障描述', repairerId: '维修人', repairResult: '维修结果', repairCost: '维修费用',
  barcodeValue: '条码内容', barcodeType: '条码类型', generatedRule: '生成规则', lotNo: '产品批次号', batchQuantity: '批次数量', usageId: '用料记录',
  remark: '备注', createdTime: '创建时间', updatedTime: '更新时间'
}

// 关联字段在数据库中保存 ID，但下拉框不能只显示数字。
// 这里规定每种资源用哪些可读字段拼成选项文字，例如设备显示“设备名称 + 设备编码”。
export const referencePresentation = {
  'sys-user': ['username'], 'sys-role': ['roleName', 'roleCode'], 'sys-menu': ['menuName'], 'sys-file': ['fileName'],
  'org-department': ['departmentName', 'departmentCode'], 'org-employee': ['employeeName', 'employeeNo'], 'org-team': ['teamName', 'teamCode'], 'org-shift': ['shiftName', 'shiftCode'],
  'mdm-unit': ['unitName', 'unitCode'], 'mdm-supplier': ['supplierName', 'supplierCode'], 'mdm-customer': ['customerName', 'customerCode'],
  'mdm-material-category': ['categoryName', 'categoryCode'], 'mdm-material': ['materialName', 'materialCode'], 'mdm-product': ['productName', 'productCode'], 'mdm-bom': ['bomCode', 'bomVersion'],
  'process-operation': ['operationName', 'operationCode'], 'process-route': ['routeName', 'routeCode'],
  'factory-workshop': ['workshopName', 'workshopCode'], 'factory-line': ['lineName', 'lineCode'], 'factory-station': ['stationName', 'stationCode'],
  'mfg-production-order': ['productionOrderNo'], 'mfg-production-plan': ['planNo'], 'mfg-work-order': ['workOrderNo'], 'mfg-dispatch-order': ['dispatchNo'], 'mfg-task': ['taskNo'], 'mfg-work-report': ['reportNo'], 'mfg-exception': ['exceptionNo'], 'mfg-andon-alert': ['alertNo'],
  'wms-warehouse': ['warehouseName', 'warehouseCode'], 'wms-location': ['locationName', 'locationCode'], 'wms-material-batch': ['batchNo'],
  'qms-inspection-item': ['itemName', 'itemCode'], 'qms-inspection-standard': ['standardValue'], 'qms-inspection-scheme': ['schemeName', 'schemeCode'], 'qms-inspection-order': ['inspectionNo'], 'qms-inspection-result': ['measuredValue'], 'qms-nonconforming-order': ['ncrNo'],
  'eam-equipment': ['equipmentName', 'equipmentCode'], 'eam-maintenance-plan': ['maintenanceType'], 'eam-spot-check-plan': ['checkCycle'],
  'trace-barcode': ['barcodeValue'], 'trace-product-lot': ['lotNo']
}

// 根据常见的“xxxId”字段名，推断它关联哪一种资源。
// 例如 productId 自动关联 mdm-product，页面就能加载产品下拉选项。
const defaultReferences = {
  userId: 'sys-user', approverId: 'org-employee', changedBy: 'org-employee', employeeId: 'org-employee', managerId: 'org-employee', leaderId: 'org-employee', operatorId: 'org-employee', applicantId: 'org-employee', inspectorId: 'org-employee', maintainerId: 'org-employee', checkerId: 'org-employee', repairerId: 'org-employee', responsibleId: 'org-employee', reportedBy: 'org-employee', triggeredBy: 'org-employee',
  roleId: 'sys-role', menuId: 'sys-menu', fileId: 'sys-file', photoFileId: 'sys-file', departmentId: 'org-department', teamId: 'org-team', shiftId: 'org-shift', unitId: 'mdm-unit', supplierId: 'mdm-supplier', customerId: 'mdm-customer', materialId: 'mdm-material', productId: 'mdm-product', bomId: 'mdm-bom', operationId: 'process-operation', routeId: 'process-route', workshopId: 'factory-workshop', lineId: 'factory-line', stationId: 'factory-station', productionOrderId: 'mfg-production-order', workOrderId: 'mfg-work-order', dispatchId: 'mfg-dispatch-order', taskId: 'mfg-task', reportId: 'mfg-work-report', warehouseId: 'wms-warehouse', locationId: 'wms-location', batchId: 'wms-material-batch', schemeId: 'qms-inspection-scheme', itemId: 'qms-inspection-item', standardId: 'qms-inspection-standard', inspectionId: 'qms-inspection-order', inspectionResultId: 'qms-inspection-result', ncrId: 'qms-nonconforming-order', equipmentId: 'eam-equipment', exceptionId: 'mfg-exception', barcodeId: 'trace-barcode', productLotId: 'trace-product-lot'
}

const dateProps = new Set(['deliveryDate', 'effectiveDate', 'joinDate', 'leaveDate', 'productionDate', 'receivedDate', 'purchaseDate'])
const datetimePattern = /(Time|At)$/
const numberPattern = /(Quantity|Rate|Capacity|Cost|Hours|Scale)$/
const textareaPattern = /(remark|description|content|comment|result|reason|items)$/i
const auditProps = new Set(['createdBy', 'updatedBy', 'deletedFlag', 'version'])
const secondaryTableProps = new Set(['remark', 'createdTime', 'updatedTime'])

// 没有预先配置中文名时，把 camelCase 字段名转换成较容易阅读的标题作为兜底。
function humanize(prop) {
  return labels[prop] || prop.replace(/([A-Z])/g, ' $1').replace(/^./, (value) => value.toUpperCase())
}

export function field(prop, overrides = {}) {
  // 先根据字段名推断关联、日期、数字、长文本等控件类型，再用 overrides 覆盖特殊规则。
  // 最终结果会交给 ResourceField.vue，决定页面显示输入框、下拉框还是日期控件。
  let type = overrides.type
  const reference = overrides.reference || defaultReferences[prop]
  if (!type) {
    if (reference) type = 'reference'
    else if (dateProps.has(prop)) type = 'date'
    else if (datetimePattern.test(prop)) type = 'datetime'
    else if (numberPattern.test(prop)) type = 'number'
    else if (textareaPattern.test(prop)) type = 'textarea'
    else type = 'text'
  }
  return {
    prop,
    label: humanize(prop),
    type,
    reference,
    options: overrides.options,
    required: false,
    table: !auditProps.has(prop),
    defaultVisible: !secondaryTableProps.has(prop),
    form: !auditProps.has(prop),
    detail: !auditProps.has(prop),
    min: type === 'number' ? 0 : undefined,
    ...overrides
  }
}

export function defineResource(config) {
  // 将 mfg.js 等文件中的精简配置标准化：补齐字段对象、筛选条件、默认权限、路由路径等。
  // 因此业务资源只需描述差异，不必为每个页面重复编写整套表格和表单代码。
  const overrides = config.overrides || {}
  const fields = config.fields.map((prop) => field(prop, overrides[prop]))
  const fieldMap = Object.fromEntries(fields.map((item) => [item.prop, item]))
  const filters = (config.filters || ['keyword']).map((item) => {
    if (typeof item === 'object') return field(item.prop, { ...fieldMap[item.prop], ...item, form: false, table: false })
    if (item === 'keyword') return field('keyword', { placeholder: `搜索${config.title.replace('管理', '')}`, form: false, table: false })
    return field(item, { ...fieldMap[item], form: false, table: false })
  })
  return {
    icon: 'database',
    description: `${config.title.replace('管理', '')}的查询、维护与数据管理`,
    permissions: { create: true, edit: true, delete: true, export: true },
    defaults: {},
    actions: [],
    menu: true,
    ...config,
    routePath: `/${config.route}`,
    fields,
    filters
  }
}

// 把 [值, 中文标签, 标签颜色] 的简写数组转换成 Element Plus 下拉选项对象。
export function values(entries) {
  return entries.map(([value, label, type]) => option(value, label, type))
}
