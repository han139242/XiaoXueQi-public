<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import AppIcon from '@/components/common/AppIcon.vue'
import MesChart from '@/components/chart/MesChart.vue'
import {
  getDashboardSummary,
  getEquipmentStatus,
  getProductionTrend,
  getQualityStatistics
} from '@/api/dashboard'
import { isFrameworkPreview } from '@/utils/auth'

const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const previewMode = isFrameworkPreview()
const dateRange = ref([dayjs().subtract(6, 'day').toDate(), dayjs().toDate()])
const summary = ref({})
const trend = ref([])
const quality = ref({})
const equipment = ref({})

const previewPayload = {
  summary: {
    productionOrderCount: 24,
    workOrderCount: 38,
    runningTaskCount: 12,
    todayQualifiedQuantity: 12860,
    todayDefectQuantity: 126,
    inventoryWarningCount: 4,
    equipmentFaultCount: 2
  },
  trend: [
    [6, 820, 18],
    [5, 1080, 21],
    [4, 960, 17],
    [3, 1420, 25],
    [2, 1310, 19],
    [1, 1680, 16],
    [0, 1820, 10]
  ].map(([offset, qualifiedQuantity, defectQuantity]) => ({
    date: dayjs().subtract(offset, 'day').format('YYYY-MM-DD'),
    qualifiedQuantity,
    defectQuantity
  })),
  quality: {
    inspectionCount: 34,
    qualifiedCount: 31,
    unqualifiedCount: 3,
    qualifiedRate: 91.18
  },
  equipment: {
    running: 42,
    stopped: 6,
    fault: 2,
    maintenance: 3
  }
}

const dateSubtitle = computed(() => {
  const week = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][dayjs().day()]
  return `${dayjs().format('YYYY年MM月DD日')} ${week}`
})

const metrics = computed(() => [
  {
    label: '生产订单',
    value: formatNumber(summary.value.productionOrderCount),
    unit: '单',
    icon: 'clipboard',
    tone: 'green'
  },
  {
    label: '运行中任务',
    value: formatNumber(summary.value.runningTaskCount),
    unit: '项',
    icon: 'activity',
    tone: 'blue'
  },
  {
    label: '今日合格产量',
    value: formatNumber(summary.value.todayQualifiedQuantity),
    unit: '件',
    icon: 'check',
    tone: 'teal'
  },
  {
    label: '检验合格率',
    value: Number(quality.value.qualifiedRate || 0).toFixed(2),
    unit: '%',
    icon: 'shield-check',
    tone: 'amber'
  }
])

const operationalStatus = computed(() => [
  {
    label: '生产工单',
    value: formatNumber(summary.value.workOrderCount),
    icon: 'package',
    tone: 'normal'
  },
  {
    label: '库存预警',
    value: formatNumber(summary.value.inventoryWarningCount),
    icon: 'alert',
    tone: Number(summary.value.inventoryWarningCount || 0) > 0 ? 'warning' : 'normal'
  },
  {
    label: '故障设备',
    value: formatNumber(summary.value.equipmentFaultCount),
    icon: 'wrench',
    tone: Number(summary.value.equipmentFaultCount || 0) > 0 ? 'danger' : 'normal'
  }
])

const trendOption = computed(() => ({
  animationDuration: 500,
  color: ['#087f5b', '#c2413b'],
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#17201d',
    borderWidth: 0,
    textStyle: { color: '#fff', fontSize: 12 }
  },
  legend: {
    top: 2,
    right: 2,
    itemWidth: 14,
    itemHeight: 3,
    textStyle: { color: '#64716b', fontSize: 11 }
  },
  grid: { top: 44, right: 12, bottom: 24, left: 52 },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trend.value.map((item) => dayjs(item.date).format('MM/DD')),
    axisLine: { lineStyle: { color: '#dde3e0' } },
    axisTick: { show: false },
    axisLabel: { color: '#8a9690', fontSize: 10 }
  },
  yAxis: {
    type: 'value',
    splitNumber: 4,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#8a9690', fontSize: 10 },
    splitLine: { lineStyle: { color: '#edf1ef' } }
  },
  series: [
    {
      name: '合格产量',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2.5 },
      areaStyle: { color: 'rgba(8,127,91,.08)' },
      data: trend.value.map((item) => Number(item.qualifiedQuantity || 0))
    },
    {
      name: '不良产量',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { width: 1.8 },
      data: trend.value.map((item) => Number(item.defectQuantity || 0))
    }
  ]
}))

