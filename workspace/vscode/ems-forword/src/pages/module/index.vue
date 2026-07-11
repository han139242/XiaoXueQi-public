<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppIcon from '@/components/common/AppIcon.vue'
import { getModuleDefinition } from '@/config/navigation'

const route = useRoute()
const keyword = ref('')
const definition = computed(() => getModuleDefinition(route.params.moduleKey))
const filteredEntries = computed(() => {
  const value = keyword.value.trim()
  if (!value) {
    return definition.value.entries
  }
  return definition.value.entries.filter((entry) => entry.label.includes(value))
})

watch(definition, (value) => {
  document.title = `${value.title} - 羽球智造 MES`
  keyword.value = ''
}, { immediate: true })

function openEntry(entry) {
  if (entry.path) {
    window.location.hash = `#${entry.path}`
  }
}
</script>

<template>
  <div class="mes-page module-page">
    <header class="mes-page-header module-header">
      <div class="module-heading">
        <span class="module-heading-icon" :class="`tone-${definition.tone}`">
          <AppIcon :name="definition.icon" :size="22" />
        </span>
        <div>
          <h1 class="mes-page-title">{{ definition.title }}</h1>
          <p class="mes-page-subtitle">模块导航与业务入口</p>
        </div>
      </div>
      <el-input v-model="keyword" class="module-search" clearable placeholder="搜索功能">
        <template #prefix><AppIcon name="search" :size="16" /></template>
      </el-input>
    </header>

    <section class="module-overview">
      <div class="overview-label">
        <span>业务功能</span>
        <strong>{{ definition.entries.length }}</strong>
      </div>
      <div class="overview-divider" />
      <div class="overview-state">
        <span class="state-dot" />
        <div><strong>业务界面已接入</strong><small>连接后端真实接口与状态编码</small></div>
      </div>
    </section>

    <section class="mes-panel module-panel">
      <div class="mes-panel-header">
        <h2 class="mes-panel-title">功能入口</h2>
        <span class="entry-count">{{ filteredEntries.length }} 项</span>
      </div>
      <div v-if="filteredEntries.length" class="entry-grid">
        <button v-for="(entry, index) in filteredEntries" :key="entry.label" type="button" @click="openEntry(entry)">
          <span class="entry-index">{{ String(index + 1).padStart(2, '0') }}</span>
          <span class="entry-name">{{ entry.label }}</span>
          <el-tag size="small" type="success" effect="plain">已接入</el-tag>
          <AppIcon name="right" :size="15" />
        </button>
      </div>
      <el-empty v-else description="没有匹配的功能" :image-size="72" />
    </section>
  </div>
</template>

<style scoped>
.module-header {
  align-items: center;
}

.module-heading,
.overview-state {
  display: flex;
  align-items: center;
}

.module-heading {
  gap: 13px;
}

.module-heading-icon {
  display: inline-flex;
  width: 46px;
  height: 46px;
  flex: none;
  align-items: center;
  justify-content: center;
  color: var(--tone-color);
  border: 1px solid var(--tone-border);
  border-radius: var(--mes-radius);
  background: var(--tone-soft);
}

.tone-green {
  --tone-color: var(--mes-data);
  --tone-border: #b9d7e7;
  --tone-soft: var(--mes-data-soft);
}

.tone-teal {
  --tone-color: var(--mes-data);
  --tone-border: #b9d7e7;
  --tone-soft: var(--mes-data-soft);
}

.tone-blue {
  --tone-color: var(--mes-primary);
  --tone-border: var(--mes-primary-border);
  --tone-soft: var(--mes-primary-soft);
}

.tone-amber {
  --tone-color: var(--mes-warning);
  --tone-border: #ead0a8;
  --tone-soft: var(--mes-warning-soft);
}

.module-search {
  width: 240px;
}

.module-overview {
  display: flex;
  min-height: 76px;
  align-items: center;
  gap: 24px;
  margin-bottom: 14px;
  padding: 0 20px;
  border: 1px solid var(--mes-border);
  border-left: 3px solid var(--mes-primary);
  border-radius: var(--mes-radius);
  background: #fff;
}

.overview-label {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.overview-label span {
  color: var(--mes-text-secondary);
  font-size: 12px;
}

.overview-label strong {
  color: var(--mes-text);
  font-size: 23px;
}

.overview-divider {
  width: 1px;
  height: 30px;
  background: var(--mes-border);
}

.overview-state {
  gap: 10px;
}

.overview-state > div {
  display: flex;
  flex-direction: column;
}

.overview-state strong {
  font-size: 12px;
  font-weight: 620;
}

.overview-state small {
  margin-top: 2px;
  color: var(--mes-text-muted);
  font-size: 10px;
}

.state-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--mes-primary);
  box-shadow: 0 0 0 4px var(--mes-primary-soft);
}

.entry-count {
  color: var(--mes-text-muted);
  font-size: 11px;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.entry-grid button {
  display: grid;
  min-height: 64px;
  grid-template-columns: 34px minmax(0, 1fr) auto 18px;
  align-items: center;
  gap: 12px;
  padding: 10px 18px;
  color: var(--mes-text);
  border: 0;
  border-right: 1px solid var(--mes-border);
  border-bottom: 1px solid var(--mes-border);
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.entry-grid button:nth-child(2n) {
  border-right: 0;
}

.entry-grid button:hover {
  background: #f3f7fc;
}

.entry-grid button:hover .entry-name,
.entry-grid button:hover > .app-icon {
  color: var(--mes-primary);
}

.entry-index {
  color: var(--mes-text-muted);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.entry-name {
  overflow: hidden;
  font-size: 13px;
  font-weight: 580;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-grid button > .app-icon {
  color: var(--mes-text-muted);
}

@media (max-width: 720px) {
  .module-search {
    width: 100%;
  }

  .entry-grid {
    grid-template-columns: 1fr;
  }

  .entry-grid button {
    border-right: 0;
  }

  .module-overview {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
    padding: 16px;
  }

  .overview-divider {
    width: 100%;
    height: 1px;
  }
}
</style>
