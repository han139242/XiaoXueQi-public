import Cookies from 'js-cookie'
import { ref } from 'vue'

const TOKEN_KEY = 'mes_access_token'
const USER_KEY = 'mes_current_user'
const PREVIEW_KEY = 'mes_framework_preview'

export const authRevision = ref(0)

export function getToken() {
  return Cookies.get(TOKEN_KEY) || ''
}

export function setToken(token, remember = true) {
  const options = { sameSite: 'Lax' }
  if (remember) {
    options.expires = 7
  }
  Cookies.set(TOKEN_KEY, token, options)
}

export function getCurrentUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || '{}')
  } catch {
    return {}
  }
}

export function setCurrentUser(user = {}) {
  const {
    password,
    passwordHash,
    token,
    apiKey,
    ...safeUser
  } = user || {}
  localStorage.setItem(USER_KEY, JSON.stringify(safeUser))
  authRevision.value += 1
}

export function clearAuth() {
  Cookies.remove(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  sessionStorage.removeItem(PREVIEW_KEY)
  authRevision.value += 1
}

export function startFrameworkPreview() {
  if (!import.meta.env.DEV) {
    return false
  }
  sessionStorage.setItem(PREVIEW_KEY, 'true')
  setCurrentUser({ username: 'preview', realName: '框架预览' })
  return true
}

export function isFrameworkPreview() {
  return import.meta.env.DEV && sessionStorage.getItem(PREVIEW_KEY) === 'true'
}

export function hasAuthenticatedSession() {
  return Boolean(getToken()) || isFrameworkPreview()
}
