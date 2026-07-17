import dayjs from 'dayjs'
import { getResourceList, getResourcePage } from '@/api/resource'
import { getResourceConfig } from '@/config/resources'
import { referencePresentation } from '@/config/resources/helpers'

// 缓存已经加载过的关联选项，避免一个页面中的多个 productId 下拉框反复请求同一接口。
const cache = new Map()

// 递归展开树形接口数据，让父节点和所有children都能成为下拉选项。
function flatten(items) {
  return (items || []).flatMap((item) => [item, ...flatten(item.children)])
}

// 根据资源的展示字段，把一行数据拼成用户可读的下拉选项名称。
function labelFor(config, row) {
  // 数据库存的是关联 ID，界面根据 referencePresentation 拼出“名称 + 编码”等可读文字。
  const fields = referencePresentation[config.key] || config.fields.filter((item) => item.type === 'text').slice(0, 2).map((item) => item.prop)
  const values = fields.map((prop) => row[prop]).filter((value) => value !== null && value !== undefined && value !== '')
  return values.length ? values.join(' · ') : `${config.title.replace('管理', '')} #${row[config.idField]}`
}

// 开发预览或接口回退时生成8条本地下拉选项。
function previewOptions(config) {
  return Array.from({ length: 8 }, (_, index) => {
    const value = index + 1
    return { value, label: `${config.title.replace('管理', '')} ${String(value).padStart(2, '0')}` }
  })
}

// 只有开发环境且环境变量未明确关闭时，才允许真实接口失败后使用模拟数据。
export function canUseResourceMockFallback() {
  return import.meta.env.DEV && import.meta.env.VITE_RESOURCE_MOCK_FALLBACK !== 'false'
}

// 加载某类关联资源并转换为value/label选项，同一模式和资源的并发请求共用缓存Promise。
export async function loadReferenceOptions(resourceKey, previewMode = false) {
  // 读取目标资源配置，再调用通用 API 获取数据，最后转换为 el-select 需要的 value/label。
  const mockFallback = canUseResourceMockFallback()
  const cacheKey = `${previewMode ? 'preview' : mockFallback ? 'mock-fallback' : 'live'}:${resourceKey}`
  if (cache.has(cacheKey)) return cache.get(cacheKey)
  const config = getResourceConfig(resourceKey)
  if (!config) return []
  const promise = (async () => {
    if (previewMode) return previewOptions(config)
    try {
      const result = config.treeEndpoint
        ? await getResourceList(config.treeEndpoint)
        : await getResourcePage(config.baseUrl, { current: 1, size: 200 })
      const data = result?.data
      const records = Array.isArray(data) ? flatten(data) : (data?.records || [])
      const options = records.map((row) => ({ value: row[config.idField], label: labelFor(config, row), row }))
      return options.length || !mockFallback ? options : previewOptions(config)
    } catch (error) {
      if (mockFallback) return previewOptions(config)
      throw error
    }
  })()
  cache.set(cacheKey, promise)
  try {
    return await promise
  } catch (error) {
    cache.delete(cacheKey)
    throw error
  }
}

// 新增或修改资源后清除它在三种数据模式下的选项缓存。
export function clearReferenceOptions(resourceKey) {
  // 新增或修改关联资源后清掉缓存，下次打开下拉框时才能看到最新数据。
  cache.delete(`live:${resourceKey}`)
  cache.delete(`preview:${resourceKey}`)
  cache.delete(`mock-fallback:${resourceKey}`)
}

// 按字段类型、名称和业务含义生成一条合理的预览值。
function previewValue(field, index, config) {
  // 开发预览模式没有后端数据时，根据字段类型生成结构合理的演示值。
  if (field.options?.length) return field.options[index % field.options.length].value
  if (field.type === 'reference') return (index % 6) + 1
  if (field.type === 'date') return dayjs().subtract(index, 'day').format('YYYY-MM-DD')
  if (field.type === 'datetime') return dayjs().subtract(index * 2, 'hour').format('YYYY-MM-DDTHH:mm:ss')
  if (field.type === 'time') return ['08:00:00', '16:00:00', '00:00:00'][index % 3]
  if (field.type === 'number') {
    if (/Rate/.test(field.prop)) return Number((1.5 + index * 0.2).toFixed(2))
    if (field.prop === 'differenceQuantity') return index % 3 === 0 ? -8 - index : 5 + index
    if (/defectQuantity|scrapQuantity|shortageQuantity|lockedQuantity|inspectionQuantity/i.test(field.prop)) return 8 + index * 3
    if (/completedQuantity|qualifiedQuantity|actualQuantity/i.test(field.prop)) return 920 + index * 145
    if (/orderQuantity|plannedQuantity|requiredQuantity|bookQuantity|batchQuantity|availableQuantity|inboundQuantity|outboundQuantity|returnQuantity|suggestedQuantity|dailyCapacity/i.test(field.prop)) return 1200 + index * 180
    if (field.prop === 'actualWorkHours') return Number((6.5 + index * 0.25).toFixed(2))
    if (field.prop === 'repairCost') return 180 + index * 45
    return index + 1
  }
  if (/Id$/.test(field.prop)) return 1001 + index
  if (field.prop === 'remark') return index % 3 === 0 ? '重点生产数据，请及时跟进' : ''
  if (/Code$/.test(field.prop)) return `${config.domain.toUpperCase()}-${String(index + 1).padStart(4, '0')}`
  if (/No$/.test(field.prop)) return `${dayjs().format('YYYYMMDD')}-${String(index + 1).padStart(3, '0')}`
  if (/Name$/.test(field.prop) || field.prop === 'title') return `${config.title.replace('管理', '')} ${index + 1}`
  if (/phone/i.test(field.prop)) return `1380000${String(index + 1).padStart(4, '0')}`
  return `${field.label}${index + 1}`
}

// 根据完整资源字段配置批量生成指定数量的预览表格行。
export function createPreviewRows(config, count = 12) {
  // 仅供开发预览或明确开启的 mock fallback 使用，不会写入真实数据库。
  return Array.from({ length: count }, (_, index) => {
    const row = { [config.idField]: index + 1 }
    config.fields.forEach((field) => {
      row[field.prop] = previewValue(field, index, config)
    })
    return row
  })
}
