<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import AppIcon from '@/components/common/AppIcon.vue'
import MesChart from '@/components/chart/MesChart.vue'
import {
  getDashboardSummary,
  getEquipmentStatus,
  getProductionTrend,
  getQualityStatistics
} from '@/api/dashboard'
import { getResourcePage } from '@/api/resource'
import { createDashboardPreview } from '@/config/dashboard-preview'
import { isFrameworkPreview } from '@/utils/auth'

// 生产大屏面向车间看板，以固定1920x1080设计稿等比缩放，并每分钟刷新生产运营数据。
const router = useRouter()
const previewMode = isFrameworkPreview()

// scale和offset控制固定画布在不同屏幕上的缩放与居中；其余ref保存接口数据和加载状态。
const scale = ref(1)
const offsetX = ref(0)
const offsetY = ref(0)
const now = ref(dayjs())
const lastUpdated = ref(null)
const loading = ref(false)
const fullscreenActive = ref(false)
const loadError = ref('')
const failedDataCount = ref(0)
const summary = ref({})
const trend = ref([])
const quality = ref({})
const qualityClosure = ref({ total: 0, closed: 0, rate: null })
const equipment = ref({})
const schedulePlans = ref([])
const productionLines = ref([])
const scheduleError = ref('')
const scheduleExpanded = ref(false)

// 时钟每秒更新，业务数据每60秒更新；离开页面时必须清理。
let clockTimer
let refreshTimer

// 后端状态编码在大屏中转换为中文标签和CSS色调。
const scheduleStatusMeta = {
  DRAFT: { label: '待生效', tone: 'draft' },
  RELEASED: { label: '待执行', tone: 'released' },
  RUNNING: { label: '生产中', tone: 'running' },
  FINISHED: { label: '已完成', tone: 'finished' },
  CLOSED: { label: '已取消', tone: 'closed' }
}

const lineStatusMeta = {
  RUNNING: { label: '运行中', tone: 'running' },
  IDLE: { label: '待机', tone: 'idle' },
  STOPPED: { label: '已停机', tone: 'stopped' },
  MAINTENANCE: { label: '维保中', tone: 'maintenance' }
}

// 生成固定画布当前的平移和缩放CSS transform。
const stageStyle = computed(() => ({
  // translate负责居中，scale负责保持1920:1080比例且不超出可视区域。
  transform: `translate3d(${offsetX.value}px, ${offsetY.value}px, 0) scale(${scale.value})`
}))

// 以下四个计算属性把now和lastUpdated格式化为大屏时钟文字。
const dateText = computed(() => now.value.format('YYYY-MM-DD'))
// 将星期数字转换为中文星期。
const weekdayText = computed(() => ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][now.value.day()])
// 显示精确到秒的当前时间。
const timeText = computed(() => now.value.format('HH:mm:ss'))
// 显示最近一次成功同步数据的时间。
const updateText = computed(() => lastUpdated.value ? lastUpdated.value.format('HH:mm:ss') : '--:--:--')
// 综合加载状态和失败接口数量生成数据同步状态文字。
const syncText = computed(() => {
  // 根据成功/失败接口数量显示“已同步、部分更新或连接失败”。
  if (previewMode) return '演示数据'
  if (loading.value && !lastUpdated.value) return '正在连接数据'
  if (failedDataCount.value === 4) return '数据连接失败'
  if (failedDataCount.value > 0) return '部分数据未更新'
  if (scheduleError.value) return '排期数据未更新'
  return '数据已同步'
})

// 从生产汇总中读取今日合格数量。
const todayQualified = computed(() => Number(summary.value.todayQualifiedQuantity || 0))
// 今日产量和报工合格率由合格数、不良数在前端做简单派生计算。
const todayDefect = computed(() => Number(summary.value.todayDefectQuantity || 0))
// 今日总报工量等于合格数量加不良数量。
const todayOutput = computed(() => todayQualified.value + todayDefect.value)
// 有报工数据时计算今日报工合格率，否则返回null。
const reportPassRate = computed(() => todayOutput.value
  ? (todayQualified.value / todayOutput.value) * 100
  : null)
// 读取当前质量检验总次数。
const inspectionCount = computed(() => Number(quality.value.inspectionCount || 0))
const qualifiedInspectionCount = computed(() => Number(quality.value.qualifiedCount || 0))
const unqualifiedInspectionCount = computed(() => Number(quality.value.unqualifiedCount || 0))
const evaluatedInspectionCount = computed(() => qualifiedInspectionCount.value + unqualifiedInspectionCount.value)
const pendingInspectionCount = computed(() => Math.max(0, inspectionCount.value - evaluatedInspectionCount.value))
// 一次检验合格率只使用已经判定为合格或不合格的质检单，待判定单不进入分母。
const inspectionPassRate = computed(() => evaluatedInspectionCount.value
  ? (qualifiedInspectionCount.value / evaluatedInspectionCount.value) * 100
  : null)
// 有不合格处理数据时优先展示处理完成率；权限不足或无异常时回退到一次检验合格率。
const qualityFocusRate = computed(() => qualityClosure.value.rate ?? inspectionPassRate.value)
const qualityFocusLabel = computed(() => qualityClosure.value.rate === null ? '一次检验合格率' : '不合格处理完成率')
// 把质量主读数限制在0到100之间，供环形进度使用。
const qualityProgressWidth = computed(() => Math.min(100, Math.max(0, qualityFocusRate.value || 0)))
// 将百分比换算为圆形质量仪表盘需要的角度CSS变量。
const qualityDialStyle = computed(() => ({
  '--quality-angle': `${qualityProgressWidth.value * 3.6}deg`
}))
// 汇总运行、待机、停机、故障和维保设备得到设备总数。
const equipmentTotal = computed(() => ['running', 'idle', 'stopped', 'fault', 'maintenance']
  .reduce((total, key) => total + Number(equipment.value[key] || 0), 0))
// 读取库存补货预警数量。
const warningCount = computed(() => Number(summary.value.inventoryWarningCount || 0))
// 读取设备故障数量。
const faultCount = computed(() => Number(summary.value.equipmentFaultCount || 0))
// 将七日趋势原始记录补充总产量、日期、星期和合格率。
const trendRows = computed(() => trend.value.map((item) => {
  // 给后端趋势记录补充总产量、日期文本、星期和当日合格率，便于表格与图表复用。
  const qualifiedQuantity = Number(item.qualifiedQuantity || 0)
  const defectQuantity = Number(item.defectQuantity || 0)
  const output = qualifiedQuantity + defectQuantity
  const date = dayjs(item.date)
  return {
    ...item,
    output,
    dateLabel: date.isValid() ? date.format('MM-DD') : '--',
    weekdayLabel: date.isValid() ? `周${['日', '一', '二', '三', '四', '五', '六'][date.day()]}` : '--',
    passRate: output ? Number(((qualifiedQuantity / output) * 100).toFixed(2)) : null
  }
}))
// 判断七日趋势中是否至少有一天存在产量数据。
const hasTrendData = computed(() => trendRows.value.some((item) => item.output > 0))
// 汇总七日总报工产量。
const sevenDayOutput = computed(() => trendRows.value.reduce((total, item) => total + item.output, 0))

// 已超过结束时间且仍未完成/关闭的计划需要作为生产异常重点提示。
function isOverduePlan(item) {
  const end = dayjs(item.plannedEndTime)
  return end.isValid()
    && end.isBefore(now.value)
    && !['FINISHED', 'CLOSED'].includes(item.status)
}

// 统计全部已加载计划中的逾期项，不受主时间轴的近7日显示窗口限制。
const overduePlans = computed(() => schedulePlans.value
  .filter(isOverduePlan)
  .sort((a, b) => dayjs(a.plannedEndTime).valueOf() - dayjs(b.plannedEndTime).valueOf()))
const overduePlanCount = computed(() => overduePlans.value.length)

// 统计计划、设备、库存和质量四类风险中有多少类当前非零。
const riskCategoryCount = computed(() => [overduePlanCount.value, faultCount.value, warningCount.value, todayDefect.value]
  .filter((value) => value > 0).length)

// 排期保留今天的正常计划、未来计划以及近7日仍未闭环的逾期计划，并按开始时间排序。
const scheduleRows = computed(() => schedulePlans.value
  .filter((item) => {
    const start = dayjs(item.plannedStartTime)
    const end = dayjs(item.plannedEndTime)
    const recentRiskBoundary = now.value.subtract(6, 'day').startOf('day')
    return start.isValid()
      && end.isValid()
      && end.isAfter(start)
      && item.status !== 'CLOSED'
      && (isOverduePlan(item)
        ? !end.isBefore(recentRiskBoundary)
        : !end.isBefore(now.value.startOf('day')))
  })
  .map((item) => {
    if (isOverduePlan(item)) {
      return { ...item, statusLabel: '已逾期', statusTone: 'overdue' }
    }
    const status = scheduleStatusMeta[item.status] || { label: item.status || '未知状态', tone: 'closed' }
    return { ...item, statusLabel: status.label, statusTone: status.tone }
  })
  .sort((a, b) => dayjs(a.plannedStartTime).valueOf() - dayjs(b.plannedStartTime).valueOf()))
// 同一产线的多个计划共用一条时间轴，避免计划多时重复显示产线名称。
const scheduleLineRows = computed(() => {
  const grouped = new Map(productionLines.value.map((line) => [String(line.lineId), { ...line, plans: [] }]))
  scheduleRows.value.forEach((plan) => {
    const key = String(plan.lineId || 'unknown')
    if (!grouped.has(key)) {
      grouped.set(key, {
        lineId: plan.lineId,
        lineCode: '',
        lineName: plan.lineName,
        status: '',
        dailyCapacity: 0,
        plans: []
      })
    }
    grouped.get(key).plans.push(plan)
  })
  return [...grouped.values()]
    .sort((a, b) => {
      const priority = (line) => {
        if (line.plans.some((plan) => plan.statusTone === 'overdue')) return 0
        if (line.plans.some((plan) => plan.status === 'RUNNING')) return 1
        if (line.plans.length) return 2
        if (line.stationCount === 0) return 4
        return 3
      }
      return priority(a) - priority(b)
        || String(a.lineCode || a.lineName).localeCompare(String(b.lineCode || b.lineName), 'zh-CN')
    })
})

const visibleScheduleLineRows = computed(() => scheduleLineRows.value.slice(0, 5))
const expandedScheduleLineRows = computed(() => scheduleLineRows.value)

