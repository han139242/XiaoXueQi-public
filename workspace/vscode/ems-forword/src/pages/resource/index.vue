<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppIcon from '@/components/common/AppIcon.vue'
import ResourceField from '@/components/business/ResourceField.vue'
import {
  createResource,
  deleteResource,
  downloadResource,
  executeResourceAction,
  exportResource,
  getResourceDetail,
  getResourceList,
  getResourcePage,
  updateResource,
  uploadResource
} from '@/api/resource'
import { getResourceConfig } from '@/config/resources'
import { clearReferenceOptions, createPreviewRows, loadReferenceOptions } from '@/utils/resource-options'
import { isFrameworkPreview } from '@/utils/auth'

const props = defineProps({
  resourceKey: { type: String, required: true }
})

const route = useRoute()
const router = useRouter()
const config = computed(() => getResourceConfig(props.resourceKey))
const previewMode = isFrameworkPreview()
const loading = ref(false)
const loadError = ref('')
const submitting = ref(false)
const exporting = ref(false)
const rows = ref([])
const total = ref(0)
const filters = reactive({})
const columnVisibility = reactive({})
const pagination = reactive({ current: 1, size: 20 })
const filterExpanded = ref(false)
const referenceOptions = reactive({})
const formOpen = ref(false)
const formMode = ref('create')
const formModel = ref({})
const formRef = ref()
const childRows = ref([])
const actionOpen = ref(false)
const currentAction = ref(null)
const actionRow = ref(null)
const actionModel = ref({})
const actionFormRef = ref()
const traceOpen = ref(false)
const traceTitle = ref('追溯结果')
const traceData = ref(null)
const uploadOpen = ref(false)
const uploadFile = ref(null)
const uploadMeta = reactive({ bizType: '', bizId: null })

const permissions = computed(() => ({ create: true, edit: true, delete: true, export: true, ...config.value.permissions }))
const tableFields = computed(() => config.value.fields.filter((item) => item.table !== false))
const visibleTableFields = computed(() => tableFields.value.filter((item) => columnVisibility[item.prop] !== false))
const visibleFilters = computed(() => filterExpanded.value ? config.value.filters : config.value.filters.slice(0, 4))
const hasExtraFilters = computed(() => config.value.filters.length > 4)
const formFields = computed(() => config.value.fields.filter((item) => {
  if (item.form === false) return false
  if (formMode.value === 'create' && item.create === false) return false
  if (formMode.value === 'edit' && item.edit === false) return false
  return true
}))
const formTitle = computed(() => `${formMode.value === 'create' ? '新增' : '编辑'}${config.value.title.replace('管理', '')}`)
const childConfig = computed(() => config.value.fullChild ? getResourceConfig(config.value.fullChild.resource) : null)
const childFields = computed(() => {
  if (!childConfig.value) return []
  return childConfig.value.fields.filter((item) => item.form !== false && item.prop !== config.value.fullChild.parentField)
})
const actionRules = computed(() => rulesFor(currentAction.value?.fields || [], 'action'))
const formRules = computed(() => rulesFor(formFields.value, formMode.value))
const activeFilterCount = computed(() => Object.values(filters).filter((value) => value !== '' && value !== null && value !== undefined).length)
const traceEntries = computed(() => {
  if (!traceData.value || typeof traceData.value !== 'object') return []
  return Object.entries(traceData.value)
})
const stateField = computed(() => {
  const priorities = ['auditStatus', 'stocktakeStatus', 'approvalStatus', 'qualityStatus', 'runningStatus', 'inspectionResult', 'status']
  return priorities.map((prop) => tableFields.value.find((item) => item.prop === prop && item.options?.length)).find(Boolean)
})
const pagePulse = computed(() => {
  const metrics = [{ key: 'records', label: '当前结果', value: total.value, type: 'primary' }]
  if (stateField.value) {
    return metrics.concat(stateField.value.options.map((item) => ({
      key: `${stateField.value.prop}-${item.value}`,
      label: item.label,
      value: rows.value.filter((row) => String(row[stateField.value.prop]) === String(item.value)).length,
      type: item.type || 'info'
    })))
  }
  const quantityFields = tableFields.value.filter((item) => item.type === 'number' && /(Quantity|Hours|Cost)$/.test(item.prop)).slice(0, 3)
  return metrics.concat(quantityFields.map((item) => ({
    key: item.prop,
    label: item.label,
    value: rows.value.reduce((sum, row) => sum + Number(row[item.prop] || 0), 0),
    type: 'info'
  })))
})

function rulesFor(fields, mode) {
  return Object.fromEntries(fields.map((item) => {
    const required = item.required || (mode === 'create' && item.requiredOnCreate)
    const rules = []
    if (required) rules.push({ required: true, message: `${item.label}不能为空`, trigger: item.type === 'select' || item.type === 'reference' ? 'change' : 'blur' })
    if (item.minLength) rules.push({ min: item.minLength, message: `${item.label}至少${item.minLength}个字符`, trigger: 'blur' })
    return [item.prop, rules]
  }).filter(([, rules]) => rules.length))
}

function optionsFor(field) {
  return field.options || referenceOptions[field.reference] || []
}

function optionFor(field, value) {
  return optionsFor(field).find((item) => String(item.value) === String(value))
}

