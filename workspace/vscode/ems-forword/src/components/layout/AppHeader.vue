<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElNotification } from 'element-plus'
import AppIcon from '@/components/common/AppIcon.vue'
import ThemeDrawer from '@/components/layout/ThemeDrawer.vue'
import { clearAuth, getCurrentUser, isFrameworkPreview } from '@/utils/auth'
import { getModuleDefinition, getNavigationItems } from '@/config/navigation'
import { openAiAssistant } from '@/utils/app-events'
import { activeTheme } from '@/utils/theme'
import { getCurrentUserInfo, logout } from '@/api/auth'
import {
  getNotificationPage,
  getNotificationUnreadCount,
  markNotificationRead
} from '@/api/sys/notification'
import { setCurrentUser } from '@/utils/auth'
import { hasPermission } from '@/utils/permission'

// 顶部栏负责菜单开关、全局搜索、通知、全屏、主题和用户退出。
defineProps({
  mobile: { type: Boolean, default: false }
})

// toggle事件通知父布局切换桌面侧栏或移动端菜单。
const emit = defineEmits(['toggle'])
const route = useRoute()
const router = useRouter()
const searchOpen = ref(false)
const searchKeyword = ref('')
const themeOpen = ref(false)
const notificationOpen = ref(false)
const now = ref(dayjs())
const currentUser = ref(getCurrentUser())
const notificationSummary = ref({ total: 0, categories: {} })
const recentNotifications = ref([])
const notificationLoading = ref(false)
// 定时器和Element Plus通知实例不参与模板渲染，因此使用普通变量保存。
let clockTimer
let notificationTimer
let urgentNotification

const NOTIFICATION_INTERVAL = 60_000

// 用户显示名称按真实姓名、昵称、用户名依次降级；computed会在用户信息更新后自动重算。
const displayName = computed(() => currentUser.value.realName || currentUser.value.nickname || currentUser.value.username || 'MES 用户')
// 用户头像没有图片时显示名称首字母。
const displayInitial = computed(() => displayName.value.slice(0, 1).toUpperCase())
// 顶部栏日期和时间都从每30秒更新一次的now派生。
const dateText = computed(() => now.value.format('YYYY.MM.DD'))
// 格式化顶部栏当前时分。
const timeText = computed(() => now.value.format('HH:mm'))
const previewMode = isFrameworkPreview()
// 三个computed分别控制AI入口、通知入口和标记已读操作的权限。
const canUseAi = computed(() => hasPermission('ai:assistant:use'))
// 判断当前账号是否能看到通知入口和通知列表。
const canViewNotifications = computed(() => hasPermission('sys:notification:view'))
// 判断当前账号是否能把通知标记为已读。
const canReadNotifications = computed(() => hasPermission('sys:notification:read'))
// 将通知统计中的total规范化为数字，供角标显示。
const unreadNotificationCount = computed(() => Number(notificationSummary.value.total || 0))

// 面包屑标题通常来自路由meta，动态模块首页则从navigation配置中取标题。
const breadcrumbTitle = computed(() => {
  if (route.name === 'ModuleHub') {
    return getModuleDefinition(route.params.moduleKey).title
  }
  return route.meta.title || '首页'
})

// 根据关键词从当前用户可见菜单中筛选顶部全局搜索结果。
const searchResults = computed(() => {
  // 未输入关键词时展示常用入口，输入后在当前账号可见菜单中搜索。
  const keyword = searchKeyword.value.trim().toLowerCase()
  const items = getNavigationItems()
  if (!keyword) {
    return items.slice(0, 7)
  }
  return items.filter((item) => item.label.toLowerCase().includes(keyword)).slice(0, 8)
})

// 打开全局搜索对话框。
function openSearch() {
  searchOpen.value = true
}

// 点击搜索结果后关闭搜索，并根据入口类型执行路由跳转或打开AI助手。
async function navigateTo(item) {
  searchOpen.value = false
  searchKeyword.value = ''
  if (item.action === 'ai') {
    openAiAssistant()
  } else if (item.path) {
    await router.push(item.path)
  }
}

// 在浏览器普通模式和全屏模式之间切换。
async function toggleFullscreen() {
  // Fullscreen API是浏览器原生能力；可选链兼容不支持该API的环境。
  if (!document.fullscreenElement) {
    await document.documentElement.requestFullscreen?.()
  } else {
    await document.exitFullscreen?.()
  }
}

