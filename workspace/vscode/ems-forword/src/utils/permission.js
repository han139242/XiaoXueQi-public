import { authRevision, getCurrentUser, isFrameworkPreview } from '@/utils/auth'

// 普通 CRUD 接口只有属于这些业务域时，才会按“域:资源:动作”推导权限码。
const domains = new Set(['sys', 'org', 'mdm', 'process', 'factory', 'mfg', 'wms', 'qms', 'eam', 'trace'])
// 当前端接口资源名和后端权限资源名不完全一致时，在这里做别名转换。
const aliases = new Map([['wms/replenishment-suggestion', 'replenishment']])

// 特殊业务动作无法只根据 POST/PUT 推断含义，例如 release 是“下达”而不是普通“修改”。
// 每条规则依次保存：HTTP 方法、接口路径正则、该接口要求的权限码。
const specialRules = [
  ['put', /^\/api\/sys\/user\/[^/]+\/reset-password$/, 'sys:user:reset-password'],
  ['put', /^\/api\/sys\/user\/[^/]+\/roles$/, 'sys:user:assign-role'],
  ['put', /^\/api\/sys\/role\/[^/]+\/menus$/, 'sys:role:assign-menu'],
  ['put', /^\/api\/sys\/notification\/read-all$/, 'sys:notification:read'],
  ['put', /^\/api\/sys\/notification\/[^/]+\/read$/, 'sys:notification:read'],
  ['put', /^\/api\/sys\/notification\/[^/]+\/ack$/, 'sys:notification:ack'],
  ['get', /^\/api\/sys\/notification\/audit\/page$/, 'sys:notification:audit'],
  ['post', /^\/api\/sys\/file\/upload$/, 'sys:file:upload'],
  ['get', /^\/api\/sys\/file\/download\/[^/]+$/, 'sys:file:download'],
  ['post', /^\/api\/mfg\/andon-alert\/trigger$/, 'mfg:andon-alert:trigger'],
  ['put', /^\/api\/mfg\/andon-alert\/[^/]+\/close$/, 'mfg:andon-alert:close'],
  ['put', /^\/api\/mfg\/exception\/[^/]+\/handle$/, 'mfg:exception:handle'],
  ['put', /^\/api\/mfg\/production-order\/[^/]+\/release$/, 'mfg:production-order:release'],
  ['put', /^\/api\/mfg\/production-order\/[^/]+\/withdraw$/, 'mfg:production-order:withdraw'],
  ['post', /^\/api\/mfg\/production-plan\/generate\/[^/]+$/, 'mfg:production-plan:generate'],
  ['put', /^\/api\/mfg\/production-plan\/[^/]+\/release$/, 'mfg:production-plan:release'],
  ['put', /^\/api\/mfg\/production-plan\/[^/]+\/withdraw$/, 'mfg:production-plan:withdraw'],
  ['post', /^\/api\/mfg\/work-order\/generate\/[^/]+$/, 'mfg:work-order:generate'],
  ['post', /^\/api\/mfg\/work-order\/[^/]+\/generate-material-issue$/, 'mfg:work-order:material-issue'],
  ['put', /^\/api\/mfg\/work-order\/[^/]+\/release$/, 'mfg:work-order:release'],
  ['put', /^\/api\/mfg\/work-order\/[^/]+\/withdraw$/, 'mfg:work-order:withdraw'],
  ['post', /^\/api\/mfg\/dispatch-order\/generate\/[^/]+$/, 'mfg:dispatch-order:generate'],
  ['put', /^\/api\/mfg\/dispatch-order\/[^/]+\/release$/, 'mfg:dispatch-order:release'],
  ['put', /^\/api\/mfg\/dispatch-order\/[^/]+\/withdraw$/, 'mfg:dispatch-order:withdraw'],
  ['put', /^\/api\/mfg\/work-order\/[^/]+\/start$/, 'mfg:work-order:start'],
  ['put', /^\/api\/mfg\/task\/[^/]+\/start$/, 'mfg:task:start'],
  ['put', /^\/api\/mfg\/task\/[^/]+\/withdraw$/, 'mfg:task:withdraw'],
  ['put', /^\/api\/mfg\/task\/[^/]+\/finish$/, 'mfg:task:finish'],
  ['put', /^\/api\/mfg\/task\/[^/]+\/controlled-close$/, 'mfg:task:controlled-close'],
  ['post', /^\/api\/mfg\/work-report$/, 'mfg:work-report:submit'],
  ['post', /^\/api\/mfg\/work-report\/submit$/, 'mfg:work-report:submit'],
  ['post', /^\/api\/mfg\/work-report\/full$/, 'mfg:work-report:submit'],
  ['put', /^\/api\/mfg\/work-report\/[^/]+\/audit$/, 'mfg:work-report:audit'],
  ['put', /^\/api\/mfg\/work-report\/[^/]+\/withdraw$/, 'mfg:work-report:withdraw'],
  ['post', /^\/api\/mfg\/schedule-scenario\/[^/]+\/validate$/, 'mfg:schedule-scenario:validate'],
  ['post', /^\/api\/mfg\/schedule-scenario\/[^/]+\/orders\/[^/]+\/ai-advice$/, 'mfg:schedule-scenario:ai-advice'],
  ['post', /^\/api\/mfg\/schedule-scenario\/[^/]+\/ai-advice$/, 'mfg:schedule-scenario:ai-advice'],
  ['post', /^\/api\/mfg\/schedule-scenario\/manual-board\/ai-advice$/, 'mfg:schedule-scenario:ai-advice'],
  ['post', /^\/api\/mfg\/schedule-scenario\/manual-board\/ai-batch$/, 'mfg:schedule-scenario:create'],
  ['delete', /^\/api\/mfg\/schedule-scenario\/manual-board\/ai-batch\/[^/]+$/, 'mfg:schedule-scenario:delete'],
  ['put', /^\/api\/mfg\/schedule-scenario\/manual-board\/ai-batch\/[^/]+\/confirm$/, 'mfg:schedule-scenario:confirm'],
  ['put', /^\/api\/mfg\/schedule-scenario\/[^/]+\/confirm$/, 'mfg:schedule-scenario:confirm'],
  ['put', /^\/api\/mfg\/schedule-scenario\/[^/]+\/items\/[^/]+\/confirm$/, 'mfg:schedule-scenario:confirm'],
  ['post', /^\/api\/wms\/inbound-order\/generate-production\/[^/]+$/, 'wms:inbound-order:create'],
  ['put', /^\/api\/wms\/inbound-order\/[^/]+\/confirm$/, 'wms:inbound-order:confirm'],
  ['put', /^\/api\/wms\/inbound-order\/[^/]+\/withdraw$/, 'wms:inbound-order:withdraw'],
  ['put', /^\/api\/wms\/outbound-order\/[^/]+\/confirm$/, 'wms:outbound-order:confirm'],
  ['put', /^\/api\/wms\/outbound-order\/[^/]+\/withdraw$/, 'wms:outbound-order:withdraw'],
  ['put', /^\/api\/wms\/return-order\/[^/]+\/confirm$/, 'wms:return-order:confirm'],
  ['put', /^\/api\/wms\/return-order\/[^/]+\/withdraw$/, 'wms:return-order:withdraw'],
  ['put', /^\/api\/wms\/stocktake-order\/[^/]+\/finish$/, 'wms:stocktake-order:finish'],
  ['post', /^\/api\/wms\/replenishment-suggestion\/generate$/, 'wms:replenishment:generate'],
  ['put', /^\/api\/qms\/inspection-order\/[^/]+\/submit$/, 'qms:inspection-order:submit'],
  ['put', /^\/api\/qms\/inspection-order\/[^/]+\/withdraw$/, 'qms:inspection-order:withdraw'],
  ['post', /^\/api\/qms\/inspection-result\/batch$/, 'qms:inspection-result:batch-save'],
  ['put', /^\/api\/qms\/nonconforming-order\/[^/]+\/approve$/, 'qms:nonconforming-order:approve'],
  ['post', /^\/api\/qms\/nonconforming-order\/generate-from-inspection\/[^/]+$/, 'qms:nonconforming-order:generate'],
  ['put', /^\/api\/qms\/nonconforming-order\/[^/]+\/complete-disposition$/, 'qms:nonconforming-order:complete'],
  ['put', /^\/api\/eam\/repair-order\/[^/]+\/assign$/, 'eam:repair-order:assign'],
  ['put', /^\/api\/eam\/repair-order\/[^/]+\/close$/, 'eam:repair-order:close'],
  ['post', /^\/api\/trace\/barcode\/generate$/, 'trace:barcode:generate'],
  ['get', /^\/api\/trace\/barcode\/scan\/[^/]+$/, 'trace:barcode:scan'],
  ['get', /^\/api\/trace\/product-lot\/lot-no\/[^/]+$/, 'trace:product-lot:trace'],
  ['get', /^\/api\/trace\/material-usage\/batch\/[^/]+$/, 'trace:material-usage:reverse-trace']
]