// 产线状态只使用真实产线与真实生产计划，不用排期时间推导虚假的生产进度。
const lineStatusRows = computed(() => productionLines.value.map((line) => {
  const plans = scheduleRows.value.filter((plan) => String(plan.lineId) === String(line.lineId))
  const lineOverduePlans = plans.filter((plan) => plan.statusTone === 'overdue')
  const currentPlan = plans.find((plan) => {
    const current = now.value.valueOf()
    return current >= dayjs(plan.plannedStartTime).valueOf() && current <= dayjs(plan.plannedEndTime).valueOf()
  })
  const nextPlan = plans.find((plan) => dayjs(plan.plannedStartTime).isAfter(now.value))
  const focusPlan = lineOverduePlans[0] || currentPlan || nextPlan || null
  const status = line.stationCount === 0
    ? { label: '配置不完整', tone: 'incomplete' }
    : lineOverduePlans.length
      ? { label: `${lineOverduePlans.length}项计划逾期`, tone: 'overdue' }
      : (lineStatusMeta[line.status] || { label: line.status || '状态未知', tone: 'idle' })
  return {
    ...line,
    statusLabel: status.label,
    statusTone: status.tone,
    planCount: plans.length,
    scheduledQuantity: plans.reduce((total, plan) => total + Number(plan.plannedQuantity || 0), 0),
    currentPlan: currentPlan || null,
    nextPlan: nextPlan || null,
    focusPlan,
    focusLabel: lineOverduePlans.length ? '逾期计划' : (currentPlan ? '当前计划' : '下一计划'),
    overduePlanCount: lineOverduePlans.length
  }
}).sort((a, b) => {
  const priority = (line) => {
    if (line.overduePlanCount) return 0
    if (line.currentPlan) return 1
    if (line.nextPlan) return 2
    if (line.stationCount === 0) return 4
    return 3
  }
  return priority(a) - priority(b)
    || String(a.lineCode || a.lineName).localeCompare(String(b.lineCode || b.lineName), 'zh-CN')
}))

// 根据展开产线数动态计算弹窗高度，并限制最小和最大值。
const scheduleModalStyle = computed(() => ({
  '--schedule-modal-height': `${Math.min(940, Math.max(560, 222 + expandedScheduleLineRows.value.length * 86))}px`
}))
// 根据全部有效计划计算统一时间轴起止点、时长和刻度。
const scheduleWindow = computed(() => {
  // 找出所有排期的最早开始和最晚结束，生成统一时间轴范围及7个刻度。
  if (!scheduleRows.value.length) {
    return { start: null, end: null, duration: 0, ticks: [] }
  }

  const earliest = Math.min(...scheduleRows.value.map((item) => dayjs(item.plannedStartTime).valueOf()))
  const latest = Math.max(...scheduleRows.value.map((item) => dayjs(item.plannedEndTime).valueOf()))
  const start = dayjs(earliest).startOf('hour')
  let end = dayjs(latest).add(1, 'hour').startOf('hour')
  if (end.diff(start, 'hour', true) < 8) {
    end = start.add(8, 'hour')
  }
  const duration = end.valueOf() - start.valueOf()
  const tickFormat = duration <= 36 * 60 * 60 * 1000 ? 'HH:mm' : 'MM-DD HH:mm'
  const ticks = Array.from({ length: 7 }, (_, index) => {
    const ratio = index / 6
    return {
      label: start.add(duration * ratio, 'millisecond').format(tickFormat),
      left: `${ratio * 100}%`
    }
  })
  return { start, end, duration, ticks }
})
// 将时间轴起止点格式化为弹窗顶部的可读范围文字。
const scheduleRangeLabel = computed(() => {
  const { start, end } = scheduleWindow.value
  if (!start || !end) return '暂无有效排期'
  if (start.isSame(end, 'day')) {
    return `${start.format('MM月DD日 HH:mm')} - ${end.format('HH:mm')}`
  }
  return `${start.format('MM-DD HH:mm')} - ${end.format('MM-DD HH:mm')}`
})

// 将原始统计组织成模板可循环渲染的KPI、生产状态、设备状态和风险项结构。
const kpis = computed(() => [
  {
    label: '生产订单',
    value: formatNumber(summary.value.productionOrderCount),
    unit: '单',
    description: '当前生产订单台账',
    icon: 'clipboard',
    tone: 'blue'
  },
  {
    label: '生产工单',
    value: formatNumber(summary.value.workOrderCount),
    unit: '单',
    description: '当前生产工单台账',
    icon: 'package',
    tone: 'amber'
  },
  {
    label: '今日报工产量',
    value: formatNumber(todayOutput.value),
    unit: '件',
    description: `合格 ${formatNumber(todayQualified.value)} / 不良 ${formatNumber(todayDefect.value)}`,
    icon: 'factory',
    tone: 'cyan'
  },
  {
    label: '今日报工合格率',
    value: formatPercent(reportPassRate.value),
    unit: reportPassRate.value === null ? '' : '%',
    description: '按今日报工数量计算',
    icon: 'shield-check',
    tone: 'green'
  },
  {
    label: '运行设备',
    value: formatNumber(equipment.value.running),
    unit: '台',
    description: `设备总计 ${formatNumber(equipmentTotal.value)} 台`,
    icon: 'wrench',
    tone: 'steel'
  },
  {
    label: '逾期生产计划',
    value: formatNumber(overduePlanCount.value),
    unit: '项',
    description: overduePlanCount.value > 0 ? '已超过计划结束时间' : '当前没有逾期计划',
    icon: 'alert',
    tone: overduePlanCount.value > 0 ? 'red' : 'muted'
  }
])

// 把生产订单、工单和执行任务整理为统一统计卡数据。
const productionStats = computed(() => [
  {
    label: '当前生产订单',
    value: Number(summary.value.productionOrderCount || 0),
    note: '系统内未删除的订单总数',
    icon: 'clipboard'
  },
  {
    label: '当前生产工单',
    value: Number(summary.value.workOrderCount || 0),
    note: '系统内未删除的工单总数',
    icon: 'package'
  },
  {
    label: '正在执行任务',
    value: Number(summary.value.runningTaskCount || 0),
    note: '当前处于生产中的任务',
    icon: 'activity'
  }
])

// 把设备状态对象转换为模板可循环的五类状态数组。
const equipmentItems = computed(() => [
  { key: 'running', label: '运行', value: Number(equipment.value.running || 0), tone: 'green' },
  { key: 'idle', label: '待机', value: Number(equipment.value.idle || 0), tone: 'blue' },
  { key: 'stopped', label: '停机', value: Number(equipment.value.stopped || 0), tone: 'gray' },
  { key: 'maintenance', label: '维保', value: Number(equipment.value.maintenance || 0), tone: 'amber' },
  { key: 'fault', label: '故障', value: Number(equipment.value.fault || 0), tone: 'red' }
])

// 将计划、设备、库存和不良数据整理为可点击的风险关注事项。
const attentionItems = computed(() => [
  {
    label: '逾期生产计划',
    value: overduePlanCount.value,
    note: overduePlanCount.value > 0 ? '需确认执行、完工或调整计划' : '当前没有逾期生产计划',
    icon: 'calendar',
    tone: overduePlanCount.value > 0 ? 'danger' : 'normal',
    path: '/mfg/production-plans'
  },
  {
    label: '故障设备',
    value: faultCount.value,
    note: faultCount.value > 0 ? '需要设备人员优先处理' : '当前没有故障设备',
    icon: 'wrench',
    tone: faultCount.value > 0 ? 'danger' : 'normal',
    path: '/eam/equipment'
  },
  {
    label: '补货预警',
    value: warningCount.value,
    note: warningCount.value > 0 ? '需要仓储人员确认补货' : '当前没有待处理补货建议',
    icon: 'warehouse',
    tone: warningCount.value > 0 ? 'warning' : 'normal',
    path: '/wms/replenishment'
  },
  {
    label: '今日不良报工',
    value: todayDefect.value,
    note: todayDefect.value > 0 ? '建议进入报工明细复核原因' : '今日暂无不良报工记录',
    icon: 'alert',
    tone: todayDefect.value > 0 ? 'warning' : 'normal',
    path: '/mfg/work-reports'
  }
])

// 生产趋势图同时使用柱状图展示产量、折线图展示合格率，并使用双Y轴。
const trendOption = computed(() => ({
  animationDuration: 500,
  color: ['#559dd2', '#4bc58a'],
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    backgroundColor: 'rgb(8 22 39 / 98%)',
    borderColor: '#31526d',
    padding: [10, 12],
    textStyle: { color: '#f2f7fb', fontSize: 16 }
  },
  legend: {
    top: 4,
    right: 10,
    itemWidth: 18,
    itemHeight: 10,
    textStyle: { color: '#a8bac9', fontSize: 16 },
    data: ['报工产量', '报工合格率']
  },
  grid: { top: 62, right: 72, bottom: 42, left: 76 },
  xAxis: {
    type: 'category',
    data: trendRows.value.map((item) => dayjs(item.date).format('MM-DD')),
    axisLine: { lineStyle: { color: '#35516a' } },
    axisTick: { show: false },
    axisLabel: { color: '#9fb1c0', fontSize: 16, margin: 14 }
  },
  yAxis: [
    {
      type: 'value',
      name: '产量（件）',
      minInterval: 1,
      nameTextStyle: { color: '#8fa4b5', fontSize: 15, padding: [0, 0, 4, 0] },
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#8fa4b5', fontSize: 15 },
      splitLine: { lineStyle: { color: 'rgb(99 132 158 / 18%)', type: 'dashed' } }
    },
    {
      type: 'value',
      name: '合格率（%）',
      min: 0,
      max: 100,
      interval: 25,
      nameTextStyle: { color: '#8fa4b5', fontSize: 15, padding: [0, 0, 4, 0] },
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#8fa4b5', fontSize: 15, formatter: '{value}%' },
      splitLine: { show: false }
    }
  ],
  series: [
    {
      name: '报工产量',
      type: 'bar',
      barMaxWidth: 44,
      itemStyle: { borderRadius: [4, 4, 0, 0] },
      tooltip: { valueFormatter: (value) => `${formatNumber(value)} 件` },
      data: trendRows.value.map((item) => item.output)
    },
    {
      name: '报工合格率',
      type: 'line',
      yAxisIndex: 1,
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { width: 3 },
      tooltip: { valueFormatter: (value) => value === null ? '--' : `${Number(value).toFixed(1)}%` },
      data: trendRows.value.map((item) => item.passRate)
    }
  ]
}))

// 将数量转换为中文千分位格式，并最多保留一位小数。
function formatNumber(value) {
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 1 }).format(Number(value || 0))
}

