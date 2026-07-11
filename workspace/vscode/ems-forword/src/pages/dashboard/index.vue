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
import { createDashboardPreview } from '@/config/dashboard-preview'
import { isFrameworkPreview } from '@/utils/auth'
import { openAiAssistant } from '@/utils/app-events'

const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const previewMode = isFrameworkPreview()
const dateRange = ref([dayjs().subtract(6, 'day').toDate(), dayjs().toDate()])
const summary = ref({})
const trend = ref([])
const quality = ref({})
const equipment = ref({})

const previewPayload = createDashboardPreview()

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
    tone: 'primary'
  },
  {
    label: '运行中任务',
    value: formatNumber(summary.value.runningTaskCount),
    unit: '项',
    icon: 'activity',
    tone: 'primary'
  },
  {
    label: '今日合格产量',
    value: formatNumber(summary.value.todayQualifiedQuantity),
    unit: '件',
    icon: 'check',
    tone: 'data'
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
  color: ['#1688b5', '#c44842'],
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#171d25',
    borderWidth: 0,
    textStyle: { color: '#fff', fontSize: 12 }
  },
  legend: {
    top: 2,
    right: 2,
    itemWidth: 14,
    itemHeight: 3,
    textStyle: { color: '#647384', fontSize: 11 }
  },
  grid: { top: 44, right: 12, bottom: 24, left: 52 },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trend.value.map((item) => dayjs(item.date).format('MM/DD')),
    axisLine: { lineStyle: { color: '#dfe4ea' } },
    axisTick: { show: false },
    axisLabel: { color: '#8a96a3', fontSize: 10 }
  },
  yAxis: {
    type: 'value',
    splitNumber: 4,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#8a96a3', fontSize: 10 },
    splitLine: { lineStyle: { color: '#edf1f4' } }
  },
  series: [
    {
      name: '合格产量',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2.5 },
      areaStyle: { color: 'rgba(22,136,181,.09)' },
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
    color: hasData ? ['#1688b5', '#c44842'] : ['#dfe4ea'],
    tooltip: {
      trigger: 'item',
      backgroundColor: '#171d25',
      borderWidth: 0,
      textStyle: { color: '#fff', fontSize: 12 }
    },
    legend: {
      bottom: 2,
      left: 'center',
      itemWidth: 8,
      itemHeight: 8,
      textStyle: { color: '#647384', fontSize: 11 }
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
        color: '#202a36',
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
  color: ['#2f6fae'],
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    backgroundColor: '#171d25',
    borderWidth: 0,
    textStyle: { color: '#fff', fontSize: 12 }
  },
  grid: { top: 12, right: 20, bottom: 18, left: 58 },
  xAxis: {
    type: 'value',
    axisLabel: { color: '#8794a2', fontSize: 10 },
    splitLine: { lineStyle: { color: '#edf1f5' } }
  },
  yAxis: {
    type: 'category',
    data: ['运行', '停机', '保养', '故障'],
    axisTick: { show: false },
    axisLine: { show: false },
    axisLabel: { color: '#647384', fontSize: 11 }
  },
  series: [{
    type: 'bar',
    barWidth: 12,
    itemStyle: { borderRadius: 2 },
    data: [
      { value: Number(equipment.value.running || 0), itemStyle: { color: '#327ed0' } },
      { value: Number(equipment.value.stopped || 0), itemStyle: { color: '#788592' } },
      { value: Number(equipment.value.maintenance || 0), itemStyle: { color: '#c48618' } },
      { value: Number(equipment.value.fault || 0), itemStyle: { color: '#c44842' } }
    ]
  }]
}))

const processSteps = [
  { label: '订单', icon: 'clipboard', path: '/mfg/production-orders' },
  { label: '工单', icon: 'package', path: '/mfg/work-orders' },
  { label: '派工', icon: 'users', path: '/mfg/dispatch-orders' },
  { label: '报工', icon: 'activity', path: '/mfg/work-reports' },
  { label: '质检', icon: 'shield-check', path: '/qms/inspection-orders' },
  { label: '入库', icon: 'warehouse', path: '/wms/inbound-orders' },
  { label: '追溯', icon: 'scan', path: '/trace/product-lots' }
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
      <div class="command-copy">
        <span class="command-kicker"><i />LIVE MANUFACTURING OVERVIEW</span>
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
        <el-button class="ai-insight-command" @click="openAiAssistant">
          <AppIcon name="bot" :size="16" />
          AI 解读
        </el-button>
        <el-button type="primary" plain @click="router.push('/big-screen/production')">
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
          <el-tag size="small" effect="plain">按日</el-tag>
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
          <template v-for="(step, index) in processSteps" :key="step.label">
            <button class="process-step" type="button" @click="router.push(step.path)">
              <span><AppIcon :name="step.icon" :size="19" /></span>
              <strong>{{ step.label }}</strong>
            </button>
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
  position: relative;
  min-height: 112px;
  align-items: center;
  display: grid;
  grid-template-columns: minmax(260px, 1fr) auto;
  gap: 22px;
  overflow: hidden;
  padding: 18px 20px;
  border: 1px solid #2e3a47;
  border-radius: var(--mes-radius);
  background: #171d25;
  box-shadow: 0 10px 26px rgb(24 33 43 / 13%);
}

