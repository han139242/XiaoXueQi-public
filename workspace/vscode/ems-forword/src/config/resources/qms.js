import { defineResource, field, optionSets, values } from './helpers'

const inspectionTypes = values([['INCOMING', '来料检', 'primary'], ['PROCESS', '过程检', 'warning'], ['FINAL', '终检', 'success']])
const dispositionTypes = values([['REWORK', '返工', 'warning'], ['SCRAP', '报废', 'danger'], ['DOWNGRADE', '降级', 'info'], ['CONCESSION', '让步接收', 'primary']])

export default [
  defineResource({
    key: 'qms-inspection-item', domain: 'qms', route: 'qms/inspection-items', title: '检验项目', icon: 'check', baseUrl: '/api/qms/inspection-item', idField: 'itemId',
    fields: ['itemCode', 'itemName', 'itemType', 'inspectionMethod', 'unitId', 'remark', 'createdTime'], filters: ['keyword', 'itemType'],
    overrides: { itemCode: { required: true }, itemName: { required: true }, itemType: { required: true, type: 'select', options: values([['DIMENSION', '尺寸', 'primary'], ['PERFORMANCE', '性能', 'success'], ['APPEARANCE', '外观', 'warning'], ['WEIGHT', '重量', 'info']]) }, inspectionMethod: { required: true, type: 'textarea' }, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'qms-inspection-standard', domain: 'qms', route: 'qms/standards', title: '检验标准', icon: 'shield-check', baseUrl: '/api/qms/inspection-standard', idField: 'standardId',
    fields: ['itemId', 'productId', 'operationId', 'standardValue', 'upperLimit', 'lowerLimit', 'judgementRule', 'remark', 'createdTime'], filters: ['itemId', 'productId', 'operationId'],
    overrides: { itemId: { required: true }, standardValue: { required: true }, judgementRule: { required: true, type: 'textarea' }, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'qms-inspection-scheme', domain: 'qms', route: 'qms/schemes', title: '检验方案', icon: 'clipboard', baseUrl: '/api/qms/inspection-scheme', idField: 'schemeId',
    fields: ['schemeCode', 'schemeName', 'productId', 'operationId', 'inspectionType', 'status', 'remark', 'createdTime'], filters: ['schemeCode', 'schemeName', 'productId', 'operationId', 'inspectionType', 'status'], defaults: { status: 'DRAFT' },
    overrides: { schemeCode: { required: true }, schemeName: { required: true }, productId: { required: true }, inspectionType: { required: true, type: 'select', options: inspectionTypes }, status: { type: 'select', options: optionSets.release }, createdTime: { form: false, width: 170 } },
    relations: [{ key: 'items', title: '方案检验项目', resource: 'qms-inspection-scheme-item', parentField: 'schemeId', endpoint: (row) => `/api/qms/inspection-scheme/${row.schemeId}/items` }]
  }),
  defineResource({
    key: 'qms-inspection-scheme-item', domain: 'qms', route: 'qms/scheme-items', title: '方案检验项目', baseUrl: '/api/qms/inspection-scheme-item', idField: 'id', menu: false,
    fields: ['schemeId', 'itemId', 'standardId', 'sampleQuantity', 'sequenceNo', 'remark'], filters: ['schemeId', 'itemId', 'standardId'],
    overrides: { schemeId: { required: true }, itemId: { required: true }, standardId: { required: true }, sampleQuantity: { required: true, type: 'number', min: 0 }, sequenceNo: { required: true, type: 'number', min: 1 } }
  }),
  defineResource({
    key: 'qms-inspection-order', domain: 'qms', route: 'qms/inspection-orders', title: '检验单管理', icon: 'shield-check', baseUrl: '/api/qms/inspection-order', idField: 'inspectionId', createPath: '/full', updatePath: '/full',
    fields: ['inspectionNo', 'inspectionType', 'schemeId', 'workOrderId', 'taskId', 'reportId', 'batchId', 'inspectorId', 'inspectionResult', 'status', 'remark', 'createdTime'], filters: ['keyword', 'inspectionType', 'schemeId', 'workOrderId', 'taskId', 'reportId', 'batchId', 'inspectorId', 'status', 'inspectionResult'], defaults: { status: 'DRAFT' },
    overrides: { inspectionNo: { required: true }, inspectionType: { required: true, type: 'select', options: inspectionTypes }, schemeId: { required: true }, inspectorId: { required: true }, inspectionResult: { type: 'select', options: optionSets.inspectionResult, create: false, edit: false }, status: { type: 'select', options: optionSets.inspectionOrder, create: false, edit: false }, createdTime: { form: false, width: 170 } },
    canEdit: (row) => row.status !== 'FINISHED', canDelete: (row) => row.status !== 'FINISHED', lockedReason: '已完成检验的单据不可修改或删除',
    fullChild: { property: 'results', title: '检验结果明细', resource: 'qms-inspection-result', parentField: 'inspectionId', endpoint: (row) => `/api/qms/inspection-result/inspection/${row.inspectionId}` },
    actions: [{ key: 'submit', label: '提交检验', icon: 'check', method: 'put', type: 'success', path: (row) => `/api/qms/inspection-order/${row.inspectionId}/submit`, visible: (row) => row.status !== 'FINISHED', fields: [field('inspectionResult', { type: 'select', options: optionSets.inspectionResult, required: true }), field('remark', { type: 'textarea' }), field('operatorId', { label: '操作人', type: 'reference', reference: 'org-employee' })], previewPatch: (values) => ({ inspectionResult: values.inspectionResult, status: 'FINISHED' }) }]
  }),
  defineResource({
    key: 'qms-inspection-result', domain: 'qms', route: 'qms/inspection-results', title: '检验结果明细', baseUrl: '/api/qms/inspection-result', idField: 'resultId', menu: false,
    fields: ['inspectionId', 'itemId', 'measuredValue', 'result', 'defectDescription', 'photoFileId', 'remark'], filters: ['inspectionId', 'itemId', 'result', 'photoFileId'],
    overrides: { inspectionId: { required: true }, itemId: { required: true }, measuredValue: { required: true }, result: { type: 'select', options: optionSets.inspectionResult, required: true }, defectDescription: { type: 'textarea' } }
  }),
  defineResource({
    key: 'qms-nonconforming-order', domain: 'qms', route: 'qms/nonconforming-orders', title: '不合格品处理', icon: 'alert', baseUrl: '/api/qms/nonconforming-order', idField: 'ncrId', createPath: '/full', updatePath: '/full',
    fields: ['ncrNo', 'inspectionId', 'productLotId', 'defectReason', 'dispositionType', 'approvalStatus', 'status', 'remark', 'createdTime'], filters: ['keyword', 'inspectionId', 'productLotId', 'dispositionType', 'approvalStatus', 'status'], defaults: { approvalStatus: 'SUBMITTED', status: 'SUBMITTED' },
    overrides: { ncrNo: { required: true }, inspectionId: { required: true }, productLotId: { required: true }, defectReason: { required: true, type: 'textarea' }, dispositionType: { required: true, type: 'select', options: dispositionTypes }, approvalStatus: { type: 'select', options: optionSets.approval, create: false, edit: false }, status: { type: 'select', options: optionSets.ncrStatus, create: false, edit: false }, createdTime: { form: false, width: 170 } },
    canEdit: (row) => row.approvalStatus === 'SUBMITTED' && row.status !== 'CLOSED', canDelete: (row) => row.approvalStatus === 'SUBMITTED' && row.status !== 'CLOSED', lockedReason: '已审批或已关闭的处置单不可修改或删除',
    fullChild: { property: 'details', title: '不合格品明细', resource: 'qms-nonconforming-detail', parentField: 'ncrId', endpoint: (row) => `/api/qms/nonconforming-detail/ncr/${row.ncrId}` },
    actions: [{ key: 'approve', label: '审批处置单', icon: 'shield-check', method: 'put', type: 'success', path: (row) => `/api/qms/nonconforming-order/${row.ncrId}/approve`, visible: (row) => row.approvalStatus === 'SUBMITTED', fields: [field('approvalResult', { label: '审批结果', type: 'select', options: optionSets.approval.filter((item) => item.value !== 'SUBMITTED'), required: true }), field('approvalComment', { label: '审批意见', type: 'textarea' }), field('operatorId', { label: '审批人', type: 'reference', reference: 'org-employee' })], previewPatch: (values) => ({ approvalStatus: values.approvalResult, status: values.approvalResult === 'APPROVED' ? 'PROCESSING' : 'CLOSED' }) }]
  }),
  defineResource({
    key: 'qms-nonconforming-detail', domain: 'qms', route: 'qms/nonconforming-details', title: '不合格品明细', baseUrl: '/api/qms/nonconforming-detail', idField: 'detailId', menu: false,
    fields: ['ncrId', 'barcodeId', 'inspectionResultId', 'defectQuantity', 'finalDisposition', 'recheckResult', 'remark'], filters: ['ncrId', 'barcodeId', 'inspectionResultId', 'finalDisposition', 'recheckResult'],
    overrides: { ncrId: { required: true }, defectQuantity: { required: true, type: 'number', min: 0 }, finalDisposition: { type: 'select', options: dispositionTypes }, recheckResult: { type: 'select', options: optionSets.inspectionResult } }
  })
]
