<script setup>
import AppIcon from '@/components/common/AppIcon.vue'
import { activeTheme, activeThemeKey, setTheme, themeOptions } from '@/utils/theme'

// open通过父组件的v-model双向绑定，父组件控制抽屉打开，本组件可以主动关闭。
const open = defineModel({ type: Boolean, default: false })

// 应用并持久化主题后关闭抽屉；真正修改<html data-theme>由theme.js负责。
function selectTheme(themeKey) {
  setTheme(themeKey)
  open.value = false
}
</script>

<template>
  <!-- 根据themeOptions循环生成主题预览，不为每套主题重复写一份HTML。 -->
  <el-drawer v-model="open" size="410px" :with-header="false" append-to-body>
    <div class="theme-drawer-shell">
      <header class="theme-drawer-head">
        <div>
          <strong>选择主题</strong>
          <span>当前：{{ activeTheme.name }}</span>
        </div>
        <el-button text aria-label="关闭主题面板" @click="open = false">
          <AppIcon name="close" :size="18" />
        </el-button>
      </header>

      <div class="theme-option-list" role="radiogroup" aria-label="界面主题">
        <button
          v-for="option in themeOptions"
          :key="option.key"
          type="button"
          class="theme-layout-option"
          :class="{ active: activeThemeKey === option.key }"
          role="radio"
          :aria-checked="activeThemeKey === option.key"
          @click="selectTheme(option.key)"
        >
          <span class="theme-layout-preview" aria-hidden="true">
            <i class="preview-header" :style="{ backgroundColor: option.preview.header }" />
            <i class="preview-sidebar" :style="{ backgroundColor: option.preview.sidebar }" />
            <i class="preview-content" :style="{ backgroundColor: option.preview.content }">
              <b /><b /><b />
            </i>
          </span>
          <span class="theme-layout-copy">
            <strong>{{ option.name }}</strong>
            <small>{{ option.mode }}</small>
          </span>
          <span v-if="activeThemeKey === option.key" class="theme-selected">
            <AppIcon name="check" :size="14" :stroke-width="2.4" />
          </span>
        </button>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
/* 主题抽屉样式：控制主题预览缩略图、选中状态和抽屉头部。 */
.theme-drawer-shell {
  min-height: 100%;
  background: #fff;
}

.theme-drawer-head {
  display: flex;
  min-height: 64px;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px 0 20px;
  color: var(--mes-header-text);
  background: var(--mes-header-bg);
}

.theme-drawer-head > div {
  display: flex;
  flex-direction: column;
}

.theme-drawer-head strong {
  font-size: 16px;
  font-weight: 650;
}

.theme-drawer-head span {
  margin-top: 3px;
  color: var(--mes-header-muted);
  font-size: 10px;
}

.theme-drawer-head :deep(.el-button) {
  color: var(--mes-header-text);
}

.theme-option-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 22px 24px;
}

.theme-layout-option {
  position: relative;
  display: flex;
  min-height: 76px;
  flex-direction: column;
  gap: 8px;
  padding: 0;
  color: var(--mes-text);
  border: 0;
  border-radius: 5px;
  background: transparent;
  box-shadow: none;
  text-align: left;
  cursor: pointer;
}

.theme-layout-option:hover,
.theme-layout-option.active {
  box-shadow: none;
}

.theme-layout-preview {
  position: relative;
  display: grid;
  width: 100%;
  height: 74px;
  grid-template-columns: 96px 1fr;
  grid-template-rows: 20px 1fr;
  overflow: hidden;
  border: 1px solid #d4d2d2;
  border-radius: 5px;
  background: #fff;
  box-shadow: 0 2px 8px rgb(117 114 114 / 12%);
  transition: transform .16s ease, box-shadow .16s ease;
}

.theme-layout-option:hover .theme-layout-preview,
.theme-layout-option.active .theme-layout-preview {
  transform: translateY(-1px);
  box-shadow: 0 5px 14px rgb(117 114 114 / 20%);
}

.theme-layout-option.active .theme-layout-preview {
  outline: 2px solid rgb(var(--mes-primary-rgb) / 36%);
  outline-offset: 2px;
}

.preview-header {
  grid-column: 1 / -1;
}

.preview-sidebar {
  border-right: 1px solid rgb(80 90 100 / 13%);
}

.preview-content {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 8px;
}

.preview-content b {
  display: block;
  width: 82%;
  height: 5px;
  border-radius: 1px;
  background: rgb(112 126 140 / 22%);
}

.preview-content b:nth-child(2) {
  width: 58%;
}

.preview-content b:nth-child(3) {
  width: 70%;
}

.theme-layout-copy {
  display: grid;
  width: 100%;
  grid-template-columns: 1fr auto;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.theme-layout-copy strong {
  font-size: 13px;
  font-weight: 650;
}

.theme-layout-copy small {
  color: var(--mes-text-muted);
  font-size: 10px;
}

.theme-selected {
  position: absolute;
  right: 10px;
  top: 10px;
  display: inline-flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  color: #fff;
  border-radius: 50%;
  background: var(--mes-primary);
}

@media (max-width: 420px) {
  .theme-option-list {
    padding: 18px;
  }
}
</style>
