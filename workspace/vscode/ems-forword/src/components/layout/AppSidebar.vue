<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/common/AppIcon.vue'
import { getNavigationItems, navigationGroups } from '@/config/navigation'
import { openAiAssistant } from '@/utils/app-events'

const props = defineProps({
  collapsed: { type: Boolean, default: false },
  mobile: { type: Boolean, default: false }
})

const emit = defineEmits(['navigate'])
const route = useRoute()
const router = useRouter()
const navigationItems = getNavigationItems()
const activePath = computed(() => route.path)
const menuCollapsed = computed(() => props.collapsed && !props.mobile)

function itemIndex(item) {
  return item.path || `action:${item.action}`
}

function handleSelect(index) {
  const item = navigationItems.find((candidate) => itemIndex(candidate) === index)
  if (!item) return

  if (item.action === 'ai') {
    openAiAssistant()
  } else if (item.path) {
    router.push(item.path)
  }
  emit('navigate')
}
</script>

<template>
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

    <el-scrollbar class="sidebar-scroll">
      <el-menu
        :default-active="activePath"
        :collapse="menuCollapsed"
        :collapse-transition="false"
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
.app-sidebar {
  display: flex;
  width: 100%;
  height: 100%;
  flex-direction: column;
  color: var(--mes-sidebar-text);
  background: var(--mes-sidebar);
}

.sidebar-brand {
  display: flex;
  height: var(--mes-header-height);
  flex: none;
  align-items: center;
  gap: 11px;
  padding: 0 18px;
  border-bottom: 1px solid rgb(255 255 255 / 7%);
}

.is-collapsed .sidebar-brand {
  justify-content: center;
  padding: 0;
}

.sidebar-logo {
  position: relative;
  display: inline-flex;
  width: 34px;
  height: 34px;
  flex: none;
  align-items: center;
  justify-content: center;
  color: #d9ecfb;
  border: 1px solid rgb(111 174 224 / 42%);
  border-radius: 50%;
  background: rgb(50 111 209 / 22%);
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
  font-size: 16px;
  font-weight: 680;
  line-height: 1.25;
  white-space: nowrap;
}

.brand-copy span {
  margin-top: 2px;
  color: var(--mes-sidebar-muted);
  font-size: 9px;
  font-weight: 700;
}

.sidebar-scroll {
  flex: 1;
}

:deep(.el-menu) {
  border-right: 0;
  background: transparent;
}

:deep(.el-menu-item) {
  height: 42px;
  margin: 2px 10px;
  padding: 0 13px !important;
  gap: 12px;
  color: var(--mes-sidebar-text);
  border-radius: var(--mes-radius-sm);
  font-size: 13px;
}

:deep(.el-sub-menu__title) {
  height: 42px;
  margin: 2px 10px;
  padding: 0 13px !important;
  gap: 12px;
  color: var(--mes-sidebar-text);
  border-radius: var(--mes-radius-sm);
  font-size: 13px;
}

:deep(.el-sub-menu__title:hover) {
  color: #fff;
  background: var(--mes-sidebar-raised);
}

:deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #e0effb;
  background: rgb(50 111 209 / 22%);
}

:deep(.el-sub-menu .el-menu-item) {
  height: 36px;
  margin-left: 22px;
  padding-left: 16px !important;
  font-size: 12px;
}

.submenu-marker {
  width: 5px;
  height: 5px;
  flex: none;
  border-radius: 50%;
  background: #647382;
}

:deep(.el-sub-menu .el-menu-item.is-active) .submenu-marker {
  background: #7fb0f2;
}

:deep(.el-menu-item:hover) {
  color: #fff;
  background: var(--mes-sidebar-raised);
}

:deep(.el-menu-item.is-active) {
  color: #f2f8fd;
  background: #2b5fae;
}

:deep(.el-menu-item.is-featured) {
  color: #dcebf8;
  border: 1px solid rgb(93 145 222 / 20%);
  background: rgb(50 111 209 / 18%);
}

:deep(.el-menu-item.is-featured:hover),
:deep(.el-menu-item.is-featured.is-active) {
  color: #f2f8fd;
  border-color: rgb(111 165 239 / 36%);
  background: #2b5fae;
}

:deep(.el-menu--collapse .el-menu-item) {
  justify-content: center;
  margin-right: 9px;
  margin-left: 9px;
  padding: 0 !important;
}

:deep(.el-menu--collapse .el-sub-menu__title) {
  justify-content: center;
  margin-right: 9px;
  margin-left: 9px;
  padding: 0 !important;
}

.menu-group-label {
  padding: 17px 22px 6px;
  color: #74808c;
  font-size: 10px;
  font-weight: 700;
}

.sidebar-footer {
  display: flex;
  min-height: 60px;
  flex: none;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-top: 1px solid rgb(255 255 255 / 7%);
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
  color: #d8e0e8;
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
