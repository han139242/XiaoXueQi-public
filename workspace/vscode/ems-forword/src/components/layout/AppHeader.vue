<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import AppIcon from '@/components/common/AppIcon.vue'
import ThemeDrawer from '@/components/layout/ThemeDrawer.vue'
import { clearAuth, getCurrentUser, isFrameworkPreview } from '@/utils/auth'
import { getModuleDefinition, getNavigationItems } from '@/config/navigation'
import { openAiAssistant } from '@/utils/app-events'
import { activeTheme } from '@/utils/theme'
import { getCurrentUserInfo, logout } from '@/api/auth'
import { setCurrentUser } from '@/utils/auth'

defineProps({
  mobile: { type: Boolean, default: false }
})

const emit = defineEmits(['toggle'])
const route = useRoute()
const router = useRouter()
const searchOpen = ref(false)
const searchKeyword = ref('')
const themeOpen = ref(false)
const now = ref(dayjs())
const currentUser = ref(getCurrentUser())
let timer

const displayName = computed(() => currentUser.value.realName || currentUser.value.nickname || currentUser.value.username || 'MES 用户')
const displayInitial = computed(() => displayName.value.slice(0, 1).toUpperCase())
const dateText = computed(() => now.value.format('YYYY.MM.DD'))
const timeText = computed(() => now.value.format('HH:mm'))
const previewMode = isFrameworkPreview()

const breadcrumbTitle = computed(() => {
  if (route.name === 'ModuleHub') {
    return getModuleDefinition(route.params.moduleKey).title
  }
  return route.meta.title || '首页'
})

const searchResults = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  const items = getNavigationItems()
  if (!keyword) {
    return items.slice(0, 7)
  }
  return items.filter((item) => item.label.toLowerCase().includes(keyword)).slice(0, 8)
})

function openSearch() {
  searchOpen.value = true
}

async function navigateTo(item) {
  searchOpen.value = false
  searchKeyword.value = ''
  if (item.action === 'ai') {
    openAiAssistant()
  } else if (item.path) {
    await router.push(item.path)
  }
}

async function toggleFullscreen() {
  if (!document.fullscreenElement) {
    await document.documentElement.requestFullscreen?.()
  } else {
    await document.exitFullscreen?.()
  }
}

async function handleCommand(command) {
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

function handleShortcut(event) {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    openSearch()
  }
}

onMounted(async () => {
  if (!previewMode) {
    try {
      const result = await getCurrentUserInfo()
      currentUser.value = result.data || {}
      setCurrentUser(currentUser.value)
    } catch {
      // 请求层负责处理失效登录；其他错误下继续使用本地用户信息。
    }
  }
  timer = window.setInterval(() => {
    now.value = dayjs()
  }, 30000)
  window.addEventListener('keydown', handleShortcut)
})

onBeforeUnmount(() => {
  window.clearInterval(timer)
  window.removeEventListener('keydown', handleShortcut)
})
</script>

<template>
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

      <el-popover placement="bottom" :width="280" trigger="click" popper-class="notification-popover">
        <template #reference>
          <el-button class="header-icon-button hide-mobile" text aria-label="通知">
            <AppIcon name="bell" :size="18" />
          </el-button>
        </template>
        <div class="notification-head">
          <strong>消息通知</strong>
          <span>0 条未读</span>
        </div>
        <div class="notification-empty">暂无待处理通知</div>
      </el-popover>

      <el-tooltip content="AI 智能助手" placement="bottom">
        <el-button class="header-icon-button" text aria-label="AI 智能助手" @click="openAiAssistant">
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

.notification-empty {
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
