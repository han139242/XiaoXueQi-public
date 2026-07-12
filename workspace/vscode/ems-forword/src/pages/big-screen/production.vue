<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import AppIcon from '@/components/common/AppIcon.vue'
import MesChart from '@/components/chart/MesChart.vue'
import AiAssistant from '@/components/ai/AiAssistant.vue'
import {
  getDashboardSummary,
  getEquipmentStatus,
  getProductionTrend,
  getQualityStatistics
} from '@/api/dashboard'
import { createDashboardPreview } from '@/config/dashboard-preview'
import { isFrameworkPreview } from '@/utils/auth'

const router = useRouter()
const previewMode = isFrameworkPreview()
const scale = ref(1)
const now = ref(dayjs())
const loading = ref(false)
const loadError = ref('')
const aiOpen = ref(false)
const summary = ref({})
const trend = ref([])
const quality = ref({})
const equipment = ref({})
let clockTimer
let refreshTimer

const stageStyle = computed(() => ({ transform: `translate(-50%, -50%) scale(${scale.value})` }))
const timeText = computed(() => now.value.format('HH:mm:ss'))
const dateText = computed(() => now.value.format('YYYY-MM-DD'))
const weekdayText = computed(() => ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][now.value.day()])
const qualityRate = computed(() => Number(quality.value.qualifiedRate || 0))
const todayProduced = computed(() => Number(summary.value.todayQualifiedQuantity || 0) + Number(summary.value.todayDefectQuantity || 0))
const equipmentTotal = computed(() => ['running', 'stopped', 'fault', 'maintenance']
  .reduce((total, key) => total + Number(equipment.value[key] || 0), 0))
const equipmentAvailability = computed(() => (equipmentTotal.value
  ? (Number(equipment.value.running || 0) / equipmentTotal.value) * 100
  : 0))
// The dashboard API has no planned-cycle performance field yet, so this is an availability-and-quality OEE estimate.
const equipmentOee = computed(() => (equipmentAvailability.value * qualityRate.value) / 100)
const overallRate = computed(() => {
  if (!todayProduced.value) return qualityRate.value
  return Math.min(100, (Number(summary.value.todayQualifiedQuantity || 0) / todayProduced.value) * 100)
})

const metrics = computed(() => [
  { label: '生产工单', value: summary.value.workOrderCount, unit: '', icon: 'package', tone: 'blue', compare: '待办工单', change: formatNumber(summary.value.runningTaskCount), direction: 'neutral' },
  { label: '今日报工产量', value: todayProduced.value, unit: '', icon: 'factory', tone: 'amber', compare: '较昨日', change: '5.4%', direction: 'up' },
  { label: '今日合格产量', value: summary.value.todayQualifiedQuantity, unit: '', icon: 'shield-check', tone: 'teal', compare: '较昨日', change: '7.8%', direction: 'up' },
  { label: '设备OEE', value: equipmentOee.value, unit: '%', icon: 'settings', tone: 'violet', compare: '设备可动率', change: `${equipmentAvailability.value.toFixed(1)}%`, direction: 'neutral' },
  { label: '质量合格率', value: qualityRate.value.toFixed(2), unit: '%', icon: 'check', tone: 'cyan', compare: '较昨日', change: '0.6%', direction: 'up' },
  { label: '不良品数', value: summary.value.todayDefectQuantity, unit: '', icon: 'alert', tone: 'red', compare: '较昨日', change: '3.1%', direction: 'down' }
])

const lineStates = computed(() => {
  const running = Number(summary.value.runningTaskCount || 0)
  const presets = [
    ['产线 01', '加工一线', '运行中', 85, 'green'],
    ['产线 02', '加工二线', '运行中', 72, 'cyan'],
    ['产线 03', '装配一线', '运行中', 90, 'green'],
    ['产线 04', '装配二线', '调试中', 45, 'amber'],
    ['产线 05', '检测线', '运行中', 65, 'green'],
    ['产线 06', '包装线', '待排产', 20, 'red']
  ]
  return presets.map(([code, name, status, rate, tone], index) => ({
    code,
    name,
    status,
    rate: Math.min(99, Number(rate) + (running % (index + 2))),
    tone
  }))
})

const scheduleRows = [
  {
    line: '加工一线',
    tasks: [
      { code: 'MO-20260712-001', name: '碳纤维预浸料加工', quantity: '500 PCS', start: 2, span: 3.5, progress: 85, tone: 'green' },
      { code: 'MO-20260712-002', name: '拍框成型加工', quantity: '300 PCS', start: 6.1, span: 3.2, progress: 62, tone: 'green' },
      { code: 'MO-20260712-003', name: '组件精加工', quantity: '200 PCS', start: 9.9, span: 2.7, progress: 30, tone: 'green' }
    ]
  },
  {
    line: '加工二线',
    tasks: [
      { code: 'MO-20260712-004', name: '热压成型加工', quantity: '150 PCS', start: 2, span: 3.8, progress: 72, tone: 'blue' },
      { code: 'MO-20260712-005', name: '精密部件加工', quantity: '100 PCS', start: 6.2, span: 3.3, progress: 45, tone: 'blue' },
      { code: 'MO-20260712-006', name: '功能部件加工', quantity: '500 PCS', start: 10.1, span: 2.5, progress: 18, tone: 'blue' }
    ]
  },
  {
    line: '装配一线',
    tasks: [
      { code: 'MO-20260712-007', name: '框架组件装配', quantity: '120 PCS', start: 3.6, span: 3.5, progress: 68, tone: 'violet' },
      { code: 'MO-20260712-008', name: '握把与穿线装配', quantity: '120 PCS', start: 8.1, span: 3.5, progress: 25, tone: 'violet' }
    ]
  },
  {
    line: '装配二线',
    tasks: [
      { code: 'MO-20260712-009', name: '球拍组装工序', quantity: '80 PCS', start: 2, span: 4.4, progress: 100, tone: 'amber' },
      { code: 'MO-20260712-010', name: '成品组装工序', quantity: '80 PCS', start: 7.2, span: 3.6, progress: 60, tone: 'green' }
    ]
  },
  {
    line: '检验线',
    tasks: [
      { code: 'MO-20260712-011', name: '成品外观检测', quantity: '200 PCS', start: 2.4, span: 4.5, progress: 90, tone: 'green' },
      { code: 'MO-20260712-012', name: '批次质量抽检', quantity: '200 PCS', start: 7.6, span: 3.9, progress: 40, tone: 'green' }
    ]
  },
  {
    line: '包装线',
    tasks: [
      { code: 'MO-20260712-013', name: '包装与码垛', quantity: '150 PCS', start: 2.3, span: 3.7, progress: 15, tone: 'blue' },
      { code: 'MO-20260712-014', name: '待入库批次', quantity: '150 PCS', start: 7.9, span: 3.6, progress: 0, tone: 'blue' }
    ]
  }
]

const timeSlots = ['07:00', '08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00']

const progressItems = computed(() => [
  { label: '今日报工', value: todayProduced.value, tone: 'blue' },
  { label: '合格产量', value: summary.value.todayQualifiedQuantity, tone: 'green' },
  { label: '不良产量', value: summary.value.todayDefectQuantity, tone: 'muted' }
])

const riskItems = computed(() => [
  { label: 'CNC-03 设备主轴温度异常', line: '加工一线', time: '10:24', icon: 'alert', tone: 'red', value: summary.value.equipmentFaultCount },
  { label: '工序（加工二）检出不良品', line: '加工二线', time: '10:15', icon: 'alert', tone: 'amber', value: summary.value.todayDefectQuantity },
  { label: '物料【高性能毛羽】库存不足', line: '装配一线', time: '09:58', icon: 'warehouse', tone: 'blue', value: summary.value.inventoryWarningCount },
  { label: '工单进度滞后，建议复核排程', line: '加工二线', time: '09:40', icon: 'activity', tone: 'amber', value: summary.value.runningTaskCount }
])

const qualityCards = computed(() => [
  { label: '不良品数', value: summary.value.todayDefectQuantity, compare: '较昨日', change: '3.1%', tone: 'red', icon: 'alert', direction: 'down' },
  { label: '不良率', value: `${todayProduced.value ? ((Number(summary.value.todayDefectQuantity || 0) / todayProduced.value) * 100).toFixed(2) : '0.00'}%`, compare: '较昨日', change: '0.4%', tone: 'violet', icon: 'activity', direction: 'down' },
  { label: '直通率', value: `${qualityRate.value.toFixed(2)}%`, compare: '较昨日', change: '0.6%', tone: 'green', icon: 'check', direction: 'up' },
  { label: '合格率', value: `${qualityRate.value.toFixed(2)}%`, compare: '较昨日', change: '0.8%', tone: 'amber', icon: 'shield-check', direction: 'up' }
])

