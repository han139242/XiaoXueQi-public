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
    <!-- ── 左侧品牌区 ── -->
    <section class="brand-panel" aria-label="羽球智造 MES">
      <div class="brand-grid" aria-hidden="true" />
      <div class="brand-shell">
        <header class="brand-header">
          <span class="brand-mark" aria-hidden="true">
            <AppIcon name="factory" :size="20" />
          </span>
          <div>
            <strong>羽球智造 MES</strong>
            <span>MANUFACTURING EXECUTION SYSTEM</span>
          </div>
        </header>

        <div class="brand-copy">
          <span class="eyebrow">企业级制造执行平台</span>
          <h1>羽毛球制造<br>执行系统</h1>
          <p>覆盖生产计划、工单执行、质量管控、仓储收发与批次追溯。</p>
        </div>

        <div class="brand-stats">
          <div>
            <strong>78</strong>
            <span>业务资源</span>
          </div>
          <div>
            <strong>4</strong>
            <span>核心闭环</span>
          </div>
          <div>
            <strong>AI</strong>
            <span>辅助分析</span>
          </div>
        </div>

        <div class="brand-meta">
          <span>BADMINTON SMART FACTORY</span>
          <span>PRODUCTION READY</span>
        </div>
      </div>
    </section>

    <!-- ── 右侧登录区 ── -->
    <section class="login-panel">
      <div class="login-card">
        <div class="login-heading">
          <span class="login-eyebrow">ACCOUNT LOGIN</span>
          <h2>登录系统</h2>
          <p>使用系统账号进入运营后台</p>
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
            <span class="demo-hint">演示账号&nbsp;admin / 123456</span>
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

        <div class="login-badge-row">
          <span><AppIcon name="shield-check" :size="14" />JWT 鉴权</span>
          <span><AppIcon name="bot" :size="14" />AI 助手</span>
          <span><AppIcon name="activity" :size="14" />实时大屏</span>
        </div>

        <footer class="login-footer">
          <span>Badminton MES</span>
          <span>© 2026 Production Ready</span>
        </footer>
      </div>
    </section>
  </main>
</template>

<style scoped>
/* ── 整体布局 ── */
.login-page {
  display: grid;
  min-height: 100dvh;
  grid-template-columns: minmax(540px, 1fr) 460px;
  overflow: hidden;
  background: #0a1018;
  font-family: var(--mes-font);
}

/* ── 左侧：品牌区 ── */
.brand-panel {
  position: relative;
  display: flex;
  min-height: 100dvh;
  align-items: center;
  padding: clamp(36px, 5vw, 76px);
  overflow: hidden;
  background: linear-gradient(145deg, #0c1829 0%, #081220 100%);
}

/* 网格线叠加 */
.brand-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgb(90 140 190 / 8%) 1px, transparent 1px),
    linear-gradient(90deg, rgb(90 140 190 / 8%) 1px, transparent 1px);
  background-size: 52px 52px;
  pointer-events: none;
}

/* 右侧渐变光晕 */
.brand-panel::after {
  position: absolute;
  top: -120px;
  right: -160px;
  width: 520px;
  height: 520px;
  border-radius: 50%;
  background: radial-gradient(circle, rgb(45 140 240 / 14%), transparent 62%);
  content: '';
  pointer-events: none;
}

.brand-shell {
  position: relative;
  z-index: 1;
  width: min(740px, 100%);
}

/* 品牌标题行 */
.brand-header {
  display: flex;
  align-items: center;
  gap: 13px;
}

