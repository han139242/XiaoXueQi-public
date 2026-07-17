<script setup>
import { computed, ref, watch } from 'vue'
import dayjs from 'dayjs'
import AppIcon from '@/components/common/AppIcon.vue'
import { chatWithAi, getAiStatus, getDashboardInsight, getModuleSuggestion } from '@/api/ai'
import { isFrameworkPreview } from '@/utils/auth'

// 这是各业务页面可复用的侧边AI助手；open由父组件v-model控制，module决定AI分析上下文。
const open = defineModel({ type: Boolean, default: false })
// module由父页面传入，决定AI使用通用、看板还是具体业务模块接口。
const props = defineProps({
  module: { type: String, default: 'general' }
})

// status描述后端开关、密钥配置和模型可用性；messages保存当前抽屉中的临时对话。
const previewMode = isFrameworkPreview()
const loadingStatus = ref(false)
const sending = ref(false)
const question = ref('')
const status = ref({
  enabled: false,
  configured: false,
  available: false,
  state: 'DISABLED',
  provider: 'DEEPSEEK',
  model: null
})
const messages = ref([
  {
    id: 1,
    role: 'assistant',
    content: '你好，我是羽球智造 AI 助手。可以协助解读生产、质量、库存和设备数据。',
    time: dayjs().format('HH:mm')
  }
])
let abortController

// 快捷问题只是把常见问题填入输入框，最终仍调用与手工输入相同的sendQuestion流程。
const quickQuestions = [
  '解读今日生产情况',
  '分析当前质量风险',
  '检查库存与设备异常'
]

// 将后端AI状态编码转换成用户能看到的服务状态文字。
const statusLabel = computed(() => {
  if (status.value.state === 'READY') return '服务就绪'
  if (status.value.state === 'MISCONFIGURED') return '配置不完整'
  return '服务未启用'
})

// 将AI状态转换为CSS色调名称，用于正常、警告和离线三种视觉样式。
const statusTone = computed(() => {
  if (status.value.state === 'READY') return 'ready'
  if (status.value.state === 'MISCONFIGURED') return 'warning'
  return 'offline'
})

// 服务可用时提示输入问题，不可用时明确禁止发送。
const inputPlaceholder = computed(() => {
  if (status.value.available) return '输入需要分析的问题'
  return 'AI 服务尚未可用'
})

// 统一清洗父组件传入的模块名称，避免大小写和空格影响接口选择。
const normalizedModule = computed(() => String(props.module || 'general').trim().toLowerCase())
// 仪表盘、具体业务模块和普通聊天使用三个不同后端提示词接口。
const dashboardContext = computed(() => normalizedModule.value === 'dashboard')
// 除dashboard和general外的名称都按具体业务模块分析。
const moduleContext = computed(() => !['dashboard', 'general'].includes(normalizedModule.value))

// 查询后端AI开关和模型配置，并更新抽屉顶部的服务状态。
async function loadStatus() {
  // 打开抽屉时查询AI服务状态；开发预览明确不调用真实模型。
  if (previewMode) {
    status.value = {
      enabled: false,
      configured: false,
      available: false,
      state: 'DISABLED',
      provider: 'DEEPSEEK',
      model: null
    }
    return
  }

  loadingStatus.value = true
  try {
    const result = await getAiStatus()
    status.value = result.data || status.value
  } catch {
    status.value = { ...status.value, available: false, state: 'DISABLED' }
  } finally {
    loadingStatus.value = false
  }
}

// 点击快捷问题后写入输入框；服务可用时直接发送。
function selectQuickQuestion(value) {
  question.value = value
  if (status.value.available) {
    sendQuestion()
  }
}

// 从后端详情、Axios显示消息和统一兜底中选择最适合用户的AI错误说明。
function resolveAiErrorMessage(error) {
  // 优先显示后端业务错误，其次使用Axios拦截器整理的错误，最后使用通用文案。
  const detail = error?.data?.message
  if (typeof detail === 'string' && detail.trim()) {
    return detail.trim()
  }
  if (typeof error?.displayMessage === 'string' && error.displayMessage.trim()) {
    return error.displayMessage.trim()
  }
  return '本次分析未完成，请稍后重试。'
}

