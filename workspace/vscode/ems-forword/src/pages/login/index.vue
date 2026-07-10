<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppIcon from '@/components/common/AppIcon.vue'
import { login } from '@/api/auth'
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
  <main class="login-page">
    <section class="login-visual" aria-label="羽毛球制造执行系统">
      <div class="visual-overlay" />
      <div class="visual-brand">
        <span class="brand-mark" aria-hidden="true">
          <span class="brand-feather" />
          <span class="brand-base" />
        </span>
        <div>
          <strong>羽球智造</strong>
          <span>MES</span>
        </div>
      </div>

      <div class="visual-copy">
        <span class="visual-kicker">BADMINTON MANUFACTURING</span>
        <h1>制造执行与质量追溯平台</h1>
        <p>连接订单、工艺、生产、质量、仓储和设备，形成可追踪的制造现场。</p>
        <div class="visual-capabilities" aria-label="核心业务域">
          <span>生产执行</span>
          <span>质量闭环</span>
          <span>批次追溯</span>
        </div>
      </div>
    </section>

    <section class="login-panel">
      <div class="mobile-brand">
        <span class="brand-mark" aria-hidden="true">
          <span class="brand-feather" />
          <span class="brand-base" />
        </span>
        <strong>羽球智造 MES</strong>
      </div>

      <div class="login-form-wrap">
        <div class="login-heading">
          <span class="login-eyebrow">MES CONTROL CENTER</span>
          <h2>欢迎登录</h2>
          <p>使用已授权的系统账号进入制造运营中心</p>
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
              <template #prefix><AppIcon name="user" :size="17" /></template>
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
              <template #prefix><AppIcon name="lock" :size="17" /></template>
            </el-input>
          </el-form-item>

          <div class="login-options">
            <el-checkbox v-model="form.remember">保持登录</el-checkbox>
            <span>仅限授权人员使用</span>
          </div>

          <el-button class="login-submit" type="primary" :loading="loading" @click="handleLogin">
            进入系统
            <AppIcon v-if="!loading" name="arrow" :size="17" />
          </el-button>

          <el-button v-if="showPreview" class="preview-button" plain @click="enterPreview">
            查看基础框架
          </el-button>
        </el-form>

        <footer class="login-footer">
          <span>Badminton MES</span>
          <span>v1.0</span>
        </footer>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  min-height: 100dvh;
  grid-template-columns: minmax(520px, 58%) minmax(420px, 42%);
  background: var(--mes-surface);
}

.login-visual {
  position: relative;
  min-height: 100dvh;
  overflow: hidden;
  color: #fff;
  background: url('@/assets/login-badminton.jpg') center / cover no-repeat;
}

.visual-overlay {
  position: absolute;
  inset: 0;
  background: rgb(13 27 22 / 68%);
}

.visual-brand,
.visual-copy {
  position: absolute;
  z-index: 1;
}

.visual-brand {
  top: 42px;
  left: 48px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.visual-brand > div {
  display: flex;
  align-items: baseline;
  gap: 9px;
}

.visual-brand strong {
  font-size: 20px;
  font-weight: 680;
}

.visual-brand > div > span {
  color: rgb(255 255 255 / 65%);
  font-size: 12px;
  font-weight: 700;
}

.brand-mark {
  position: relative;
  display: inline-flex;
  width: 34px;
  height: 34px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgb(255 255 255 / 38%);
  border-radius: 50%;
}

.brand-feather {
  position: absolute;
  top: 7px;
  width: 11px;
  height: 14px;
  border: 2px solid currentColor;
  border-bottom: 0;
  border-radius: 50% 50% 35% 35%;
  transform: rotate(-10deg);
}

.brand-base {
  position: absolute;
  bottom: 7px;
  width: 13px;
  height: 5px;
  border: 2px solid currentColor;
  border-radius: 50%;
}

.visual-copy {
  right: 9%;
  bottom: 10%;
  left: 8%;
  max-width: 680px;
}

.visual-kicker {
  display: block;
  margin-bottom: 15px;
  color: #9ee3c7;
  font-size: 12px;
  font-weight: 750;
}

.visual-copy h1 {
  max-width: 620px;
  font-size: 40px;
  font-weight: 680;
  line-height: 1.25;
  letter-spacing: 0;
}

.visual-copy p {
  max-width: 580px;
  margin-top: 18px;
  color: rgb(255 255 255 / 74%);
  font-size: 15px;
  line-height: 1.8;
}

.visual-capabilities {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-top: 32px;
  color: rgb(255 255 255 / 88%);
  font-size: 13px;
}

.visual-capabilities span {
  position: relative;
  padding-left: 14px;
}

.visual-capabilities span::before {
  position: absolute;
  top: 7px;
  left: 0;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #5dd0a5;
  content: '';
}

.login-panel {
  display: flex;
  min-height: 100dvh;
  align-items: center;
  justify-content: center;
  padding: 48px clamp(38px, 5vw, 82px);
}

.mobile-brand {
  display: none;
}

.login-form-wrap {
  width: min(100%, 410px);
}

.login-heading {
  margin-bottom: 32px;
}

.login-eyebrow {
  display: block;
  margin-bottom: 10px;
  color: var(--mes-primary);
  font-size: 11px;
  font-weight: 750;
}

.login-heading h2 {
  color: var(--mes-text);
  font-size: 30px;
  font-weight: 680;
  line-height: 1.3;
}

.login-heading p {
  margin-top: 9px;
  color: var(--mes-text-secondary);
  font-size: 14px;
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -2px 0 22px;
  color: var(--mes-text-muted);
  font-size: 12px;
}

.login-submit.el-button,
.preview-button.el-button {
  width: 100%;
  height: 44px;
  margin: 0;
}

.login-submit.el-button {
  gap: 8px;
}

.preview-button.el-button {
  margin-top: 12px;
  color: var(--mes-text-secondary);
}

.login-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 36px;
  padding-top: 18px;
  border-top: 1px solid var(--mes-border);
  color: var(--mes-text-muted);
  font-size: 11px;
}

:deep(.el-form-item) {
  margin-bottom: 22px;
}

:deep(.el-form-item__label) {
  padding-bottom: 7px;
  color: var(--mes-text);
  font-size: 13px;
  font-weight: 600;
}

:deep(.el-input__wrapper) {
  min-height: 44px;
  box-shadow: 0 0 0 1px var(--mes-border) inset;
}

@media (max-width: 980px) {
  .login-page {
    grid-template-columns: minmax(360px, 48%) minmax(400px, 52%);
  }

  .visual-copy h1 {
    font-size: 32px;
  }
}

@media (max-width: 760px) {
  .login-page {
    display: block;
    min-height: 100dvh;
  }

  .login-visual {
    display: none;
  }

  .login-panel {
    min-height: 100dvh;
    align-items: flex-start;
    padding: 28px 24px 36px;
  }

  .mobile-brand {
    display: flex;
    position: absolute;
    top: 24px;
    left: 24px;
    align-items: center;
    gap: 10px;
    color: var(--mes-primary);
  }

  .mobile-brand .brand-mark {
    width: 30px;
    height: 30px;
    border-color: rgb(8 127 91 / 35%);
  }

  .mobile-brand strong {
    color: var(--mes-text);
    font-size: 16px;
  }

  .login-form-wrap {
    margin: auto 0;
    padding-top: 76px;
  }
}
</style>
