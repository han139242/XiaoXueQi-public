<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import AppIcon from '@/components/common/AppIcon.vue'
import {
  acknowledgeNotification,
  getNotificationPage,
  getNotificationUnreadCount,
  markAllNotificationsRead,
  markNotificationRead
} from '@/api/sys/notification'
import { hasPermission } from '@/utils/permission'

const router = useRouter()
const loading = ref(false)
const markingAll = ref(false)
const rows = ref([])
const total = ref(0)
const unreadTotal = ref(0)
const categoryCounts = ref({})
const filter = reactive({ current: 1, size: 20, view: 'UNREAD', category: '', severity: '' })

const canRead = computed(() => hasPermission('sys:notification:read'))
const canAcknowledge = computed(() => hasPermission('sys:notification:ack'))
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / filter.size)))

const categoryOptions = [
  { value: '', label: '全部分类' },
  { value: 'MFG', label: '生产' },
  { value: 'PLAN', label: '计划' },
  { value: 'QMS', label: '质量' },
  { value: 'WMS', label: '仓储' },
  { value: 'EAM', label: '设备' },
  { value: 'SYSTEM', label: '系统' }
]

const severityOptions = [
  { value: '', label: '全部级别' },
  { value: 'INFO', label: '普通' },
  { value: 'WARNING', label: '提醒' },
  { value: 'HIGH', label: '重要' },
  { value: 'CRITICAL', label: '紧急' }
]

async function loadData() {
  loading.value = true
  try {
    const [pageResult, unreadResult] = await Promise.all([
      getNotificationPage({
        current: filter.current,
        size: filter.size,
        category: filter.category || undefined,
        severity: filter.severity || undefined,
        readStatus: filter.view === 'ALL' ? undefined : filter.view
      }),
      getNotificationUnreadCount()
    ])
    rows.value = pageResult?.data?.records || []
    total.value = Number(pageResult?.data?.total || 0)
    unreadTotal.value = Number(unreadResult?.data?.total || 0)
    categoryCounts.value = unreadResult?.data?.categories || {}
  } finally {
    loading.value = false
  }
}

function changeView() {
  filter.current = 1
  loadData()
}

function changeFilter() {
  filter.current = 1
  loadData()
}

async function openNotification(row) {
  if (row.readStatus === 'UNREAD' && canRead.value) {
    await markNotificationRead(row.recipientId)
  }
  if (isSafeRoute(row.routePath)) {
    await router.push(row.routePath)
  } else {
    await loadData()
  }
}

async function markAllRead() {
  if (!canRead.value || !unreadTotal.value) return
  markingAll.value = true
  try {
    await markAllNotificationsRead(filter.category || undefined)
    ElMessage.success('已标记为已读')
    await loadData()
  } finally {
    markingAll.value = false
  }
}

async function acknowledge(row) {
  await acknowledgeNotification(row.recipientId)
  ElMessage.success('已确认收到')
  await loadData()
}

function isSafeRoute(path) {
  return typeof path === 'string' && path.startsWith('/') && !path.startsWith('//')
}

function categoryLabel(value) {
  return categoryOptions.find((item) => item.value === value)?.label || value || '业务'
}

function severityMeta(value) {
  return {
    INFO: { label: '普通', type: 'info' },
    WARNING: { label: '提醒', type: 'warning' },
    HIGH: { label: '重要', type: 'danger' },
    CRITICAL: { label: '紧急', type: 'danger' }
  }[value] || { label: value || '普通', type: 'info' }
}

function formatTime(value) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '--'
}

onMounted(loadData)
</script>

<template>
  <div class="notification-center-page">
    <header class="notification-page-header">
      <div class="notification-page-title">
        <span class="notification-page-icon"><AppIcon name="bell" :size="21" /></span>
        <div>
          <h1>消息中心</h1>
          <span>{{ unreadTotal }} 条未读</span>
        </div>
      </div>
      <div class="notification-page-actions">
        <el-button circle aria-label="刷新消息" :loading="loading" @click="loadData">
          <AppIcon name="refresh" :size="16" />
        </el-button>
        <el-button v-if="canRead" :loading="markingAll" :disabled="!unreadTotal" @click="markAllRead">
          <AppIcon name="check" :size="15" />
          全部已读
        </el-button>
      </div>
    </header>

    <section class="notification-toolbar">
      <el-tabs v-model="filter.view" class="notification-tabs" @tab-change="changeView">
        <el-tab-pane name="UNREAD" :label="`未读 ${unreadTotal}`" />
        <el-tab-pane name="READ" label="已读" />
        <el-tab-pane name="ALL" label="全部" />
      </el-tabs>
      <div class="notification-filters">
        <el-select v-model="filter.category" aria-label="通知分类" @change="changeFilter">
          <el-option
            v-for="item in categoryOptions"
            :key="item.value || 'all'"
            :label="item.value ? `${item.label} ${categoryCounts[item.value] || 0}` : item.label"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="filter.severity" aria-label="通知级别" @change="changeFilter">
          <el-option v-for="item in severityOptions" :key="item.value || 'all'" :label="item.label" :value="item.value" />
        </el-select>
      </div>
    </section>

    <section v-loading="loading" class="notification-list" element-loading-text="正在加载消息">
      <div
        v-for="row in rows"
        :key="row.recipientId"
        class="notification-row"
        :class="{ unread: row.readStatus === 'UNREAD' }"
        role="button"
        tabindex="0"
        @click="openNotification(row)"
        @keydown.enter.prevent="openNotification(row)"
        @keydown.space.prevent="openNotification(row)"
      >
        <span class="notification-status-dot" />
        <span class="notification-row-main">
          <span class="notification-row-title">
            <strong>{{ row.title }}</strong>
            <el-tag size="small" effect="plain">{{ categoryLabel(row.category) }}</el-tag>
            <el-tag size="small" effect="plain" :type="severityMeta(row.severity).type">{{ severityMeta(row.severity).label }}</el-tag>
          </span>
          <span class="notification-summary">{{ row.summary || row.bizNo || '业务状态已更新' }}</span>
          <span class="notification-meta">
            <span>{{ row.bizNo || row.bizType }}</span>
            <span>{{ formatTime(row.occurredTime) }}</span>
            <span v-if="row.readStatus === 'UNREAD'">未读</span>
          </span>
        </span>
        <span class="notification-row-actions">
          <el-button
            v-if="canAcknowledge && row.ackStatus === 'PENDING'"
            size="small"
            @click.stop="acknowledge(row)"
          >确认收到</el-button>
          <AppIcon name="right" :size="16" />
        </span>
      </div>
      <el-empty v-if="!loading && !rows.length" description="暂无符合条件的消息" :image-size="70" />
    </section>

    <footer v-if="total" class="notification-pagination">
      <span>共 {{ total }} 条</span>
      <el-pagination
        v-model:current-page="filter.current"
        :page-size="filter.size"
        :page-count="pageCount"
        layout="prev, pager, next"
        @current-change="loadData"
      />
    </footer>
  </div>
