<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppIcon from '@/components/common/AppIcon.vue'
import {
  addManualScheduleItem,
  confirmScheduleScenario,
  getDraftProductionOrderPage,
  getManualEligibleLines,
  getManualOrderAiAdvice,
  getScheduleScenarioPage,
  getScheduleScenarioDetail,
  getScheduleScenarioItems,
  openManualScheduleDraft,
  removeManualScheduleItem,
  updateManualSchedulePlacement,
  validateManualScheduleDraft
} from '@/api/mfg/scheduling'
import { loadReferenceOptions } from '@/utils/resource-options'
import { hasPermission } from '@/utils/permission'

const draft = ref(null)
const scenarioOptions = ref([])
const selectedScenarioId = ref('')
const scheduleItems = ref([])
const pendingOrders = ref([])
const lineOptions = ref([])
const productOptions = ref([])
const orderOptions = ref([])
const loading = ref(false)
const scenarioLoading = ref(false)
const pendingLoading = ref(false)
const candidateLoading = ref(false)
const addingLineId = ref(null)
const savingPlacement = ref(false)
const removingItem = ref(false)
const validating = ref(false)
const confirming = ref(false)
const adviceLoading = ref(false)
const candidateOpen = ref(false)
const placementOpen = ref(false)
const addLineOpen = ref(false)
const confirmOpen = ref(false)
const selectedOrder = ref(null)
const selectedItem = ref(null)
const candidates = ref([])
const candidateError = ref('')
const advice = ref(null)
const visibleLineIds = ref([])
const timelineZoom = ref(1.5)
const pendingFilter = reactive({ keyword: '', productId: '', deliveryDate: '' })
const placementForm = reactive({ lineId: '', timeRange: [], remark: '' })
const confirmForm = reactive({ remark: '' })
const lineToAdd = ref('')
let candidateRequestSequence = 0

const weekdayLabels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const lineMap = computed(() => new Map(lineOptions.value.map((item) => [String(item.value), item.label])))
const productMap = computed(() => new Map(productOptions.value.map((item) => [String(item.value), item.label])))
const orderMap = computed(() => new Map([
  ...orderOptions.value.map((item) => [String(item.value), item.row || {}]),
  ...pendingOrders.value.map((item) => [String(item.productionOrderId), item])
]))
const filteredPendingOrders = computed(() => {
  const keyword = pendingFilter.keyword.trim().toLocaleLowerCase()
  return pendingOrders.value.filter((order) => {
    const matchesKeyword = !keyword || [order.productionOrderNo, productLabel(order.productId)]
      .some((value) => String(value || '').toLocaleLowerCase().includes(keyword))
    return matchesKeyword
      && (!pendingFilter.productId || String(order.productId) === String(pendingFilter.productId))
      && (!pendingFilter.deliveryDate || order.deliveryDate === pendingFilter.deliveryDate)
  })
})
const scheduledItems = computed(() => scheduleItems.value.filter((item) => item.lineId && item.plannedStartTime && item.plannedEndTime))
const conflictItems = computed(() => scheduleItems.value.filter((item) => item.status === 'CONFLICT'))
const canCreateSchedule = computed(() => hasPermission('mfg:schedule-scenario:create'))
const canViewPendingOrders = computed(() => hasPermission('mfg:production-order:view'))
const canUpdateSchedule = computed(() => hasPermission('mfg:schedule-scenario:update'))
const canDeleteSchedule = computed(() => hasPermission('mfg:schedule-scenario:delete'))
const canValidateSchedule = computed(() => hasPermission('mfg:schedule-scenario:validate'))
const canRequestAdvice = computed(() => hasPermission('mfg:schedule-scenario:ai-advice'))
const canConfirmSchedule = computed(() => hasPermission('mfg:schedule-scenario:confirm'))
const isEditable = computed(() => draft.value?.status === 'DRAFT')
const isCalculated = computed(() => draft.value?.status === 'CALCULATED')
const canConfirm = computed(() => canConfirmSchedule.value && isCalculated.value && !conflictItems.value.length)
const draftStatus = computed(() => ({
  DRAFT: { label: '编辑中', type: 'info' },
  CALCULATED: { label: '校验通过', type: 'success' },
  CONFIRMED: { label: '已确认', type: 'success' }
}[draft.value?.status] || { label: draft.value?.status || '--', type: 'info' }))
const placementLine = computed(() => candidates.value.find((item) => String(item.lineId) === String(placementForm.lineId)))
const lineGroups = computed(() => {
  const ids = new Set([...visibleLineIds.value.map(String), ...scheduledItems.value.map((item) => String(item.lineId))])
  return [...ids].map((id) => {
    const lineId = Number(id)
    const items = scheduledItems.value.filter((item) => String(item.lineId) === id)
      .sort((left, right) => dayjs(left.plannedStartTime).valueOf() - dayjs(right.plannedStartTime).valueOf())
    const trackEnds = []
    const layoutItems = items.map((item) => {
      const geometry = timelineGeometry(item)
      const track = trackEnds.findIndex((end) => geometry.start >= end)
      const timelineTrack = track === -1 ? trackEnds.length : track
      trackEnds[timelineTrack] = geometry.end + 0.2
      return { ...item, geometry, timelineTrack }
    })
    return { lineId, label: lineLabel(lineId), items: layoutItems, trackCount: Math.max(1, trackEnds.length) }
  }).sort((left, right) => left.label.localeCompare(right.label, 'zh-CN'))
})
const horizonStart = computed(() => dayjs(draft.value?.horizonStart))
const horizonEnd = computed(() => dayjs(draft.value?.horizonEnd))
const timelineCanvasWidth = computed(() => {
  if (!horizonStart.value.isValid() || !horizonEnd.value.isValid()) return 960
  return Math.max(960, Math.round(Math.max(1, horizonEnd.value.diff(horizonStart.value, 'hour', true) / 24) * 176 * timelineZoom.value))
})
const timelineTicks = computed(() => {
  if (!horizonStart.value.isValid() || !horizonEnd.value.isValid() || !horizonEnd.value.isAfter(horizonStart.value)) return []
  const totalHours = horizonEnd.value.diff(horizonStart.value, 'hour', true)
  const stepHours = totalHours <= 48 ? 4 : totalHours <= 168 ? 12 : 24
  const ticks = []
  let point = stepHours === 24 ? horizonStart.value.startOf('day') : horizonStart.value.startOf('hour')
  if (point.isBefore(horizonStart.value)) point = point.add(stepHours, 'hour')
  while (point.isBefore(horizonEnd.value) || point.isSame(horizonEnd.value)) {
    ticks.push({
      key: point.valueOf(),
      left: `${Math.min(100, Math.max(0, point.diff(horizonStart.value) / horizonEnd.value.diff(horizonStart.value) * 100))}%`,
      date: point.format('MM-DD'),
      detail: stepHours === 24 ? weekdayLabels[point.day()] : point.format('HH:mm')
    })
    point = point.add(stepHours, 'hour')
  }
  return ticks
})

