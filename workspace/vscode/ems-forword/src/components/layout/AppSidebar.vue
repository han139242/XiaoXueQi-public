<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/common/AppIcon.vue'
import { getNavigationGroups, getNavigationItems } from '@/config/navigation'
import { openAiAssistant } from '@/utils/app-events'

// collapsed控制桌面端折叠；mobile表示当前使用移动端抽屉布局。
const props = defineProps({
  collapsed: { type: Boolean, default: false },
  mobile: { type: Boolean, default: false }
})

// 点击菜单后通知父布局关闭移动端抽屉。
const emit = defineEmits(['navigate'])
const route = useRoute()
const router = useRouter()
// navigation.js已按当前用户权限过滤菜单，这里分别取得扁平搜索列表和分组展示列表。
const navigationItems = computed(() => getNavigationItems())
// 分组菜单用于正常侧边栏展示。
const navigationGroups = computed(() => getNavigationGroups())
// 当前路由路径交给Element Plus高亮正在访问的菜单项。
const activePath = computed(() => route.path)
// 移动端始终显示完整抽屉菜单，只有桌面端collapsed才折叠为图标栏。
const menuCollapsed = computed(() => props.collapsed && !props.mobile)
const searchKeyword = ref('')
// 菜单搜索同时匹配标题和业务说明，computed会随输入内容自动更新。
const searchResults = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return []
  return navigationItems.value.filter((item) => [item.label, item.description]
    .some((value) => String(value || '').toLowerCase().includes(keyword)))
})

// 为Element Plus菜单生成唯一index：路由项使用path，动作项使用action前缀。
function itemIndex(item) {
  // Element Plus菜单需要唯一index；普通菜单用路径，AI按钮用action前缀区分。
  return item.path || `action:${item.action}`
}

// 处理菜单选择：普通项跳转路由，AI项广播打开助手事件，最后通知移动端关闭菜单。
function handleSelect(index) {
  // 根据index找回菜单配置，再决定打开AI助手还是执行路由跳转。
  const item = navigationItems.value.find((candidate) => itemIndex(candidate) === index)
  if (!item) return

  if (item.action === 'ai') {
    openAiAssistant()
  } else if (item.path) {
    router.push(item.path)
  }
  searchKeyword.value = ''
  emit('navigate')
}

// 在菜单搜索框按Enter时直接打开第一条匹配结果。
function openFirstSearchResult() {
  const first = searchResults.value[0]
  if (first) handleSelect(itemIndex(first))
}
</script>

<template>
  <!-- 搜索时显示扁平结果；正常状态按navigationGroups渲染多级Element Plus菜单。 -->
  <aside class="app-sidebar" :class="{ 'is-collapsed': menuCollapsed }">
    <div class="sidebar-brand">
      <span class="sidebar-logo" aria-hidden="true">
        <span class="logo-feather" />
        <span class="logo-base" />
      </span>
      <div v-show="!menuCollapsed" class="brand-copy">
        <strong>羽球智造</strong>
        <span>MES CONTROL</span>
      </div>
    </div>

    <div v-if="!menuCollapsed" class="sidebar-search">
      <el-input v-model="searchKeyword" clearable placeholder="搜索菜单" @keyup.enter="openFirstSearchResult">
        <template #prefix><AppIcon name="search" :size="15" /></template>
      </el-input>
    </div>

    <el-scrollbar class="sidebar-scroll">
      <div v-if="searchKeyword && !menuCollapsed" class="sidebar-search-results">
        <button v-for="item in searchResults" :key="itemIndex(item)" type="button" @click="handleSelect(itemIndex(item))">
          <span><AppIcon :name="item.icon || 'database'" :size="17" /></span>
          <div><strong>{{ item.label }}</strong><small>{{ item.description || '打开业务页面' }}</small></div>
          <AppIcon name="right" :size="14" />
        </button>
        <p v-if="!searchResults.length">没有匹配的菜单</p>
      </div>
      <el-menu
        v-else
        :default-active="activePath"
        :collapse="menuCollapsed"
        :collapse-transition="false"
        unique-opened
        @select="handleSelect"
      >
        <template v-for="group in navigationGroups" :key="group.label">
          <div v-if="!menuCollapsed" class="menu-group-label">{{ group.label }}</div>
          <template v-for="item in group.items" :key="item.key || item.path || item.action">
            <el-sub-menu v-if="item.children?.length" :index="`group:${item.key}`">
              <template #title>
                <AppIcon :name="item.icon" :size="18" />
                <span>{{ item.label }}</span>
              </template>
              <el-menu-item v-for="child in item.children" :key="child.path" :index="itemIndex(child)">
                <span class="submenu-marker" />
                <template #title>{{ child.label }}</template>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item
              v-else
              :index="itemIndex(item)"
              :class="{ 'is-featured': item.featured }"
            >
              <AppIcon :name="item.icon" :size="18" />
              <template #title>{{ item.label }}</template>
            </el-menu-item>
          </template>
        </template>
      </el-menu>
    </el-scrollbar>

    <div class="sidebar-footer" :class="{ compact: menuCollapsed }">
      <span class="service-dot" />
      <div v-if="!menuCollapsed">
        <strong>MES 服务</strong>
        <span>管理端 v1.0</span>
      </div>
    </div>
  </aside>
