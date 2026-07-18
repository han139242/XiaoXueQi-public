<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppIcon from '@/components/common/AppIcon.vue'
import { getCurrentUserInfo, login } from '@/api/auth'
import shuttlecockImage from '@/assets/login-background-blue.png'
import { firstAccessiblePath, isNavigationPathAccessible } from '@/config/navigation'
import {
  setCurrentUser,
  setToken,
  startFrameworkPreview
} from '@/utils/auth'

const route = useRoute()
const router = useRouter()
// ref 保存单个响应式值；reactive 适合把整个登录表单作为一个响应式对象管理。
const formRef = ref()
const loading = ref(false)
const showPreview = import.meta.env.DEV

const form = reactive({
  username: '',
  password: '',
  remember: true
})

// rules 交给 Element Plus 的 el-form，在发送登录请求前先检查必填项。
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 路由守卫把未登录用户送到登录页时，会用 redirect 记录用户原本想访问的页面。
// 登录成功后回到该页面；没有 redirect 时进入首页，并限制目标必须是站内路径。
function resolveTarget() {
  const fallback = firstAccessiblePath('/forbidden')
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  if (!redirect.startsWith('/')) {
    return fallback
  }
  return isNavigationPathAccessible(redirect) ? redirect : fallback
}

// 执行表单校验、调用登录接口、保存Token和用户权限，最后进入原目标页面。
async function handleLogin() {
  // 第一步先执行前端表单验证，验证不通过或正在登录时不重复发送请求。
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || loading.value) {
    return
  }

  loading.value = true
  try {
    // 第二步调用后端登录接口，账号密码正确时后端签发 JWT Token。
    const result = await login({
      username: form.username.trim(),
      password: form.password
    })
    // 第三步保存 Token。之后 request.js 会把它自动放进每次请求的 Authorization 请求头。
    setToken(result.token, form.remember)
    // 第四步再查询用户、角色和权限并保存，菜单及按钮权限都依赖这份用户信息。
    const userInfo = await getCurrentUserInfo()
    setCurrentUser(userInfo.data || result.data)
    ElMessage.success('登录成功')
    // replace 不会把登录页留在浏览器历史中，按“返回”不会再次回到登录页。
    await router.replace(resolveTarget())
  } catch {
    // 请求错误已经由共享Axios响应拦截器统一提示，这里不重复弹窗。
  } finally {
    loading.value = false
  }
}

// 开发环境下建立不访问后端的框架预览会话并进入首页。
async function enterPreview() {
  // 仅开发环境提供框架预览，不请求真实登录接口。
  if (startFrameworkPreview()) {
    await router.replace('/dashboard')
  }
}
</script>

<template>
  <!-- 登录页由品牌背景区和登录表单区组成，表单提交统一进入handleLogin。 -->
  <main class="login-page" :style="{ '--shuttlecock-image': `url(${shuttlecockImage})` }">
    <section class="brand-panel" aria-label="羽毛球制造执行系统介绍">
      <div class="brand-shell">
        <div class="brand-copy">
          <span class="brand-kicker">BADMINTON MANUFACTURING</span>
          <h1>羽球智造 <strong>MES</strong></h1>
          <p>羽毛球制造执行系统</p>
        </div>
      </div>
    </section>

    <section class="login-panel" aria-label="系统登录">
      <div class="login-card">
        <div class="login-heading">
          <span class="login-eyebrow"><i />系统访问</span>
          <h2>欢迎回来</h2>
          <p>登录羽球智造制造运营工作台</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          :aria-busy="loading"
          label-position="top"
          size="large"
          @keyup.enter="handleLogin"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" autocomplete="username" placeholder="请输入用户名">
              <template #prefix><AppIcon name="user" :size="16" /></template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              autocomplete="current-password"
              placeholder="请输入密码"
              show-password
            >
              <template #prefix><AppIcon name="lock" :size="16" /></template>
            </el-input>
          </el-form-item>

          <div class="login-options">
            <el-checkbox v-model="form.remember">保持登录</el-checkbox>
            <span class="login-status"><AppIcon name="shield-check" :size="13" />JWT 安全认证</span>
          </div>

          <el-button class="login-submit" type="primary" :loading="loading" @click="handleLogin">
            <template v-if="!loading">
              进入工作台
              <AppIcon name="arrow" :size="16" />
            </template>
            <template v-else>登录中…</template>
          </el-button>

          <el-button v-if="showPreview" class="preview-button" plain @click="enterPreview">
            查看基础框架
          </el-button>
        </el-form>

        <footer class="login-footer">
          <span><AppIcon name="shield-check" :size="13" />企业内部系统</span>
          <span>MES 管理端 · 2026</span>
        </footer>
      </div>
    </section>
  </main>
