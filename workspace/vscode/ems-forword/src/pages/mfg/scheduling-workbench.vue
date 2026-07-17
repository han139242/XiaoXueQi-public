<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppIcon from '@/components/common/AppIcon.vue'
import {
  addManualScheduleItem,
  getManualBoardAiAdvice,
  confirmManualScheduleItem,
  getManualEligibleLines,
  getManualScheduleBoard,
  openManualScheduleDraft,
  removeManualScheduleItem,
  updateManualSchedulePlacement
} from '@/api/mfg/scheduling'
import { loadReferenceOptions } from '@/utils/resource-options'
import { hasPermission } from '@/utils/permission'

// 人工排产工作台把待排订单拖入/加入产线时间轴，调整时间后确认生成正式生产计划。
// board是后端返回的完整看板，draft是可编辑草稿，timelineItems是时间轴中的计划项。
const board = ref(null)
const draft = ref(null)
const timelineItems = ref([])
const pendingOrders = ref([])
const lineOptions = ref([])
const productOptions = ref([])
const orderOptions = ref([])
const loading = ref(false)
const candidateLoading = ref(false)
const addingLineId = ref(null)
const savingPlacement = ref(false)
const removingItem = ref(false)
const confirmingItem = ref(false)
const aiAdviceLoading = ref(false)
const candidateOpen = ref(false)
const placementOpen = ref(false)
const addLineOpen = ref(false)
const selectedOrder = ref(null)
const selectedItem = ref(null)
const candidates = ref([])
const candidateError = ref('')
const aiAdvice = ref(null)
const aiAdviceStale = ref(false)
const aiAdviceGeneratedAt = ref('')
const boardFingerprint = ref('')
const visibleLineIds = ref([])
// 缩放仅放大画布以查看细节，不扩展真实排程时间范围。
const TIMELINE_ZOOM_MIN = 1.5
const TIMELINE_ZOOM_MAX = 3
const TIMELINE_ZOOM_DEFAULT = 1.5
// 非工作时段仍保留在时间轴上，但以更小的视觉权重显示，避免跨日计划留下大段空白。
const NON_WORKING_TIME_SCALE = 0.14
const timelineZoom = ref(TIMELINE_ZOOM_DEFAULT)
const pendingFilter = reactive({ keyword: '', productId: '', deliveryDate: '' })
const placementForm = reactive({ lineId: '', startTime: '', endTime: '', remark: '' })
const lineToAdd = ref('')
// 候选产线请求序号用于丢弃过期响应，避免快速切换订单时旧请求覆盖新订单数据。
let candidateRequestSequence = 0

// 时间轴可选择的刻度间隔，单位为小时；系统按当前宽度挑选最接近的一项。
// 将下拉选项数组转成Map，使页面按ID查名称时不必每次遍历整个数组。
const lineMap = computed(() => new Map(lineOptions.value.map((item) => [String(item.value), item.label])))
// 建立产品ID到产品名称的快速映射。
const productMap = computed(() => new Map(productOptions.value.map((item) => [String(item.value), item.label])))
// 合并选项、待排订单和时间轴数据，建立订单ID到订单详情的映射。
const orderMap = computed(() => new Map([
  ...orderOptions.value.map((item) => [String(item.value), item.row || {}]),
  ...pendingOrders.value.map((item) => [String(item.productionOrderId), item]),
  ...timelineItems.value.map((item) => [String(item.productionOrderId), item])
]))
// 根据关键词、产品和交期实时筛选待排订单。
const filteredPendingOrders = computed(() => {
  // 待排订单支持订单号/产品名称、产品ID和交期组合筛选。
  const keyword = pendingFilter.keyword.trim().toLocaleLowerCase()
  return pendingOrders.value.filter((order) => {
    const matchesKeyword = !keyword || [order.productionOrderNo, productLabel(order.productId)]
      .some((value) => String(value || '').toLocaleLowerCase().includes(keyword))
    return matchesKeyword
      && (!pendingFilter.productId || String(order.productId) === String(pendingFilter.productId))
      && (!pendingFilter.deliveryDate || order.deliveryDate === pendingFilter.deliveryDate)
  })
})
// 从时间轴中筛出仍可编辑的人工草稿项。
const manualItems = computed(() => timelineItems.value.filter((item) => item.sourceType === 'MANUAL'))
// PLAN是正式计划，MANUAL是尚可调整的人工草稿项，两者在同一时间轴用不同样式展示。
const productionPlanItems = computed(() => timelineItems.value.filter((item) => item.sourceType === 'PLAN'))
// 收集已经加入人工排产的订单ID，便于快速判断重复安排。
const manuallyScheduledOrderIds = computed(() => new Set(manualItems.value
  .map((item) => String(item.productionOrderId)).filter(Boolean)))
// 从待排订单中排除已经存在人工排产项的订单。
const unscheduledPendingOrders = computed(() => pendingOrders.value
  .filter((order) => !manuallyScheduledOrderIds.value.has(String(order.productionOrderId))))
// 按交期和订单号排序，选取最多12条待排订单作为AI建议引用。
const aiAdviceOrderReferences = computed(() => [...unscheduledPendingOrders.value]
  .sort((left, right) => String(left.deliveryDate || '9999-12-31').localeCompare(String(right.deliveryDate || '9999-12-31'))
    || String(left.productionOrderNo || '').localeCompare(String(right.productionOrderNo || '')))
  .slice(0, 12))
// 将AI回答转换为模板可循环的段落数组。
const aiAdviceParagraphs = computed(() => formatAiAdviceParagraphs(aiAdvice.value?.answer))
// 筛出后端标记为时间或产能冲突的人工排产项。
const conflictItems = computed(() => manualItems.value.filter((item) => item.status === 'CONFLICT'))
// 每类操作分别判断后端权限码，按钮显示和实际请求保持一致。
const selectedIsManual = computed(() => selectedItem.value?.sourceType === 'MANUAL')
// 以下五项分别控制新增、修改、删除、AI建议和确认计划按钮权限。
const canCreateSchedule = computed(() => hasPermission('mfg:schedule-scenario:create'))
// 判断当前账号能否调整人工排产位置。
const canUpdateSchedule = computed(() => hasPermission('mfg:schedule-scenario:update'))
// 判断当前账号能否移出人工排产项。
const canDeleteSchedule = computed(() => hasPermission('mfg:schedule-scenario:delete'))
// 判断当前账号能否请求AI排产建议。
const canRequestAdvice = computed(() => hasPermission('mfg:schedule-scenario:ai-advice'))
// 判断当前账号能否确认并生成正式生产计划。
const canConfirmSchedule = computed(() => hasPermission('mfg:schedule-scenario:confirm'))
// 根据表单选择的产线取得对应候选产线完整信息。
const placementLine = computed(() => candidates.value.find((item) => String(item.lineId) === String(placementForm.lineId)))
// 读取候选产线计算出的最短工作分钟数，供弹窗校验和提示使用。
const placementMinimumWorkingMinutes = computed(() => Math.max(0, Number(placementLine.value?.minimumWorkingMinutes || 0)))
// 比较表单和原排产项，判断是否存在尚未保存的调整。
const placementChanged = computed(() => {
  // 比较弹窗表单和原始排产项，只有发生修改时才需要先保存再确认。
  if (!selectedIsManual.value || !selectedItem.value) return false
  return String(placementForm.lineId) !== String(selectedItem.value.lineId)
    || normalizeDateTime(placementForm.startTime) !== normalizeDateTime(selectedItem.value.plannedStartTime)
    || normalizeDateTime(placementForm.endTime) !== normalizeDateTime(selectedItem.value.plannedEndTime)
    || placementForm.remark.trim() !== String(selectedItem.value.remark || '').trim()
})
// 只保留产线和起止时间完整的项目进入时间轴布局计算。
const scheduledItems = computed(() => timelineItems.value.filter((item) => item.lineId && item.plannedStartTime && item.plannedEndTime))
// 按产线分组并计算重叠任务的轨道编号，避免同一产线的时间块互相遮挡。
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
// 将后端看板起点转换为dayjs对象，方便后续时间计算。
const horizonStart = computed(() => dayjs(board.value?.horizonStart))
// 时间轴结束时间至少覆盖后端视窗，并自动延伸到最晚计划的结束时间。
const horizonEnd = computed(() => scheduledItems.value.reduce((latestEnd, item) => {
  const itemEnd = dayjs(item.plannedEndTime)
  return itemEnd.isValid() && (!latestEnd.isValid() || itemEnd.isAfter(latestEnd)) ? itemEnd : latestEnd
}, dayjs(board.value?.horizonEnd)))
// 把最终时间轴结束点向后取整到12小时边界，给最后一项留出可视空间。
const timelineEnd = computed(() => {
  if (!horizonEnd.value.isValid()) return horizonEnd.value
  const dayStart = horizonEnd.value.startOf('day')
  const elapsedMinutes = horizonEnd.value.diff(dayStart, 'minute', true)
  return dayStart.add((Math.floor(elapsedMinutes / 720) + 1) * 720, 'minute')
})
// 视图范围始终使用真实排程起点和已取整的末端，不因缩放产生额外空白日期。
const timelineViewStart = computed(() => horizonStart.value)
const timelineViewEnd = computed(() => timelineEnd.value)
// 当前时间轴展示的产线，决定哪些有效班次会占用正常显示宽度。
const timelineLineIds = computed(() => new Set([
  ...visibleLineIds.value.map(String),
  ...scheduledItems.value.map((item) => String(item.lineId))
]))
// 后端返回的窗口与产能校验共用同一份周历计算，前端不自行猜测班次。
const timelineWorkingWindows = computed(() => (board.value?.workingWindows || [])
  .filter((window) => timelineLineIds.value.size === 0 || timelineLineIds.value.has(String(window.lineId))))
