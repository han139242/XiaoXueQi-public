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

const stageStyle = computed(() => ({
  transform: `translate(-50%, -50%) scale(${scale.value})`
}))

const timeText = computed(() => now.value.format('HH:mm:ss'))
const dateText = computed(() => now.value.format('YYYY年MM月DD日'))
const equipmentTotal = computed(() => ['running', 'stopped', 'fault', 'maintenance']
  .reduce((total, key) => total + Number(equipment.value[key] || 0), 0))

const metrics = computed(() => [
  ['生产订单', summary.value.productionOrderCount, '单', 'clipboard', 'blue'],
  ['生产工单', summary.value.workOrderCount, '单', 'package', 'blue'],
  ['运行任务', summary.value.runningTaskCount, '项', 'activity', 'cyan'],
  ['今日合格', summary.value.todayQualifiedQuantity, '件', 'check', 'cyan'],
  ['今日不良', summary.value.todayDefectQuantity, '件', 'alert', 'red'],
  ['库存预警', summary.value.inventoryWarningCount, '项', 'warehouse', 'amber'],
  ['故障设备', summary.value.equipmentFaultCount, '台', 'wrench', 'red']
])

const riskItems = computed(() => [
  {
    label: '库存补货预警',
    value: Number(summary.value.inventoryWarningCount || 0),
    level: Number(summary.value.inventoryWarningCount || 0) > 0 ? 'warning' : 'normal',
    icon: 'warehouse'
  },
  {
    label: '设备故障',
    value: Number(summary.value.equipmentFaultCount || 0),
    level: Number(summary.value.equipmentFaultCount || 0) > 0 ? 'danger' : 'normal',
    icon: 'wrench'
  },
  {
    label: '今日不良产量',
    value: Number(summary.value.todayDefectQuantity || 0),
    level: Number(summary.value.todayDefectQuantity || 0) > 0 ? 'danger' : 'normal',
    icon: 'shield-check'
  }
])

const processSteps = [
  ['订单', 'clipboard', 'blue'],
  ['工单', 'package', 'blue'],
  ['派工', 'users', 'cyan'],
  ['报工', 'activity', 'amber'],
  ['质检', 'shield-check', 'cyan'],
  ['入库', 'warehouse', 'blue'],
  ['追溯', 'scan', 'cyan']
]

const trendOption = computed(() => ({
  animationDuration: 700,
  color: ['#58d4ff', '#ff746c'],
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#07111b',
    borderColor: '#2b5b78',
    textStyle: { color: '#eef8ff', fontSize: 13 }
  },
  legend: {
    top: 2,
    right: 18,
    itemWidth: 18,
    itemHeight: 4,
    textStyle: { color: '#8aa8bf', fontSize: 12 }
  },
  grid: { top: 50, right: 24, bottom: 30, left: 62 },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trend.value.map((item) => dayjs(item.date).format('MM/DD')),
    axisLine: { lineStyle: { color: '#24435a' } },
    axisTick: { show: false },
    axisLabel: { color: '#7d9ab0', fontSize: 11 }
  },
  yAxis: {
    type: 'value',
    splitNumber: 4,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#7d9ab0', fontSize: 11 },
    splitLine: { lineStyle: { color: '#13283a' } }
  },
  series: [
    {
      name: '合格产量',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      lineStyle: { width: 3 },
      areaStyle: { color: 'rgba(88,212,255,.12)' },
      data: trend.value.map((item) => Number(item.qualifiedQuantity || 0))
    },
    {
      name: '不良产量',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2 },
      data: trend.value.map((item) => Number(item.defectQuantity || 0))
    }
  ]
}))

