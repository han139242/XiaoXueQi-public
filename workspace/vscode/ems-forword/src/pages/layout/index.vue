<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppTabs from '@/components/layout/AppTabs.vue'
import { OPEN_AI_ASSISTANT_EVENT } from '@/utils/app-events'

// 这是登录后的后台总布局：侧栏负责菜单，Header 负责顶部操作，Tabs 记录已打开页面，
// 内层 RouterView 再显示当前选中的具体业务页面。
const route = useRoute()
const router = useRouter()
const collapsed = ref(localStorage.getItem('mes_sidebar_collapsed') === 'true')
const mobile = ref(false)
const mobileMenuOpen = ref(false)
// 刷新标签页时递增 key，Vue 会重新创建当前路由组件，从而重新执行页面初始化和数据加载。
const viewVersion = ref(0)
const tabs = ref([{ path: '/dashboard', title: '首页', affix: true }])

function updateViewport() {
  // 根据窗口宽度在桌面侧栏和移动端抽屉菜单之间切换。
  mobile.value = window.innerWidth < 1120
  if (!mobile.value) {
    mobileMenuOpen.value = false
  }
}

function toggleSidebar() {
  if (mobile.value) {
    mobileMenuOpen.value = !mobileMenuOpen.value
    return
  }
  collapsed.value = !collapsed.value
}

function closeMobileMenu() {
  if (mobile.value) {
    mobileMenuOpen.value = false
  }
}

function handleOpenAi() {
  router.push('/ai/workbench')
}

watch(collapsed, (value) => {
  localStorage.setItem('mes_sidebar_collapsed', String(value))
})

watch(() => route.fullPath, closeMobileMenu)

onMounted(() => {
  // 组件进入页面后注册窗口尺寸和“打开 AI 助手”两个全局事件。
  updateViewport()
  window.addEventListener('resize', updateViewport)
  window.addEventListener(OPEN_AI_ASSISTANT_EVENT, handleOpenAi)
})

onBeforeUnmount(() => {
  // 组件销毁前移除监听，避免重复注册和内存泄漏。
  window.removeEventListener('resize', updateViewport)
  window.removeEventListener(OPEN_AI_ASSISTANT_EVENT, handleOpenAi)
})
</script>

<template>
  <div
    class="layout-shell"
    :class="{
      'sidebar-collapsed': collapsed && !mobile,
      'is-mobile': mobile
    }"
  >
    <div v-if="mobile && mobileMenuOpen" class="mobile-mask" @click="mobileMenuOpen = false" />

    <div class="sidebar-region" :class="{ 'mobile-open': mobileMenuOpen }">
      <AppSidebar :collapsed="collapsed" :mobile="mobile" @navigate="closeMobileMenu" />
    </div>

    <section class="layout-main">
      <AppHeader :mobile="mobile" @toggle="toggleSidebar" />
      <AppTabs v-model="tabs" @refresh="viewVersion += 1" />
      <main class="content-region">
        <!-- 这里显示 layout 的子路由，例如生产订单、设备管理等具体业务页面。 -->
        <RouterView v-slot="{ Component }">
          <component :is="Component" :key="`${route.fullPath}-${viewVersion}`" />
        </RouterView>
      </main>
    </section>
  </div>
</template>

<style scoped>
.layout-shell {
  min-height: 100dvh;
  padding-left: var(--mes-sidebar-width);
  background: var(--mes-bg);
  transition: padding-left 0.2s ease;
}

.layout-shell.sidebar-collapsed {
  padding-left: var(--mes-sidebar-collapsed-width);
}

.sidebar-region {
  position: fixed;
  z-index: 30;
  inset: 0 auto 0 0;
  width: var(--mes-sidebar-width);
  transition: width 0.2s ease, transform 0.2s ease;
}

.sidebar-collapsed .sidebar-region {
  width: var(--mes-sidebar-collapsed-width);
}

.layout-main {
  min-width: 0;
  min-height: 100dvh;
}

.content-region {
  min-height: calc(100dvh - var(--mes-header-height) - var(--mes-tabs-height));
  padding: 18px 20px 28px;
}

.mobile-mask {
  position: fixed;
  z-index: 39;
  inset: 0;
  background: rgb(14 22 31 / 45%);
}

.layout-shell.is-mobile {
  padding-left: 0;
}

.is-mobile .sidebar-region {
  z-index: 40;
  width: min(82vw, 260px);
  transform: translateX(-100%);
}

.is-mobile .sidebar-region.mobile-open {
  transform: translateX(0);
}

@media (max-width: 768px) {
  .content-region {
    padding: 12px;
  }

}
</style>
