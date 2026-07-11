import { defineResource, field, optionSets, values } from './helpers'

const barcodeTypes = values([['MATERIAL', '物料码', 'primary'], ['BATCH', '批次码', 'warning'], ['PRODUCT', '产品码', 'success'], ['DOCUMENT', '单据码', 'info']])

export default [
  defineResource({
    key: 'trace-barcode', domain: 'trace', route: 'trace/barcodes', title: '条码管理', icon: 'scan', baseUrl: '/api/trace/barcode', idField: 'barcodeId',
    fields: ['barcodeValue', 'barcodeType', 'bizType', 'bizId', 'generatedRule', 'status', 'remark', 'createdTime'], filters: ['keyword', 'barcodeType', 'bizType', 'bizId', 'status'], defaults: { status: 'ENABLE' },
    overrides: { barcodeValue: { required: true }, barcodeType: { required: true, type: 'select', options: barcodeTypes }, bizType: { required: true }, bizId: { required: true, type: 'number' }, generatedRule: { required: true }, status: { type: 'select', options: optionSets.barcode }, createdTime: { form: false, width: 170 } },
    globalActions: [{ key: 'generate', label: '批量生成条码', icon: 'plus', method: 'post', type: 'primary', url: '/api/trace/barcode/generate', fields: [field('barcodeType', { type: 'select', options: barcodeTypes, required: true }), field('bizType', { required: true }), field('bizId', { type: 'number', required: true }), field('quantity', { label: '生成数量', type: 'number', required: true, min: 1, max: 200 }), field('remark', { type: 'textarea' })] }]
  }),
  defineResource({
    key: 'trace-product-lot', domain: 'trace', route: 'trace/product-lots', title: '产品批次追溯', icon: 'route', baseUrl: '/api/trace/product-lot', idField: 'productLotId',
    fields: ['lotNo', 'productId', 'workOrderId', 'batchQuantity', 'qualifiedQuantity', 'defectQuantity', 'status', 'remark', 'createdTime'], filters: ['keyword', 'productId', 'workOrderId', 'status'], defaults: { status: 'RUNNING', qualifiedQuantity: 0, defectQuantity: 0 },
    overrides: { lotNo: { required: true }, productId: { required: true }, workOrderId: { required: true }, batchQuantity: { required: true, type: 'number', min: 0 }, qualifiedQuantity: { type: 'number', min: 0 }, defectQuantity: { type: 'number', min: 0 }, status: { type: 'select', options: optionSets.productLot }, createdTime: { form: false, width: 170 } },
    actions: [{ key: 'trace', label: '查看全链路', icon: 'scan', type: 'primary', kind: 'trace', path: (row) => `/api/trace/product-lot/lot-no/${encodeURIComponent(row.lotNo)}` }],
    relations: [{ key: 'usages', title: '物料使用记录', resource: 'trace-material-usage', parentField: 'productLotId' }]
  }),
  defineResource({
    key: 'trace-material-usage', domain: 'trace', route: 'trace/material-usages', title: '物料使用追溯', icon: 'database', baseUrl: '/api/trace/material-usage', idField: 'usageId',
    fields: ['productLotId', 'barcodeId', 'workOrderId', 'reportId', 'materialId', 'batchId', 'usedQuantity', 'remark', 'createdTime'], filters: ['productLotId', 'barcodeId', 'workOrderId', 'reportId', 'materialId', 'batchId'],
    overrides: { productLotId: { required: true }, barcodeId: { required: true }, workOrderId: { required: true }, reportId: { required: true }, materialId: { required: true }, batchId: { required: true }, usedQuantity: { required: true, type: 'number', min: 0 }, createdTime: { form: false, width: 170 } },
    actions: [{ key: 'batch-trace', label: '反向追溯', icon: 'route', type: 'primary', kind: 'trace', path: (row) => `/api/trace/material-usage/batch/${row.batchId}` }]
  })
]
