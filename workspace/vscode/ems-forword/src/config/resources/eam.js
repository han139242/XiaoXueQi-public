import { defineResource, field, optionSets, values } from './helpers'

export default [
  defineResource({
    key: 'eam-equipment-category', domain: 'eam', route: 'eam/equipment-categories', title: '设备分类', icon: 'database', baseUrl: '/api/eam/equipment-category', idField: 'categoryId',
    fields: ['parentId', 'categoryCode', 'categoryName', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status'], defaults: { status: 'ENABLE' },
    overrides: { parentId: { type: 'reference', reference: 'eam-equipment-category' }, categoryCode: { required: true }, categoryName: { required: true }, status: { type: 'select', options: optionSets.enable }, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'eam-equipment', domain: 'eam', route: 'eam/equipment', title: '设备台账', icon: 'wrench', baseUrl: '/api/eam/equipment', idField: 'equipmentId',
    fields: ['equipmentCode', 'equipmentName', 'categoryId', 'model', 'manufacturer', 'purchaseDate', 'stationId', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status', 'categoryId'], defaults: { status: 'IDLE' },
    overrides: { equipmentCode: { required: true, width: 190 }, equipmentName: { required: true, width: 160 }, categoryId: { type: 'reference', reference: 'eam-equipment-category', required: true, width: 160 }, model: { width: 140 }, manufacturer: { width: 140 }, stationId: { required: true, width: 160 }, status: { type: 'select', options: optionSets.equipment, width: 112 }, remark: { cellLines: 2 }, createdTime: { form: false, width: 170 } },
    alerts: [{ key: 'fault', label: '台设备处于故障状态', shortLabel: '故障设备', actionLabel: '只看故障设备', filters: { status: 'FAULT' } }],
    rowTone: (row) => row.status === 'FAULT' ? 'danger' : row.status === 'REPAIRING' ? 'warning' : ''
  }),
  defineResource({
    key: 'eam-equipment-status-log', domain: 'eam', route: 'eam/equipment-status', title: '设备状态日志', icon: 'activity', baseUrl: '/api/eam/equipment-status-log', idField: 'statusLogId',
    fields: ['equipmentId', 'runningStatus', 'downtimeType', 'alertLevel', 'stationId', 'collectedTime', 'description', 'remark'], filters: ['equipmentId', 'runningStatus', 'downtimeType', 'alertLevel', 'stationId'],
    permissions: { create: false, edit: false, delete: false, export: true },
    overrides: { equipmentId: { width: 180 }, runningStatus: { type: 'select', options: optionSets.equipment, width: 112 }, downtimeType: { type: 'select', width: 112, options: values([['PLANNED', '计划停机', 'info'], ['UNPLANNED', '非计划停机', 'warning'], ['FAULT', '故障停机', 'danger'], ['CHANGEOVER', '换型停机', 'primary']]) }, alertLevel: { type: 'select', width: 100, options: values([['INFO', '提示', 'info'], ['WARNING', '警告', 'warning'], ['CRITICAL', '严重', 'danger']]) }, description: { width: 260, cellLines: 2 }, collectedTime: { form: false, width: 170 } },
    rowTone: (row) => row.alertLevel === 'CRITICAL' || row.runningStatus === 'FAULT' || row.downtimeType === 'FAULT'
      ? 'danger'
      : row.alertLevel === 'WARNING' || row.downtimeType === 'UNPLANNED' ? 'warning' : ''
  }),
  defineResource({
    key: 'eam-maintenance-plan', domain: 'eam', route: 'eam/maintenance-plans', title: '保养计划', icon: 'calendar', baseUrl: '/api/eam/maintenance-plan', idField: 'planId',
    fields: ['equipmentId', 'maintenanceType', 'cycleType', 'cycleValue', 'maintenanceContent', 'responsibleId', 'status', 'remark', 'createdTime'], filters: ['equipmentId', 'maintenanceType', 'cycleType', 'responsibleId', 'status'], defaults: { status: 'ENABLE' },
    overrides: { equipmentId: { required: true }, maintenanceType: { required: true, type: 'select', options: values([['DAILY', '日常保养', 'info'], ['LEVEL_ONE', '一级保养', 'primary'], ['LEVEL_TWO', '二级保养', 'warning']]) }, cycleType: { required: true, type: 'select', options: values([['DAY', '按天', 'primary'], ['HOUR', '按小时', 'warning'], ['COUNT', '按次数', 'success']]) }, cycleValue: { required: true, type: 'number', min: 0 }, maintenanceContent: { required: true, type: 'textarea' }, responsibleId: { required: true }, status: { type: 'select', options: optionSets.enable }, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'eam-maintenance-record', domain: 'eam', route: 'eam/maintenance-records', title: '保养记录', icon: 'clipboard', baseUrl: '/api/eam/maintenance-record', idField: 'recordId',
    fields: ['planId', 'equipmentId', 'maintainerId', 'maintenanceTime', 'maintenanceResult', 'nextMaintenanceTime', 'remark', 'createdTime'], filters: ['planId', 'equipmentId', 'maintainerId'],
    overrides: { planId: { type: 'reference', reference: 'eam-maintenance-plan', required: true }, equipmentId: { required: true }, maintainerId: { required: true }, maintenanceTime: { required: true }, maintenanceResult: { required: true, type: 'textarea' }, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'eam-spot-check-plan', domain: 'eam', route: 'eam/spot-check-plans', title: '点检计划', icon: 'check', baseUrl: '/api/eam/spot-check-plan', idField: 'planId',
    fields: ['equipmentId', 'checkCycle', 'checkItems', 'responsibleRole', 'status', 'remark', 'createdTime'], filters: ['equipmentId', 'checkCycle', 'responsibleRole', 'status'], defaults: { status: 'ENABLE' },
    overrides: { equipmentId: { required: true }, checkCycle: { required: true }, checkItems: { required: true, type: 'textarea' }, responsibleRole: { required: true }, status: { type: 'select', options: optionSets.enable }, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'eam-spot-check-record', domain: 'eam', route: 'eam/spot-check-records', title: '点检记录', icon: 'clipboard', baseUrl: '/api/eam/spot-check-record', idField: 'recordId',
    fields: ['planId', 'equipmentId', 'checkerId', 'checkTime', 'checkResult', 'abnormalDescription', 'remark', 'createdTime'], filters: ['planId', 'equipmentId', 'checkerId', 'checkResult'],
    overrides: { planId: { type: 'reference', reference: 'eam-spot-check-plan', required: true }, equipmentId: { required: true }, checkerId: { required: true }, checkTime: { required: true }, checkResult: { required: true, type: 'select', options: values([['NORMAL', '正常', 'success'], ['ABNORMAL', '异常', 'danger']]) }, abnormalDescription: { type: 'textarea' }, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'eam-repair-order', domain: 'eam', route: 'eam/repair-orders', title: '维修工单', icon: 'wrench', baseUrl: '/api/eam/repair-order', idField: 'repairId',
    fields: ['repairNo', 'equipmentId', 'exceptionId', 'reportedBy', 'reportedTime', 'faultDescription', 'repairerId', 'repairResult', 'repairCost', 'status', 'remark', 'createdTime'], filters: ['keyword', 'equipmentId', 'exceptionId', 'repairerId', 'status'], defaults: { status: 'SUBMITTED' },
    overrides: { repairNo: { required: true, width: 200 }, equipmentId: { required: true, width: 170 }, reportedBy: { required: true, width: 150 }, reportedTime: { required: true, width: 168 }, faultDescription: { required: true, type: 'textarea', width: 280, cellLines: 2 }, repairerId: { create: false, edit: false, width: 150 }, repairResult: { type: 'textarea', create: false, edit: false, width: 240, cellLines: 2 }, repairCost: { type: 'number', min: 0, create: false, edit: false }, status: { type: 'select', options: optionSets.repair, create: false, edit: false, width: 112 }, createdTime: { form: false, width: 170 } },
    alerts: [{ key: 'pending-assignment', label: '张维修单等待派工', shortLabel: '待派工维修单', actionLabel: '只看待派工', filters: { status: 'SUBMITTED' } }],
    rowTone: (row) => row.status === 'SUBMITTED' ? 'danger' : ['REPAIRING', 'WAIT_ACCEPTANCE'].includes(row.status) ? 'warning' : '',
    canEdit: (row) => row.status === 'SUBMITTED', canDelete: (row) => row.status === 'SUBMITTED', lockedReason: '已派工或已关闭的维修单不可普通修改或删除',
    actions: [
      { key: 'assign', label: '维修派工', icon: 'users', method: 'put', type: 'primary', path: (row) => `/api/eam/repair-order/${row.repairId}/assign`, visible: (row) => row.status === 'SUBMITTED', fields: [field('repairerId', { type: 'reference', reference: 'org-employee', required: true }), field('remark', { type: 'textarea' })], previewPatch: (values) => ({ repairerId: values.repairerId, status: 'REPAIRING' }) },
      { key: 'close', label: '关闭维修单', icon: 'check', method: 'put', type: 'success', path: (row) => `/api/eam/repair-order/${row.repairId}/close`, visible: (row) => ['REPAIRING', 'WAIT_ACCEPTANCE'].includes(row.status), fields: [field('repairResult', { type: 'textarea', required: true }), field('repairCost', { type: 'number', min: 0 })], previewPatch: (values) => ({ repairResult: values.repairResult, ...(values.repairCost !== '' && values.repairCost !== null && values.repairCost !== undefined ? { repairCost: values.repairCost } : {}), status: 'CLOSED' }) }
    ]
  })
]