function productLabel(productId) {
  return productMap.value.get(String(productId)) || `产品 #${productId}`
}

function lineLabel(lineId) {
  return lineMap.value.get(String(lineId)) || `产线 #${lineId}`
}

function scheduledOrder(item) {
  return orderMap.value.get(String(item?.productionOrderId)) || null
}

function scheduledOrderLabel(item) {
  const order = scheduledOrder(item)
  return order?.productionOrderNo || `订单 #${item?.productionOrderId || '--'}`
}

function scheduledProductLabel(item) {
  const order = scheduledOrder(item)
  return order?.productId ? productLabel(order.productId) : '型号待补充'
}

function formatNumber(value, digits = 2) {
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: digits }).format(Number(value || 0))
}

function formatDateTime(value) {
  return dayjs(value).isValid() ? dayjs(value).format('MM-DD HH:mm') : '--'
}

function timelineGeometry(item) {
  const start = dayjs(item.plannedStartTime)
  const end = dayjs(item.plannedEndTime)
  const total = horizonEnd.value.diff(horizonStart.value)
  if (!start.isValid() || !end.isValid() || total <= 0) return { start: 0, width: 0, end: 0 }
  const left = Math.max(0, Math.min(100, start.diff(horizonStart.value) / total * 100))
  const rawWidth = end.diff(start) / total * 100
  const width = Math.min(100 - left, Math.max((154 / timelineCanvasWidth.value) * 100, rawWidth))
  return { start: left, width, end: Math.min(100, left + width) }
}

function timelineBlockStyle(item) {
  return { left: `${item.geometry.start}%`, width: `${item.geometry.width}%`, top: `${14 + item.timelineTrack * 66}px` }
}

function resetPendingFilters() {
  pendingFilter.keyword = ''
  pendingFilter.productId = ''
  pendingFilter.deliveryDate = ''
}

async function loadReferenceData() {
  const results = await Promise.allSettled([
    loadReferenceOptions('factory-line'),
    loadReferenceOptions('mdm-product'),
    loadReferenceOptions('mfg-production-order')
  ])
  if (results[0].status === 'fulfilled') lineOptions.value = results[0].value
  if (results[1].status === 'fulfilled') productOptions.value = results[1].value
  if (results[2].status === 'fulfilled') orderOptions.value = results[2].value
}

async function loadPendingOrders() {
  if (!canViewPendingOrders.value) {
    pendingOrders.value = []
    return
  }
  pendingLoading.value = true
  try {
    const result = await getDraftProductionOrderPage({ current: 1, size: 100 })
    pendingOrders.value = result?.data?.records || []
  } catch {
    pendingOrders.value = []
  } finally {
    pendingLoading.value = false
  }
}

function clearScenario() {
  draft.value = null
  scheduleItems.value = []
  visibleLineIds.value = []
  advice.value = null
  selectedOrder.value = null
  selectedItem.value = null
}

