const request = require('../utils/request')

const definitions = {
  product: {
    url: '/api/mdm/product/page',
    id: 'productId',
    label(item) {
      return [item.productCode, item.productName].filter(Boolean).join(' · ')
    },
    details(item) {
      return { productCode: item.productCode || '', productName: item.productName || '' }
    }
  },
  customer: {
    url: '/api/mdm/customer/page',
    id: 'customerId',
    label(item) {
      return item.customerName || item.customerCode
    }
  },
  operation: {
    url: '/api/process/operation/page',
    id: 'operationId',
    label(item) {
      return item.operationName
    }
  },
  station: {
    url: '/api/factory/station/page',
    id: 'stationId',
    label(item) {
      return item.stationName
    }
  },
  employee: {
    url: '/api/org/employee/page',
    id: 'employeeId',
    label(item) {
      return item.employeeName
    }
  },
  material: {
    url: '/api/mdm/material/page',
    id: 'materialId',
    label(item) {
      return [item.materialCode, item.materialName].filter(Boolean).join(' · ')
    }
  },
  warehouse: {
    url: '/api/wms/warehouse/page',
    id: 'warehouseId',
    label(item) {
      return item.warehouseName || item.warehouseCode
    }
  },
  location: {
    url: '/api/wms/location/page',
    id: 'locationId',
    label(item) {
      return item.locationName || item.locationCode
    }
  },
  equipment: {
    url: '/api/eam/equipment/page',
    id: 'equipmentId',
    label(item) {
      return [item.equipmentCode, item.equipmentName].filter(Boolean).join(' · ')
    }
  },
  workOrder: {
    url: '/api/mfg/work-order/page',
    id: 'workOrderId',
    label(item) {
      return item.workOrderNo
    },
    details(item) {
      return { productId: item.productId, planId: item.planId, createdTime: item.createdTime, updatedTime: item.updatedTime }
    }
  }
}

function getCache() {
  const app = getApp()
  if (!app.globalData.lookups) app.globalData.lookups = {}
  return app.globalData.lookups
}

async function loadOne(name, force) {
  const definition = definitions[name]
  if (!definition) return { items: [], map: {} }
  const cache = getCache()
  const cached = cache[name]
  if (!force && cached && Date.now() - cached.loadedAt < 5 * 60 * 1000) {
    return cached
  }

  try {
    const result = await request({
      url: definition.url,
      data: { current: 1, size: 500 },
      silent: true
    })
    const records = result && Array.isArray(result.records) ? result.records : []
    const items = records.map((item) => ({
      id: item[definition.id],
      label: definition.label(item) || String(item[definition.id]),
      ...(definition.details ? definition.details(item) : {})
    })).filter((item) => item.id !== null && item.id !== undefined)
    const map = {}
    items.forEach((item) => {
      map[String(item.id)] = item.label
    })
    cache[name] = { items, map, loadedAt: Date.now() }
    return cache[name]
  } catch (error) {
    return cached || { items: [], map: {}, loadedAt: 0 }
  }
}

async function load(names, force) {
  const values = await Promise.all(names.map((name) => loadOne(name, force)))
  const result = {}
  names.forEach((name, index) => {
    result[name] = values[index]
  })
  return result
}

function label(lookup, id, prefix) {
  if (id === null || id === undefined || id === '') return '-'
  return lookup && lookup.map && lookup.map[String(id)]
    ? lookup.map[String(id)]
    : prefix + ' ID ' + id
}

module.exports = {
  load,
  label
}