const qualityOption = computed(() => {
  const passed = Number(quality.value.qualifiedCount || 0)
  const failed = Number(quality.value.unqualifiedCount || 0)
  const hasData = passed + failed > 0
  return {
    animationDuration: 700,
    color: hasData ? ['#58d4ff', '#ff746c'] : ['#203449'],
    tooltip: {
      trigger: 'item',
      backgroundColor: '#07111b',
      borderColor: '#2b5b78',
      textStyle: { color: '#eef8ff' }
    },
    legend: {
      bottom: 2,
      left: 'center',
      itemWidth: 9,
      itemHeight: 9,
      textStyle: { color: '#8aa8bf', fontSize: 11 }
    },
    series: [{
      type: 'pie',
      radius: ['63%', '80%'],
      center: ['50%', '45%'],
      label: {
        show: true,
        position: 'center',
        formatter: `${Number(quality.value.qualifiedRate || 0).toFixed(1)}%`,
        color: '#eef8ff',
        fontSize: 34,
        fontWeight: 760
      },
      labelLine: { show: false },
      data: hasData
        ? [{ value: passed, name: '合格' }, { value: failed, name: '不合格' }]
        : [{ value: 1, name: '暂无检验' }]
    }]
  }
})

const equipmentOption = computed(() => ({
  animationDuration: 700,
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    backgroundColor: '#07111b',
    borderColor: '#2b5b78',
    textStyle: { color: '#eef8ff' }
  },
  grid: { top: 12, right: 30, bottom: 18, left: 76 },
  xAxis: {
    type: 'value',
    axisLabel: { color: '#7d9ab0', fontSize: 10 },
    splitLine: { lineStyle: { color: '#13283a' } }
  },
  yAxis: {
    type: 'category',
    data: ['运行', '停机', '维保', '故障'],
    axisTick: { show: false },
    axisLine: { show: false },
    axisLabel: { color: '#a6c1d5', fontSize: 11 }
  },
  series: [{
    type: 'bar',
    barWidth: 13,
    data: [
      { value: Number(equipment.value.running || 0), itemStyle: { color: '#58d4ff', borderRadius: 3 } },
      { value: Number(equipment.value.stopped || 0), itemStyle: { color: '#788b9e', borderRadius: 3 } },
      { value: Number(equipment.value.maintenance || 0), itemStyle: { color: '#e6ad50', borderRadius: 3 } },
      { value: Number(equipment.value.fault || 0), itemStyle: { color: '#ff746c', borderRadius: 3 } }
    ]
  }]
}))

function formatNumber(value) {
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 2 }).format(Number(value || 0))
}

function updateScale() {
  scale.value = Math.min(window.innerWidth / 1920, window.innerHeight / 1080)
}

