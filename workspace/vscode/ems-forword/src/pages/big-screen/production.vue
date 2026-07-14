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

const router = useRouter()
const previewMode = isFrameworkPreview()

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
const equipment = ref({})
const schedulePlans = ref([])
const scheduleError = ref('')
const scheduleExpanded = ref(false)

let clockTimer
let refreshTimer

const scheduleStatusMeta = {
  DRAFT: { label: '待生效', tone: 'draft' },
  RELEASED: { label: '已生效', tone: 'released' },
  FINISHED: { label: '已完成', tone: 'finished' },
  CLOSED: { label: '已取消', tone: 'closed' }
}

const stageStyle = computed(() => ({
  transform: `translate3d(${offsetX.value}px, ${offsetY.value}px, 0) scale(${scale.value})`
}))

const dateText = computed(() => now.value.format('YYYY-MM-DD'))
const weekdayText = computed(() => ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][now.value.day()])
const timeText = computed(() => now.value.format('HH:mm:ss'))
const updateText = computed(() => lastUpdated.value ? lastUpdated.value.format('HH:mm:ss') : '--:--:--')
const syncText = computed(() => {
  if (previewMode) return '演示数据'
  if (loading.value && !lastUpdated.value) return '正在连接数据'
  if (failedDataCount.value === 4) return '数据连接失败'
  if (failedDataCount.value > 0) return '部分数据未更新'
  if (scheduleError.value) return '排期数据未更新'
  return '数据已同步'
})

const todayQualified = computed(() => Number(summary.value.todayQualifiedQuantity || 0))
const todayDefect = computed(() => Number(summary.value.todayDefectQuantity || 0))
const todayOutput = computed(() => todayQualified.value + todayDefect.value)
const reportPassRate = computed(() => todayOutput.value
  ? (todayQualified.value / todayOutput.value) * 100
  : null)
const inspectionCount = computed(() => Number(quality.value.inspectionCount || 0))
const inspectionPassRate = computed(() => inspectionCount.value
  ? Number(quality.value.qualifiedRate || 0)
  : null)
const qualityProgressWidth = computed(() => Math.min(100, Math.max(0, inspectionPassRate.value || 0)))
const qualityDialStyle = computed(() => ({
  '--quality-angle': `${qualityProgressWidth.value * 3.6}deg`
}))
const equipmentTotal = computed(() => ['running', 'stopped', 'fault', 'maintenance']
  .reduce((total, key) => total + Number(equipment.value[key] || 0), 0))
const warningCount = computed(() => Number(summary.value.inventoryWarningCount || 0))
const faultCount = computed(() => Number(summary.value.equipmentFaultCount || 0))
const riskCategoryCount = computed(() => [faultCount.value, warningCount.value, todayDefect.value]
  .filter((value) => value > 0).length)
const trendRows = computed(() => trend.value.map((item) => {
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
const hasTrendData = computed(() => trendRows.value.some((item) => item.output > 0))
const sevenDayOutput = computed(() => trendRows.value.reduce((total, item) => total + item.output, 0))

const scheduleRows = computed(() => schedulePlans.value
  .filter((item) => {
    const start = dayjs(item.plannedStartTime)
    const end = dayjs(item.plannedEndTime)
    return start.isValid() && end.isValid() && end.isAfter(start)
  })
  .sort((a, b) => dayjs(a.plannedStartTime).valueOf() - dayjs(b.plannedStartTime).valueOf()))
const visibleScheduleRows = computed(() => scheduleRows.value.slice(0, 5))
const expandedScheduleRows = computed(() => scheduleRows.value.slice(0, 10))
const scheduleModalStyle = computed(() => ({
  '--schedule-modal-height': `${Math.min(940, Math.max(560, 222 + expandedScheduleRows.value.length * 78))}px`
}))
const scheduleWindow = computed(() => {
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
  const tickFormat = duration <= 36 * 60 * 60 * 1000 ? 'HH:mm' : 'MM-DD'
  const ticks = Array.from({ length: 7 }, (_, index) => {
    const ratio = index / 6
    return {
      label: start.add(duration * ratio, 'millisecond').format(tickFormat),
      left: `${ratio * 100}%`
    }
  })
  return { start, end, duration, ticks }
})
const scheduleRangeLabel = computed(() => {
  const { start, end } = scheduleWindow.value
  if (!start || !end) return '暂无有效排期'
  if (start.isSame(end, 'day')) {
    return `${start.format('MM月DD日 HH:mm')} - ${end.format('HH:mm')}`
  }
  return `${start.format('MM-DD HH:mm')} - ${end.format('MM-DD HH:mm')}`
})

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
    label: '今日不良报工',
    value: formatNumber(todayDefect.value),
    unit: '件',
    description: todayDefect.value > 0 ? '进入报工明细复核原因' : '今日暂无不良报工',
    icon: 'alert',
    tone: todayDefect.value > 0 ? 'red' : 'muted'
  }
])

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

