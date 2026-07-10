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
import productImage from '@/assets/login-badminton.jpg'

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
  ['生产订单', summary.value.productionOrderCount, '单', 'clipboard', 'green'],
  ['生产工单', summary.value.workOrderCount, '单', 'package', 'blue'],
  ['运行任务', summary.value.runningTaskCount, '项', 'activity', 'cyan'],
  ['今日合格', summary.value.todayQualifiedQuantity, '件', 'check', 'green'],
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
  ['订单', 'clipboard', 'green'],
  ['工单', 'package', 'blue'],
  ['派工', 'users', 'cyan'],
  ['报工', 'activity', 'amber'],
  ['质检', 'shield-check', 'green'],
  ['入库', 'warehouse', 'blue'],
  ['追溯', 'scan', 'cyan']
]

const trendOption = computed(() => ({
  animationDuration: 700,
  color: ['#50d3a1', '#f06a63'],
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#07110e',
    borderColor: '#2b5b49',
    textStyle: { color: '#edf8f3', fontSize: 13 }
  },
  legend: {
    top: 0,
    right: 16,
    itemWidth: 18,
    itemHeight: 4,
    textStyle: { color: '#86a497', fontSize: 12 }
  },
  grid: { top: 48, right: 22, bottom: 28, left: 62 },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trend.value.map((item) => dayjs(item.date).format('MM/DD')),
    axisLine: { lineStyle: { color: '#28443a' } },
    axisTick: { show: false },
    axisLabel: { color: '#789388', fontSize: 11 }
  },
  yAxis: {
    type: 'value',
    splitNumber: 4,
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#789388', fontSize: 11 },
    splitLine: { lineStyle: { color: '#1b332a' } }
  },
  series: [
    {
      name: '合格产量',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      lineStyle: { width: 3 },
      areaStyle: { color: 'rgba(80,211,161,.1)' },
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
    color: hasData ? ['#50d3a1', '#f06a63'] : ['#29453a'],
    tooltip: {
      trigger: 'item',
      backgroundColor: '#07110e',
      borderColor: '#2b5b49',
      textStyle: { color: '#edf8f3' }
    },
    legend: {
      bottom: 2,
      left: 'center',
      itemWidth: 9,
      itemHeight: 9,
      textStyle: { color: '#86a497', fontSize: 11 }
    },
    series: [{
      type: 'pie',
      radius: ['63%', '80%'],
      center: ['50%', '45%'],
      label: {
        show: true,
        position: 'center',
        formatter: `${Number(quality.value.qualifiedRate || 0).toFixed(1)}%`,
        color: '#effbf6',
        fontSize: 34,
        fontWeight: 700
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
    backgroundColor: '#07110e',
    borderColor: '#2b5b49',
    textStyle: { color: '#edf8f3' }
  },
  grid: { top: 12, right: 30, bottom: 18, left: 76 },
  xAxis: {
    type: 'value',
    axisLabel: { color: '#789388', fontSize: 10 },
    splitLine: { lineStyle: { color: '#1b332a' } }
  },
  yAxis: {
    type: 'category',
    data: ['运行', '停机', '维保', '故障'],
    axisTick: { show: false },
    axisLine: { show: false },
    axisLabel: { color: '#9bb4aa', fontSize: 11 }
  },
  series: [{
    type: 'bar',
    barWidth: 13,
    data: [
      { value: Number(equipment.value.running || 0), itemStyle: { color: '#50d3a1', borderRadius: 2 } },
      { value: Number(equipment.value.stopped || 0), itemStyle: { color: '#769087', borderRadius: 2 } },
      { value: Number(equipment.value.maintenance || 0), itemStyle: { color: '#e0a548', borderRadius: 2 } },
      { value: Number(equipment.value.fault || 0), itemStyle: { color: '#f06a63', borderRadius: 2 } }
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
    <div class="big-screen-stage" :style="stageStyle">
      <header class="screen-header">
        <div class="screen-brand">
          <span class="screen-logo"><AppIcon name="factory" :size="25" /></span>
          <div><strong>羽球智造</strong><small>BADMINTON MES</small></div>
        </div>
        <div class="screen-title">
          <span />
          <div>
            <h1>生产运营驾驶舱</h1>
            <p>PRODUCTION OPERATION COMMAND CENTER</p>
          </div>
          <span />
        </div>
        <div class="screen-tools">
          <div class="screen-time">
            <strong>{{ timeText }}</strong>
            <span>{{ dateText }}</span>
          </div>
          <el-tooltip content="刷新数据" placement="bottom">
            <button type="button" aria-label="刷新数据" @click="loadData">
              <AppIcon name="refresh" :size="19" />
            </button>
          </el-tooltip>
          <el-tooltip content="全屏" placement="bottom">
            <button type="button" aria-label="全屏" @click="toggleFullscreen">
              <AppIcon name="fullscreen" :size="19" />
            </button>
          </el-tooltip>
          <el-tooltip content="返回后台" placement="bottom">
            <button type="button" aria-label="返回后台" @click="router.push('/dashboard')">
              <AppIcon name="logout" :size="19" />
            </button>
          </el-tooltip>
        </div>
      </header>

      <main class="screen-content">
        <div v-if="previewMode" class="preview-flag">FRAMEWORK PREVIEW · 预览数据</div>
        <div v-if="loadError" class="screen-error">
          <AppIcon name="alert" :size="18" />
          <span>{{ loadError }}</span>
          <button type="button" @click="loadData">重新加载</button>
        </div>

        <section class="screen-kpis">
          <article
            v-for="metric in metrics"
            :key="metric[0]"
            class="screen-kpi"
            :class="`tone-${metric[4]}`"
          >
            <span class="kpi-icon"><AppIcon :name="metric[3]" :size="23" /></span>
            <div>
              <span>{{ metric[0] }}</span>
              <p><strong>{{ formatNumber(metric[1]) }}</strong><small>{{ metric[2] }}</small></p>
            </div>
          </article>
        </section>

        <section class="screen-grid">
          <div class="screen-column side-column">
            <article class="screen-panel quality-screen-panel">
              <header><div><strong>质量检验概况</strong><span>QUALITY OVERVIEW</span></div><b>{{ quality.inspectionCount || 0 }} 单</b></header>
              <MesChart :option="qualityOption" height="304px" />
            </article>

            <article class="screen-panel equipment-screen-panel">
              <header><div><strong>设备运行状态</strong><span>EQUIPMENT STATUS</span></div><b>{{ equipmentTotal }} 台</b></header>
              <MesChart :option="equipmentOption" height="300px" />
            </article>
          </div>

          <div class="screen-column center-column">
            <article class="screen-panel production-flow-panel">
              <header><div><strong>羽毛球制造链路</strong><span>END-TO-END PRODUCTION FLOW</span></div><b><i />链路在线</b></header>
              <div class="flow-content">
                <div class="flow-steps">
                  <template v-for="(step, index) in processSteps" :key="step[0]">
                    <div class="flow-step" :class="`tone-${step[2]}`">
                      <span><AppIcon :name="step[1]" :size="20" /></span>
                      <strong>{{ step[0] }}</strong>
                    </div>
                    <AppIcon v-if="index < processSteps.length - 1" class="flow-arrow" name="right" :size="16" />
                  </template>
                </div>
                <div class="product-visual">
                  <img :src="productImage" alt="羽毛球产品" />
                  <div><span>BADMINTON</span><strong>产品制造现场</strong></div>
                </div>
              </div>
            </article>

            <article class="screen-panel trend-screen-panel">
              <header><div><strong>近七日生产趋势</strong><span>7-DAY PRODUCTION TREND</span></div><b>按日报工</b></header>
              <MesChart :option="trendOption" height="444px" />
            </article>
          </div>

          <div class="screen-column side-column right-column">
            <article class="screen-panel risk-panel">
              <header><div><strong>运营风险监测</strong><span>OPERATION RISK</span></div><b>实时</b></header>
              <div class="risk-list">
                <div v-for="item in riskItems" :key="item.label" :class="`is-${item.level}`">
                  <span><AppIcon :name="item.icon" :size="19" /></span>
                  <div><strong>{{ item.label }}</strong><small>{{ item.value ? '需要关注' : '运行正常' }}</small></div>
                  <b>{{ formatNumber(item.value) }}</b>
                </div>
              </div>
            </article>

            <article class="screen-panel ai-insight-panel">
              <header><div><strong>AI 运营解读</strong><span>INTELLIGENT INSIGHT</span></div><b>重点能力</b></header>
              <div class="ai-insight-body">
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
              <header><div><strong>系统运行</strong><span>SYSTEM HEALTH</span></div><b><i />在线</b></header>
              <div class="system-grid">
                <div><span>数据刷新</span><strong>60s</strong></div>
                <div><span>接口来源</span><strong>REAL API</strong></div>
                <div><span>统计周期</span><strong>近 7 日</strong></div>
                <div><span>更新时间</span><strong>{{ now.format('HH:mm') }}</strong></div>
              </div>
            </article>
          </div>
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
  color: #eaf5f0;
  background: #04090a;
  font-family: var(--mes-font);
}

.big-screen-stage {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 1920px;
  height: 1080px;
  overflow: hidden;
  transform-origin: center;
  background: #08110f;
}

.screen-header {
  display: grid;
  height: 84px;
  grid-template-columns: 360px 1fr 440px;
  align-items: center;
  padding: 0 26px;
  border-bottom: 1px solid #27433e;
  background: #0b1614;
}

.screen-brand,
.screen-tools,
.screen-title,
.screen-kpi,
.screen-panel header,
.flow-steps,
.flow-step,
.ai-insight-body button {
  display: flex;
  align-items: center;
}

.screen-brand {
  gap: 12px;
}

.screen-logo {
  display: inline-flex;
  width: 46px;
  height: 46px;
  align-items: center;
  justify-content: center;
  color: #78e0b7;
  border: 1px solid #2f6b54;
  border-radius: 50%;
  background: #10281f;
}

.screen-brand > div {
  display: flex;
  flex-direction: column;
}

.screen-brand strong {
  font-size: 20px;
  font-weight: 700;
}

.screen-brand small {
  margin-top: 3px;
  color: #678278;
  font-size: 9px;
  font-weight: 700;
}

.screen-title {
  justify-content: center;
  gap: 22px;
  text-align: center;
}

.screen-title > span {
  width: 86px;
  height: 1px;
  background: #4292a0;
}

.screen-title h1 {
  color: #f2fbf7;
  font-size: 28px;
  font-weight: 680;
  line-height: 1.2;
}

.screen-title p {
  margin-top: 5px;
  color: #678f91;
  font-size: 9px;
  font-weight: 700;
}

.screen-tools {
  justify-content: flex-end;
  gap: 9px;
}

.screen-time {
  display: flex;
  min-width: 132px;
  flex-direction: column;
  align-items: flex-end;
  margin-right: 8px;
}

.screen-time strong {
  color: #dff9ee;
  font-size: 19px;
  font-variant-numeric: tabular-nums;
}

.screen-time span {
  color: #69867a;
  font-size: 10px;
}

.screen-tools button {
  display: inline-flex;
  width: 36px;
  height: 36px;
  align-items: center;
  justify-content: center;
  color: #8ca79b;
  border: 1px solid #2a4942;
  border-radius: 4px;
  background: #0e1d1a;
  cursor: pointer;
}

.screen-tools button:hover {
  color: #dff9ee;
  border-color: #4a9c79;
  background: #173429;
}

.screen-content {
  position: relative;
  height: 996px;
  padding: 18px 22px 22px;
}

.preview-flag {
  position: absolute;
  z-index: 3;
  top: 8px;
  left: 50%;
  padding: 3px 12px;
  color: #d9b76d;
  border: 1px solid #66542e;
  border-radius: 3px;
  background: #211e12;
  font-size: 9px;
  font-weight: 700;
  transform: translateX(-50%);
}

.screen-error {
  position: absolute;
  z-index: 5;
  top: 20px;
  right: 22px;
  display: flex;
  height: 42px;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  color: #ffc0bc;
  border: 1px solid #6f3834;
  border-radius: 4px;
  background: #2c1513;
  font-size: 11px;
}

.screen-error button {
  color: #ffd7d4;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.screen-kpis {
  display: grid;
  height: 126px;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
}

.screen-kpi {
  position: relative;
  gap: 13px;
  padding: 18px;
  overflow: hidden;
  border: 1px solid #244338;
  border-top: 2px solid var(--tone);
  border-radius: 5px;
  background: #0d1b16;
}

.screen-kpi::after {
  position: absolute;
  right: -16px;
  bottom: -26px;
  width: 80px;
  height: 54px;
  border: 1px solid var(--tone);
  opacity: .12;
  content: '';
  transform: rotate(-25deg);
}

.screen-kpi.tone-green { --tone: #50d3a1; --tone-soft: #12382b; }
.screen-kpi.tone-blue { --tone: #62a9ed; --tone-soft: #112d40; }
.screen-kpi.tone-cyan { --tone: #54c5c0; --tone-soft: #103532; }
.screen-kpi.tone-amber { --tone: #e0a548; --tone-soft: #3a2c13; }
.screen-kpi.tone-red { --tone: #f06a63; --tone-soft: #3b1d1a; }

.kpi-icon {
  display: inline-flex;
  width: 44px;
  height: 44px;
  flex: none;
  align-items: center;
  justify-content: center;
  color: var(--tone);
  border: 1px solid color-mix(in srgb, var(--tone) 35%, transparent);
  border-radius: 5px;
  background: var(--tone-soft);
}

.screen-kpi > div > span {
  color: #7f9b8f;
  font-size: 12px;
}

.screen-kpi p {
  display: flex;
  align-items: baseline;
  gap: 7px;
  margin-top: 7px;
}

.screen-kpi p strong {
  color: #f0faf5;
  font-size: 27px;
  font-weight: 700;
  line-height: 1;
}

.screen-kpi p small {
  color: #6f897e;
  font-size: 10px;
}

.screen-grid {
  display: grid;
  height: 820px;
  grid-template-columns: 420px minmax(0, 1fr) 420px;
  gap: 14px;
  margin-top: 14px;
}

.screen-column {
  display: grid;
  min-width: 0;
  gap: 14px;
}

.side-column {
  grid-template-rows: 1fr 1fr;
}

.center-column {
  grid-template-rows: 270px minmax(0, 1fr);
}

.right-column {
  grid-template-rows: 320px 270px minmax(0, 1fr);
}

.screen-panel {
  --panel-accent: #50d3a1;
  position: relative;
  min-width: 0;
  overflow: hidden;
  border: 1px solid #29443e;
  border-top-color: color-mix(in srgb, var(--panel-accent) 62%, #29443e);
  border-radius: 5px;
  background: #0c1816;
}

.quality-screen-panel { --panel-accent: #50d3a1; }
.equipment-screen-panel { --panel-accent: #62a9ed; }
.production-flow-panel { --panel-accent: #54c5c0; }
.trend-screen-panel { --panel-accent: #50d3a1; }
.risk-panel { --panel-accent: #e0a548; }
.ai-insight-panel { --panel-accent: #62a9ed; }
.system-panel { --panel-accent: #7f9c92; }

.screen-panel::before,
.screen-panel::after {
  position: absolute;
  z-index: 2;
  width: 28px;
  height: 2px;
  background: var(--panel-accent);
  content: '';
}

.screen-panel::before { top: -1px; left: 14px; }
.screen-panel::after { right: 14px; bottom: -1px; }

.screen-panel header {
  height: 58px;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #203a34;
  background: #0f1d1a;
}

.screen-panel header > div {
  display: flex;
  flex-direction: column;
}

.screen-panel header strong {
  color: #dcece5;
  font-size: 15px;
  font-weight: 650;
}

.screen-panel header span {
  margin-top: 3px;
  color: #527064;
  font-size: 8px;
  font-weight: 700;
}

.screen-panel header > b {
  color: color-mix(in srgb, var(--panel-accent) 72%, #a6bbb2);
  font-size: 10px;
  font-weight: 600;
}

.screen-panel header > b i,
.system-panel header b i {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 6px;
  border-radius: 50%;
  background: #50d3a1;
}

.flow-content {
  display: grid;
  height: 211px;
  grid-template-columns: minmax(0, 1fr) 250px;
}

.flow-steps {
  min-width: 0;
  justify-content: space-around;
  gap: 4px;
  padding: 18px;
}

.flow-step {
  --flow-tone: #50d3a1;
  --flow-soft: #102c22;
  min-width: 58px;
  flex-direction: column;
  gap: 9px;
}

.flow-step.tone-green { --flow-tone: #50d3a1; --flow-soft: #102c22; }
.flow-step.tone-blue { --flow-tone: #62a9ed; --flow-soft: #10283a; }
.flow-step.tone-cyan { --flow-tone: #54c5c0; --flow-soft: #10302f; }
.flow-step.tone-amber { --flow-tone: #e0a548; --flow-soft: #352812; }

.flow-step > span {
  display: inline-flex;
  width: 42px;
  height: 42px;
  align-items: center;
  justify-content: center;
  color: var(--flow-tone);
  border: 1px solid color-mix(in srgb, var(--flow-tone) 45%, #233d36);
  border-radius: 50%;
  background: var(--flow-soft);
}

.flow-step strong {
  color: #a8c0b6;
  font-size: 11px;
}

.flow-arrow {
  flex: none;
  color: #36594b;
}

.product-visual {
  position: relative;
  overflow: hidden;
  border-left: 1px solid #244338;
}

.product-visual img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-visual::after {
  position: absolute;
  inset: 0;
  background: rgb(5 15 11 / 38%);
  content: '';
}

.product-visual > div {
  position: absolute;
  z-index: 1;
  right: 14px;
  bottom: 13px;
  left: 14px;
  display: flex;
  flex-direction: column;
  padding: 8px 10px;
  border-left: 2px solid #60d1a5;
  background: rgb(5 15 11 / 76%);
}

.product-visual span {
  color: #61a78c;
  font-size: 8px;
  font-weight: 700;
}

.product-visual strong {
  margin-top: 2px;
  font-size: 12px;
}

.risk-list {
  display: flex;
  height: 261px;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
  padding: 14px;
}

.risk-list > div {
  display: grid;
  min-height: 64px;
  grid-template-columns: 38px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border: 1px solid #28463b;
  border-left: 2px solid #4f9d7d;
  border-radius: 4px;
  background: #101f19;
}

.risk-list > div.is-warning { border-left-color: #e0a548; }
.risk-list > div.is-danger { border-left-color: #f06a63; }

.risk-list > div > span {
  display: inline-flex;
  width: 34px;
  height: 34px;
  align-items: center;
  justify-content: center;
  color: #7fc4a7;
  border-radius: 4px;
  background: #173328;
}

.risk-list > div.is-warning > span { color: #edbd6f; background: #352812; }
.risk-list > div.is-danger > span { color: #f18a84; background: #341917; }

.risk-list > div > div {
  display: flex;
  flex-direction: column;
}

.risk-list strong {
  color: #c7d9d1;
  font-size: 12px;
}

.risk-list small {
  margin-top: 3px;
  color: #668377;
  font-size: 9px;
}

.risk-list b {
  color: #f0faf5;
  font-size: 24px;
}

.ai-insight-body {
  display: grid;
  height: 211px;
  grid-template-columns: 62px minmax(0, 1fr);
  align-content: center;
  gap: 6px 14px;
  padding: 16px;
}

.ai-orbit {
  display: inline-flex;
  width: 58px;
  height: 58px;
  grid-row: span 2;
  align-items: center;
  justify-content: center;
  color: #c6edff;
  border: 1px solid #3e7898;
  border-radius: 50%;
  background: #133043;
  box-shadow: 0 0 0 7px rgb(54 132 171 / 10%);
}

.ai-insight-body > div {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.ai-insight-body > div strong {
  color: #dcece5;
  font-size: 13px;
}

.ai-insight-body p {
  margin-top: 5px;
  color: #769287;
  font-size: 10px;
  line-height: 1.6;
}

.ai-insight-body button {
  height: 36px;
  grid-column: 1 / -1;
  justify-content: center;
  gap: 8px;
  margin-top: 12px;
  color: #ddf4ff;
  border: 1px solid #397b9d;
  border-radius: 4px;
  background: #18516f;
  font-size: 11px;
  cursor: pointer;
}

.ai-insight-body button:hover {
  background: #1d678c;
}

.system-grid {
  display: grid;
  height: 170px;
  grid-template-columns: 1fr 1fr;
  padding: 12px;
}

.system-grid > div {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 14px;
  border-right: 1px solid #1e382e;
  border-bottom: 1px solid #1e382e;
}

.system-grid > div:nth-child(2n) { border-right: 0; }
.system-grid > div:nth-child(n+3) { border-bottom: 0; }

.system-grid span {
  color: #617e72;
  font-size: 9px;
}

.system-grid strong {
  margin-top: 5px;
  color: #c9ddd4;
  font-size: 12px;
}
</style>