</template>

<style scoped>
/* 登录页样式：控制全屏背景、品牌文字、登录卡片、表单控件和移动端适配。 */
.login-page {
  position: relative;
  min-height: 100dvh;
  overflow-x: hidden;
  overflow-y: auto;
  background: #dceafa;
  font-family: var(--mes-font);
}

.login-page::before {
  position: absolute;
  inset: 0;
  background: var(--shuttlecock-image) center / cover no-repeat;
  filter: brightness(.96) saturate(.9) contrast(1.02);
  content: '';
  opacity: 1;
  animation: background-breathe 18s ease-in-out infinite alternate;
  transform-origin: center;
}

.login-page::after {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgb(240 248 255 / 24%) 0%, transparent 30%, rgb(27 86 145 / 7%) 100%),
    linear-gradient(90deg, rgb(255 255 255 / 10%) 0%, transparent 50%, rgb(20 74 130 / 10%) 100%);
  content: '';
  pointer-events: none;
}

.brand-panel {
  position: absolute;
  z-index: 1;
  inset: 0;
  min-height: 100dvh;
  padding: 0 20px;
  background: transparent;
  pointer-events: none;
}

.brand-shell {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  flex-direction: column;
  align-items: center;
  animation: brand-enter .9s cubic-bezier(.2, .8, .2, 1) both;
}

.brand-copy {
  width: min(520px, 100%);
  margin: 34px auto 0;
  text-align: center;
}

.brand-kicker {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 11px;
  color: #245f95;
  font-size: 11px;
  font-weight: 760;
  line-height: 1.2;
}

.brand-kicker::before,
.brand-kicker::after {
  width: 28px;
  height: 1px;
  background: rgb(var(--mes-primary-rgb) / 78%);
  content: '';
}

.brand-copy h1 {
  color: #123a62;
  font-size: 40px;
  font-weight: 720;
  line-height: 1.14;
  letter-spacing: 0;
  text-shadow: 0 2px 12px rgb(255 255 255 / 52%);
}

.brand-copy h1 strong {
  color: var(--mes-primary);
  font-weight: 780;
}

.brand-copy p {
  max-width: 480px;
  margin-top: 9px;
  color: #416583;
  font-size: 14px;
  font-weight: 520;
  line-height: 1.6;
}

.login-panel {
  position: relative;
  z-index: 2;
  display: flex;
  min-height: 100dvh;
  align-items: center;
  justify-content: center;
  padding: 152px 24px 34px;
  background: transparent;
}

.login-card {
  position: relative;
  width: 100%;
  max-width: 432px;
  overflow: hidden;
  padding: 34px 36px 25px;
  border: 1px solid rgb(255 255 255 / 92%);
  border-radius: 7px;
  background: rgb(250 253 255 / 91%);
  box-shadow: 0 28px 72px rgb(24 70 117 / 20%), 0 3px 10px rgb(26 61 96 / 8%);
  backdrop-filter: blur(20px) saturate(106%);
  animation: login-enter .9s cubic-bezier(.2, .8, .2, 1) .12s both;
}

