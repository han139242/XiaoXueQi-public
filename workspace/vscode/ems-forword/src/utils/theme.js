import { computed, ref } from 'vue'

const STORAGE_KEY = 'mes_ui_theme'
const DEFAULT_VERSION_KEY = 'mes_ui_theme_default_version'
const CURRENT_DEFAULT_VERSION = 'blue-default-20260711'
const DEFAULT_THEME = 'blue'

export const themeOptions = [
  {
    key: 'blue',
    name: '经典蓝',
    mode: '明亮导航',
    preview: { header: '#2d8cf0', sidebar: '#ffffff', content: '#eeeeee' },
    chart: { primary: '#2d8cf0', data: '#2db7f5' }
  },
  {
    key: 'dark',
    name: '曜石黑',
    mode: '深色导航',
    preview: { header: '#272929', sidebar: '#090a0a', content: '#eeeeee' },
    chart: { primary: '#277bc0', data: '#2db7f5' }
  },
  {
    key: 'green',
    name: '制造绿',
    mode: '明亮导航',
    preview: { header: '#147a5c', sidebar: '#ffffff', content: '#eeeeee' },
    chart: { primary: '#147a5c', data: '#3979bd' }
  },
  {
    key: 'orange',
    name: '工业橙',
    mode: '明亮导航',
    preview: { header: '#b85a12', sidebar: '#ffffff', content: '#eeeeee' },
    chart: { primary: '#b85a12', data: '#347cab' }
  },
  {
    key: 'red',
    name: '企业红',
    mode: '明亮导航',
    preview: { header: '#b5393e', sidebar: '#ffffff', content: '#eeeeee' },
    chart: { primary: '#b5393e', data: '#287c9f' }
  }
]

export const activeThemeKey = ref(DEFAULT_THEME)
export const activeTheme = computed(() => (
  themeOptions.find((item) => item.key === activeThemeKey.value) || themeOptions[0]
))

function normalizeTheme(themeKey) {
  return themeOptions.some((item) => item.key === themeKey) ? themeKey : DEFAULT_THEME
}

function applyTheme(themeKey) {
  const normalized = normalizeTheme(themeKey)
  activeThemeKey.value = normalized
  document.documentElement.dataset.theme = normalized
  return normalized
}

export function initTheme() {
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
    // Storage may be unavailable in restricted browser environments.
  }
  applyTheme(storedTheme)
}

export function setTheme(themeKey) {
  const normalized = applyTheme(themeKey)
  try {
    localStorage.setItem(STORAGE_KEY, normalized)
    localStorage.setItem(DEFAULT_VERSION_KEY, CURRENT_DEFAULT_VERSION)
  } catch {
    // The selected theme still applies for the current session.
  }
}