// 以产线为单位保存真实有效班次，不能由订单空档或其他产线的班次推断。
const timelineWorkingWindowsByLine = computed(() => {
  const windowsByLine = new Map()
  timelineWorkingWindows.value.forEach((window) => {
    const start = dayjs(window.startTime).valueOf()
    const end = dayjs(window.endTime).valueOf()
    if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) return
    const lineId = String(window.lineId)
    const lineWindows = windowsByLine.get(lineId) || []
    lineWindows.push({ start, end })
    windowsByLine.set(lineId, lineWindows)
  })
  return windowsByLine
})
// 将真实时间切分为工作段和非工作段，再为非工作段分配压缩后的视觉长度。
const timelineDisplaySegments = computed(() => {
  const start = timelineViewStart.value
  const end = timelineViewEnd.value
  if (!start.isValid() || !end.isValid() || !end.isAfter(start)) return []
  const startValue = start.valueOf()
  const endValue = end.valueOf()
  const workingWindows = [...timelineWorkingWindowsByLine.value.values()].flat()
    .filter((window) => Number.isFinite(window.start) && Number.isFinite(window.end) && window.end > startValue && window.start < endValue)
    .map((window) => ({ start: Math.max(startValue, window.start), end: Math.min(endValue, window.end) }))
  if (!workingWindows.length) {
    return [{ start: startValue, end: endValue, working: true, displayStart: 0, displayEnd: 1 }]
  }
  const boundaries = new Set([startValue, endValue])
  workingWindows.forEach((window) => {
    boundaries.add(window.start)
    boundaries.add(window.end)
  })
  const points = [...boundaries].sort((left, right) => left - right)
  const segments = []
  for (let index = 0; index < points.length - 1; index += 1) {
    const segmentStart = points[index]
    const segmentEnd = points[index + 1]
    if (segmentEnd <= segmentStart) continue
    const working = workingWindows.some((window) => window.start <= segmentStart && window.end >= segmentEnd)
    segments.push({ start: segmentStart, end: segmentEnd, working, weight: (segmentEnd - segmentStart) * (working ? 1 : NON_WORKING_TIME_SCALE) })
  }
  const totalWeight = segments.reduce((total, segment) => total + segment.weight, 0)
  if (totalWeight <= 0) return []
  let usedWeight = 0
  return segments.map((segment) => {
    const displayStart = usedWeight / totalWeight
    usedWeight += segment.weight
    return { ...segment, displayStart, displayEnd: usedWeight / totalWeight }
  })
})
// 根据排产总天数和缩放比例计算横向画布像素宽度。
const timelineCanvasWidth = computed(() => {
  if (!timelineDisplaySegments.value.length) return 960
  const weightedHours = timelineDisplaySegments.value.reduce((total, segment) => total
    + (segment.end - segment.start) / 3600000 * (segment.working ? 1 : NON_WORKING_TIME_SCALE), 0)
  const baseWidth = Math.max(960, weightedHours * 12.5)
  return Math.round(baseWidth * Math.max(1, timelineZoom.value / TIMELINE_ZOOM_DEFAULT))
})
// 将内部缩放值转换为用户看到的百分比。
const timelineZoomPercent = computed(() => Math.round(timelineZoom.value / TIMELINE_ZOOM_DEFAULT * 100))
// 根据可视范围和画布宽度动态生成日期时间刻度。
const timelineTicks = computed(() => {
  // 根据可视时间范围和画布宽度选择合适刻度，缩放后仍保持刻度数量可读。
  const segments = timelineDisplaySegments.value
  if (!segments.length) return []
  let previousDate = ''
  return segments.map((segment) => {
    const tickPoint = dayjs(segment.start)
    const dateKey = tickPoint.format('YYYY-MM-DD')
    const tick = {
      key: tickPoint.valueOf(),
      left: `${timelineVisualPosition(tickPoint)}%`,
      date: dateKey === previousDate ? '' : tickPoint.format('MM-DD'),
      detail: tickPoint.format('HH:mm')
    }
    previousDate = dateKey
    return tick
  })
})

// 根据产品ID读取产品名称，找不到时显示带ID的兜底文字。
function productLabel(productId) {
  return productMap.value.get(String(productId)) || `产品 #${productId || '--'}`
}

// 根据产线ID读取产线名称，找不到时显示带ID的兜底文字。
function lineLabel(lineId) {
  return lineMap.value.get(String(lineId)) || `产线 #${lineId}`
}

// 从订单Map中取得某个排产项关联的完整生产订单。
function scheduledOrder(item) {
  return orderMap.value.get(String(item?.productionOrderId)) || null
}

// 优先显示排产项已有订单号，否则从订单Map补充。
function scheduledOrderLabel(item) {
  return item?.productionOrderNo || scheduledOrder(item)?.productionOrderNo || `订单 #${item?.productionOrderId || '--'}`
}

// 根据排产项或关联订单的产品ID得到产品名称。
function scheduledProductLabel(item) {
  const productId = item?.productId || scheduledOrder(item)?.productId
  return productId ? productLabel(productId) : '型号待补充'
}

// 将AI返回的连续中文文本按标题和空行切成可读段落。
function formatAiAdviceParagraphs(answer) {
  // 后端AI可能返回连续文本，这里按常见中文标题拆成段落供模板逐段显示。
  const text = String(answer || '').trim()
  if (!text) return []
  const paragraphStarts = '(?:当前排产状态[：:]|待排订单安排建议(?:如下)?[：:]|风险与优化建议[：:]|[一二三四五六七八九十]+、)'
  return text
    .replace(/\r\n?/g, '\n')
    .replace(new RegExp(`\\s*(?=${paragraphStarts})`, 'g'), '\n\n')
    .split(/\n{2,}/)
    .map((paragraph) => paragraph.trim())
    .filter(Boolean)
}

// 判断待排订单是否已经存在对应的人工排产项。
function isManuallyScheduled(order) {
  return manuallyScheduledOrderIds.value.has(String(order?.productionOrderId))
}

// 已排订单点击时直接打开它在时间轴上的安排详情。
function openPendingOrderPlacement(order) {
  const manualItem = manualItems.value.find((item) => String(item.productionOrderId) === String(order?.productionOrderId))
  if (manualItem) openPlacement(manualItem)
}

// 为PLAN和MANUAL两种来源的时间轴项生成稳定且不冲突的Vue key。
function itemKey(item) {
  return `${item.sourceType}-${item.scheduleItemId || item.planId}`
}

// 将任意合法时间统一格式化为后端需要的ISO本地日期时间字符串。
function normalizeDateTime(value) {
  return dayjs(value).isValid() ? dayjs(value).format('YYYY-MM-DDTHH:mm:ss') : ''
}

// 提取看板关键字段生成JSON指纹，用于判断AI建议是否已过期。
function createBoardFingerprint(value) {
  // 只提取影响排产结果的字段生成指纹；看板变化后标记旧AI建议已经过期。
  return JSON.stringify({
    timelineItems: (value?.timelineItems || []).map((item) => [
      item.sourceType, item.scheduleItemId || item.planId, item.scenarioVersion, item.itemVersion,
      item.lineId, item.plannedStartTime, item.plannedEndTime, item.status
    ]),
    pendingOrderIds: (value?.pendingOrders || []).map((item) => item.productionOrderId)
  })
}

// 将数量格式化为中文千分位，并限制最大小数位。
function formatNumber(value, digits = 2) {
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: digits }).format(Number(value || 0))
}

// 把后端日期时间格式化为时间轴和弹窗使用的月日时分。
function formatDateTime(value) {
  return dayjs(value).isValid() ? dayjs(value).format('MM-DD HH:mm') : '--'
}

// 将分钟数换算成小时，并按较适合阅读的精度显示。
function formatDurationHours(minutes) {
  const value = Number(minutes || 0)
  if (value <= 0) return '--'
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 1, minimumFractionDigits: value < 60 ? 1 : 0 })
    .format(Math.ceil(value / 6) / 10)
}

// 将安排的开始或结束时间按指定小时数前后微调。
function shiftPlacementTime(field, hours) {
  if (!canUpdateSchedule.value || !selectedIsManual.value || !['startTime', 'endTime'].includes(field)) return
  const current = dayjs(placementForm[field])
  if (!current.isValid()) return
  placementForm[field] = current.add(hours, 'hour').format('YYYY-MM-DDTHH:mm:ss')
}