// 关闭当前Element Plus紧急通知实例，并清空引用。
function closeUrgentNotification() {
  urgentNotification?.close()
  urgentNotification = undefined
}

// 打开一条通知：必要时标记已读、安全跳转业务页，然后刷新未读统计。
async function openNotification(item) {
  // 打开通知时先按权限标记已读，只允许跳转到经过校验的站内路由。
  notificationOpen.value = false
  closeUrgentNotification()
  if (item?.readStatus === 'UNREAD' && canReadNotifications.value) {
    await markNotificationRead(item.recipientId)
  }
  rememberUrgentNotification(item?.recipientId)
  if (isSafeNotificationRoute(item?.routePath)) {
    await router.push(item.routePath)
  }
  await loadNotifications()
}

// 关闭通知下拉并进入完整消息中心页面。
async function openNotificationCenter() {
  notificationOpen.value = false
  closeUrgentNotification()
  await router.push('/notifications')
}

// 判断通知携带的地址是否为安全站内路由。
function isSafeNotificationRoute(path) {
  // 禁止//开头的协议相对URL，避免通知数据把用户带到站外地址。
  return typeof path === 'string' && path.startsWith('/') && !path.startsWith('//')
}

// 按当前用户生成紧急通知去重所需的localStorage键。
function urgentStorageKey() {
  const identity = currentUser.value.userId || currentUser.value.username || 'unknown'
  return `mes:urgent-notification:${identity}`
}

// 读取该用户上一次已经弹出过的紧急通知ID。
function storedUrgentNotification() {
  try {
    return window.localStorage.getItem(urgentStorageKey()) || ''
  } catch {
    return ''
  }
}

// 记录已经弹出的通知ID，防止每分钟轮询都重复提醒同一条消息。
function rememberUrgentNotification(recipientId) {
  if (!recipientId) return
  try {
    window.localStorage.setItem(urgentStorageKey(), String(recipientId))
  } catch {
    // 浏览器禁用持久化时只影响跨标签页去重。
  }
}

// 使用Element Plus在右下角显示高优先级或严重通知。
function showUrgentNotification(item) {
  // 高优先级通知使用Element Plus桌面弹窗，严重告警保持显示直到用户处理。
  if (!item || route.path === item.routePath) return
  closeUrgentNotification()
  urgentNotification = ElNotification({
    title: item.title,
    message: item.summary || item.bizNo || '业务状态已更新',
    type: item.severity === 'CRITICAL' ? 'error' : 'warning',
    position: 'bottom-right',
    duration: item.severity === 'CRITICAL' ? 0 : 8000,
    offset: 18,
    onClick: () => openNotification(item)
  })
  rememberUrgentNotification(item.recipientId)
}

// 并行加载未读总数和最近6条通知，可选地触发紧急通知弹窗。
async function loadNotifications({ notify = false } = {}) {
  // 未读总数与最近通知并行加载；notify=true时额外检查是否弹出高优先级提醒。
  if (previewMode || !canViewNotifications.value || notificationLoading.value) {
    return
  }
  notificationLoading.value = true
  try {
    const [countResult, pageResult] = await Promise.all([
      getNotificationUnreadCount(),
      getNotificationPage({ current: 1, size: 6, readStatus: 'UNREAD' })
    ])
    notificationSummary.value = {
      total: Number(countResult?.data?.total || 0),
      categories: countResult?.data?.categories || {}
    }
    recentNotifications.value = pageResult?.data?.records || []
    if (!unreadNotificationCount.value) {
      closeUrgentNotification()
    } else if (notify) {
      const urgent = recentNotifications.value.find((item) => ['HIGH', 'CRITICAL'].includes(item.severity))
      if (urgent && String(urgent.recipientId) !== storedUrgentNotification()) {
        showUrgentNotification(urgent)
      }
    }
  } catch {
    // 请求层统一展示错误，保留上一次成功的未读数量。
  } finally {
    notificationLoading.value = false
  }
}

// 将通知业务分类编码转换成中文名称。
function categoryLabel(value) {
  return { SYSTEM: '系统', MFG: '生产', PLAN: '计划', QMS: '质量', WMS: '仓储', EAM: '设备' }[value] || '业务'
}

