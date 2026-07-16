import { computed, ref } from 'vue'

// 主题模块统一管理当前主题、CSS 标记和浏览器持久化；刷新页面后仍保留用户选择。
const STORAGE_KEY = 'mes_ui_theme'
const DEFAULT_VERSION_KEY = 'mes_ui_theme_default_version'
const CURRENT_DEFAULT_VERSION = 'blue-default-20260711'
const DEFAULT_THEME = 'blue'

// 可选主题清单：key用于存储和CSS选择，preview用于主题抽屉，chart用于ECharts配色。
export const themeOptions = [
  {
    key: 'blue',
    name: '经典蓝',
    mode: '明亮导航',
    preview: { header: '#2d8cf0', sidebar: '#ffffff', content: '#f1f3f6' },
    chart: { primary: '#2d8cf0', data: '#2db7f5' }
  },
  {
    key: 'dark',
    name: '曜石黑',
    mode: '深色导航',
    preview: { header: '#272929', sidebar: '#090a0a', content: '#f1f3f6' },
    chart: { primary: '#277bc0', data: '#2db7f5' }
  },
  {
    key: 'green',
    name: '制造绿',
    mode: '明亮导航',
    preview: { header: '#147a5c', sidebar: '#ffffff', content: '#f1f3f6' },
    chart: { primary: '#147a5c', data: '#3979bd' }
  },
  {
    key: 'orange',
    name: '工业橙',
    mode: '明亮导航',
    preview: { header: '#b85a12', sidebar: '#ffffff', content: '#f1f3f6' },
    chart: { primary: '#b85a12', data: '#347cab' }
  },
  {
    key: 'red',
    name: '企业红',
    mode: '明亮导航',
    preview: { header: '#b5393e', sidebar: '#ffffff', content: '#f1f3f6' },
    chart: { primary: '#b5393e', data: '#287c9f' }
  }
]

// 当前主题键是响应式状态，切换后使用它的组件会自动更新。
export const activeThemeKey = ref(DEFAULT_THEME)
// 根据当前键取得完整主题对象，不存在时回退第一套主题。
export const activeTheme = computed(() => (
  themeOptions.find((item) => item.key === activeThemeKey.value) || themeOptions[0]
))

// 校验主题键是否存在，不合法的值统一回退到默认蓝色主题。
function normalizeTheme(themeKey) {
  return themeOptions.some((item) => item.key === themeKey) ? themeKey : DEFAULT_THEME
}

// 把规范化后的主题同时写入Vue状态和html的data-theme属性。
function applyTheme(themeKey) {
  // 将主题名写到 <html data-theme="...">，tokens.css 会据此切换整套 CSS 变量。
  const normalized = normalizeTheme(themeKey)
  activeThemeKey.value = normalized
  document.documentElement.dataset.theme = normalized
  return normalized
}

// 应用启动时恢复浏览器保存的主题，并处理默认主题版本升级。
export function initTheme() {
  // main.js 启动应用时调用：优先恢复用户选择，并处理默认主题版本升级。
  let storedTheme = DEFAULT_THEME
  try {
    const defaultVersion = localStorage.getItem(DEFAULT_VERSION_KEY)
    const persistedTheme = localStorage.getItem(STORAGE_KEY)
    if (defaultVersion !== CURRENT_DEFAULT_VERSION) {
      storedTheme = !persistedTheme || persistedTheme === 'dark' ? DEFAULT_THEME : persistedTheme
      localStorage.setItem(STORAGE_KEY, storedTheme)
      localStorage.setItem(DEFAULT_VERSION_KEY, CURRENT_DEFAULT_VERSION)
    } else {
      storedTheme = persistedTheme || DEFAULT_THEME
    }
  } catch {
    // 浏览器禁用localStorage时继续使用当前默认主题。
  }
  applyTheme(storedTheme)
}

// 用户主动选择主题时立即应用，并保存供下次打开恢复。
export function setTheme(themeKey) {
  // 用户在界面切换主题时立即应用，并保存到 localStorage。
  const normalized = applyTheme(themeKey)
  try {
    localStorage.setItem(STORAGE_KEY, normalized)
    localStorage.setItem(DEFAULT_VERSION_KEY, CURRENT_DEFAULT_VERSION)
  } catch {
    // 即使无法持久化，主题在本次页面会话中仍然有效。
  }
}