</template>

<style scoped>
.notification-center-page{min-width:0}.notification-page-header,.notification-page-title,.notification-page-actions,.notification-toolbar,.notification-filters,.notification-row-title,.notification-meta,.notification-row-actions,.notification-pagination{display:flex;align-items:center}.notification-page-header{justify-content:space-between;gap:16px;margin-bottom:14px}.notification-page-title{gap:11px}.notification-page-title h1{margin:0;font-size:20px;letter-spacing:0}.notification-page-title span{color:var(--mes-text-muted);font-size:11px}.notification-page-icon{display:inline-flex;width:42px;height:42px;align-items:center;justify-content:center;color:var(--mes-primary);border:1px solid var(--mes-primary-border);border-radius:6px;background:var(--mes-primary-soft)}.notification-page-actions{gap:8px}.notification-page-actions :deep(.el-button>span){display:inline-flex;align-items:center;gap:5px}.notification-toolbar{min-height:52px;justify-content:space-between;gap:16px;padding:0 14px;border:1px solid var(--mes-border);border-bottom:0;background:var(--mes-surface)}.notification-tabs{align-self:stretch}.notification-tabs :deep(.el-tabs__header){height:100%;margin:0}.notification-tabs :deep(.el-tabs__nav-wrap),.notification-tabs :deep(.el-tabs__nav-scroll),.notification-tabs :deep(.el-tabs__nav){height:100%}.notification-tabs :deep(.el-tabs__item){height:100%;font-size:12px}.notification-filters{gap:8px}.notification-filters :deep(.el-select){width:132px}.notification-list{min-height:320px;border:1px solid var(--mes-border);background:var(--mes-surface)}.notification-row{display:grid;width:100%;min-height:92px;grid-template-columns:10px minmax(0,1fr) auto;align-items:center;gap:12px;padding:13px 16px;color:var(--mes-text);border:0;border-bottom:1px solid var(--mes-border);background:transparent;font:inherit;text-align:left;cursor:pointer}.notification-row:hover{background:var(--mes-surface-muted)}.notification-row.unread{background:rgb(var(--mes-primary-rgb)/4%)}.notification-status-dot{width:7px;height:7px;border-radius:50%;background:var(--mes-border-strong)}.notification-row.unread .notification-status-dot{background:var(--mes-primary)}.notification-row-main{display:flex;min-width:0;flex-direction:column;gap:7px}.notification-row-title{min-width:0;gap:6px}.notification-row-title strong{overflow:hidden;margin-right:4px;font-size:13px;text-overflow:ellipsis;white-space:nowrap}.notification-summary{overflow:hidden;color:var(--mes-text-secondary);font-size:12px;text-overflow:ellipsis;white-space:nowrap}.notification-meta{gap:14px;color:var(--mes-text-muted);font-size:10px}.notification-row-actions{gap:10px;color:var(--mes-text-muted)}.notification-pagination{justify-content:space-between;padding:11px 14px;border:1px solid var(--mes-border);border-top:0;background:var(--mes-surface)}.notification-pagination>span{color:var(--mes-text-muted);font-size:11px}@media(max-width:700px){.notification-page-header,.notification-toolbar{align-items:flex-start;flex-direction:column}.notification-page-actions,.notification-filters{width:100%}.notification-toolbar{padding-top:10px}.notification-tabs{width:100%}.notification-filters :deep(.el-select){min-width:0;flex:1}.notification-row{grid-template-columns:8px minmax(0,1fr);padding:12px}.notification-row-actions{grid-column:2;justify-content:flex-end}.notification-row-title{align-items:flex-start;flex-wrap:wrap}.notification-row-title strong{width:100%}.notification-summary{white-space:normal}.notification-meta{align-items:flex-start;flex-direction:column;gap:3px}}
</style>