// 从当前用户信息中读取权限数组并转成Set，使权限码查询保持高效。
export function currentPermissions() {
  // 读取 authRevision.value 是为了让依赖此函数的 computed 在登录信息变化后重新计算。
  authRevision.value
  const permissions = getCurrentUser()?.permissions
  return new Set(Array.isArray(permissions) ? permissions : [])
}

// 判断当前用户是否拥有一个明确的权限码；超级管理员直接通过。
export function hasPermission(permissionCode) {
  if (isFrameworkPreview()) return true
  const user = getCurrentUser()
  if (user?.superAdmin || user?.roles?.includes('SUPER_ADMIN')) return true
  return Boolean(permissionCode) && currentPermissions().has(permissionCode)
}

// 把资源配置中的 mfg-production-order + view 转成后端使用的 mfg:production-order:view。
export function resourcePermission(resourceKey, action = 'view') {
  const [domain, ...resourceParts] = String(resourceKey || '').split('-')
  if (!domain || !resourceParts.length) return ''
  return `${domain}:${resourceParts.join('-')}:${action}`
}

// 根据一次 Axios 请求的方法和 URL，推导出它需要的后端权限码。
// 优先匹配上面的特殊动作，普通接口再按 GET/POST/PUT/DELETE 推导 CRUD 权限。
export function resolveRequestPermission(method, rawUrl) {
  const requestMethod = String(method || '').toLowerCase()
  const [rawPath] = String(rawUrl || '').split('?', 2)
  const path = rawPath.replace(/\/$/, '')
  const special = specialRules.find(([ruleMethod, pattern]) => ruleMethod === requestMethod && pattern.test(path))
  if (special) return special[2]
  if (path === '/api/ai' || path.startsWith('/api/ai/')) return 'ai:assistant:use'
  if (path === '/api/dashboard' || path.startsWith('/api/dashboard/')) return 'dashboard:summary:view'

  const matched = path.match(/^\/api\/([a-z]+)\/([a-z0-9-]+)(?:\/.*)?$/)
  if (!matched || !domains.has(matched[1])) return ''
  const domain = matched[1]
  const resource = aliases.get(`${domain}/${matched[2]}`) || matched[2]
  let action = ''
  if (requestMethod === 'get') action = path.endsWith('/excel') ? 'export' : 'view'
  if (requestMethod === 'post') action = 'create'
  if (requestMethod === 'put' || requestMethod === 'patch') action = 'update'
  if (requestMethod === 'delete') action = 'delete'
  return action ? `${domain}:${resource}:${action}` : ''
}

// 页面在显示按钮或执行操作前调用它，使“看得见的按钮”和“允许发送的请求”口径一致。
export function hasRequestPermission(method, url) {
  if (isFrameworkPreview()) return true
  return hasPermission(resolveRequestPermission(method, url))
}