// 将合格率格式化为一位小数，空值显示占位符。
function formatPercent(value) {
  return value === null || value === undefined ? '--' : Number(value).toFixed(1)
}

// 从通用接口响应中兼容提取数组或分页records。
function pageRecords(result) {
  // 兼容后端直接返回数组和MyBatis-Plus分页对象两种数据结构。
  const data = result?.data
  return Array.isArray(data) ? data : (data?.records || [])
}

// 把排期起止时间换算成时间轴中的left和width百分比。
function scheduleBarStyle(item) {
  // 把计划起止时间换算为展开排期弹窗中的百分比位置和宽度。
  const { start, duration } = scheduleWindow.value
  if (!start || !duration) return { left: '0%', width: '0%' }
  const itemStart = dayjs(item.plannedStartTime).valueOf()
  const itemEnd = dayjs(item.plannedEndTime).valueOf()
  const left = Math.max(0, Math.min(96, ((itemStart - start.valueOf()) / duration) * 100))
  const naturalWidth = Math.max(0, ((itemEnd - itemStart) / duration) * 100)
  const width = Math.max(4, Math.min(100 - left, naturalWidth))
  return { left: `${left}%`, width: `${width}%` }
}

// 拼接排期块的悬停提示，包含订单、产品、产线、时间、数量和状态。
function scheduleBarTitle(item) {
  return [
    item.orderNo,
    item.productName,
    item.lineName,
    `${dayjs(item.plannedStartTime).format('MM-DD HH:mm')} - ${dayjs(item.plannedEndTime).format('MM-DD HH:mm')}`,
    `${formatNumber(item.plannedQuantity)} 件`,
    item.statusLabel
  ].join(' · ')
}

// 格式化排期时间范围，同一天时省略重复结束日期。
function formatScheduleTime(item) {
  const start = dayjs(item.plannedStartTime)
  const end = dayjs(item.plannedEndTime)
  if (!start.isValid() || !end.isValid()) return '--'
  if (start.isSame(end, 'day')) {
    return `${start.format('MM-DD HH:mm')} - ${end.format('HH:mm')}`
  }
  return `${start.format('MM-DD HH:mm')} - ${end.format('MM-DD HH:mm')}`
}

// 并行查询计划、订单、产线和产品，再把关联ID补充为可读名称。
async function loadProductionSchedule() {
  // 计划表只存关联ID，因此并行查询订单、产线、产品，再用Map补齐可读名称。
  const [planResult, orderResult, lineResult, stationResult, productResult] = await Promise.allSettled([
    getResourcePage('/api/mfg/production-plan', { current: 1, size: 200 }),
    getResourcePage('/api/mfg/production-order', { current: 1, size: 200 }),
    getResourcePage('/api/factory/line', { current: 1, size: 200 }),
    getResourcePage('/api/factory/station', { current: 1, size: 500 }),
    getResourcePage('/api/mdm/product', { current: 1, size: 200 })
  ])
  if (planResult.status === 'rejected') {
    throw planResult.reason
  }

  const optionalRecords = (result) => result.status === 'fulfilled' ? pageRecords(result.value) : []
  const plans = pageRecords(planResult.value)
  const orders = new Map(optionalRecords(orderResult).map((item) => [String(item.productionOrderId), item]))
  const lines = new Map(optionalRecords(lineResult).map((item) => [String(item.lineId), item]))
  const stationCounts = new Map()
  optionalRecords(stationResult).forEach((station) => {
    const key = String(station.lineId)
    stationCounts.set(key, (stationCounts.get(key) || 0) + 1)
  })
  const products = new Map(optionalRecords(productResult).map((item) => [String(item.productId), item]))

  const mappedPlans = plans.map((plan) => {
    const order = orders.get(String(plan.productionOrderId)) || {}
    const line = lines.get(String(plan.lineId)) || {}
    const product = products.get(String(order.productId)) || {}
    const status = scheduleStatusMeta[plan.status] || { label: plan.status || '未知状态', tone: 'closed' }
    return {
      ...plan,
      orderNo: order.productionOrderNo || `订单 #${plan.productionOrderId || '--'}`,
      productName: product.productName || `产品 #${order.productId || '--'}`,
      lineName: line.lineName || `产线 #${plan.lineId || '--'}`,
      statusLabel: status.label,
      statusTone: status.tone
    }
  })
  const mappedLines = [...lines.values()].map((line) => ({
    ...line,
    stationCount: stationResult.status === 'fulfilled' ? (stationCounts.get(String(line.lineId)) || 0) : null
  }))
  const fallbackLines = [...new Map(mappedPlans.map((plan) => [String(plan.lineId), {
    lineId: plan.lineId,
    lineCode: '',
    lineName: plan.lineName,
    dailyCapacity: 0,
    status: ''
  }])).values()]
  return { plans: mappedPlans, lines: mappedLines.length ? mappedLines : fallbackLines }
}

// 读取近7日不合格处理单并计算真实关闭率；权限不足时由主加载流程保留一次检验率回退。
async function loadQualityClosure() {
  const result = await getResourcePage('/api/qms/nonconforming-order', { current: 1, size: 200 })
  const start = now.value.subtract(6, 'day').startOf('day')
  const end = now.value.endOf('day')
  const records = pageRecords(result).filter((item) => {
    const createdTime = dayjs(item.createdTime)
    return createdTime.isValid() && !createdTime.isBefore(start) && !createdTime.isAfter(end)
  })
  const closed = records.filter((item) => item.status === 'CLOSED').length
  return {
    total: records.length,
    closed,
    rate: records.length ? Number(((closed / records.length) * 100).toFixed(2)) : null
  }
}

// 打开大屏中的生产排期展开弹窗。
function openSchedule() {
  scheduleExpanded.value = true
}

// 关闭生产排期展开弹窗。
function closeSchedule() {
  scheduleExpanded.value = false
}

// 关闭弹窗后跳转到完整生产计划管理页面。
async function openProductionPlans() {
  closeSchedule()
  await openModule('/mfg/production-plans')
}

// 用户按Escape时只关闭展开排期，不直接退出大屏。
function handleScreenKeydown(event) {
  // 排期弹窗打开时，Escape只关闭弹窗而不退出整个大屏。
  if (event.key === 'Escape' && scheduleExpanded.value) {
    closeSchedule()
  }
}

// 根据实际视口计算1920x1080固定画布的等比缩放和居中偏移。
function updateScale() {
  // 以1920x1080为逻辑画布，取宽高缩放比中的较小值，确保内容完整显示。
  const viewport = window.visualViewport
  const viewportWidth = viewport?.width || window.innerWidth
  const viewportHeight = viewport?.height || window.innerHeight
  const nextScale = Math.min(viewportWidth / 1920, viewportHeight / 1080)
  scale.value = nextScale
  offsetX.value = Math.max(0, (viewportWidth - 1920 * nextScale) / 2)
  offsetY.value = Math.max(0, (viewportHeight - 1080 * nextScale) / 2)
}

// 浏览器全屏状态变化后同步按钮状态并重新计算缩放。
function handleFullscreenChange() {
  fullscreenActive.value = Boolean(document.fullscreenElement)
  updateScale()
}

// 调用浏览器Fullscreen API进入或退出全屏。
async function toggleFullscreen() {
  // 使用浏览器Fullscreen API切换全屏，并在动画结束后再次校准缩放。
  if (!document.fullscreenElement) {
    await document.documentElement.requestFullscreen?.()
  } else {
    await document.exitFullscreen?.()
  }
  window.setTimeout(updateScale, 120)
}

// 退出大屏并返回普通首页工作台。
async function exitBigScreen() {
  await openModule('/dashboard')
}

// 统一封装大屏各业务入口的Vue Router跳转。
async function openModule(path) {
  await router.push(path)
}

// 并行刷新生产、趋势、质量、设备和排期数据，并保留成功接口的结果。
async function loadData() {
  // 防止定时刷新和手工刷新重叠发送；预览模式直接使用本地演示数据。
  if (loading.value) return

  if (previewMode) {
    const data = createDashboardPreview()
    summary.value = data.summary
    trend.value = data.trend
    quality.value = data.quality
    qualityClosure.value = data.qualityClosure || { total: 0, closed: 0, rate: null }
    equipment.value = data.equipment
    schedulePlans.value = data.schedule || []
    productionLines.value = data.lines || [...new Map((data.schedule || []).map((item) => [String(item.lineId), {
      lineId: item.lineId,
      lineCode: `LINE-${item.lineId}`,
      lineName: item.lineName,
      dailyCapacity: 0,
      status: 'RUNNING'
    }])).values()]
    lastUpdated.value = dayjs()
    loadError.value = ''
    scheduleError.value = ''
    failedDataCount.value = 0
    return
  }

  loading.value = true
  loadError.value = ''
  scheduleError.value = ''
  const params = {
    beginDate: dayjs().subtract(6, 'day').format('YYYY-MM-DD'),
    endDate: dayjs().format('YYYY-MM-DD')
  }

  try {
    // allSettled允许单块接口失败时继续保留并展示其他成功数据，而不是整屏清空。
    const results = await Promise.allSettled([
      getDashboardSummary(),
      getProductionTrend(params),
      getQualityStatistics(params),
      getEquipmentStatus(),
      loadProductionSchedule(),
      loadQualityClosure()
    ])
    const dashboardResults = results.slice(0, 4)
    const scheduleResult = results[4]
    const qualityClosureResult = results[5]
    const targets = [summary, trend, quality, equipment]
    const labels = ['生产汇总', '生产趋势', '质量统计', '设备状态']
    const failedLabels = []
    let successCount = 0

    dashboardResults.forEach((result, index) => {
      if (result.status === 'fulfilled') {
        targets[index].value = result.value.data ?? (index === 1 ? [] : {})
        successCount += 1
      } else {
        failedLabels.push(labels[index])
      }
    })

    if (scheduleResult.status === 'fulfilled') {
      schedulePlans.value = scheduleResult.value.plans
      productionLines.value = scheduleResult.value.lines
    } else {
      scheduleError.value = scheduleResult.reason?.displayMessage || scheduleResult.reason?.message || '生产排期暂未更新'
    }

    qualityClosure.value = qualityClosureResult.status === 'fulfilled'
      ? qualityClosureResult.value
      : { total: 0, closed: 0, rate: null }

    failedDataCount.value = failedLabels.length
    if (successCount > 0) {
      lastUpdated.value = dayjs()
    }
    if (failedLabels.length === 4) {
      loadError.value = lastUpdated.value
        ? '看板数据连接失败，当前保留上次成功数据'
        : '看板数据连接失败，请检查服务后重新加载'
    } else if (failedLabels.length > 0) {
      loadError.value = `${failedLabels.join('、')}暂未更新，其他数据已同步`
    }
  } catch (error) {
    failedDataCount.value = 4
    loadError.value = error.displayMessage || error.message || '大屏数据加载失败'
  } finally {
    loading.value = false
  }
}