const qualityOption = computed(() => {
  const qualified = Number(quality.value.qualifiedCount || 0)
  const unqualified = Number(quality.value.unqualifiedCount || 0)
  const hasData = qualified + unqualified > 0
  return {
    animationDuration: 500,
    color: hasData ? ['#087f5b', '#c2413b'] : ['#dde3e0'],
    tooltip: {
      trigger: 'item',
      backgroundColor: '#17201d',
      borderWidth: 0,
      textStyle: { color: '#fff', fontSize: 12 }
    },
    legend: {
      bottom: 2,
      left: 'center',
      itemWidth: 8,
      itemHeight: 8,
      textStyle: { color: '#64716b', fontSize: 11 }
    },
    series: [{
      type: 'pie',
      radius: ['57%', '76%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: false,
      label: {
        show: true,
        position: 'center',
        formatter: `${Number(quality.value.qualifiedRate || 0).toFixed(1)}%`,
        color: '#1f2b26',
        fontSize: 23,
        fontWeight: 650
      },
      labelLine: { show: false },
      data: hasData
        ? [
            { value: qualified, name: '合格' },
            { value: unqualified, name: '不合格' }
          ]
        : [{ value: 1, name: '暂无检验' }]
    }]
  }
})

const equipmentOption = computed(() => ({
  animationDuration: 500,
  color: ['#2463a7'],
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    backgroundColor: '#17201d',
    borderWidth: 0,
    textStyle: { color: '#fff', fontSize: 12 }
  },
  grid: { top: 12, right: 20, bottom: 18, left: 58 },
  xAxis: {
    type: 'value',
    axisLabel: { color: '#8a9690', fontSize: 10 },
    splitLine: { lineStyle: { color: '#edf1ef' } }
  },
  yAxis: {
    type: 'category',
    data: ['运行', '停机', '保养', '故障'],
    axisTick: { show: false },
    axisLine: { show: false },
    axisLabel: { color: '#64716b', fontSize: 11 }
  },
  series: [{
    type: 'bar',
    barWidth: 12,
    itemStyle: { borderRadius: 2 },
    data: [
      { value: Number(equipment.value.running || 0), itemStyle: { color: '#087f5b' } },
      { value: Number(equipment.value.stopped || 0), itemStyle: { color: '#7b8781' } },
      { value: Number(equipment.value.maintenance || 0), itemStyle: { color: '#c77b12' } },
      { value: Number(equipment.value.fault || 0), itemStyle: { color: '#c2413b' } }
    ]
  }]
}))

const processSteps = [
  ['订单', 'clipboard'],
  ['工单', 'package'],
  ['派工', 'users'],
  ['报工', 'activity'],
  ['质检', 'shield-check'],
  ['入库', 'warehouse'],
  ['追溯', 'scan']
]

function formatNumber(value) {
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 2 }).format(Number(value || 0))
}

function queryParams() {
  return {
    beginDate: dayjs(dateRange.value[0]).format('YYYY-MM-DD'),
    endDate: dayjs(dateRange.value[1]).format('YYYY-MM-DD')
  }
}