function formatValue(field, value, row) {
  if (value === null || value === undefined || value === '') return '--'
  if (field.formatter) return field.formatter(value, row)
  const matched = optionFor(field, value)
  if (matched) return matched.label
  if (field.type === 'datetime') return dayjs(value).isValid() ? dayjs(value).format('YYYY-MM-DD HH:mm') : value
  if (field.type === 'date') return dayjs(value).isValid() ? dayjs(value).format('YYYY-MM-DD') : value
  if (/Id$/.test(field.prop) && field.type !== 'reference') return `#${value}`
  if (field.type === 'number' && typeof value === 'number') return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 4 }).format(value)
  if (Array.isArray(value)) return value.join('、')
  return String(value)
}

function formatMetric(value) {
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 2 }).format(Number(value || 0))
}

function cleanParams(source) {
  return Object.fromEntries(Object.entries(source).filter(([, value]) => value !== '' && value !== null && value !== undefined))
}

function flattenTree(items) {
  return (items || []).flatMap((item) => [item, ...flattenTree(item.children)])
}

function initFilters() {
  Object.keys(filters).forEach((key) => delete filters[key])
  config.value.filters.forEach((item) => {
    const queryValue = route.query[item.prop]
    filters[item.prop] = queryValue ?? ''
  })
}

function initColumns() {
  Object.keys(columnVisibility).forEach((key) => delete columnVisibility[key])
  let stored = {}
  try {
    stored = JSON.parse(localStorage.getItem(`mes_resource_columns:${config.value.key}`) || '{}')
  } catch {
    stored = {}
  }
  tableFields.value.forEach((item) => {
    columnVisibility[item.prop] = stored[item.prop] !== false
  })
}

function saveColumns() {
  localStorage.setItem(`mes_resource_columns:${config.value.key}`, JSON.stringify(columnVisibility))
}

function resetColumns() {
  tableFields.value.forEach((item) => {
    columnVisibility[item.prop] = true
  })
  saveColumns()
}

function initForm(row = {}) {
  const model = { ...config.value.defaults }
  config.value.fields.forEach((item) => {
    if (item.defaultValue !== undefined) model[item.prop] = item.defaultValue
  })
  formModel.value = { ...model, ...row }
}

function collectReferenceFields() {
  const actionFields = [...(config.value.actions || []), ...(config.value.globalActions || [])].flatMap((item) => item.fields || [])
  return [...config.value.fields, ...config.value.filters, ...actionFields, ...childFields.value]
    .filter((item) => item.type === 'reference' && item.reference)
}

async function loadReferences() {
  const keys = [...new Set(collectReferenceFields().map((item) => item.reference))]
  const results = await Promise.allSettled(keys.map(async (key) => {
    referenceOptions[key] = await loadReferenceOptions(key, previewMode)
  }))
  const failed = results.filter((item) => item.status === 'rejected').length
  if (failed && !previewMode) ElMessage.warning(`${failed} 组关联数据暂未加载，仍可使用编号操作`)
}