async function toggleFullscreen() {
  if (!document.fullscreenElement) {
    await document.documentElement.requestFullscreen?.()
  } else {
    await document.exitFullscreen?.()
  }
  window.setTimeout(updateScale, 120)
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
    const params = {
      beginDate: dayjs().subtract(6, 'day').format('YYYY-MM-DD'),
      endDate: dayjs().format('YYYY-MM-DD')
    }
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
    <div class="screen-background" />
<div class="big-screen-stage" :style="stageStyle">
      <header class="screen-header">
        <div class="screen-brand">
          <span class="screen-logo"><AppIcon name="factory" :size="25" /></span>
          <div>
            <strong>羽球智造 MES</strong>
            <small>BADMINTON SMART FACTORY</small>
          </div>
        </div>

        <div class="screen-title">
          <i />
          <div>
            <h1>生产运营指挥中心</h1>
            <p>PRODUCTION OPERATION COMMAND CENTER</p>
          </div>
          <i />
        </div>

        <div class="screen-actions">
          <div class="screen-time">
            <strong>{{ timeText }}</strong>
            <span>{{ dateText }}</span>
          </div>
          <button type="button" :class="{ rotating: loading }" aria-label="刷新数据" @click="loadData">
            <AppIcon name="refresh" :size="18" />
          </button>
          <button type="button" aria-label="全屏" @click="toggleFullscreen">
            <AppIcon name="fullscreen" :size="18" />
          </button>
          <button type="button" aria-label="返回后台" @click="router.push('/dashboard')">
            <AppIcon name="logout" :size="18" />
          </button>
        </div>
      </header>

      <main class="screen-content">
        <div v-if="previewMode" class="preview-flag">FRAMEWORK PREVIEW · 预览数据</div>
        <div v-if="loadError" class="screen-error">
          <AppIcon name="alert" :size="18" />
          <span>{{ loadError }}</span>
          <button type="button" @click="loadData">重新加载</button>
        </div>

        <section class="metric-strip">
          <article
            v-for="metric in metrics"
            :key="metric[0]"
            class="metric-card"
            :class="`tone-${metric[4]}`"
          >
            <span class="metric-icon"><AppIcon :name="metric[3]" :size="22" /></span>
            <div>
              <label>{{ metric[0] }}</label>
              <p><strong>{{ formatNumber(metric[1]) }}</strong><small>{{ metric[2] }}</small></p>
            </div>
          </article>
        </section>

        <section class="command-grid">
          <aside class="screen-stack left-stack">
            <article class="screen-panel quality-panel">
              <header>
                <div><strong>质量检验</strong><span>QUALITY GATE</span></div>
                <b>{{ quality.inspectionCount || 0 }} 单</b>
              </header>
              <MesChart :option="qualityOption" height="292px" />
            </article>

            <article class="screen-panel equipment-panel">
              <header>
                <div><strong>设备状态</strong><span>EQUIPMENT STATUS</span></div>
                <b>{{ equipmentTotal }} 台</b>
              </header>
              <MesChart :option="equipmentOption" height="286px" />
            </article>
          </aside>

          <section class="center-stack">
            <article class="screen-panel command-panel">
              <div class="command-core">
                <div class="flow-board">
                  <header>
                    <div>
                      <strong>端到端制造链路</strong>
                      <span>ORDER · PLAN · WORK ORDER · QUALITY · WAREHOUSE</span>
                    </div>
                    <b><i />链路在线</b>
                  </header>
                  <div class="flow-line">
                    <template v-for="(step, index) in processSteps" :key="step[0]">
                      <div class="flow-node" :class="`tone-${step[2]}`">
                        <span><AppIcon :name="step[1]" :size="20" /></span>
                        <strong>{{ step[0] }}</strong>
                      </div>
                      <i v-if="index < processSteps.length - 1" />
                    </template>
                  </div>
                </div>

                <div class="topology-card">
                  <div class="topology-map" aria-hidden="true">
                    <i class="bus bus-a" />
                    <i class="bus bus-b" />
                    <i class="bus bus-c" />
                    <i class="bus bus-d" />
                    <span class="topology-node node-core"><AppIcon name="factory" :size="32" /></span>
                    <span class="topology-node node-order"><AppIcon name="clipboard" :size="18" /></span>
                    <span class="topology-node node-task"><AppIcon name="activity" :size="18" /></span>
                    <span class="topology-node node-quality"><AppIcon name="shield-check" :size="18" /></span>
                    <span class="topology-node node-stock"><AppIcon name="warehouse" :size="18" /></span>
                  </div>
                  <div class="topology-copy">
                    <span>LIVE TOPOLOGY</span>
                    <strong>制造现场数据链路在线</strong>
                    <small>订单、任务、质检、库存实时汇聚</small>
                  </div>
                  <div class="topology-status">
                    <div><small>采集</small><b>实时</b></div>
                    <div><small>链路</small><b>7</b></div>
                    <div><small>追溯</small><b>在线</b></div>
                  </div>
                </div>
              </div>
            </article>

            <article class="screen-panel trend-panel">
              <header>
                <div><strong>近七日生产趋势</strong><span>7-DAY PRODUCTION TREND</span></div>
                <b>按日报工</b>
              </header>
              <MesChart :option="trendOption" height="432px" />
            </article>
          </section>

          <aside class="screen-stack right-stack">
            <article class="screen-panel risk-panel">
              <header>
                <div><strong>运营风险</strong><span>OPERATION RISK</span></div>
                <b>实时</b>
              </header>
              <div class="risk-list">
                <div v-for="item in riskItems" :key="item.label" :class="`is-${item.level}`">
                  <span><AppIcon :name="item.icon" :size="19" /></span>
                  <div>
                    <strong>{{ item.label }}</strong>
                    <small>{{ item.value ? '需要关注' : '运行正常' }}</small>
                  </div>
                  <b>{{ formatNumber(item.value) }}</b>
                </div>
              </div>
            </article>

            <article class="screen-panel ai-panel">
              <header>
                <div><strong>AI 解读</strong><span>INTELLIGENT INSIGHT</span></div>
                <b>GPT-5.5</b>
              </header>
              <div class="ai-card">
                <span class="ai-orbit"><AppIcon name="bot" :size="30" /></span>
                <div>
                  <strong>智造 AI 分析助手</strong>
                  <p>基于真实看板汇总生成生产、质量、库存和设备建议。</p>
                </div>
                <button type="button" @click="aiOpen = true">
                  打开智能解读<AppIcon name="right" :size="16" />
                </button>
              </div>
            </article>

            <article class="screen-panel system-panel">
              <header>
                <div><strong>系统运行</strong><span>SYSTEM HEALTH</span></div>
                <b><i />在线</b>
              </header>
              <div class="system-grid">
                <div><span>数据刷新</span><strong>60s</strong></div>
                <div><span>接口来源</span><strong>{{ previewMode ? 'PREVIEW' : 'REAL API' }}</strong></div>
                <div><span>统计周期</span><strong>近 7 日</strong></div>
                <div><span>更新时间</span><strong>{{ now.format('HH:mm') }}</strong></div>
              </div>
            </article>
          </aside>
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
  color: #d6eaf8;
  background: #020c18;
  font-family: var(--mes-font);
}