// 将用户问题加入消息列表，选择对应AI接口，并把回答或错误追加回会话。
async function sendQuestion() {
  // 防止空问题、重复提交和服务不可用时发送请求。
  const content = question.value.trim()
  if (!content || sending.value || !status.value.available) {
    return
  }

  messages.value.push({
    id: Date.now(),
    role: 'user',
    content,
    time: dayjs().format('HH:mm')
  })
  question.value = ''
  sending.value = true
  // 每次提问创建独立AbortController，用户可在模型生成过程中点击停止。
  abortController = new AbortController()

  try {
    // 根据module选择对应AI接口，signal传给Axios用于取消本次HTTP请求。
    const requestAi = dashboardContext.value
      ? getDashboardInsight
      : moduleContext.value ? getModuleSuggestion : chatWithAi
    const result = await requestAi({
      question: content,
      module: normalizedModule.value
    }, abortController.signal)
    const answer = result.data || {}
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: answer.answer || '暂时没有生成可用内容。',
      suggestions: Array.isArray(answer.suggestions) ? answer.suggestions : [],
      disclaimer: answer.disclaimer || '智能建议，仅供参考',
      model: answer.model,
      time: dayjs(answer.generatedAt || undefined).format('HH:mm')
    })
  } catch (error) {
    if (error.code !== 'ERR_CANCELED') {
      messages.value.push({
        id: Date.now() + 1,
        role: 'system',
        content: resolveAiErrorMessage(error),
        retryQuestion: content,
        time: dayjs().format('HH:mm')
      })
    }
  } finally {
    sending.value = false
    abortController = null
  }
}

// 主动终止当前AbortController关联的Axios请求。
function cancelRequest() {
  // abort后Axios抛出ERR_CANCELED，catch中不会把它当成真正错误显示。
  abortController?.abort()
}

// 把失败消息保存的原问题重新放入输入框并再次提交。
function retryMessage(message) {
  if (sending.value || !status.value.available || !message.retryQuestion) return
  question.value = message.retryQuestion
  sendQuestion()
}

// 监听抽屉开关：打开时刷新服务状态，关闭时停止仍在生成的回答。
watch(open, (value) => {
  // 打开时检查服务，关闭抽屉时取消仍在生成的请求。
  if (value) {
    loadStatus()
  } else if (sending.value) {
    cancelRequest()
  }
})
</script>

<template>
  <!-- AI侧边抽屉依次显示服务状态、消息列表、快捷问题和输入发送区域。 -->
  <el-drawer
    v-model="open"
    class="ai-assistant-drawer"
    size="min(448px, 100vw)"
    direction="rtl"
    :with-header="false"
    append-to-body
  >
    <section class="ai-shell">
      <header class="ai-header">
        <div class="ai-brand-mark"><AppIcon name="bot" :size="22" /></div>
        <div class="ai-heading">
          <div>
            <strong>智造 AI 助手</strong>
            <span class="ai-status" :class="`is-${statusTone}`">
              <i />{{ loadingStatus ? '检查中' : statusLabel }}
            </span>
          </div>
          <small>{{ status.model || 'DEEPSEEK' }}</small>
        </div>
        <el-button class="ai-close" text aria-label="关闭 AI 助手" @click="open = false">
          <AppIcon name="close" :size="18" />
        </el-button>
      </header>

      <div v-if="!status.available" class="ai-service-notice" :class="`is-${statusTone}`">
        <AppIcon name="alert" :size="17" />
        <div>
          <strong>{{ statusLabel }}</strong>
          <span>{{ previewMode ? '框架预览不调用模型服务' : '请检查后端 AI 开关与模型配置' }}</span>
        </div>
        <el-button link type="primary" @click="loadStatus">重新检查</el-button>
      </div>

      <main class="ai-conversation">
        <div
          v-for="message in messages"
          :key="message.id"
          class="ai-message"
          :class="`is-${message.role}`"
        >
          <div v-if="message.role !== 'user'" class="message-avatar">
            <AppIcon :name="message.role === 'system' ? 'alert' : 'bot'" :size="15" />
          </div>
          <div class="message-content">
            <p>{{ message.content }}</p>
            <button v-if="message.retryQuestion" class="message-retry" type="button" @click="retryMessage(message)">
              <AppIcon name="refresh" :size="13" />重新尝试
            </button>
            <ul v-if="message.suggestions?.length" class="message-suggestions">
              <li v-for="suggestion in message.suggestions" :key="suggestion">{{ suggestion }}</li>
            </ul>
            <div class="message-meta">
              <span>{{ message.time }}</span>
              <span v-if="message.model">{{ message.model }}</span>
            </div>
            <small v-if="message.disclaimer">{{ message.disclaimer }}</small>
          </div>
        </div>

        <div v-if="sending" class="ai-message is-assistant">
          <div class="message-avatar"><AppIcon name="bot" :size="15" /></div>
          <div class="thinking-indicator"><span /><span /><span /></div>
        </div>
      </main>

      <section class="ai-compose">
        <div class="quick-questions">
          <button
            v-for="item in quickQuestions"
            :key="item"
            type="button"
            :disabled="sending || !status.available"
            @click="selectQuickQuestion(item)"
          >
            {{ item }}
          </button>
        </div>

        <div class="compose-box" :class="{ disabled: !status.available }">
          <el-input
            v-model="question"
            type="textarea"
            :rows="3"
            resize="none"
            maxlength="500"
            :disabled="!status.available"
            :placeholder="inputPlaceholder"
            @keydown.ctrl.enter="sendQuestion"
          />
          <div class="compose-actions">
            <span>{{ question.length }}/500</span>
            <el-button
              v-if="sending"
              class="send-button"
              type="danger"
              circle
              aria-label="停止生成"
              @click="cancelRequest"
            >
              <AppIcon name="close" :size="16" />
            </el-button>
            <el-button
              v-else
              class="send-button"
              type="primary"
              circle
              aria-label="发送问题"
              :disabled="!status.available || !question.trim()"
              @click="sendQuestion"
            >
              <AppIcon name="arrow" :size="16" />
            </el-button>
          </div>
        </div>
        <p class="ai-disclaimer">智能建议仅供参考，关键业务操作需人工确认</p>
      </section>
    </section>
  </el-drawer>