async function loadDashboard() {
  if (previewMode) {
    summary.value = previewPayload.summary
    trend.value = previewPayload.trend
    quality.value = previewPayload.quality
    equipment.value = previewPayload.equipment
    return
  }

  loading.value = true
  loadError.value = ''
  try {
    const params = queryParams()
    const [summaryResult, trendResult, qualityResult, equipmentResult] = await Promise.all([
      getDashboardSummary(params),
      getProductionTrend(params),
      getQualityStatistics(params),
      getEquipmentStatus()
    ])
    summary.value = summaryResult.data || {}
    trend.value = trendResult.data || []
    quality.value = qualityResult.data || {}
    equipment.value = equipmentResult.data || {}
  } catch (error) {
    loadError.value = error.displayMessage || error.message || '看板数据加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <div class="mes-page dashboard-page" v-loading="loading">
    <header class="mes-page-header dashboard-header">
      <div>
        <div class="title-line">
          <h1 class="mes-page-title">运营工作台</h1>
          <el-tag v-if="previewMode" size="small" effect="plain">预览数据</el-tag>
        </div>
        <p class="mes-page-subtitle">{{ dateSubtitle }} · 制造运营总览</p>
      </div>
      <div class="dashboard-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          unlink-panels
          :clearable="false"
          @change="loadDashboard"
        />
        <el-tooltip content="刷新数据" placement="bottom">
          <el-button class="mes-icon-button" aria-label="刷新数据" @click="loadDashboard">
            <AppIcon name="refresh" :size="17" />
          </el-button>
        </el-tooltip>
        <el-button type="primary" plain @click="router.push('/module/big-screen')">
          <AppIcon name="monitor" :size="16" />
          生产大屏
        </el-button>
      </div>
    </header>

    <el-alert
      v-if="loadError"
      class="dashboard-alert"
      :title="loadError"
      type="error"
      show-icon
      :closable="false"
    >
      <template #default>
        <el-button link type="danger" @click="loadDashboard">重新加载</el-button>
      </template>
    </el-alert>

    <section class="metrics-grid" aria-label="关键指标">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card" :class="`tone-${metric.tone}`">
        <div class="metric-icon"><AppIcon :name="metric.icon" :size="20" /></div>
        <div class="metric-copy">
          <span>{{ metric.label }}</span>
          <div><strong>{{ metric.value }}</strong><small>{{ metric.unit }}</small></div>
        </div>
      </article>
    </section>

    <section class="status-strip mes-panel">
      <div class="status-heading">
        <span class="live-dot" />
        <div>
          <strong>现场运行状态</strong>
          <small>当前业务数据</small>
        </div>
      </div>
      <div
        v-for="item in operationalStatus"
        :key="item.label"
        class="status-item"
        :class="`is-${item.tone}`"
      >
        <span class="status-icon"><AppIcon :name="item.icon" :size="17" /></span>
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </section>

    <section class="dashboard-grid">
      <article class="mes-panel trend-panel">
        <div class="mes-panel-header">
          <div>
            <h2 class="mes-panel-title">近七日生产趋势</h2>
            <span class="panel-caption">合格产量与不良产量</span>
          </div>
          <el-tag size="small" type="success" effect="plain">按日</el-tag>
        </div>
        <div class="chart-body"><MesChart :option="trendOption" height="286px" /></div>
      </article>

      <article class="mes-panel quality-panel">
        <div class="mes-panel-header">
          <div>
            <h2 class="mes-panel-title">质量概况</h2>
            <span class="panel-caption">检验单合格分布</span>
          </div>
          <span class="inspection-count">{{ quality.inspectionCount || 0 }} 单</span>
        </div>
        <div class="chart-body"><MesChart :option="qualityOption" height="286px" /></div>
      </article>

      <article class="mes-panel process-panel">
        <div class="mes-panel-header">
          <div>
            <h2 class="mes-panel-title">生产业务链路</h2>
            <span class="panel-caption">从订单到批次追溯</span>
          </div>
          <span class="chain-state"><span />流程在线</span>
        </div>
        <div class="process-flow">
          <template v-for="(step, index) in processSteps" :key="step[0]">
            <div class="process-step">
              <span><AppIcon :name="step[1]" :size="19" /></span>
              <strong>{{ step[0] }}</strong>
            </div>
            <div v-if="index < processSteps.length - 1" class="process-link">
              <AppIcon name="right" :size="15" />
            </div>
          </template>
        </div>
      </article>

      <article class="mes-panel equipment-panel">
        <div class="mes-panel-header">
          <div>
            <h2 class="mes-panel-title">设备状态</h2>
            <span class="panel-caption">运行、停机与维保分布</span>
          </div>
          <span class="equipment-total">
            {{ Number(equipment.running || 0) + Number(equipment.stopped || 0) + Number(equipment.fault || 0) + Number(equipment.maintenance || 0) }} 台
          </span>
        </div>
        <div class="chart-body"><MesChart :option="equipmentOption" height="226px" /></div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.dashboard-page {
  min-height: 100%;
}

.dashboard-header {
  align-items: center;
}

.title-line,
.dashboard-actions {
  display: flex;
  align-items: center;
}

.title-line {
  gap: 10px;
}

.dashboard-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.dashboard-actions :deep(.el-date-editor) {
  width: 250px;
}

.dashboard-actions :deep(.el-button) {
  gap: 7px;
  margin: 0;
}

.dashboard-alert {
  margin-bottom: 14px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  position: relative;
  display: flex;
  min-height: 108px;
  align-items: center;
  gap: 15px;
  padding: 18px;
  overflow: hidden;
  border: 1px solid var(--mes-border);
  border-top: 3px solid var(--tone-color);
  border-radius: var(--mes-radius);
  background: #fff;
  box-shadow: var(--mes-shadow-sm);
}

.metric-card.tone-green {
  --tone-color: var(--mes-primary);
  --tone-soft: var(--mes-primary-soft);
}

.metric-card.tone-blue {
  --tone-color: var(--mes-data);
  --tone-soft: var(--mes-data-soft);
}

.metric-card.tone-teal {
  --tone-color: #2f8d88;
  --tone-soft: #eaf6f5;
}

.metric-card.tone-amber {
  --tone-color: var(--mes-warning);
  --tone-soft: var(--mes-warning-soft);
}

.metric-icon {
  display: inline-flex;
  width: 42px;
  height: 42px;
  flex: none;
  align-items: center;
  justify-content: center;
  color: var(--tone-color);
  border-radius: var(--mes-radius);
  background: var(--tone-soft);
}

.metric-copy {
  min-width: 0;
}

.metric-copy > span {
  color: var(--mes-text-secondary);
  font-size: 12px;
}

.metric-copy > div {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 6px;
}

.metric-copy strong {
  overflow: hidden;
  color: var(--mes-text);
  font-size: 25px;
  font-weight: 680;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-copy small {
  color: var(--mes-text-muted);
  font-size: 10px;
}

.status-strip {
  display: grid;
  min-height: 68px;
  grid-template-columns: 1.4fr repeat(3, 1fr);
  align-items: center;
  margin-top: 14px;
  padding: 0 18px;
}

.status-heading,
.status-item {
  display: flex;
  align-items: center;
}

.status-heading {
  gap: 11px;
}

.status-heading > div {
  display: flex;
  flex-direction: column;
}

.status-heading strong {
  font-size: 13px;
  font-weight: 650;
}

.status-heading small {
  color: var(--mes-text-muted);
  font-size: 10px;
}

.live-dot {
  width: 9px;
  height: 9px;
  border: 2px solid #bce4d4;
  border-radius: 50%;
  background: var(--mes-primary);
  box-shadow: 0 0 0 4px var(--mes-primary-soft);
}

.status-item {
  height: 36px;
  gap: 9px;
  padding: 0 18px;
  border-left: 1px solid var(--mes-border);
  color: var(--mes-text-secondary);
  font-size: 12px;
}

.status-item strong {
  margin-left: auto;
  color: var(--mes-text);
  font-size: 17px;
  font-weight: 680;
}

.status-icon {
  display: inline-flex;
  color: var(--mes-data);
}

.status-item.is-warning .status-icon,
.status-item.is-warning strong {
  color: var(--mes-warning);
}

.status-item.is-danger .status-icon,
.status-item.is-danger strong {
  color: var(--mes-danger);
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.9fr) minmax(300px, 1fr);
  gap: 14px;
  margin-top: 14px;
}

.mes-panel-header > div {
  min-width: 0;
}

.panel-caption {
  display: block;
  margin-top: 2px;
  color: var(--mes-text-muted);
  font-size: 10px;
}

.chart-body {
  padding: 8px 12px 4px;
}

.inspection-count,
.equipment-total {
  color: var(--mes-text-secondary);
  font-size: 11px;
}

.process-panel {
  min-width: 0;
}

.chain-state {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--mes-primary);
  font-size: 10px;
}

.chain-state span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--mes-primary);
}

