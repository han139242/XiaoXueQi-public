<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppTabs from '@/components/layout/AppTabs.vue'

const route = useRoute()
const collapsed = ref(localStorage.getItem('mes_sidebar_collapsed') === 'true')
const mobile = ref(false)
const mobileMenuOpen = ref(false)
const viewVersion = ref(0)
const tabs = ref([{ path: '/dashboard', title: '运营工作台', affix: true }])

function updateViewport() {
  mobile.value = window.innerWidth < 1024
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

watch(collapsed, (value) => {
  localStorage.setItem('mes_sidebar_collapsed', String(value))
})

watch(() => route.fullPath, closeMobileMenu)

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewport)
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
        <RouterView v-slot="{ Component }">
          <Transition name="page-fade" mode="out-in">
            <component :is="Component" :key="`${route.fullPath}-${viewVersion}`" />
          </Transition>
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
  padding: 20px;
}

.mobile-mask {
  position: fixed;
  z-index: 39;
  inset: 0;
  background: rgb(13 24 20 / 45%);
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.14s ease, transform 0.14s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(4px);
}

.page-fade-leave-to {
  opacity: 0;
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
    padding: 14px;
  }
}
</style>