</template>

<style>
/* 抽屉挂载到body外层，此处使用非scoped样式调整Element Plus抽屉容器。 */
.ai-assistant-drawer .el-drawer__body {
  padding: 0;
}

.ai-assistant-drawer.el-drawer {
  border-left: 1px solid var(--mes-border);
  background: #f4f6f8;
  box-shadow: -14px 0 36px rgb(25 34 44 / 16%);
}
</style>

<style scoped>
/* AI助手内部样式：控制消息气泡、状态提示、快捷问题和输入区域。 */
.ai-shell {
  display: grid;
  height: 100dvh;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  color: var(--mes-text);
  background: #f4f6f8;
}

.ai-header {
  display: grid;
  min-height: 72px;
  grid-template-columns: 42px minmax(0, 1fr) 34px;
  align-items: center;
  gap: 11px;
  padding: 0 16px;
  color: #eef5fb;
  background: #171d25;
}

.ai-brand-mark {
  display: inline-flex;
  width: 40px;
  height: 40px;
  align-items: center;
  justify-content: center;
  color: #c5e1f5;
  border: 1px solid rgb(112 174 224 / 34%);
  border-radius: 50%;
  background: rgb(47 111 174 / 28%);
}

.ai-heading {
  min-width: 0;
}

.ai-heading > div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-heading strong {
  font-size: 15px;
  font-weight: 680;
}

.ai-heading small {
  display: block;
  margin-top: 3px;
  color: #8a98a7;
  font-size: 9px;
  font-weight: 650;
}

.ai-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #98a5b3;
  font-size: 10px;
}

.ai-status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #778696;
}

.ai-status.is-ready {
  color: #8ed4b4;
}

.ai-status.is-ready i {
  background: var(--mes-success);
}

.ai-status.is-warning {
  color: #f0c77c;
}

.ai-status.is-warning i {
  background: #d69a35;
}

.ai-close.el-button {
  width: 34px;
  height: 34px;
  padding: 0;
  color: #9ba8b6;
}

.ai-service-notice {
  display: grid;
  min-height: 62px;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  align-items: center;
  gap: 9px;
  padding: 10px 16px;
  color: #705321;
  border-bottom: 1px solid #ead8b7;
  background: #fff8e9;
}

.ai-service-notice > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.ai-service-notice strong {
  font-size: 12px;
}

.ai-service-notice span {
  margin-top: 2px;
  overflow: hidden;
  color: #9a7b45;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-conversation {
  min-height: 0;
  overflow-y: auto;
  padding: 18px 16px 24px;
}

.ai-message {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  margin-bottom: 18px;
}

.ai-message.is-user {
  justify-content: flex-end;
}