.screen-background {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 50% -8%, #0e2a4a 0%, #020c18 52%),
    radial-gradient(ellipse at 10% 80%, rgb(0 100 200 / 10%), transparent 40%),
    radial-gradient(ellipse at 90% 80%, rgb(0 180 200 / 8%), transparent 36%);
}

.screen-background::after {
  position: absolute;
  inset: 0;
  opacity: .6;
  background-image:
    linear-gradient(rgb(24 144 255 / 7%) 1px, transparent 1px),
    linear-gradient(90deg, rgb(24 144 255 / 7%) 1px, transparent 1px);
  background-size: 48px 48px;
  content: '';
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
  display: grid;
  height: 88px;
  grid-template-columns: 420px 1fr 470px;
  align-items: center;
  padding: 0 30px;
  border-bottom: 1px solid rgb(24 144 255 / 22%);
  background: linear-gradient(180deg, rgb(4 18 36 / 96%), rgb(2 12 24 / 86%));
  box-shadow: 0 1px 0 rgb(24 144 255 / 14%), 0 18px 40px rgb(0 0 0 / 40%);
}

.screen-brand,
.screen-actions,
.screen-title,
.screen-logo,
.metric-card,
.metric-icon,
.screen-panel header,
.flow-line,
.flow-node,
.ai-card button {
  display: flex;
  align-items: center;
}

.screen-brand {
  gap: 14px;
}

.screen-logo {
  width: 48px;
  height: 48px;
  justify-content: center;
  color: #cce8ff;
  border: 1px solid rgb(24 144 255 / 50%);
  border-radius: 10px;
  background: linear-gradient(135deg, rgb(24 144 255 / 40%), rgb(0 80 160 / 30%));
  box-shadow: 0 0 20px rgb(24 144 255 / 25%), inset 0 1px 0 rgb(255 255 255 / 12%);
  animation: logoGlow 3s ease-in-out infinite;
}

.screen-brand > div,
.screen-panel header > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.screen-brand strong {
  color: #fff;
  font-size: 22px;
  font-weight: 760;
}

