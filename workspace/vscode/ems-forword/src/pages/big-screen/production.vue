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

let clockTimer
let refreshTimer

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
const equipmentTotal = computed(() => ['running', 'stopped', 'fault', 'maintenance']
  .reduce((total, key) => total + Number(equipment.value[key] || 0), 0))
const warningCount = computed(() => Number(summary.value.inventoryWarningCount || 0))
const faultCount = computed(() => Number(summary.value.equipmentFaultCount || 0))
const attentionCount = computed(() => warningCount.value + faultCount.value)
const riskCategoryCount = computed(() => [faultCount.value, warningCount.value, todayDefect.value]
  .filter((value) => value > 0).length)
const trendRows = computed(() => trend.value.map((item) => {
  const qualifiedQuantity = Number(item.qualifiedQuantity || 0)
  const defectQuantity = Number(item.defectQuantity || 0)
  const output = qualifiedQuantity + defectQuantity
  return {
    ...item,
    output,
    passRate: output ? Number(((qualifiedQuantity / output) * 100).toFixed(2)) : null
  }
}))
const hasTrendData = computed(() => trendRows.value.some((item) => item.output > 0))
const sevenDayOutput = computed(() => trendRows.value.reduce((total, item) => total + item.output, 0))

const kpis = computed(() => [
  {
    label: '今日报工产量',
    value: formatNumber(todayOutput.value),
    unit: '件',
    description: `合格 ${formatNumber(todayQualified.value)} / 不良 ${formatNumber(todayDefect.value)}`,
    icon: 'factory',
    tone: 'blue'
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
    label: '运行中生产任务',
    value: formatNumber(summary.value.runningTaskCount),
    unit: '项',
    description: '当前正在执行的生产任务',
    icon: 'activity',
    tone: 'cyan'
  },
  {
    label: '待处理生产预警',
    value: formatNumber(attentionCount.value),
    unit: '项',
    description: `设备故障 ${faultCount.value} / 补货预警 ${warningCount.value}`,
    icon: 'alert',
    tone: attentionCount.value > 0 ? 'red' : 'muted'
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
  if (document.fullscreenElement) {
    await document.exitFullscreen?.()
  }
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
    lastUpdated.value = dayjs()
    loadError.value = ''
    failedDataCount.value = 0
    return
  }

  loading.value = true
  loadError.value = ''
  const params = {
    beginDate: dayjs().subtract(6, 'day').format('YYYY-MM-DD'),
    endDate: dayjs().format('YYYY-MM-DD')
  }

  try {
    const results = await Promise.allSettled([
      getDashboardSummary(),
      getProductionTrend(params),
      getQualityStatistics(params),
      getEquipmentStatus()
    ])
    const targets = [summary, trend, quality, equipment]
    const labels = ['生产汇总', '生产趋势', '质量统计', '设备状态']
    const failedLabels = []
    let successCount = 0

    results.forEach((result, index) => {
      if (result.status === 'fulfilled') {
        targets[index].value = result.value.data ?? (index === 1 ? [] : {})
        successCount += 1
      } else {
        failedLabels.push(labels[index])
      }
    })

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
  document.addEventListener('fullscreenchange', handleFullscreenChange)
  window.visualViewport?.addEventListener('resize', updateScale)
})

onBeforeUnmount(() => {
  window.clearInterval(clockTimer)
  window.clearInterval(refreshTimer)
  window.removeEventListener('resize', updateScale)
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
          <strong>生产运营看板</strong>
          <span>产量、质量、设备与预警</span>
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
          <article class="screen-panel trend-panel">
            <header class="panel-header">
              <div>
                <strong>近 7 日生产趋势</strong>
                <span>柱状为报工总量，折线为报工合格率</span>
              </div>
              <aside class="panel-tools">
                <div class="panel-total">
                  <span>7 日累计</span>
                  <b>{{ formatNumber(sevenDayOutput) }} 件</b>
                </div>
                <button class="panel-link" type="button" title="打开生产报工明细" aria-label="打开生产报工明细" @click="openModule('/mfg/work-reports')">
                  <AppIcon name="arrow" :size="20" />
                </button>
              </aside>
            </header>
            <div class="trend-chart-wrap">
              <MesChart :option="trendOption" height="372px" />
              <div v-if="!hasTrendData" class="empty-overlay">
                <AppIcon name="activity" :size="28" />
                <strong>近 7 日暂无报工数据</strong>
                <span>产生生产报工后，趋势会自动显示在这里</span>
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
              <div class="quality-rate">
                <span>质检单合格率</span>
                <strong>{{ formatPercent(inspectionPassRate) }}<small v-if="inspectionPassRate !== null">%</small></strong>
              </div>
              <div class="quality-progress" :class="{ 'is-empty': inspectionPassRate === null }">
                <i :style="{ width: `${qualityProgressWidth}%` }" />
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
    linear-gradient(rgb(61 91 116 / 7%) 1px, transparent 1px),
    linear-gradient(90deg, rgb(61 91 116 / 7%) 1px, transparent 1px),
    linear-gradient(180deg, #07111c 0%, #081522 48%, #060e17 100%);
  background-size: 64px 64px, 64px 64px, auto;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
  letter-spacing: 0;
}

.big-screen-page::after {
  position: absolute;
  inset: 0;
  border: 1px solid rgb(81 133 171 / 18%);
  box-shadow: inset 0 0 120px rgb(0 0 0 / 26%);
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
  background: linear-gradient(180deg, rgb(6 17 29 / 92%), rgb(7 18 29 / 96%));
  font-variant-numeric: tabular-nums;
  transform-origin: top left;
}

.screen-header {
  display: grid;
  height: 84px;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 0 28px;
  border-bottom: 1px solid #29465d;
  background: rgb(8 22 36 / 96%);
  box-shadow: 0 8px 28px rgb(0 0 0 / 20%);
}

.header-branding,
.header-actions,
.header-clock,
.update-status {
  display: flex;
  align-items: center;
}

.header-branding { gap: 15px; }
.header-branding > div { display: grid; gap: 4px; }
.header-branding strong { color: #f3f8fc; font-size: 20px; font-weight: 720; line-height: 1.25; }
.header-branding span { color: #9aafbf; font-size: 17px; line-height: 1.25; }

.header-title { display: grid; justify-items: center; gap: 5px; }
.header-title strong { color: #fff; font-size: 30px; font-weight: 760; line-height: 1.2; }
.header-title span { color: #9cb0bf; font-size: 17px; line-height: 1.25; }

.header-actions { justify-content: flex-end; gap: 10px; }
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
  height: calc(100% - 84px);
  grid-template-rows: 148px minmax(0, 1fr) 292px;
  gap: 18px;
  padding: 22px 24px 24px;
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.kpi-card {
  --accent: #5297d4;
  display: grid;
  min-width: 0;
  grid-template-columns: 76px minmax(0, 1fr);
  align-items: center;
  gap: 19px;
  padding: 20px 24px;
  border: 1px solid #2c475d;
  border-left: 5px solid var(--accent);
  border-radius: 5px;
  background: linear-gradient(105deg, rgb(18 42 61 / 94%), rgb(10 26 40 / 96%));
  box-shadow: 0 8px 22px rgb(0 0 0 / 16%);
}

.kpi-card.is-green { --accent: #4bc58a; }
.kpi-card.is-cyan { --accent: #49b9d0; }
.kpi-card.is-red { --accent: #e76d75; }
.kpi-card.is-muted { --accent: #788c9b; }

.kpi-icon {
  display: inline-flex;
  width: 72px;
  height: 72px;
  align-items: center;
  justify-content: center;
  color: var(--accent);
  border: 1px solid color-mix(in srgb, var(--accent) 45%, #29465d);
  border-radius: 6px;
  background: color-mix(in srgb, var(--accent) 13%, #102435);
}

.kpi-copy { display: grid; min-width: 0; gap: 5px; }
.kpi-copy > span { color: #b9c9d5; font-size: 20px; font-weight: 650; line-height: 1.2; }
.kpi-copy strong { overflow: hidden; color: #fff; font-size: 52px; font-weight: 760; line-height: 1; text-overflow: ellipsis; white-space: nowrap; }
.kpi-copy strong small { margin-left: 7px; color: #9eb0be; font-size: 17px; font-weight: 550; }
.kpi-copy p { overflow: hidden; margin: 0; color: #9bafbd; font-size: 17px; line-height: 1.2; text-overflow: ellipsis; white-space: nowrap; }

.primary-grid {
  display: grid;
  min-height: 0;
  grid-template-columns: minmax(0, 1.72fr) minmax(520px, .78fr);
  gap: 18px;
}

.secondary-grid {
  display: grid;
  min-height: 0;
  grid-template-columns: 1.02fr 1fr 1.18fr;
  gap: 18px;
}

.screen-panel {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #2b465b;
  border-radius: 5px;
  background: rgb(10 27 42 / 94%);
  box-shadow: 0 8px 24px rgb(0 0 0 / 14%);
}

.panel-header {
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 0 22px;
  border-bottom: 1px solid #29445a;
  background: rgb(16 38 56 / 72%);
}

.panel-header.compact { min-height: 68px; }
.panel-header > div { display: grid; gap: 5px; }
.panel-header > div > strong { color: #f2f7fb; font-size: 22px; font-weight: 720; line-height: 1.25; }
.panel-header > div > span { color: #a2b4c1; font-size: 17px; line-height: 1.25; }

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

.trend-chart-wrap { position: relative; height: calc(100% - 72px); padding: 5px 8px 0; }
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

.execution-panel { display: grid; grid-template-rows: auto 1fr auto auto; }
.live-badge { display: inline-flex; align-items: center; gap: 7px; color: #79cfa0; font-size: 16px; }
.live-badge i { width: 8px; height: 8px; border-radius: 50%; background: #4bc58a; box-shadow: 0 0 8px rgb(75 197 138 / 58%); }

.output-focus { display: grid; align-content: center; justify-items: center; gap: 7px; padding: 16px 24px 10px; }
.output-focus > span { color: #a8bac6; font-size: 19px; line-height: 1.25; }
.output-focus strong { color: #fff; font-size: 68px; font-weight: 780; line-height: 1; }
.output-focus strong small { margin-left: 8px; color: #91a5b5; font-size: 19px; font-weight: 550; }
.output-focus em { padding: 5px 12px; color: #7ed5a5; border-radius: 3px; background: rgb(75 197 138 / 10%); font-size: 16px; font-style: normal; line-height: 1.25; }
.output-focus em.is-empty { color: #8da0ae; background: rgb(120 140 155 / 10%); }

.output-breakdown { display: grid; grid-template-columns: 1fr 1fr; margin: 0 22px 14px; border-top: 1px solid #29445a; border-bottom: 1px solid #29445a; }
.output-breakdown > div { display: flex; align-items: baseline; justify-content: space-between; padding: 14px 18px; }
.output-breakdown > div + div { border-left: 1px solid #29445a; }
.output-breakdown span { color: #9eb1bf; font-size: 17px; line-height: 1.25; }
.output-breakdown b { color: #dcebf4; font-size: 28px; line-height: 1.1; }
.output-breakdown .is-qualified b { color: #66cf98; }
.output-breakdown .is-defect b { color: #e58b68; }

.production-stat-list { display: grid; grid-template-columns: 1fr; padding: 0 22px 10px; }
.production-stat-row { display: grid; min-width: 0; grid-template-columns: 42px minmax(0, 1fr) auto; align-items: center; gap: 12px; padding: 6px 4px; }
.production-stat-row + .production-stat-row { border-top: 1px solid #29445a; }
.production-stat-row > span { display: inline-flex; width: 34px; height: 34px; align-items: center; justify-content: center; color: #73add3; border-radius: 4px; background: #123149; }
.production-stat-row div { display: grid; min-width: 0; gap: 2px; }
.production-stat-row b { color: #c9d7e1; font-size: 16px; line-height: 1.25; }
.production-stat-row small { overflow: hidden; color: #91a6b5; font-size: 16px; line-height: 1.25; text-overflow: ellipsis; white-space: nowrap; }
.production-stat-row strong { color: #fff; font-size: 28px; }

.quality-main { display: grid; gap: 14px; padding: 18px 22px; }
.quality-rate { display: flex; align-items: end; justify-content: space-between; }
.quality-rate span { color: #a2b5c2; font-size: 18px; }
.quality-rate strong { color: #fff; font-size: 45px; line-height: 1; }
.quality-rate strong small { margin-left: 3px; color: #94a8b6; font-size: 17px; }
.quality-progress { height: 13px; overflow: hidden; border-radius: 3px; background: #1c3548; }
.quality-progress i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #3aaa78, #62d498); }
.quality-progress.is-empty i { width: 0 !important; }
.quality-counts { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.quality-counts > div { display: grid; gap: 5px; padding: 8px 14px; }
.quality-counts > div + div { border-left: 1px solid #365066; }
.quality-counts span { color: #9aafbd; font-size: 16px; }
.quality-counts b { color: #eaf3f8; font-size: 25px; }

.equipment-total,
.attention-total { color: #f0f6fa; font-size: 30px; font-weight: 750; }
.equipment-total small,
.attention-total small { margin-left: 6px; color: #9aacb9; font-size: 16px; font-weight: 550; }
.attention-total.has-warning { color: #ed9d5a; }

.equipment-body { position: relative; display: grid; gap: 18px; padding: 24px 22px 18px; }
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

.attention-list { display: grid; padding: 4px 18px 8px; }
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