.message-avatar {
  display: inline-flex;
  width: 28px;
  height: 28px;
  flex: none;
  align-items: center;
  justify-content: center;
  color: var(--mes-primary);
  border: 1px solid #c8d8e6;
  border-radius: 50%;
  background: #fff;
}

.message-content {
  max-width: 82%;
}

.message-content p {
  padding: 11px 13px;
  color: var(--mes-text);
  border: 1px solid var(--mes-border);
  border-radius: 3px 8px 8px 8px;
  background: #fff;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.is-user .message-content p {
  color: #fff;
  border-color: var(--mes-primary);
  border-radius: 8px 3px 8px 8px;
  background: var(--mes-primary);
}

.is-system .message-content p {
  color: #9a4c47;
  border-color: #f0c8c5;
  background: #fff3f2;
}

.message-retry {
  display: inline-flex;
  height: 26px;
  align-items: center;
  gap: 5px;
  margin-top: 6px;
  padding: 0 9px;
  color: var(--mes-primary);
  border: 1px solid var(--mes-primary-border);
  border-radius: 3px;
  background: #fff;
  font-size: 10px;
  cursor: pointer;
}

.message-retry:hover {
  border-color: var(--mes-primary);
  background: var(--mes-primary-soft);
}

.message-suggestions {
  margin: 7px 0 0;
  padding: 9px 12px 9px 28px;
  color: var(--mes-text-secondary);
  border: 1px solid var(--mes-border);
  border-radius: 4px;
  background: #fff;
  font-size: 11px;
  line-height: 1.65;
}

.message-suggestions li + li {
  margin-top: 3px;
}

.message-meta {
  display: flex;
  gap: 8px;
  margin-top: 4px;
  color: #97a3b0;
  font-size: 9px;
}

.is-user .message-meta {
  justify-content: flex-end;
}

.message-content > small {
  display: block;
  margin-top: 5px;
  color: #8a96a3;
  font-size: 9px;
}

.thinking-indicator {
  display: flex;
  min-width: 64px;
  height: 38px;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: 1px solid var(--mes-border);
  border-radius: 3px 8px 8px 8px;
  background: #fff;
}

.thinking-indicator span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #8294a6;
  animation: thinking 1.1s ease-in-out infinite;
}

.thinking-indicator span:nth-child(2) {
  animation-delay: 0.16s;
}

.thinking-indicator span:nth-child(3) {
  animation-delay: 0.32s;
}

.ai-compose {
  padding: 12px 14px 14px;
  border-top: 1px solid var(--mes-border);
  background: #fff;
}

.quick-questions {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding-bottom: 9px;
  scrollbar-width: none;
}

.quick-questions button {
  flex: none;
  height: 28px;
  padding: 0 10px;
  color: var(--mes-text-secondary);
  border: 1px solid var(--mes-border);
  border-radius: 14px;
  background: var(--mes-surface-muted);
  font-size: 10px;
  cursor: pointer;
}

.quick-questions button:hover {
  color: var(--mes-primary);
  border-color: #9bbbd8;
}

.quick-questions button:disabled {
  color: var(--mes-text-muted);
  border-color: var(--mes-border);
  opacity: .62;
  cursor: not-allowed;
}

.compose-box {
  position: relative;
  border: 1px solid var(--mes-border-strong);
  border-radius: var(--mes-radius);
  background: #fff;
}

.compose-box:focus-within {
  border-color: var(--mes-primary);
  box-shadow: 0 0 0 3px rgb(47 111 174 / 9%);
}

.compose-box.disabled {
  background: #f4f6f8;
}

.compose-box :deep(.el-textarea__inner) {
  min-height: 78px !important;
  padding: 10px 12px 32px;
  border: 0;
  background: transparent;
  box-shadow: none;
  font-size: 12px;
}

.compose-actions {
  position: absolute;
  right: 8px;
  bottom: 7px;
  left: 11px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  pointer-events: none;
}

.compose-actions > span {
  color: #9da8b4;
  font-size: 9px;
}

.send-button.el-button {
  width: 28px;
  height: 28px;
  pointer-events: auto;
}

.ai-disclaimer {
  margin-top: 8px;
  color: #98a3af;
  font-size: 9px;
  text-align: center;
}

@keyframes thinking {
  0%, 60%, 100% { transform: translateY(0); opacity: .45; }
  30% { transform: translateY(-3px); opacity: 1; }
}
</style>