.screen-brand small,
.screen-title p,
.screen-panel header span {
  margin-top: 4px;
  color: #7493aa;
  font-size: 9px;
  font-weight: 850;
  letter-spacing: .14em;
}

.screen-title {
  justify-content: center;
  gap: 24px;
  text-align: center;
}

.screen-title i {
  width: 120px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgb(24 144 255 / 65%), transparent);
}

.screen-title h1 {
  color: #e8f4ff;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: .06em;
  text-shadow:
    0 0 20px rgb(24 144 255 / 55%),
    0 0 50px rgb(24 144 255 / 25%);
}

.screen-actions {
  justify-content: flex-end;
  gap: 10px;
}

.screen-time {
  display: flex;
  min-width: 144px;
  flex-direction: column;
  align-items: flex-end;
  margin-right: 8px;
}

.screen-time strong {
  color: #e7f7ff;
  font-size: 21px;
  font-variant-numeric: tabular-nums;
}

.screen-time span {
  color: #7792a8;
  font-size: 10px;
}

.screen-actions button {
  display: inline-flex;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  color: #9cb3c5;
  border: 1px solid rgb(92 152 208 / 26%);
  border-radius: 10px;
  background: rgb(12 22 32 / 70%);
  cursor: pointer;
}

.screen-actions button:hover {
  color: #e8fbff;
  border-color: rgb(88 212 255 / 54%);
  background: rgb(21 58 78 / 72%);
}

.screen-actions button.rotating {
  animation: rotate 1s linear infinite;
}

.screen-content {
  position: relative;
  height: 988px;
  padding: 18px 24px 24px;
}

.preview-flag,
.screen-error {
  position: absolute;
  z-index: 5;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 760;
}

.preview-flag {
  top: 9px;
  left: 50%;
  padding: 4px 13px;
  color: #efc776;
  border: 1px solid rgb(230 173 80 / 34%);
  background: rgb(46 33 13 / 82%);
  transform: translateX(-50%);
}

.screen-error {
  top: 22px;
  right: 24px;
  display: flex;
  min-height: 42px;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  color: #ffd0cc;
  border: 1px solid rgb(255 116 108 / 35%);
  background: rgb(53 19 17 / 88%);
}

.screen-error button {
  color: #ffd8d4;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.metric-strip {
  display: grid;
  height: 126px;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  --tone: #1890ff;
  --tone-soft: rgb(24 144 255 / 12%);
  position: relative;
  gap: 13px;
  padding: 14px 16px;
  overflow: hidden;
  border: 1px solid rgb(255 255 255 / 7%);
  border-left: 3px solid var(--tone);
  border-radius: 4px;
  background: rgb(4 16 30 / 90%);
  box-shadow: 0 2px 12px rgb(0 0 0 / 30%);
}

.metric-card::after {
  content: none;
}