.brand-mark {
  display: inline-flex;
  width: 42px;
  height: 42px;
  align-items: center;
  justify-content: center;
  color: #d4eeff;
  border: 1px solid rgb(90 148 210 / 40%);
  border-radius: 9px;
  background: linear-gradient(135deg, #1a2e40, #0d1928);
  box-shadow: 0 0 24px rgb(45 140 240 / 12%);
}

.brand-header > div {
  display: flex;
  flex-direction: column;
}

.brand-header strong {
  color: #f0f8ff;
  font-size: 20px;
  font-weight: 720;
  letter-spacing: .01em;
}

.brand-header span {
  margin-top: 2px;
  color: #5e7e98;
  font-size: 9px;
  font-weight: 760;
  letter-spacing: .14em;
}

/* 主 copy */
.brand-copy {
  margin-top: clamp(60px, 10vh, 100px);
}

.eyebrow {
  display: inline-block;
  margin-bottom: 18px;
  color: #6ab6e8;
  font-size: 12px;
  font-weight: 660;
  letter-spacing: .08em;
}

.brand-copy h1 {
  color: #f4faff;
  font-size: clamp(36px, 4.2vw, 52px);
  font-weight: 780;
  line-height: 1.1;
  letter-spacing: -.04em;
}

.brand-copy p {
  max-width: 480px;
  margin-top: 22px;
  color: #8ea9be;
  font-size: 15px;
  line-height: 1.9;
}

/* 统计栏 */
.brand-stats {
  display: flex;
  width: min(480px, 100%);
  margin-top: 46px;
  border: 1px solid rgb(90 140 200 / 18%);
  border-radius: 2px;
  background: rgb(255 255 255 / 3%);
}

.brand-stats > div {
  flex: 1;
  min-width: 0;
  padding: 18px 22px;
  border-left: 1px solid rgb(90 140 200 / 14%);
}

.brand-stats > div:first-child {
  border-left: 0;
}

.brand-stats strong {
  display: block;
  color: #e8f6ff;
  font-size: 28px;
  font-weight: 780;
  line-height: 1;
}

.brand-stats span {
  display: block;
  margin-top: 8px;
  color: #7090a8;
  font-size: 11px;
}

/* 底部信息行 */
.brand-meta {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 36px;
  color: #4a6478;
  font-size: 9px;
  font-weight: 760;
  letter-spacing: .12em;
}

/* ── 右侧：登录区 ── */
.login-panel {
  display: flex;
  min-height: 100dvh;
  align-items: center;
  justify-content: center;
  padding: 40px 48px;
  background: #f1f4f8;
  border-left: 1px solid #d4dbe4;
}

.login-card {
  width: 100%;
  max-width: 360px;
  padding: 36px 32px 28px;
  border: 1px solid #dce3eb;
  border-radius: 8px;
  background: #fff;
  box-shadow:
    0 2px 8px rgb(0 0 0 / 6%),
    0 20px 48px rgb(16 28 44 / 10%);
}

/* 卡片标题 */
.login-heading {
  margin-bottom: 28px;
}

.login-eyebrow {
  display: block;
  margin-bottom: 10px;
  color: var(--mes-primary);
  font-size: 10px;
  font-weight: 760;
  letter-spacing: .14em;
}

.login-heading h2 {
  color: #1a2736;
  font-size: 26px;
  font-weight: 740;
  line-height: 1.2;
}

.login-heading p {
  margin-top: 7px;
  color: #7d8e9e;
  font-size: 13px;
}

/* 选项行 */
.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: -4px 0 22px;
}

.demo-hint {
  overflow: hidden;
  color: #8a9aaa;
  font-size: 11px;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 按钮 */
.login-submit.el-button,
.preview-button.el-button {
  width: 100%;
  height: 44px;
  margin: 0;
  border-radius: 5px;
  font-size: 14px;
  font-weight: 660;
}

.login-submit.el-button {
  display: inline-flex;
  gap: 8px;
  border: 0;
  background: var(--mes-primary);
  letter-spacing: .02em;
}

.login-submit.el-button:hover:not(:disabled) {
  background: var(--mes-primary-hover);
}

.preview-button.el-button {
  margin-top: 10px;
  color: #596875;
  border-color: #d0d9e2;
  background: #fafbfc;
}

/* 特性徽章 */
.login-badge-row {
  display: flex;
  gap: 8px;
  margin-top: 20px;
}

.login-badge-row span {
  display: inline-flex;
  height: 30px;
  flex: 1;
  align-items: center;
  justify-content: center;
  gap: 5px;
  color: #6a7c8c;
  border: 1px solid #e2e9f0;
  border-radius: 4px;
  background: #f8fafc;
  font-size: 10px;
}

/* 底部版权 */
.login-footer {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid #edf1f5;
  color: #9aaab7;
  font-size: 10px;
}

/* ── Element Plus 覆盖 ── */
:deep(.el-form-item) { margin-bottom: 18px; }

:deep(.el-form-item__label) {
  padding-bottom: 7px;
  color: #3a4a58;
  font-size: 12px;
  font-weight: 660;
}

:deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 5px;
  background: #fff;
  box-shadow: 0 0 0 1px #d4dce6 inset;
  transition: box-shadow .18s;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1.5px var(--mes-primary) inset, 0 0 0 3px rgb(var(--mes-primary-rgb) / 10%);
}

:deep(.el-input__inner) {
  color: #1a2736;
  font-size: 14px;
}

:deep(.el-input__prefix) { color: #8a9aaa; }

:deep(.el-checkbox) {
  --el-checkbox-text-color: #6a7c8c;
  --el-checkbox-checked-text-color: var(--mes-primary);
}

/* ── 响应式 ── */
@media (max-width: 1080px) {
  .login-page { grid-template-columns: minmax(460px, 1fr) 420px; }
}

@media (max-width: 880px) {
  .login-page { grid-template-columns: 1fr; }

  .brand-panel { min-height: auto; padding: 36px 28px 32px; }
  .brand-copy { margin-top: 40px; }
  .brand-copy h1 { font-size: 36px; }

  .login-panel {
    min-height: auto;
    padding: 32px 22px 48px;
    border-left: 0;
    border-top: 1px solid #d4dbe4;
  }
}

@media (max-width: 600px) {
  .brand-header span,
  .brand-copy p,
  .brand-stats { display: none; }

  .brand-copy { margin-top: 48px; }
  .brand-copy h1 { font-size: 30px; }
  .login-card { padding: 28px 20px 22px; }
}
</style>