async function loadScenarioOptions() {
  scenarioLoading.value = true
  try {
    const result = await getScheduleScenarioPage({ current: 1, size: 100 })
    scenarioOptions.value = result?.data?.records || []
  } finally {
    scenarioLoading.value = false
  }
}

async function loadDraft(scenarioId = selectedScenarioId.value || draft.value?.scenarioId) {
  if (!scenarioId) {
    selectedScenarioId.value = ''
    clearScenario()
    return
  }
  const [detailResult, itemResult] = await Promise.all([
    getScheduleScenarioDetail(scenarioId),
    getScheduleScenarioItems(scenarioId)
  ])
  draft.value = detailResult?.data || null
  scheduleItems.value = itemResult?.data || []
  selectedScenarioId.value = draft.value?.scenarioId ? String(draft.value.scenarioId) : ''
  advice.value = null
  selectedOrder.value = null
  selectedItem.value = null
  const itemLineIds = scheduleItems.value.filter((item) => item.lineId).map((item) => String(item.lineId))
  visibleLineIds.value = [...new Set(itemLineIds)]
}

async function selectScenario(scenarioId) {
  loading.value = true
  try {
    await loadDraft(scenarioId)
  } finally {
    loading.value = false
  }
}

async function openDraft() {
  if (!canCreateSchedule.value) return
  loading.value = true
  try {
    const result = await openManualScheduleDraft()
    const scenarioId = result?.data?.scenarioId
    await loadScenarioOptions()
    await loadDraft(scenarioId)
    ElMessage.success('排产草案已打开')
  } finally {
    loading.value = false
  }
}

async function refreshWorkbench() {
  loading.value = true
  try {
    await Promise.all([loadScenarioOptions(), loadPendingOrders(), loadReferenceData()])
    const selectedExists = scenarioOptions.value.some((item) => String(item.scenarioId) === String(selectedScenarioId.value))
    const scenarioId = selectedExists ? selectedScenarioId.value : scenarioOptions.value[0]?.scenarioId
    await loadDraft(scenarioId)
  } finally {
    loading.value = false
  }
}

async function openCandidates(order) {
  if (!canCreateSchedule.value || !draft.value || !isEditable.value) return
  const requestSequence = ++candidateRequestSequence
  selectedOrder.value = order
  candidates.value = []
  candidateError.value = ''
  candidateOpen.value = true
  candidateLoading.value = true
  try {
    const result = await getManualEligibleLines(draft.value.scenarioId, order.productionOrderId)
    if (requestSequence !== candidateRequestSequence) return
    candidates.value = result?.data || []
  } catch (error) {
    if (requestSequence !== candidateRequestSequence) return
    candidateError.value = error?.displayMessage || error?.message || '候选产线加载失败，请重试'
  } finally {
    if (requestSequence === candidateRequestSequence) candidateLoading.value = false
  }
}

async function addToLine(candidate) {
  if (!canCreateSchedule.value || !draft.value || !selectedOrder.value || addingLineId.value !== null) return
  addingLineId.value = candidate.lineId
  try {
    await addManualScheduleItem(draft.value.scenarioId, {
      scenarioVersion: draft.value.version,
      productionOrderId: selectedOrder.value.productionOrderId,
      lineId: candidate.lineId
    })
    visibleLineIds.value = [...new Set([...visibleLineIds.value.map(String), String(candidate.lineId)])]
    candidateOpen.value = false
    ElMessage.success('订单已加入产线时间安排')
    await Promise.all([loadDraft(), loadPendingOrders()])
  } finally {
    addingLineId.value = null
  }
}

function currentLineCandidate(item) {
  const option = lineOptions.value.find((line) => String(line.value) === String(item?.lineId))
  const row = option?.row || {}
  return {
    lineId: item?.lineId,
    lineCode: row.lineCode || option?.label || lineLabel(item?.lineId),
    lineName: row.lineName || option?.label || lineLabel(item?.lineId),
    workshopId: row.workshopId
  }
}

async function openAdvice(order) {
  if (!canRequestAdvice.value || !draft.value) return
  selectedOrder.value = order
  adviceLoading.value = true
  advice.value = null
  try {
    const result = await getManualOrderAiAdvice(draft.value.scenarioId, order.productionOrderId, { scenarioVersion: draft.value.version })
    advice.value = result?.data || null
  } finally {
    adviceLoading.value = false
  }
}

async function openPlacement(item) {
  if (!canUpdateSchedule.value || !draft.value || !isEditable.value) return
  selectedItem.value = item
  placementForm.lineId = String(item.lineId)
  placementForm.timeRange = [item.plannedStartTime, item.plannedEndTime]
  placementForm.remark = item.remark || ''
  const currentLine = currentLineCandidate(item)
  candidates.value = [currentLine]
  placementOpen.value = true
  candidateLoading.value = true
  try {
    const result = await getManualEligibleLines(draft.value.scenarioId, item.productionOrderId)
    const loaded = result?.data || []
    candidates.value = loaded.some((line) => String(line.lineId) === String(item.lineId))
      ? loaded
      : [currentLine, ...loaded]
  } finally {
    candidateLoading.value = false
  }
}

