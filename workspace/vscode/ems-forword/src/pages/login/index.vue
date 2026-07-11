<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppIcon from '@/components/common/AppIcon.vue'
import { login } from '@/api/auth'
import shuttlecockImage from '@/assets/login-background.jpg'
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
  grid-template-columns: minmax(0, 1.08fr) minmax(460px, .92fr);
  overflow: hidden;
  background: var(--shuttlecock-image) center / cover no-repeat;
  font-family: var(--mes-font);
}

.login-page::before {
  position: absolute;
  inset: 0;
  background: var(--shuttlecock-image) center / cover no-repeat;
  filter: brightness(.98) saturate(.94) contrast(.97);
  content: '';
  opacity: 1;
  animation: background-breathe 18s ease-in-out infinite alternate;
  transform-origin: center;
}

.login-page::after {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgb(17 24 21 / 18%) 0%, transparent 52%, rgb(17 24 21 / 8%) 100%);
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
  background: linear-gradient(90deg, rgb(18 25 22 / 19%) 0%, transparent 65%);
}

.brand-panel::before {
  position: absolute;
  top: -120px;
  left: -100px;
  width: 310px;
  height: 310px;
  border-radius: 50%;
  background: radial-gradient(circle, rgb(255 228 156 / 36%), rgb(255 239 192 / 4%) 64%, transparent 72%);
  content: '';
  filter: blur(26px);
  opacity: .62;
  pointer-events: none;
  animation: glow-drift 13s ease-in-out infinite alternate;
}

.brand-panel::after {
  position: absolute;
  bottom: -150px;
  left: 9%;
  width: 360px;
  height: 360px;
  border-radius: 50%;
  background: radial-gradient(circle, rgb(83 136 117 / 31%), rgb(163 204 157 / 4%) 66%, transparent 74%);
  content: '';
  filter: blur(30px);
  opacity: .52;
  pointer-events: none;
  animation: glow-drift 16s ease-in-out -4s infinite alternate-reverse;
}

.brand-shell {
  position: relative;
  z-index: 1;
  display: flex;
  width: min(740px, 100%);
  flex-direction: column;
  animation: brand-enter .9s cubic-bezier(.2, .8, .2, 1) both;
}

.brand-header strong {
  color: #f4fbf7;
  font-size: 19px;
  font-weight: 560;
  letter-spacing: .01em;
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
  color: #eff9f2;
  border: 1px solid rgb(226 246 232 / 38%);
  border-radius: 6px;
  background: rgb(29 80 72 / 32%);
  backdrop-filter: blur(6px);
}

.brand-copy {
  margin: auto 0;
}

.brand-copy h1 {
  color: #f5fcf8;
  font-size: clamp(34px, 3.4vw, 46px);
  font-weight: 500;
  line-height: 1.2;
  letter-spacing: 0;
}

.brand-copy p {
  max-width: 480px;
  margin-top: 22px;
  color: #d5e6dd;
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
  background: rgb(24 28 32 / 12%);
  backdrop-filter: blur(10px) saturate(92%);
  border-left: 1px solid rgb(255 250 232 / 20%);
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 10px 0;
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
  color: #e4f1e6;
  font-size: 10px;
  font-weight: 760;
  letter-spacing: .14em;
}

.login-heading h2 {
  color: #fff;
  font-family: Georgia, 'Times New Roman', 'Microsoft YaHei', serif;
  font-size: 34px;
  font-weight: 500;
  line-height: 1.2;
}

.login-heading p {
  margin-top: 7px;
  color: #edf4ed;
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
  color: #ebf5ed;
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
  color: #314643;
  background: #f7faf5;
  letter-spacing: .02em;
}

.login-submit.el-button:hover:not(:disabled) {
  background: #e7f0e8;
  box-shadow: 0 10px 22px rgb(18 31 25 / 24%);
  transform: translateY(-2px);
}

.login-submit.el-button:active:not(:disabled),
.preview-button.el-button:active:not(:disabled) {
  transform: translateY(0);
}

.preview-button.el-button {
  margin-top: 10px;
  color: #f5faf6;
  border-color: rgb(245 252 246 / 58%);
  background: rgb(255 255 255 / 7%);
}

.preview-button.el-button:hover:not(:disabled) {
  border-color: rgb(255 255 255 / 85%);
  background: rgb(255 255 255 / 15%);
  box-shadow: 0 8px 18px rgb(17 27 21 / 18%);
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
  border-top: 1px solid rgb(243 251 244 / 28%);
  color: #e1eee3;
  font-size: 10px;
}

:deep(.el-form-item) { margin-bottom: 18px; }

:deep(.el-form-item__label) {
  padding-bottom: 7px;
  color: #f3faf4;
  font-size: 12px;
  font-weight: 660;
}

:deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 5px;
  background: rgb(255 255 255 / 7%);
  box-shadow: 0 0 0 1px rgb(244 252 244 / 70%) inset;
  transition: box-shadow .18s, background-color .18s;
}

:deep(.el-input__wrapper.is-focus) {
  background: rgb(255 255 255 / 13%);
  box-shadow: 0 0 0 2px #fff inset !important;
}

:deep(.el-form-item__content) { overflow: visible; }

:deep(.el-input__inner) {
  color: #fff;
  font-size: 14px;
}

:deep(.el-input__prefix) { color: #e2f0e4; }

:deep(.el-input__inner:-webkit-autofill),
:deep(.el-input__inner:-webkit-autofill:hover),
:deep(.el-input__inner:-webkit-autofill:focus) {
  -webkit-text-fill-color: #fff;
  box-shadow: 0 0 0 1000px #486861 inset;
  caret-color: #fff;
  transition: background-color 9999s ease-out 0s;
}

:deep(.el-checkbox) {
  --el-checkbox-text-color: #f0f7f1;
  --el-checkbox-checked-text-color: #fff;
  --el-checkbox-input-border-color: rgb(244 252 244 / 72%);
  --el-checkbox-checked-bg-color: #f7faf5;
  --el-checkbox-checked-input-border-color: #f7faf5;
  --el-checkbox-checked-icon-color: #31554c;
}

@media (max-width: 1080px) {
  .login-page { grid-template-columns: minmax(420px, 1fr) 400px; }
  .login-panel { padding-inline: 42px; }
}

@media (max-width: 880px) {
  .login-page { grid-template-columns: 1fr; }

  .brand-panel { min-height: auto; padding: 38px 28px 36px; }
  .brand-copy { margin: 42px 0 0; }
  .brand-copy h1 { font-size: 36px; }

  .login-panel {
    min-height: auto;
    padding: 32px 22px 48px;
    border-left: 0;
    border-top: 1px solid rgb(255 230 207 / 12%);
  }
}

@media (max-width: 600px) {
  .brand-copy p { display: none; }

  .brand-copy { margin: 42px 0 0; }
  .brand-copy h1 { font-size: 30px; }
  .login-card { padding: 30px 22px 24px; }
}

@keyframes background-breathe {
  from { filter: brightness(.92) saturate(.88) contrast(.97); transform: scale(1.08); }
  to { filter: brightness(.99) saturate(.96) contrast(.98); transform: scale(1.02); }
}

@keyframes glow-drift {
  from { transform: translate3d(0, 0, 0) scale(1); }
  to { transform: translate3d(28px, 18px, 0) scale(1.12); }
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
  .brand-panel::before,
  .brand-panel::after,
  .brand-shell,
  .login-card { animation: none; }
}
</style>