async function loadData() {
  loading.value = true
  loadError.value = ''
  try {
    if (previewMode) {
      const all = createPreviewRows(config.value, 12)
      const filtered = all.filter((row) => config.value.filters.every((item) => {
        const value = filters[item.prop]
        if (value === '' || value === null || value === undefined) return true
        if (item.prop === 'keyword') return Object.values(row).some((cell) => String(cell || '').toLowerCase().includes(String(value).toLowerCase()))
        return String(row[item.prop] ?? '') === String(value) || String(row[item.prop] ?? '').includes(String(value))
      }))
      rows.value = filtered
      total.value = filtered.length
      return
    }
    const params = cleanParams({ current: pagination.current, size: pagination.size, ...filters })
    const result = config.value.treeEndpoint
      ? await getResourceList(config.value.treeEndpoint, params)
      : await getResourcePage(config.value.baseUrl, params)
    const data = result?.data
    if (Array.isArray(data)) {
      rows.value = data
      total.value = flattenTree(data).length
    } else {
      rows.value = data?.records || []
      total.value = Number(data?.total || 0)
      pagination.current = Number(data?.current || pagination.current)
      pagination.size = Number(data?.size || pagination.size)
    }
  } catch (error) {
    rows.value = []
    total.value = 0
    loadError.value = error?.displayMessage || error?.message || '数据加载失败'
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function resetSearch() {
  initFilters()
  pagination.current = 1
  loadData()
}

async function openCreate() {
  formMode.value = 'create'
  initForm()
  childRows.value = []
  formOpen.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function fetchRow(row) {
  if (previewMode) return { ...row }
  const result = await getResourceDetail(config.value.baseUrl, row[config.value.idField])
  return result?.data || { ...row }
}

async function loadChildren(row) {
  if (!config.value.fullChild) return
  if (previewMode) {
    childRows.value = createPreviewRows(childConfig.value, 3).map((item) => ({ ...item, _clientId: crypto.randomUUID() }))
    return
  }
  const result = await getResourceList(config.value.fullChild.endpoint(row))
  const data = result?.data
  childRows.value = (Array.isArray(data) ? data : data?.records || []).map((item) => ({ ...item, _clientId: crypto.randomUUID() }))
}

async function openEdit(row) {
  formMode.value = 'edit'
  submitting.value = true
  try {
    initForm(await fetchRow(row))
    await loadChildren(row)
    formOpen.value = true
    await nextTick()
    formRef.value?.clearValidate()
  } catch {
    formOpen.value = false
  } finally {
    submitting.value = false
  }
}

async function openDetail(row) {
  submitting.value = true
  try {
    formMode.value = 'view'
    formModel.value = await fetchRow(row)
    await loadChildren(row)
    formOpen.value = true
  } catch {
    formOpen.value = false
  } finally {
    submitting.value = false
  }
}

function addChild() {
  const child = { ...childConfig.value.defaults, _clientId: crypto.randomUUID() }
  childConfig.value.fields.forEach((item) => {
    if (item.defaultValue !== undefined) child[item.prop] = item.defaultValue
  })
  childRows.value.push(child)
}

function removeChild(index) {
  childRows.value.splice(index, 1)
}

function validateChildren() {
  if (!config.value.fullChild) return true
  for (const [index, row] of childRows.value.entries()) {
    const missing = childFields.value.find((item) => item.required && (row[item.prop] === '' || row[item.prop] === null || row[item.prop] === undefined))
    if (missing) {
      ElMessage.warning(`第 ${index + 1} 条${config.value.fullChild.title}的“${missing.label}”不能为空`)
      return false
    }
  }
  return true
}

function submitPayload() {
  const allowed = new Set([...formFields.value.map((item) => item.prop), config.value.idField])
  if (formMode.value === 'create') {
    Object.keys(config.value.defaults || {}).forEach((key) => allowed.add(key))
  }
  const payload = Object.fromEntries(Object.entries(formModel.value).filter(([key]) => allowed.has(key)))
  if (config.value.fullChild) {
    payload[config.value.fullChild.property] = childRows.value.map(({ _clientId, ...item }) => item)
  }
  return payload
}

async function submitForm() {
  if (formMode.value === 'view') return
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  if (config.value.fullChild && !childRows.value.length) {
    ElMessage.warning(`请至少添加一条${config.value.fullChild.title}`)
    return
  }
  if (!validateChildren()) return
  submitting.value = true
  try {
    const payload = submitPayload()
    if (previewMode) {
      if (formMode.value === 'create') {
        rows.value.unshift({ ...payload, [config.value.idField]: Date.now() })
        total.value += 1
      } else {
        const index = rows.value.findIndex((item) => item[config.value.idField] === payload[config.value.idField])
        if (index >= 0) rows.value[index] = { ...rows.value[index], ...payload }
      }
    } else if (formMode.value === 'create') {
      await createResource(config.value.baseUrl, payload, config.value.createPath || '')
    } else {
      await updateResource(config.value.baseUrl, payload, config.value.updatePath || '')
    }
    clearReferenceOptions(config.value.key)
    ElMessage.success(`${formMode.value === 'create' ? '新增' : '保存'}成功`)
    formOpen.value = false
    if (!previewMode) await loadData()
  } catch {
    // 请求层已经给出具体错误提示。
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除这条${config.value.title.replace('管理', '')}记录吗？`, '删除确认', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  } catch {
    return
  }
  try {
    if (previewMode) {
      rows.value = rows.value.filter((item) => item[config.value.idField] !== row[config.value.idField])
      total.value = rows.value.length
    } else {
      await deleteResource(config.value.baseUrl, row[config.value.idField])
    }
    clearReferenceOptions(config.value.key)
    ElMessage.success('删除成功')
    if (!previewMode) await loadData()
  } catch {
    // 请求层已经给出具体错误提示。
  }
}

async function handleExport() {
  if (previewMode) {
    ElMessage.info('框架预览不生成真实导出文件')
    return
  }
  exporting.value = true
  try {
    const response = await exportResource(config.value.baseUrl, cleanParams(filters))
    saveBlob(response, `${config.value.title}-${dayjs().format('YYYYMMDD-HHmm')}.xlsx`)
    ElMessage.success('导出任务已完成')
  } catch {
    // 请求层已经给出具体错误提示。
  } finally {
    exporting.value = false
  }
}

function saveBlob(response, fallbackName) {
  const blob = response.data instanceof Blob ? response.data : new Blob([response.data])
  const disposition = response.headers?.['content-disposition'] || ''
  const encodedName = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  const filename = encodedName ? decodeURIComponent(encodedName) : fallbackName
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.click()
  URL.revokeObjectURL(url)
}

function visibleActions(row) {
  return (config.value.actions || []).filter((item) => !item.visible || item.visible(row))
}

function canEditRow(row) {
  return permissions.value.edit && (!config.value.canEdit || config.value.canEdit(row))
}

function canDeleteRow(row) {
  return permissions.value.delete && (!config.value.canDelete || config.value.canDelete(row))
}

function rowLockReason(row, action) {
  if (typeof config.value.lockedReason === 'function') return config.value.lockedReason(row, action)
  return config.value.lockedReason || `当前状态不可${action === 'edit' ? '编辑' : '删除'}`
}

async function openAction(action, row = null) {
  if (action.kind === 'download') {
    if (previewMode) return ElMessage.info('框架预览不下载真实文件')
    try {
      const response = await downloadResource(action.path(row))
      saveBlob(response, row.fileName || '附件下载')
    } catch {
      // 请求层已经给出具体错误提示。
    }
    return
  }
  if (action.kind === 'trace') {
    try {
      traceTitle.value = action.label
      traceData.value = previewMode ? { 当前记录: row, 追溯结论: '开发预览链路完整，正式环境将显示真实后端结果' } : (await getResourceList(action.path(row))).data
      traceOpen.value = true
    } catch {
      traceOpen.value = false
    }
    return
  }
  if (action.kind === 'upload') {
    uploadFile.value = null
    uploadMeta.bizType = ''
    uploadMeta.bizId = null
    currentAction.value = action
    uploadOpen.value = true
    return
  }
  currentAction.value = action
  actionRow.value = row
  actionModel.value = Object.fromEntries((action.fields || []).map((item) => [
    item.prop,
    item.defaultValue ?? (item.multiple ? [] : item.type === 'number' ? undefined : '')
  ]))
  if (action.fields?.length) {
    actionOpen.value = true
    await nextTick()
    actionFormRef.value?.clearValidate()
  } else {
    try {
      await ElMessageBox.confirm(`确认执行“${action.label}”吗？`, '操作确认', { type: 'warning' })
    } catch {
      return
    }
    await submitAction()
  }
}

async function submitAction() {
  if (currentAction.value?.fields?.length) {
    try {
      await actionFormRef.value?.validate()
    } catch {
      return
    }
  }
  submitting.value = true
  try {
    const action = currentAction.value
    if (previewMode) {
      if (actionRow.value && action.previewPatch) Object.assign(actionRow.value, action.previewPatch(actionModel.value, actionRow.value))
    } else {
      const rawData = action.payload ? action.payload(actionModel.value, actionRow.value) : actionModel.value
      const data = cleanParams(rawData)
      await executeResourceAction({ method: action.method || 'put', url: action.url || action.path(actionRow.value), data })
    }
    ElMessage.success(`${currentAction.value.label}成功`)
    actionOpen.value = false
    if (!previewMode) await loadData()
  } catch {
    // 请求层已经给出具体错误提示。
  } finally {
    submitting.value = false
  }
}

async function submitUpload() {
  if (!uploadFile.value) return ElMessage.warning('请选择需要上传的文件')
  submitting.value = true
  try {
    if (!previewMode) {
      const formData = new FormData()
      formData.append('file', uploadFile.value.raw)
      if (uploadMeta.bizType) formData.append('bizType', uploadMeta.bizType)
      if (uploadMeta.bizId !== null && uploadMeta.bizId !== '') formData.append('bizId', uploadMeta.bizId)
      await uploadResource(currentAction.value.url, formData)
    }
    ElMessage.success('文件上传成功')
    uploadOpen.value = false
    await loadData()
  } catch {
    // 请求层已经给出具体错误提示。
  } finally {
    submitting.value = false
  }
}

function openRelation(relation, row) {
  const target = getResourceConfig(relation.resource)
  router.push({ path: target.routePath, query: { [relation.parentField]: row[config.value.idField], from: config.value.title } })
}

function handlePageChange(value) {
  pagination.current = value
  loadData()
}

function handleSizeChange(value) {
  pagination.size = value
  pagination.current = 1
  loadData()
}

async function initialize() {
  initFilters()
  initColumns()
  pagination.current = 1
  await Promise.all([loadReferences(), loadData()])
}

watch(() => route.fullPath, initialize)
onMounted(initialize)
</script>

<template>
  <div v-if="config" class="mes-page resource-page">
    <header class="resource-heading">
      <div class="resource-title-group">
        <span class="resource-title-icon"><AppIcon :name="config.icon" :size="22" /></span>
        <div>
          <div class="resource-title-line">
            <h1 class="mes-page-title">{{ config.title }}</h1>
            <el-tag v-if="previewMode" size="small" type="warning" effect="plain">预览数据</el-tag>
          </div>
          <p class="mes-page-subtitle">{{ config.description }}</p>
        </div>
      </div>
      <div class="resource-summary">
        <span>当前结果</span><strong>{{ total }}</strong><small>条</small>
      </div>
    </header>

    <section class="resource-pulse" aria-label="当前页数据态势">
      <div class="pulse-caption">
        <span>当前页态势</span>
        <small>{{ config.treeEndpoint ? '树形数据' : `第 ${pagination.current} 页 · ${pagination.size} 条/页` }}</small>
      </div>
      <div class="pulse-metrics">
        <div v-for="metric in pagePulse" :key="metric.key" class="pulse-metric" :class="`tone-${metric.type}`">
          <i />
          <span>{{ metric.label }}</span>
          <strong>{{ formatMetric(metric.value) }}</strong>
        </div>
      </div>
    </section>

    <section class="resource-search-band">
      <div class="filter-grid">
        <div v-for="item in visibleFilters" :key="item.prop" class="filter-item">
          <label>{{ item.label }}</label>
          <ResourceField v-model="filters[item.prop]" :field="item" :options="optionsFor(item)" compact @keyup.enter="handleSearch" />
        </div>
      </div>
      <div class="filter-actions">
        <el-button type="primary" @click="handleSearch"><AppIcon name="search" :size="15" />查询</el-button>
        <el-button @click="resetSearch"><AppIcon name="refresh" :size="15" />重置</el-button>
        <el-button v-if="hasExtraFilters" text @click="filterExpanded = !filterExpanded">
          <AppIcon name="filter" :size="15" />{{ filterExpanded ? '收起条件' : `更多条件${activeFilterCount ? ` (${activeFilterCount})` : ''}` }}
        </el-button>
      </div>
    </section>

    <section class="resource-workspace">
      <div class="resource-toolbar">
        <div class="toolbar-primary">
          <el-button v-if="permissions.create" type="primary" @click="openCreate"><AppIcon name="plus" :size="16" />新增</el-button>
          <el-button
            v-for="action in config.globalActions || []"
            :key="action.key"
            :type="action.type || 'default'"
            :plain="action.type !== 'primary'"
            @click="openAction(action)"
          >
            <AppIcon :name="action.icon || 'plus'" :size="16" />{{ action.label }}
          </el-button>
        </div>
        <div class="toolbar-secondary">
          <span class="data-source"><i />{{ previewMode ? '开发预览' : '实时接口' }}</span>
          <el-tooltip content="导出 Excel" placement="top">
            <el-button v-if="permissions.export" class="mes-icon-button" :loading="exporting" aria-label="导出 Excel" @click="handleExport"><AppIcon name="download" :size="16" /></el-button>
          </el-tooltip>
          <el-popover placement="bottom-end" :width="250" trigger="click">
            <template #reference>
              <el-button class="mes-icon-button" aria-label="设置显示字段"><AppIcon name="columns" :size="16" /></el-button>
            </template>
            <div class="column-settings">
              <header><strong>显示字段</strong><el-button link type="primary" @click="resetColumns">重置</el-button></header>
              <div>
                <el-checkbox
                  v-for="item in tableFields"
                  :key="item.prop"
                  v-model="columnVisibility[item.prop]"
                  :disabled="visibleTableFields.length === 1 && columnVisibility[item.prop]"
                  @change="saveColumns"
                >{{ item.label }}</el-checkbox>
              </div>
            </div>
          </el-popover>
          <el-tooltip content="刷新数据" placement="top">
            <el-button class="mes-icon-button" aria-label="刷新数据" @click="loadData"><AppIcon name="refresh" :size="16" /></el-button>
          </el-tooltip>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        :row-key="config.idField"
        :tree-props="{ children: 'children' }"
        default-expand-all
        stripe
        border
        height="min(58vh, 620px)"
        class="resource-table"
      >
        <el-table-column type="index" label="#" width="54" align="center" />
        <el-table-column
          v-for="(item, index) in visibleTableFields"
          :key="item.prop"
          :prop="item.prop"
          :label="item.label"
          :min-width="item.width || (item.type === 'textarea' ? 190 : item.type === 'datetime' ? 168 : 128)"
          :sortable="['text', 'number', 'date', 'datetime'].includes(item.type)"
          show-overflow-tooltip
        >
          <template #default="scope">
            <button v-if="index === 0" class="primary-cell" type="button" @click="openDetail(scope.row)">{{ formatValue(item, scope.row[item.prop], scope.row) }}</button>
            <el-tag v-else-if="item.options?.length" :type="optionFor(item, scope.row[item.prop])?.type || 'info'" size="small" effect="light">
              {{ formatValue(item, scope.row[item.prop], scope.row) }}
            </el-tag>
            <span v-else :class="{ 'reference-cell': item.type === 'reference' }">{{ formatValue(item, scope.row[item.prop], scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="218" fixed="right" align="center">
          <template #default="scope">
            <div class="row-actions">
              <el-tooltip content="查看详情" placement="top"><el-button link type="primary" aria-label="查看详情" @click="openDetail(scope.row)"><AppIcon name="eye" :size="16" /></el-button></el-tooltip>
              <el-tooltip v-if="permissions.edit" :content="canEditRow(scope.row) ? '编辑' : rowLockReason(scope.row, 'edit')" placement="top">
                <span class="row-action-control"><el-button link type="primary" aria-label="编辑" :disabled="!canEditRow(scope.row)" @click="openEdit(scope.row)"><AppIcon name="edit" :size="16" /></el-button></span>
              </el-tooltip>
              <el-dropdown v-if="visibleActions(scope.row).length || config.relations?.length" trigger="click">
                <el-button link type="primary">更多<AppIcon name="chevron" :size="13" /></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="action in visibleActions(scope.row)" :key="action.key" @click="openAction(action, scope.row)">
                      <AppIcon :name="action.icon || 'activity'" :size="15" />{{ action.label }}
                    </el-dropdown-item>
                    <el-dropdown-item v-for="relation in config.relations || []" :key="relation.key" @click="openRelation(relation, scope.row)">
                      <AppIcon name="link" :size="15" />{{ relation.title }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-tooltip v-if="permissions.delete" :content="canDeleteRow(scope.row) ? '删除' : rowLockReason(scope.row, 'delete')" placement="top">
                <span class="row-action-control"><el-button link type="danger" aria-label="删除" :disabled="!canDeleteRow(scope.row)" @click="handleDelete(scope.row)"><AppIcon name="trash" :size="16" /></el-button></span>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <div class="table-empty">
            <el-empty :description="loadError || '暂无符合条件的数据'" :image-size="70">
              <el-button v-if="loadError" type="primary" plain @click="loadData"><AppIcon name="refresh" :size="15" />重新加载</el-button>
            </el-empty>
          </div>
        </template>
      </el-table>

      <div v-if="!config.treeEndpoint" class="resource-pagination">
        <span>共 {{ total }} 条记录</span>
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-drawer v-model="formOpen" class="resource-drawer" :size="formMode === 'view' ? 'min(720px, 100vw)' : 'min(820px, 100vw)'" :with-header="false" append-to-body>
      <div class="drawer-shell">
        <header class="drawer-header">
          <div><span class="drawer-icon"><AppIcon :name="formMode === 'view' ? 'eye' : config.icon" :size="19" /></span><div><strong>{{ formMode === 'view' ? `${config.title.replace('管理', '')}详情` : formTitle }}</strong><small>{{ formMode === 'view' ? '完整业务字段与关联信息' : '请按实际业务信息填写' }}</small></div></div>
          <el-button text aria-label="关闭" @click="formOpen = false"><AppIcon name="close" :size="18" /></el-button>
        </header>
        <main class="drawer-content">
          <el-descriptions v-if="formMode === 'view'" :column="2" border class="detail-descriptions">
            <el-descriptions-item v-for="item in config.fields.filter((fieldItem) => fieldItem.detail !== false)" :key="item.prop" :label="item.label">
              <el-tag v-if="item.options?.length" :type="optionFor(item, formModel[item.prop])?.type || 'info'" size="small">{{ formatValue(item, formModel[item.prop], formModel) }}</el-tag>
              <span v-else>{{ formatValue(item, formModel[item.prop], formModel) }}</span>
            </el-descriptions-item>
          </el-descriptions>
          <el-form v-else ref="formRef" :model="formModel" :rules="formRules" label-position="top" class="resource-form">
            <el-form-item v-for="item in formFields" :key="item.prop" :label="item.label" :prop="item.prop" :class="{ wide: item.type === 'textarea' }">
              <ResourceField v-model="formModel[item.prop]" :field="item" :options="optionsFor(item)" />
            </el-form-item>
          </el-form>

          <section v-if="config.fullChild" class="child-editor">
            <div class="child-editor-head"><div><strong>{{ config.fullChild.title }}</strong><span>{{ childRows.length }} 条</span></div><el-button v-if="formMode !== 'view'" type="primary" plain @click="addChild"><AppIcon name="plus" :size="15" />添加明细</el-button></div>
            <el-table :data="childRows" border size="small" empty-text="暂无明细">
              <el-table-column type="index" width="48" align="center" />
              <el-table-column v-for="item in childFields" :key="item.prop" :label="item.label" :min-width="item.type === 'textarea' ? 180 : 145">
                <template #default="scope">
                  <ResourceField v-if="formMode !== 'view'" v-model="scope.row[item.prop]" :field="item" :options="optionsFor(item)" compact />
                  <span v-else>{{ formatValue(item, scope.row[item.prop], scope.row) }}</span>
                </template>
              </el-table-column>
              <el-table-column v-if="formMode !== 'view'" width="58" fixed="right" align="center"><template #default="scope"><el-button link type="danger" aria-label="移除明细" @click="removeChild(scope.$index)"><AppIcon name="trash" :size="15" /></el-button></template></el-table-column>
            </el-table>
          </section>
        </main>
        <footer class="drawer-footer">
          <el-button @click="formOpen = false">{{ formMode === 'view' ? '关闭' : '取消' }}</el-button>
          <el-button v-if="formMode !== 'view'" type="primary" :loading="submitting" @click="submitForm"><AppIcon name="save" :size="16" />保存</el-button>
        </footer>
      </div>
    </el-drawer>

    <el-dialog v-model="actionOpen" :title="currentAction?.label" width="min(520px, 92vw)" append-to-body>
      <el-form ref="actionFormRef" :model="actionModel" :rules="actionRules" label-position="top">
        <el-form-item v-for="item in currentAction?.fields || []" :key="item.prop" :label="item.label" :prop="item.prop">
          <ResourceField v-model="actionModel[item.prop]" :field="item" :options="optionsFor(item)" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="actionOpen = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitAction">确认执行</el-button></template>
    </el-dialog>

    <el-dialog v-model="uploadOpen" title="上传附件" width="min(520px, 92vw)" append-to-body>
      <el-upload drag :auto-upload="false" :limit="1" :on-change="(file) => uploadFile = file" :on-remove="() => uploadFile = null">
        <AppIcon name="upload" :size="28" />
        <div class="upload-copy"><strong>选择或拖放文件</strong><span>文件将通过后端安全上传接口保存</span></div>
      </el-upload>
      <div class="upload-meta"><el-input v-model="uploadMeta.bizType" placeholder="业务类型（可选）" /><el-input-number v-model="uploadMeta.bizId" :min="1" controls-position="right" placeholder="业务 ID" /></div>
      <template #footer><el-button @click="uploadOpen = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitUpload">上传</el-button></template>
    </el-dialog>

    <el-drawer v-model="traceOpen" class="resource-drawer" size="min(760px, 100vw)" :with-header="false" append-to-body>
      <div class="drawer-shell"><header class="drawer-header"><div><span class="drawer-icon"><AppIcon name="scan" :size="19" /></span><div><strong>{{ traceTitle }}</strong><small>来自后端追溯接口的实时结果</small></div></div><el-button text @click="traceOpen = false"><AppIcon name="close" :size="18" /></el-button></header>
        <main class="drawer-content trace-content">
          <section v-for="([key, value]) in traceEntries" :key="key" class="trace-section">
            <h3>{{ key }}</h3>
            <el-table v-if="Array.isArray(value)" :data="value" border size="small"><el-table-column v-for="column in Object.keys(value[0] || {})" :key="column" :prop="column" :label="column" min-width="120" show-overflow-tooltip /></el-table>
            <el-descriptions v-else-if="value && typeof value === 'object'" :column="1" border><el-descriptions-item v-for="(childValue, childKey) in value" :key="childKey" :label="childKey">{{ typeof childValue === 'object' ? JSON.stringify(childValue) : childValue }}</el-descriptions-item></el-descriptions>
            <p v-else>{{ value ?? '--' }}</p>
          </section>
          <el-empty v-if="!traceEntries.length" description="暂无追溯结果" />
        </main>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.resource-page { min-width: 0; }
.resource-heading { display:flex; min-height:72px; align-items:center; justify-content:space-between; gap:20px; margin-bottom:12px; padding:0 4px; }
.resource-title-group,.resource-title-line,.resource-summary,.resource-toolbar,.toolbar-primary,.toolbar-secondary,.filter-actions,.row-actions,.drawer-header>div,.child-editor-head,.child-editor-head>div,.data-source { display:flex; align-items:center; }
.resource-title-group { gap:13px; }
.resource-title-icon { display:inline-flex; width:44px; height:44px; flex:none; align-items:center; justify-content:center; color:var(--mes-primary); border:1px solid #bfd2e6; border-radius:var(--mes-radius); background:var(--mes-primary-soft); }
.resource-title-line { gap:10px; }
.resource-summary { min-width:128px; justify-content:flex-end; gap:6px; color:var(--mes-text-muted); }
.resource-summary strong { color:var(--mes-text); font-size:24px; font-variant-numeric:tabular-nums; }
.resource-summary span,.resource-summary small { font-size:11px; }
.resource-pulse { display:grid; min-height:58px; grid-template-columns:124px minmax(0,1fr); align-items:stretch; margin-bottom:12px; overflow:hidden; border:1px solid var(--mes-border); border-left:3px solid var(--mes-primary); border-radius:var(--mes-radius); background:#fff; }
.pulse-caption { display:flex; flex-direction:column; justify-content:center; padding:9px 14px; border-right:1px solid var(--mes-border); }
.pulse-caption span { color:var(--mes-text); font-size:12px; font-weight:650; }
.pulse-caption small { margin-top:3px; color:var(--mes-text-muted); font-size:9px; white-space:nowrap; }
.pulse-metrics { display:flex; min-width:0; align-items:stretch; overflow-x:auto; }
.pulse-metric { --pulse-color:var(--mes-text-muted); display:grid; min-width:112px; grid-template-columns:7px minmax(0,1fr); grid-template-rows:auto auto; align-content:center; column-gap:8px; padding:8px 14px; border-right:1px solid var(--mes-border); }
.pulse-metric i { width:7px; height:7px; grid-row:1/3; align-self:center; border-radius:50%; background:var(--pulse-color); box-shadow:0 0 0 3px color-mix(in srgb,var(--pulse-color) 14%,transparent); }
.pulse-metric span { color:var(--mes-text-muted); font-size:9px; white-space:nowrap; }
.pulse-metric strong { margin-top:1px; color:var(--mes-text); font-size:16px; font-variant-numeric:tabular-nums; }
.pulse-metric.tone-primary { --pulse-color:var(--mes-primary); }
.pulse-metric.tone-success { --pulse-color:var(--mes-success); }
.pulse-metric.tone-warning { --pulse-color:var(--mes-warning); }
.pulse-metric.tone-danger { --pulse-color:var(--mes-danger); }
.pulse-metric.tone-info { --pulse-color:var(--mes-data); }
.resource-search-band { display:grid; grid-template-columns:minmax(0,1fr) auto; gap:14px; align-items:end; margin-bottom:12px; padding:14px 16px; border:1px solid var(--mes-border); border-radius:var(--mes-radius); background:#fff; }
.filter-grid { display:grid; min-width:0; grid-template-columns:repeat(4,minmax(150px,1fr)); gap:11px 14px; }
.filter-item { min-width:0; }
.filter-item label { display:block; margin-bottom:5px; color:var(--mes-text-secondary); font-size:11px; font-weight:600; }
.filter-actions { align-self:end; gap:7px; }
.filter-actions :deep(.el-button) { margin:0; }
.resource-workspace { overflow:hidden; border:1px solid var(--mes-border); border-radius:var(--mes-radius); background:#fff; box-shadow:var(--mes-shadow-sm); }
.resource-toolbar { min-height:52px; justify-content:space-between; gap:12px; padding:8px 12px; border-top:2px solid var(--mes-primary); border-bottom:1px solid var(--mes-border); }
.toolbar-primary,.toolbar-secondary { gap:7px; }
.toolbar-primary :deep(.el-button),.toolbar-secondary :deep(.el-button) { margin:0; }
.data-source { gap:6px; margin-right:4px; color:var(--mes-text-muted); font-size:10px; }
.data-source i { width:6px; height:6px; border-radius:50%; background:var(--mes-primary); box-shadow:0 0 0 3px var(--mes-primary-soft); }
.column-settings header { display:flex; min-height:34px; align-items:center; justify-content:space-between; border-bottom:1px solid var(--mes-border); }
.column-settings header strong { font-size:12px; }
.column-settings header :deep(.el-button) { margin:0; }
.column-settings>div { display:grid; max-height:280px; grid-template-columns:1fr 1fr; gap:2px 10px; overflow:auto; padding-top:8px; }
.column-settings :deep(.el-checkbox) { min-width:0; margin:0; }
.column-settings :deep(.el-checkbox__label) { overflow:hidden; font-size:11px; text-overflow:ellipsis; white-space:nowrap; }
.resource-table { width:100%; }
.resource-table :deep(.el-table__cell) { padding:7px 0; }
.resource-table :deep(.cell) { font-size:12px; }
.table-empty { display:flex; min-height:230px; align-items:center; justify-content:center; }
.primary-cell { max-width:100%; overflow:hidden; padding:0; color:#1d55a4; border:0; background:transparent; font-weight:600; text-overflow:ellipsis; white-space:nowrap; cursor:pointer; }
.primary-cell:hover { color:var(--mes-primary); text-decoration:underline; }
.reference-cell { color:#3b5f7e; }
.row-actions { justify-content:center; gap:5px; }
.row-actions :deep(.el-button) { margin:0; padding:4px; }
.row-action-control { display:inline-flex; }
.resource-pagination { display:flex; min-height:58px; align-items:center; justify-content:space-between; gap:16px; padding:10px 14px; border-top:1px solid var(--mes-border); }
.resource-pagination>span { color:var(--mes-text-muted); font-size:11px; }
.drawer-shell { display:grid; height:100dvh; grid-template-rows:auto minmax(0,1fr) auto; background:#f4f6f8; }
.drawer-header { display:flex; min-height:70px; align-items:center; justify-content:space-between; padding:0 18px; color:#edf5fb; background:#171e27; }
.drawer-header>div { gap:11px; }
.drawer-header>div>div { display:flex; flex-direction:column; }
.drawer-header strong { font-size:15px; }
.drawer-header small { margin-top:2px; color:#91a1b2; font-size:9px; }
.drawer-header :deep(.el-button) { color:#aeb9c5; }
.drawer-icon { display:inline-flex; width:36px; height:36px; align-items:center; justify-content:center; color:#c6e1f5; border:1px solid #3f668a; border-radius:50%; background:#1e3c58; }
.drawer-content { min-height:0; overflow:auto; padding:18px; }
.resource-form { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:0 18px; padding:18px; border:1px solid var(--mes-border); border-radius:var(--mes-radius); background:#fff; }
.resource-form :deep(.el-form-item) { min-width:0; margin-bottom:16px; }
.resource-form .wide { grid-column:1/-1; }
.drawer-footer { display:flex; min-height:64px; align-items:center; justify-content:flex-end; gap:8px; padding:10px 18px; border-top:1px solid var(--mes-border); background:#fff; }
.drawer-footer :deep(.el-button) { min-width:88px; margin:0; }
.detail-descriptions :deep(.el-descriptions__label) { width:126px; color:var(--mes-text-secondary); }
.child-editor { margin-top:16px; overflow:hidden; border:1px solid var(--mes-border); border-radius:var(--mes-radius); background:#fff; }
.child-editor-head { min-height:52px; justify-content:space-between; padding:8px 12px; border-bottom:1px solid var(--mes-border); }
.child-editor-head>div { gap:9px; }
.child-editor-head strong { font-size:13px; }
.child-editor-head span { color:var(--mes-text-muted); font-size:10px; }
.child-editor :deep(.el-input-number),.child-editor :deep(.el-select),.child-editor :deep(.el-date-editor) { width:100%; }
.upload-copy { display:flex; flex-direction:column; margin-top:9px; }
.upload-copy strong { font-size:13px; }
.upload-copy span { margin-top:3px; color:var(--mes-text-muted); font-size:10px; }
.upload-meta { display:grid; grid-template-columns:1fr 1fr; gap:10px; margin-top:14px; }
.upload-meta :deep(.el-input-number) { width:100%; }
.trace-content { display:flex; flex-direction:column; gap:14px; }
.trace-section { overflow:hidden; border:1px solid var(--mes-border); border-radius:var(--mes-radius); background:#fff; }
.trace-section h3 { padding:11px 14px; border-bottom:1px solid var(--mes-border); font-size:13px; }
.trace-section>p { padding:14px; color:var(--mes-text-secondary); font-size:12px; white-space:pre-wrap; }
@media (max-width:1280px){.filter-grid{grid-template-columns:repeat(2,minmax(160px,1fr));}.resource-search-band{grid-template-columns:1fr}.filter-actions{justify-content:flex-end}}
@media (max-width:720px){.resource-heading{align-items:flex-start;flex-direction:column}.resource-summary{display:none}.resource-pulse{grid-template-columns:1fr}.pulse-caption{border-right:0;border-bottom:1px solid var(--mes-border)}.filter-grid{grid-template-columns:1fr}.filter-actions{flex-wrap:wrap;justify-content:flex-start}.resource-toolbar{align-items:flex-start;flex-direction:column}.toolbar-primary{width:100%;overflow-x:auto}.toolbar-secondary{align-self:flex-end}.resource-pagination{align-items:flex-start;flex-direction:column}.resource-pagination :deep(.el-pagination){max-width:100%;overflow-x:auto}.resource-form{grid-template-columns:1fr;padding:14px}.resource-form .wide{grid-column:auto}.detail-descriptions :deep(.el-descriptions__body){overflow-x:auto}.upload-meta{grid-template-columns:1fr}}
</style>