// 将通知创建时间格式化为月日和时分。
function notificationTime(value) {
  return value ? dayjs(value).format('MM-DD HH:mm') : '--'
}

// 处理用户菜单命令，目前logout会调用后端退出并无条件清理本地认证状态。
async function handleCommand(command) {
  // 退出时无论后端logout是否成功都清理本地Token，避免用户继续停留在受保护页面。
  if (command === 'logout') {
    try {
      if (!previewMode) await logout()
    } catch {
      // 本地登录态仍需清理，请求层已经展示服务端错误。
    } finally {
      clearAuth()
      await router.replace('/login')
    }
  }
}

// 处理全局Ctrl/Cmd+K快捷键并阻止浏览器默认行为。
function handleShortcut(event) {
  // Ctrl/Cmd + K打开全局菜单搜索。
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    openSearch()
  }
}

// 挂载后刷新用户、首次加载通知，并启动时钟和通知轮询。
onMounted(async () => {
  // 进入后台后刷新当前用户信息，并启动时钟和通知轮询。
  if (!previewMode) {
    try {
      const result = await getCurrentUserInfo()
      currentUser.value = result.data || {}
      setCurrentUser(currentUser.value)
    } catch {
      // 请求层负责处理失效登录；其他错误下继续使用本地用户信息。
    }
  }
  await loadNotifications({ notify: true })
  clockTimer = window.setInterval(() => {
    now.value = dayjs()
  }, 30000)
  if (!previewMode && canViewNotifications.value) {
    notificationTimer = window.setInterval(() => loadNotifications({ notify: true }), NOTIFICATION_INTERVAL)
  }
  window.addEventListener('keydown', handleShortcut)
})

// 卸载前释放定时器、通知实例和键盘事件。
onBeforeUnmount(() => {
  // 布局销毁时清除定时器、通知实例和键盘监听。
  window.clearInterval(clockTimer)
  window.clearInterval(notificationTimer)
  closeUrgentNotification()
  window.removeEventListener('keydown', handleShortcut)
})
</script>