const trendOption = computed(() => ({
  animationDuration: 650,
  color: ['#5e92ff', '#71d486'],
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgb(4 20 43 / 96%)',
    borderColor: '#1c6197',
    textStyle: { color: '#dff2ff', fontSize: 13 }
  },
  legend: {
    top: 8,
    right: 13,
    itemWidth: 16,
    itemHeight: 5,
    textStyle: { color: '#97bad7', fontSize: 12 }
  },
  grid: { top: 39, right: 18, bottom: 29, left: 45 },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trend.value.map((item) => dayjs(item.date).format('MM-DD')),
    axisLine: { lineStyle: { color: '#1b4265' } },
    axisTick: { show: false },
    axisLabel: { color: '#7598b8', fontSize: 11 }
  },
  yAxis: {
    type: 'value',
    splitNumber: 4,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#7598b8', fontSize: 11 },
    splitLine: { lineStyle: { color: 'rgb(52 119 165 / 18%)', type: 'dashed' } }
  },
  series: [
    {
      name: '合格产量',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { width: 2 },
      areaStyle: { color: 'rgb(86 139 255 / 11%)' },
      data: trend.value.map((item) => Number(item.qualifiedQuantity || 0))
    },
    {
      name: '不良产量',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { width: 2 },
      areaStyle: { color: 'rgb(90 207 134 / 5%)' },
      data: trend.value.map((item) => Number(item.defectQuantity || 0))
    }
  ]
}))

const progressOption = computed(() => {
  const rate = Math.max(0, Math.min(100, overallRate.value))
  return {
    animationDuration: 650,
    color: ['#238dff', '#0a2748'],
    series: [{
      type: 'pie',
      radius: ['58%', '76%'],
      center: ['45%', '50%'],
      startAngle: 90,
      clockwise: true,
      label: {
        show: true,
        position: 'center',
        formatter: `{caption|总体进度}\n{rate|${rate.toFixed(2)}%}`,
        rich: {
          caption: { color: '#92b4d1', fontSize: 12, lineHeight: 22 },
          rate: { color: '#edf8ff', fontSize: 29, fontWeight: 750, lineHeight: 33 }
        }
      },
      labelLine: { show: false },
      data: [{ value: rate, name: '当前达成' }, { value: Math.max(0, 100 - rate), name: '剩余' }]
    }]
  }
})

const equipmentOption = computed(() => {
  const running = Number(equipment.value.running || 0)
  const stopped = Number(equipment.value.stopped || 0)
  const maintenance = Number(equipment.value.maintenance || 0)
  const fault = Number(equipment.value.fault || 0)
  return {
    animationDuration: 650,
    color: ['#4acb78', '#478eff', '#bd93d5', '#e4606e'],
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgb(4 20 43 / 96%)',
      borderColor: '#1c6197',
      textStyle: { color: '#dff2ff', fontSize: 13 }
    },
    legend: {
      orient: 'vertical',
      right: 14,
      top: 'middle',
      itemWidth: 8,
      itemHeight: 8,
      itemGap: 13,
      textStyle: { color: '#a1bfd8', fontSize: 12 }
    },
    series: [{
      type: 'pie',
      radius: ['56%', '74%'],
      center: ['33%', '52%'],
      label: {
        show: true,
        position: 'center',
        formatter: `{caption|设备总数}\n{total|${equipmentTotal.value}台}`,
        rich: {
          caption: { color: '#87a8c4', fontSize: 12, lineHeight: 21 },
          total: { color: '#f1f9ff', fontSize: 25, fontWeight: 750, lineHeight: 30 }
        }
      },
      labelLine: { show: false },
      data: [
        { value: running, name: `运行中   ${running}` },
        { value: stopped, name: `停机中   ${stopped}` },
        { value: maintenance, name: `保养中   ${maintenance}` },
        { value: fault, name: `故障中   ${fault}` }
      ]
    }]
  }
})

const efficiencyOption = computed(() => {
  const values = trend.value.map((item, index) => {
    const qualified = Number(item.qualifiedQuantity || 0)
    const defect = Number(item.defectQuantity || 0)
    const total = qualified + defect
    if (!total) return Math.max(52, qualityRate.value - 18 + index * 2)
    return Math.min(100, Math.max(0, (qualified / total) * 100))
  })
  return {
    animationDuration: 650,
    color: ['#5b9eff'],
    tooltip: {
      trigger: 'axis',
      valueFormatter: (value) => `${Number(value).toFixed(1)}%`,
      backgroundColor: 'rgb(4 20 43 / 96%)',
      borderColor: '#1c6197',
      textStyle: { color: '#dff2ff', fontSize: 13 }
    },
    grid: { top: 17, right: 16, bottom: 30, left: 40 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.value.map((item) => dayjs(item.date).format('MM-DD')),
      axisLine: { lineStyle: { color: '#1b4265' } },
      axisTick: { show: false },
      axisLabel: { color: '#7598b8', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      interval: 20,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#7598b8', fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgb(52 119 165 / 18%)', type: 'dashed' } }
    },
    series: [{
      name: '质量达成',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { width: 2 },
      areaStyle: { color: 'rgb(91 158 255 / 13%)' },
      markPoint: {
        symbol: 'roundRect',
        symbolSize: [42, 20],
        label: { color: '#dff4ff', fontSize: 11, formatter: ({ value }) => `${Number(value).toFixed(1)}%` },
        itemStyle: { color: '#216aa5' },
        data: values.length ? [{ type: 'max' }] : []
      },
      data: values
    }]
  }
})

function formatNumber(value) {
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 1 }).format(Number(value || 0))
}

function updateScale() {
  scale.value = Math.min(window.innerWidth / 1920, window.innerHeight / 1080)
}

async function toggleFullscreen() {
  if (!document.fullscreenElement) await document.documentElement.requestFullscreen?.()
  else await document.exitFullscreen?.()
  window.setTimeout(updateScale, 120)
}

async function exitBigScreen() {
  if (document.fullscreenElement) {
    await document.exitFullscreen?.()
  }
  await router.push('/dashboard')
}

async function loadData() {
  if (previewMode) {
    const data = createDashboardPreview()
    summary.value = data.summary
    trend.value = data.trend
    quality.value = data.quality
    equipment.value = data.equipment
    return
  }

  loading.value = true
  loadError.value = ''
  try {
    const params = { beginDate: dayjs().subtract(6, 'day').format('YYYY-MM-DD'), endDate: dayjs().format('YYYY-MM-DD') }
    const [summaryResult, trendResult, qualityResult, equipmentResult] = await Promise.all([
      getDashboardSummary(params), getProductionTrend(params), getQualityStatistics(params), getEquipmentStatus()
    ])
    summary.value = summaryResult.data || {}
    trend.value = trendResult.data || []
    quality.value = qualityResult.data || {}
    equipment.value = equipmentResult.data || {}
  } catch (error) {
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
})

onBeforeUnmount(() => {
  window.clearInterval(clockTimer)
  window.clearInterval(refreshTimer)
  window.removeEventListener('resize', updateScale)
})
</script>