async function savePlacement() {
  if (!canUpdateSchedule.value || !draft.value || !selectedItem.value || !placementForm.lineId || placementForm.timeRange.length !== 2) return
  savingPlacement.value = true
  try {
    await updateManualSchedulePlacement(draft.value.scenarioId, selectedItem.value.scheduleItemId, {
      scenarioVersion: draft.value.version,
      itemVersion: selectedItem.value.version,
      lineId: Number(placementForm.lineId),
      plannedStartTime: placementForm.timeRange[0],
      plannedEndTime: placementForm.timeRange[1],
      remark: placementForm.remark.trim() || undefined
    })
    visibleLineIds.value = [...new Set([...visibleLineIds.value.map(String), String(placementForm.lineId)])]
    placementOpen.value = false
    ElMessage.success('时间安排已更新')
    await loadDraft()
  } finally {
    savingPlacement.value = false
  }
}

async function removeItem(item) {
  if (!canDeleteSchedule.value || !draft.value || !isEditable.value || removingItem.value) return
  try {
    await ElMessageBox.confirm('移出草案后，订单会回到待排列表。', '移出订单', { type: 'warning', confirmButtonText: '移出', cancelButtonText: '取消' })
  } catch {
    return
  }
  removingItem.value = true
  try {
    await removeManualScheduleItem(draft.value.scenarioId, item.scheduleItemId, {
      scenarioVersion: draft.value.version,
      itemVersion: item.version
    })
    placementOpen.value = false
    selectedItem.value = null
    ElMessage.success('订单已移出草案')
    await Promise.all([loadDraft(), loadPendingOrders()])
  } finally {
    removingItem.value = false
  }
}

function addVisibleLine() {
  if (!lineToAdd.value) return
  visibleLineIds.value = [...new Set([...visibleLineIds.value.map(String), String(lineToAdd.value)])]
  lineToAdd.value = ''
  addLineOpen.value = false
}

async function validateDraft() {
  if (!canValidateSchedule.value || !draft.value || !isEditable.value) return
  validating.value = true
  try {
    await validateManualScheduleDraft(draft.value.scenarioId, { scenarioVersion: draft.value.version })
    ElMessage.success('草案校验完成')
    await loadDraft()
  } finally {
    validating.value = false
  }
}

function openConfirmDialog() {
  if (!canConfirm.value) return
  confirmForm.remark = ''
  confirmOpen.value = true
}

async function confirmDraft() {
  if (!canConfirmSchedule.value || !draft.value) return
  confirming.value = true
  try {
    await confirmScheduleScenario(draft.value.scenarioId, { version: draft.value.version, remark: confirmForm.remark.trim() || undefined })
    confirmOpen.value = false
    ElMessage.success('草案已确认，生产计划已生成')
    await Promise.all([loadDraft(), loadPendingOrders()])
  } finally {
    confirming.value = false
  }
}

onMounted(() => {
  refreshWorkbench()
})
</script>

