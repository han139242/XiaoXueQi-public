<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppIcon from '@/components/common/AppIcon.vue'
import { login } from '@/api/auth'
import shuttlecockImage from '@/assets/login-background-blue.png'
import {
  setCurrentUser,
  setToken,
  startFrameworkPreview
} from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const showPreview = import.meta.env.DEV

const form = reactive({
  username: '',
  password: '',
  remember: true
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function resolveTarget() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
  return redirect.startsWith('/') ? redirect : '/dashboard'
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || loading.value) {
    return
  }

  loading.value = true
  try {
    const result = await login({
      username: form.username.trim(),
      password: form.password
    })
    setToken(result.token, form.remember)
    setCurrentUser(result.data)
    ElMessage.success('登录成功')
    await router.replace(resolveTarget())
  } catch {
    // Request errors are presented by the shared Axios interceptor.
  } finally {
    loading.value = false
  }
}

async function enterPreview() {
  if (startFrameworkPreview()) {
    await router.replace('/dashboard')
  }
}
</script>

<template>
  <main class="login-page" :style="{ '--shuttlecock-image': `url(${shuttlecockImage})` }">
    <section class="brand-panel" aria-label="羽毛球制造执行系统介绍">
      <div class="brand-shell">
        <header class="brand-header">
          <span class="brand-mark" aria-hidden="true"><AppIcon name="factory" :size="17" /></span>
          <strong>羽球智造 MES</strong>
        </header>

        <div class="brand-copy">
          <h1>羽毛球制造系统</h1>
          <p>让生产过程清晰可见。</p>
        </div>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-card">
        <div class="login-heading">
          <span class="login-eyebrow">WELCOME BACK</span>
          <h2>登录工作台</h2>
          <p>使用系统账号进入制造运营后台</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
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
            <span class="login-status">安全连接</span>
          </div>

          <el-button class="login-submit" type="primary" :loading="loading" @click="handleLogin">
            <template v-if="!loading">
              进入系统
              <AppIcon name="arrow" :size="16" />
            </template>
            <template v-else>登录中…</template>
          </el-button>

          <el-button v-if="showPreview" class="preview-button" plain @click="enterPreview">
            查看基础框架
          </el-button>
        </el-form>

        <footer class="login-footer">
          <span><AppIcon name="shield-check" :size="13" />安全认证</span>
          <span>© 2026 Badminton MES</span>
        </footer>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  position: relative;
  display: grid;
  min-height: 100dvh;
  grid-template-columns: minmax(0, 1.18fr) minmax(430px, .82fr);
  overflow: hidden;
  background: #eaf1f8;
  font-family: var(--mes-font);
}

.login-page::before {
  position: absolute;
  inset: 0;
  background: var(--shuttlecock-image) center / cover no-repeat;
  filter: brightness(.98) saturate(.9) contrast(1.02);
  content: '';
  opacity: 1;
  animation: background-breathe 18s ease-in-out infinite alternate;
  transform-origin: center;
}

.login-page::after {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgb(255 255 255 / 8%) 0%, transparent 56%, rgb(14 50 88 / 14%) 100%);
  content: '';
  pointer-events: none;
}

.brand-panel {
  position: relative;
  z-index: 1;
  display: flex;
  min-height: 100dvh;
  align-items: stretch;
  padding: clamp(26px, 3.2vw, 52px) clamp(40px, 6vw, 92px) clamp(40px, 6vw, 92px);
  overflow: hidden;
  background: transparent;
}

.brand-shell {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  flex-direction: column;
  animation: brand-enter .9s cubic-bezier(.2, .8, .2, 1) both;
}

.brand-header strong {
  color: #173d66;
  font-size: 19px;
  font-weight: 560;
  letter-spacing: 0;
}