<template>
  <div class="big-screen-page">
    <div class="big-screen-stage" :style="stageStyle">
      <header class="screen-header">
        <div class="header-left">
          <button class="header-brand" type="button" aria-label="返回工作台" @click="router.push('/dashboard')"><AppIcon name="menu" :size="24" /></button>
          <div class="header-crumb"><span>MES</span><b>生产看板</b></div>
        </div>
        <div class="header-title">
          <strong>羽球智造工厂数字化平台 · 生产看板</strong>
          <span>数据驱动生产 · 实时掌控全局</span>
        </div>
        <div class="header-actions">
          <div class="header-date"><AppIcon name="calendar" :size="17" /><span>{{ dateText }}</span><span>{{ weekdayText }}</span><b>{{ timeText }}</b></div>
          <button class="header-action" type="button" :class="{ rotating: loading }" aria-label="刷新数据" @click="loadData"><AppIcon name="refresh" :size="19" /></button>
          <button class="header-action" type="button" aria-label="AI 助手" @click="aiOpen = true"><AppIcon name="bot" :size="19" /></button>
          <button class="header-action" type="button" aria-label="全屏显示" @click="toggleFullscreen"><AppIcon name="fullscreen" :size="20" /></button>
          <button class="header-exit" type="button" aria-label="退出生产看板" @click="exitBigScreen"><AppIcon name="logout" :size="18" /><span>退出大屏</span></button>
        </div>
      </header>

      <main class="screen-shell">
        <div v-if="loadError" class="screen-error"><AppIcon name="alert" :size="17" /><span>{{ loadError }}</span><button type="button" @click="loadData">重新加载</button></div>

        <section class="metric-strip" aria-label="生产关键指标">
          <article v-for="metric in metrics" :key="metric.label" class="metric-card" :class="`is-${metric.tone}`">
            <span class="metric-wave" aria-hidden="true"><i v-for="index in 8" :key="index" /></span>
            <span class="metric-icon"><AppIcon :name="metric.icon" :size="40" :stroke-width="2.3" /></span>
            <div class="metric-content">
              <span>{{ metric.label }}</span>
              <strong>{{ formatNumber(metric.value) }}<small>{{ metric.unit }}</small></strong>
              <em :class="`is-${metric.direction}`">{{ metric.compare }} <i>{{ metric.direction === 'up' ? '↑' : metric.direction === 'down' ? '↓' : '·' }}</i> {{ metric.change }}</em>
            </div>
            <span class="metric-spark" aria-hidden="true"><i v-for="index in 8" :key="index" /></span>
          </article>
        </section>

        <section class="dashboard-grid">
          <section class="top-board">
            <article class="screen-panel line-panel">
              <header class="panel-header"><strong>产线状态</strong><button type="button" class="panel-icon" aria-label="产线详情"><AppIcon name="more" :size="19" /></button></header>
              <div class="line-list">
                <div v-for="line in lineStates" :key="line.code" class="line-row">
                  <span class="line-symbol" :class="`is-${line.tone}`"><AppIcon name="factory" :size="18" /></span>
                  <div class="line-copy"><b>{{ line.code }}</b><strong>{{ line.name }}</strong><span>{{ line.status }} · 计划 {{ Math.max(200, line.rate * 8) }} / 实际 {{ Math.max(120, line.rate * 6) }}</span></div>
                  <div class="line-rate"><i :style="{ '--rate': `${line.rate}%` }"><em>{{ line.rate }}%</em></i></div>
                </div>
              </div>
              <footer class="line-footer"><span>物料齐套率</span><b>{{ Math.max(82, 100 - Number(summary.inventoryWarningCount || 0) * 3) }}%</b></footer>
            </article>

            <article class="screen-panel schedule-panel">
              <header class="panel-header schedule-header">
                <strong>生产订单排期</strong>
                <div class="schedule-center"><div class="schedule-date"><button type="button" aria-label="前一天"><AppIcon name="left" :size="15" /></button><span>{{ dateText }}</span></div><span class="schedule-view"><b>日</b><span>周</span><span>月</span></span></div>
                <i class="schedule-legend"><em class="is-blue" />计划 <em class="is-green" />进行中 <em class="is-amber" />已完成 <em class="is-violet" />延迟</i>
              </header>
              <div class="schedule-board">
                <div class="schedule-ruler"><span>产线</span><div><b v-for="slot in timeSlots" :key="slot">{{ slot }}</b></div></div>
                <div v-for="row in scheduleRows" :key="row.line" class="schedule-row">
                  <strong>{{ row.line }}</strong>
                  <div class="schedule-track">
                    <i v-for="slot in timeSlots" :key="slot" />
                    <span v-for="task in row.tasks" :key="task.code" class="schedule-task" :class="`is-${task.tone}`" :style="{ '--start': task.start, '--span': task.span }">
                      <b>{{ task.code }}</b><small>{{ task.name }}</small><em>{{ task.quantity }}</em><i>{{ task.progress }}%</i>
                    </span>
                  </div>
                </div>
                <div class="schedule-now"><span>{{ timeText.slice(0, 5) }}</span></div>
              </div>
            </article>

            <aside class="right-stack">
              <article class="screen-panel progress-panel">
                <header class="panel-header"><strong>生产进度总览</strong><span class="panel-live">实时</span></header>
                <div class="progress-body"><div class="progress-chart"><MesChart :option="progressOption" height="188px" /></div><ul><li v-for="item in progressItems" :key="item.label"><i :class="`is-${item.tone}`" /><span>{{ item.label }}</span><b>{{ formatNumber(item.value) }}</b></li></ul><span class="factory-ghost" aria-hidden="true"><i v-for="index in 10" :key="index" /></span></div>
              </article>
              <article class="screen-panel production-trend-panel">
                <header class="panel-header"><strong>产量趋势 <small>(单位：件)</small></strong><div class="period-tabs"><b>今日</b><span>近7天</span><span>近30天</span></div></header>
                <MesChart :option="trendOption" height="247px" />
              </article>
            </aside>
          </section>

          <section class="bottom-board">
            <article class="screen-panel equipment-panel">
              <header class="panel-header"><strong>设备状态分布</strong><button type="button" class="panel-icon" aria-label="设备详情"><AppIcon name="more" :size="19" /></button></header>
              <MesChart :option="equipmentOption" height="221px" />
            </article>

            <article class="screen-panel efficiency-panel">
              <header class="panel-header"><strong>生产效率趋势 <small>(%)</small></strong><button type="button" class="panel-icon" aria-label="趋势详情"><AppIcon name="more" :size="19" /></button></header>
              <MesChart :option="efficiencyOption" height="221px" />
            </article>

            <article class="screen-panel alerts-panel">
              <header class="panel-header"><strong>异常预警</strong><button type="button" class="more-text">更多 <AppIcon name="right" :size="13" /></button></header>
              <div class="alert-list"><div v-for="item in riskItems" :key="item.label" class="alert-row" :class="`is-${item.tone}`"><span><AppIcon :name="item.icon" :size="16" /></span><strong>{{ item.label }}</strong><em>{{ item.line }}</em><b>{{ item.time }}</b></div></div>
            </article>

            <article class="screen-panel quality-panel">
              <header class="panel-header"><strong>质量指标</strong><button type="button" class="more-text">更多 <AppIcon name="right" :size="13" /></button></header>
              <div class="quality-grid"><article v-for="item in qualityCards" :key="item.label" :class="`is-${item.tone}`"><span><AppIcon :name="item.icon" :size="20" /></span><div><b>{{ item.label }}</b><strong>{{ item.value }}</strong><em :class="`is-${item.direction}`">{{ item.compare }} {{ item.direction === 'up' ? '↑' : '↓' }} {{ item.change }}</em></div></article></div>
            </article>
          </section>
        </section>
      </main>
    </div>
    <AiAssistant v-model="aiOpen" module="dashboard" />
  </div>
</template>