<template>
  <div class="mes-page manual-scheduling-workbench">
    <header class="mes-page-header workbench-header">
      <div class="workbench-heading">
        <span class="workbench-heading-icon"><AppIcon name="route" :size="23" /></span>
        <div><h1 class="mes-page-title">智能排产工作台</h1><p class="mes-page-subtitle">订单选线、时间安排、风险校验与人工确认</p></div>
      </div>
      <div class="workbench-actions">
        <el-select v-model="selectedScenarioId" class="scenario-selector" filterable :loading="scenarioLoading" placeholder="选择排产方案" @change="selectScenario"><el-option v-for="item in scenarioOptions" :key="item.scenarioId" :label="`${item.scenarioNo} · ${item.status}`" :value="String(item.scenarioId)" /></el-select>
        <el-button v-if="canCreateSchedule" type="primary" @click="openDraft"><AppIcon name="plus" :size="15" />打开排产草案</el-button>
        <el-tag v-if="draft" :type="draftStatus.type" effect="plain">{{ draftStatus.label }}</el-tag>
        <el-tooltip content="刷新工作台" placement="bottom"><el-button circle :loading="loading" aria-label="刷新工作台" @click="refreshWorkbench"><AppIcon name="refresh" :size="16" /></el-button></el-tooltip>
      </div>
    </header>

    <section class="workbench-summary">
      <div><span>待排订单</span><strong>{{ pendingOrders.length }}</strong></div>
      <div><span>草案订单</span><strong>{{ draft?.orderCount || 0 }}</strong></div>
      <div><span>阻断风险</span><strong :class="{ danger: conflictItems.length }">{{ draft?.conflictOrderCount || 0 }}</strong></div>
      <div><span>预计延期</span><strong :class="{ warning: Number(draft?.lateOrderCount || 0) > 0 }">{{ draft?.lateOrderCount || 0 }}</strong></div>
      <div><span>当前方案</span><strong>{{ draft?.scenarioNo || '--' }}</strong><small>仅人工确认后生成生产计划</small></div>
    </section>

    <section class="top-grid">
      <section class="mes-panel pending-panel">
        <div class="mes-panel-header"><div><h2 class="mes-panel-title">待排生产订单</h2><span>当前 DRAFT</span></div><small>{{ filteredPendingOrders.length }} 条</small></div>
        <div class="filter-row">
          <el-input v-model="pendingFilter.keyword" clearable placeholder="订单号或型号" />
          <el-select v-model="pendingFilter.productId" clearable placeholder="产品"><el-option v-for="item in productOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
          <el-date-picker v-model="pendingFilter.deliveryDate" type="date" value-format="YYYY-MM-DD" placeholder="交期" />
          <el-tooltip content="重置筛选" placement="top"><el-button circle aria-label="重置筛选" @click="resetPendingFilters"><AppIcon name="refresh" :size="15" /></el-button></el-tooltip>
        </div>
        <el-table v-loading="pendingLoading" :data="filteredPendingOrders" size="small" max-height="300" class="compact-table">
          <el-table-column label="订单 / 型号" min-width="160"><template #default="{ row }"><strong>{{ row.productionOrderNo }}</strong><small>{{ productLabel(row.productId) }}</small></template></el-table-column>
          <el-table-column label="数量" width="86" align="right"><template #default="{ row }">{{ formatNumber(row.orderQuantity, 4) }}</template></el-table-column>
          <el-table-column label="交期" width="94"><template #default="{ row }">{{ row.deliveryDate || '--' }}</template></el-table-column>
          <el-table-column label="操作" width="112" fixed="right"><template #default="{ row }"><el-tooltip v-if="canCreateSchedule" content="添加到产线" placement="top"><el-button link type="primary" aria-label="添加到产线" :disabled="!isEditable" @click="openCandidates(row)"><AppIcon name="plus" :size="16" /></el-button></el-tooltip><el-tooltip v-if="canRequestAdvice" content="查看 AI 建议" placement="top"><el-button link aria-label="查看 AI 建议" :disabled="!draft" @click="openAdvice(row)"><AppIcon name="bot" :size="16" /></el-button></el-tooltip></template></el-table-column>
          <template #empty><el-empty description="暂无待排订单" :image-size="54" /></template>
        </el-table>
      </section>

      <section class="mes-panel detail-panel">
        <div class="mes-panel-header"><div><h2 class="mes-panel-title">详情与风险确认</h2><span>{{ selectedOrder ? selectedOrder.productionOrderNo : '选择订单查看建议' }}</span></div><AppIcon name="shield-check" :size="18" /></div>
        <div v-if="adviceLoading" class="panel-loading"><el-skeleton :rows="4" animated /></div>
        <div v-else-if="advice" class="advice-content"><p>{{ advice.answer }}</p><ul v-if="advice.suggestions?.length"><li v-for="item in advice.suggestions" :key="item">{{ item }}</li></ul><small>{{ advice.disclaimer }}</small></div>
        <el-empty v-else description="点击订单的建议按钮查看产线安排建议" :image-size="48" />
        <div class="risk-footer"><div><strong>{{ conflictItems.length ? `${conflictItems.length} 项阻断风险` : isCalculated ? '草案已通过校验' : '请完成时间安排后校验草案' }}</strong><span>{{ isCalculated ? '可人工确认并生成生产计划' : '校验会复核产线能力、周历与时间冲突' }}</span></div><div class="risk-actions"><el-button v-if="canValidateSchedule" :loading="validating" :disabled="!isEditable || !scheduleItems.length" @click="validateDraft"><AppIcon name="shield-check" :size="15" />校验草案</el-button><el-button v-if="canConfirmSchedule" type="success" :disabled="!canConfirm" @click="openConfirmDialog"><AppIcon name="check" :size="15" />确认</el-button></div></div>
      </section>
    </section>

    <section class="mes-panel timeline-panel">
      <div class="mes-panel-header"><div><h2 class="mes-panel-title">手工时间安排</h2><span>{{ draft ? `${formatDateTime(draft.horizonStart)} 至 ${formatDateTime(draft.horizonEnd)}` : '暂无方案' }}</span></div><div class="timeline-actions"><el-button v-if="canUpdateSchedule" :disabled="!isEditable" @click="addLineOpen = true"><AppIcon name="plus" :size="15" />添加产线</el-button><el-button v-if="canValidateSchedule" :disabled="!isEditable" @click="validateDraft"><AppIcon name="shield-check" :size="15" />编辑完成并校验</el-button></div></div>
      <div class="timeline-toolbar"><span>点击订单块可编辑产线与生产时间</span><div><el-button circle size="small" aria-label="缩小时间轴" :disabled="timelineZoom <= .75" @click="timelineZoom = Number(Math.max(.75, timelineZoom - .25).toFixed(2))"><AppIcon name="zoom-out" :size="14" /></el-button><el-slider v-model="timelineZoom" :min=".75" :max="3" :step=".25" :show-tooltip="false" /><el-button circle size="small" aria-label="放大时间轴" :disabled="timelineZoom >= 3" @click="timelineZoom = Number(Math.min(3, timelineZoom + .25).toFixed(2))"><AppIcon name="zoom-in" :size="14" /></el-button></div></div>
      <div v-if="lineGroups.length" class="timeline-board"><div class="timeline-inner" :style="{ '--timeline-width': `${timelineCanvasWidth}px` }"><div class="timeline-head"><div class="timeline-corner">产线</div><div class="timeline-axis"><span v-for="tick in timelineTicks" :key="tick.key" class="axis-tick" :style="{ left: tick.left }"><b>{{ tick.date }}</b><small>{{ tick.detail }}</small></span></div></div><div v-for="group in lineGroups" :key="group.lineId" class="timeline-row"><div class="line-label"><strong>{{ group.label }}</strong><small>{{ group.items.length }} 个订单</small></div><div class="timeline-lane" :style="{ '--tracks': group.trackCount }"><i v-for="tick in timelineTicks" :key="tick.key" :style="{ left: tick.left }" /><button v-for="item in group.items" :key="item.scheduleItemId" type="button" class="timeline-block" :class="{ conflict: item.status === 'CONFLICT', late: Number(item.tardinessMinutes || 0) > 0 }" :style="timelineBlockStyle(item)" :disabled="!isEditable || !canUpdateSchedule" @click="openPlacement(item)"><strong>{{ scheduledOrderLabel(item) }}</strong><span>{{ scheduledProductLabel(item) }} · {{ formatDateTime(item.plannedStartTime) }}</span></button></div></div></div></div>
      <el-empty v-else :description="draft ? '当前方案暂无时间安排' : '暂无可查看的排产方案'" :image-size="62" />
      <div v-if="conflictItems.length" class="conflict-strip"><AppIcon name="alert" :size="16" /><span v-for="item in conflictItems" :key="item.scheduleItemId">订单 #{{ item.productionOrderId }}：{{ item.conflictReason || '存在排程冲突' }}</span></div>
    </section>

    <el-dialog v-model="candidateOpen" title="选择生产产线" width="min(720px, calc(100vw - 32px))" destroy-on-close><p class="dialog-tip">{{ selectedOrder?.productionOrderNo }} · {{ productLabel(selectedOrder?.productId) }}。选择后会自动加入时间轴；未显示的产线会自动新增到左侧。</p><el-table v-loading="candidateLoading" element-loading-text="正在匹配可生产产线..." :data="candidates" size="small"><el-table-column label="产线" min-width="170"><template #default="{ row }"><strong>{{ row.lineCode || row.lineName }}</strong><small>{{ row.lineName }}</small></template></el-table-column><el-table-column label="车间" width="94"><template #default="{ row }">#{{ row.workshopId }}</template></el-table-column><el-table-column label="最早可用" min-width="142"><template #default="{ row }">{{ formatDateTime(row.earliestStartTime) }}</template></el-table-column><el-table-column label="预计结束" min-width="142"><template #default="{ row }">{{ formatDateTime(row.earliestEndTime) }}</template></el-table-column><el-table-column label="操作" width="82"><template #default="{ row }"><el-button v-if="canCreateSchedule" link type="primary" :loading="String(addingLineId) === String(row.lineId)" :disabled="addingLineId !== null" @click="addToLine(row)">添加</el-button></template></el-table-column><template #empty><el-empty :description="candidateError || '没有符合能力和周历条件的产线'" :image-size="50" /></template></el-table></el-dialog>

    <el-drawer v-model="placementOpen" title="编辑订单时间安排" size="min(440px, 100vw)" destroy-on-close><el-form label-position="top"><el-form-item label="订单"><el-input :model-value="selectedItem ? `${scheduledOrderLabel(selectedItem)} · ${scheduledProductLabel(selectedItem)}` : ''" disabled /></el-form-item><el-form-item label="生产产线"><el-select v-model="placementForm.lineId" class="full-width" :loading="candidateLoading" loading-text="正在加载候选产线"><el-option v-for="line in candidates" :key="line.lineId" :label="`${line.lineCode || line.lineName} · 车间 #${line.workshopId || '--'}`" :value="String(line.lineId)" /></el-select></el-form-item><el-form-item label="所属车间"><el-input :model-value="placementLine?.workshopId ? `车间 #${placementLine.workshopId}` : '--'" disabled /></el-form-item><el-form-item label="生产时间"><el-date-picker v-model="placementForm.timeRange" class="full-width" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY-MM-DD HH:mm" start-placeholder="开始" end-placeholder="结束" /></el-form-item><el-form-item label="备注"><el-input v-model="placementForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item></el-form><template #footer><el-button v-if="canDeleteSchedule" type="danger" plain :loading="removingItem" :disabled="savingPlacement" @click="removeItem(selectedItem)"><AppIcon name="trash" :size="15" />移出草案</el-button><el-button :disabled="removingItem" @click="placementOpen = false">取消</el-button><el-button v-if="canUpdateSchedule" type="primary" :loading="savingPlacement" :disabled="removingItem" @click="savePlacement">保存安排</el-button></template></el-drawer>

    <el-dialog v-model="addLineOpen" title="添加时间轴产线" width="min(420px, calc(100vw - 32px))" destroy-on-close><p class="dialog-tip">仅改变当前时间轴显示，不创建产线主数据。</p><el-select v-model="lineToAdd" class="full-width" filterable placeholder="选择现有产线"><el-option v-for="line in lineOptions" :key="line.value" :label="line.label" :value="String(line.value)" /></el-select><template #footer><el-button @click="addLineOpen = false">取消</el-button><el-button type="primary" @click="addVisibleLine">添加</el-button></template></el-dialog>

    <el-dialog v-model="confirmOpen" title="确认手工排程草案" width="min(460px, calc(100vw - 32px))" destroy-on-close><p class="confirm-copy">确认后将为所有已排订单创建生产计划，并把生产订单下达为 `RELEASED`。</p><el-form label-position="top"><el-form-item label="确认备注"><el-input v-model="confirmForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item></el-form><template #footer><el-button @click="confirmOpen = false">取消</el-button><el-button v-if="canConfirmSchedule" type="success" :loading="confirming" @click="confirmDraft">确认并生成计划</el-button></template></el-dialog>
  </div>