.brand-header {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.brand-mark {
  display: inline-flex;
  width: 34px;
  height: 34px;
  align-items: center;
  justify-content: center;
  color: var(--mes-primary);
  border: 1px solid rgb(var(--mes-primary-rgb) / 34%);
  border-radius: 6px;
  background: rgb(255 255 255 / 72%);
  backdrop-filter: blur(8px);
}

.brand-copy {
  width: min(430px, 56%);
  margin: clamp(125px, 21vh, 215px) 0 0 auto;
}

.brand-copy h1 {
  color: #153b63;
  font-size: clamp(34px, 3.1vw, 44px);
  font-weight: 680;
  line-height: 1.2;
  letter-spacing: 0;
}

.brand-copy p {
  max-width: 480px;
  margin-top: 22px;
  color: #4c6986;
  font-size: 14px;
  line-height: 1.8;
}

.login-panel {
  position: relative;
  z-index: 2;
  display: flex;
  min-height: 100dvh;
  align-items: center;
  justify-content: center;
  padding: 40px clamp(58px, 8vw, 154px) 40px clamp(42px, 6vw, 100px);
  background: rgb(247 249 252 / 92%);
  backdrop-filter: blur(14px) saturate(88%);
  border-left: 1px solid rgb(199 211 224 / 82%);
  box-shadow: -18px 0 42px rgb(25 56 86 / 10%);
}

.login-card {
  width: 100%;
  max-width: 390px;
  padding: 22px 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
  animation: login-enter .9s cubic-bezier(.2, .8, .2, 1) .12s both;
}

.login-heading {
  margin-bottom: 28px;
}

.login-eyebrow {
  display: block;
  margin-bottom: 10px;
  color: var(--mes-primary);
  font-size: 10px;
  font-weight: 760;
  letter-spacing: 0;
}

.login-heading h2 {
  color: var(--mes-text);
  font-family: var(--mes-font);
  font-size: 30px;
  font-weight: 680;
  line-height: 1.2;
}

.login-heading p {
  margin-top: 7px;
  color: var(--mes-text-secondary);
  font-size: 13px;
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: -4px 0 22px;
}

.login-status {
  overflow: hidden;
  color: var(--mes-text-muted);
  font-size: 11px;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.login-submit.el-button,
.preview-button.el-button {
  width: 100%;
  height: 44px;
  margin: 0;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 660;
  transition: transform .2s ease, box-shadow .2s ease, background-color .2s ease, border-color .2s ease;
}

.login-submit.el-button {
  display: inline-flex;
  gap: 8px;
  border: 0;
  color: #fff;
  background: var(--mes-primary);
  letter-spacing: 0;
}

.login-submit.el-button:hover:not(:disabled) {
  background: var(--mes-primary-hover);
  box-shadow: 0 10px 22px rgb(var(--mes-primary-rgb) / 24%);
  transform: translateY(-2px);
}

.login-submit.el-button:active:not(:disabled),
.preview-button.el-button:active:not(:disabled) {
  transform: translateY(0);
}

.preview-button.el-button {
  margin-top: 10px;
  color: var(--mes-text-secondary);
  border-color: var(--mes-border-strong);
  background: #fff;
}

.preview-button.el-button:hover:not(:disabled) {
  color: var(--mes-primary);
  border-color: var(--mes-primary-border);
  background: var(--mes-primary-soft);
  box-shadow: 0 8px 18px rgb(var(--mes-primary-rgb) / 12%);
  transform: translateY(-2px);
}

.login-footer {
  display: inline-flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 26px;
  padding-top: 16px;
  border-top: 1px solid var(--mes-border);
  color: var(--mes-text-muted);
  font-size: 10px;
}

:deep(.el-form-item) { margin-bottom: 18px; }

:deep(.el-form-item__label) {
  padding-bottom: 7px;
  color: #3e4d5d;
  font-size: 12px;
  font-weight: 660;
}

:deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 5px;
  background: #fff;
  box-shadow: 0 0 0 1px #c9d3de inset;
  transition: box-shadow .18s, background-color .18s;
}

:deep(.el-input__wrapper:hover) {
  background: #fbfdff;
  box-shadow: 0 0 0 1px var(--mes-primary-border) inset;
}

:deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 2px var(--mes-primary) inset !important;
}

:deep(.el-form-item__content) { overflow: visible; }

:deep(.el-input__inner) {
  color: var(--mes-text);
  background: transparent;
  font-size: 14px;
}

:deep(.el-input__prefix) { color: #6f7f90; }

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
  .login-page { grid-template-columns: minmax(420px, 1fr) 400px; }
  .login-panel { padding-inline: 42px; }
  .brand-copy { width: min(390px, 62%); }
}

@media (max-width: 880px) {
  .login-page { grid-template-columns: 1fr; }

  .login-page::before { background-position: 28% center; }

  .brand-panel { min-height: 280px; padding: 30px 28px 34px; }
  .brand-copy { width: min(430px, 100%); margin: 54px 0 0; }
  .brand-copy h1 { font-size: 32px; }

  .login-panel {
    min-height: auto;
    padding: 32px 22px 48px;
    background: #f7f9fc;
    border-left: 0;
    border-top: 1px solid var(--mes-border);
    box-shadow: 0 -12px 32px rgb(25 56 86 / 9%);
  }
}

@media (max-width: 600px) {
  .brand-copy p { display: none; }

  .brand-copy { margin: 42px 0 0; }
  .brand-copy h1 { font-size: 30px; }
  .login-card { padding: 30px 22px 24px; }
}

@keyframes background-breathe {
  from { filter: brightness(.94) saturate(.86) contrast(1.03); transform: scale(1.035); }
  to { filter: brightness(1.01) saturate(.94) contrast(1.01); transform: scale(1.005); }
}

@keyframes login-enter {
  from { opacity: 0; transform: translateY(28px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes brand-enter {
  from { opacity: 0; transform: translateX(-34px); }
  to { opacity: 1; transform: translateX(0); }
}

@media (prefers-reduced-motion: reduce) {
  .login-page::before,
  .brand-shell,
  .login-card { animation: none; }
}
</style>