// 在最小和最大范围内调整时间轴缩放比例。
function changeTimelineZoom(delta) {
  timelineZoom.value = Number(Math.min(TIMELINE_ZOOM_MAX,
    Math.max(TIMELINE_ZOOM_MIN, timelineZoom.value + delta)).toFixed(2))
}

// 把排产项时间转换为时间轴百分比位置和长度。
function timelineGeometry(item) {
  // 把计划开始/结束时间换算为时间轴上的百分比位置和宽度。
  const start = dayjs(item.plannedStartTime)
  const end = dayjs(item.plannedEndTime)
  if (!start.isValid() || !end.isValid() || !end.isAfter(start)) return { start: 0, width: 0, end: 0 }
  const left = timelineVisualPosition(start)
  const right = timelineVisualPosition(end)
  // 跨班计划使用起止时刻的非线性映射，条形连续越过被压缩的停工段。
  const width = Math.min(100 - left, Math.max(0, right - left))
  return { start: left, width, end: Math.min(100, left + width) }
}

function timelineVisualPosition(value) {
  const point = dayjs(value)
  const segments = timelineDisplaySegments.value
  if (!point.isValid() || !segments.length) return 0
  const pointValue = point.valueOf()
  if (pointValue <= segments[0].start) return 0
  const last = segments.at(-1)
  if (pointValue >= last.end) return 100
  const segment = segments.find((item) => pointValue <= item.end) || last
  const fraction = (pointValue - segment.start) / (segment.end - segment.start)
  return Math.max(0, Math.min(100, (segment.displayStart + fraction * (segment.displayEnd - segment.displayStart)) * 100))
}

// 根据几何信息和重叠轨道号生成时间块行内样式。
function timelineBlockStyle(item) {
  return { left: `${item.geometry.start}%`, width: `${item.geometry.width}%`, top: `${14 + item.timelineTrack * 66}px` }
}

// 清空待排订单的关键词、产品和交期筛选条件。
function resetPendingFilters() {
  pendingFilter.keyword = ''
  pendingFilter.productId = ''
  pendingFilter.deliveryDate = ''
}

// 并行加载产线、产品和生产订单关联选项。
async function loadReferenceData() {
  // 并行加载产线、产品和订单名称，用于把数据库ID显示成可读文字。
  const results = await Promise.allSettled([
    loadReferenceOptions('factory-line'),
    loadReferenceOptions('mdm-product'),
    loadReferenceOptions('mfg-production-order')
  ])
  if (results[0].status === 'fulfilled') lineOptions.value = results[0].value
  if (results[1].status === 'fulfilled') productOptions.value = results[1].value
  if (results[2].status === 'fulfilled') orderOptions.value = results[2].value
}

// 打开草稿并加载人工排产统一看板，拆分为时间轴和待排订单数据。
async function loadBoard({ open = false } = {}) {
  // 有创建权限时先打开可编辑草稿，再获取统一看板并拆分到各个响应式状态。
  loading.value = true
  try {
    const seed = canCreateSchedule.value && (open || !draft.value) ? await openManualScheduleDraft() : null
    const result = await getManualScheduleBoard()
    const nextBoard = result?.data || null
    const nextFingerprint = createBoardFingerprint(nextBoard)
    if (aiAdvice.value && boardFingerprint.value && nextFingerprint !== boardFingerprint.value) {
      aiAdviceStale.value = true
    }
    boardFingerprint.value = nextFingerprint
    board.value = nextBoard
    draft.value = board.value?.workspaceDraft || seed?.data || draft.value
    timelineItems.value = board.value?.timelineItems || []
    pendingOrders.value = board.value?.pendingOrders || []
    const itemLineIds = timelineItems.value.filter((item) => item.lineId).map((item) => String(item.lineId))
    visibleLineIds.value = [...new Set([...visibleLineIds.value.map(String), ...itemLineIds])]
  } finally {
    loading.value = false
  }
}

// 并行刷新排产看板与关联名称数据。
async function refreshWorkbench() {
  // 看板数据和关联名称没有依赖关系，因此并行刷新。
  await Promise.all([loadBoard({ open: true }), loadReferenceData()])
}

// 查询所选订单满足能力和产能约束的候选产线并打开候选弹窗。
async function openCandidates(order) {
  // 选择待排订单后向后端查询满足产品能力、日历和产能要求的候选产线。
  if (!canCreateSchedule.value || !draft.value) return
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

// 把选中的待排订单加入指定产线草稿，并重新加载后端看板。
async function addToLine(candidate) {
  // 后端创建草稿项后重新加载看板，确保版本号、冲突状态和计算时间以服务端为准。
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
    ElMessage.success('订单已加入排产总览')
    await loadBoard()
  } catch (error) {
    await showSchedulingError(error, '添加订单失败', '订单未能加入排程，请刷新后重试。')
  } finally {
    addingLineId.value = null
  }
}

// 将当前产线下拉项转换为候选产线结构，保证编辑弹窗至少能显示原产线。
function currentLineCandidate(item) {
  const option = lineOptions.value.find((line) => String(line.value) === String(item?.lineId))
  const row = option?.row || {}
  return {
    lineId: item?.lineId,
    lineCode: row.lineCode || option?.label || lineLabel(item?.lineId),
    lineName: row.lineName || option?.label || lineLabel(item?.lineId),
    workshopId: row.workshopId,
    earliestStartTime: item?.plannedStartTime,
    earliestEndTime: item?.plannedEndTime
  }
}

// 打开排产详情；人工项额外查询候选产线以支持编辑。
async function openPlacement(item) {
  // 正式PLAN只允许查看；MANUAL项还会加载候选产线并允许调整位置。
  selectedItem.value = item
  placementForm.lineId = String(item.lineId || '')
  placementForm.startTime = item.plannedStartTime || ''
  placementForm.endTime = item.plannedEndTime || ''
  placementForm.remark = item.remark || ''
  candidates.value = item.lineId ? [currentLineCandidate(item)] : []
  placementOpen.value = true
  if (item.sourceType !== 'MANUAL' || !canUpdateSchedule.value) return
  candidateLoading.value = true
  try {
    const result = await getManualEligibleLines(item.scenarioId, item.productionOrderId)
    const loaded = result?.data || []
    const currentLine = currentLineCandidate(item)
    candidates.value = loaded.some((line) => String(line.lineId) === String(item.lineId))
      ? loaded
      : [currentLine, ...loaded]
  } catch (error) {
    candidateError.value = schedulingErrorMessage(error, '候选产线加载失败，请重试。')
    await showSchedulingError(error, '候选产线加载失败', '候选产线加载失败，请刷新后重试。')
  } finally {
    candidateLoading.value = false
  }
}

// 提交产线、时间和版本号保存人工安排，可选择保存后是否关闭弹窗。
async function persistPlacement(closeAfterSave) {
  // 同时提交方案版本和明细版本，后端通过乐观锁阻止多人排产互相覆盖。
  if (!canUpdateSchedule.value || !selectedIsManual.value || !selectedItem.value || !placementForm.lineId
    || !placementForm.startTime || !placementForm.endTime) return false
  savingPlacement.value = true
  const scheduleItemId = selectedItem.value.scheduleItemId
  try {
    await updateManualSchedulePlacement(selectedItem.value.scenarioId, scheduleItemId, {
      scenarioVersion: selectedItem.value.scenarioVersion,
      itemVersion: selectedItem.value.itemVersion,
      lineId: Number(placementForm.lineId),
      plannedStartTime: placementForm.startTime,
      plannedEndTime: placementForm.endTime,
      remark: placementForm.remark.trim() || undefined
    })
    visibleLineIds.value = [...new Set([...visibleLineIds.value.map(String), String(placementForm.lineId)])]
    await loadBoard()
    selectedItem.value = timelineItems.value.find((item) => item.sourceType === 'MANUAL' && item.scheduleItemId === scheduleItemId) || null
    if (closeAfterSave) placementOpen.value = false
    ElMessage.success('时间安排已更新')
    return true
  } catch (error) {
    await showSchedulingError(error, '保存临时安排失败', '请检查产线、班次和计划时间后重试。')
    return false
  } finally {
    savingPlacement.value = false
  }
}

// 判断后端错误是否属于多人编辑导致的排程版本冲突。
function isSchedulingConflict(error) {
  // 业务码1007或版本相关文案都表示当前看板已被其他操作更新。
  const message = String(error?.displayMessage || error?.message || '')
  return Number(error?.code) === 1007 || /排程已更新|其他用户修改|版本/.test(message)
}

// 将排产业务错误整理为用户可以直接理解的中文处理提示。
function schedulingErrorMessage(error, fallback) {
  const message = String(error?.displayMessage || error?.message || '').trim()
  if (isSchedulingConflict(error)) return '排程已更新，页面已刷新，请重新选择要操作的安排。'
  if (/DRAFT|草案|另一份可编辑/.test(message)) return '该订单当前不能继续安排，请刷新后查看它是否已生成生产计划或进入其他生产状态。'
  return message || fallback
}