.process-flow {
  display: flex;
  min-height: 226px;
  align-items: center;
  justify-content: space-around;
  gap: 6px;
  padding: 22px 18px;
  overflow-x: auto;
}

.process-step {
  display: flex;
  min-width: 64px;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: var(--mes-text-secondary);
}

.process-step > span {
  display: inline-flex;
  width: 42px;
  height: 42px;
  align-items: center;
  justify-content: center;
  color: var(--mes-primary);
  border: 1px solid #bcdacf;
  border-radius: 50%;
  background: var(--mes-primary-soft);
}

.process-step strong {
  font-size: 11px;
  font-weight: 620;
  white-space: nowrap;
}

.process-link {
  color: var(--mes-border-strong);
}

@media (max-width: 1280px) {
  .metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .status-strip {
    grid-template-columns: repeat(3, 1fr);
    padding: 10px 14px;
  }

  .status-heading {
    display: none;
  }

  .status-item:first-of-type {
    border-left: 0;
  }
}

@media (max-width: 900px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-actions {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .metric-card {
    min-height: 92px;
  }

  .status-strip {
    grid-template-columns: 1fr;
  }

  .status-item {
    border-top: 1px solid var(--mes-border);
    border-left: 0;
  }

  .status-item:first-of-type {
    border-top: 0;
  }

  .dashboard-actions :deep(.el-date-editor) {
    width: 100%;
  }

  .process-flow {
    justify-content: flex-start;
  }
}
</style>
