import { authRevision, getCurrentUser, isFrameworkPreview } from '@/utils/auth'

const domains = new Set(['sys', 'org', 'mdm', 'process', 'factory', 'mfg', 'wms', 'qms', 'eam', 'trace'])
const aliases = new Map([['wms/replenishment-suggestion', 'replenishment']])

const specialRules = [
  ['put', /^\/api\/sys\/user\/[^/]+\/reset-password$/, 'sys:user:reset-password'],
  ['put', /^\/api\/sys\/user\/[^/]+\/roles$/, 'sys:user:assign-role'],
  ['put', /^\/api\/sys\/role\/[^/]+\/menus$/, 'sys:role:assign-menu'],
  ['post', /^\/api\/sys\/file\/upload$/, 'sys:file:upload'],
  ['get', /^\/api\/sys\/file\/download\/[^/]+$/, 'sys:file:download'],
  ['post', /^\/api\/mfg\/andon-alert\/trigger$/, 'mfg:andon-alert:trigger'],
  ['put', /^\/api\/mfg\/andon-alert\/[^/]+\/close$/, 'mfg:andon-alert:close'],
  ['put', /^\/api\/mfg\/exception\/[^/]+\/handle$/, 'mfg:exception:handle'],
  ['put', /^\/api\/mfg\/production-order\/[^/]+\/release$/, 'mfg:production-order:release'],
  ['post', /^\/api\/mfg\/production-plan\/generate\/[^/]+$/, 'mfg:production-plan:generate'],
  ['post', /^\/api\/mfg\/work-order\/generate\/[^/]+$/, 'mfg:work-order:generate'],
  ['put', /^\/api\/mfg\/work-order\/[^/]+\/release$/, 'mfg:work-order:release'],
  ['put', /^\/api\/mfg\/task\/[^/]+\/start$/, 'mfg:task:start'],
  ['put', /^\/api\/mfg\/task\/[^/]+\/finish$/, 'mfg:task:finish'],
  ['post', /^\/api\/mfg\/work-report$/, 'mfg:work-report:submit'],
  ['post', /^\/api\/mfg\/work-report\/submit$/, 'mfg:work-report:submit'],
  ['post', /^\/api\/mfg\/work-report\/full$/, 'mfg:work-report:submit'],
  ['put', /^\/api\/mfg\/work-report\/[^/]+\/audit$/, 'mfg:work-report:audit'],
  ['post', /^\/api\/mfg\/schedule-scenario\/[^/]+\/validate$/, 'mfg:schedule-scenario:validate'],
  ['post', /^\/api\/mfg\/schedule-scenario\/[^/]+\/orders\/[^/]+\/ai-advice$/, 'mfg:schedule-scenario:ai-advice'],
  ['post', /^\/api\/mfg\/schedule-scenario\/[^/]+\/ai-advice$/, 'mfg:schedule-scenario:ai-advice'],
  ['put', /^\/api\/mfg\/schedule-scenario\/[^/]+\/confirm$/, 'mfg:schedule-scenario:confirm'],
  ['put', /^\/api\/wms\/inbound-order\/[^/]+\/confirm$/, 'wms:inbound-order:confirm'],
  ['put', /^\/api\/wms\/outbound-order\/[^/]+\/confirm$/, 'wms:outbound-order:confirm'],
  ['put', /^\/api\/wms\/return-order\/[^/]+\/confirm$/, 'wms:return-order:confirm'],
  ['put', /^\/api\/wms\/stocktake-order\/[^/]+\/finish$/, 'wms:stocktake-order:finish'],
  ['post', /^\/api\/wms\/replenishment-suggestion\/generate$/, 'wms:replenishment:generate'],
  ['put', /^\/api\/qms\/inspection-order\/[^/]+\/submit$/, 'qms:inspection-order:submit'],
  ['post', /^\/api\/qms\/inspection-result\/batch$/, 'qms:inspection-result:batch-save'],
  ['put', /^\/api\/qms\/nonconforming-order\/[^/]+\/approve$/, 'qms:nonconforming-order:approve'],
  ['put', /^\/api\/eam\/repair-order\/[^/]+\/assign$/, 'eam:repair-order:assign'],
  ['put', /^\/api\/eam\/repair-order\/[^/]+\/close$/, 'eam:repair-order:close'],
  ['post', /^\/api\/trace\/barcode\/generate$/, 'trace:barcode:generate'],
  ['get', /^\/api\/trace\/barcode\/scan\/[^/]+$/, 'trace:barcode:scan'],
  ['get', /^\/api\/trace\/product-lot\/lot-no\/[^/]+$/, 'trace:product-lot:trace'],
  ['get', /^\/api\/trace\/material-usage\/batch\/[^/]+$/, 'trace:material-usage:reverse-trace']
]

export function currentPermissions() {
  authRevision.value
  const permissions = getCurrentUser()?.permissions
  return new Set(Array.isArray(permissions) ? permissions : [])
}

export function hasPermission(permissionCode) {
  if (isFrameworkPreview()) return true
  const user = getCurrentUser()
  if (user?.superAdmin || user?.roles?.includes('SUPER_ADMIN')) return true
  return Boolean(permissionCode) && currentPermissions().has(permissionCode)
}

export function resourcePermission(resourceKey, action = 'view') {
  const [domain, ...resourceParts] = String(resourceKey || '').split('-')
  if (!domain || !resourceParts.length) return ''
  return `${domain}:${resourceParts.join('-')}:${action}`
}

export function resolveRequestPermission(method, rawUrl) {
  const requestMethod = String(method || '').toLowerCase()
  const path = String(rawUrl || '').split('?')[0].replace(/\/$/, '')
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

export function hasRequestPermission(method, url) {
  if (isFrameworkPreview()) return true
  return hasPermission(resolveRequestPermission(method, url))
}
