<script setup>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/common/AppIcon.vue'
import { getModuleDefinition } from '@/config/navigation'

const emit = defineEmits(['refresh'])
const route = useRoute()
const router = useRouter()

const tabs = defineModel({
  type: Array,
  default: () => []
})

const activePath = computed(() => route.fullPath)

function routeTitle(currentRoute) {
  if (currentRoute.name === 'ModuleHub') {
    return getModuleDefinition(currentRoute.params.moduleKey).title
  }
  return currentRoute.meta.title || '未命名页面'
}

function addCurrentRoute(currentRoute) {
  if (!currentRoute.name || currentRoute.meta.public) {
    return
  }
  const exists = tabs.value.some((tab) => tab.path === currentRoute.fullPath)
  if (!exists) {
    tabs.value.push({
      path: currentRoute.fullPath,
      title: routeTitle(currentRoute),
      affix: Boolean(currentRoute.meta.affix)
    })
  } else {
    const target = tabs.value.find((tab) => tab.path === currentRoute.fullPath)
    target.title = routeTitle(currentRoute)
  }
}

function closeTab(tab) {
  if (tab.affix) {
    return
  }
  const index = tabs.value.findIndex((item) => item.path === tab.path)
  tabs.value.splice(index, 1)
  if (activePath.value === tab.path) {
    const next = tabs.value[index] || tabs.value[index - 1] || { path: '/dashboard' }
    router.push(next.path)
  }
}

function closeOthers() {
  tabs.value = tabs.value.filter((tab) => tab.affix || tab.path === activePath.value)
}

watch(() => route.fullPath, () => addCurrentRoute(route), { immediate: true })
</script>

<template>
  <div class="app-tabs">
    <div class="tabs-scroll">
      <button
        v-for="tab in tabs"
        :key="tab.path"
        type="button"
        class="tab-item"
        :class="{ active: tab.path === activePath }"
        @click="router.push(tab.path)"
      >
        <span class="tab-title">{{ tab.title }}</span>
        <span
          v-if="!tab.affix"
          class="tab-close"
          role="button"
          tabindex="0"
          aria-label="关闭标签页"
          @click.stop="closeTab(tab)"
          @keydown.enter.stop="closeTab(tab)"
        >
          <AppIcon name="close" :size="12" />
        </span>
      </button>
    </div>

    <div class="tab-actions">
      <el-tooltip content="刷新当前页" placement="bottom">
        <el-button text aria-label="刷新当前页" @click="emit('refresh')">
          <AppIcon name="refresh" :size="15" />
        </el-button>
      </el-tooltip>
      <el-dropdown trigger="click">
        <el-button text aria-label="标签页菜单"><AppIcon name="more" :size="16" /></el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="closeOthers">关闭其他标签页</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style scoped>
.app-tabs {
  display: flex;
  width: 100%;
  height: var(--mes-tabs-height);
  align-items: center;
  border-bottom: 1px solid var(--mes-border);
  background: var(--mes-tabs-bg);
}

.tabs-scroll {
  display: flex;
  min-width: 0;
  flex: 1;
  align-items: center;
  gap: 2px;
  overflow-x: auto;
  padding: 0 10px;
  scrollbar-width: none;
}

.tabs-scroll::-webkit-scrollbar {
  display: none;
}

.tab-item {
  position: relative;
  display: inline-flex;
  height: 36px;
  min-width: 82px;
  max-width: 174px;
  flex: none;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 0;
  padding: 0 12px;
  color: var(--mes-text-secondary);
  border: 0;
  border-right: 1px solid var(--mes-border);
  border-radius: 0;
  background: transparent;
  font-size: 12px;
  cursor: pointer;
}

.tab-item:hover {
  color: var(--mes-text);
  background: rgb(255 255 255 / 62%);
}

.tab-item.active {
  z-index: 1;
  color: var(--mes-primary);
  background: var(--mes-tab-surface);
  box-shadow: inset 0 -2px 0 var(--mes-primary);
  font-weight: 620;
}

.tab-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tab-close {
  display: inline-flex;
  width: 18px;
  height: 18px;
  flex: none;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: var(--mes-text-muted);
}

.tab-close:hover {
  color: var(--mes-danger);
  background: var(--mes-danger-soft);
}

.tab-actions {
  display: flex;
  flex: none;
  align-items: center;
  padding: 0 7px;
  border-left: 1px solid var(--mes-border);
  background: transparent;
}

.tab-actions :deep(.el-button) {
  width: 30px;
  height: 30px;
  margin: 0;
  padding: 0;
  color: var(--mes-text-muted);
}

@media (max-width: 640px) {
  .tab-item {
    min-width: 94px;
  }

  .tab-actions {
    display: none;
  }
}
</style>
