export const moduleCatalog = {
  sys: {
    title: '系统管理',
    icon: 'settings',
    tone: 'blue',
    entries: ['用户管理', '角色权限', '菜单配置', '字典管理', '系统参数', '操作日志']
  },
  org: {
    title: '组织人事',
    icon: 'users',
    tone: 'teal',
    entries: ['部门管理', '员工管理', '班组管理', '班次管理']
  },
  mdm: {
    title: '主数据',
    icon: 'database',
    tone: 'blue',
    entries: ['计量单位', '供应商', '客户', '物料分类', '物料管理', '产品管理', 'BOM']
  },
  process: {
    title: '工艺管理',
    icon: 'route',
    tone: 'amber',
    entries: ['工序管理', '工艺路线', '工艺参数', '作业指导书']
  },
  factory: {
    title: '工厂建模',
    icon: 'factory',
    tone: 'teal',
    entries: ['车间管理', '产线管理', '工位管理', '工位工序']
  },
  mfg: {
    title: '生产制造',
    icon: 'clipboard',
    tone: 'green',
    entries: ['生产订单', '生产计划', '生产工单', '派工任务', '生产报工', '生产异常', '安灯告警']
  },
  wms: {
    title: '仓储库存',
    icon: 'warehouse',
    tone: 'blue',
    entries: ['仓库库位', '物料批次', '库存余额', '库存流水', '入库管理', '出库管理', '盘点管理']
  },
  qms: {
    title: '质量管理',
    icon: 'shield-check',
    tone: 'green',
    entries: ['检验项目', '检验标准', '检验方案', '检验单', '不合格品处理']
  },
  eam: {
    title: '设备运维',
    icon: 'wrench',
    tone: 'amber',
    entries: ['设备分类', '设备台账', '保养管理', '点检管理', '维修工单']
  },
  trace: {
    title: '条码追溯',
    icon: 'scan',
    tone: 'teal',
    entries: ['条码管理', '产品批次追溯', '物料使用追溯']
  },
  'big-screen': {
    title: '可视化大屏',
    icon: 'monitor',
    tone: 'blue',
    entries: ['生产运营大屏', '质量分析', '库存预警', '设备状态']
  },
  ai: {
    title: 'AI 智能助手',
    icon: 'bot',
    tone: 'green',
    entries: ['智能问答', '看板解读', '业务建议']
  }
}

export const navigationGroups = [
  {
    label: '运营中心',
    items: [
      { label: '运营工作台', icon: 'dashboard', path: '/dashboard' },
      { label: '生产制造', icon: 'clipboard', path: '/module/mfg' },
      { label: '仓储库存', icon: 'warehouse', path: '/module/wms' },
      { label: '质量管理', icon: 'shield-check', path: '/module/qms' }
    ]
  },
  {
    label: '基础建模',
    items: [
      { label: '主数据', icon: 'database', path: '/module/mdm' },
      { label: '工艺管理', icon: 'route', path: '/module/process' },
      { label: '工厂建模', icon: 'factory', path: '/module/factory' }
    ]
  },
  {
    label: '运营保障',
    items: [
      { label: '设备运维', icon: 'wrench', path: '/module/eam' },
      { label: '条码追溯', icon: 'scan', path: '/module/trace' },
      { label: '组织人事', icon: 'users', path: '/module/org' },
      { label: '系统管理', icon: 'settings', path: '/module/sys' }
    ]
  },
  {
    label: '重点能力',
    items: [
      { label: '可视化大屏', icon: 'monitor', path: '/module/big-screen' },
      { label: 'AI 智能助手', icon: 'bot', path: '/module/ai' }
    ]
  }
]

export function getModuleDefinition(moduleKey) {
  return moduleCatalog[moduleKey] || moduleCatalog.sys
}

export function getNavigationItems() {
  return navigationGroups.flatMap((group) => group.items)
}