<template>
  <!-- 顶部栏包含菜单开关、面包屑、全局搜索、AI、通知、主题、全屏和用户菜单。 -->
  <header class="app-header">
    <div class="header-left">
      <el-tooltip content="展开或收起导航" placement="bottom">
        <el-button class="header-icon-button" text aria-label="展开或收起导航" @click="emit('toggle')">
          <AppIcon name="menu" :size="20" />
        </el-button>
      </el-tooltip>

      <div class="breadcrumb">
        <span>羽球智造</span>
        <AppIcon name="right" :size="14" />
        <strong>{{ breadcrumbTitle }}</strong>
      </div>
    </div>

    <div class="header-actions">
      <button class="global-search" type="button" @click="openSearch">
        <AppIcon name="search" :size="17" />
        <span>搜索模块</span>
      </button>

      <div class="header-clock">
        <strong>{{ timeText }}</strong>
        <span>{{ dateText }}</span>
      </div>

      <el-tooltip content="全屏" placement="bottom">
        <el-button class="header-icon-button hide-mobile" text aria-label="全屏" @click="toggleFullscreen">
          <AppIcon name="fullscreen" :size="18" />
        </el-button>
      </el-tooltip>

      <el-tooltip :content="`界面主题：${activeTheme.name}`" placement="bottom">
        <el-button class="header-icon-button" text aria-label="界面主题" @click="themeOpen = true">
          <AppIcon name="palette" :size="18" />
        </el-button>
      </el-tooltip>

      <el-popover
        v-if="canViewNotifications"
        v-model:visible="notificationOpen"
        placement="bottom"
        :width="340"
        trigger="click"
        popper-class="notification-popover"
      >
        <template #reference>
          <el-badge
            class="notification-badge"
            :value="unreadNotificationCount"
            :max="99"
            :hidden="unreadNotificationCount === 0"
          >
            <el-button class="header-icon-button" text aria-label="消息中心">
              <AppIcon name="bell" :size="18" />
            </el-button>
          </el-badge>
        </template>
        <div class="notification-head">
          <strong>消息中心</strong>
          <span>{{ unreadNotificationCount ? `${unreadNotificationCount} 条未读` : '暂无未读' }}</span>
        </div>
        <div v-if="notificationLoading" class="notification-empty">正在刷新消息</div>
        <div v-else-if="recentNotifications.length" class="notification-preview-list">
          <button
            v-for="item in recentNotifications"
            :key="item.recipientId"
            type="button"
            class="notification-preview-item"
            @click="openNotification(item)"
          >
            <span class="notification-preview-dot" :class="`is-${String(item.severity || 'INFO').toLowerCase()}`" />
            <span class="notification-preview-copy">
              <strong>{{ item.title }}</strong>
              <span>{{ item.summary || item.bizNo || '业务状态已更新' }}</span>
              <small>{{ categoryLabel(item.category) }} · {{ notificationTime(item.occurredTime) }}</small>
            </span>
            <AppIcon name="right" :size="14" />
          </button>
          <el-button class="notification-center-action" text @click="openNotificationCenter">
            查看全部消息
            <AppIcon name="right" :size="14" />
          </el-button>
        </div>
        <div v-else class="notification-empty">
          <span>暂无未读消息</span>
          <el-button text @click="openNotificationCenter">查看消息中心</el-button>
        </div>
      </el-popover>

      <el-tooltip v-if="canUseAi" content="AI 工作台" placement="bottom">
        <el-button class="header-icon-button" text aria-label="AI 工作台" @click="openAiAssistant">
          <AppIcon name="bot" :size="18" />
        </el-button>
      </el-tooltip>

      <el-dropdown trigger="click" @command="handleCommand">
        <button class="user-entry" type="button">
          <span class="user-avatar">{{ displayInitial }}</span>
          <span class="user-copy">
            <strong>{{ displayName }}</strong>
            <small>{{ previewMode ? '框架预览' : '系统用户' }}</small>
          </span>
          <AppIcon name="chevron" :size="14" />
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">
              <AppIcon name="logout" :size="16" />
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-dialog v-model="searchOpen" class="search-dialog" title="快速导航" width="520px" append-to-body>
      <el-input v-model="searchKeyword" size="large" placeholder="输入模块名称" autofocus>
        <template #prefix><AppIcon name="search" :size="18" /></template>
      </el-input>
      <div class="search-results">
        <button
          v-for="item in searchResults"
          :key="item.path || item.action"
          type="button"
          @click="navigateTo(item)"
        >
          <span class="search-result-icon"><AppIcon :name="item.icon" :size="18" /></span>
          <span>{{ item.label }}</span>
          <AppIcon name="right" :size="15" />
        </button>
        <div v-if="!searchResults.length" class="search-empty">没有匹配的模块</div>
      </div>
    </el-dialog>

    <ThemeDrawer v-model="themeOpen" />
  </header>
</template>

<style scoped>
/* 顶部栏样式：控制桌面/移动布局、搜索弹窗、通知下拉和用户操作区域。 */
.app-header {
  position: sticky;
  z-index: 20;
  top: 0;
  display: flex;
  width: 100%;
  height: var(--mes-header-height);
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 0 20px 0 12px;
  color: var(--mes-header-text);
  border-bottom: 1px solid var(--mes-header-border);
  background: var(--mes-header-bg);
  box-shadow: 0 2px 10px rgb(32 42 54 / 10%);
}

.header-left,
.header-actions,
.breadcrumb,
.user-entry,
.global-search {
  display: flex;
  align-items: center;
}

.header-left {
  min-width: 0;
  gap: 10px;
}

.header-actions {
  flex: none;
  gap: 7px;
}

.header-icon-button.el-button {
  width: 38px;
  height: 38px;
  padding: 0;
  color: var(--mes-header-text);
}

.header-icon-button.el-button:hover {
  color: #fff;
  background: var(--mes-header-hover);
}

.breadcrumb {
  min-width: 0;
  gap: 7px;
  color: var(--mes-header-muted);
  font-size: 13px;
}

