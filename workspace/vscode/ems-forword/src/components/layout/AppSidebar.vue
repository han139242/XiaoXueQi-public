<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppIcon from '@/components/common/AppIcon.vue'
import { navigationGroups } from '@/config/navigation'

const props = defineProps({
  collapsed: { type: Boolean, default: false },
  mobile: { type: Boolean, default: false }
})

const emit = defineEmits(['navigate'])
const route = useRoute()
const activePath = computed(() => route.path)
const menuCollapsed = computed(() => props.collapsed && !props.mobile)
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
        router
        @select="emit('navigate')"
      >
        <template v-for="group in navigationGroups" :key="group.label">
          <div v-if="!menuCollapsed" class="menu-group-label">{{ group.label }}</div>
          <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
            <AppIcon :name="item.icon" :size="18" />
            <template #title>{{ item.label }}</template>
          </el-menu-item>
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
  color: #dff8ee;
  border: 1px solid rgb(126 218 182 / 42%);
  border-radius: 50%;
  background: rgb(8 127 91 / 22%);
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
  color: #f4f8f6;
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

:deep(.el-menu-item:hover) {
  color: #fff;
  background: var(--mes-sidebar-raised);
}

:deep(.el-menu-item.is-active) {
  color: #e7fff5;
  background: #0b7456;
}

:deep(.el-menu--collapse .el-menu-item) {
  justify-content: center;
  margin-right: 9px;
  margin-left: 9px;
  padding: 0 !important;
}

.menu-group-label {
  padding: 17px 22px 6px;
  color: #718078;
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
  color: #d8e0dc;
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
  border: 2px solid rgb(113 207 169 / 30%);
  border-radius: 50%;
  background: #52c896;
  box-shadow: 0 0 0 3px rgb(82 200 150 / 10%);
}
</style>
