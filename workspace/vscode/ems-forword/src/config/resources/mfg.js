import { defineResource, field, optionSets, values } from './helpers'

const sourceTypes = values([['ERP', 'ERP 同步', 'primary'], ['MANUAL', '手工创建', 'success']])
const alertTypes = values([['MATERIAL', '缺料', 'warning'], ['EQUIPMENT', '设备', 'danger'], ['QUALITY', '质量', 'danger'], ['SAFETY', '安全', 'danger'], ['OTHER', '其他', 'info']])
const weekdays = values([[1, '周一', 'primary'], [2, '周二', 'primary'], [3, '周三', 'primary'], [4, '周四', 'primary'], [5, '周五', 'primary'], [6, '周六', 'warning'], [7, '周日', 'info']])

export default [
  defineResource({
    key: 'mfg-line-product-capability', domain: 'mfg', route: 'mfg/line-product-capabilities', title: '产线产品能力', icon: 'route', baseUrl: '/api/mfg/line-product-capability', idField: 'capabilityId',
    description: '维护产品可进入的产线、已发布工艺路线、适配优先级和有效产能系数',
    fields: ['lineId', 'productId', 'routeId', 'priorityLevel', 'capacityFactor', 'status', 'remark', 'createdTime'], filters: ['lineId', 'productId', 'routeId', 'status'], defaults: { priorityLevel: 100, capacityFactor: 1, status: 'ENABLE' },
    permissions: { delete: false, export: false },
    overrides: {
      lineId: { required: true, type: 'reference', reference: 'factory-line' }, productId: { required: true, type: 'reference', reference: 'mdm-product' }, routeId: { required: true, type: 'reference', reference: 'process-route' },
      priorityLevel: { required: true, type: 'number', min: 1, precision: 0 }, capacityFactor: { required: true, type: 'number', min: 0.0001, max: 2, precision: 4, step: 0.1 },
      status: { required: true, type: 'select', options: optionSets.enable }, remark: { type: 'textarea' }, createdTime: { form: false, width: 170 }
    },
    actions: [{ key: 'disable', label: '停用', icon: 'close', method: 'put', path: (row) => `/api/mfg/line-product-capability/${row.capabilityId}/disable`, visible: (row) => row.status === 'ENABLE', previewPatch: () => ({ status: 'DISABLE' }) }]
  }),
  defineResource({
    key: 'mfg-line-calendar-rule', domain: 'mfg', route: 'mfg/line-calendar-rules', title: '产线周历规则', icon: 'calendar', baseUrl: '/api/mfg/line-calendar-rule', idField: 'calendarRuleId',
    description: '维护产线按星期和班次生效的排产日历与有效产能系数',
    fields: ['lineId', 'dayOfWeek', 'shiftId', 'effectiveFrom', 'effectiveTo', 'capacityFactor', 'status', 'remark', 'createdTime'], filters: ['lineId', 'dayOfWeek', 'shiftId', 'status'], defaults: { capacityFactor: 1, status: 'ENABLE' },
    permissions: { delete: false, export: false },
    overrides: {
      lineId: { required: true, type: 'reference', reference: 'factory-line' }, dayOfWeek: { required: true, type: 'select', options: weekdays }, shiftId: { required: true, type: 'reference', reference: 'org-shift' },
      effectiveFrom: { required: true, type: 'date' }, effectiveTo: { type: 'date' }, capacityFactor: { required: true, type: 'number', min: 0.0001, max: 2, precision: 4, step: 0.1 },
      status: { required: true, type: 'select', options: optionSets.enable }, remark: { type: 'textarea' }, createdTime: { form: false, width: 170 }
    },
    actions: [{ key: 'disable', label: '停用', icon: 'close', method: 'put', path: (row) => `/api/mfg/line-calendar-rule/${row.calendarRuleId}/disable`, visible: (row) => row.status === 'ENABLE', previewPatch: () => ({ status: 'DISABLE' }) }]
  }),
  defineResource({
    key: 'mfg-production-order', domain: 'mfg', route: 'mfg/production-orders', title: '生产订单', icon: 'clipboard', baseUrl: '/api/mfg/production-order', idField: 'productionOrderId',
    description: '管理客户生产需求、交付日期和订单执行状态',
    fields: ['productionOrderNo', 'customerId', 'productId', 'orderQuantity', 'deliveryDate', 'sourceType', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status', 'customerId', 'productId'], defaults: { sourceType: 'MANUAL', status: 'DRAFT' },
    overrides: { productionOrderNo: { required: true }, customerId: { required: true }, productId: { required: true }, orderQuantity: { required: true, type: 'number', min: 0 }, deliveryDate: { required: true }, sourceType: { type: 'select', options: sourceTypes }, status: { type: 'select', options: optionSets.productionOrder }, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'mfg-production-plan', domain: 'mfg', route: 'mfg/production-plans', title: '生产计划', icon: 'calendar', baseUrl: '/api/mfg/production-plan', idField: 'planId',
    fields: ['planNo', 'productionOrderId', 'lineId', 'plannedStartTime', 'plannedEndTime', 'plannedQuantity', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status', 'productionOrderId', 'lineId'], defaults: { status: 'DRAFT' },
    overrides: { planNo: { required: true }, productionOrderId: { required: true }, lineId: { required: true }, plannedStartTime: { required: true }, plannedEndTime: { required: true }, plannedQuantity: { required: true, type: 'number', min: 0 }, status: { type: 'select', options: optionSets.productionPlan }, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'mfg-work-order', domain: 'mfg', route: 'mfg/work-orders', title: '生产工单', icon: 'package', baseUrl: '/api/mfg/work-order', idField: 'workOrderId',
    fields: ['workOrderNo', 'planId', 'productId', 'bomId', 'routeId', 'plannedQuantity', 'completedQuantity', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status', 'planId', 'productId'], defaults: { status: 'DRAFT', completedQuantity: 0 },
    overrides: { workOrderNo: { required: true }, planId: { reference: 'mfg-production-plan', required: true }, productId: { required: true }, bomId: { required: true }, routeId: { required: true }, plannedQuantity: { required: true, type: 'number', min: 0 }, completedQuantity: { type: 'number', min: 0 }, status: { type: 'select', options: optionSets.workOrder }, createdTime: { form: false, width: 170 } },
    relations: [{ key: 'materials', title: '工单用料需求', resource: 'mfg-work-order-material', parentField: 'workOrderId', endpoint: (row) => `/api/mfg/work-order-material/work-order/${row.workOrderId}` }]
  }),
  defineResource({
    key: 'mfg-work-order-material', domain: 'mfg', route: 'mfg/work-order-materials', title: '工单用料需求', baseUrl: '/api/mfg/work-order-material', idField: 'requirementId', menu: false,
    fields: ['workOrderId', 'materialId', 'requiredQuantity', 'issuedQuantity', 'shortageQuantity', 'status', 'remark'], filters: ['workOrderId', 'materialId', 'status'], defaults: { status: 'WAIT_READY', issuedQuantity: 0, shortageQuantity: 0 },
    overrides: { workOrderId: { required: true }, materialId: { required: true }, requiredQuantity: { required: true, type: 'number', min: 0 }, issuedQuantity: { type: 'number', min: 0 }, shortageQuantity: { type: 'number', min: 0 }, status: { type: 'select', options: optionSets.readiness } }
  }),
  defineResource({
    key: 'mfg-dispatch-order', domain: 'mfg', route: 'mfg/dispatch-orders', title: '派工管理', icon: 'users', baseUrl: '/api/mfg/dispatch-order', idField: 'dispatchId',
    fields: ['dispatchNo', 'workOrderId', 'teamId', 'plannedQuantity', 'dueTime', 'materialReadyStatus', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status', 'workOrderId', 'teamId'], defaults: { materialReadyStatus: 'WAIT_READY', status: 'WAIT_DISPATCH' },
    overrides: { dispatchNo: { required: true }, workOrderId: { required: true }, teamId: { required: true }, plannedQuantity: { required: true, type: 'number', min: 0 }, dueTime: { required: true }, materialReadyStatus: { type: 'select', options: optionSets.readiness }, status: { type: 'select', options: optionSets.dispatch }, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'mfg-task', domain: 'mfg', route: 'mfg/tasks', title: '生产任务', icon: 'activity', baseUrl: '/api/mfg/task', idField: 'taskId',
    fields: ['taskNo', 'dispatchId', 'workOrderId', 'operationId', 'stationId', 'plannedQuantity', 'completedQuantity', 'defectQuantity', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status', 'dispatchId', 'workOrderId', 'operationId', 'stationId'], defaults: { status: 'WAIT_START', completedQuantity: 0, defectQuantity: 0 },
    overrides: { taskNo: { required: true }, dispatchId: { required: true }, workOrderId: { required: true }, operationId: { required: true }, stationId: { required: true }, plannedQuantity: { required: true, type: 'number', min: 0 }, completedQuantity: { type: 'number', min: 0, create: false, edit: false }, defectQuantity: { type: 'number', min: 0, create: false, edit: false }, status: { type: 'select', options: optionSets.task, create: false, edit: false }, createdTime: { form: false, width: 170 } },
    canEdit: (row) => row.status === 'WAIT_START', canDelete: (row) => row.status === 'WAIT_START', lockedReason: '已开工或已结束的生产任务不可普通修改或删除',
    actions: [
      { key: 'start', label: '开始任务', icon: 'play', method: 'put', type: 'primary', path: (row) => `/api/mfg/task/${row.taskId}/start`, visible: (row) => row.status === 'WAIT_START', fields: [field('stationId', { label: '实际工位', type: 'reference', reference: 'factory-station' })], previewPatch: (values) => ({ status: 'RUNNING', ...(values.stationId ? { stationId: values.stationId } : {}) }) },
      { key: 'finish', label: '完成任务', icon: 'check', method: 'put', type: 'success', path: (row) => `/api/mfg/task/${row.taskId}/finish`, visible: (row) => ['RUNNING', 'PAUSED'].includes(row.status), fields: [field('completedQuantity', { required: true, type: 'number', min: 0 }), field('defectQuantity', { required: true, type: 'number', min: 0 }), field('remark', { type: 'textarea' })], previewPatch: (values) => ({ completedQuantity: values.completedQuantity, defectQuantity: values.defectQuantity, ...(values.remark ? { remark: values.remark } : {}), status: 'FINISHED' }) }
    ],
    relations: [{ key: 'operators', title: '任务人员', resource: 'mfg-task-operator', parentField: 'taskId', endpoint: (row) => `/api/mfg/task-operator/task/${row.taskId}` }]
  }),
  defineResource({ key: 'mfg-task-operator', domain: 'mfg', route: 'mfg/task-operators', title: '任务人员', baseUrl: '/api/mfg/task-operator', idField: 'id', menu: false, fields: ['taskId', 'employeeId', 'roleInTask', 'assignedTime'], filters: ['taskId', 'employeeId', 'roleInTask'], overrides: { taskId: { required: true }, employeeId: { required: true }, roleInTask: { required: true }, assignedTime: { required: true } } }),
  defineResource({
    key: 'mfg-work-report', domain: 'mfg', route: 'mfg/work-reports', title: '生产报工', icon: 'clipboard', baseUrl: '/api/mfg/work-report', idField: 'reportId',
    fields: ['reportNo', 'taskId', 'operatorId', 'stationId', 'qualifiedQuantity', 'defectQuantity', 'actualWorkHours', 'reportTime', 'auditStatus', 'remark', 'createdTime'], filters: ['keyword', 'taskId', 'operatorId', 'stationId', 'auditStatus'], defaults: { auditStatus: 'SUBMITTED', qualifiedQuantity: 0, defectQuantity: 0 },
    overrides: { reportNo: { required: true }, taskId: { required: true }, operatorId: { required: true }, stationId: { required: true }, qualifiedQuantity: { required: true, type: 'number', min: 0 }, defectQuantity: { required: true, type: 'number', min: 0 }, actualWorkHours: { type: 'number', min: 0 }, reportTime: { required: true }, auditStatus: { type: 'select', options: optionSets.audit, create: false, edit: false }, createdTime: { form: false, width: 170 } },
    fullChild: { property: 'materials', title: '报工用料', resource: 'mfg-report-material', parentField: 'reportId', endpoint: (row) => `/api/mfg/report-material/report/${row.reportId}` },
    createPath: '/full', updatePath: '/full',
    canEdit: (row) => row.auditStatus !== 'APPROVED', canDelete: (row) => row.auditStatus !== 'APPROVED', lockedReason: '已审核通过的报工单不可修改或删除',
    actions: [{ key: 'audit', label: '审核报工', icon: 'shield-check', method: 'put', type: 'success', path: (row) => `/api/mfg/work-report/${row.reportId}/audit`, visible: (row) => row.auditStatus === 'SUBMITTED', fields: [field('auditResult', { label: '审核结果', type: 'select', options: optionSets.audit.filter((item) => item.value !== 'SUBMITTED'), required: true }), field('auditComment', { label: '审核意见', type: 'textarea' })], previewPatch: (values) => ({ auditStatus: values.auditResult }) }]
  }),
  defineResource({ key: 'mfg-report-material', domain: 'mfg', route: 'mfg/report-materials', title: '报工用料', baseUrl: '/api/mfg/report-material', idField: 'id', menu: false, fields: ['reportId', 'materialId', 'batchId', 'usedQuantity', 'scrapQuantity', 'remark'], filters: ['reportId', 'materialId', 'batchId'], overrides: { reportId: { required: true }, materialId: { required: true }, batchId: { required: true }, usedQuantity: { required: true, type: 'number', min: 0 }, scrapQuantity: { type: 'number', min: 0 } } }),
  defineResource({
    key: 'mfg-exception', domain: 'mfg', route: 'mfg/exceptions', title: '生产异常', icon: 'alert', baseUrl: '/api/mfg/exception', idField: 'exceptionId',
    fields: ['exceptionNo', 'exceptionType', 'sourceAlertId', 'taskId', 'stationId', 'reportedBy', 'description', 'handleResult', 'status', 'remark', 'createdTime'], filters: ['keyword', 'exceptionType', 'status', 'taskId', 'stationId'], defaults: { status: 'SUBMITTED' },
    overrides: { exceptionNo: { required: true }, exceptionType: { required: true, type: 'select', options: alertTypes }, sourceAlertId: { type: 'reference', reference: 'mfg-andon-alert' }, taskId: { required: true }, stationId: { required: true }, reportedBy: { required: true }, description: { required: true, type: 'textarea' }, handleResult: { type: 'textarea' }, status: { type: 'select', options: optionSets.exception }, createdTime: { form: false, width: 170 } },
    actions: [{ key: 'handle', label: '处理异常', icon: 'wrench', method: 'put', type: 'success', path: (row) => `/api/mfg/exception/${row.exceptionId}/handle`, visible: (row) => !['RESOLVED', 'CLOSED'].includes(row.status), fields: [field('handleResult', { type: 'textarea', required: true }), field('remark', { type: 'textarea' })], previewPatch: (values) => ({ handleResult: values.handleResult, ...(values.remark ? { remark: values.remark } : {}), status: 'RESOLVED' }) }]
  }),
  defineResource({
    key: 'mfg-andon-alert', domain: 'mfg', route: 'mfg/andon-alerts', title: '安灯告警', icon: 'alert', baseUrl: '/api/mfg/andon-alert', idField: 'alertId', createPath: '/trigger',
    fields: ['alertNo', 'lineId', 'stationId', 'triggeredBy', 'alertType', 'triggerTime', 'responseTime', 'resolvedTime', 'status', 'remark', 'createdTime'], filters: ['keyword', 'alertType', 'status', 'lineId', 'stationId'], defaults: { status: 'ACTIVE' },
    overrides: { alertNo: { required: true }, lineId: { required: true }, stationId: { required: true }, triggeredBy: { required: true }, alertType: { required: true, type: 'select', options: alertTypes }, triggerTime: { form: false, width: 170 }, responseTime: { form: false, width: 170 }, resolvedTime: { form: false, width: 170 }, status: { type: 'select', options: optionSets.andon, create: false, edit: false }, createdTime: { form: false, table: false } },
    actions: [{ key: 'close', label: '解除告警', icon: 'check', method: 'put', type: 'success', path: (row) => `/api/mfg/andon-alert/${row.alertId}/close`, visible: (row) => row.status !== 'CLOSED', fields: [field('handleResult', { label: '解除说明', type: 'textarea', required: true })], previewPatch: () => ({ status: 'CLOSED', resolvedTime: new Date().toISOString().slice(0, 19) }) }]
  })
]
