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
import { getCurrentUser, isFrameworkPreview } from '@/utils/auth'
import { openAiAssistant } from '@/utils/app-events'
import { activeTheme } from '@/utils/theme'

const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const previewMode = isFrameworkPreview()
const dateRange = ref([dayjs().subtract(6, 'day').toDate(), dayjs().toDate()])
const summary = ref({})
const trend = ref([])
const quality = ref({})
const equipment = ref({})
const currentUser = getCurrentUser()

const previewPayload = createDashboardPreview()

const dateSubtitle = computed(() => {
  const week = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][dayjs().day()]
  return `${dayjs().format('YYYY年MM月DD日')} ${week}`
})

const displayName = computed(() => currentUser.realName || currentUser.nickname || currentUser.username || 'MES 用户')
const greeting = computed(() => {
  const hour = dayjs().hour()
  if (hour < 6) return '夜间值守'
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})
const chartPalette = computed(() => activeTheme.value.chart)

const numberStats = computed(() => [
  {
    label: '生产订单',
    value: formatNumber(summary.value.productionOrderCount),
    unit: '单',
    icon: 'clipboard',
    color: '#f2b458'
  },
  {
    label: '运行中任务',
    value: formatNumber(summary.value.runningTaskCount),
    unit: '项',
    icon: 'activity',
    color: '#2db7f5'
  },
  {
    label: '区间合格产量',
    value: formatNumber(summary.value.todayQualifiedQuantity),
    unit: '件',
    icon: 'check',
    color: '#19be6b'
  },
  {
    label: '检验合格率',
    value: Number(quality.value.qualifiedRate || 0).toFixed(2),
    unit: '%',
    icon: 'shield-check',
    color: '#546e7a'
  },
  {
    label: '生产工单',
    value: formatNumber(summary.value.workOrderCount),
    unit: '单',
    icon: 'package',
    color: '#2d8cf0'
  },
  {
    label: '区间不良',
    value: formatNumber(summary.value.todayDefectQuantity),
    unit: '件',
    icon: 'alert',
    color: '#ed4014'
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

const quickCommands = [
  { label: '生产任务', description: '开工与完工', icon: 'activity', path: '/mfg/tasks', color: '#4f92d3' },
  { label: '生产报工', description: '提交产量与用料', icon: 'clipboard', path: '/mfg/work-reports', color: '#3aa0f3' },
  { label: '质量检验', description: '检验与判定', icon: 'shield-check', path: '/qms/inspection-orders', color: '#18be6b' },
  { label: '入库作业', description: '成品与物料入库', icon: 'warehouse', path: '/wms/inbound-orders', color: '#f2b458' },
  { label: '设备维修', description: '派工与关闭', icon: 'wrench', path: '/eam/repair-orders', color: '#d05a50' },
  { label: '批次追溯', description: '查询产品去向', icon: 'scan', path: '/trace/product-lots', color: '#144a78' }
]

const attentionItems = computed(() => [
  {
    label: '库存预警',
    value: formatNumber(summary.value.inventoryWarningCount),
    unit: '项',
    tone: Number(summary.value.inventoryWarningCount || 0) > 0 ? 'warning' : 'normal',
    path: '/wms/inventory'
  },
  {
    label: '故障设备',
    value: formatNumber(summary.value.equipmentFaultCount),
    unit: '台',
    tone: Number(summary.value.equipmentFaultCount || 0) > 0 ? 'danger' : 'normal',
    path: '/eam/equipment'
  },
  {
    label: '区间不良',
    value: formatNumber(summary.value.todayDefectQuantity),
    unit: '件',
    tone: Number(summary.value.todayDefectQuantity || 0) > 0 ? 'danger' : 'normal',
    path: '/qms/inspection-orders'
  }
])

const trendOption = computed(() => ({
  animationDuration: 500,
  color: [chartPalette.value.data, '#c44842'],
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
      areaStyle: { color: `${chartPalette.value.data}18` },
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
    color: hasData ? [chartPalette.value.primary, '#c44842'] : ['#dfe4ea'],
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
  color: [chartPalette.value.primary],
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
      { value: Number(equipment.value.running || 0), itemStyle: { color: chartPalette.value.primary } },
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
    <header class="mes-page-header dashboard-toolbar">
      <div class="command-copy">
        <div class="title-line">
          <h1 class="mes-page-title">首页</h1>
          <el-tag v-if="previewMode" size="small" effect="plain">预览数据</el-tag>
        </div>
        <p class="mes-page-subtitle">{{ greeting }}，{{ displayName }} · {{ dateSubtitle }}</p>
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

    <section class="dashboard-section" aria-label="快捷菜单">
      <div class="section-title">
        <h2>快捷菜单</h2>
      </div>
      <div class="quick-menu-grid">
        <button
          v-for="item in quickCommands"
          :key="item.path"
          type="button"
          class="quick-menu-card"
          :style="{ '--tile-color': item.color }"
          @click="router.push(item.path)"
        >
          <span class="tile-icon"><AppIcon :name="item.icon" :size="34" /></span>
          <span class="tile-divider" />
          <span class="tile-copy">
            <strong>{{ item.label }}</strong>
            <small>{{ item.description }}</small>
          </span>
        </button>
      </div>
    </section>

    <section class="dashboard-section" aria-label="数量统计">
      <div class="section-title">
        <h2>数量统计</h2>
      </div>
      <div class="number-card-grid">
        <article
          v-for="item in numberStats"
          :key="item.label"
          class="number-card"
          :style="{ '--tile-color': item.color }"
        >
          <span class="tile-icon"><AppIcon :name="item.icon" :size="34" /></span>
          <span class="tile-divider" />
          <span class="tile-copy">
            <small>{{ item.label }}</small>
            <strong>{{ item.value }}<em>{{ item.unit }}</em></strong>
          </span>
        </article>
      </div>
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

    <section class="attention-band mes-panel" aria-label="运营关注">
      <div class="band-heading">
        <div>
          <strong>运营关注</strong>
          <span>当前待关注业务</span>
        </div>
        <span class="live-label"><i />当前数据</span>
      </div>
      <div class="attention-list">
        <button
          v-for="item in attentionItems"
          :key="item.label"
          type="button"
          :class="`tone-${item.tone}`"
          @click="router.push(item.path)"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}<small>{{ item.unit }}</small></strong>
        </button>
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

.dashboard-toolbar {
  display: grid;
  min-height: 66px;
  grid-template-columns: minmax(260px, 1fr) auto;
  align-items: center;
  gap: 18px;
  margin-bottom: 10px;
  padding: 10px 14px;
  border: 1px solid var(--mes-border);
  border-radius: 0;
  background: #fff;
  box-shadow: var(--mes-shadow-sm);
}

.dashboard-toolbar .mes-page-title {
  font-size: 20px;
}

.dashboard-toolbar :deep(.el-tag) {
  border-radius: 3px;
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

.dashboard-actions .ai-insight-command.el-button {
  color: var(--mes-primary);
  border-color: var(--mes-primary-border);
  background: var(--mes-primary-soft);
}

.dashboard-actions .ai-insight-command.el-button:hover {
  color: #fff;
  border-color: var(--mes-primary);
  background: var(--mes-primary);
}

.dashboard-actions :deep(.el-button--primary.is-plain) {
  color: var(--mes-primary);
  border-color: var(--mes-primary-border);
  background: #fff;
}

.dashboard-actions :deep(.el-button--primary.is-plain:hover) {
  color: #fff;
  border-color: var(--mes-primary);
  background: var(--mes-primary-hover);
}

.dashboard-actions :deep(.el-date-editor) {
  width: 250px;
}

.dashboard-actions :deep(.el-button) {
  gap: 7px;
  margin: 0;
}

.dashboard-alert {
  margin-bottom: 10px;
}

.dashboard-section {
  border: 1px solid var(--mes-border);
  background: #fff;
}

.dashboard-section + .dashboard-section {
  margin-top: 10px;
}

.section-title {
  min-height: 50px;
  padding: 10px 18px 8px;
  border-bottom: 1px dotted #d4d4d4;
  background: #fff;
}

.section-title h2 {
  color: #000;
  font-size: 20px;
  font-weight: 650;
  line-height: 1.4;
}

.quick-menu-grid,
.number-card-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(160px, 1fr));
  gap: 14px;
  padding: 18px 20px 22px;
  background: #fff;
}

.quick-menu-card,
.number-card {
  display: grid;
  min-width: 0;
  min-height: 96px;
  grid-template-columns: 80px 1px minmax(0, 1fr);
  align-items: center;
  overflow: hidden;
  color: #fff;
  border: 0;
  border-radius: 6px;
  background: var(--tile-color);
  box-shadow: 0 2px 8px rgb(0 0 0 / 18%), 0 6px 20px rgb(0 0 0 / 12%);
  cursor: pointer;
  transition: transform .14s ease, box-shadow .14s ease;
}

.number-card {
  cursor: default;
}

.quick-menu-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgb(0 0 0 / 26%), 0 2px 8px rgb(0 0 0 / 14%);
}

.tile-icon {
  display: inline-flex;
  min-width: 0;
  height: 100%;
  align-items: center;
  justify-content: center;
  color: rgb(255 255 255 / 92%);
  background: rgb(0 0 0 / 10%);
}

.tile-divider {
  width: 1px;
  height: 64%;
  background: rgb(255 255 255 / 56%);
}

.tile-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  text-align: center;
}

.quick-menu-card .tile-copy {
  align-items: flex-start;
  text-align: left;
}

.tile-copy strong {
  max-width: 100%;
  overflow: hidden;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.3;
  text-overflow: ellipsis;
}

.quick-menu-card .tile-copy strong {
  font-size: 17px;
}

.tile-copy small {
  max-width: 100%;
  margin-top: 5px;
  overflow: hidden;
  color: rgb(255 255 255 / 88%);
  font-size: 11px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.number-card .tile-copy strong {
  display: flex;
  max-width: 100%;
  align-items: baseline;
  gap: 4px;
  font-size: 22px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.number-card em {
  font-size: 11px;
  font-style: normal;
  font-weight: 600;
  opacity: .9;
}

.status-strip {
  display: grid;
  min-height: 68px;
  grid-template-columns: 1.4fr repeat(3, 1fr);
  align-items: center;
  margin-top: 10px;
  padding: 0 18px;
  border-radius: 0;
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

.attention-band {
  margin-top: 10px;
  padding: 14px 16px 16px;
  border-radius: 0;
}

.band-heading,
.band-heading > div,
.live-label {
  display: flex;
  align-items: center;
}

.band-heading {
  min-height: 30px;
  justify-content: space-between;
  color: var(--mes-text-muted);
}

.band-heading > div {
  gap: 9px;
}

.band-heading strong {
  color: var(--mes-text);
  font-size: 13px;
  font-weight: 650;
}

.band-heading span {
  font-size: 10px;
}

.live-label {
  gap: 6px;
  color: var(--mes-success);
}

.live-label i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--mes-success);
}

.attention-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  margin-top: 9px;
}

.attention-list button {
  display: flex;
  min-width: 0;
  min-height: 58px;
  flex-direction: column;
  justify-content: center;
  padding: 7px 10px;
  color: var(--mes-text-secondary);
  border: 0;
  border-left: 3px solid var(--mes-border-strong);
  border-radius: 0 var(--mes-radius-sm) var(--mes-radius-sm) 0;
  background: var(--mes-surface-muted);
  text-align: left;
  cursor: pointer;
}

.attention-list button:hover {
  background: var(--mes-primary-soft);
}

.attention-list button.tone-warning {
  border-left-color: var(--mes-warning);
}

.attention-list button.tone-danger {
  border-left-color: var(--mes-danger);
}

.attention-list button > span {
  font-size: 9px;
}

.attention-list strong {
  margin-top: 3px;
  color: var(--mes-text);
  font-size: 18px;
  line-height: 1;
}

.attention-list .tone-warning strong {
  color: var(--mes-warning);
}

.attention-list .tone-danger strong {
  color: var(--mes-danger);
}

.attention-list small {
  margin-left: 3px;
  color: var(--mes-text-muted);
  font-size: 9px;
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
  border: 1px solid var(--mes-primary-border);
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
  .dashboard-toolbar {
    grid-template-columns: minmax(240px, 1fr) auto;
  }

  .quick-menu-grid,
  .number-card-grid {
    grid-template-columns: repeat(3, minmax(180px, 1fr));
    gap: 14px;
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
  .dashboard-toolbar {
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
  .quick-menu-grid,
  .number-card-grid {
    grid-template-columns: 1fr;
    padding: 14px;
  }

  .quick-menu-card,
  .number-card {
    min-height: 82px;
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
    max-width: 100%;
    justify-content: flex-start;
  }

  .attention-list {
    grid-template-columns: 1fr;
  }
}
</style>
