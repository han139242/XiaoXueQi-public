import { domainDefinitions, getDomainResources } from '@/config/resources'

function moduleEntry(domain) {
  const definition = domainDefinitions[domain]
  return {
    key: domain,
    label: definition.title,
    icon: definition.icon,
    children: [
      ...getDomainResources(domain).map((resource) => ({
        label: resource.title,
        icon: resource.icon,
        path: resource.routePath,
        resourceKey: resource.key
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
      description: resource.description
    }))
  }
]))

moduleCatalog['big-screen'] = {
  title: '可视化大屏', icon: 'monitor', tone: 'blue', description: '生产运营指挥与实时数据监控',
  entries: [{ label: '生产运营大屏', path: '/big-screen/production', icon: 'monitor' }]
}

moduleCatalog.ai = {
  title: 'AI 智能助手', icon: 'bot', tone: 'blue', description: '智能问答、看板解读与业务建议',
  entries: [{ label: '打开智能助手', action: 'ai', icon: 'bot' }]
}

export const navigationGroups = [
  {
    label: '运营中心',
    items: [
      { label: '首页', icon: 'dashboard', path: '/dashboard' },
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
      { label: '可视化大屏', icon: 'monitor', path: '/big-screen/production', featured: true },
      { label: 'AI 智能助手', icon: 'bot', action: 'ai', featured: true }
    ]
  }
]

export function getModuleDefinition(moduleKey) {
  return moduleCatalog[moduleKey] || moduleCatalog.sys
}

function flattenItems(items) {
  return items.flatMap((item) => [
    ...(item.path || item.action ? [item] : []),
    ...(item.children ? flattenItems(item.children) : [])
  ])
}

export function getNavigationItems() {
  return navigationGroups.flatMap((group) => flattenItems(group.items))
}
