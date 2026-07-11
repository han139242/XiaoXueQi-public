import { defineResource, optionSets, values } from './helpers'

const enable = { type: 'select', options: optionSets.enable }

export default [
  defineResource({ key: 'mdm-unit', domain: 'mdm', route: 'mdm/units', title: '计量单位', icon: 'database', baseUrl: '/api/mdm/unit', idField: 'unitId', fields: ['unitCode', 'unitName', 'precisionScale', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status'], defaults: { status: 'ENABLE', precisionScale: 2 }, overrides: { unitCode: { required: true }, unitName: { required: true }, precisionScale: { type: 'number', min: 0, max: 8 }, status: enable, createdTime: { form: false, width: 170 } } }),
  defineResource({ key: 'mdm-supplier', domain: 'mdm', route: 'mdm/suppliers', title: '供应商管理', icon: 'warehouse', baseUrl: '/api/mdm/supplier', idField: 'supplierId', fields: ['supplierCode', 'supplierName', 'contactName', 'contactPhone', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status'], defaults: { status: 'ENABLE' }, overrides: { supplierCode: { required: true }, supplierName: { required: true }, status: enable, createdTime: { form: false, width: 170 } } }),
  defineResource({ key: 'mdm-customer', domain: 'mdm', route: 'mdm/customers', title: '客户管理', icon: 'users', baseUrl: '/api/mdm/customer', idField: 'customerId', fields: ['customerCode', 'customerName', 'contactName', 'contactPhone', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status'], defaults: { status: 'ENABLE' }, overrides: { customerCode: { required: true }, customerName: { required: true }, status: enable, createdTime: { form: false, width: 170 } } }),
  defineResource({
    key: 'mdm-material-category', domain: 'mdm', route: 'mdm/material-categories', title: '物料分类', icon: 'database', baseUrl: '/api/mdm/material-category', idField: 'categoryId', treeEndpoint: '/api/mdm/material-category/tree',
    fields: ['parentId', 'categoryCode', 'categoryName', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status'], defaults: { status: 'ENABLE' },
    overrides: { parentId: { type: 'reference', reference: 'mdm-material-category' }, categoryCode: { required: true }, categoryName: { required: true }, status: enable, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'mdm-material', domain: 'mdm', route: 'mdm/materials', title: '物料管理', icon: 'package', baseUrl: '/api/mdm/material', idField: 'materialId',
    fields: ['materialCode', 'materialName', 'categoryId', 'specModel', 'unitId', 'materialType', 'status', 'remark', 'createdTime'], filters: ['materialCode', 'materialName', 'categoryId', 'status'], defaults: { status: 'ENABLE' },
    overrides: {
      materialCode: { required: true }, materialName: { required: true }, categoryId: { reference: 'mdm-material-category', required: true }, unitId: { required: true },
      materialType: { type: 'select', options: values([['RAW', '原料', 'primary'], ['SEMI_FINISHED', '半成品', 'warning'], ['FINISHED', '成品', 'success'], ['AUXILIARY', '辅料', 'info']]) },
      status: enable, createdTime: { form: false, width: 170 }
    }
  }),
  defineResource({
    key: 'mdm-product', domain: 'mdm', route: 'mdm/products', title: '产品管理', icon: 'package', baseUrl: '/api/mdm/product', idField: 'productId',
    fields: ['productCode', 'productName', 'productModel', 'materialId', 'status', 'remark', 'createdTime'], filters: ['productCode', 'productName', 'status'], defaults: { status: 'ENABLE' },
    overrides: { productCode: { required: true }, productName: { required: true }, materialId: { required: true }, status: enable, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'mdm-bom', domain: 'mdm', route: 'mdm/boms', title: '产品 BOM', icon: 'route', baseUrl: '/api/mdm/bom', idField: 'bomId',
    fields: ['bomCode', 'productId', 'bomVersion', 'effectiveDate', 'status', 'remark', 'createdTime'], filters: ['bomCode', 'productId', 'status'], defaults: { status: 'DRAFT' },
    overrides: { bomCode: { required: true }, productId: { required: true }, bomVersion: { required: true }, status: { type: 'select', options: optionSets.release }, createdTime: { form: false, width: 170 } },
    relations: [{ key: 'items', title: 'BOM 物料明细', resource: 'mdm-bom-item', parentField: 'bomId', endpoint: (row) => `/api/mdm/bom-item/page?current=1&size=200&bomId=${row.bomId}` }]
  }),
  defineResource({
    key: 'mdm-bom-item', domain: 'mdm', route: 'mdm/bom-items', title: 'BOM 物料明细', baseUrl: '/api/mdm/bom-item', idField: 'bomItemId', menu: false,
    fields: ['bomId', 'materialId', 'quantity', 'lossRate', 'sequenceNo', 'remark'], filters: ['bomId', 'materialId'],
    overrides: { bomId: { required: true }, materialId: { required: true }, quantity: { required: true, type: 'number', min: 0 }, lossRate: { type: 'number', min: 0, max: 100 }, sequenceNo: { type: 'number', min: 1 } }
  })
]