const equipmentItems = computed(() => [
  { key: 'running', label: '运行', value: Number(equipment.value.running || 0), tone: 'green' },
  { key: 'stopped', label: '停机', value: Number(equipment.value.stopped || 0), tone: 'gray' },
  { key: 'maintenance', label: '维保', value: Number(equipment.value.maintenance || 0), tone: 'amber' },
  { key: 'fault', label: '故障', value: Number(equipment.value.fault || 0), tone: 'red' }
])

const attentionItems = computed(() => [
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

function formatNumber(value) {
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 1 }).format(Number(value || 0))
}

function formatPercent(value) {
  return value === null || value === undefined ? '--' : Number(value).toFixed(1)
}

function pageRecords(result) {
  const data = result?.data
  return Array.isArray(data) ? data : (data?.records || [])
}

function scheduleBarStyle(item) {
  const { start, duration } = scheduleWindow.value
  if (!start || !duration) return { left: '0%', width: '0%' }
  const itemStart = dayjs(item.plannedStartTime).valueOf()
  const itemEnd = dayjs(item.plannedEndTime).valueOf()
  const left = Math.max(0, Math.min(96, ((itemStart - start.valueOf()) / duration) * 100))
  const naturalWidth = Math.max(0, ((itemEnd - itemStart) / duration) * 100)
  const width = Math.max(4, Math.min(100 - left, naturalWidth))
  return { left: `${left}%`, width: `${width}%` }
}

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

function formatScheduleTime(item) {
  const start = dayjs(item.plannedStartTime)
  const end = dayjs(item.plannedEndTime)
  if (!start.isValid() || !end.isValid()) return '--'
  if (start.isSame(end, 'day')) {
    return `${start.format('MM-DD HH:mm')} - ${end.format('HH:mm')}`
  }
  return `${start.format('MM-DD HH:mm')} - ${end.format('MM-DD HH:mm')}`
}

