import { domainDefinitions, getDomainResources } from '@/config/resources'
import { hasPermission, resourcePermission } from '@/utils/permission'

function moduleEntry(domain) {
  const definition = domainDefinitions[domain]
  return {
    key: domain,
    label: definition.title,
    icon: definition.icon,
    permission: `module:${domain}:view`,
    children: [
      ...getDomainResources(domain).map((resource) => ({
        label: resource.title,
        icon: resource.icon,
        path: resource.routePath,
        resourceKey: resource.key,
        permission: resourcePermission(resource.key, 'view')
      }))
    ]
  }
}

export const moduleCatalog = Object.fromEntries(Object.entries(domainDefinitions).map(([key, definition]) => [
  key,
  {
    ...definition,
    entries: getDomainResources(key).map((resource) => ({
      label: resource.title,
      path: resource.routePath,
      icon: resource.icon,
      description: resource.description,
      permission: resourcePermission(resource.key, 'view')
    }))
  }
]))

moduleCatalog['big-screen'] = {
  title: '可视化大屏', icon: 'monitor', tone: 'blue', description: '生产运营指挥与实时数据监控',
  entries: [{ label: '生产运营大屏', path: '/big-screen/production', icon: 'monitor', permission: 'dashboard:big-screen:view' }]
}

moduleCatalog.ai = {
  title: 'AI 智能助手', icon: 'bot', tone: 'blue', description: '智能问答、看板解读与业务建议',
  entries: [{ label: '打开智能助手', action: 'ai', icon: 'bot', permission: 'ai:assistant:use' }]
}

export const navigationGroups = [
  {
    label: '运营中心',
    items: [
      { label: '首页', icon: 'dashboard', path: '/dashboard', permission: 'dashboard:summary:view' },
      moduleEntry('mfg'),
      moduleEntry('wms'),
      moduleEntry('qms')
    ]
  },
  {
    label: '制造基础',
    items: [moduleEntry('mdm'), moduleEntry('process'), moduleEntry('factory')]
  },
  {
    label: '运营保障',
    items: [moduleEntry('eam'), moduleEntry('trace'), moduleEntry('org'), moduleEntry('sys')]
  },
  {
    label: '重点能力',
    items: [
      { label: '可视化大屏', icon: 'monitor', path: '/big-screen/production', featured: true, permission: 'dashboard:big-screen:view' },
      { label: 'AI 智能助手', icon: 'bot', action: 'ai', featured: true, permission: 'ai:assistant:use' }
    ]
  }
]

export function getModuleDefinition(moduleKey) {
  const definition = moduleCatalog[moduleKey] || moduleCatalog.sys
  return { ...definition, entries: (definition.entries || []).filter((item) => !item.permission || hasPermission(item.permission)) }
}

function flattenItems(items) {
  return items.flatMap((item) => [
    ...(item.path || item.action ? [item] : []),
    ...(item.children ? flattenItems(item.children) : [])
  ])
}

export function getNavigationItems() {
  return getNavigationGroups().flatMap((group) => flattenItems(group.items))
}

export function getNavigationGroups() {
  return navigationGroups.map((group) => ({
    ...group,
    items: group.items.map(filterItem).filter(Boolean)
  })).filter((group) => group.items.length)
}

function filterItem(item) {
  if (item.permission && !hasPermission(item.permission)) return null
  if (!item.children) return item
  const children = item.children.map(filterItem).filter(Boolean)
  return children.length ? { ...item, children } : null
}