// 版本冲突时先刷新看板，再用阻塞对话框展示明确错误。
async function showSchedulingError(error, title, fallback) {
  // 版本冲突时先关闭旧弹窗并刷新看板，再告诉用户重新选择，避免继续编辑过期数据。
  const conflict = isSchedulingConflict(error)
  if (conflict) {
    candidateOpen.value = false
    placementOpen.value = false
    selectedItem.value = null
    try {
      await refreshWorkbench()
    } catch {
      // 即使后续刷新失败，下面仍会继续展示最初的排产业务错误。
    }
  }
  try {
    await ElMessageBox.alert(schedulingErrorMessage(error, fallback), title, {
      type: 'error',
      confirmButtonText: '关闭',
      showClose: true,
      closeOnClickModal: false,
      closeOnPressEscape: true
    })
  } catch {
    // 关闭错误弹窗不会丢弃未保存表单；只有版本冲突触发看板刷新时才会重置。
  }
}

// 后端已确认但新计划暂未出现在看板时，在本地立即替换为正式PLAN项。
function ensureConfirmedPlanVisible(plan, manualItem) {
  // 确认接口成功但看板刷新暂未包含新计划时，在本地立即把MANUAL项替换为PLAN项。
  if (!plan?.planId || !manualItem) return
  const hasFormalPlan = timelineItems.value.some((item) => item.sourceType === 'PLAN'
    && String(item.planId) === String(plan.planId))
  if (hasFormalPlan) return
  const formalItem = {
    ...manualItem,
    sourceType: 'PLAN',
    planId: plan.planId,
    planNo: plan.planNo,
    status: plan.status || 'RELEASED',
    remark: plan.remark || manualItem.remark,
    editable: false
  }
  timelineItems.value = [...timelineItems.value
    .filter((item) => !(item.sourceType === 'MANUAL'
      && String(item.scheduleItemId) === String(manualItem.scheduleItemId))), formalItem]
    .sort((left, right) => dayjs(left.plannedStartTime).valueOf() - dayjs(right.plannedStartTime).valueOf())
}

// 校验产线和起止时间后保存临时安排并关闭弹窗。
async function savePlacement() {
  if (!placementForm.lineId || !placementForm.startTime || !placementForm.endTime) {
    await showSchedulingError(null, '无法保存临时安排', '请先完整选择生产产线、开始时间和结束时间。')
    return
  }
  await persistPlacement(true)
}

// 二次确认后移除人工排产项，使订单重新回到待排列表。
async function removeItem() {
  // 移出草稿项前二次确认，成功后订单会重新回到待排列表。
  if (!canDeleteSchedule.value || !selectedIsManual.value || !selectedItem.value || removingItem.value) return
  try {
    await ElMessageBox.confirm('移出后订单会回到待排列表。', '移出订单', {
      type: 'warning', confirmButtonText: '移出', cancelButtonText: '取消'
    })
  } catch {
    return
  }
  removingItem.value = true
  try {
    await removeManualScheduleItem(selectedItem.value.scenarioId, selectedItem.value.scheduleItemId, {
      scenarioVersion: selectedItem.value.scenarioVersion,
      itemVersion: selectedItem.value.itemVersion
    })
    placementOpen.value = false
    selectedItem.value = null
    ElMessage.success('订单已移出时间安排')
    await loadBoard()
  } catch (error) {
    await showSchedulingError(error, '移出安排失败', '订单未能移出当前安排，请刷新后重试。')
  } finally {
    removingItem.value = false
  }
}

// 必要时先保存改动，再确认人工项并生成正式生产计划。
async function confirmPlan() {
  // 若表单有未保存修改则先保存，再调用确认接口生成正式生产计划。
  if (!canConfirmSchedule.value || !selectedIsManual.value || !selectedItem.value || confirmingItem.value) return
  try {
    await ElMessageBox.confirm('确认后将保存当前安排，并生成正式生产计划。', '生成生产计划', {
      type: 'warning', confirmButtonText: '确认并生成', cancelButtonText: '取消'
    })
  } catch {
    return
  }
  confirmingItem.value = true
  try {
    if (placementChanged.value) {
      const saved = await persistPlacement(false)
      if (!saved || !selectedItem.value) return
    }
    const confirmedManualItem = { ...selectedItem.value }
    const result = await confirmManualScheduleItem(selectedItem.value.scenarioId, selectedItem.value.scheduleItemId, {
      scenarioVersion: selectedItem.value.scenarioVersion,
      itemVersion: selectedItem.value.itemVersion,
      remark: placementForm.remark.trim() || undefined
    })
    placementOpen.value = false
    selectedItem.value = null
    ElMessage.success('生产计划已生成')
    await loadBoard()
    ensureConfirmedPlanVisible(result?.data, confirmedManualItem)
  } catch (error) {
    await showSchedulingError(error, '生成生产计划失败', '当前安排未能生成生产计划，请刷新后重试。')
  } finally {
    confirmingItem.value = false
  }
}

// 请求AI分析当前整张排产看板，并记录生成时间和有效状态。
async function runAiAdvice() {
  // AI建议针对当前整张看板；后续看板变化时通过指纹将建议标记为已过期。
  if (!canRequestAdvice.value || aiAdviceLoading.value) return
  aiAdviceLoading.value = true
  try {
    const result = await getManualBoardAiAdvice()
    aiAdvice.value = result?.data || null
    aiAdviceGeneratedAt.value = dayjs().format('YYYY-MM-DD HH:mm')
    aiAdviceStale.value = false
  } finally {
    aiAdviceLoading.value = false
  }
}

// 将用户选择的空产线加入时间轴可见产线集合。
function addVisibleLine() {
  if (!lineToAdd.value) return
  visibleLineIds.value = [...new Set([...visibleLineIds.value.map(String), String(lineToAdd.value)])]
  lineToAdd.value = ''
  addLineOpen.value = false
}

// 工作台挂载后执行第一次完整刷新。
onMounted(refreshWorkbench)
</script>