.login-card::before {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 4px;
  background: var(--mes-primary);
  content: '';
}

.login-heading {
  margin-bottom: 24px;
}

.login-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 10px;
  color: #2f6fa7;
  font-size: 11px;
  font-weight: 760;
  letter-spacing: 0;
}

.login-eyebrow i {
  width: 7px;
  height: 7px;
  border: 2px solid rgb(var(--mes-primary-rgb) / 26%);
  border-radius: 50%;
  background: var(--mes-primary);
}

.login-heading h2 {
  color: var(--mes-text);
  font-family: var(--mes-font);
  font-size: 28px;
  font-weight: 720;
  line-height: 1.2;
}

.login-heading p {
  margin-top: 7px;
  color: var(--mes-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: -2px 0 21px;
}

.login-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  overflow: hidden;
  color: #708294;
  font-size: 11px;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.login-submit.el-button,
.preview-button.el-button {
  width: 100%;
  height: 48px;
  margin: 0;
  border-radius: 5px;
  font-size: 14px;
  font-weight: 660;
  transition: transform .2s ease, box-shadow .2s ease, background-color .2s ease, border-color .2s ease;
}

.login-submit.el-button {
  display: inline-flex;
  gap: 8px;
  border: 0;
  color: #fff;
  background: #2d8cf0;
  box-shadow: 0 8px 20px rgb(31 112 194 / 18%);
  letter-spacing: 0;
}

.login-submit.el-button:hover:not(:disabled) {
  background: #1679dc;
  box-shadow: 0 10px 24px rgb(31 112 194 / 24%);
  transform: translateY(-1px);
}

.login-submit.el-button:active:not(:disabled),
.preview-button.el-button:active:not(:disabled) {
  transform: translateY(0);
}

.preview-button.el-button {
  margin-top: 10px;
  color: var(--mes-text-secondary);
  border-color: #c8d7e5;
  background: rgb(255 255 255 / 76%);
}

.preview-button.el-button:hover:not(:disabled) {
  color: var(--mes-primary);
  border-color: var(--mes-primary-border);
  background: var(--mes-primary-soft);
  box-shadow: 0 8px 18px rgb(var(--mes-primary-rgb) / 12%);
  transform: translateY(-1px);
}

.login-footer {
  display: inline-flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 22px;
  padding-top: 14px;
  border-top: 1px solid rgb(190 207 222 / 72%);
  color: #748697;
  font-size: 10px;
}

.login-footer span:first-child {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

:deep(.el-form-item) { margin-bottom: 17px; }

:deep(.el-form-item__label) {
  padding-bottom: 7px;
  color: #3e4d5d;
  font-size: 13px;
  font-weight: 660;
}

:deep(.el-input__wrapper) {
  min-height: 48px;
  padding: 1px 12px 1px 10px;
  border-radius: 5px;
  background: rgb(255 255 255 / 94%);
  box-shadow: 0 0 0 1px #bdccda inset, 0 2px 6px rgb(48 76 104 / 4%);
  transition: box-shadow .18s ease, background-color .18s ease;
}

:deep(.el-input__wrapper:hover) {
  background: #fff;
  box-shadow: 0 0 0 1px #83b2de inset, 0 3px 8px rgb(48 76 104 / 6%);
}

:deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px #2d8cf0 inset, 0 0 0 4px rgb(45 140 240 / 10%) !important;
}

:deep(.el-form-item__content) { overflow: visible; }

:deep(.el-input__inner) {
  color: var(--mes-text);
  background: transparent;
  font-size: 14px;
}

:deep(.el-input__inner::placeholder) { color: #9aa8b5; }

:deep(.el-input__prefix) { color: var(--mes-primary); }

:deep(.el-input__prefix-inner) {
  width: 29px;
  justify-content: center;
  margin-right: 7px;
  border-right: 1px solid #e0e8ef;
}

:deep(.el-input__suffix) { color: #748494; }

:deep(.el-input__inner:-webkit-autofill),
:deep(.el-input__inner:-webkit-autofill:hover),
:deep(.el-input__inner:-webkit-autofill:focus) {
  -webkit-text-fill-color: var(--mes-text);
  box-shadow: 0 0 0 1000px #fff inset;
  caret-color: var(--mes-text);
  transition: background-color 9999s ease-out 0s;
}

:deep(.el-input__wrapper:hover .el-input__inner:-webkit-autofill) {
  box-shadow: 0 0 0 1000px #fbfdff inset;
}

:deep(.el-input__wrapper.is-focus .el-input__inner:-webkit-autofill) {
  box-shadow: 0 0 0 1000px #fff inset;
}

:deep(.el-checkbox) {
  --el-checkbox-text-color: var(--mes-text-secondary);
  --el-checkbox-checked-text-color: var(--mes-text);
  --el-checkbox-input-border-color: #aebbc8;
  --el-checkbox-checked-bg-color: var(--mes-primary);
  --el-checkbox-checked-input-border-color: var(--mes-primary);
  --el-checkbox-checked-icon-color: #fff;
}

@media (max-width: 1080px) {
  .brand-copy { margin-top: 30px; }
  .login-panel { padding-top: 146px; }
}

@media (max-width: 880px) {
  .login-page::before { background-position: 31% center; }

  .login-page::after {
    background: linear-gradient(180deg, rgb(242 249 255 / 30%) 0%, rgb(235 245 255 / 8%) 55%, rgb(28 87 145 / 8%) 100%);
  }

  .brand-panel { padding-inline: 18px; }
  .brand-copy { width: min(430px, 100%); margin-top: 27px; }
  .brand-copy h1 { font-size: 32px; }

  .login-panel {
    min-height: 100dvh;
    padding: 136px 20px 30px;
    background: transparent;
  }

  .login-card {
    max-width: 480px;
    margin: 0 auto;
    background: rgb(255 255 255 / 94%);
    box-shadow: 0 18px 46px rgb(25 63 102 / 16%);
    backdrop-filter: blur(18px);
  }
}

@media (max-width: 600px) {
  .brand-copy { margin-top: 22px; }
  .brand-copy h1 { font-size: 30px; }
  .brand-copy p { margin-top: 6px; font-size: 13px; }
  .brand-kicker { margin-bottom: 8px; font-size: 10px; }
  .brand-kicker::before,
  .brand-kicker::after { width: 18px; }
  .login-panel { padding: 124px 14px 22px; }
  .login-card { padding: 27px 22px 21px; }
  .login-footer { align-items: flex-start; flex-direction: column; gap: 5px; }
}

@media (max-height: 760px) and (min-width: 881px) {
  .brand-copy { margin-top: 22px; }
  .brand-copy h1 { font-size: 32px; }
  .brand-copy p { margin-top: 5px; font-size: 13px; }
  .brand-kicker { margin-bottom: 7px; }
  .login-panel { padding: 114px 24px 20px; }
  .login-card { padding: 27px 30px 20px; }
  .login-heading { margin-bottom: 20px; }
  :deep(.el-form-item) { margin-bottom: 14px; }
  .login-options { margin-bottom: 17px; }
  .login-footer { margin-top: 18px; padding-top: 12px; }
}

@keyframes background-breathe {
  from { filter: brightness(.95) saturate(.88) contrast(1.02); transform: scale(1.014); }
  to { filter: brightness(.98) saturate(.92) contrast(1.01); transform: scale(1.002); }
}

@keyframes login-enter {
  from { opacity: 0; transform: translateY(28px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes brand-enter {
  from { opacity: 0; transform: translateY(-22px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (prefers-reduced-motion: reduce) {
  .login-page::before,
  .brand-shell,
  .login-card { animation: none; }
}
</style>
