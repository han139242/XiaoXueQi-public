import { defineResource, field, optionSets, values } from './helpers'

const warehouseTypes = values([['RAW', '原料仓', 'primary'], ['SEMI_FINISHED', '半成品仓', 'warning'], ['FINISHED', '成品仓', 'success'], ['DEFECTIVE', '不良品仓', 'danger']])
const locationTypes = values([['NORMAL', '正常库位', 'success'], ['INSPECTION', '待检区', 'warning'], ['DEFECTIVE', '不良区', 'danger'], ['FROZEN', '冻结区', 'info']])

const confirmAction = (kind, idField, label = '确认') => ({
  key: `confirm-${kind}`, label, icon: 'check', method: 'put', type: 'success',
  path: (row) => `/api/wms/${kind}-order/${row[idField]}/confirm`,
  visible: (row) => row.auditStatus === 'SUBMITTED',
  fields: kind === 'inbound'
    ? [field('remark', { type: 'textarea' })]
    : [field('operatorId', { label: '操作人', type: 'reference', reference: 'org-employee' }), field('remark', { type: 'textarea' })],
  previewPatch: () => ({ auditStatus: 'APPROVED' })
})

export default [
  defineResource({ key: 'wms-warehouse', domain: 'wms', route: 'wms/warehouses', title: '仓库管理', icon: 'warehouse', baseUrl: '/api/wms/warehouse', idField: 'warehouseId', fields: ['warehouseCode', 'warehouseName', 'warehouseType', 'managerId', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status'], defaults: { status: 'ENABLE' }, overrides: { warehouseCode: { required: true }, warehouseName: { required: true }, warehouseType: { required: true, type: 'select', options: warehouseTypes }, status: { type: 'select', options: optionSets.warehouse }, createdTime: { form: false, width: 170 } } }),
  defineResource({ key: 'wms-location', domain: 'wms', route: 'wms/locations', title: '库位管理', icon: 'warehouse', baseUrl: '/api/wms/location', idField: 'locationId', fields: ['locationCode', 'locationName', 'warehouseId', 'locationType', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status'], defaults: { status: 'IDLE' }, overrides: { locationCode: { required: true }, locationName: { required: true }, warehouseId: { required: true }, locationType: { required: true, type: 'select', options: locationTypes }, status: { type: 'select', options: optionSets.location }, createdTime: { form: false, width: 170 } } }),
  defineResource({
    key: 'wms-material-batch', domain: 'wms', route: 'wms/material-batches', title: '物料批次', icon: 'package', baseUrl: '/api/wms/material-batch', idField: 'batchId',
    fields: ['batchNo', 'materialId', 'supplierId', 'supplierBatchNo', 'productionDate', 'receivedDate', 'qualityStatus', 'status', 'sourceOrderType', 'sourceOrderId', 'sourceProductLotId', 'remark', 'createdTime'], filters: ['keyword', 'materialId', 'supplierId', 'qualityStatus', 'status'], defaults: { qualityStatus: 'WAIT_INSPECTION', status: 'FROZEN' },
    overrides: { batchNo: { required: true }, materialId: { required: true }, productionDate: { required: true }, receivedDate: { required: true }, qualityStatus: { type: 'select', options: optionSets.batchQuality, create: false, edit: false }, status: { type: 'select', options: optionSets.batchStatus, create: false, edit: false }, sourceOrderId: { type: 'number', min: 1 }, sourceProductLotId: { type: 'reference', reference: 'trace-product-lot' }, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'wms-inventory', domain: 'wms', route: 'wms/inventory', title: '库存余额', icon: 'database', baseUrl: '/api/wms/inventory', idField: 'inventoryId',
    description: '实时查看仓库、库位、物料和批次库存余额',
    fields: ['warehouseId', 'locationId', 'materialId', 'batchId', 'availableQuantity', 'lockedQuantity', 'inspectionQuantity', 'remark', 'updatedTime'], filters: ['warehouseId', 'locationId', 'materialId', 'batchId'],
    permissions: { create: false, edit: false, delete: false, export: true }, overrides: { updatedTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'wms-stock-movement', domain: 'wms', route: 'wms/movements', title: '库存流水', icon: 'activity', baseUrl: '/api/wms/stock-movement', idField: 'movementId',
    fields: ['movementNo', 'movementType', 'sourceOrderType', 'sourceOrderId', 'materialId', 'batchId', 'quantity', 'direction', 'operationTime', 'remark'], filters: ['keyword', 'movementType', 'sourceOrderType', 'sourceOrderId', 'materialId', 'batchId', 'direction'],
    permissions: { create: false, edit: false, delete: false, export: true },
    overrides: { sourceOrderId: { type: 'number', min: 1 }, direction: { type: 'select', options: values([['IN', '入库', 'success'], ['OUT', '出库', 'warning']]) }, operationTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'wms-inbound-order', domain: 'wms', route: 'wms/inbound-orders', title: '入库管理', icon: 'warehouse', baseUrl: '/api/wms/inbound-order', idField: 'inboundId', createPath: '/full', updatePath: '/full',
    fields: ['inboundNo', 'inboundType', 'warehouseId', 'sourceOrderType', 'sourceOrderId', 'auditStatus', 'inboundTime', 'remark', 'createdTime'], filters: ['keyword', 'inboundType', 'warehouseId', 'sourceOrderType', 'sourceOrderId', 'auditStatus'], defaults: { auditStatus: 'SUBMITTED' },
    overrides: { inboundNo: { required: true }, inboundType: { required: true, type: 'select', options: values([['PURCHASE', '采购入库', 'primary'], ['PRODUCTION', '生产入库', 'success'], ['RETURN', '退料入库', 'warning']]) }, warehouseId: { required: true }, sourceOrderId: { type: 'number', min: 1 }, auditStatus: { type: 'select', options: optionSets.audit, create: false, edit: false }, inboundTime: { required: true }, createdTime: { form: false, width: 170 } },
    canEdit: (row) => row.auditStatus !== 'APPROVED', canDelete: (row) => row.auditStatus !== 'APPROVED', lockedReason: '已确认入库的单据不可修改或删除',
    fullChild: { property: 'details', title: '入库明细', resource: 'wms-inbound-detail', parentField: 'inboundId', endpoint: (row) => `/api/wms/inbound-detail/inbound/${row.inboundId}` }, actions: [confirmAction('inbound', 'inboundId', '确认入库')]
  }),
  defineResource({ key: 'wms-inbound-detail', domain: 'wms', route: 'wms/inbound-details', title: '入库明细', baseUrl: '/api/wms/inbound-detail', idField: 'detailId', menu: false, fields: ['inboundId', 'materialId', 'batchId', 'locationId', 'inboundQuantity', 'remark'], filters: ['inboundId', 'materialId', 'batchId', 'locationId'], overrides: { inboundId: { required: true }, materialId: { required: true }, batchId: { required: true }, locationId: { required: true }, inboundQuantity: { required: true, type: 'number', min: 0 } } }),
  defineResource({
    key: 'wms-outbound-order', domain: 'wms', route: 'wms/outbound-orders', title: '出库管理', icon: 'logout', baseUrl: '/api/wms/outbound-order', idField: 'outboundId', createPath: '/full', updatePath: '/full',
    fields: ['outboundNo', 'outboundType', 'warehouseId', 'workOrderId', 'applicantId', 'auditStatus', 'outboundTime', 'remark', 'createdTime'], filters: ['keyword', 'outboundType', 'warehouseId', 'workOrderId', 'applicantId', 'auditStatus'], defaults: { auditStatus: 'SUBMITTED' },
    overrides: { outboundNo: { required: true }, outboundType: { required: true, type: 'select', options: values([['PRODUCTION', '生产领料', 'primary'], ['SALES', '销售出库', 'success'], ['TRANSFER', '调拨出库', 'warning']]) }, warehouseId: { required: true }, applicantId: { required: true }, auditStatus: { type: 'select', options: optionSets.audit, create: false, edit: false }, outboundTime: { required: true }, createdTime: { form: false, width: 170 } },
    canEdit: (row) => row.auditStatus !== 'APPROVED', canDelete: (row) => row.auditStatus !== 'APPROVED', lockedReason: '已确认出库的单据不可修改或删除',
    fullChild: { property: 'details', title: '出库明细', resource: 'wms-outbound-detail', parentField: 'outboundId', endpoint: (row) => `/api/wms/outbound-detail/outbound/${row.outboundId}` }, actions: [confirmAction('outbound', 'outboundId', '确认出库')]
  }),
  defineResource({ key: 'wms-outbound-detail', domain: 'wms', route: 'wms/outbound-details', title: '出库明细', baseUrl: '/api/wms/outbound-detail', idField: 'detailId', menu: false, fields: ['outboundId', 'materialId', 'batchId', 'locationId', 'outboundQuantity', 'remark'], filters: ['outboundId', 'materialId', 'batchId', 'locationId'], overrides: { outboundId: { required: true }, materialId: { required: true }, batchId: { required: true }, locationId: { required: true }, outboundQuantity: { required: true, type: 'number', min: 0 } } }),
  defineResource({
    key: 'wms-return-order', domain: 'wms', route: 'wms/return-orders', title: '退料管理', icon: 'refresh', baseUrl: '/api/wms/return-order', idField: 'returnId', createPath: '/full', updatePath: '/full',
    fields: ['returnNo', 'returnType', 'workOrderId', 'applicantId', 'auditStatus', 'returnTime', 'remark', 'createdTime'], filters: ['keyword', 'returnType', 'workOrderId', 'applicantId', 'auditStatus'], defaults: { auditStatus: 'SUBMITTED' },
    overrides: { returnNo: { required: true }, returnType: { required: true, type: 'select', options: values([['GOOD', '良品退料', 'success'], ['DEFECTIVE', '不良退料', 'danger'], ['FINISHED', '成品退库', 'warning']]) }, workOrderId: { required: true }, applicantId: { required: true }, auditStatus: { type: 'select', options: optionSets.audit, create: false, edit: false }, returnTime: { required: true }, createdTime: { form: false, width: 170 } },
    canEdit: (row) => row.auditStatus !== 'APPROVED', canDelete: (row) => row.auditStatus !== 'APPROVED', lockedReason: '已确认退料的单据不可修改或删除',
    fullChild: { property: 'details', title: '退料明细', resource: 'wms-return-detail', parentField: 'returnId', endpoint: (row) => `/api/wms/return-detail/return/${row.returnId}` }, actions: [confirmAction('return', 'returnId', '确认退料')]
  }),
  defineResource({ key: 'wms-return-detail', domain: 'wms', route: 'wms/return-details', title: '退料明细', baseUrl: '/api/wms/return-detail', idField: 'detailId', menu: false, fields: ['returnId', 'materialId', 'batchId', 'returnQuantity', 'returnReason', 'remark'], filters: ['returnId', 'materialId', 'batchId'], overrides: { returnId: { required: true }, materialId: { required: true }, batchId: { required: true }, returnQuantity: { required: true, type: 'number', min: 0 }, returnReason: { required: true, type: 'textarea' } } }),
  defineResource({
    key: 'wms-stocktake-order', domain: 'wms', route: 'wms/stocktakes', title: '盘点管理', icon: 'check', baseUrl: '/api/wms/stocktake-order', idField: 'stocktakeId', createPath: '/full', updatePath: '/full',
    fields: ['stocktakeNo', 'warehouseId', 'stocktakeType', 'stocktakeStatus', 'startedTime', 'finishedTime', 'remark', 'createdTime'], filters: ['keyword', 'warehouseId', 'stocktakeType', 'stocktakeStatus'], defaults: { stocktakeStatus: 'WAIT_START' },
    overrides: { stocktakeNo: { required: true }, warehouseId: { required: true }, stocktakeType: { required: true, type: 'select', options: values([['FULL', '全盘', 'primary'], ['SAMPLE', '抽盘', 'warning'], ['CYCLE', '循环盘点', 'success']]) }, stocktakeStatus: { type: 'select', options: optionSets.stocktake, create: false, edit: false }, startedTime: { required: true }, finishedTime: { create: false, edit: false }, createdTime: { form: false, width: 170 } },
    canEdit: (row) => row.stocktakeStatus !== 'FINISHED', canDelete: (row) => row.stocktakeStatus !== 'FINISHED', lockedReason: '已完成盘点的单据不可修改或删除',
    fullChild: { property: 'details', title: '盘点明细', resource: 'wms-stocktake-detail', parentField: 'stocktakeId', endpoint: (row) => `/api/wms/stocktake-detail/stocktake/${row.stocktakeId}` },
    actions: [{ key: 'finish', label: '完成盘点', icon: 'check', method: 'put', type: 'success', path: (row) => `/api/wms/stocktake-order/${row.stocktakeId}/finish`, visible: (row) => row.stocktakeStatus !== 'FINISHED', fields: [field('operatorId', { label: '操作人', type: 'reference', reference: 'org-employee' }), field('remark', { type: 'textarea' })], previewPatch: () => ({ stocktakeStatus: 'FINISHED', finishedTime: new Date().toISOString().slice(0, 19) }) }]
  }),
  defineResource({ key: 'wms-stocktake-detail', domain: 'wms', route: 'wms/stocktake-details', title: '盘点明细', baseUrl: '/api/wms/stocktake-detail', idField: 'detailId', menu: false, fields: ['stocktakeId', 'materialId', 'batchId', 'locationId', 'bookQuantity', 'actualQuantity', 'differenceQuantity', 'remark'], filters: ['stocktakeId', 'materialId', 'batchId', 'locationId'], overrides: { stocktakeId: { required: true }, materialId: { required: true }, batchId: { required: true }, locationId: { required: true }, bookQuantity: { required: true, type: 'number', min: 0 }, actualQuantity: { required: true, type: 'number', min: 0 }, differenceQuantity: { type: 'number' } } }),
  defineResource({
    key: 'wms-replenishment', domain: 'wms', route: 'wms/replenishment', title: '补货建议', icon: 'alert', baseUrl: '/api/wms/replenishment-suggestion', idField: 'suggestionId',
    fields: ['suggestionNo', 'materialId', 'requiredQuantity', 'availableQuantity', 'suggestedQuantity', 'sourceType', 'status', 'remark', 'createdTime'], filters: ['keyword', 'materialId', 'sourceType', 'status'], defaults: { status: 'SUBMITTED' },
    overrides: { suggestionNo: { required: true }, materialId: { required: true }, requiredQuantity: { required: true, type: 'number', min: 0 }, availableQuantity: { type: 'number', min: 0 }, suggestedQuantity: { type: 'number', min: 0 }, sourceType: { type: 'select', options: values([['INVENTORY_WARNING', '库存预警', 'warning'], ['WORK_ORDER', '工单需求', 'primary'], ['MANUAL', '人工发起', 'info']]) }, status: { type: 'select', options: values([['SUBMITTED', '待处理', 'warning'], ['APPROVED', '已确认', 'success'], ['CLOSED', '已关闭', 'info']]) }, createdTime: { form: false, width: 170 } },
    globalActions: [{ key: 'generate', label: '生成补货建议', icon: 'plus', type: 'primary', method: 'post', url: '/api/wms/replenishment-suggestion/generate', fields: [field('materialId', { required: true, type: 'reference', reference: 'mdm-material' }), field('warehouseId', { required: true, type: 'reference', reference: 'wms-warehouse' }), field('requiredQuantity', { required: true, type: 'number', min: 0 })] }]
  })
]