<template>
  <!-- 左侧管理待排订单，中间是分产线时间轴，弹窗负责候选产线和具体时间编辑。 -->
  <div class="mes-page manual-scheduling-workbench">
    <header class="mes-page-header workbench-header">
      <div class="workbench-heading">
        <span class="workbench-heading-icon"><AppIcon name="route" :size="23" /></span>
        <div><h1 class="mes-page-title">智能排产工作台</h1><p class="mes-page-subtitle">待排订单、手工安排与生产计划总览</p></div>
      </div>
      <div class="workbench-actions">
        <el-tag type="primary" effect="plain">整体计划板</el-tag>
        <el-tooltip content="刷新工作台" placement="bottom"><el-button circle :loading="loading" aria-label="刷新工作台" @click="refreshWorkbench"><AppIcon name="refresh" :size="16" /></el-button></el-tooltip>
      </div>
    </header>

    <section class="top-grid">
      <section class="mes-panel pending-panel">
        <div class="mes-panel-header"><div><h2 class="mes-panel-title">待排生产订单</h2><span>含已排但尚未确认生产计划的订单</span></div><small>{{ filteredPendingOrders.length }} 条</small></div>
        <div class="filter-row">
          <el-input v-model="pendingFilter.keyword" clearable placeholder="订单号或型号" />
          <el-select v-model="pendingFilter.productId" clearable placeholder="产品"><el-option v-for="item in productOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
          <el-date-picker v-model="pendingFilter.deliveryDate" type="date" value-format="YYYY-MM-DD" placeholder="交期" />
          <el-tooltip content="重置筛选" placement="top"><el-button circle aria-label="重置筛选" @click="resetPendingFilters"><AppIcon name="refresh" :size="15" /></el-button></el-tooltip>
        </div>
        <el-table v-live-column-resize v-loading="loading" :data="filteredPendingOrders" size="small" height="335" class="compact-table">
          <el-table-column label="序号" width="58" align="center" fixed="left"><template #default="{ $index }">{{ $index + 1 }}</template></el-table-column>
          <el-table-column label="订单 / 型号" min-width="180"><template #default="{ row }"><strong>{{ row.productionOrderNo }}</strong><small>{{ productLabel(row.productId) }}</small></template></el-table-column>
          <el-table-column label="数量" width="92" align="right"><template #default="{ row }">{{ formatNumber(row.orderQuantity, 4) }}</template></el-table-column>
          <el-table-column label="交期" width="104"><template #default="{ row }">{{ row.deliveryDate || '--' }}</template></el-table-column>
          <el-table-column label="状态" width="76"><template #default="{ row }"><el-tag size="small" :effect="isManuallyScheduled(row) ? 'light' : 'plain'" :type="isManuallyScheduled(row) ? 'primary' : 'info'">{{ isManuallyScheduled(row) ? '已排' : '待排' }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="70" fixed="right"><template #default="{ row }"><el-tooltip v-if="isManuallyScheduled(row)" content="查看已排安排" placement="top"><el-button link type="primary" aria-label="查看已排安排" @click="openPendingOrderPlacement(row)"><AppIcon name="eye" :size="16" /></el-button></el-tooltip><el-tooltip v-else-if="canCreateSchedule" content="添加到产线" placement="top"><el-button link type="primary" aria-label="添加到产线" :disabled="!draft" @click="openCandidates(row)"><AppIcon name="plus" :size="16" /></el-button></el-tooltip></template></el-table-column>
          <template #empty><el-empty description="暂无待确认生产计划的订单" :image-size="54" /></template>
        </el-table>
      </section>

      <section class="mes-panel overview-panel">
        <div class="mes-panel-header"><div><h2 class="mes-panel-title">排产状态</h2><span>当前全局负荷</span></div><AppIcon name="activity" :size="18" /></div>
        <div class="overview-grid">
          <div class="tone-pending"><span class="overview-icon"><AppIcon name="clipboard" :size="22" /></span><span class="overview-copy"><small>待排订单</small><strong>{{ unscheduledPendingOrders.length }}<em>单</em></strong></span></div>
          <div class="tone-formal"><span class="overview-icon"><AppIcon name="factory" :size="22" /></span><span class="overview-copy"><small>生产计划</small><strong>{{ productionPlanItems.length }}<em>项</em></strong></span></div>
          <div class="tone-manual"><span class="overview-icon"><AppIcon name="edit" :size="22" /></span><span class="overview-copy"><small>手工安排</small><strong>{{ manualItems.length }}<em>项</em></strong></span></div>
          <div class="tone-risk"><span class="overview-icon"><AppIcon name="alert" :size="22" /></span><span class="overview-copy"><small>冲突 / 延期</small><strong>{{ board?.conflictCount || 0 }} / {{ board?.lateCount || 0 }}</strong></span></div>
        </div>
        <div class="overview-window">
          <div class="tone-start"><span class="overview-icon"><AppIcon name="calendar" :size="21" /></span><span class="overview-copy"><small>当前计划最早开始</small><strong>{{ formatDateTime(board?.horizonStart) }}</strong></span></div>
          <div class="tone-end"><span class="overview-icon"><AppIcon name="calendar" :size="21" /></span><span class="overview-copy"><small>当前计划最晚结束</small><strong>{{ formatDateTime(board?.horizonEnd) }}</strong></span></div>
        </div>
        <div class="overview-lines tone-lines"><span class="overview-icon"><AppIcon name="route" :size="22" /></span><span class="overview-copy"><small>已排产线</small><strong>{{ lineGroups.length }}<em>条</em></strong></span></div>
      </section>
    </section>

    <section class="mes-panel timeline-panel">
      <div class="mes-panel-header"><div><h2 class="mes-panel-title">生产排程总览</h2><span>{{ formatDateTime(horizonStart) }} 至 {{ formatDateTime(horizonEnd) }}</span></div><div class="timeline-actions"><el-button @click="addLineOpen = true"><AppIcon name="plus" :size="15" />添加产线</el-button></div></div>
      <div class="timeline-toolbar"><div class="timeline-legend"><span><i class="manual" />手工安排</span><span><i class="formal" />生产计划</span><span><i class="risk" />风险安排</span></div><div class="timeline-zoom-controls" aria-label="时间轴缩放"><el-tooltip content="缩小时间轴" placement="top"><el-button circle size="small" aria-label="缩小时间轴" :disabled="timelineZoom <= TIMELINE_ZOOM_MIN" @click="changeTimelineZoom(-.1)"><AppIcon name="zoom-out" :size="14" /></el-button></el-tooltip><el-slider v-model="timelineZoom" :min="TIMELINE_ZOOM_MIN" :max="TIMELINE_ZOOM_MAX" :step=".05" :show-tooltip="false" /><span class="zoom-value">{{ timelineZoomPercent }}%</span><el-tooltip content="放大时间轴" placement="top"><el-button circle size="small" aria-label="放大时间轴" :disabled="timelineZoom >= TIMELINE_ZOOM_MAX" @click="changeTimelineZoom(.1)"><AppIcon name="zoom-in" :size="14" /></el-button></el-tooltip></div></div>
      <div v-if="lineGroups.length" class="timeline-board">
        <div class="timeline-inner" :style="{ '--timeline-width': `${timelineCanvasWidth}px` }">
          <div class="timeline-head">
            <div class="timeline-corner">产线</div>
            <div class="timeline-axis"><span v-for="tick in timelineTicks" :key="tick.key" class="axis-tick" :class="{ 'has-date': tick.date }" :style="{ left: tick.left }"><b v-if="tick.date">{{ tick.date }}</b><small>{{ tick.detail }}</small></span></div>
          </div>
          <div v-for="group in lineGroups" :key="group.lineId" class="timeline-row">
            <div class="line-label"><strong>{{ group.label }}</strong><small>{{ group.items.length }} 个计划</small></div>
            <div class="timeline-lane" :style="{ '--tracks': group.trackCount }">
              <i v-for="tick in timelineTicks" :key="tick.key" :style="{ left: tick.left }" />
              <el-tooltip v-for="item in group.items" :key="itemKey(item)" placement="top" :show-after="180" popper-class="timeline-plan-tooltip">
                <template #content>
                  <strong>{{ scheduledOrderLabel(item) }}</strong>
                  <span>开始：{{ formatDateTime(item.plannedStartTime) }}</span>
                  <span>结束：{{ formatDateTime(item.plannedEndTime) }}</span>
                </template>
                <button type="button" class="timeline-block" :class="{ formal: item.sourceType === 'PLAN', conflict: item.status === 'CONFLICT', late: Number(item.tardinessMinutes || 0) > 0 }" :style="timelineBlockStyle(item)" @click="openPlacement(item)"><strong>{{ scheduledOrderLabel(item) }}</strong><span>{{ scheduledProductLabel(item) }} · {{ formatDateTime(item.plannedStartTime) }}</span></button>
              </el-tooltip>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无排产安排" :image-size="62" />
      <div v-if="conflictItems.length" class="conflict-strip"><AppIcon name="alert" :size="16" /><div><strong>当前风险安排</strong><button v-for="item in conflictItems" :key="itemKey(item)" type="button" @click="openPlacement(item)">{{ scheduledOrderLabel(item) }}：{{ item.conflictReason || '存在排程冲突' }}</button></div></div>
    </section>

    <section class="mes-panel ai-advice-panel">
      <div class="mes-panel-header">
        <div><h2 class="mes-panel-title">AI 建议</h2><span>{{ aiAdviceGeneratedAt ? `最近生成 ${aiAdviceGeneratedAt}` : '尚未生成建议' }}</span></div>
        <div class="ai-advice-actions"><el-tag v-if="aiAdviceStale" type="warning" effect="plain">排程已调整</el-tag><el-button v-if="canRequestAdvice" type="primary" :loading="aiAdviceLoading" @click="runAiAdvice"><AppIcon name="bot" :size="15" />生成 AI 建议</el-button></div>
      </div>
      <div class="ai-advice-body">
        <div v-if="aiAdviceLoading" class="ai-advice-loading"><el-skeleton :rows="5" animated /></div>
        <div v-else-if="aiAdvice" class="ai-advice-result">
          <div v-if="aiAdviceStale" class="ai-advice-stale"><AppIcon name="alert" :size="15" /><span>排程内容已经调整，以下建议来自上一次分析，请重新生成 AI 建议后再作为当前排产依据。</span></div>
          <div class="ai-advice-answer"><p v-for="(paragraph, index) in aiAdviceParagraphs" :key="`${index}-${paragraph}`">{{ paragraph }}</p></div>
          <div v-if="aiAdviceOrderReferences.length" class="ai-advice-references"><span class="ai-advice-references-title">待排订单建议编号对照</span><div v-for="(order, index) in aiAdviceOrderReferences" :key="order.productionOrderId" class="ai-advice-reference"><strong>P{{ index + 1 }}</strong><span>{{ order.productionOrderNo }}</span><small>{{ productLabel(order.productId) }} · {{ formatNumber(order.orderQuantity, 4) }} · 交期 {{ order.deliveryDate || '--' }}</small></div></div>
          <ul v-if="aiAdvice.suggestions?.length"><li v-for="item in aiAdvice.suggestions" :key="item">{{ item }}</li></ul>
          <small v-if="aiAdvice.disclaimer">{{ aiAdvice.disclaimer }}</small>
        </div>
        <el-empty v-else description="暂无 AI 建议" :image-size="54" />
      </div>
    </section>

    <el-dialog v-model="candidateOpen" title="选择生产产线" width="min(720px, calc(100vw - 32px))" destroy-on-close><p class="dialog-tip">{{ selectedOrder?.productionOrderNo }} · {{ productLabel(selectedOrder?.productId) }}</p><el-table v-live-column-resize v-loading="candidateLoading" element-loading-text="正在匹配可生产产线..." :data="candidates" size="small"><el-table-column label="产线" min-width="170"><template #default="{ row }"><strong>{{ row.lineCode || row.lineName }}</strong><small>{{ row.lineName }}</small></template></el-table-column><el-table-column label="车间" width="94"><template #default="{ row }">#{{ row.workshopId }}</template></el-table-column><el-table-column label="最早可用" min-width="142"><template #default="{ row }">{{ formatDateTime(row.earliestStartTime) }}</template></el-table-column><el-table-column label="预计结束" min-width="142"><template #default="{ row }">{{ formatDateTime(row.earliestEndTime) }}</template></el-table-column><el-table-column label="操作" width="82"><template #default="{ row }"><el-button v-if="canCreateSchedule" link type="primary" :loading="String(addingLineId) === String(row.lineId)" :disabled="addingLineId !== null" @click="addToLine(row)">添加</el-button></template></el-table-column><template #empty><el-empty :description="candidateError || '没有符合能力和周历条件的产线'" :image-size="50" /></template></el-table></el-dialog>

    <el-drawer v-model="placementOpen" :title="selectedIsManual ? '编辑计划安排' : '生产计划详情'" size="min(460px, 100vw)" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="订单"><el-input :model-value="selectedItem ? `${scheduledOrderLabel(selectedItem)} · ${scheduledProductLabel(selectedItem)}` : ''" disabled /></el-form-item>
        <el-form-item v-if="!selectedIsManual" label="计划编号"><el-input :model-value="selectedItem?.planNo || '--'" disabled /></el-form-item>
        <el-form-item label="生产产线"><el-select v-model="placementForm.lineId" class="full-width" :disabled="!selectedIsManual || !canUpdateSchedule" :loading="candidateLoading" loading-text="正在加载候选产线"><el-option v-for="line in candidates" :key="line.lineId" :label="`${line.lineCode || line.lineName} · 车间 #${line.workshopId || '--'}`" :value="String(line.lineId)" /></el-select></el-form-item>
        <el-form-item label="所属车间"><el-input :model-value="placementLine?.workshopId ? `车间 #${placementLine.workshopId}` : '--'" disabled /></el-form-item>

        <div v-if="selectedIsManual" class="capacity-hint">
          <AppIcon name="activity" :size="17" />
          <div v-if="candidateLoading"><strong>正在核算最短生产用时</strong><span>根据订单数量和所选产线当前产能计算</span></div>
          <div v-else-if="placementMinimumWorkingMinutes"><strong>建议至少预留 {{ formatDurationHours(placementMinimumWorkingMinutes) }} 小时产线工作时间</strong><span>订单数量 {{ formatNumber(selectedItem?.plannedQuantity, 4) }}，按 {{ placementLine?.lineCode || placementLine?.lineName || lineLabel(placementForm.lineId) }} 当前产能与班次核算；允许跨越下班和停工时段，但开始、结束时刻必须落在有效班次内。</span></div>
          <div v-else><strong>将按产线班次核算可用工作时间</strong><span>允许跨越下班和停工时段，跨越部分不计入工作时长；开始、结束时刻仍需落在有效班次内。</span></div>
        </div>

        <el-form-item label="生产时间">
          <div class="placement-time-stack">
            <div class="placement-time-row">
              <span>开始时间</span>
              <el-date-picker v-model="placementForm.startTime" class="placement-time-input" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY-MM-DD HH:mm" placeholder="选择开始时间" :disabled="!selectedIsManual || !canUpdateSchedule" />
              <div v-if="selectedIsManual && canUpdateSchedule" class="time-stepper">
                <el-tooltip content="增加 1 小时" placement="right"><button type="button" aria-label="开始时间增加1小时" @click="shiftPlacementTime('startTime', 1)"><AppIcon class="step-up-icon" name="chevron" :size="13" /></button></el-tooltip>
                <el-tooltip content="减少 1 小时" placement="right"><button type="button" aria-label="开始时间减少1小时" @click="shiftPlacementTime('startTime', -1)"><AppIcon class="step-down-icon" name="chevron" :size="13" /></button></el-tooltip>
              </div>
            </div>
            <div class="placement-time-row">
              <span>结束时间</span>
              <el-date-picker v-model="placementForm.endTime" class="placement-time-input" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" format="YYYY-MM-DD HH:mm" placeholder="选择结束时间" :disabled="!selectedIsManual || !canUpdateSchedule" />
              <div v-if="selectedIsManual && canUpdateSchedule" class="time-stepper">
                <el-tooltip content="增加 1 小时" placement="right"><button type="button" aria-label="结束时间增加1小时" @click="shiftPlacementTime('endTime', 1)"><AppIcon class="step-up-icon" name="chevron" :size="13" /></button></el-tooltip>
                <el-tooltip content="减少 1 小时" placement="right"><button type="button" aria-label="结束时间减少1小时" @click="shiftPlacementTime('endTime', -1)"><AppIcon class="step-down-icon" name="chevron" :size="13" /></button></el-tooltip>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="placementForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit :disabled="!selectedIsManual || !canUpdateSchedule" /></el-form-item>
      </el-form>
      <template #footer>
        <div v-if="selectedIsManual" class="drawer-actions placement-drawer-actions">
          <el-button v-if="canDeleteSchedule" class="placement-remove-button" type="danger" plain aria-label="移出安排" :loading="removingItem" :disabled="savingPlacement || confirmingItem" @click="removeItem"><AppIcon name="trash" :size="15" /><span class="placement-remove-label">移出安排</span></el-button>
          <span class="placement-action-spacer" />
          <el-button class="placement-cancel-button" :disabled="removingItem || savingPlacement || confirmingItem" @click="placementOpen = false">取消</el-button>
          <el-button v-if="canUpdateSchedule" class="placement-save-button" type="primary" :loading="savingPlacement" :disabled="removingItem || confirmingItem" @click="savePlacement">保存临时安排</el-button>
          <el-button v-if="canConfirmSchedule" class="placement-confirm-button" type="success" :loading="confirmingItem" :disabled="removingItem || savingPlacement || selectedItem?.status === 'CONFLICT'" @click="confirmPlan"><AppIcon name="check" :size="15" />确认并生成计划</el-button>
        </div>
        <div v-else class="drawer-actions plan-detail-actions"><el-button @click="placementOpen = false">关闭</el-button></div>
      </template>
    </el-drawer>

    <el-dialog v-model="addLineOpen" title="添加时间轴产线" width="min(420px, calc(100vw - 32px))" destroy-on-close><el-select v-model="lineToAdd" class="full-width" filterable placeholder="选择现有产线"><el-option v-for="line in lineOptions" :key="line.value" :label="line.label" :value="String(line.value)" /></el-select><template #footer><el-button @click="addLineOpen = false">取消</el-button><el-button type="primary" @click="addVisibleLine">添加</el-button></template></el-dialog>

  </div>