</template>

<style scoped>
.workbench-header,.workbench-heading,.workbench-actions,.mes-panel-header>div,.timeline-actions,.timeline-toolbar,.timeline-toolbar>div,.risk-actions{display:flex;align-items:center}.workbench-header{justify-content:space-between;gap:16px}.workbench-heading{gap:12px}.workbench-heading-icon{display:inline-flex;width:44px;height:44px;align-items:center;justify-content:center;color:var(--mes-primary);border:1px solid var(--mes-primary-border);border-radius:6px;background:var(--mes-primary-soft)}.workbench-actions{gap:8px}.scenario-selector{width:250px}.workbench-summary{display:grid;grid-template-columns:repeat(4,minmax(0,1fr)) minmax(250px,1.6fr);margin-bottom:14px;border:1px solid var(--mes-border);border-left:3px solid var(--mes-primary);background:var(--mes-surface)}.workbench-summary>div{display:flex;min-width:0;flex-direction:column;justify-content:center;gap:3px;padding:12px 16px;border-right:1px solid var(--mes-border)}.workbench-summary>div:last-child{border:0}.workbench-summary span,.workbench-summary small{color:var(--mes-text-muted);font-size:10px}.workbench-summary strong{overflow:hidden;font-size:17px;text-overflow:ellipsis;white-space:nowrap}.workbench-summary .danger{color:var(--mes-danger)}.workbench-summary .warning{color:var(--mes-warning)}.top-grid{display:grid;grid-template-columns:minmax(0,1.35fr) minmax(330px,.9fr);gap:14px;margin-bottom:14px}.mes-panel{overflow:hidden;border-radius:var(--mes-radius)}.mes-panel-header{display:flex;min-height:52px;align-items:center;justify-content:space-between;gap:12px;padding:9px 14px;border-bottom:1px solid var(--mes-border)}.mes-panel-header>div{min-width:0;gap:8px}.mes-panel-header span,.mes-panel-header small{overflow:hidden;color:var(--mes-text-muted);font-size:10px;text-overflow:ellipsis;white-space:nowrap}.filter-row{display:flex;gap:7px;padding:8px 12px;border-bottom:1px solid var(--mes-border);background:var(--mes-surface-muted)}.filter-row :deep(.el-input){min-width:0;flex:1}.filter-row :deep(.el-select){width:145px}.filter-row :deep(.el-date-editor){width:118px}.compact-table{width:100%}.compact-table :deep(.el-table__cell){padding:7px 0}.compact-table strong,.compact-table small{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.compact-table small{margin-top:2px;color:var(--mes-text-muted);font-size:10px}.detail-panel{display:flex;min-height:358px;flex-direction:column}.detail-panel>:deep(.app-icon){color:var(--mes-primary)}.advice-content{flex:1;padding:14px;color:var(--mes-text-secondary);font-size:12px;line-height:1.7;white-space:pre-line}.advice-content p{margin:0}.advice-content ul{margin:10px 0;padding-left:18px}.advice-content li+li{margin-top:4px}.advice-content small{display:block;margin-top:12px;color:var(--mes-text-muted);font-size:10px}.panel-loading{padding:14px}.risk-footer{display:flex;align-items:center;justify-content:space-between;gap:10px;margin-top:auto;padding:12px 14px;border-top:1px solid var(--mes-border);background:var(--mes-surface-muted)}.risk-footer>div:first-child{display:flex;min-width:0;flex-direction:column;gap:3px}.risk-footer strong{font-size:12px}.risk-footer span{color:var(--mes-text-muted);font-size:10px}.risk-actions{flex:none;gap:7px}.risk-actions :deep(.el-button)>span,.timeline-actions :deep(.el-button)>span{display:inline-flex;align-items:center;gap:5px}.timeline-panel{min-height:540px}.timeline-toolbar{justify-content:space-between;gap:12px;padding:8px 14px;border-bottom:1px solid var(--mes-border);background:var(--mes-surface-muted)}.timeline-toolbar>span{color:var(--mes-text-secondary);font-size:11px}.timeline-toolbar>div{gap:5px}.timeline-toolbar :deep(.el-slider){width:108px;margin:0 3px}.timeline-board{overflow:auto}.timeline-inner{width:max-content;min-width:100%}.timeline-head,.timeline-row{display:grid;grid-template-columns:172px var(--timeline-width,960px)}.timeline-head{position:sticky;top:0;z-index:3;border-bottom:1px solid var(--mes-border);background:var(--mes-surface-muted)}.timeline-corner,.line-label{position:sticky;left:0;z-index:2;display:flex;flex-direction:column;justify-content:center;background:var(--mes-surface)}.timeline-corner{z-index:4;min-height:48px;padding:0 14px;color:var(--mes-text-secondary);font-size:11px;font-weight:650;background:var(--mes-surface-muted)}.timeline-axis{position:relative;height:48px}.axis-tick{position:absolute;bottom:0;display:flex;height:100%;flex-direction:column;justify-content:center;gap:3px;padding-left:4px;border-left:1px solid var(--mes-border);color:var(--mes-text-secondary);font-size:10px}.axis-tick small{color:var(--mes-text-muted);font-size:9px}.timeline-row{min-height:86px;border-bottom:1px solid var(--mes-border)}.line-label{gap:3px;padding:12px 14px;border-right:1px solid var(--mes-border)}.line-label strong{overflow:hidden;max-width:140px;text-overflow:ellipsis;white-space:nowrap;font-size:12px}.line-label small{color:var(--mes-text-muted);font-size:10px}.timeline-lane{position:relative;min-height:calc(var(--tracks,1)*66px + 18px);background:#fff}.timeline-lane>i{position:absolute;top:0;bottom:0;width:1px;background:var(--mes-border);opacity:.7}.timeline-block{position:absolute;display:flex;min-width:154px;height:52px;overflow:hidden;flex-direction:column;justify-content:center;gap:3px;padding:6px 8px;color:#fff;border:1px solid rgb(var(--mes-primary-rgb)/75%);border-radius:5px;background:var(--mes-primary);box-shadow:0 3px 8px rgb(var(--mes-primary-rgb)/16%);font:inherit;text-align:left;cursor:pointer}.timeline-block:disabled{cursor:default}.timeline-block strong,.timeline-block span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.timeline-block strong{font-size:11px}.timeline-block span{color:rgb(255 255 255 / 84%);font-size:10px}.timeline-block.late{border-color:#a96d00;background:var(--mes-warning)}.timeline-block.conflict{border-color:#b24a45;background:var(--mes-danger)}.conflict-strip{display:flex;flex-wrap:wrap;align-items:center;gap:8px;padding:10px 14px;color:var(--mes-danger);border-top:1px solid var(--mes-border);background:var(--mes-danger-soft);font-size:11px}.dialog-tip,.confirm-copy{margin:0 0 14px;color:var(--mes-text-secondary);font-size:12px;line-height:1.65}.full-width{width:100%}@media(max-width:1050px){.top-grid{grid-template-columns:1fr}.workbench-summary{grid-template-columns:repeat(4,minmax(0,1fr))}.workbench-summary>div:last-child{grid-column:1/-1;border-top:1px solid var(--mes-border)}}@media(max-width:680px){.workbench-header{align-items:flex-start;flex-direction:column}.workbench-actions{width:100%;flex-wrap:wrap;justify-content:flex-start}.scenario-selector{width:min(100%,250px)}.workbench-summary{grid-template-columns:repeat(2,minmax(0,1fr))}.workbench-summary>div{border-bottom:1px solid var(--mes-border)}.filter-row{flex-wrap:wrap}.filter-row :deep(.el-input){min-width:180px}.timeline-actions{width:100%;justify-content:space-between}.mes-panel-header{align-items:flex-start;flex-direction:column}.timeline-head,.timeline-row{grid-template-columns:132px var(--timeline-width,720px)}.line-label{padding:10px}.line-label strong{max-width:106px}.risk-footer{align-items:flex-start;flex-direction:column}.risk-actions{width:100%}.risk-actions :deep(.el-button){flex:1}.timeline-toolbar{align-items:flex-start;flex-direction:column}}
</style>