<style scoped>
.big-screen-page {
  position: fixed;
  inset: 0;
  overflow: hidden;
  color: #d9ecff;
  background:
    radial-gradient(circle at 50% -8%, rgb(18 100 190 / 22%), transparent 36%),
    linear-gradient(180deg, #04122a 0%, #020a19 66%, #031126 100%);
  font-family: var(--mes-font);
}

.big-screen-page::before,
.big-screen-page::after {
  position: absolute;
  inset: 0;
  content: '';
  pointer-events: none;
}

.big-screen-page::before {
  background-image:
    linear-gradient(rgb(26 116 196 / 8%) 1px, transparent 1px),
    linear-gradient(90deg, rgb(26 116 196 / 8%) 1px, transparent 1px);
  background-size: 52px 52px;
  opacity: .56;
}

.big-screen-page::after {
  border: 1px solid rgb(52 148 241 / 22%);
  box-shadow: inset 0 0 120px rgb(0 63 138 / 28%);
}

.big-screen-stage {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 1920px;
  height: 1080px;
  overflow: hidden;
  transform-origin: center;
}

.screen-header {
  position: relative;
  display: grid;
  height: 74px;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 0 22px;
  border-bottom: 1px solid #174e83;
  background:
    linear-gradient(90deg, rgb(5 26 56 / 98%), rgb(8 36 76 / 91%) 50%, rgb(5 26 56 / 98%)),
    #071b39;
  box-shadow: 0 6px 24px rgb(0 0 0 / 34%), inset 0 -1px 0 rgb(91 194 255 / 28%);
}

.screen-header::before,
.screen-header::after {
  position: absolute;
  bottom: -1px;
  width: 270px;
  height: 2px;
  background: linear-gradient(90deg, transparent, #1a93e3, transparent);
  content: '';
}

.screen-header::before { left: 12%; }
.screen-header::after { right: 12%; }

.header-left,
.header-actions,
.header-date,
.header-crumb { display: flex; align-items: center; }

.header-left { gap: 15px; }

.header-brand {
  position: relative;
  display: inline-flex;
  width: 62px;
  height: 38px;
  align-items: center;
  justify-content: center;
  color: #cdeeff;
  border: 1px solid #2466a1;
  border-radius: 2px;
  background: linear-gradient(110deg, #0c4d91, #123569 65%, transparent);
  box-shadow: inset 0 0 13px rgb(71 167 255 / 22%), 0 0 16px rgb(23 126 233 / 18%);
  cursor: pointer;
}

.header-brand::after {
  position: absolute;
  top: 4px;
  right: -8px;
  width: 16px;
  height: 28px;
  border-top: 1px solid #2474b5;
  border-right: 1px solid #2474b5;
  transform: skewX(-24deg);
  content: '';
}

.header-crumb {
  height: 34px;
  gap: 10px;
  padding: 0 22px 0 16px;
  color: #8fc9ff;
  border-left: 1px solid #1e5c91;
  background: linear-gradient(90deg, rgb(21 94 164 / 40%), transparent);
  font-size: 14px;
}

.header-crumb span { color: #6098cf; }
.header-crumb b { color: #d9f1ff; font-weight: 650; }

.header-title { display: grid; justify-items: center; gap: 5px; }

.header-title strong {
  color: #f2f9ff;
  font-size: 26px;
  font-weight: 760;
  line-height: 1;
  text-shadow: 0 0 20px rgb(63 169 255 / 24%);
}

.header-title span { color: #83b6e4; font-size: 12px; letter-spacing: 4px; }

.header-actions { justify-content: flex-end; gap: 13px; }

.header-date { gap: 12px; color: #b9d5ec; font-size: 12px; white-space: nowrap; }
.header-date b { color: #eff8ff; font-size: 13px; font-weight: 600; }

.header-action {
  position: relative;
  display: inline-flex;
  width: 30px;
  height: 30px;
  align-items: center;
  justify-content: center;
  color: #b5d9f4;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.header-action:hover { color: #fff; text-shadow: 0 0 11px #5eb9ff; }
.header-action.rotating .app-icon { animation: spin 1s linear infinite; }
.header-exit { display: inline-flex; height: 30px; align-items: center; gap: 5px; padding: 0 8px; color: #bfdbf4; border: 1px solid #245b8d; border-radius: 3px; background: rgb(12 49 82 / 62%); font-size: 11px; cursor: pointer; }.header-exit:hover { color: #fff; border-color: #4b9cd9; background: #164a79; box-shadow: 0 0 12px rgb(73 157 218 / 18%); }

.screen-shell {
  position: relative;
  display: grid;
  height: calc(100% - 74px);
  grid-template-rows: 108px minmax(0, 1fr);
  gap: 14px;
  padding: 14px 20px 18px;
}

.screen-error {
  position: absolute;
  z-index: 5;
  top: 119px;
  left: 50%;
  transform: translateX(-50%);
}

.screen-error { display: flex; align-items: center; gap: 8px; min-width: 350px; padding: 8px 12px; color: #ffd8db; border: 1px solid #9b3e4d; background: #391522; font-size: 12px; }
.screen-error button { margin-left: auto; color: #fff; border: 0; background: transparent; cursor: pointer; }

.metric-strip {
  position: relative;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  padding: 0 15px;
}

.metric-strip::before,
.metric-strip::after {
  position: absolute;
  top: 2px;
  bottom: 2px;
  width: 12px;
  border: 1px solid #215a91;
  background: #092648;
  box-shadow: inset 0 0 13px rgb(48 153 252 / 16%);
  content: '';
}

.metric-strip::before { left: 0; border-radius: 3px 0 0 3px; }
.metric-strip::after { right: 0; border-radius: 0 3px 3px 0; }

.metric-card {
  --metric: #4f9dff;
  --metric-bg: rgb(22 84 158 / 46%);
  --metric-border: #255c91;
  position: relative;
  display: grid;
  min-width: 0;
  grid-template-columns: 62px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  overflow: hidden;
  padding: 10px 14px;
  border: 1px solid var(--metric-border);
  border-radius: 5px;
  background: linear-gradient(105deg, var(--metric-bg), #081d39 58%, rgb(15 62 103 / 78%));
  box-shadow: inset 0 0 27px color-mix(in srgb, var(--metric) 8%, transparent), inset 0 1px 0 rgb(255 255 255 / 8%), 0 5px 13px rgb(0 0 0 / 18%);
}

.metric-card::before { position: absolute; top: -28px; left: 9px; width: 98px; height: 98px; border: 1px solid color-mix(in srgb, var(--metric) 14%, transparent); border-radius: 50%; box-shadow: 0 0 33px color-mix(in srgb, var(--metric) 14%, transparent); content: ''; }
.metric-card::after { position: absolute; right: 10px; bottom: 9px; left: 10px; height: 1px; background: linear-gradient(90deg, transparent, color-mix(in srgb, var(--metric) 35%, transparent), transparent); content: ''; }
.metric-card.is-amber { --metric: #e4aa5b; --metric-bg: rgb(132 78 25 / 52%); --metric-border: #6d4d2a; }
.metric-card.is-teal { --metric: #59caa9; --metric-bg: rgb(19 106 89 / 49%); --metric-border: #266b60; }
.metric-card.is-violet { --metric: #9378e8; --metric-bg: rgb(69 50 140 / 56%); --metric-border: #4d4181; }
.metric-card.is-cyan { --metric: #52cfdf; --metric-bg: rgb(18 104 127 / 49%); --metric-border: #266c80; }
.metric-card.is-red { --metric: #eb818b; --metric-bg: rgb(133 44 61 / 53%); --metric-border: #713745; }

.metric-icon {
  position: relative;
  z-index: 1;
  display: inline-flex;
  width: 58px;
  height: 58px;
  align-items: center;
  justify-content: center;
  color: color-mix(in srgb, var(--metric) 76%, white);
  border: 1px solid color-mix(in srgb, var(--metric) 44%, transparent);
  border-radius: 50%;
  background: color-mix(in srgb, var(--metric) 20%, #09203c);
  box-shadow: inset 0 0 17px color-mix(in srgb, var(--metric) 20%, transparent), 0 0 20px color-mix(in srgb, var(--metric) 12%, transparent);
}

.metric-content { position: relative; z-index: 1; display: grid; min-width: 0; gap: 4px; }
.metric-content > span { overflow: hidden; color: #e9f5ff; font-size: 12px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.metric-content strong { overflow: hidden; color: #fff; font-size: 28px; font-weight: 750; line-height: 1; text-overflow: ellipsis; white-space: nowrap; text-shadow: 0 2px 7px rgb(0 0 0 / 26%); }
.metric-content strong small { margin-left: 4px; color: #b5cce1; font-size: 11px; font-weight: 550; }
.metric-content em { overflow: hidden; color: #7f9eb9; font-size: 9px; font-style: normal; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.metric-content em i { margin-left: 2px; font-size: 12px; font-style: normal; }
.metric-content em.is-up { color: #66d598; }
.metric-content em.is-down { color: #ee7682; }
.metric-content em.is-neutral { color: #84bce7; }

.metric-spark { position: absolute; right: 9px; bottom: 14px; width: 55px; height: 30px; opacity: .48; }
.metric-spark::before { position: absolute; right: 0; bottom: 2px; left: 0; height: 1px; background: color-mix(in srgb, var(--metric) 38%, transparent); content: ''; }
.metric-spark i { position: absolute; width: 10px; height: 1px; background: var(--metric); box-shadow: 0 0 6px color-mix(in srgb, var(--metric) 50%, transparent); transform-origin: left center; }
.metric-spark i:nth-child(1) { top: 22px; left: 0; transform: rotate(-26deg); }.metric-spark i:nth-child(2) { top: 18px; left: 7px; transform: rotate(26deg); }.metric-spark i:nth-child(3) { top: 20px; left: 14px; transform: rotate(-40deg); }.metric-spark i:nth-child(4) { top: 12px; left: 21px; transform: rotate(32deg); }.metric-spark i:nth-child(5) { top: 15px; left: 28px; transform: rotate(-46deg); }.metric-spark i:nth-child(6) { top: 7px; left: 35px; transform: rotate(40deg); }.metric-spark i:nth-child(7) { top: 12px; left: 42px; transform: rotate(-18deg); }.metric-spark i:nth-child(8) { top: 9px; left: 49px; transform: rotate(9deg); }

.dashboard-grid { display: grid; min-height: 0; grid-template-rows: minmax(0, 1fr) 278px; gap: 14px; }
.top-board { display: grid; min-height: 0; grid-template-columns: 304px minmax(0, 1fr) 486px; gap: 14px; }
.bottom-board { display: grid; min-height: 0; grid-template-columns: 354px 504px 485px minmax(0, 1fr); gap: 14px; }

.screen-panel {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #185183;
  border-top: 2px solid #1b90d3;
  border-radius: 5px;
  background: linear-gradient(150deg, rgb(7 29 57 / 98%), rgb(4 18 38 / 98%));
  box-shadow: inset 0 1px 0 rgb(117 204 255 / 8%), inset 0 0 42px rgb(8 82 152 / 7%), 0 4px 13px rgb(0 0 0 / 14%);
}

.panel-header {
  display: flex;
  height: 48px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 0 15px;
  border-bottom: 1px solid #173e64;
  background: linear-gradient(90deg, rgb(11 47 85 / 57%), transparent 80%);
}

.panel-header > strong { color: #e9f6ff; font-size: 15px; font-weight: 700; }
.panel-header > strong small { color: #6f97ba; font-size: 10px; font-weight: 500; }
.panel-icon { display: inline-flex; align-items: center; justify-content: center; color: #82b8e1; border: 0; background: transparent; cursor: pointer; }.panel-icon:hover { color: #fff; }
.panel-live { display: inline-flex; align-items: center; gap: 5px; color: #78dba3; font-size: 10px; }.panel-live::before { width: 6px; height: 6px; border-radius: 50%; background: #4ad789; box-shadow: 0 0 8px #4ad789; content: ''; }

.line-panel { display: grid; grid-template-rows: auto minmax(0, 1fr) 38px; }
.line-list { display: grid; align-content: start; padding: 4px 14px 0; }
.line-row { display: grid; min-height: 76px; grid-template-columns: 37px minmax(0, 1fr) 57px; align-items: center; gap: 9px; border-bottom: 1px solid rgb(37 91 135 / 42%); }
.line-symbol { display: inline-flex; width: 34px; height: 34px; align-items: center; justify-content: center; color: #63c8fa; border: 1px solid #246596; border-radius: 50%; background: #0a2b48; }.line-symbol.is-green { color: #73d59e; border-color: #286c52; background: #112e29; }.line-symbol.is-amber { color: #efbc67; border-color: #875f2b; background: #332617; }.line-symbol.is-red { color: #ee8490; border-color: #743b48; background: #351923; }.line-symbol.is-cyan { color: #68d5e5; border-color: #28777f; background: #10343d; }
.line-copy { display: grid; min-width: 0; gap: 3px; }.line-copy b { color: #7ea4c5; font-size: 9px; font-weight: 500; }.line-copy strong { overflow: hidden; color: #e4f4ff; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }.line-copy span { overflow: hidden; color: #7390ab; font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.line-rate { display: flex; justify-content: flex-end; }.line-rate i { position: relative; display: inline-flex; width: 48px; height: 48px; align-items: center; justify-content: center; border: 4px solid #1b4265; border-radius: 50%; font-style: normal; }.line-rate i::after { position: absolute; inset: -4px; border: 4px solid transparent; border-top-color: #4cc897; border-right-color: #4cc897; border-radius: 50%; content: ''; transform: rotate(calc(var(--rate) * 1.8deg - 90deg)); }.line-rate em { position: relative; z-index: 1; color: #d9efff; font-size: 10px; font-style: normal; font-weight: 700; }
.line-footer { display: flex; align-items: center; justify-content: space-between; padding: 0 15px; color: #7e9bb5; border-top: 1px solid #173e64; font-size: 10px; }.line-footer b { color: #6dd5a1; font-size: 12px; }

.schedule-panel { min-width: 0; }
.schedule-tools { display: flex; align-items: center; gap: 10px; color: #87a8c7; font-size: 10px; white-space: nowrap; }.schedule-tools button { display: inline-flex; padding: 0; color: #95b9d6; border: 0; background: transparent; cursor: pointer; }.schedule-tools > b { padding: 5px 12px; color: #e9f7ff; border: 1px solid #2876b1; border-radius: 3px; background: #174d83; }.schedule-legend { display: flex; align-items: center; gap: 8px; margin-left: 8px; color: #83a2bf; font-style: normal; }.schedule-legend em { width: 9px; height: 7px; margin-right: -5px; border-radius: 2px; background: #2f83dd; }.schedule-legend em.is-green { background: #47b973; }.schedule-legend em.is-muted { background: #8292a8; }.schedule-legend em.is-violet { background: #9a84c1; }
.schedule-board { position: relative; padding: 9px 14px 10px; }
.schedule-ruler, .schedule-row { display: grid; grid-template-columns: 78px minmax(0, 1fr); }.schedule-ruler { height: 29px; color: #6f96b7; border-bottom: 1px solid #194266; font-size: 9px; }.schedule-ruler > span { padding: 7px 8px; }.schedule-ruler > div { display: grid; grid-template-columns: repeat(12, 1fr); }.schedule-ruler b { padding-top: 7px; border-left: 1px solid #143755; font-weight: 500; text-align: center; }
.schedule-row { height: 63px; }.schedule-row > strong { display: flex; align-items: center; padding-left: 8px; color: #d0e6f6; border-bottom: 1px solid rgb(29 75 112 / 75%); font-size: 11px; }.schedule-track { position: relative; display: grid; grid-template-columns: repeat(12, 1fr); border-bottom: 1px solid rgb(29 75 112 / 75%); }.schedule-track > i { border-left: 1px solid rgb(26 67 101 / 72%); }
.schedule-task { position: absolute; top: 8px; left: calc((var(--start) - 1) * 8.333% + 4px); display: grid; width: calc(var(--span) * 8.333% - 8px); height: 47px; grid-template-columns: minmax(0, 1fr) auto; grid-template-rows: 14px 14px 13px; padding: 4px 7px; border: 1px solid #347fc8; border-radius: 4px; background: linear-gradient(100deg, #175690, #1a638c); box-shadow: inset 0 1px 0 rgb(255 255 255 / 12%), 0 3px 8px rgb(0 0 0 / 18%); box-sizing: border-box; }.schedule-task b, .schedule-task small, .schedule-task em { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.schedule-task b { color: #f2faff; font-size: 9px; font-weight: 650; }.schedule-task small { color: #d1eaff; font-size: 9px; }.schedule-task em { color: #bcd9ed; font-size: 8px; font-style: normal; }.schedule-task > i { grid-column: 2; grid-row: 1 / 4; align-self: center; color: #edf8ff; border: 0; font-size: 10px; font-style: normal; }.schedule-task.is-green { border-color: #3c986d; background: linear-gradient(100deg, #216a4f, #237453); }.schedule-task.is-violet { border-color: #7159c1; background: linear-gradient(100deg, #4a358d, #513aa1); }.schedule-task.is-amber { border-color: #a67531; background: linear-gradient(100deg, #795020, #8a5e28); }.schedule-task.is-cyan { border-color: #2d8d9a; background: linear-gradient(100deg, #155f67, #18747c); }.schedule-task.is-empty { border-color: #657c9a; border-style: dashed; background: rgb(29 60 91 / 43%); }
.schedule-now { position: absolute; top: 39px; bottom: 11px; left: 39.5%; width: 1px; background: #e54860; box-shadow: 0 0 9px #ec4962; }.schedule-now span { position: absolute; top: -25px; left: -18px; padding: 3px 5px; color: #fff; background: #d63f57; font-size: 9px; white-space: nowrap; }

.right-stack { display: grid; min-height: 0; grid-template-rows: 253px minmax(0, 1fr); gap: 14px; }.progress-panel { position: relative; }.progress-body { position: relative; display: grid; height: calc(100% - 48px); grid-template-columns: 1.16fr .92fr; align-items: center; padding: 2px 13px 8px; overflow: hidden; }.progress-body ul { position: relative; z-index: 1; display: grid; gap: 16px; margin: 0; padding: 0; list-style: none; }.progress-body li { display: grid; grid-template-columns: 8px minmax(0, 1fr) auto; align-items: center; gap: 7px; color: #a8c1d6; font-size: 11px; }.progress-body li i { width: 8px; height: 8px; border-radius: 2px; background: #3d91fa; }.progress-body li i.is-green { background: #68c98b; }.progress-body li i.is-muted { background: #91a4b7; }.progress-body li b { color: #ecf8ff; font-size: 14px; }.factory-ghost { position: absolute; right: 10px; bottom: 4px; width: 168px; height: 79px; opacity: .46; transform: perspective(190px) rotateX(51deg) rotateZ(-32deg); }.factory-ghost::before, .factory-ghost::after { position: absolute; inset: 5px; border: 1px solid #268fc4; box-shadow: 0 0 14px rgb(44 170 237 / 35%), inset 0 0 14px rgb(44 170 237 / 16%); content: ''; }.factory-ghost::after { inset: 23px 37px; border-color: #65c7ee; }.factory-ghost i { position: absolute; width: 30px; height: 30px; border: 1px solid #319bd0; background: rgb(38 130 189 / 13%); box-shadow: inset 0 0 12px rgb(81 193 241 / 22%); }.factory-ghost i:nth-child(1) { top: 3px; left: 9px; }.factory-ghost i:nth-child(2) { right: 9px; top: 9px; width: 39px; height: 39px; }.factory-ghost i:nth-child(3) { bottom: 4px; left: 36px; width: 43px; height: 25px; }.factory-ghost i:nth-child(4) { right: 42px; bottom: 0; width: 22px; height: 22px; }.factory-ghost i:nth-child(5) { top: 26px; left: 70px; width: 16px; height: 16px; }
.production-trend-panel { min-height: 0; }.period-tabs { display: flex; gap: 12px; align-items: center; color: #7599b9; font-size: 10px; }.period-tabs b { padding: 4px 10px; color: #e8f6ff; border-radius: 3px; background: #194e83; font-weight: 600; }

.bottom-board > .screen-panel { min-height: 0; }.equipment-panel, .efficiency-panel { display: grid; grid-template-rows: auto minmax(0, 1fr); }.alerts-panel { display: grid; grid-template-rows: auto minmax(0, 1fr); border-top-color: #d09a3f; }.quality-panel { display: grid; grid-template-rows: auto minmax(0, 1fr); }
.more-text { display: inline-flex; align-items: center; gap: 2px; color: #86abd0; border: 0; background: transparent; font-size: 10px; cursor: pointer; }.more-text:hover { color: #fff; }
.alert-list { display: grid; padding: 1px 14px 6px; }.alert-row { display: grid; min-width: 0; grid-template-columns: 25px minmax(0, 1fr) 51px 32px; align-items: center; gap: 7px; border-bottom: 1px solid rgb(29 75 112 / 69%); }.alert-row > span { display: inline-flex; width: 21px; height: 21px; align-items: center; justify-content: center; color: #f1b55d; border: 1px solid #705025; border-radius: 3px; background: #2e2416; }.alert-row.is-red > span { color: #f27c89; border-color: #753a49; background: #351923; }.alert-row.is-blue > span { color: #6ac8fa; border-color: #256d97; background: #103049; }.alert-row strong { overflow: hidden; color: #d9edf9; font-size: 10px; font-weight: 550; text-overflow: ellipsis; white-space: nowrap; }.alert-row em { overflow: hidden; color: #85a3bd; font-size: 9px; font-style: normal; text-align: right; text-overflow: ellipsis; white-space: nowrap; }.alert-row b { color: #e7f5ff; font-size: 10px; text-align: right; }
.quality-grid { display: grid; height: 100%; grid-template-columns: 1fr 1fr; gap: 1px; background: #173e61; }.quality-grid article { display: grid; min-width: 0; grid-template-columns: 42px minmax(0, 1fr); align-items: center; gap: 7px; padding: 12px 13px; background: #081b32; }.quality-grid article > span { display: inline-flex; width: 38px; height: 38px; align-items: center; justify-content: center; color: #73c5e8; border: 1px solid #236683; border-radius: 5px; background: #10394d; }.quality-grid article.is-red > span { color: #ef8b94; border-color: #723946; background: #43222a; }.quality-grid article.is-violet > span { color: #ab92ed; border-color: #55458b; background: #2e2759; }.quality-grid article.is-green > span { color: #7bd9a0; border-color: #307054; background: #173c2c; }.quality-grid article.is-amber > span { color: #f0bf68; border-color: #805d2e; background: #3c2d18; }.quality-grid article div { display: grid; min-width: 0; gap: 2px; }.quality-grid article b { overflow: hidden; color: #abc3d7; font-size: 10px; font-weight: 550; text-overflow: ellipsis; white-space: nowrap; }.quality-grid article strong { overflow: hidden; color: #eff8ff; font-size: 20px; line-height: 1.1; text-overflow: ellipsis; white-space: nowrap; }.quality-grid article em { overflow: hidden; color: #7d9ab3; font-size: 8px; font-style: normal; text-overflow: ellipsis; white-space: nowrap; }.quality-grid article em.is-up { color: #6fd59b; }.quality-grid article em.is-down { color: #ef7d88; }

/* The canvas is scaled down on common displays, so every information tier needs a larger native size. */
.big-screen-page { font-weight: 550; }
.header-crumb { font-size: 16px; }
.header-title strong { font-size: 30px; font-weight: 780; }
.header-title span { font-size: 14px; font-weight: 650; }
.header-date { font-size: 14px; font-weight: 600; }
.header-date b { font-size: 15px; font-weight: 700; }
.header-exit { font-size: 13px; font-weight: 650; }
.screen-error { font-size: 14px; font-weight: 600; }

.metric-content > span { font-size: 14px; font-weight: 700; }
.metric-content strong { font-size: 32px; font-weight: 780; }
.metric-content strong small { font-size: 13px; font-weight: 650; }
.metric-content em { font-size: 11px; font-weight: 650; }
.metric-content em i { font-size: 13px; }

.panel-header > strong { font-size: 17px; font-weight: 750; }
.panel-header > strong small { font-size: 12px; font-weight: 600; }
.panel-live { font-size: 12px; font-weight: 650; }
.line-copy b { font-size: 11px; font-weight: 600; }
.line-copy strong { font-size: 14px; font-weight: 700; }
.line-copy span { font-size: 11px; font-weight: 550; }
.line-rate em { font-size: 12px; font-weight: 750; }
.line-footer { font-size: 12px; font-weight: 600; }
.line-footer b { font-size: 14px; }

.schedule-tools { font-size: 12px; font-weight: 600; }
.schedule-ruler { font-size: 11px; }
.schedule-ruler b { font-weight: 600; }
.schedule-row { height: 66px; }
.schedule-row > strong { font-size: 13px; font-weight: 700; }
.schedule-task { top: 7px; height: 52px; grid-template-rows: 16px 16px 14px; padding: 3px 7px; }
.schedule-task b { font-size: 11px; font-weight: 700; }
.schedule-task small { font-size: 10px; font-weight: 600; }
.schedule-task em { font-size: 10px; font-weight: 550; }
.schedule-task > i { font-size: 12px; font-weight: 700; }
.schedule-now span { font-size: 11px; font-weight: 700; }

.progress-body li { font-size: 13px; font-weight: 600; }
.progress-body li b { font-size: 16px; font-weight: 750; }
.period-tabs { font-size: 12px; font-weight: 600; }
.more-text { font-size: 12px; font-weight: 650; }
.alert-row strong { font-size: 12px; font-weight: 650; }
.alert-row em { font-size: 10px; font-weight: 600; }
.alert-row b { font-size: 12px; font-weight: 700; }
.quality-grid article b { font-size: 12px; font-weight: 650; }
.quality-grid article strong { font-size: 23px; font-weight: 780; }
.quality-grid article em { font-size: 10px; font-weight: 600; }

/* KPI cards use saturated production states, fuller icon medallions, and visible telemetry decoration. */
.metric-card {
  --metric: #5b9fff;
  --metric-deep: #0e438e;
  --metric-glow: rgb(63 146 255 / 42%);
  grid-template-columns: 78px minmax(0, 1fr);
  gap: 15px;
  padding: 10px 16px;
  border-color: color-mix(in srgb, var(--metric) 55%, #24476e);
  background:
    radial-gradient(circle at 17% 48%, color-mix(in srgb, var(--metric) 36%, transparent) 0 28%, transparent 50%),
    linear-gradient(105deg, var(--metric-deep) 0%, color-mix(in srgb, var(--metric-deep) 74%, #071936) 43%, #071a35 100%);
  box-shadow: inset 0 0 34px var(--metric-glow), inset 0 1px 0 rgb(255 255 255 / 11%), 0 5px 14px rgb(0 0 0 / 22%);
}

.metric-card::before {
  top: -12px;
  left: 2px;
  width: 112px;
  height: 112px;
  border-color: color-mix(in srgb, var(--metric) 34%, transparent);
  background: radial-gradient(circle, color-mix(in srgb, var(--metric) 17%, transparent) 0 48%, transparent 49%);
  box-shadow: 0 0 38px var(--metric-glow), inset 0 0 22px color-mix(in srgb, var(--metric) 16%, transparent);
}

.metric-card::after {
  right: 13px;
  bottom: 9px;
  left: 13px;
  height: 2px;
  background: linear-gradient(90deg, transparent, color-mix(in srgb, var(--metric) 70%, transparent), transparent);
  box-shadow: 0 0 8px color-mix(in srgb, var(--metric) 38%, transparent);
}

.metric-card.is-amber { --metric: #ffb14f; --metric-deep: #704018; --metric-glow: rgb(255 161 58 / 39%); }
.metric-card.is-teal { --metric: #54d8ae; --metric-deep: #105c50; --metric-glow: rgb(63 220 170 / 38%); }
.metric-card.is-violet { --metric: #a884ff; --metric-deep: #46317c; --metric-glow: rgb(153 113 255 / 39%); }
.metric-card.is-cyan { --metric: #4ad9ea; --metric-deep: #0a5b76; --metric-glow: rgb(40 207 232 / 37%); }
.metric-card.is-red { --metric: #ff8090; --metric-deep: #6d2b3d; --metric-glow: rgb(252 93 117 / 38%); }

.metric-icon {
  width: 74px;
  height: 74px;
  color: color-mix(in srgb, var(--metric) 70%, white);
  border: 2px solid color-mix(in srgb, var(--metric) 54%, transparent);
  background:
    radial-gradient(circle at 34% 28%, rgb(255 255 255 / 18%), transparent 22%),
    color-mix(in srgb, var(--metric) 25%, #071b35);
  box-shadow: inset 0 0 23px color-mix(in srgb, var(--metric) 30%, transparent), 0 0 28px color-mix(in srgb, var(--metric) 26%, transparent), 0 0 0 8px color-mix(in srgb, var(--metric) 7%, transparent);
}

.metric-icon::before {
  position: absolute;
  top: 8px;
  right: 9px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--metric) 58%, white);
  box-shadow: 0 0 11px var(--metric);
  content: '';
}

.metric-wave {
  position: absolute;
  z-index: 0;
  bottom: 8px;
  left: 14px;
  width: 122px;
  height: 42px;
  opacity: .72;
  pointer-events: none;
}

.metric-wave::before {
  position: absolute;
  right: 0;
  bottom: 3px;
  left: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, color-mix(in srgb, var(--metric) 66%, transparent), transparent);
  content: '';
}

.metric-wave i {
  position: absolute;
  width: 21px;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--metric));
  box-shadow: 0 0 6px var(--metric);
  transform-origin: left center;
}

.metric-wave i:nth-child(1) { top: 31px; left: 0; transform: rotate(-32deg); }
.metric-wave i:nth-child(2) { top: 20px; left: 18px; transform: rotate(33deg); }
.metric-wave i:nth-child(3) { top: 31px; left: 36px; transform: rotate(-44deg); }
.metric-wave i:nth-child(4) { top: 16px; left: 54px; transform: rotate(28deg); }
.metric-wave i:nth-child(5) { top: 25px; left: 72px; transform: rotate(-28deg); }
.metric-wave i:nth-child(6) { top: 16px; left: 90px; transform: rotate(40deg); }
.metric-wave i:nth-child(7) { top: 28px; left: 106px; transform: rotate(-37deg); }
.metric-wave i:nth-child(8) { top: 18px; left: 118px; transform: rotate(12deg); }

.metric-spark {
  right: 10px;
  bottom: 18px;
  width: 66px;
  height: 36px;
  opacity: .7;
}

.metric-spark i { width: 12px; height: 2px; }
.metric-spark i:nth-child(1) { top: 27px; left: 0; }.metric-spark i:nth-child(2) { top: 22px; left: 8px; }.metric-spark i:nth-child(3) { top: 25px; left: 16px; }.metric-spark i:nth-child(4) { top: 15px; left: 24px; }.metric-spark i:nth-child(5) { top: 20px; left: 32px; }.metric-spark i:nth-child(6) { top: 8px; left: 40px; }.metric-spark i:nth-child(7) { top: 15px; left: 48px; }.metric-spark i:nth-child(8) { top: 10px; left: 56px; }

/* Pull the canvas back to a darker navy field and keep KPI colors as restrained accents. */
.big-screen-page {
  background:
    radial-gradient(circle at 50% -10%, rgb(15 83 158 / 14%), transparent 34%),
    linear-gradient(180deg, #020a18 0%, #01060e 66%, #020a17 100%);
}

.big-screen-page::before { opacity: .38; }
.big-screen-page::after { border-color: rgb(42 122 202 / 16%); box-shadow: inset 0 0 140px rgb(0 33 76 / 24%); }

.metric-card {
  --metric-deep: #0a203d;
  --metric-glow: rgb(63 146 255 / 18%);
  border-color: color-mix(in srgb, var(--metric) 32%, #1d3c5d);
  background:
    radial-gradient(circle at 17% 48%, color-mix(in srgb, var(--metric) 13%, transparent) 0 27%, transparent 48%),
    linear-gradient(105deg, #0a2547 0%, #071a32 46%, #061426 100%);
  box-shadow: inset 0 0 22px var(--metric-glow), inset 0 1px 0 rgb(255 255 255 / 6%), 0 5px 14px rgb(0 0 0 / 24%);
}

.metric-card.is-amber { --metric-deep: #172033; --metric-glow: rgb(255 161 58 / 16%); }
.metric-card.is-teal { --metric-deep: #092631; --metric-glow: rgb(63 220 170 / 16%); }
.metric-card.is-violet { --metric-deep: #151d39; --metric-glow: rgb(153 113 255 / 16%); }
.metric-card.is-cyan { --metric-deep: #082633; --metric-glow: rgb(40 207 232 / 16%); }
.metric-card.is-red { --metric-deep: #221d31; --metric-glow: rgb(252 93 117 / 16%); }

.metric-card::before {
  border-color: color-mix(in srgb, var(--metric) 20%, transparent);
  background: radial-gradient(circle, color-mix(in srgb, var(--metric) 8%, transparent) 0 48%, transparent 49%);
  box-shadow: 0 0 24px var(--metric-glow), inset 0 0 16px color-mix(in srgb, var(--metric) 8%, transparent);
}

.metric-card::after { background: linear-gradient(90deg, transparent, color-mix(in srgb, var(--metric) 44%, transparent), transparent); box-shadow: 0 0 6px color-mix(in srgb, var(--metric) 20%, transparent); }
.metric-icon { border-color: color-mix(in srgb, var(--metric) 43%, transparent); background: radial-gradient(circle at 34% 28%, rgb(255 255 255 / 12%), transparent 22%), color-mix(in srgb, var(--metric) 16%, #07182d); box-shadow: inset 0 0 18px color-mix(in srgb, var(--metric) 18%, transparent), 0 0 18px color-mix(in srgb, var(--metric) 14%, transparent), 0 0 0 7px color-mix(in srgb, var(--metric) 4%, transparent); }
.metric-wave { opacity: .43; }
.metric-spark { opacity: .42; }

/* Open industrial framing: use short angular marks instead of closed rectangles. */
.screen-header {
  border-bottom: 0;
  background: linear-gradient(90deg, rgb(3 15 31 / 82%), rgb(4 20 41 / 34%) 50%, rgb(3 15 31 / 82%));
  box-shadow: none;
}

.screen-header::before,
.screen-header::after { display: none; }

.header-title { position: relative; isolation: isolate; }

.header-title::before,
.header-title::after {
  position: absolute;
  top: 19px;
  width: 172px;
  height: 21px;
  background: linear-gradient(90deg, transparent, rgb(49 153 228 / 72%), rgb(18 79 146 / 20%));
  clip-path: polygon(0 47%, 42% 47%, 55% 0, 100% 0, 82% 46%, 100% 100%, 55% 100%, 42% 54%, 0 54%);
  content: '';
  opacity: .78;
  pointer-events: none;
}

.header-title::before { right: calc(100% + 27px); }
.header-title::after { left: calc(100% + 27px); transform: scaleX(-1); }

.metric-strip::before,
.metric-strip::after { display: none; }

.metric-card {
  border: 0;
  border-radius: 0;
  overflow: hidden;
  background: linear-gradient(90deg, rgb(5 23 46 / 38%), rgb(2 13 27 / 8%));
  box-shadow: none;
  clip-path: polygon(0 15px, 15px 0, calc(100% - 25px) 0, 100% 17px, 100% calc(100% - 12px), calc(100% - 17px) 100%, 26px 100%, 10px calc(100% - 10px), 0 calc(100% - 10px));
}

.metric-card::before {
  top: 0;
  left: 0;
  width: 106px;
  height: 74px;
  border: 0;
  border-radius: 0;
  background:
    linear-gradient(90deg, var(--metric), transparent) 15px 0 / 82px 1px no-repeat,
    linear-gradient(180deg, var(--metric), transparent) 0 15px / 1px 48px no-repeat,
    linear-gradient(135deg, transparent 47%, var(--metric) 48% 53%, transparent 54%) 0 0 / 18px 18px no-repeat,
    linear-gradient(90deg, color-mix(in srgb, var(--metric) 18%, transparent), transparent) 0 31px / 70px 1px no-repeat;
  box-shadow: none;
  opacity: .82;
}

.metric-card::after {
  right: 0;
  bottom: 0;
  left: auto;
  width: 94px;
  height: 25px;
  background:
    linear-gradient(90deg, transparent, var(--metric)) right bottom / 70px 1px no-repeat,
    linear-gradient(180deg, transparent, var(--metric)) right bottom / 1px 18px no-repeat,
    linear-gradient(135deg, transparent 47%, var(--metric) 48% 53%, transparent 54%) right bottom / 18px 18px no-repeat;
  box-shadow: none;
  opacity: .72;
}

.metric-icon { box-shadow: inset 0 0 18px color-mix(in srgb, var(--metric) 18%, transparent), 0 0 15px color-mix(in srgb, var(--metric) 10%, transparent); }
.metric-wave { opacity: .3; }
.metric-spark { opacity: .3; }

.screen-panel {
  position: relative;
  isolation: isolate;
  border: 0;
  border-radius: 0;
  background: linear-gradient(145deg, rgb(5 22 43 / 42%), rgb(2 12 27 / 18%));
  box-shadow: none;
  clip-path: polygon(0 15px, 15px 0, calc(100% - 24px) 0, 100% 16px, 100% calc(100% - 17px), calc(100% - 17px) 100%, 25px 100%, 0 calc(100% - 15px));
}

.screen-panel::before {
  position: absolute;
  z-index: 0;
  inset: 0;
  background:
    linear-gradient(90deg, #2f8cd2, transparent) 15px 0 / 102px 1px no-repeat,
    linear-gradient(180deg, #2f8cd2, transparent) 0 15px / 1px 70px no-repeat,
    linear-gradient(135deg, transparent 47%, #2f8cd2 48% 53%, transparent 54%) 0 0 / 18px 18px no-repeat,
    linear-gradient(90deg, transparent, #236da8) right 15px bottom / 100px 1px no-repeat,
    linear-gradient(180deg, transparent, #236da8) right bottom 15px / 1px 70px no-repeat,
    linear-gradient(135deg, transparent 47%, #236da8 48% 53%, transparent 54%) right bottom / 18px 18px no-repeat;
  content: '';
  opacity: .76;
  pointer-events: none;
}

.screen-panel::after {
  position: absolute;
  z-index: 0;
  top: 0;
  right: 30px;
  width: 34px;
  height: 8px;
  background: #2b83c6;
  clip-path: polygon(18% 0, 100% 0, 82% 100%, 0 100%);
  content: '';
  opacity: .72;
}

.screen-panel > * { position: relative; z-index: 1; }

.panel-header {
  position: relative;
  border-bottom: 0;
  background: linear-gradient(90deg, rgb(14 55 93 / 34%), transparent 54%);
}

.panel-header::after {
  position: absolute;
  bottom: 0;
  left: 14px;
  width: 158px;
  height: 1px;
  background: linear-gradient(90deg, #2a8fd2, rgb(42 143 210 / 28%), transparent);
  clip-path: polygon(0 0, 88% 0, 100% 100%, 0 100%);
  content: '';
}

/* Increase panel separation without restoring a closed rectangular border. */
.big-screen-page {
  background:
    radial-gradient(circle at 50% -12%, rgb(11 64 124 / 12%), transparent 32%),
    linear-gradient(180deg, #01060d 0%, #000307 64%, #01060d 100%);
}

.screen-panel {
  --panel-edge-bright: #51bfff;
  --panel-edge-deep: #1c6fae;
  background:
    radial-gradient(ellipse at 2% 0%, rgb(30 116 191 / 18%), transparent 38%),
    radial-gradient(ellipse at 100% 100%, rgb(14 69 125 / 19%), transparent 37%),
    linear-gradient(145deg, rgb(7 31 60 / 84%), rgb(3 16 35 / 73%));
  box-shadow: inset 0 0 36px rgb(20 100 175 / 10%), 0 7px 20px rgb(0 0 0 / 16%);
}

.screen-panel::before {
  background:
    radial-gradient(ellipse 148px 102px at 0 0, transparent 0 61px, var(--panel-edge-bright) 62px 64px, transparent 65px) 0 0 / 156px 110px no-repeat,
    radial-gradient(ellipse 148px 102px at 100% 100%, transparent 0 61px, var(--panel-edge-deep) 62px 64px, transparent 65px) 100% 100% / 156px 110px no-repeat,
    linear-gradient(90deg, var(--panel-edge-bright), transparent) 15px 0 / 154px 2px no-repeat,
    linear-gradient(180deg, var(--panel-edge-bright), transparent) 0 15px / 2px 88px no-repeat,
    linear-gradient(135deg, transparent 45%, var(--panel-edge-bright) 46% 54%, transparent 55%) 0 0 / 20px 20px no-repeat,
    linear-gradient(90deg, transparent, var(--panel-edge-deep)) right 15px bottom / 152px 2px no-repeat,
    linear-gradient(180deg, transparent, var(--panel-edge-deep)) right bottom 15px / 2px 88px no-repeat,
    linear-gradient(135deg, transparent 45%, var(--panel-edge-deep) 46% 54%, transparent 55%) right bottom / 20px 20px no-repeat;
  opacity: .94;
}

.screen-panel::after {
  top: 0;
  right: 38px;
  width: 118px;
  height: 11px;
  background: linear-gradient(90deg, transparent, #216ea9 18%, #58c5ff 54%, transparent 100%);
  clip-path: polygon(0 55%, 17% 0, 100% 0, 83% 100%, 14% 100%);
  opacity: .92;
}

.panel-header {
  background: linear-gradient(90deg, rgb(14 62 106 / 55%), rgb(8 36 67 / 20%) 44%, transparent 72%);
}

.panel-header::after {
  left: 15px;
  width: 218px;
  height: 2px;
  background: linear-gradient(90deg, var(--panel-edge-bright), rgb(55 159 222 / 46%), transparent);
  box-shadow: 0 0 8px rgb(56 169 238 / 24%);
}

/* Gradients live on the open border itself: bright at two corners, quiet away from them. */
.screen-panel {
  background:
    radial-gradient(ellipse 270px 190px at 0 0, rgb(34 125 202 / 24%), transparent 100%),
    radial-gradient(ellipse 250px 180px at 100% 100%, rgb(24 105 181 / 20%), transparent 100%),
    linear-gradient(145deg, rgb(4 18 34 / 88%), rgb(1 8 16 / 83%));
  box-shadow: inset 0 0 28px rgb(46 132 205 / 6%), 0 7px 20px rgb(0 0 0 / 14%);
}

.screen-panel::before {
  background:
    linear-gradient(90deg, var(--panel-edge-bright), rgb(66 167 229 / 38%) 40%, transparent 100%) 15px 0 / 244px 2px no-repeat,
    linear-gradient(180deg, var(--panel-edge-bright), rgb(66 167 229 / 38%) 40%, transparent 100%) 0 15px / 2px 164px no-repeat,
    linear-gradient(135deg, transparent 45%, var(--panel-edge-bright) 46% 54%, transparent 55%) 0 0 / 20px 20px no-repeat,
    linear-gradient(90deg, transparent 0%, rgb(41 125 190 / 35%) 56%, var(--panel-edge-deep) 100%) right bottom / 324px 5px no-repeat,
    linear-gradient(180deg, transparent 0%, rgb(41 125 190 / 35%) 56%, var(--panel-edge-deep) 100%) right bottom / 5px 214px no-repeat,
    linear-gradient(135deg, transparent 43%, var(--panel-edge-deep) 44% 56%, transparent 57%) right bottom / 24px 24px no-repeat;
  opacity: 1;
}

.screen-panel::after { display: none; }

.panel-header {
  background: linear-gradient(90deg, rgb(18 72 118 / 29%), rgb(8 37 66 / 12%) 44%, transparent 72%);
}

.panel-header::after {
  width: 174px;
  height: 1px;
  background: linear-gradient(90deg, rgb(76 180 245 / 82%), rgb(55 146 208 / 28%), transparent);
  box-shadow: none;
}

/* Use the KPI palette at a restrained opacity so work orders stay readable, not vivid. */
.schedule-task {
  --schedule-tone: #5b9fff;
  border-color: color-mix(in srgb, var(--schedule-tone) 44%, #6f8293);
  background:
    linear-gradient(105deg,
      color-mix(in srgb, var(--schedule-tone) 33%, #22364e),
      color-mix(in srgb, var(--schedule-tone) 22%, #17293d));
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 12%), 0 3px 8px rgb(0 0 0 / 13%);
}

.schedule-task small { color: #d9e2e8; }
.schedule-task em { color: #c4d0d9; }
.schedule-task > i { color: #edf2f5; text-shadow: 0 1px 3px rgb(6 22 36 / 32%); }
.schedule-task.is-green { --schedule-tone: #54d8ae; }
.schedule-task.is-violet { --schedule-tone: #a884ff; }
.schedule-task.is-amber { --schedule-tone: #ffb14f; }
.schedule-task.is-cyan { --schedule-tone: #4ad9ea; }
.schedule-task.is-empty { border-color: rgb(119 133 144 / 68%); background: rgb(35 48 59 / 45%); }

.schedule-legend em { background: #6e96bf; }
.schedule-legend em.is-green { background: #71aa91; }
.schedule-legend em.is-amber { background: #b9915a; }
.schedule-legend em.is-violet { background: #8e7bb5; }
.schedule-now { background: #9a626d; box-shadow: 0 0 7px rgb(154 98 109 / 42%); }
.schedule-now span { background: #805661; }

/* Keep the date centered and make every visible task tone match the legend. */
.schedule-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 14px;
}

.schedule-date,
.schedule-center,
.schedule-view {
  display: flex;
  align-items: center;
  white-space: nowrap;
}

.schedule-date { gap: 9px; color: #9cc5e8; font-size: 12px; font-weight: 700; }
.schedule-date button { display: inline-flex; padding: 0; color: #95b9d6; border: 0; background: transparent; cursor: pointer; }
.schedule-center { justify-self: center; gap: 20px; }
.schedule-view { gap: 10px; }
.schedule-view > b { padding: 5px 12px; color: #e9f7ff; border: 1px solid #2876b1; border-radius: 3px; background: #174d83; }
.schedule-legend { justify-self: end; margin-left: 0; }

/* Keep the complete doughnut inside the available panel height and let the factory scene sit behind data. */
.progress-body {
  grid-template-columns: 1.1fr .9fr;
  align-items: center;
  padding: 3px 13px 8px;
  background:
    radial-gradient(ellipse at 34% 100%, rgb(48 151 224 / 16%), transparent 53%),
    linear-gradient(0deg, rgb(15 85 141 / 10%), transparent 46%);
}

.progress-body::before {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 0;
  height: 114px;
  background:
    linear-gradient(30deg, transparent 49%, rgb(77 180 241 / 20%) 50%, transparent 51%) 0 0 / 34px 19px,
    linear-gradient(-30deg, transparent 49%, rgb(77 180 241 / 17%) 50%, transparent 51%) 0 0 / 34px 19px,
    linear-gradient(90deg, transparent 0 5%, rgb(66 160 221 / 16%) 5.5% 6%, transparent 6.5% 100%) 0 100% / 66px 58px,
    linear-gradient(90deg, rgb(47 144 210 / 15%), transparent 76%);
  content: '';
  opacity: .78;
  pointer-events: none;
}

.progress-chart { position: relative; z-index: 1; height: 188px; min-width: 0; }
.progress-body ul { z-index: 2; }
.factory-ghost {
  z-index: 0;
  right: 0;
  bottom: -14px;
  width: 278px;
  height: 128px;
  opacity: .45;
  background:
    linear-gradient(30deg, transparent 48%, rgb(76 180 238 / 16%) 49% 51%, transparent 52%) 0 0 / 40px 22px,
    linear-gradient(-30deg, transparent 48%, rgb(76 180 238 / 14%) 49% 51%, transparent 52%) 0 0 / 40px 22px,
    linear-gradient(90deg, rgb(77 182 241 / 16%), transparent 76%) 16px 15px / 204px 1px no-repeat,
    linear-gradient(180deg, rgb(77 182 241 / 14%), transparent 76%) 16px 15px / 1px 94px no-repeat;
}

.factory-ghost::before,
.factory-ghost::after {
  border-color: rgb(67 172 234 / 78%);
  box-shadow: 0 0 16px rgb(44 170 237 / 24%), inset 0 0 14px rgb(44 170 237 / 13%);
}

.factory-ghost i {
  border-color: rgb(66 167 226 / 74%);
  background: rgb(38 130 189 / 11%);
  box-shadow: inset 0 0 12px rgb(81 193 241 / 17%);
}

.factory-ghost i:nth-child(1) { top: 12px; left: 17px; width: 48px; height: 34px; }
.factory-ghost i:nth-child(2) { top: 2px; right: 17px; width: 56px; height: 56px; }
.factory-ghost i:nth-child(3) { bottom: 12px; left: 57px; width: 58px; height: 33px; }
.factory-ghost i:nth-child(4) { right: 67px; bottom: 8px; width: 27px; height: 27px; }
.factory-ghost i:nth-child(5) { top: 40px; left: 116px; width: 22px; height: 22px; }
.factory-ghost i:nth-child(6) { top: 48px; left: 10px; width: 68px; height: 14px; border-left: 0; border-right: 0; }
.factory-ghost i:nth-child(7) { top: 61px; left: 137px; width: 73px; height: 13px; border-left: 0; border-right: 0; }
.factory-ghost i:nth-child(8) { top: 21px; left: 83px; width: 17px; height: 17px; border-radius: 50%; box-shadow: 0 0 13px rgb(86 198 247 / 40%), inset 0 0 9px rgb(81 193 241 / 27%); }
.factory-ghost i:nth-child(9) { right: 27px; bottom: 39px; width: 15px; height: 15px; border-radius: 50%; box-shadow: 0 0 12px rgb(86 198 247 / 38%), inset 0 0 8px rgb(81 193 241 / 24%); }
.factory-ghost i:nth-child(10) { right: 103px; bottom: 7px; width: 93px; height: 14px; border-left: 0; border-right: 0; }

@keyframes spin { to { transform: rotate(360deg); } }
</style>