// 挂载后首次适配和加载，并启动时钟、数据轮询及尺寸事件监听。
onMounted(() => {
  // 首次加载后启动时钟、业务轮询，并监听窗口/全屏/视觉视口尺寸变化。
  updateScale()
  loadData()
  clockTimer = window.setInterval(() => { now.value = dayjs() }, 1000)
  refreshTimer = window.setInterval(loadData, 60000)
  window.addEventListener('resize', updateScale)
  window.addEventListener('keydown', handleScreenKeydown)
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  window.visualViewport?.addEventListener('resize', updateScale)
})

// 卸载时清除定时器和所有事件监听，防止离开大屏后继续运行。
onBeforeUnmount(() => {
  // 清除全部定时器和事件监听，避免退出大屏后后台继续请求和计算。
  window.clearInterval(clockTimer)
  window.clearInterval(refreshTimer)
  window.removeEventListener('resize', updateScale)
  window.removeEventListener('keydown', handleScreenKeydown)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  window.visualViewport?.removeEventListener('resize', updateScale)
})
</script>

<template>
  <!-- stage内部始终按1920x1080布局，外层页面只负责缩放；各数据区由computed结构循环渲染。 -->
  <div class="big-screen-page">
    <div class="big-screen-stage" :style="stageStyle">
      <header class="screen-header">
        <div class="header-branding">
          <button class="icon-button back-button" type="button" title="返回工作台" aria-label="返回工作台" @click="exitBigScreen">
            <AppIcon name="left" :size="24" />
          </button>
          <div>
            <strong>羽球智造 MES</strong>
            <span>生产运营总览</span>
          </div>
        </div>

        <div class="header-title">
          <strong>智能车间生产管控大屏</strong>
          <span>PRODUCTION CONTROL CENTER</span>
        </div>

        <div class="header-actions">
          <div class="update-status" :class="{ 'has-error': loadError, 'is-preview': previewMode }" role="status" aria-live="polite">
            <i />
            <span>{{ syncText }}</span>
            <b>{{ updateText }}</b>
          </div>
          <div class="header-clock">
            <span>{{ dateText }}</span>
            <span>{{ weekdayText }}</span>
            <b>{{ timeText }}</b>
          </div>
          <button class="icon-button" type="button" title="刷新数据" aria-label="刷新数据" :class="{ rotating: loading }" @click="loadData">
            <AppIcon name="refresh" :size="21" />
          </button>
          <button
            class="icon-button"
            type="button"
            :title="fullscreenActive ? '退出全屏' : '全屏显示'"
            :aria-label="fullscreenActive ? '退出全屏' : '全屏显示'"
            @click="toggleFullscreen"
          >
            <AppIcon name="fullscreen" :size="22" />
          </button>
          <button class="exit-button" type="button" @click="exitBigScreen">
            <AppIcon name="logout" :size="19" />
            <span>退出大屏</span>
          </button>
        </div>
      </header>

      <main class="screen-shell">
        <div v-if="loadError" class="screen-error" role="alert">
          <AppIcon name="alert" :size="18" />
          <span>{{ loadError }}</span>
          <button type="button" @click="loadData">重新加载</button>
        </div>

        <section class="kpi-strip" aria-label="生产关键指标">
          <article v-for="kpi in kpis" :key="kpi.label" class="kpi-card" :class="`is-${kpi.tone}`">
            <span class="kpi-icon"><AppIcon :name="kpi.icon" :size="34" :stroke-width="2.1" /></span>
            <div class="kpi-copy">
              <span>{{ kpi.label }}</span>
              <strong>{{ kpi.value }}<small>{{ kpi.unit }}</small></strong>
              <p>{{ kpi.description }}</p>
            </div>
          </article>
        </section>

        <section class="primary-grid">
          <article class="screen-panel schedule-panel">
            <header class="panel-header">
              <div>
                <strong>生产计划排期</strong>
                <span>按产线汇总订单计划与执行窗口</span>
              </div>
              <aside class="panel-tools">
                <span v-if="overduePlanCount" class="overdue-badge"><AppIcon name="alert" :size="14" />{{ overduePlanCount }} 项逾期</span>
                <span class="schedule-range">{{ scheduleRangeLabel }}</span>
                <button class="panel-link" type="button" title="放大生产计划排期" aria-label="放大生产计划排期" @click="openSchedule">
                  <AppIcon name="fullscreen" :size="19" />
                </button>
                <button class="panel-link" type="button" title="打开生产计划管理" aria-label="打开生产计划管理" @click="openProductionPlans">
                  <AppIcon name="arrow" :size="20" />
                </button>
              </aside>
            </header>
            <div
              class="schedule-board"
              role="button"
              tabindex="0"
              aria-label="点击放大生产计划排期"
              @click="openSchedule"
              @keydown.enter="openSchedule"
              @keydown.space.prevent="openSchedule"
            >
              <div class="schedule-axis">
                <span>产线 / 计划</span>
                <div class="schedule-axis-track">
                  <i v-for="tick in scheduleWindow.ticks" :key="`${tick.left}-${tick.label}`" :style="{ left: tick.left }">
                    <b>{{ tick.label }}</b>
                  </i>
                </div>
              </div>
              <div v-if="visibleScheduleLineRows.length" class="schedule-rows">
                <div v-for="line in visibleScheduleLineRows" :key="line.lineId || line.lineName" class="schedule-row">
                  <div class="schedule-row-meta">
                    <strong>{{ line.lineName }}</strong>
                    <span :class="{ 'has-overdue': line.plans.some((plan) => plan.statusTone === 'overdue') }">
                      {{ line.plans.length }} 项计划<span v-if="line.plans.some((plan) => plan.statusTone === 'overdue')"> / {{ line.plans.filter((plan) => plan.statusTone === 'overdue').length }} 项逾期</span> · 日产能 {{ formatNumber(line.dailyCapacity) }}
                    </span>
                  </div>
                  <div class="schedule-row-track">
                    <span v-if="!line.plans.length" class="schedule-row-empty">暂无有效计划</span>
                    <span
                      v-for="item in line.plans"
                      :key="item.planId"
                      class="schedule-bar"
                      :class="`is-${item.statusTone}`"
                      :style="scheduleBarStyle(item)"
                      :title="scheduleBarTitle(item)"
                    >
                      <b>{{ item.orderNo }}</b>
                      <small>{{ item.productName }} · {{ formatNumber(item.plannedQuantity) }} 件</small>
                    </span>
                  </div>
                </div>
              </div>
              <div v-else-if="scheduleError" class="schedule-empty is-error">
                <AppIcon name="alert" :size="28" />
                <strong>排期数据暂未更新</strong>
                <span>{{ scheduleError }}</span>
              </div>
              <div v-else class="schedule-empty">
                <AppIcon name="calendar" :size="30" />
                <strong>暂无有效生产排期</strong>
                <span>请先为生产订单建立包含开始、结束时间和产线的生产计划</span>
              </div>
              <div v-if="scheduleError && visibleScheduleLineRows.length" class="schedule-stale"><AppIcon name="alert" :size="14" />当前保留上次成功排期</div>
              <div class="schedule-expand-hint"><AppIcon name="fullscreen" :size="16" />点击放大查看完整排期</div>
            </div>
          </article>

          <article class="screen-panel line-status-panel">
            <header class="panel-header">
              <div>
                <strong>产线运行状态</strong>
                <span>真实产线、日产能与当前排期</span>
              </div>
              <aside class="panel-tools">
                <strong class="line-count">{{ lineStatusRows.length }}<small>条</small></strong>
                <button class="panel-link" type="button" title="打开产线管理" aria-label="打开产线管理" @click="openModule('/factory/lines')">
                  <AppIcon name="arrow" :size="20" />
                </button>
              </aside>
            </header>
            <div v-if="lineStatusRows.length" class="line-status-list">
              <article v-for="line in lineStatusRows" :key="line.lineId" class="line-status-row" :class="`is-${line.statusTone}`">
                <span class="line-status-icon"><AppIcon name="factory" :size="22" /></span>
                <div class="line-status-copy">
                  <div><strong>{{ line.lineName }}</strong><span><i />{{ line.statusLabel }}</span></div>
                  <p v-if="line.focusPlan">
                    {{ line.focusLabel }}：{{ line.focusPlan.orderNo }} · {{ formatNumber(line.focusPlan.plannedQuantity) }} 件
                  </p>
                  <p v-else>当前没有有效生产计划</p>
                  <small>日产能 {{ formatNumber(line.dailyCapacity) }} · 排期 {{ line.planCount }} 项 / {{ formatNumber(line.scheduledQuantity) }} 件</small>
                </div>
              </article>
            </div>
            <div v-else class="line-status-empty">
              <AppIcon name="factory" :size="30" />
              <strong>暂无产线数据</strong>
              <span>请先维护工厂产线与运行状态</span>
            </div>
          </article>

          <article class="screen-panel execution-panel">
            <header class="panel-header">
              <div>
                <strong>今日生产情况</strong>
                <span>今日报工结果与当前生产台账</span>
              </div>
              <aside class="panel-tools">
                <span class="live-badge"><i />60 秒刷新</span>
                <button class="panel-link" type="button" title="打开生产报工明细" aria-label="打开生产报工明细" @click="openModule('/mfg/work-reports')">
                  <AppIcon name="arrow" :size="20" />
                </button>
              </aside>
            </header>

            <div class="output-focus">
              <span>今日报工总产量</span>
              <strong>{{ formatNumber(todayOutput) }}<small>件</small></strong>
              <em v-if="reportPassRate !== null">合格率 {{ formatPercent(reportPassRate) }}%</em>
              <em v-else class="is-empty">暂无今日报工</em>
            </div>

            <div class="output-breakdown">
              <div class="is-qualified">
                <span>合格产量</span>
                <b>{{ formatNumber(todayQualified) }}</b>
              </div>
              <div class="is-defect">
                <span>不良产量</span>
                <b>{{ formatNumber(todayDefect) }}</b>
              </div>
            </div>

            <div class="production-stat-list">
              <div v-for="item in productionStats" :key="item.label" class="production-stat-row">
                <span><AppIcon :name="item.icon" :size="20" /></span>
                <div><b>{{ item.label }}</b><small>{{ item.note }}</small></div>
                <strong>{{ formatNumber(item.value) }}</strong>
              </div>
            </div>
          </article>
        </section>

        <section class="secondary-grid">
          <article class="screen-panel trend-summary-panel">
            <header class="panel-header compact">
              <div>
                <strong>近 7 日生产趋势</strong>
                <span>报工总量与报工合格率</span>
              </div>
              <aside class="panel-tools">
                <div class="panel-total compact-total">
                  <span>累计</span>
                  <b>{{ formatNumber(sevenDayOutput) }} 件</b>
                </div>
                <button class="panel-link" type="button" title="打开生产报工明细" aria-label="打开生产报工明细" @click="openModule('/mfg/work-reports')">
                  <AppIcon name="arrow" :size="20" />
                </button>
              </aside>
            </header>
            <div class="trend-chart-wrap compact-chart">
              <MesChart :option="trendOption" height="205px" />
              <div v-if="!hasTrendData" class="empty-overlay compact-empty">
                <AppIcon name="activity" :size="24" />
                <strong>近 7 日暂无报工</strong>
              </div>
            </div>
          </article>

          <article class="screen-panel quality-panel">
            <header class="panel-header compact">
              <div>
                <strong>近 7 日质量情况</strong>
                <span>质检结果与不合格处理进度</span>
              </div>
              <button class="panel-link" type="button" title="打开检验单明细" aria-label="打开检验单明细" @click="openModule('/qms/inspection-orders')">
                <AppIcon name="arrow" :size="20" />
              </button>
            </header>
            <div class="quality-main">
              <div class="quality-dial-stack">
                <strong class="quality-focus-title">{{ qualityFocusLabel }}</strong>
                <div
                  class="quality-dial"
                  :class="{ 'is-empty': qualityFocusRate === null }"
                  :style="qualityDialStyle"
                  :title="qualityClosure.rate === null ? '合格质检单数 ÷ 已判定质检单数' : '已完成处理的不合格单数 ÷ 需要处理的不合格单数'"
                >
                  <div>
                    <strong>{{ formatPercent(qualityFocusRate) }}<small v-if="qualityFocusRate !== null">%</small></strong>
                  </div>
                </div>
                <small v-if="qualityClosure.rate !== null" class="quality-dial-summary">{{ qualityClosure.closed }}/{{ qualityClosure.total }} 已完成</small>
                <small v-else-if="evaluatedInspectionCount" class="quality-dial-summary">{{ qualifiedInspectionCount }}/{{ evaluatedInspectionCount }} 已判定合格</small>
              </div>
              <div class="quality-counts">
                <div><span>质检单</span><b>{{ formatNumber(inspectionCount) }}</b></div>
                <div><span>已判定</span><b>{{ formatNumber(evaluatedInspectionCount) }}</b></div>
                <div><span>检验合格</span><b>{{ formatNumber(qualifiedInspectionCount) }}</b></div>
                <div><span>检验不合格</span><b>{{ formatNumber(unqualifiedInspectionCount) }}</b></div>
                <div><span>待判定</span><b>{{ formatNumber(pendingInspectionCount) }}</b></div>
              </div>
            </div>
          </article>

          <article class="screen-panel equipment-panel">
            <header class="panel-header compact">
              <div>
                <strong>设备关键状态</strong>
                <span>当前运行、停机、维保与故障设备</span>
              </div>
              <aside class="panel-tools">
                <strong class="equipment-total">{{ formatNumber(equipmentTotal) }}<small>台</small></strong>
                <button class="panel-link" type="button" title="打开设备台账" aria-label="打开设备台账" @click="openModule('/eam/equipment')">
                  <AppIcon name="arrow" :size="20" />
                </button>
              </aside>
            </header>
            <div class="equipment-body">
              <div class="equipment-bar" :class="{ 'is-empty': equipmentTotal === 0 }">
                <i
                  v-for="item in equipmentItems"
                  :key="item.key"
                  :class="`is-${item.tone}`"
                  :style="{ width: equipmentTotal ? `${(item.value / equipmentTotal) * 100}%` : '0%' }"
                />
              </div>
              <div class="equipment-grid">
                <div v-for="item in equipmentItems" :key="item.key">
                  <i :class="`is-${item.tone}`" />
                  <span>{{ item.label }}</span>
                  <b>{{ formatNumber(item.value) }}</b>
                </div>
              </div>
              <p v-if="equipmentTotal === 0" class="inline-empty">暂无设备台账数据</p>
            </div>
          </article>

          <article class="screen-panel attention-panel">
            <header class="panel-header compact">
              <div>
                <strong>需关注事项</strong>
                <span>计划、设备、仓储与质量异常</span>
              </div>
              <strong class="attention-total" :class="{ 'has-warning': riskCategoryCount > 0 }">{{ riskCategoryCount }}<small>类关注</small></strong>
            </header>
            <div class="attention-list">
              <button
                v-for="item in attentionItems"
                :key="item.label"
                class="attention-row"
                :class="`is-${item.tone}`"
                type="button"
                :title="`打开${item.label}明细`"
                :aria-label="`打开${item.label}明细，当前数量 ${formatNumber(item.value)}`"
                @click="openModule(item.path)"
              >
                <span class="attention-icon"><AppIcon :name="item.icon" :size="21" /></span>
                <span class="attention-copy"><b>{{ item.label }}</b><small>{{ item.note }}</small></span>
                <span class="attention-value"><strong>{{ formatNumber(item.value) }}</strong><AppIcon name="right" :size="18" /></span>
              </button>
            </div>
          </article>
        </section>
      </main>

      <div v-if="scheduleExpanded" class="schedule-modal-backdrop" @click.self="closeSchedule">
        <section class="schedule-modal" :style="scheduleModalStyle" role="dialog" aria-modal="true" aria-labelledby="schedule-modal-title">
          <header class="schedule-modal-header">
            <div>
              <span>PRODUCTION SCHEDULE</span>
              <strong id="schedule-modal-title">生产计划排期总览</strong>
              <p>{{ previewMode ? '演示排期' : '真实生产计划' }} · {{ scheduleRangeLabel }}</p>
            </div>
            <aside>
              <button class="modal-command" type="button" @click="openProductionPlans">
                <AppIcon name="calendar" :size="19" />
                <span>生产计划管理</span>
              </button>
              <button class="modal-close" type="button" title="关闭排期总览" aria-label="关闭排期总览" @click="closeSchedule">
                <AppIcon name="close" :size="22" />
              </button>
            </aside>
          </header>

          <div class="schedule-board expanded-board">
            <div class="schedule-axis">
              <span>产线 / 生产计划</span>
              <div class="schedule-axis-track">
                <i v-for="tick in scheduleWindow.ticks" :key="`expanded-${tick.left}-${tick.label}`" :style="{ left: tick.left }">
                  <b>{{ tick.label }}</b>
                </i>
              </div>
            </div>
            <div v-if="expandedScheduleLineRows.length" class="schedule-rows">
              <div v-for="line in expandedScheduleLineRows" :key="`expanded-${line.lineId || line.lineName}`" class="schedule-row">
                <div class="schedule-row-meta">
                  <strong>{{ line.lineName }}</strong>
                  <span :class="{ 'has-overdue': line.plans.some((plan) => plan.statusTone === 'overdue') }">
                    {{ line.plans.length }} 项计划<span v-if="line.plans.some((plan) => plan.statusTone === 'overdue')"> / {{ line.plans.filter((plan) => plan.statusTone === 'overdue').length }} 项逾期</span> · 日产能 {{ formatNumber(line.dailyCapacity) }}
                  </span>
                  <small>{{ line.plans.map(formatScheduleTime).join(' / ') }}</small>
                </div>
                <div class="schedule-row-track">
                  <span v-if="!line.plans.length" class="schedule-row-empty">暂无有效计划</span>
                  <span
                    v-for="item in line.plans"
                    :key="`expanded-${item.planId}`"
                    class="schedule-bar"
                    :class="`is-${item.statusTone}`"
                    :style="scheduleBarStyle(item)"
                    :title="scheduleBarTitle(item)"
                  >
                    <b>{{ item.orderNo }}</b>
                    <small>{{ item.productName }} · {{ formatNumber(item.plannedQuantity) }} 件</small>
                  </span>
                </div>
              </div>
            </div>
            <div v-else-if="scheduleError" class="schedule-empty is-error">
              <AppIcon name="alert" :size="34" />
              <strong>排期数据暂未更新</strong>
              <span>{{ scheduleError }}</span>
            </div>
            <div v-else class="schedule-empty">
              <AppIcon name="calendar" :size="36" />
              <strong>暂无有效生产排期</strong>
              <span>请在生产计划中补齐订单、产线、计划开始和计划结束时间</span>
            </div>
          </div>

          <footer class="schedule-modal-footer">
            <span><i class="is-draft" />待生效</span>
            <span><i class="is-released" />待执行</span>
            <span><i class="is-running" />生产中</span>
            <span><i class="is-finished" />已完成</span>
            <span><i class="is-overdue" />已逾期</span>
            <b>显示 {{ expandedScheduleLineRows.length }} 条产线 / {{ scheduleRows.length }} 项有效计划</b>
          </footer>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 生产大屏样式：固定1920x1080画布，并控制KPI、图表、排期弹窗和全屏状态。 */