</template>

<style scoped>
/* 侧边栏样式：控制品牌区、搜索、菜单层级、折叠状态和移动端展示。 */
.app-sidebar {
  display: flex;
  width: 100%;
  height: 100%;
  flex-direction: column;
  color: var(--mes-sidebar-text);
  border-right: 1px solid var(--mes-sidebar-border);
  background: var(--mes-sidebar);
  box-shadow: 2px 0 12px rgb(32 42 54 / 5%);
}

.sidebar-brand {
  display: flex;
  height: var(--mes-header-height);
  flex: none;
  align-items: center;
  gap: 12px;
  padding: 0 19px;
  border-bottom: 1px solid var(--mes-header-border);
  background: var(--mes-brand-bg);
}

.is-collapsed .sidebar-brand {
  justify-content: center;
  padding: 0;
}

.sidebar-search {
  flex: none;
  padding: 10px 12px;
  border-bottom: 1px solid var(--mes-sidebar-border);
  background: var(--mes-sidebar);
}

.sidebar-search :deep(.el-input__wrapper) {
  min-height: 34px;
  border-radius: 4px;
  box-shadow: 0 0 0 1px var(--mes-sidebar-border) inset;
}

.sidebar-search-results {
  display: flex;
  flex-direction: column;
  padding: 8px 0;
}

