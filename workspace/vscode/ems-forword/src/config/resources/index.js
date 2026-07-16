// 每个文件保存一个MES业务域的资源配置，这里是全部配置的统一入口。
import sys from './sys'
import org from './org'
import mdm from './mdm'
import process from './process'
import factory from './factory'
import mfg from './mfg'
import wms from './wms'
import qms from './qms'
import eam from './eam'
import trace from './trace'
import { resourcePermission } from '@/utils/permission'

// 展开并合并所有模块数组，形成约80个资源配置的总数组。
export const resources = [...sys, ...org, ...mdm, ...process, ...factory, ...mfg, ...wms, ...qms, ...eam, ...trace]
// 将数组转换为 key -> 配置 的Map结构，便于通过mfg-task等key快速找到页面配置。
export const resourceMap = Object.fromEntries(resources.map((item) => [item.key, item]))

// 业务域定义用于生成模块名称、图标和菜单分组，不是后端数据。
export const domainDefinitions = {
  mfg: { title: '生产制造', icon: 'clipboard', tone: 'blue', description: '订单、计划、工单、派工、任务与报工' },
  wms: { title: '仓储库存', icon: 'warehouse', tone: 'blue', description: '仓库、库存、出入库、盘点与补货' },
  qms: { title: '质量管理', icon: 'shield-check', tone: 'teal', description: '检验标准、检验方案、质检与不合格品闭环' },
  mdm: { title: '主数据', icon: 'database', tone: 'blue', description: '单位、供应商、客户、物料、产品与物料清单' },
  process: { title: '工艺管理', icon: 'route', tone: 'amber', description: '工序、参数、工艺路线与作业指导书' },
  factory: { title: '工厂建模', icon: 'factory', tone: 'teal', description: '车间、产线、工位及工序能力' },
  eam: { title: '设备运维', icon: 'wrench', tone: 'amber', description: '设备台账、状态、保养、点检与维修' },
  trace: { title: '条码追溯', icon: 'scan', tone: 'teal', description: '条码、产品批次与物料使用全链追溯' },
  org: { title: '组织人事', icon: 'users', tone: 'teal', description: '部门、员工、班组和班次' },
  sys: { title: '系统管理', icon: 'settings', tone: 'blue', description: '用户角色、字典参数、文件和审计日志' }
}

export function getResourceConfig(key) {
  return resourceMap[key]
}

// 获取某个模块的资源；menu=false的主从明细资源默认不出现在菜单中。
export function getDomainResources(domain, includeHidden = false) {
  return resources.filter((item) => item.domain === domain && (includeHidden || item.menu !== false))
}

// 配置驱动的核心：运行时把每个资源配置转换成一个Vue Router路由对象。
// 这些路由共用resource/index.vue，再通过resourceKey读取各自字段、接口和按钮配置。
export const resourceRoutes = resources.map((config) => ({
  path: config.route,
  name: `Resource-${config.key}`,
  component: () => import('@/pages/resource/index.vue'),
  // props把资源身份传给通用页面，例如resourceKey=mfg-production-order。
  props: { resourceKey: config.key },
  // meta提供标题、图标以及进入页面所需的view权限。
  meta: { title: config.title, icon: config.icon, resourceKey: config.key, permission: resourcePermission(config.key, 'view') }
}))
