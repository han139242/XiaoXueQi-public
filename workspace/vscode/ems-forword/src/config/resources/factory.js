import { defineResource, optionSets, values } from './helpers'

// 工厂建模资源：车间 -> 产线 -> 工位 -> 工位可执行工序。
// 每个defineResource对象都会被通用资源页转换为路由、菜单、查询表格和编辑表单。
// 默认导出该业务域的资源数组，由resources/index.js统一汇总。
export default [
  defineResource({
    key: 'factory-workshop', domain: 'factory', route: 'factory/workshops', title: '车间管理', icon: 'factory', baseUrl: '/api/factory/workshop', idField: 'workshopId',
    fields: ['workshopCode', 'workshopName', 'managerId', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status'], defaults: { status: 'RUNNING' },
    overrides: { workshopCode: { required: true }, workshopName: { required: true }, status: { type: 'select', options: values([['RUNNING', '运行', 'success'], ['DISABLE', '停用', 'info']]) }, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'factory-line', domain: 'factory', route: 'factory/lines', title: '产线管理', icon: 'route', baseUrl: '/api/factory/line', idField: 'lineId',
    fields: ['lineCode', 'lineName', 'workshopId', 'dailyCapacity', 'status', 'remark', 'createdTime'], filters: ['keyword', 'workshopId', 'status'], defaults: { status: 'RUNNING' },
    overrides: { lineCode: { required: true }, lineName: { required: true }, workshopId: { required: true }, dailyCapacity: { type: 'number', min: 0 }, status: { type: 'select', options: values([['RUNNING', '运行', 'success'], ['STOPPED', '停线', 'danger'], ['MAINTAINING', '维护', 'warning']]) }, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'factory-station', domain: 'factory', route: 'factory/stations', title: '工位管理', icon: 'factory', baseUrl: '/api/factory/station', idField: 'stationId',
    fields: ['stationCode', 'stationName', 'lineId', 'stationType', 'status', 'remark', 'createdTime'], filters: ['keyword', 'lineId', 'stationType', 'status'], defaults: { status: 'RUNNING' },
    overrides: {
      stationCode: { required: true }, stationName: { required: true }, lineId: { required: true },
      stationType: { type: 'select', options: values([['PRODUCTION', '生产', 'primary'], ['INSPECTION', '检验', 'success'], ['PACKAGING', '包装', 'warning']]) },
      status: { type: 'select', options: values([['RUNNING', '运行', 'success'], ['DISABLE', '停用', 'info'], ['MAINTAINING', '维护', 'warning']]) }, createdTime: { form: false, width: 170 }
    },
    relations: [{ key: 'operations', title: '可执行工序', resource: 'factory-station-operation', parentField: 'stationId', endpoint: (row) => `/api/factory/station-operation/station/${row.stationId}` }]
  }),
  defineResource({
    key: 'factory-station-operation', domain: 'factory', route: 'factory/station-operations', title: '工位工序', icon: 'activity', baseUrl: '/api/factory/station-operation', idField: 'id',
    fields: ['stationId', 'operationId', 'isDefault', 'status', 'createdTime'], filters: ['stationId', 'operationId', 'isDefault', 'status'], defaults: { status: 'ENABLE', isDefault: 0 },
    overrides: { stationId: { required: true }, operationId: { required: true }, isDefault: { type: 'select', options: optionSets.yesNo }, status: { type: 'select', options: optionSets.enable }, createdTime: { form: false, width: 170 } }
  })
]