.metric-card.tone-blue  { --tone: #1890ff; --tone-soft: rgb(24 144 255 / 12%); }
.metric-card.tone-cyan  { --tone: #00c8d4; --tone-soft: rgb(0 200 212 / 12%); }
.metric-card.tone-amber { --tone: #f5a623; --tone-soft: rgb(245 166 35 / 14%); }
.metric-card.tone-red   { --tone: #ff4d4f; --tone-soft: rgb(255 77 79 / 13%); }

.metric-icon {
  width: 44px;
  height: 44px;
  flex: none;
  justify-content: center;
  color: var(--tone);
  border: 1px solid color-mix(in srgb, var(--tone) 38%, transparent);
  border-radius: 13px;
  background: var(--tone-soft);
}

.metric-card label {
  color: #83a0b6;
  font-size: 12px;
}

.metric-card p {
  display: flex;
  align-items: baseline;
  gap: 7px;
  margin-top: 8px;
}

.metric-card strong {
  color: #f4fbff;
  font-size: 28px;
  font-weight: 780;
  line-height: 1;
}

.metric-card small {
  color: #6f8aa0;
  font-size: 10px;
}

.command-grid {
  display: grid;
  height: 820px;
  grid-template-columns: 418px minmax(0, 1fr) 418px;
  gap: 14px;
  margin-top: 14px;
}

.screen-stack,
.center-stack {
  display: grid;
  min-width: 0;
  gap: 14px;
}

.left-stack {
  grid-template-rows: 1fr 1fr;
}

.center-stack {
  grid-template-rows: 304px minmax(0, 1fr);
}

.right-stack {
  grid-template-rows: 300px 260px minmax(0, 1fr);
}

.screen-panel {
  --panel-accent: #1890ff;
  position: relative;
  min-width: 0;
  overflow: hidden;
  border: 1px solid rgb(24 144 255 / 16%);
  border-top: 2px solid var(--panel-accent);
  border-radius: 4px;
  background: rgb(4 16 30 / 92%);
  box-shadow: 0 4px 20px rgb(0 0 0 / 36%), inset 0 1px 0 rgb(255 255 255 / 3%);
}

.quality-panel   { --panel-accent: #00c8d4; }
.equipment-panel { --panel-accent: #1890ff; }
.command-panel   { --panel-accent: #1890ff; }
.trend-panel     { --panel-accent: #00c8d4; }
.risk-panel      { --panel-accent: #f5a623; }
.ai-panel        { --panel-accent: #1890ff; }
.system-panel    { --panel-accent: #52c41a; }

/* 移除角标装饰，仅保留顶部色线 */
.screen-panel::before,
.screen-panel::after {
  content: none;
}

.screen-panel header {
  height: 52px;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid rgb(24 144 255 / 14%);
  background: rgb(2 10 20 / 60%);
}

.screen-panel header strong {
  color: #cce0f0;
  font-size: 14px;
  font-weight: 680;
}

.screen-panel header b {
  color: color-mix(in srgb, var(--panel-accent) 85%, #cce0f0);
  font-size: 11px;
  font-weight: 680;
}

.screen-panel header b i,
.system-panel header b i {
  display: inline-block;
  width: 7px;
  height: 7px;
  margin-right: 6px;
  border-radius: 50%;
  background: #5bd99e;
  box-shadow: 0 0 12px #5bd99e;
  animation: statusPulse 2.4s ease-in-out infinite;
}

.command-core {
  display: grid;
  height: 100%;
  grid-template-columns: minmax(0, 1fr) 260px;
}

.flow-board {
  min-width: 0;
}

.flow-board header {
  height: 66px;
}

.flow-line {
  height: 238px;
  justify-content: space-around;
  gap: 6px;
  padding: 24px 22px;
}

.flow-line > i {
  width: 36px;
  height: 1px;
  flex: none;
  background: linear-gradient(90deg, rgb(88 212 255 / 55%), rgb(88 212 255 / 8%));
}

.flow-node {
  --flow-tone: #58d4ff;
  --flow-soft: rgb(88 212 255 / 12%);
  min-width: 64px;
  flex-direction: column;
  gap: 10px;
}

.flow-node.tone-blue { --flow-tone: #67b7ff; --flow-soft: rgb(103 183 255 / 12%); }
.flow-node.tone-cyan { --flow-tone: #58d4ff; --flow-soft: rgb(88 212 255 / 12%); }
.flow-node.tone-amber { --flow-tone: #e6ad50; --flow-soft: rgb(230 173 80 / 14%); }

.flow-node > span {
  display: inline-flex;
  width: 50px;
  height: 50px;
  align-items: center;
  justify-content: center;
  color: var(--flow-tone);
  border: 1px solid color-mix(in srgb, var(--flow-tone) 42%, rgb(92 152 208 / 18%));
  border-radius: 15px;
  background: var(--flow-soft);
  box-shadow: 0 0 24px color-mix(in srgb, var(--flow-tone) 14%, transparent);
}

.flow-node strong {
  color: #b9d1e2;
  font-size: 12px;
}

.topology-card {
  position: relative;
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: space-between;
  padding: 18px 16px 16px;
  overflow: hidden;
  border-left: 1px solid rgb(92 152 208 / 18%);
  background:
    radial-gradient(circle at 50% 32%, rgb(88 212 255 / 16%), transparent 42%),
    linear-gradient(180deg, rgb(7 16 25 / 84%), rgb(5 10 17 / 92%));
}

.topology-card::before {
  position: absolute;
  inset: 14px;
  border: 1px solid rgb(92 152 208 / 14%);
  content: '';
}

.topology-map {
  position: relative;
  height: 142px;
  border: 1px solid rgb(92 152 208 / 18%);
  background:
    linear-gradient(rgb(88 212 255 / 5%) 1px, transparent 1px),
    linear-gradient(90deg, rgb(88 212 255 / 5%) 1px, transparent 1px);
  background-size: 22px 22px;
}

.topology-node,
.bus {
  position: absolute;
  z-index: 1;
}

.topology-node {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #bcefff;
  border: 1px solid rgb(88 212 255 / 36%);
  border-radius: 12px;
  background: rgb(14 35 50 / 86%);
  box-shadow: 0 0 22px rgb(88 212 255 / 10%);
}

.node-core {
  top: 50%;
  left: 50%;
  width: 72px;
  height: 72px;
  color: #f2fbff;
  border-color: rgb(88 212 255 / 56%);
  border-radius: 18px;
  background: linear-gradient(135deg, rgb(var(--mes-primary-rgb) / 78%), rgb(16 61 84 / 86%));
  transform: translate(-50%, -50%);
}

.node-order,
.node-task,
.node-quality,
.node-stock {
  width: 40px;
  height: 40px;
}

.node-order { top: 13px; left: 18px; }
.node-task { top: 13px; right: 18px; }
.node-quality { right: 18px; bottom: 13px; }
.node-stock { bottom: 13px; left: 18px; }

.bus {
  height: 2px;
  background: linear-gradient(90deg, rgb(88 212 255 / 8%) 0%, rgb(88 212 255 / 80%) 50%, rgb(88 212 255 / 8%) 100%);
  background-size: 200% 100%;
  transform-origin: center;
  animation: busFlow 2.2s linear infinite;
}

.bus-b { animation-delay: -.55s; }
.bus-c { animation-delay: -1.1s; }
.bus-d { animation-delay: -1.65s; }

.bus-a,
.bus-b,
.bus-c,
.bus-d {
  width: 80px;
}

.bus-a { top: 45px; left: 58px; transform: rotate(22deg); }
.bus-b { top: 45px; right: 58px; transform: rotate(-22deg); }
.bus-c { right: 58px; bottom: 45px; transform: rotate(22deg); }
.bus-d { bottom: 45px; left: 58px; transform: rotate(-22deg); }

.topology-copy {
  position: relative;
  z-index: 1;
  padding: 12px 12px 0;
}

.topology-copy span {
  color: #58d4ff;
  font-size: 9px;
  font-weight: 850;
  letter-spacing: .13em;
}

.topology-copy strong,
.topology-copy small {
  display: block;
}

.topology-copy strong {
  margin-top: 6px;
  color: #f1f8ff;
  font-size: 14px;
}

.topology-copy small {
  margin-top: 5px;
  color: #7897ae;
  font-size: 10px;
}

.topology-status {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 7px;
}

.topology-status > div {
  min-width: 0;
  padding: 9px 6px;
  border: 1px solid rgb(92 152 208 / 18%);
  background: rgb(255 255 255 / 3%);
  text-align: center;
}

.topology-status small {
  display: block;
  color: #6f8aa0;
  font-size: 9px;
}

.topology-status b {
  display: block;
  margin-top: 4px;
  color: #dff7ff;
  font-size: 13px;
}

.risk-list {
  display: flex;
  height: 242px;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
  padding: 14px;
}

.risk-list > div {
  display: grid;
  min-height: 62px;
  grid-template-columns: 40px minmax(0, 1fr) auto;
  align-items: center;
  gap: 11px;
  padding: 9px 12px;
  border: 1px solid rgb(92 152 208 / 20%);
  border-left: 3px solid #58d4ff;
  border-radius: 14px;
  background: rgb(8 16 25 / 64%);
}

.risk-list > div.is-warning { border-left-color: #e6ad50; }
.risk-list > div.is-danger { border-left-color: #ff746c; }

.risk-list > div > span {
  display: inline-flex;
  width: 36px;
  height: 36px;
  align-items: center;
  justify-content: center;
  color: #83dfff;
  border-radius: 12px;
  background: rgb(88 212 255 / 12%);
}

.risk-list > div.is-warning > span { color: #efc473; background: rgb(230 173 80 / 14%); }
.risk-list > div.is-danger > span { color: #ff9a94; background: rgb(255 116 108 / 13%); }

.risk-list strong {
  color: #d5e4ef;
  font-size: 12px;
}

.risk-list small {
  display: block;
  margin-top: 3px;
  color: #7c96aa;
  font-size: 9px;
}

.risk-list b {
  color: #f5fbff;
  font-size: 24px;
}

.ai-card {
  display: grid;
  height: 202px;
  grid-template-columns: 64px minmax(0, 1fr);
  align-content: center;
  gap: 8px 14px;
  padding: 18px;
}

.ai-orbit {
  display: inline-flex;
  width: 60px;
  height: 60px;
  grid-row: span 2;
  align-items: center;
  justify-content: center;
  color: #d6f7ff;
  border: 1px solid rgb(88 212 255 / 42%);
  border-radius: 50%;
  background: rgb(21 70 92 / 40%);
  box-shadow: 0 0 0 8px rgb(88 212 255 / 8%), 0 0 34px rgb(88 212 255 / 14%);
}

.ai-card strong {
  color: #e7f2fa;
  font-size: 14px;
}

.ai-card p {
  margin-top: 6px;
  color: #83a0b6;
  font-size: 11px;
  line-height: 1.7;
}

.ai-card button {
  height: 38px;
  grid-column: 1 / -1;
  justify-content: center;
  gap: 8px;
  margin-top: 10px;
  color: #e8fbff;
  border: 1px solid rgb(88 212 255 / 42%);
  border-radius: 12px;
  background: linear-gradient(135deg, rgb(var(--mes-primary-rgb) / 76%), rgb(27 100 134 / 82%));
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.system-grid {
  display: grid;
  height: 202px;
  grid-template-columns: 1fr 1fr;
  padding: 14px;
}

.system-grid > div {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 14px;
  border-right: 1px solid rgb(92 152 208 / 18%);
  border-bottom: 1px solid rgb(92 152 208 / 18%);
}

.system-grid > div:nth-child(2n) { border-right: 0; }
.system-grid > div:nth-child(n+3) { border-bottom: 0; }

.system-grid span {
  color: #7c96aa;
  font-size: 10px;
}

.system-grid strong {
  margin-top: 6px;
  color: #dbe8f2;
  font-size: 13px;
}

@keyframes rotate {
  to { transform: rotate(360deg); }
}
@keyframes busFlow {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}

@keyframes statusPulse {
  0%, 100% { box-shadow: 0 0 7px #52c41a; opacity: 1; }
  50% { box-shadow: 0 0 16px #52c41a, 0 0 30px rgb(82 196 26 / 32%); opacity: .82; }
}

@keyframes logoGlow {
  0%, 100% { box-shadow: 0 0 18px rgb(24 144 255 / 26%), inset 0 1px 0 rgb(255 255 255 / 10%); }
  50% { box-shadow: 0 0 30px rgb(24 144 255 / 48%), inset 0 1px 0 rgb(255 255 255 / 16%); }
}
</style>