.sidebar-search-results button {
  display: grid;
  width: 100%;
  min-height: 54px;
  grid-template-columns: 30px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  padding: 7px 14px;
  color: var(--mes-sidebar-text);
  border: 0;
  border-left: 3px solid transparent;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.sidebar-search-results button:hover {
  color: var(--mes-sidebar-active-text);
  border-left-color: var(--mes-primary);
  background: var(--mes-sidebar-active);
}

.sidebar-search-results button>span {
  display: inline-flex;
  width: 30px;
  height: 30px;
  align-items: center;
  justify-content: center;
  color: var(--mes-primary);
  border: 1px solid var(--mes-primary-border);
  border-radius: 4px;
  background: var(--mes-primary-soft);
}

.sidebar-search-results button>div {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.sidebar-search-results strong,
.sidebar-search-results small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-search-results strong { font-size: 12px; }
.sidebar-search-results small { margin-top: 2px; color: var(--mes-sidebar-muted); font-size: 9px; }
.sidebar-search-results p { padding: 24px 16px; color: var(--mes-sidebar-muted); font-size: 11px; text-align: center; }

.sidebar-logo {
  position: relative;
  display: inline-flex;
  width: 36px;
  height: 36px;
  flex: none;
  align-items: center;
  justify-content: center;
  color: #eff8ff;
  border: 1px solid rgb(255 255 255 / 58%);
  border-radius: 3px;
  background: rgb(255 255 255 / 16%);
}

.logo-feather {
  position: absolute;
  top: 7px;
  width: 10px;
  height: 14px;
  border: 2px solid currentColor;
  border-bottom: 0;
  border-radius: 50% 50% 35% 35%;
  transform: rotate(-10deg);
}

.logo-base {
  position: absolute;
  bottom: 7px;
  width: 13px;
  height: 5px;
  border: 2px solid currentColor;
  border-radius: 50%;
}

.brand-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.brand-copy strong {
  overflow: hidden;
  color: #f4f7fa;
  font-size: 17px;
  font-weight: 680;
  line-height: 1.25;
  white-space: nowrap;
}

.brand-copy span {
  margin-top: 2px;
  color: rgb(255 255 255 / 72%);
  font-size: 10px;
  font-weight: 700;
}

.sidebar-scroll {
  flex: 1;
  background: var(--mes-sidebar);
}

:deep(.el-menu) {
  border-right: 0;
  background: transparent;
}

:deep(.el-menu-item) {
  height: 42px;
  margin: 0;
  padding: 0 18px !important;
  gap: 12px;
  color: var(--mes-sidebar-text);
  border-left: 3px solid transparent;
  border-radius: 0;
  font-size: 13px;
  line-height: 42px;
}

:deep(.el-sub-menu__title) {
  height: 42px;
  margin: 0;
  padding: 0 18px !important;
  gap: 12px;
  color: var(--mes-sidebar-text);
  border-left: 3px solid transparent;
  border-radius: 0;
  font-size: 13px;
  line-height: 42px;
}

:deep(.el-sub-menu__title:hover) {
  color: var(--mes-sidebar-active-text);
  background: var(--mes-sidebar-raised);
}

:deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: var(--mes-sidebar-active-text);
  border-left-color: var(--mes-primary);
  background: var(--mes-sidebar-active);
  font-weight: 650;
}

:deep(.el-sub-menu .el-menu-item) {
  height: 40px;
  margin: 0;
  padding-left: 38px !important;
  font-size: 12px;
  line-height: 40px;
}

.submenu-marker {
  width: 5px;
  height: 5px;
  flex: none;
  border-radius: 50%;
  background: var(--mes-sidebar-muted);
}

:deep(.el-sub-menu .el-menu-item.is-active) .submenu-marker {
  background: var(--mes-primary);
}

:deep(.el-menu-item:hover) {
  color: var(--mes-sidebar-active-text);
  background: var(--mes-sidebar-raised);
}

:deep(.el-menu-item.is-active) {
  color: var(--mes-sidebar-active-text);
  border-left-color: var(--mes-primary);
  background: var(--mes-sidebar-active);
  box-shadow: none;
  font-weight: 650;
}

:deep(.el-menu-item.is-featured) {
  color: var(--mes-sidebar-active-text);
  border-left-color: var(--mes-primary);
  background: var(--mes-sidebar-featured);
}

:deep(.el-menu-item.is-featured:hover),
:deep(.el-menu-item.is-featured.is-active) {
  color: var(--mes-sidebar-active-text);
  background: var(--mes-sidebar-active);
}

:deep(.el-menu--collapse .el-menu-item) {
  justify-content: center;
  padding: 0 !important;
}

:deep(.el-menu--collapse .el-sub-menu__title) {
  justify-content: center;
  padding: 0 !important;
}

.menu-group-label {
  padding: 16px 21px 7px;
  color: var(--mes-sidebar-group);
  font-size: 11px;
  font-weight: 700;
}

.sidebar-footer {
  display: flex;
  min-height: 60px;
  flex: none;
  align-items: center;
  gap: 10px;
  padding: 12px 19px;
  border-top: 1px solid var(--mes-sidebar-border);
}

.sidebar-footer.compact {
  justify-content: center;
  padding: 0;
}

.sidebar-footer > div {
  display: flex;
  flex-direction: column;
}

.sidebar-footer strong {
  color: var(--mes-sidebar-text);
  font-size: 11px;
  font-weight: 600;
}

.sidebar-footer span:not(.service-dot) {
  color: var(--mes-sidebar-muted);
  font-size: 10px;
}

.service-dot {
  width: 8px;
  height: 8px;
  flex: none;
  border: 2px solid rgb(88 190 139 / 30%);
  border-radius: 50%;
  background: var(--mes-success);
  box-shadow: 0 0 0 3px rgb(43 138 98 / 12%);
}
</style>