.command-copy {
  position: relative;
  z-index: 1;
}

.command-kicker {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 7px;
  color: #7eb7e2;
  font-size: 9px;
  font-weight: 750;
}

.command-kicker i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4d9de0;
  box-shadow: 0 0 0 4px rgb(77 157 224 / 13%);
}

.dashboard-header .mes-page-title {
  color: #f1f6fb;
}

.dashboard-header .mes-page-subtitle {
  color: #91a1b1;
}

.dashboard-header :deep(.el-tag) {
  color: #a8d0ec;
  border-color: #3d6588;
  background: #1c3349;
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
  position: relative;
  z-index: 1;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.dashboard-actions .ai-insight-command.el-button {
  color: #c6e7fb;
  border-color: #3f6f95;
  background: #1b3850;
}

.dashboard-actions .ai-insight-command.el-button:hover {
  color: #f1f9ff;
  border-color: #619bd0;
  background: #255176;
}

.dashboard-actions :deep(.el-button--primary.is-plain) {
  color: #eff7fd;
  border-color: #5d91bf;
  background: #2d699f;
}

.dashboard-actions :deep(.el-button--primary.is-plain:hover) {
  color: #fff;
  border-color: #4b86bb;
  background: #3979b1;
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
  gap: 0;
  overflow: hidden;
  border: 1px solid var(--mes-border);
  border-radius: var(--mes-radius);
  background: #fff;
  box-shadow: var(--mes-shadow-sm);
}

.metric-card {
  position: relative;
  display: flex;
  min-height: 108px;
  align-items: center;
  gap: 15px;
  padding: 18px;
  overflow: hidden;
  border: 0;
  border-right: 1px solid var(--mes-border);
  border-radius: 0;
  background: #fff;
  box-shadow: none;
  transition: background .18s ease;
}

.metric-card:last-child { border-right: 0; }

.metric-card:hover {
  background: #f8fafc;
}

.metric-card.tone-success {
  --tone-color: var(--mes-success);
  --tone-soft: var(--mes-success-soft);
}

.metric-card.tone-primary {
  --tone-color: var(--mes-primary);
  --tone-soft: var(--mes-primary-soft);
}

.metric-card.tone-data {
  --tone-color: var(--mes-data);
  --tone-soft: var(--mes-data-soft);
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
  border: 2px solid #b9d8c9;
  border-radius: 50%;
  background: var(--mes-success);
  box-shadow: 0 0 0 4px var(--mes-success-soft);
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
  min-width: 0;
  grid-template-columns: minmax(0, 1.9fr) minmax(300px, 1fr);
  gap: 14px;
  margin-top: 14px;
}

.dashboard-grid > .mes-panel {
  min-width: 0;
  transition: border-color .18s ease, box-shadow .18s ease;
}

.dashboard-grid > .mes-panel:hover {
  border-color: #c1cbd5;
  box-shadow: 0 8px 22px rgb(32 42 54 / 7%);
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
  min-width: 0;
  overflow: hidden;
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
  color: var(--mes-data);
  font-size: 10px;
}

.chain-state span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--mes-data);
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
  border: 0;
  background: transparent;
  cursor: pointer;
}

.process-step > span {
  display: inline-flex;
  width: 42px;
  height: 42px;
  align-items: center;
  justify-content: center;
  color: var(--mes-primary);
  border: 1px solid #bfd2e6;
  border-radius: 50%;
  background: var(--mes-primary-soft);
}

.process-step:hover > span {
  color: #fff;
  border-color: var(--mes-primary);
  background: var(--mes-primary);
}

.process-step:hover strong { color:var(--mes-primary); }

.process-step strong {
  font-size: 11px;
  font-weight: 620;
  white-space: nowrap;
}

.process-link {
  color: var(--mes-border-strong);
}

@media (max-width: 1280px) {
  .dashboard-header {
    grid-template-columns: minmax(240px, 1fr) auto;
  }

  .metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-card:nth-child(2n) { border-right: 0; }
  .metric-card:nth-child(-n+2) { border-bottom: 1px solid var(--mes-border); }

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
  .dashboard-header {
    display: flex;
    align-items: flex-start;
    flex-direction: column;
  }

  .dashboard-grid {
    grid-template-columns: minmax(0, 1fr);
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
    border-right: 0;
    border-bottom: 1px solid var(--mes-border);
  }

  .metric-card:last-child { border-bottom: 0; }

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
    max-width: 100%;
    justify-content: flex-start;
  }
}
</style>