.big-screen-page {
  position: fixed;
  inset: 0;
  overflow: hidden;
  color: #edf4fa;
  background:
    linear-gradient(rgb(61 100 130 / 7%) 1px, transparent 1px),
    linear-gradient(90deg, rgb(61 100 130 / 7%) 1px, transparent 1px),
    linear-gradient(180deg, #06111c 0%, #071521 50%, #050e17 100%);
  background-size: 72px 72px, 72px 72px, auto;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
  letter-spacing: 0;
}

.big-screen-page::after {
  position: absolute;
  inset: 0;
  border: 2px solid rgb(81 145 186 / 20%);
  box-shadow: inset 0 0 140px rgb(0 0 0 / 30%);
  content: '';
  pointer-events: none;
}

.big-screen-stage {
  position: absolute;
  top: 0;
  left: 0;
  width: 1920px;
  height: 1080px;
  overflow: hidden;
  background: linear-gradient(180deg, rgb(5 16 28 / 91%), rgb(6 17 28 / 96%));
  font-variant-numeric: tabular-nums;
  transform-origin: top left;
}

.screen-header {
  display: grid;
  height: 90px;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 0 30px;
  border-bottom: 1px solid #27506a;
  background: rgb(6 19 32 / 97%);
  box-shadow: 0 10px 30px rgb(0 0 0 / 22%), inset 0 -1px 0 rgb(69 163 201 / 12%);
}

.header-branding,
.header-actions,
.header-clock,
.update-status {
  display: flex;
  align-items: center;
}

.header-branding { gap: 16px; }
.header-branding > div { display: grid; gap: 4px; }
.header-branding strong { color: #f3f8fc; font-size: 21px; font-weight: 720; line-height: 1.25; }
.header-branding span { color: #91aabd; font-size: 16px; line-height: 1.25; }

.header-title { display: grid; justify-items: center; gap: 5px; }
.header-title {
  position: relative;
  min-width: 540px;
  padding: 0 82px 10px;
}
.header-title::before,
.header-title::after {
  position: absolute;
  bottom: 0;
  width: 142px;
  height: 19px;
  border-bottom: 2px solid #51b4d8;
  content: '';
}
.header-title::before { left: 0; border-left: 2px solid #51b4d8; transform: skewX(38deg); }
.header-title::after { right: 0; border-right: 2px solid #51b4d8; transform: skewX(-38deg); }
.header-title strong { color: #fff; font-size: 32px; font-weight: 760; line-height: 1.2; text-shadow: 0 0 20px rgb(81 180 216 / 28%); }
.header-title span { color: #75b2ca; font-size: 14px; font-weight: 650; line-height: 1.25; }

.header-actions { justify-content: flex-end; gap: 9px; }
.header-clock { gap: 9px; margin-left: 6px; color: #b4c3ce; font-size: 16px; white-space: nowrap; }
.header-clock b { color: #fff; font-size: 18px; font-weight: 700; }

.update-status {
  gap: 7px;
  padding-right: 12px;
  color: #9eb2c1;
  border-right: 1px solid #2b4356;
  font-size: 16px;
  white-space: nowrap;
}

.update-status i { width: 8px; height: 8px; border-radius: 50%; background: #49c787; box-shadow: 0 0 8px rgb(73 199 135 / 58%); }
.update-status b { color: #d0dbe3; font-size: 16px; }
.update-status.has-error i { background: #e7a24f; box-shadow: 0 0 8px rgb(231 162 79 / 58%); }
.update-status.is-preview { color: #e5b66f; }
.update-status.is-preview i { background: #e0a34d; box-shadow: 0 0 8px rgb(224 163 77 / 58%); }

.icon-button,
.exit-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #b8cbd9;
  border: 1px solid #34536b;
  border-radius: 4px;
  background: #102537;
  cursor: pointer;
}

.icon-button { width: 42px; height: 42px; }
.icon-button:hover,
.exit-button:hover { color: #fff; border-color: #4c7da0; background: #17344b; }
.back-button { color: #d8e8f3; }
.icon-button.rotating .app-icon { animation: spin 1s linear infinite; }

.exit-button { height: 42px; gap: 7px; padding: 0 13px; font-size: 16px; font-weight: 650; }

.screen-shell {
  position: relative;
  display: grid;
  height: calc(100% - 90px);
  grid-template-rows: 128px minmax(0, 1fr) 284px;
  gap: 16px;
  padding: 20px 22px 22px;
  box-sizing: border-box;
}

.screen-error {
  position: absolute;
  z-index: 10;
  top: 12px;
  left: 50%;
  display: flex;
  min-width: 440px;
  align-items: center;
  gap: 9px;
  padding: 9px 13px;
  color: #ffe5bf;
  border: 1px solid #8d6632;
  border-radius: 4px;
  background: #352819;
  font-size: 16px;
  transform: translateX(-50%);
}

.screen-error button { margin-left: auto; color: #fff; border: 0; background: transparent; cursor: pointer; }

.kpi-strip {
  display: grid;
  min-width: 0;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.kpi-card {
  --accent: #5297d4;
  position: relative;
  display: grid;
  min-width: 0;
  grid-template-columns: 64px minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  overflow: hidden;
  padding: 15px 16px;
  border: 1px solid #294b62;
  border-radius: 4px;
  background: rgb(9 28 44 / 96%);
  box-shadow: 0 10px 26px rgb(0 0 0 / 17%), inset 0 1px 0 rgb(255 255 255 / 2%);
}

.kpi-card::before {
  position: absolute;
  top: -1px;
  left: -1px;
  width: 82px;
  height: 3px;
  background: var(--accent);
  content: '';
}

.kpi-card::after {
  position: absolute;
  right: -1px;
  bottom: -1px;
  width: 22px;
  height: 22px;
  border-right: 3px solid color-mix(in srgb, var(--accent) 74%, #24465d);
  border-bottom: 3px solid color-mix(in srgb, var(--accent) 74%, #24465d);
  content: '';
}

.kpi-card.is-green { --accent: #4bc58a; }
.kpi-card.is-cyan { --accent: #49b9d0; }
.kpi-card.is-amber { --accent: #d8a253; }
.kpi-card.is-steel { --accent: #7f9fb8; }
.kpi-card.is-red { --accent: #e76d75; }
.kpi-card.is-muted { --accent: #788c9b; }

.kpi-icon {
  display: inline-flex;
  width: 62px;
  height: 62px;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  border: 1px solid color-mix(in srgb, var(--accent) 45%, #29465d);
  border-radius: 50%;
  background: color-mix(in srgb, var(--accent) 13%, #102435);
  box-shadow: inset 0 0 22px color-mix(in srgb, var(--accent) 12%, transparent), 0 0 18px color-mix(in srgb, var(--accent) 8%, transparent);
}

.kpi-copy { display: grid; min-width: 0; gap: 4px; }
.kpi-copy > span { color: #bdccd7; font-size: 16px; font-weight: 650; line-height: 1.2; }
.kpi-copy strong { overflow: hidden; color: #fff; font-size: 38px; font-weight: 760; line-height: 1; text-overflow: ellipsis; white-space: nowrap; }
.kpi-copy strong small { margin-left: 5px; color: #9eb0be; font-size: 15px; font-weight: 550; }
.kpi-copy p { overflow: hidden; margin: 0; color: #94aaba; font-size: 14px; line-height: 1.2; text-overflow: ellipsis; white-space: nowrap; }

.primary-grid {
  display: grid;
  min-height: 0;
  grid-template-areas: 'lines schedule execution';
  grid-template-columns: 350px minmax(0, 1fr) 390px;
  gap: 16px;
}

.schedule-panel { grid-area: schedule; }
.line-status-panel { grid-area: lines; }
.execution-panel { grid-area: execution; }

.secondary-grid {
  display: grid;
  min-height: 0;
  grid-template-columns: 1.18fr 1.05fr 1fr 1.17fr;
  gap: 16px;
}

.screen-panel {
  position: relative;
  min-width: 0;
  overflow: hidden;
  border: 1px solid #294a61;
  border-radius: 4px;
  background: rgb(8 25 40 / 96%);
  box-shadow: 0 10px 28px rgb(0 0 0 / 15%), inset 0 1px 0 rgb(255 255 255 / 2%);
}
.screen-panel::before {
  position: absolute;
  z-index: 2;
  top: -1px;
  left: 20px;
  width: 104px;
  height: 3px;
  background: #51b4d8;
  content: '';
}

.screen-panel::after {
  position: absolute;
  z-index: 2;
  right: -1px;
  bottom: -1px;
  width: 25px;
  height: 25px;
  border-right: 3px solid #337fa3;
  border-bottom: 3px solid #337fa3;
  content: '';
  pointer-events: none;
}

.panel-header {
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 0 22px;
  border-bottom: 1px solid #2a4960;
  background: rgb(13 37 56 / 82%);
}

.panel-header.compact { min-height: 68px; }
.panel-header > div { display: grid; gap: 5px; }
.panel-header > div > strong { color: #f2f7fb; font-size: 21px; font-weight: 720; line-height: 1.25; }
.panel-header > div > span { color: #9db1c0; font-size: 16px; line-height: 1.25; }

.panel-tools { display: flex; align-items: center; gap: 12px; }
.panel-total { display: grid; gap: 4px; justify-items: end; }
.panel-total span { color: #98abba; font-size: 16px; }
.panel-total b { color: #dce9f2; font-size: 20px; font-weight: 700; }
.panel-link {
  display: inline-flex;
  width: 38px;
  height: 38px;
  flex: none;
  align-items: center;
  justify-content: center;
  padding: 0;
  color: #9db5c7;
  border: 1px solid #355269;
  border-radius: 4px;
  background: #102638;
  cursor: pointer;
}
.panel-link:hover { color: #fff; border-color: #4f7896; background: #17364d; }

.schedule-panel .panel-header > div { min-width: 0; }
.schedule-panel .panel-tools { gap: 8px; }
.schedule-range {
  max-width: 210px;
  overflow: hidden;
  color: #86b9d3;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.overdue-badge {
  display: inline-flex;
  height: 28px;
  align-items: center;
  gap: 5px;
  padding: 0 8px;
  color: #ffb0a6;
  border: 1px solid rgb(226 99 104 / 38%);
  border-radius: 3px;
  background: rgb(173 49 57 / 18%);
  font-size: 13px;
  font-weight: 650;
  white-space: nowrap;
}

.schedule-board {
  position: relative;
  height: calc(100% - 72px);
  padding: 8px 14px 34px;
  box-sizing: border-box;
  outline: none;
  cursor: zoom-in;
}

.schedule-board:focus-visible {
  box-shadow: inset 0 0 0 2px #51b4d8;
}

.schedule-axis,
.schedule-row {
  display: grid;
  grid-template-columns: 170px minmax(0, 1fr);
}

.schedule-axis {
  height: 44px;
  align-items: end;
  color: #91a7b7;
  border-bottom: 1px solid #31536b;
  font-size: 15px;
}

.schedule-axis > span { padding: 0 12px 10px 2px; }
.schedule-axis-track { position: relative; height: 100%; }
.schedule-axis-track i {
  position: absolute;
  bottom: -1px;
  height: 11px;
  border-left: 1px solid #42657d;
  font-style: normal;
  transform: translateX(-50%);
}
.schedule-axis-track i:first-child { transform: none; }
.schedule-axis-track i:last-child { transform: translateX(-100%); }
.schedule-axis-track b {
  position: absolute;
  right: 0;
  bottom: 15px;
  color: #9eb5c5;
  font-size: 15px;
  font-weight: 550;
  white-space: nowrap;
  transform: translateX(50%);
}
.schedule-axis-track i:first-child b { right: auto; left: 0; transform: none; }
.schedule-axis-track i:last-child b { right: 0; transform: none; }

.schedule-rows { min-height: 0; }
.schedule-row {
  height: 64px;
  align-items: stretch;
  border-bottom: 1px solid rgb(48 79 101 / 72%);
}
.schedule-row-meta {
  display: grid;
  min-width: 0;
  align-content: center;
  gap: 4px;
  padding: 0 12px 0 2px;
}
.schedule-row-meta strong,
.schedule-row-meta span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.schedule-row-meta strong { color: #dbe8f0; font-size: 17px; font-weight: 680; }
.schedule-row-meta span { color: #829caf; font-size: 14px; }
.schedule-row-meta span.has-overdue { color: #e88d82; }
.schedule-row-track {
  position: relative;
  min-width: 0;
  overflow: hidden;
  background-image: linear-gradient(90deg, rgb(72 108 134 / 18%) 1px, transparent 1px);
  background-size: 16.6667% 100%;
}
.schedule-row-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  padding-left: 12px;
  color: #607d91;
  font-size: 13px;
}
.schedule-bar {
  position: absolute;
  top: 8px;
  display: grid;
  height: 48px;
  min-width: 48px;
  align-content: center;
  gap: 2px;
  overflow: hidden;
  padding: 5px 8px;
  color: #eef7ff;
  border: 1px solid rgb(117 174 215 / 52%);
  border-radius: 4px;
  background: #315d88;
  box-sizing: border-box;
  box-shadow: 0 4px 12px rgb(0 0 0 / 18%);
  white-space: nowrap;
}
.schedule-bar.is-released { border-color: rgb(90 169 209 / 60%); background: #2b6288; }
.schedule-bar.is-running { border-color: rgb(83 194 139 / 58%); background: #26704f; }
.schedule-bar.is-finished { border-color: rgb(215 166 83 / 58%); background: #765726; }
.schedule-bar.is-overdue { border-color: rgb(238 105 111 / 72%); background: #8a3942; box-shadow: 0 4px 16px rgb(175 46 56 / 26%); }
.schedule-bar.is-closed { border-color: rgb(130 149 164 / 48%); background: #485765; }
.schedule-bar b,
.schedule-bar small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.schedule-bar b { font-size: 15px; font-weight: 700; }
.schedule-bar small { color: rgb(239 247 252 / 78%); font-size: 12px; }

.schedule-empty {
  display: grid;
  height: calc(100% - 44px);
  align-content: center;
  justify-items: center;
  gap: 8px;
  color: #7790a2;
  text-align: center;
}
.schedule-empty strong { color: #bdcbd5; font-size: 18px; }
.schedule-empty span { max-width: 520px; font-size: 14px; }
.schedule-empty.is-error { color: #d69a5f; }
.schedule-stale {
  position: absolute;
  bottom: 8px;
  left: 16px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #dda45f;
  font-size: 13px;
}
.schedule-expand-hint {
  position: absolute;
  right: 16px;
  bottom: 8px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #7f9caf;
  font-size: 13px;
}

.schedule-modal-backdrop {
  position: absolute;
  z-index: 80;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgb(2 9 16 / 88%);
  backdrop-filter: blur(5px);
}
.schedule-modal {
  display: grid;
  width: 1820px;
  height: var(--schedule-modal-height, 760px);
  max-height: calc(100% - 72px);
  grid-template-rows: 88px minmax(0, 1fr) 52px;
  overflow: hidden;
  border: 1px solid #397797;
  border-radius: 5px;
  background: #071827;
  box-shadow: 0 28px 80px rgb(0 0 0 / 52%), inset 0 0 70px rgb(40 112 154 / 6%);
}
.schedule-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 0 28px;
  border-bottom: 1px solid #2d536b;
  background: #0a2134;
}
.schedule-modal-header > div { display: grid; grid-template-columns: auto auto; align-items: baseline; gap: 4px 14px; }
.schedule-modal-header > div > span { color: #54b4d7; font-size: 13px; font-weight: 700; }
.schedule-modal-header strong { color: #fff; font-size: 30px; font-weight: 750; }
.schedule-modal-header p { grid-column: 1 / -1; margin: 0; color: #91a9b9; font-size: 16px; }
.schedule-modal-header aside { display: flex; align-items: center; gap: 10px; }
.modal-command,
.modal-close {
  display: inline-flex;
  height: 42px;
  align-items: center;
  justify-content: center;
  color: #dbeaf3;
  border: 1px solid #35627d;
  border-radius: 4px;
  background: #102c42;
  cursor: pointer;
}
.modal-command { gap: 8px; padding: 0 15px; font-size: 15px; font-weight: 650; }
.modal-close { width: 42px; padding: 0; }
.modal-command:hover,
.modal-close:hover { color: #fff; border-color: #55a9ce; background: #173b55; }
.expanded-board {
  height: auto;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 24px;
  cursor: default;
}
.expanded-board .schedule-axis,
.expanded-board .schedule-row { grid-template-columns: 260px minmax(0, 1fr); }
.expanded-board .schedule-axis { height: 50px; font-size: 18px; }
.expanded-board .schedule-axis > span { padding-left: 6px; }
.expanded-board .schedule-axis-track b { font-size: 18px; }
.expanded-board .schedule-row { height: 78px; }
.expanded-board .schedule-row-meta { gap: 3px; padding-left: 6px; }
.expanded-board .schedule-row-meta strong { font-size: 20px; }
.expanded-board .schedule-row-meta span { font-size: 16px; }
.expanded-board .schedule-row-meta small { color: #6f93a9; font-size: 14px; white-space: nowrap; }
.expanded-board .schedule-bar { top: 10px; height: 58px; padding: 7px 14px; }
.expanded-board .schedule-bar b { font-size: 19px; }
.expanded-board .schedule-bar small { font-size: 16px; }
.schedule-modal-footer {
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 0 28px;
  color: #9db1bf;
  border-top: 1px solid #2d536b;
  background: #091d2d;
  font-size: 14px;
}
.schedule-modal-footer span { display: inline-flex; align-items: center; gap: 7px; }
.schedule-modal-footer i { width: 11px; height: 11px; border-radius: 2px; background: #315d88; }
.schedule-modal-footer i.is-released { background: #2b6288; }
.schedule-modal-footer i.is-running { background: #26704f; }
.schedule-modal-footer i.is-finished { background: #765726; }
.schedule-modal-footer i.is-overdue { background: #8a3942; }
.schedule-modal-footer i.is-closed { background: #485765; }
.schedule-modal-footer b { margin-left: auto; color: #c9d8e2; font-size: 14px; font-weight: 650; }

.trend-chart-wrap { position: relative; height: calc(100% - 68px); padding: 0 6px; }
.compact-total { gap: 2px; }
.compact-total span { font-size: 13px; }
.compact-total b { font-size: 16px; }
.empty-overlay {
  position: absolute;
  inset: 62px 32px 30px 72px;
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 9px;
  color: #6f899c;
  background: rgb(10 27 42 / 72%);
}
.empty-overlay strong { color: #b2c3cf; font-size: 19px; }
.empty-overlay span { font-size: 16px; }
.compact-empty { inset: 48px 20px 18px 56px; gap: 6px; }
.compact-empty strong { font-size: 16px; }

.line-count { color: #f2f7fb; font-size: 30px; font-weight: 750; }
.line-count small { margin-left: 4px; color: #91a7b7; font-size: 15px; font-weight: 550; }
.line-status-panel .panel-header { min-height: 72px; padding: 0 16px; }
.line-status-panel .panel-header > div { min-width: 0; }
.line-status-panel .panel-header > div > span {
  overflow: hidden;
  max-width: 210px;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.line-status-list {
  display: grid;
  height: calc(100% - 72px);
  align-content: stretch;
  grid-auto-rows: minmax(0, 1fr);
  overflow: hidden;
}
.line-status-row {
  position: relative;
  display: grid;
  min-height: 0;
  grid-template-columns: 42px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  border-bottom: 1px solid rgb(49 81 105 / 72%);
  box-sizing: border-box;
}
.line-status-row:last-child { border-bottom: 0; }
.line-status-row::after {
  position: absolute;
  top: 10px;
  bottom: 10px;
  left: 0;
  width: 3px;
  background: #4bc58a;
  content: '';
}
.line-status-row.is-idle::after { background: #5aa9d1; }
.line-status-row.is-stopped::after { background: #df6972; }
.line-status-row.is-maintenance::after { background: #d99a55; }
.line-status-row.is-incomplete::after { background: #e07e57; }
.line-status-row.is-overdue::after { background: #e26368; }
.line-status-icon {
  display: inline-flex;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  color: #65cf9a;
  border: 1px solid rgb(75 197 138 / 28%);
  border-radius: 50%;
  background: rgb(75 197 138 / 10%);
}
.line-status-row.is-idle .line-status-icon { color: #6fb9df; border-color: rgb(90 169 209 / 28%); background: rgb(90 169 209 / 10%); }
.line-status-row.is-stopped .line-status-icon { color: #ed7a82; border-color: rgb(223 105 114 / 30%); background: rgb(223 105 114 / 11%); }
.line-status-row.is-maintenance .line-status-icon { color: #e6a65d; border-color: rgb(217 154 85 / 30%); background: rgb(217 154 85 / 11%); }
.line-status-row.is-incomplete .line-status-icon { color: #e88864; border-color: rgb(224 126 87 / 30%); background: rgb(224 126 87 / 11%); }
.line-status-row.is-overdue .line-status-icon { color: #ed7b80; border-color: rgb(226 99 104 / 34%); background: rgb(226 99 104 / 12%); }
.line-status-copy { display: grid; min-width: 0; gap: 3px; }
.line-status-copy > div { display: flex; min-width: 0; align-items: center; justify-content: space-between; gap: 8px; }
.line-status-copy strong,
.line-status-copy p,
.line-status-copy small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.line-status-copy strong { color: #e5eef4; font-size: 16px; }
.line-status-copy > div > span { display: inline-flex; flex: none; align-items: center; gap: 5px; color: #75d4a0; font-size: 13px; }
.line-status-copy > div > span i { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
.line-status-row.is-idle .line-status-copy > div > span { color: #75bce1; }
.line-status-row.is-stopped .line-status-copy > div > span { color: #ee858c; }
.line-status-row.is-maintenance .line-status-copy > div > span { color: #e5aa67; }
.line-status-row.is-incomplete .line-status-copy > div > span { color: #ed916e; }
.line-status-row.is-overdue .line-status-copy > div > span { color: #ee8589; }
.line-status-copy p { margin: 0; color: #a7bac7; font-size: 13px; }
.line-status-copy small { color: #7893a6; font-size: 12px; }
.line-status-empty {
  display: grid;
  height: calc(100% - 72px);
  align-content: center;
  justify-items: center;
  gap: 8px;
  color: #7893a5;
}
.line-status-empty strong { color: #c1d0da; font-size: 18px; }
.line-status-empty span { font-size: 14px; }

.table-status { display: inline-flex; align-items: center; gap: 7px; color: #78cfa0; font-size: 15px; white-space: nowrap; }
.table-status i { width: 7px; height: 7px; border-radius: 50%; background: #4bc58a; box-shadow: 0 0 7px rgb(75 197 138 / 48%); }
.table-status.is-empty { color: #8da0ae; }
.table-status.is-empty i { background: #718493; box-shadow: none; }
.report-table-wrap { position: relative; height: calc(100% - 72px); padding: 0 16px 12px; box-sizing: border-box; }
.report-table { width: 100%; border-collapse: collapse; table-layout: fixed; color: #dce8f0; font-size: 16px; }
.report-table caption { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }
.report-table th {
  height: 42px;
  padding: 0 8px;
  color: #9eb4c5;
  border-bottom: 1px solid #315169;
  background: rgb(15 39 58 / 72%);
  font-size: 15px;
  font-weight: 650;
  text-align: right;
}
.report-table th:first-child,
.report-table td:first-child { width: 25%; text-align: left; }
.report-table th:last-child,
.report-table td:last-child { width: 21%; }
.report-table td { height: 45px; padding: 0 8px; border-bottom: 1px solid rgb(49 81 105 / 62%); text-align: right; }
.report-table tbody tr:nth-child(even) { background: rgb(27 67 85 / 18%); }
.report-table tbody tr.is-zero { color: #8094a3; }
.report-table td b { display: block; color: #e3edf3; font-size: 16px; }
.report-table td small { color: #7f96a7; font-size: 13px; }
.report-table td.is-qualified { color: #6ed09b; }
.report-table td.is-defect { color: #e99275; }
.rate-chip { display: inline-flex; min-width: 63px; align-items: baseline; justify-content: center; padding: 4px 7px; color: #75d2a0; border: 1px solid rgb(75 197 138 / 24%); border-radius: 3px; background: rgb(75 197 138 / 9%); }
.rate-chip small { margin-left: 2px; color: inherit !important; font-size: 12px !important; }
.rate-chip.is-empty { color: #8296a5; border-color: rgb(113 132 147 / 22%); background: rgb(113 132 147 / 8%); }
.table-empty { position: absolute; inset: 42px 16px 12px; display: grid; align-content: center; justify-items: center; gap: 8px; color: #879cab; background: #0b1d2c; font-size: 16px; }

.execution-panel { display: grid; grid-template-rows: auto 1fr auto auto; }
.execution-panel .panel-header { gap: 8px; padding: 0 14px; }
.execution-panel .panel-header > div { min-width: 0; }
.execution-panel .panel-header > div > span { max-width: 194px; overflow: hidden; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.execution-panel .panel-tools { gap: 6px; }
.execution-panel .panel-link { width: 34px; height: 34px; }
.live-badge { display: inline-flex; align-items: center; gap: 7px; color: #79cfa0; font-size: 16px; }
.execution-panel .live-badge { gap: 5px; font-size: 14px; }
.live-badge i { width: 8px; height: 8px; border-radius: 50%; background: #4bc58a; box-shadow: 0 0 8px rgb(75 197 138 / 58%); }

.output-focus { display: grid; align-content: center; justify-items: center; gap: 7px; padding: 12px 18px 8px; }
.output-focus > span { color: #a8bac6; font-size: 17px; line-height: 1.25; }
.output-focus strong { color: #fff; font-size: 60px; font-weight: 780; line-height: 1; }
.output-focus strong small { margin-left: 8px; color: #91a5b5; font-size: 19px; font-weight: 550; }
.output-focus em { padding: 5px 12px; color: #7ed5a5; border-radius: 3px; background: rgb(75 197 138 / 10%); font-size: 16px; font-style: normal; line-height: 1.25; }
.output-focus em.is-empty { color: #8da0ae; background: rgb(120 140 155 / 10%); }

.output-breakdown { display: grid; grid-template-columns: 1fr 1fr; margin: 0 14px 10px; border-top: 1px solid #29445a; border-bottom: 1px solid #29445a; }
.output-breakdown > div { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; padding: 12px 10px; }
.output-breakdown > div + div { border-left: 1px solid #29445a; }
.output-breakdown span { color: #9eb1bf; font-size: 15px; line-height: 1.25; }
.output-breakdown b { color: #dcebf4; font-size: 24px; line-height: 1.1; }
.output-breakdown .is-qualified b { color: #66cf98; }
.output-breakdown .is-defect b { color: #e58b68; }

.production-stat-list { display: grid; grid-template-columns: 1fr; padding: 0 14px 8px; }
.production-stat-row { display: grid; min-width: 0; grid-template-columns: 38px minmax(0, 1fr) auto; align-items: center; gap: 9px; padding: 6px 3px; }
.production-stat-row + .production-stat-row { border-top: 1px solid #29445a; }
.production-stat-row > span { display: inline-flex; width: 32px; height: 32px; align-items: center; justify-content: center; color: #73add3; border-radius: 4px; background: #123149; }
.production-stat-row div { display: grid; min-width: 0; gap: 2px; }
.production-stat-row b { color: #c9d7e1; font-size: 15px; line-height: 1.25; }
.production-stat-row small { overflow: hidden; color: #91a6b5; font-size: 14px; line-height: 1.25; text-overflow: ellipsis; white-space: nowrap; }
.production-stat-row strong { color: #fff; font-size: 24px; }

.quality-main { display: grid; height: calc(100% - 68px); grid-template-columns: 128px minmax(0, 1fr); align-items: center; gap: 12px; padding: 10px 14px 12px; box-sizing: border-box; }
.quality-dial-stack { display: grid; justify-items: center; gap: 4px; }
.quality-focus-title { color: #aebfcb; font-size: 13px; font-weight: 600; line-height: 1.2; white-space: nowrap; }
.quality-dial {
  position: relative;
  display: grid;
  width: 100px;
  height: 100px;
  place-items: center;
  border-radius: 50%;
  background: conic-gradient(#4bc58a var(--quality-angle), #1b3548 0);
}
.quality-dial::after { position: absolute; inset: 10px; border: 1px solid #33566f; border-radius: 50%; background: #0b1e2e; content: ''; }
.quality-dial::before { position: absolute; inset: -3px; border: 1px solid rgb(78 172 207 / 24%); border-radius: 50%; content: ''; }
.quality-dial.is-empty { background: #1b3548; }
.quality-dial > div { position: relative; z-index: 1; display: grid; justify-items: center; }
.quality-dial strong { color: #fff; font-size: 28px; line-height: 1; }
.quality-dial strong small { margin-left: 2px; color: #94a8b6; font-size: 15px; }
.quality-dial-summary { color: #95a9b7; font-size: 13px; white-space: nowrap; }
.quality-counts { display: grid; grid-template-columns: 1fr; gap: 0; border-left: 1px solid #365066; }
.quality-counts > div { display: flex; min-height: 0; align-items: center; justify-content: space-between; gap: 10px; padding: 3px 9px; }
.quality-counts > div + div { border-top: 1px solid rgb(54 80 102 / 76%); }
.quality-counts span { color: #9aafbd; font-size: 14px; line-height: 1.15; }
.quality-counts b { color: #eaf3f8; font-size: 19px; line-height: 1.15; }

.equipment-total,
.attention-total { color: #f0f6fa; font-size: 30px; font-weight: 750; }
.equipment-total small,
.attention-total small { margin-left: 6px; color: #9aacb9; font-size: 16px; font-weight: 550; }
.attention-total.has-warning { color: #ed9d5a; }

.equipment-body { position: relative; display: grid; height: calc(100% - 68px); align-content: center; gap: 18px; padding: 18px 22px; box-sizing: border-box; }
.equipment-bar { display: flex; height: 18px; overflow: hidden; border-radius: 3px; background: #1b3346; }
.equipment-bar i { display: block; height: 100%; min-width: 0; }
.equipment-bar i.is-green { background: #4bc58a; }
.equipment-bar i.is-blue { background: #5aa9d1; }
.equipment-bar i.is-gray { background: #718493; }
.equipment-bar i.is-amber { background: #d99a55; }
.equipment-bar i.is-red { background: #df6972; }
.equipment-bar.is-empty { background: #1b3346; }
.equipment-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px 22px; }
.equipment-grid > div { display: grid; grid-template-columns: 10px minmax(0, 1fr) auto; align-items: center; gap: 9px; }
.equipment-grid i { width: 9px; height: 9px; border-radius: 2px; }
.equipment-grid i.is-green { background: #4bc58a; }
.equipment-grid i.is-blue { background: #5aa9d1; }
.equipment-grid i.is-gray { background: #718493; }
.equipment-grid i.is-amber { background: #d99a55; }
.equipment-grid i.is-red { background: #df6972; }
.equipment-grid span { color: #9eb1bf; font-size: 17px; }
.equipment-grid b { color: #eef5f9; font-size: 26px; }
.inline-empty { position: absolute; top: 52px; right: 22px; margin: 0; color: #91a5b3; font-size: 16px; }

.attention-list { display: grid; height: calc(100% - 68px); grid-auto-rows: minmax(0, 1fr); align-content: stretch; padding: 4px 18px 8px; box-sizing: border-box; }
.attention-row {
  display: grid;
  width: 100%;
  min-width: 0;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 4px;
  color: inherit;
  border: 0;
  border-bottom: 1px solid #29445a;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.attention-row:last-child { border-bottom: 0; }
.attention-row:hover { background: rgb(75 111 137 / 10%); }
.attention-icon { display: inline-flex; width: 36px; height: 36px; align-items: center; justify-content: center; color: #65c793; border-radius: 4px; background: rgb(75 197 138 / 12%); }
.attention-row.is-warning .attention-icon { color: #e4a65d; background: rgb(228 166 93 / 13%); }
.attention-row.is-danger .attention-icon { color: #e2747c; background: rgb(226 116 124 / 13%); }
.attention-copy { display: grid; min-width: 0; gap: 2px; }
.attention-copy b { color: #dbe7ef; font-size: 16px; line-height: 1.2; }
.attention-copy small { overflow: hidden; color: #95a9b7; font-size: 13px; line-height: 1.2; text-overflow: ellipsis; white-space: nowrap; }
.attention-value { display: inline-flex; min-width: 68px; align-items: center; justify-content: flex-end; gap: 8px; color: #7891a3; }
.attention-value strong { min-width: 42px; color: #edf5fa; font-size: 25px; text-align: right; }

@keyframes spin { to { transform: rotate(360deg); } }

@media (prefers-reduced-motion: reduce) {
  .icon-button.rotating .app-icon { animation: none; }
}
</style>
