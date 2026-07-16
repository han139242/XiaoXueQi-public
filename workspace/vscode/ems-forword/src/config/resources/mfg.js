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
  // 生产订单是理解配置驱动页面的样板：下面一份对象同时描述路由、接口、表格、表单和业务按钮。
  defineResource({
    // 页面身份：属于mfg模块，访问/#/mfg/production-orders，请求/api/mfg/production-order。
    key: 'mfg-production-order',
    domain: 'mfg',
    route: 'mfg/production-orders',
    title: '生产订单',
    icon: 'clipboard',
    baseUrl: '/api/mfg/production-order',
    idField: 'productionOrderId',
    description: '管理客户生产需求、交付日期和订单执行状态',
    // fields决定表格和详情字段，filters决定顶部查询条件，defaults是新增时默认值。
    fields: ['productionOrderNo', 'customerId', 'productId', 'orderQuantity', 'deliveryDate', 'sourceType', 'status', 'remark', 'createdTime'],
    filters: ['keyword', 'status', 'customerId', 'productId'],
    defaults: { sourceType: 'MANUAL', status: 'DRAFT' },
    // overrides细化表单控件：编号和状态由后端/流程控制，客户产品必填，数量使用数字框。
    overrides: {
      productionOrderNo: { create: false, edit: false },
      customerId: { required: true },
      productId: { required: true },
      orderQuantity: { required: true, type: 'number', min: 0 },
      deliveryDate: { required: true },
      sourceType: { type: 'select', options: sourceTypes },
      status: { type: 'select', options: optionSets.productionOrder, create: false, edit: false },
      createdTime: { form: false, width: 170 }
    },
    // 只有DRAFT草稿允许普通编辑和删除，其余状态必须走下达、撤回等业务动作。
    canEdit: (row) => row.status === 'DRAFT', canDelete: (row) => row.status === 'DRAFT', lockedReason: '只有草稿生产订单可以编辑或删除',
    // masterDetail描述1:N关系：选中订单后，下方按productionOrderId查询关联生产计划。
    masterDetail: {
      title: '订单关联生产计划', resource: 'mfg-production-plan', parentField: 'productionOrderId',
      endpoint: () => '/api/mfg/production-plan/page',
      params: (row) => ({ current: 1, size: 100, productionOrderId: row.productionOrderId }),
      label: (row) => row.productionOrderNo || `生产订单 #${row.productionOrderId}`
    },
    // actions生成行内“更多”菜单；visible控制状态，path/method决定调用哪个后端命令接口。
    actions: [
      { key: 'release', label: '提交排产', icon: 'check', method: 'put', type: 'success', path: (row) => `/api/mfg/production-order/${row.productionOrderId}/release`, visible: (row) => row.status === 'DRAFT', fields: [field('remark', { type: 'textarea' })], previewPatch: () => ({ status: 'RELEASED' }) },
      { key: 'generate-plan', label: '编制计划草稿', icon: 'calendar', method: 'post', type: 'primary', detailKey: 'mfg-production-plan', detailCount: 'zero', path: (row) => `/api/mfg/production-plan/generate/${row.productionOrderId}`, visible: (row) => row.status === 'RELEASED', fields: [field('lineId', { required: true, type: 'reference', reference: 'factory-line' }), field('plannedStartTime', { required: true, type: 'datetime' }), field('plannedEndTime', { required: true, type: 'datetime' }), field('plannedQuantity', { required: true, type: 'number', min: 0.0001, defaultValue: (row) => row.orderQuantity }), field('remark', { type: 'textarea' })] },
      { key: 'view-plans', label: '生产计划详情', icon: 'arrow', kind: 'navigate', detailKey: 'mfg-production-plan', detailCount: 'positive', targetResource: 'mfg-production-plan', query: (row) => ({ productionOrderId: row.productionOrderId, from: row.productionOrderNo }) },
      { key: 'withdraw', label: '撤回修改', icon: 'refresh', method: 'put', type: 'warning', path: (row) => `/api/mfg/production-order/${row.productionOrderId}/withdraw`, visible: (row) => row.status === 'RELEASED', fields: [field('remark', { label: '撤回原因（选填）', type: 'textarea' })], previewPatch: () => ({ status: 'DRAFT' }) }
    ]
  }),
  defineResource({
    key: 'mfg-production-plan', domain: 'mfg', route: 'mfg/production-plans', title: '生产计划', icon: 'calendar', baseUrl: '/api/mfg/production-plan', idField: 'planId',
    contextCreateOnly: true,
    fields: ['planNo', 'productionOrderId', 'lineId', 'plannedStartTime', 'plannedEndTime', 'plannedQuantity', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status', 'productionOrderId', 'lineId'], defaults: { status: 'DRAFT' },
    overrides: { planNo: { create: false, edit: false }, productionOrderId: { create: false, edit: false }, lineId: { required: true }, plannedStartTime: { required: true }, plannedEndTime: { required: true }, plannedQuantity: { required: true, type: 'number', min: 0 }, status: { type: 'select', options: optionSets.productionPlan, create: false, edit: false }, createdTime: { form: false, width: 170 } },
    canEdit: (row) => row.status === 'DRAFT', canDelete: (row) => row.status === 'DRAFT', lockedReason: '只有草稿生产计划可以编辑或删除',
    masterDetail: {
      title: '计划关联生产工单', resource: 'mfg-work-order', parentField: 'planId',
      endpoint: () => '/api/mfg/work-order/page',
      params: (row) => ({ current: 1, size: 100, planId: row.planId }),
      label: (row) => row.planNo || `生产计划 #${row.planId}`
    },
    actions: [
      { key: 'release', label: '生效计划', icon: 'check', method: 'put', type: 'success', path: (row) => `/api/mfg/production-plan/${row.planId}/release`, visible: (row) => row.status === 'DRAFT', fields: [field('remark', { type: 'textarea' })], previewPatch: () => ({ status: 'RELEASED' }) },
      { key: 'generate-work-order', label: '生成工单', icon: 'package', method: 'post', type: 'primary', detailKey: 'mfg-work-order', detailCount: 'zero', path: (row) => `/api/mfg/work-order/generate/${row.planId}`, visible: (row) => row.status === 'RELEASED', fields: [field('remark', { type: 'textarea' })] },
      { key: 'view-work-orders', label: '生产工单详情', icon: 'arrow', kind: 'navigate', detailKey: 'mfg-work-order', detailCount: 'positive', targetResource: 'mfg-work-order', query: (row) => ({ planId: row.planId, from: row.planNo }) },
      { key: 'withdraw', label: '撤回修改', icon: 'refresh', method: 'put', type: 'warning', path: (row) => `/api/mfg/production-plan/${row.planId}/withdraw`, visible: (row) => row.status === 'RELEASED', fields: [field('remark', { label: '撤回原因（选填）', type: 'textarea' })], previewPatch: () => ({ status: 'DRAFT' }) }
    ]
  }),
  defineResource({
    key: 'mfg-work-order', domain: 'mfg', route: 'mfg/work-orders', title: '生产工单', icon: 'package', baseUrl: '/api/mfg/work-order', idField: 'workOrderId',
    contextCreateOnly: true,
    fields: ['workOrderNo', 'planId', 'productId', 'bomId', 'routeId', 'plannedQuantity', 'completedQuantity', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status', 'planId', 'productId'], defaults: { status: 'DRAFT', completedQuantity: 0 },
    overrides: { workOrderNo: { create: false, edit: false }, planId: { reference: 'mfg-production-plan', create: false, edit: false }, productId: { create: false, edit: false }, bomId: { create: false, edit: false }, routeId: { create: false, edit: false }, plannedQuantity: { type: 'number', min: 0, create: false, edit: false }, completedQuantity: { type: 'number', min: 0, create: false, edit: false }, status: { type: 'select', options: optionSets.workOrder, create: false, edit: false }, createdTime: { form: false, width: 170 } },
    canEdit: (row) => row.status === 'DRAFT', canDelete: (row) => row.status === 'DRAFT', lockedReason: '只有草稿生产工单可以编辑或删除',
    masterDetails: [
      {
        key: 'tasks', title: '生产任务', resource: 'mfg-task', parentField: 'workOrderId',
        endpoint: () => '/api/mfg/task/page',
        params: (row) => ({ current: 1, size: 100, workOrderId: row.workOrderId, sort: 'oldest' }),
        label: (row) => row.workOrderNo || `生产工单 #${row.workOrderId}`
      },
      {
        key: 'dispatches', title: '派工记录', resource: 'mfg-dispatch-order', parentField: 'workOrderId',
        endpoint: () => '/api/mfg/dispatch-order/page',
        params: (row) => ({ current: 1, size: 100, workOrderId: row.workOrderId }),
        label: (row) => row.workOrderNo || `生产工单 #${row.workOrderId}`
      },
      {
        key: 'materials', title: '用料需求', resource: 'mfg-work-order-material', parentField: 'workOrderId',
        endpoint: (row) => `/api/mfg/work-order-material/work-order/${row.workOrderId}`,
        label: (row) => row.workOrderNo || `生产工单 #${row.workOrderId}`
      }
    ],
    actions: [
      { key: 'release', label: '下达工单', icon: 'check', method: 'put', type: 'success', path: (row) => `/api/mfg/work-order/${row.workOrderId}/release`, visible: (row) => ['DRAFT', 'APPROVED'].includes(row.status), fields: [field('remark', { type: 'textarea' })], previewPatch: () => ({ status: 'RELEASED' }) },
      { key: 'generate-dispatch', label: '生成派工单', icon: 'users', method: 'post', type: 'primary', detailKey: 'dispatches', detailCount: 'zero', path: (row) => `/api/mfg/dispatch-order/generate/${row.workOrderId}`, visible: (row) => row.status === 'RELEASED', fields: [field('teamId', { required: true, type: 'reference', reference: 'org-team' }), field('dueTime', { required: true, type: 'datetime' }), field('remark', { type: 'textarea' })] },
      { key: 'view-dispatches', label: '派工详情', icon: 'arrow', kind: 'navigate', detailKey: 'dispatches', detailCount: 'positive', targetResource: 'mfg-dispatch-order', query: (row) => ({ workOrderId: row.workOrderId, from: row.workOrderNo }) },
      { key: 'view-tasks', label: '生产任务详情', icon: 'arrow', kind: 'navigate', detailKey: 'tasks', detailCount: 'positive', targetResource: 'mfg-task', query: (row) => ({ workOrderId: row.workOrderId, from: row.workOrderNo }) },
      { key: 'withdraw', label: '撤回修改', icon: 'refresh', method: 'put', type: 'warning', path: (row) => `/api/mfg/work-order/${row.workOrderId}/withdraw`, visible: (row) => row.status === 'RELEASED', fields: [field('remark', { label: '撤回原因（选填）', type: 'textarea' })], previewPatch: () => ({ status: 'DRAFT' }) }
    ],
    relations: [{ key: 'materials', title: '工单用料需求', resource: 'mfg-work-order-material', parentField: 'workOrderId', endpoint: (row) => `/api/mfg/work-order-material/work-order/${row.workOrderId}` }]
  }),
  defineResource({
    key: 'mfg-work-order-material', domain: 'mfg', route: 'mfg/work-order-materials', title: '工单用料需求', baseUrl: '/api/mfg/work-order-material', idField: 'requirementId', menu: false,
    contextCreateOnly: true,
    permissions: { create: false, update: false, delete: false },
    fields: ['workOrderId', 'materialId', 'requiredQuantity', 'issuedQuantity', 'shortageQuantity', 'status', 'remark'], filters: ['workOrderId', 'materialId', 'status'], defaults: { status: 'WAIT_READY', issuedQuantity: 0, shortageQuantity: 0 },
    overrides: { workOrderId: { form: false }, materialId: { form: false }, requiredQuantity: { type: 'number', min: 0, form: false }, issuedQuantity: { type: 'number', min: 0, form: false }, shortageQuantity: { type: 'number', min: 0, form: false }, status: { type: 'select', options: optionSets.readiness, form: false }, remark: { form: false } }
  }),
  defineResource({
    key: 'mfg-dispatch-order', domain: 'mfg', route: 'mfg/dispatch-orders', title: '派工管理', icon: 'users', baseUrl: '/api/mfg/dispatch-order', idField: 'dispatchId',
    contextCreateOnly: true,
    fields: ['dispatchNo', 'workOrderId', 'teamId', 'plannedQuantity', 'dueTime', 'materialReadyStatus', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status', 'workOrderId', 'teamId'], defaults: { materialReadyStatus: 'WAIT_READY', status: 'WAIT_DISPATCH' },
    overrides: { dispatchNo: { create: false, edit: false }, workOrderId: { create: false, edit: false }, teamId: { required: true }, plannedQuantity: { type: 'number', min: 0, create: false, edit: false }, dueTime: { required: true }, materialReadyStatus: { type: 'select', options: optionSets.readiness, create: false, edit: false }, status: { type: 'select', options: optionSets.dispatch, create: false, edit: false }, createdTime: { form: false, width: 170 } },
    masterDetail: {
      title: '派工关联生产任务', resource: 'mfg-task', parentField: 'dispatchId',
      endpoint: () => '/api/mfg/task/page',
      params: (row) => ({ current: 1, size: 100, dispatchId: row.dispatchId, sort: 'oldest' }),
      label: (row) => row.dispatchNo || `派工单 #${row.dispatchId}`
    },
    canEdit: (row) => row.status === 'WAIT_DISPATCH', canDelete: (row) => row.status === 'WAIT_DISPATCH', lockedReason: '只有待派发派工单可以编辑或删除',
    actions: [
      { key: 'release', label: '派发并生成任务', icon: 'check', method: 'put', type: 'success', path: (row) => `/api/mfg/dispatch-order/${row.dispatchId}/release`, visible: (row) => row.status === 'WAIT_DISPATCH', fields: [field('remark', { type: 'textarea' })], previewPatch: () => ({ status: 'RELEASED' }) },
      { key: 'view-tasks', label: '生产任务详情', icon: 'arrow', kind: 'navigate', detailKey: 'mfg-task', detailCount: 'positive', targetResource: 'mfg-task', query: (row) => ({ dispatchId: row.dispatchId, from: row.dispatchNo }) },
      { key: 'withdraw', label: '撤回派工', icon: 'refresh', method: 'put', type: 'warning', path: (row) => `/api/mfg/dispatch-order/${row.dispatchId}/withdraw`, visible: (row) => ['RELEASED', 'ACCEPTED'].includes(row.status), fields: [field('remark', { label: '撤回原因（选填）', type: 'textarea' })], previewPatch: () => ({ status: 'WAIT_DISPATCH' }) }
    ]
  }),
  defineResource({
    key: 'mfg-task', domain: 'mfg', route: 'mfg/tasks', title: '生产任务', icon: 'activity', baseUrl: '/api/mfg/task', idField: 'taskId',
    contextCreateOnly: true,
    fields: ['taskNo', 'dispatchId', 'workOrderId', 'operationId', 'stationId', 'plannedQuantity', 'completedQuantity', 'defectQuantity', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status', 'dispatchId', 'workOrderId', 'operationId', 'stationId'], defaults: { status: 'WAIT_START', completedQuantity: 0, defectQuantity: 0 },
    overrides: { taskNo: { create: false, edit: false }, dispatchId: { create: false, edit: false }, workOrderId: { create: false, edit: false }, operationId: { create: false, edit: false }, stationId: { required: true }, plannedQuantity: { type: 'number', min: 0, create: false, edit: false }, completedQuantity: { type: 'number', min: 0, create: false, edit: false }, defectQuantity: { type: 'number', min: 0, create: false, edit: false }, status: { type: 'select', options: optionSets.task, create: false, edit: false }, createdTime: { form: false, width: 170 } },
    canEdit: (row) => row.status === 'WAIT_START', canDelete: (row) => row.status === 'WAIT_START', lockedReason: '已开工或已结束的生产任务不可普通修改或删除',
    masterDetails: [
      {
        key: 'reports', title: '报工记录', resource: 'mfg-work-report', parentField: 'taskId',
        endpoint: () => '/api/mfg/work-report/page',
        params: (row) => ({ current: 1, size: 100, taskId: row.taskId }),
        label: (row) => row.taskNo || `生产任务 #${row.taskId}`
      },
      {
        key: 'operators', title: '任务人员', resource: 'mfg-task-operator', parentField: 'taskId',
        endpoint: (row) => `/api/mfg/task-operator/task/${row.taskId}`,
        label: (row) => row.taskNo || `生产任务 #${row.taskId}`
      }
    ],
    actions: [
      { key: 'start', label: '开始任务', icon: 'play', method: 'put', type: 'primary', path: (row) => `/api/mfg/task/${row.taskId}/start`, visible: (row) => row.status === 'WAIT_START', fields: [field('stationId', { label: '实际工位', type: 'reference', reference: 'factory-station' })], previewPatch: (values) => ({ status: 'RUNNING', ...(values.stationId ? { stationId: values.stationId } : {}) }) },
      { key: 'report', label: '去报工', icon: 'arrow', kind: 'navigate', targetResource: 'mfg-work-report', permissionAction: 'submit', visible: (row) => ['RUNNING', 'PAUSED'].includes(row.status), query: (row) => ({ taskId: row.taskId, create: '1', from: row.taskNo }) },
      { key: 'withdraw', label: '撤回开工', icon: 'refresh', method: 'put', type: 'warning', path: (row) => `/api/mfg/task/${row.taskId}/withdraw`, visible: (row) => ['RUNNING', 'PAUSED'].includes(row.status), fields: [field('remark', { label: '撤回原因（选填）', type: 'textarea' })], previewPatch: () => ({ status: 'WAIT_START' }) },
      { key: 'finish', label: '确认完工', icon: 'check', method: 'put', type: 'success', path: (row) => `/api/mfg/task/${row.taskId}/finish`, visible: (row) => ['RUNNING', 'PAUSED'].includes(row.status), fields: [field('remark', { type: 'textarea' })], previewPatch: (values) => ({ ...(values.remark ? { remark: values.remark } : {}), status: 'FINISHED' }) }
    ],
    relations: [{ key: 'operators', title: '任务人员', resource: 'mfg-task-operator', parentField: 'taskId', endpoint: (row) => `/api/mfg/task-operator/task/${row.taskId}` }]
  }),
  defineResource({ key: 'mfg-task-operator', domain: 'mfg', route: 'mfg/task-operators', title: '任务人员', baseUrl: '/api/mfg/task-operator', idField: 'id', menu: false, fields: ['taskId', 'employeeId', 'roleInTask', 'assignedTime'], filters: ['taskId', 'employeeId', 'roleInTask'], overrides: { taskId: { required: true }, employeeId: { required: true }, roleInTask: { required: true }, assignedTime: { required: true } } }),
  defineResource({
    key: 'mfg-work-report', domain: 'mfg', route: 'mfg/work-reports', title: '生产报工', icon: 'clipboard', baseUrl: '/api/mfg/work-report', idField: 'reportId',
    contextCreateOnly: true,
    fields: ['reportNo', 'taskId', 'operatorId', 'stationId', 'qualifiedQuantity', 'defectQuantity', 'actualWorkHours', 'reportTime', 'auditStatus', 'remark', 'createdTime'], filters: ['keyword', 'taskId', 'operatorId', 'stationId', 'auditStatus'], defaults: { auditStatus: 'SUBMITTED', qualifiedQuantity: 0, defectQuantity: 0 },
    overrides: { reportNo: { form: false }, taskId: { required: true, form: false }, operatorId: { form: false }, stationId: { form: false }, qualifiedQuantity: { required: true, type: 'number', min: 0 }, defectQuantity: { required: true, type: 'number', min: 0 }, actualWorkHours: { type: 'number', min: 0 }, reportTime: { form: false }, auditStatus: { type: 'select', options: optionSets.audit, create: false, edit: false }, createdTime: { form: false, width: 170 } },
    masterDetail: {
      title: '报工用料', resource: 'mfg-report-material', parentField: 'reportId',
      endpoint: (row) => `/api/mfg/report-material/report/${row.reportId}`,
      label: (row) => row.reportNo || `报工单 #${row.reportId}`
    },
    fullChild: { property: 'materials', title: '报工用料', resource: 'mfg-report-material', parentField: 'reportId', endpoint: (row) => `/api/mfg/report-material/report/${row.reportId}` },
    createPath: '/full', updatePath: '/full',
    canEdit: (row) => ['SUBMITTED', 'REJECTED'].includes(row.auditStatus), canDelete: (row) => ['SUBMITTED', 'REJECTED'].includes(row.auditStatus), lockedReason: '已审核或已撤回的报工单不可修改或删除',
    actions: [
      { key: 'withdraw', label: '撤回修改', icon: 'refresh', method: 'put', type: 'warning', path: (row) => `/api/mfg/work-report/${row.reportId}/withdraw`, visible: (row) => row.auditStatus === 'SUBMITTED', fields: [field('remark', { label: '撤回原因（选填）', type: 'textarea' })], previewPatch: () => ({ auditStatus: 'WITHDRAWN' }) },
      { key: 'audit', label: '审核报工', icon: 'shield-check', method: 'put', type: 'success', path: (row) => `/api/mfg/work-report/${row.reportId}/audit`, visible: (row) => row.auditStatus === 'SUBMITTED', fields: [field('auditResult', { label: '审核结果', type: 'select', options: optionSets.audit.filter((item) => !['SUBMITTED', 'WITHDRAWN'].includes(item.value)), required: true }), field('auditComment', { label: '审核意见', type: 'textarea' })], previewPatch: (values) => ({ auditStatus: values.auditResult }) }
    ]
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