async function loadProductionSchedule() {
  const [planResult, orderResult, lineResult, productResult] = await Promise.allSettled([
    getResourcePage('/api/mfg/production-plan', { current: 1, size: 50 }),
    getResourcePage('/api/mfg/production-order', { current: 1, size: 200 }),
    getResourcePage('/api/factory/line', { current: 1, size: 200 }),
    getResourcePage('/api/mdm/product', { current: 1, size: 200 })
  ])
  if (planResult.status === 'rejected') {
    throw planResult.reason
  }

  const optionalRecords = (result) => result.status === 'fulfilled' ? pageRecords(result.value) : []
  const plans = pageRecords(planResult.value)
  const orders = new Map(optionalRecords(orderResult).map((item) => [String(item.productionOrderId), item]))
  const lines = new Map(optionalRecords(lineResult).map((item) => [String(item.lineId), item]))
  const products = new Map(optionalRecords(productResult).map((item) => [String(item.productId), item]))

  return plans.map((plan) => {
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
}

function openSchedule() {
  scheduleExpanded.value = true
}

function closeSchedule() {
  scheduleExpanded.value = false
}

async function openProductionPlans() {
  closeSchedule()
  await openModule('/mfg/production-plans')
}

function handleScreenKeydown(event) {
  if (event.key === 'Escape' && scheduleExpanded.value) {
    closeSchedule()
  }
}

function updateScale() {
  const viewport = window.visualViewport
  const viewportWidth = viewport?.width || window.innerWidth
  const viewportHeight = viewport?.height || window.innerHeight
  const nextScale = Math.min(viewportWidth / 1920, viewportHeight / 1080)
  scale.value = nextScale
  offsetX.value = Math.max(0, (viewportWidth - 1920 * nextScale) / 2)
  offsetY.value = Math.max(0, (viewportHeight - 1080 * nextScale) / 2)
}

function handleFullscreenChange() {
  fullscreenActive.value = Boolean(document.fullscreenElement)
  updateScale()
}

async function toggleFullscreen() {
  if (!document.fullscreenElement) {
    await document.documentElement.requestFullscreen?.()
  } else {
    await document.exitFullscreen?.()
  }
  window.setTimeout(updateScale, 120)
}

async function exitBigScreen() {
  await openModule('/dashboard')
}

async function openModule(path) {
  await router.push(path)
}

async function loadData() {
  if (loading.value) return

  if (previewMode) {
    const data = createDashboardPreview()
    summary.value = data.summary
    trend.value = data.trend
    quality.value = data.quality
    equipment.value = data.equipment
    schedulePlans.value = data.schedule || []
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
    const results = await Promise.allSettled([
      getDashboardSummary(),
      getProductionTrend(params),
      getQualityStatistics(params),
      getEquipmentStatus(),
      loadProductionSchedule()
    ])
    const dashboardResults = results.slice(0, 4)
    const scheduleResult = results[4]
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
      schedulePlans.value = scheduleResult.value
    } else {
      scheduleError.value = scheduleResult.reason?.displayMessage || scheduleResult.reason?.message || '生产排期暂未更新'
    }

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

onMounted(() => {
  updateScale()
  loadData()
  clockTimer = window.setInterval(() => { now.value = dayjs() }, 1000)
  refreshTimer = window.setInterval(loadData, 60000)
  window.addEventListener('resize', updateScale)
  window.addEventListener('keydown', handleScreenKeydown)
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  window.visualViewport?.addEventListener('resize', updateScale)
})

onBeforeUnmount(() => {
  window.clearInterval(clockTimer)
  window.clearInterval(refreshTimer)
  window.removeEventListener('resize', updateScale)
  window.removeEventListener('keydown', handleScreenKeydown)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  window.visualViewport?.removeEventListener('resize', updateScale)
})
</script>

<template>
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
                <strong>生产订单排期</strong>
                <span>基于生产计划起止时间与产线形成</span>
              </div>
              <aside class="panel-tools">
                <span class="schedule-range">{{ scheduleRangeLabel }}</span>
                <button class="panel-link" type="button" title="放大生产订单排期" aria-label="放大生产订单排期" @click="openSchedule">
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
              aria-label="点击放大生产订单排期"
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
              <div v-if="visibleScheduleRows.length" class="schedule-rows">
                <div v-for="item in visibleScheduleRows" :key="item.planId" class="schedule-row">
                  <div class="schedule-row-meta">
                    <strong>{{ item.lineName }}</strong>
                    <span>{{ item.planNo }}</span>
                  </div>
                  <div class="schedule-row-track">
                    <span
                      class="schedule-bar"
                      :class="`is-${item.statusTone}`"
                      :style="scheduleBarStyle(item)"
                      :title="scheduleBarTitle(item)"
                    >
                      <b>{{ item.orderNo }}</b>
                      <small>{{ item.productName }} · {{ formatNumber(item.plannedQuantity) }} 件</small>
                      <em>{{ item.statusLabel }}</em>
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
              <div v-if="scheduleError && visibleScheduleRows.length" class="schedule-stale"><AppIcon name="alert" :size="14" />当前保留上次成功排期</div>
              <div class="schedule-expand-hint"><AppIcon name="fullscreen" :size="16" />点击放大查看完整排期</div>
            </div>
          </article>

          <article class="screen-panel report-table-panel">
            <header class="panel-header">
              <div>
                <strong>近 7 日报工明细</strong>
                <span>按报工日期汇总实际产出</span>
              </div>
              <aside class="panel-tools">
                <span class="table-status" :class="{ 'is-empty': !hasTrendData }"><i />{{ hasTrendData ? '已产生报工' : '暂无报工' }}</span>
                <button class="panel-link" type="button" title="打开生产报工明细" aria-label="打开生产报工明细" @click="openModule('/mfg/work-reports')">
                  <AppIcon name="arrow" :size="20" />
                </button>
              </aside>
            </header>
            <div class="report-table-wrap">
              <table class="report-table">
                <caption>近 7 日报工产量、合格数量、不良数量和合格率</caption>
                <thead>
                  <tr>
                    <th scope="col">日期</th>
                    <th scope="col">报工产量</th>
                    <th scope="col">合格</th>
                    <th scope="col">不良</th>
                    <th scope="col">合格率</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in trendRows" :key="row.date" :class="{ 'is-zero': row.output === 0 }">
                    <td><b>{{ row.dateLabel }}</b><small>{{ row.weekdayLabel }}</small></td>
                    <td>{{ formatNumber(row.output) }}</td>
                    <td class="is-qualified">{{ formatNumber(row.qualifiedQuantity) }}</td>
                    <td class="is-defect">{{ formatNumber(row.defectQuantity) }}</td>
                    <td><span class="rate-chip" :class="{ 'is-empty': row.passRate === null }">{{ formatPercent(row.passRate) }}<small v-if="row.passRate !== null">%</small></span></td>
                  </tr>
                </tbody>
              </table>
              <div v-if="trendRows.length === 0" class="table-empty">
                <AppIcon name="clipboard" :size="28" />
                <span>报工明细暂未返回</span>
              </div>
            </div>
          </article>

          <article class="screen-panel execution-panel">
            <header class="panel-header">
              <div>
                <strong>今日生产情况</strong>
                <span>今日报工结果与当前生产台账</span>
              </div>
              <aside class="panel-tools">
                <span class="live-badge"><i />实时</span>
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
                <strong>近 7 日质量</strong>
                <span>近 7 日新建质检单的结果统计</span>
              </div>
              <button class="panel-link" type="button" title="打开检验单明细" aria-label="打开检验单明细" @click="openModule('/qms/inspection-orders')">
                <AppIcon name="arrow" :size="20" />
              </button>
            </header>
            <div class="quality-main">
              <div class="quality-dial" :class="{ 'is-empty': inspectionPassRate === null }" :style="qualityDialStyle">
                <div>
                  <span>质检单合格率</span>
                  <strong>{{ formatPercent(inspectionPassRate) }}<small v-if="inspectionPassRate !== null">%</small></strong>
                </div>
              </div>
              <div class="quality-counts">
                <div><span>质检单</span><b>{{ formatNumber(inspectionCount) }}</b></div>
                <div><span>合格</span><b>{{ formatNumber(quality.qualifiedCount) }}</b></div>
                <div><span>不合格</span><b>{{ formatNumber(quality.unqualifiedCount) }}</b></div>
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
                <span>设备故障、补货预警与不良报工</span>
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
              <strong id="schedule-modal-title">生产订单排期总览</strong>
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
            <div v-if="expandedScheduleRows.length" class="schedule-rows">
              <div v-for="item in expandedScheduleRows" :key="`expanded-${item.planId}`" class="schedule-row">
                <div class="schedule-row-meta">
                  <strong>{{ item.lineName }}</strong>
                  <span>{{ item.planNo }} · {{ item.statusLabel }}</span>
                  <small>{{ formatScheduleTime(item) }}</small>
                </div>
                <div class="schedule-row-track">
                  <span
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
            <span><i class="is-released" />已生效</span>
            <span><i class="is-finished" />已完成</span>
            <span><i class="is-closed" />已取消</span>
            <b>显示 {{ expandedScheduleRows.length }} / {{ scheduleRows.length }} 条有效计划</b>
          </footer>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
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
  grid-template-areas: 'execution schedule report';
  grid-template-columns: minmax(320px, .65fr) minmax(780px, 1.65fr) minmax(480px, .92fr);
  gap: 16px;
}

.schedule-panel { grid-area: schedule; }
.report-table-panel { grid-area: report; }
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
  grid-template-columns: 132px minmax(0, 1fr);
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
.schedule-row-track {
  position: relative;
  min-width: 0;
  overflow: hidden;
  background-image: linear-gradient(90deg, rgb(72 108 134 / 18%) 1px, transparent 1px);
  background-size: 16.6667% 100%;
}
.schedule-bar {
  position: absolute;
  top: 8px;
  display: grid;
  height: 48px;
  min-width: 48px;
  align-content: center;
  gap: 3px;
  overflow: hidden;
  padding: 5px 64px 5px 10px;
  color: #eef7ff;
  border: 1px solid rgb(117 174 215 / 52%);
  border-radius: 4px;
  background: #315d88;
  box-sizing: border-box;
  box-shadow: 0 4px 12px rgb(0 0 0 / 18%);
  white-space: nowrap;
}
.schedule-bar.is-released { border-color: rgb(83 194 139 / 58%); background: #26704f; }
.schedule-bar.is-finished { border-color: rgb(215 166 83 / 58%); background: #765726; }
.schedule-bar.is-closed { border-color: rgb(130 149 164 / 48%); background: #485765; }
.schedule-bar b,
.schedule-bar small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.schedule-bar b { font-size: 16px; font-weight: 700; }
.schedule-bar small { color: rgb(239 247 252 / 78%); font-size: 13px; }
.schedule-bar em {
  position: absolute;
  top: 50%;
  right: 9px;
  color: #fff;
  font-size: 13px;
  font-style: normal;
  font-weight: 650;
  transform: translateY(-50%);
}

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
.schedule-modal-footer i.is-released { background: #26704f; }
.schedule-modal-footer i.is-finished { background: #765726; }
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

.quality-main { display: grid; height: calc(100% - 68px); grid-template-columns: 128px minmax(0, 1fr); align-items: center; gap: 12px; padding: 12px 14px 14px; box-sizing: border-box; }
.quality-dial {
  position: relative;
  display: grid;
  width: 118px;
  height: 118px;
  place-items: center;
  border-radius: 50%;
  background: conic-gradient(#4bc58a var(--quality-angle), #1b3548 0);
}
.quality-dial::after { position: absolute; inset: 11px; border: 1px solid #33566f; border-radius: 50%; background: #0b1e2e; content: ''; }
.quality-dial::before { position: absolute; inset: -4px; border: 1px solid rgb(78 172 207 / 24%); border-radius: 50%; content: ''; }
.quality-dial.is-empty { background: #1b3548; }
.quality-dial > div { position: relative; z-index: 1; display: grid; justify-items: center; gap: 4px; }
.quality-dial span { color: #93a9b8; font-size: 13px; }
.quality-dial strong { color: #fff; font-size: 31px; line-height: 1; }
.quality-dial strong small { margin-left: 2px; color: #94a8b6; font-size: 15px; }
.quality-counts { display: grid; grid-template-columns: 1fr; gap: 0; border-left: 1px solid #365066; }
.quality-counts > div { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 8px 10px; }
.quality-counts > div + div { border-top: 1px solid rgb(54 80 102 / 76%); }
.quality-counts span { color: #9aafbd; font-size: 15px; }
.quality-counts b { color: #eaf3f8; font-size: 23px; }

.equipment-total,
.attention-total { color: #f0f6fa; font-size: 30px; font-weight: 750; }
.equipment-total small,
.attention-total small { margin-left: 6px; color: #9aacb9; font-size: 16px; font-weight: 550; }
.attention-total.has-warning { color: #ed9d5a; }

.equipment-body { position: relative; display: grid; height: calc(100% - 68px); align-content: center; gap: 18px; padding: 18px 22px; box-sizing: border-box; }
.equipment-bar { display: flex; height: 18px; overflow: hidden; border-radius: 3px; background: #1b3346; }
.equipment-bar i { display: block; height: 100%; min-width: 0; }
.equipment-bar i.is-green { background: #4bc58a; }
.equipment-bar i.is-gray { background: #718493; }
.equipment-bar i.is-amber { background: #d99a55; }
.equipment-bar i.is-red { background: #df6972; }
.equipment-bar.is-empty { background: #1b3346; }
.equipment-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px 22px; }
.equipment-grid > div { display: grid; grid-template-columns: 10px minmax(0, 1fr) auto; align-items: center; gap: 9px; }
.equipment-grid i { width: 9px; height: 9px; border-radius: 2px; }
.equipment-grid i.is-green { background: #4bc58a; }
.equipment-grid i.is-gray { background: #718493; }
.equipment-grid i.is-amber { background: #d99a55; }
.equipment-grid i.is-red { background: #df6972; }
.equipment-grid span { color: #9eb1bf; font-size: 17px; }
.equipment-grid b { color: #eef5f9; font-size: 26px; }
.inline-empty { position: absolute; top: 52px; right: 22px; margin: 0; color: #91a5b3; font-size: 16px; }

.attention-list { display: grid; height: calc(100% - 68px); align-content: center; padding: 4px 18px 8px; box-sizing: border-box; }
.attention-row {
  display: grid;
  width: 100%;
  min-width: 0;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 8px 4px;
  color: inherit;
  border: 0;
  border-bottom: 1px solid #29445a;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.attention-row:last-child { border-bottom: 0; }
.attention-row:hover { background: rgb(75 111 137 / 10%); }
.attention-icon { display: inline-flex; width: 40px; height: 40px; align-items: center; justify-content: center; color: #65c793; border-radius: 4px; background: rgb(75 197 138 / 12%); }
.attention-row.is-warning .attention-icon { color: #e4a65d; background: rgb(228 166 93 / 13%); }
.attention-row.is-danger .attention-icon { color: #e2747c; background: rgb(226 116 124 / 13%); }
.attention-copy { display: grid; min-width: 0; gap: 2px; }
.attention-copy b { color: #dbe7ef; font-size: 17px; line-height: 1.25; }
.attention-copy small { overflow: hidden; color: #95a9b7; font-size: 16px; line-height: 1.25; text-overflow: ellipsis; white-space: nowrap; }
.attention-value { display: inline-flex; min-width: 68px; align-items: center; justify-content: flex-end; gap: 8px; color: #7891a3; }
.attention-value strong { min-width: 42px; color: #edf5fa; font-size: 28px; text-align: right; }

@keyframes spin { to { transform: rotate(360deg); } }

@media (prefers-reduced-motion: reduce) {
  .icon-button.rotating .app-icon { animation: none; }
}
</style>