</template>

<style scoped>
/* 排产工作台样式：控制待排列表、产线时间轴、计划块、候选产线和编辑弹窗。 */
.capacity-hint{display:flex;align-items:flex-start;gap:9px;margin:0 0 18px;padding:11px 12px;color:var(--mes-primary);border:1px solid var(--mes-primary-border);border-radius:6px;background:var(--mes-primary-soft)}
.capacity-hint>div{display:flex;min-width:0;flex:1;flex-direction:column;gap:3px}
.capacity-hint strong{font-size:12px;line-height:1.45}
.capacity-hint span{color:var(--mes-text-secondary);font-size:11px;line-height:1.55}
.placement-time-stack{display:flex;width:100%;flex-direction:column;gap:8px}
.placement-time-row{display:grid;grid-template-columns:58px minmax(0,1fr) 30px;align-items:stretch;gap:8px;min-height:34px}
.placement-time-row>span{display:flex;align-items:center;color:var(--mes-text-secondary);font-size:11px}
.placement-time-input{width:100%!important}
.time-stepper{display:grid;width:30px;overflow:hidden;grid-template-rows:1fr 1fr;border:1px solid var(--mes-border);border-radius:4px;background:var(--mes-surface)}
.time-stepper button{display:flex;min-height:16px;align-items:center;justify-content:center;padding:0;color:var(--mes-text-secondary);border:0;background:transparent;cursor:pointer}
.time-stepper button+button{border-top:1px solid var(--mes-border)}
.time-stepper button:hover{color:var(--mes-primary);background:var(--mes-primary-soft)}
.step-up-icon{transform:rotate(-90deg)}
.step-down-icon{transform:rotate(90deg)}
.drawer-actions.placement-drawer-actions{display:flex;flex-wrap:nowrap;gap:6px}
.placement-drawer-actions :deep(.el-button){min-width:0;flex:none;margin:0;padding:0 8px;font-size:12px}
.placement-remove-button{width:94px}
.placement-action-spacer{min-width:0;flex:1}
.placement-cancel-button{width:54px}
.placement-save-button{width:108px}
.placement-confirm-button{width:132px}
.drawer-actions.plan-detail-actions{justify-content:flex-end}
@media(max-width:480px){.placement-remove-button{width:34px;padding:0!important}.placement-remove-label{display:none}.placement-cancel-button{width:50px}.placement-save-button{width:98px}.placement-confirm-button{width:124px}}
.workbench-header,.workbench-heading,.workbench-actions,.mes-panel-header>div,.timeline-actions,.timeline-toolbar,.timeline-toolbar>div,.drawer-actions,.feasibility-actions{display:flex;align-items:center}.workbench-header{justify-content:space-between;gap:16px}.workbench-heading{gap:12px}.workbench-heading-icon{display:inline-flex;width:44px;height:44px;align-items:center;justify-content:center;color:var(--mes-primary);border:1px solid var(--mes-primary-border);border-radius:6px;background:var(--mes-primary-soft)}.workbench-actions,.timeline-actions,.feasibility-actions{gap:8px}.workbench-summary{display:grid;grid-template-columns:repeat(4,minmax(0,1fr)) minmax(250px,1.6fr);margin-bottom:14px;border:1px solid var(--mes-border);border-left:3px solid var(--mes-primary);background:var(--mes-surface)}.workbench-summary>div{display:flex;min-width:0;flex-direction:column;justify-content:center;gap:3px;padding:12px 16px;border-right:1px solid var(--mes-border)}.workbench-summary>div:last-child{border:0}.workbench-summary span,.workbench-summary small{color:var(--mes-text-muted);font-size:10px}.workbench-summary strong{overflow:hidden;font-size:17px;text-overflow:ellipsis;white-space:nowrap}.workbench-summary .danger{color:var(--mes-danger)}.top-grid{display:grid;grid-template-columns:minmax(0,1.45fr) minmax(320px,.75fr);grid-auto-rows:440px;gap:14px;margin-bottom:14px}.mes-panel{overflow:hidden;border-radius:var(--mes-radius)}.mes-panel-header{display:flex;min-height:52px;align-items:center;justify-content:space-between;gap:12px;padding:9px 14px;border-bottom:1px solid var(--mes-border)}.mes-panel-header>div{min-width:0;gap:8px}.mes-panel-header span,.mes-panel-header small{overflow:hidden;color:var(--mes-text-muted);font-size:10px;text-overflow:ellipsis;white-space:nowrap}.filter-row{display:flex;gap:7px;padding:8px 12px;border-bottom:1px solid var(--mes-border);background:var(--mes-surface-muted)}.filter-row :deep(.el-input){min-width:0;flex:1}.filter-row :deep(.el-select){width:145px}.filter-row :deep(.el-date-editor){width:118px}.pending-panel,.overview-panel{height:100%;min-height:0}.compact-table{width:100%}.compact-table :deep(.el-table__cell){padding:7px 0}.compact-table :deep(.el-table__header-wrapper th.el-table__cell){height:40px}.compact-table :deep(.el-table__body .el-table__row){height:59px}.compact-table strong,.compact-table small{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.compact-table small{margin-top:2px;color:var(--mes-text-muted);font-size:10px}.overview-panel{display:flex;flex-direction:column}.overview-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1px;background:var(--mes-border)}.overview-grid>div{display:grid;grid-template-columns:12px 1fr auto;align-items:center;gap:8px;min-height:88px;padding:14px;background:var(--mes-surface)}.overview-grid small{color:var(--mes-text-muted);font-size:11px}.overview-grid strong{font-size:20px}.overview-mark{width:9px;height:36px;border-radius:3px;background:var(--mes-primary)}.overview-mark.formal{background:#1b8f5a}.overview-mark.pending{background:#7d8793}.overview-mark.risk{background:var(--mes-danger)}.overview-lines{display:flex;flex:1;align-items:center;justify-content:space-between;padding:18px 16px;border-top:1px solid var(--mes-border);color:var(--mes-text-secondary)}.overview-lines strong{font-size:24px}.timeline-actions :deep(.el-button)>span,.drawer-actions :deep(.el-button)>span,.feasibility-actions :deep(.el-button)>span{display:inline-flex;align-items:center;gap:5px}.timeline-panel{min-height:0}.timeline-toolbar{justify-content:space-between;gap:12px;padding:8px 14px;border-bottom:1px solid var(--mes-border);background:var(--mes-surface-muted)}.timeline-toolbar>div{gap:5px}.timeline-toolbar :deep(.el-slider){width:108px;margin:0 3px}.timeline-legend{display:flex;align-items:center;gap:14px}.timeline-legend span{display:flex;align-items:center;gap:5px;color:var(--mes-text-secondary);font-size:10px}.timeline-legend i{width:14px;height:6px;border-radius:2px;background:var(--mes-primary)}.timeline-legend i.formal{background:#1b8f5a}.timeline-legend i.risk{background:var(--mes-danger)}.zoom-value{width:36px;flex:none;color:var(--mes-text-muted);font-size:10px;text-align:center}.timeline-board{overflow:auto}.timeline-inner{width:max-content;min-width:100%}.timeline-head,.timeline-row{display:grid;grid-template-columns:172px var(--timeline-width,960px)}.timeline-head{position:sticky;top:0;z-index:3;border-bottom:1px solid var(--mes-border);background:var(--mes-surface-muted)}.timeline-corner,.line-label{position:sticky;left:0;z-index:2;display:flex;flex-direction:column;justify-content:center;background:var(--mes-surface)}.timeline-corner{z-index:4;min-height:48px;padding:0 14px;color:var(--mes-text-secondary);font-size:11px;font-weight:650;background:var(--mes-surface-muted)}.timeline-axis{position:relative;height:48px}.axis-tick{position:absolute;bottom:0;display:flex;height:100%;flex-direction:column;justify-content:center;gap:3px;padding-left:4px;border-left:1px solid var(--mes-border);color:var(--mes-text-secondary);font-size:10px}.axis-tick small{color:var(--mes-text-muted);font-size:9px}.axis-tick:not(.has-date) small{color:var(--mes-text-secondary);font-size:10px}.timeline-row{min-height:86px;border-bottom:1px solid var(--mes-border)}.line-label{gap:3px;padding:12px 14px;border-right:1px solid var(--mes-border)}.line-label strong{overflow:hidden;max-width:140px;text-overflow:ellipsis;white-space:nowrap;font-size:12px}.line-label small{color:var(--mes-text-muted);font-size:10px}.timeline-lane{position:relative;min-height:calc(var(--tracks,1)*66px + 18px);background:#fff}.timeline-lane>i{position:absolute;top:0;bottom:0;width:1px;background:var(--mes-border);opacity:.7}.timeline-block{position:absolute;display:flex;min-width:154px;height:52px;overflow:hidden;flex-direction:column;justify-content:center;gap:3px;padding:6px 8px;color:#fff;border:1px solid rgb(var(--mes-primary-rgb)/75%);border-radius:5px;background:var(--mes-primary);box-shadow:0 3px 8px rgb(var(--mes-primary-rgb)/16%);font:inherit;text-align:left;cursor:pointer}.timeline-block.formal{border-color:#16764b;background:#1b8f5a}.timeline-block.late{border-color:#a96d00;background:var(--mes-warning)}.timeline-block.conflict{border-color:#b24a45;background:var(--mes-danger)}.timeline-block strong,.timeline-block span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.timeline-block strong{font-size:11px}.timeline-block span{color:rgb(255 255 255 / 84%);font-size:10px}.conflict-strip{display:flex;align-items:flex-start;gap:8px;padding:10px 14px;color:var(--mes-danger);border-top:1px solid var(--mes-border);background:var(--mes-danger-soft);font-size:11px}.conflict-strip>div{display:flex;min-width:0;flex:1;flex-direction:column;gap:5px}.conflict-strip button{overflow:hidden;padding:0;color:inherit;border:0;background:transparent;font:inherit;text-align:left;text-overflow:ellipsis;white-space:nowrap;cursor:pointer}.conflict-strip button:hover{text-decoration:underline}.dialog-tip{margin:0 0 14px;color:var(--mes-text-secondary);font-size:12px}.full-width{width:100%}.drawer-actions{width:100%;gap:8px}.drawer-actions>span{flex:1}.feasibility-panel{margin-top:14px}.feasibility-body{min-height:154px;padding:16px}.feasibility-loading{padding:0}.feasibility-result{max-height:360px;overflow:auto;overflow-wrap:anywhere;color:var(--mes-text-secondary);font-size:13px;line-height:1.75;white-space:pre-line}.feasibility-result p{margin:0}.feasibility-result ul{margin:14px 0;padding-left:20px}.feasibility-result li+li{margin-top:6px}.feasibility-result small{display:block;margin-top:14px;color:var(--mes-text-muted);font-size:10px}.feasibility-stale{display:flex;align-items:flex-start;gap:7px;margin-bottom:12px;padding:9px 10px;color:var(--mes-warning);border:1px solid rgb(169 109 0 / 28%);border-radius:5px;background:var(--mes-warning-soft);font-size:11px;line-height:1.55}@media(max-width:1050px){.top-grid{grid-template-columns:1fr;grid-auto-rows:auto}.pending-panel{height:500px}.overview-panel{min-height:360px}.workbench-summary{grid-template-columns:repeat(4,minmax(0,1fr))}.workbench-summary>div:last-child{grid-column:1/-1;border-top:1px solid var(--mes-border)}}@media(max-width:680px){.workbench-header{align-items:flex-start;flex-direction:column}.workbench-actions{width:100%;justify-content:space-between}.workbench-summary{grid-template-columns:repeat(2,minmax(0,1fr))}.workbench-summary>div{border-bottom:1px solid var(--mes-border)}.filter-row{flex-wrap:wrap}.filter-row :deep(.el-input){min-width:180px}.timeline-actions,.feasibility-actions{width:100%;justify-content:space-between}.mes-panel-header{align-items:flex-start;flex-direction:column}.timeline-toolbar{align-items:flex-start;flex-direction:column}.timeline-head,.timeline-row{grid-template-columns:132px var(--timeline-width,720px)}.line-label{padding:10px}.line-label strong{max-width:106px}.drawer-actions{flex-wrap:wrap}.drawer-actions>span{display:none}.drawer-actions :deep(.el-button){flex:1}.conflict-strip button{white-space:normal}}
.overview-panel{height:100%;min-height:0;align-self:stretch;background:var(--mes-surface)}.overview-grid{gap:1px;background:var(--mes-border)}.overview-grid>div,.overview-window>div,.overview-lines{--overview-color:var(--mes-primary);display:grid;min-width:0;grid-template-columns:48px minmax(0,1fr);align-items:center;gap:10px;background:var(--mes-surface)}.overview-grid>div{min-height:88px;padding:11px 13px}.overview-grid .tone-pending{--overview-color:#7d8793}.overview-grid .tone-formal{--overview-color:#1b8f5a}.overview-grid .tone-manual{--overview-color:var(--mes-primary)}.overview-grid .tone-risk{--overview-color:var(--mes-danger)}.overview-icon{display:inline-flex;width:40px;height:40px;align-items:center;justify-content:center;color:var(--overview-color);border:1px solid color-mix(in srgb,var(--overview-color) 24%,white);border-radius:6px;background:color-mix(in srgb,var(--overview-color) 10%,white)}.overview-copy{display:flex;min-width:0;flex-direction:column;align-items:flex-start;justify-content:center;gap:4px}.overview-copy small{overflow:hidden;max-width:100%;color:var(--mes-text-muted);font-size:10px;text-overflow:ellipsis;white-space:nowrap}.overview-copy strong{display:flex;max-width:100%;align-items:baseline;gap:4px;overflow:hidden;color:var(--mes-text);font-size:20px;font-variant-numeric:tabular-nums;text-overflow:ellipsis;white-space:nowrap}.overview-copy em{color:var(--mes-text-secondary);font-size:10px;font-style:normal;font-weight:600}.overview-window{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));border-top:1px solid var(--mes-border);background:var(--mes-border)}.overview-window>div{min-height:83px;padding:11px 13px}.overview-window>div+div{border-left:1px solid var(--mes-border)}.overview-window .tone-start{--overview-color:#2d8cf0}.overview-window .tone-end{--overview-color:#174a73}.overview-window .overview-copy strong{font-size:13px}.overview-lines{--overview-color:#59748a;min-height:72px;flex:1;padding:12px 14px;border-top:1px solid var(--mes-border);color:var(--mes-text)}.overview-lines .overview-copy strong{font-size:20px}@media(max-width:680px){.overview-grid>div,.overview-window>div,.overview-lines{grid-template-columns:42px minmax(0,1fr);gap:8px}.overview-icon{width:36px;height:36px}.overview-window>div{min-height:76px;padding:9px 10px}.overview-window .overview-copy strong{font-size:12px}.overview-lines{min-height:68px;padding:10px 12px}}
.timeline-inner{width:max(100%,calc(172px + var(--timeline-width,960px)))}.timeline-head,.timeline-row{grid-template-columns:172px minmax(var(--timeline-width,960px),1fr)}.timeline-axis{width:100%;min-width:var(--timeline-width,960px);box-sizing:border-box}.timeline-lane{width:100%;min-width:var(--timeline-width,960px);box-sizing:border-box;background:var(--mes-surface)}.timeline-axis::after,.timeline-lane::after{position:absolute;top:0;right:0;bottom:0;width:1px;background:var(--mes-border);content:''}@media(max-width:680px){.timeline-inner{width:max(100%,calc(132px + var(--timeline-width,720px)))}.timeline-head,.timeline-row{grid-template-columns:132px minmax(var(--timeline-width,720px),1fr)}.timeline-axis,.timeline-lane{min-width:var(--timeline-width,720px)}}
.timeline-block{box-sizing:border-box;min-width:0}
.timeline-zoom-controls{gap:8px!important;padding:3px 5px;border:1px solid var(--mes-border);border-radius:6px;background:var(--mes-surface)}
.timeline-zoom-controls :deep(.el-button){width:28px;height:28px;transition:color .16s ease,border-color .16s ease,background .16s ease}
.timeline-zoom-controls :deep(.el-slider){width:140px;margin:0 4px}
.timeline-zoom-controls :deep(.el-slider__runway){height:4px;margin:0;background:var(--mes-border)}
.timeline-zoom-controls :deep(.el-slider__bar){height:4px}
.timeline-zoom-controls :deep(.el-slider__button-wrapper){width:24px;height:24px;top:-10px}
.timeline-zoom-controls :deep(.el-slider__button){width:12px;height:12px;box-shadow:0 1px 3px rgb(33 43 54 / 20%);transition:transform .16s ease,box-shadow .16s ease}
.timeline-zoom-controls :deep(.el-slider__button-wrapper:focus-visible){outline:none}
.timeline-zoom-controls :deep(.el-slider__button-wrapper:focus-visible .el-slider__button){box-shadow:0 0 0 3px rgb(var(--mes-primary-rgb) / 22%)}
.timeline-zoom-controls :deep(.el-slider__button:hover){transform:scale(1.1)}
.timeline-head,.timeline-row{transition:grid-template-columns .16s ease-out}
.timeline-axis,.timeline-lane{transition:width .16s ease-out,min-width .16s ease-out}
.axis-tick,.timeline-lane>i{transition:left .16s ease-out}
.timeline-block{transition:left .16s ease-out,width .16s ease-out,box-shadow .16s ease-out}
@media(prefers-reduced-motion:reduce){.timeline-head,.timeline-row,.timeline-axis,.timeline-lane,.axis-tick,.timeline-lane>i,.timeline-block,.timeline-zoom-controls :deep(.el-button),.timeline-zoom-controls :deep(.el-slider__button){transition:none}}
:global(.timeline-plan-tooltip){display:flex;min-width:178px;flex-direction:column;gap:4px;font-size:12px;line-height:1.45}
:global(.timeline-plan-tooltip strong){margin-bottom:2px;font-size:12px}
:global(.timeline-plan-tooltip span){color:rgb(255 255 255 / 84%)}
.ai-advice-panel{margin-top:14px}
.ai-advice-actions{display:flex;align-items:center;gap:8px}
.ai-advice-actions :deep(.el-button)>span{display:inline-flex;align-items:center;gap:5px}
.ai-advice-body{min-height:154px;padding:16px}
.ai-advice-loading{padding:0}
.ai-advice-result{max-height:420px;overflow:auto;overflow-wrap:anywhere;color:var(--mes-text-secondary);font-size:13px;line-height:1.75}
.ai-advice-answer{display:grid;gap:12px}
.ai-advice-answer p{margin:0;white-space:pre-line}
.ai-advice-result ul{margin:14px 0;padding-left:20px}
.ai-advice-result li+li{margin-top:6px}
.ai-advice-result small{display:block;margin-top:14px;color:var(--mes-text-muted);font-size:10px}
.ai-advice-stale{display:flex;align-items:flex-start;gap:7px;margin-bottom:12px;padding:9px 10px;color:var(--mes-warning);border:1px solid rgb(169 109 0 / 28%);border-radius:5px;background:var(--mes-warning-soft);font-size:11px;line-height:1.55}
.ai-advice-references{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1px;margin:14px 0;border:1px solid var(--mes-border);background:var(--mes-border);white-space:normal}
.ai-advice-references-title{grid-column:1/-1;padding:7px 10px;color:var(--mes-text-secondary);background:var(--mes-surface-muted);font-size:11px;font-weight:600}
.ai-advice-reference{display:grid;min-width:0;grid-template-columns:34px minmax(0,1fr);gap:1px 8px;padding:8px 10px;background:var(--mes-surface)}
.ai-advice-reference strong{display:flex;grid-row:1/3;align-items:center;justify-content:center;color:var(--mes-primary);border-right:1px solid var(--mes-border);font-size:11px}
.ai-advice-reference span,.ai-advice-reference small{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.ai-advice-reference span{color:var(--mes-text);font-size:12px;font-weight:600}
.ai-advice-reference small{margin:0;color:var(--mes-text-muted);font-size:10px}
@media(max-width:680px){.ai-advice-actions{width:100%;justify-content:space-between}.ai-advice-references{grid-template-columns:1fr}}
</style>