.breadcrumb strong {
  overflow: hidden;
  color: var(--mes-header-text);
  font-size: 14px;
  font-weight: 620;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.global-search {
  width: 172px;
  height: 36px;
  gap: 8px;
  padding: 0 12px;
  color: var(--mes-header-control-text);
  border: 1px solid rgb(255 255 255 / 34%);
  border-radius: var(--mes-radius-sm);
  background: var(--mes-header-control-bg);
  cursor: pointer;
}

.global-search:hover {
  color: var(--mes-primary);
  border-color: rgb(255 255 255 / 72%);
  background: var(--mes-surface);
}

.global-search span {
  font-size: 13px;
}

.header-clock {
  display: flex;
  height: 34px;
  min-width: 70px;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  margin: 0 8px;
  line-height: 1.15;
}

.header-clock strong {
  color: var(--mes-header-text);
  font-size: 15px;
  font-variant-numeric: tabular-nums;
}

.header-clock span {
  margin-top: 3px;
  color: var(--mes-header-muted);
  font-size: 10px;
}

.user-entry {
  min-width: 132px;
  height: 44px;
  gap: 9px;
  padding: 3px 5px 3px 8px;
  color: var(--mes-header-text);
  border: 0;
  border-left: 1px solid var(--mes-header-divider);
  background: transparent;
  cursor: pointer;
}

.user-avatar {
  display: inline-flex;
  width: 32px;
  height: 32px;
  flex: none;
  align-items: center;
  justify-content: center;
  color: #fff;
  border: 1px solid rgb(255 255 255 / 66%);
  border-radius: 50%;
  background: rgb(255 255 255 / 15%);
  font-size: 12px;
  font-weight: 700;
}

.user-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.2;
}

.user-copy strong {
  max-width: 78px;
  overflow: hidden;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-copy small {
  margin-top: 3px;
  color: var(--mes-header-muted);
  font-size: 10px;
}

.notification-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--mes-border);
}

.notification-head strong {
  font-size: 13px;
}

.notification-head span,
.notification-empty {
  color: var(--mes-text-muted);
  font-size: 11px;
}

.notification-badge {
  display: inline-flex;
}

.notification-badge :deep(.el-badge__content) {
  top: 7px;
  right: 7px;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  border: 2px solid var(--mes-header-bg);
  background: var(--mes-danger);
  font-size: 9px;
  line-height: 13px;
}

.notification-preview-list {
  padding-top: 8px;
}

.notification-preview-item {
  display: grid;
  width: 100%;
  min-height: 68px;
  grid-template-columns: 8px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 9px 4px;
  color: var(--mes-text-secondary);
  border: 0;
  border-bottom: 1px solid var(--el-border-color-light);
  background: transparent;
  font: inherit;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
}

.notification-preview-item:hover {
  background: var(--mes-surface-muted);
}

.notification-preview-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--mes-info);
}

.notification-preview-dot.is-warning {
  background: var(--mes-warning);
}

.notification-preview-dot.is-high,
.notification-preview-dot.is-critical {
  background: var(--mes-danger);
}

.notification-preview-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.notification-preview-copy strong,
.notification-preview-copy span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-preview-copy strong {
  color: var(--mes-text);
  font-size: 12px;
}

.notification-preview-copy span {
  color: var(--mes-text-secondary);
  font-size: 11px;
}

.notification-preview-copy small {
  color: var(--mes-text-muted);
  font-size: 9px;
}

.notification-center-action.el-button {
  width: 100%;
  height: 36px;
  gap: 5px;
  margin-top: 6px;
  border-radius: var(--mes-radius-sm);
}

.notification-empty {
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 4px;
  padding: 34px 0 24px;
  text-align: center;
}

.search-results {
  display: flex;
  min-height: 150px;
  flex-direction: column;
  gap: 4px;
  margin-top: 14px;
}

.search-results button {
  display: grid;
  min-height: 46px;
  grid-template-columns: 34px 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 5px 10px;
  color: var(--mes-text);
  border: 0;
  border-radius: var(--mes-radius-sm);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.search-results button:hover {
  color: var(--mes-primary);
  background: var(--mes-primary-soft);
}

.search-result-icon {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--mes-border);
  border-radius: var(--mes-radius-sm);
  background: #fff;
}

.search-empty {
  padding: 52px 0;
  color: var(--mes-text-muted);
  text-align: center;
}

@media (max-width: 1180px) {
  .global-search,
  .header-clock,
  .user-copy,
  .user-entry > .app-icon {
    display: none;
  }

  .user-entry {
    min-width: 40px;
    padding: 3px 0 3px 8px;
  }

  .app-header {
    padding-right: 10px;
  }
}

@media (max-width: 720px) {
  .hide-mobile {
    display: none;
  }
}

@media (max-width: 520px) {
  .breadcrumb > span,
  .breadcrumb > .app-icon {
    display: none;
  }
}
</style>
