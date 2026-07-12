import { defineResource, optionSets, values } from './helpers'

export default [
  defineResource({
    key: 'process-operation', domain: 'process', route: 'process/operations', title: '工序管理', icon: 'activity', baseUrl: '/api/process/operation', idField: 'operationId',
    fields: ['operationCode', 'operationName', 'operationType', 'standardTime', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status'], defaults: { status: 'ENABLE' },
    overrides: {
      operationCode: { required: true }, operationName: { required: true },
      operationType: { type: 'select', options: values([['PRODUCTION', '生产', 'primary'], ['INSPECTION', '检验', 'success'], ['PACKAGING', '包装', 'warning']]) },
      standardTime: { label: '标准工时（分钟/单位）', type: 'number', min: 0, precision: 2, step: 0.1 }, status: { type: 'select', options: optionSets.enable }, createdTime: { form: false, width: 170 }
    },
    relations: [{ key: 'parameters', title: '工艺参数', resource: 'process-parameter', parentField: 'operationId' }]
  }),
  defineResource({
    key: 'process-parameter', domain: 'process', route: 'process/parameters', title: '工艺参数', icon: 'settings', baseUrl: '/api/process/parameter', idField: 'parameterId',
    fields: ['operationId', 'parameterName', 'standardValue', 'upperLimit', 'lowerLimit', 'unitId', 'remark'], filters: ['operationId', 'keyword'],
    overrides: { operationId: { required: true }, parameterName: { required: true }, standardValue: { required: true } }
  }),
  defineResource({
    key: 'process-route', domain: 'process', route: 'process/routes', title: '工艺路线', icon: 'route', baseUrl: '/api/process/route', idField: 'routeId',
    fields: ['routeCode', 'routeName', 'productId', 'version', 'status', 'remark', 'createdTime'], filters: ['keyword', 'productId', 'status'], defaults: { status: 'DRAFT' },
    overrides: { routeCode: { required: true }, routeName: { required: true }, productId: { required: true }, version: { required: true }, status: { type: 'select', options: optionSets.release }, createdTime: { form: false, width: 170 } },
    relations: [{ key: 'items', title: '路线工序明细', resource: 'process-route-item', parentField: 'routeId' }]
  }),
  defineResource({
    key: 'process-route-item', domain: 'process', route: 'process/route-items', title: '路线工序明细', baseUrl: '/api/process/route-item', idField: 'routeItemId', menu: false,
    fields: ['routeId', 'operationId', 'sequenceNo', 'standardTime', 'qcRequired', 'remark'], filters: ['routeId', 'operationId'],
    overrides: { routeId: { required: true }, operationId: { required: true }, sequenceNo: { required: true, type: 'number', min: 1 }, standardTime: { label: '路线工时（分钟/单位）', type: 'number', min: 0, precision: 2, step: 0.1 }, qcRequired: { type: 'select', options: optionSets.yesNo, defaultValue: 0 } }
  }),
  defineResource({
    key: 'process-work-instruction', domain: 'process', route: 'process/instructions', title: '作业指导书', icon: 'clipboard', baseUrl: '/api/process/work-instruction', idField: 'instructionId',
    fields: ['title', 'operationId', 'productId', 'version', 'fileId', 'effectiveTime', 'status', 'remark', 'createdTime'], filters: ['keyword', 'operationId', 'productId', 'status'], defaults: { status: 'DRAFT' },
    overrides: { title: { required: true }, operationId: { required: true }, version: { required: true }, status: { type: 'select', options: optionSets.release }, createdTime: { form: false, width: 170 } }
  })
]
