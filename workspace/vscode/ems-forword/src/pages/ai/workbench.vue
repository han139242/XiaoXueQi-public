<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessageBox } from 'element-plus'
import AppIcon from '@/components/common/AppIcon.vue'
import { chatWithAi, getAiStatus, getDashboardInsight, getModuleSuggestion } from '@/api/ai'
import { isFrameworkPreview } from '@/utils/auth'

const STORAGE_KEY = 'mes_ai_workbench_conversations_v1'
const previewMode = isFrameworkPreview()
const conversationScroll = ref()
const composer = ref()
const question = ref('')
const sending = ref(false)
const loadingStatus = ref(false)
const activeConversationId = ref('')
const status = ref({
  enabled: false,
  configured: false,
  available: false,
  state: 'DISABLED',
  provider: 'DEEPSEEK',
  model: null
})
let abortController

const capabilities = [
  {
    id: 'general',
    label: '业务问答',
    shortLabel: '通用问答',
    icon: 'bot',
    endpoint: 'chat',
    module: 'general',
    description: '围绕当前业务问题进行只读分析和解释。',
    dataScope: '仅结合问题和受控上下文，不额外读取业务明细',
    quickPrompts: ['今天有哪些需要优先关注的运营风险？', '请帮我梳理这个问题需要核对哪些业务数据。']
  },
  {
    id: 'dashboard',
    label: '运营看板解读',
    shortLabel: '看板解读',
    icon: 'dashboard',
    endpoint: 'dashboard',
    module: 'dashboard',
    description: '汇总生产、质量、库存和设备状态，识别优先事项。',
    dataScope: '生产、质量、库存和设备的后端汇总数据',
    quickPrompts: ['解读当前运营看板，列出最需要处理的三项风险。', '生产、质量、库存和设备之间有哪些需要联动关注的问题？']
  },
  {
    id: 'scheduling',
    label: '排程风险解读',
    shortLabel: '排程建议',
    icon: 'route',
    endpoint: 'module',
    module: 'production',
    description: '解释产能、交期和资源风险，不调整或确认排程。',
    dataScope: '生产汇总与趋势；具体方案明细请在排产工作台核对',
    quickPrompts: ['请说明当前生产的交期、产能和排程风险，不要修改排程。', '哪些生产风险应该优先在排产工作台中核对？']
  },
  {
    id: 'maintenance',
    label: '设备保养建议',
    shortLabel: '设备保养',
    icon: 'wrench',
    endpoint: 'module',
    module: 'equipment',
    description: '基于设备状态给出保养核查建议，不创建正式计划。',
    dataScope: '设备运行、停机、故障和保养状态汇总',
    quickPrompts: ['根据当前设备状态，给出本周保养核查优先级。', '哪些设备异常可能影响近期生产，请说明核查顺序。']
  },
  {
    id: 'quality',
    label: '质量风险分析',
    shortLabel: '质量分析',
    icon: 'shield-check',
    endpoint: 'module',
    module: 'quality',
    description: '解释质量统计变化，辅助确定核查方向。',
    dataScope: '后端质量统计汇总数据',
    quickPrompts: ['分析当前质量风险，并给出优先核查方向。', '质量数据中有哪些异常趋势需要跟进？']
  },
  {
    id: 'inventory',
    label: '库存异常分析',
    shortLabel: '库存分析',
    icon: 'warehouse',
    endpoint: 'module',
    module: 'inventory',
    description: '识别库存预警及其可能对生产的影响。',
    dataScope: '库存预警相关的后端汇总数据',
    quickPrompts: ['当前库存预警可能对生产造成哪些影响？', '请给出需要优先核对的库存异常清单。']
  },
  {
    id: 'writing',
    label: '业务智能写作',
    shortLabel: '业务写作',
    icon: 'clipboard',
    endpoint: 'chat',
    module: 'general',
    description: '生成日报、交接摘要和通知草稿，供人工修改确认。',
    dataScope: '仅使用您在对话中提供的信息，不自动下发或保存业务单据',
    quickPrompts: ['请根据我接下来提供的信息，生成一份班组交接摘要草稿。', '请帮我整理一份生产异常跟进通知草稿。']
  }
]

function capabilityById(id) {
  return capabilities.find((item) => item.id === id) || capabilities[0]
}

function createConversation(capabilityId = 'general') {
  const now = dayjs().toISOString()
  return {
    id: `conversation-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    title: '新对话',
    capabilityId,
    createdAt: now,
    updatedAt: now,
    archived: false,
    messages: []
  }
}

function loadConversations() {
  try {
    const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
    if (!Array.isArray(stored)) return [createConversation()]
    const valid = stored
      .filter((item) => item && typeof item === 'object' && Array.isArray(item.messages))
      .slice(0, 24)
      .map((item) => ({
        ...item,
        title: item.title || '未命名对话',
        capabilityId: capabilityById(item.capabilityId).id,
        archived: Boolean(item.archived),
        messages: item.messages.slice(-60)
      }))
    let draftKept = false
    const compacted = valid.filter((item) => {
      if (item.messages.length) return true
      if (item.archived || draftKept) return false
      draftKept = true
      return true
    })
    return compacted.length ? compacted : [createConversation()]
  } catch {
    return [createConversation()]
  }
}

const conversations = ref(loadConversations())
const activeConversation = computed(() => conversations.value.find((item) => item.id === activeConversationId.value) || conversations.value[0])
const activeCapability = computed(() => capabilityById(activeConversation.value?.capabilityId))
const recentConversations = computed(() => conversations.value
  .filter((item) => !item.archived && item.messages.length > 0)
  .sort((left, right) => dayjs(right.updatedAt).valueOf() - dayjs(left.updatedAt).valueOf()))
const archivedConversations = computed(() => conversations.value
  .filter((item) => item.archived && item.messages.length > 0)
  .sort((left, right) => dayjs(right.updatedAt).valueOf() - dayjs(left.updatedAt).valueOf()))
const showWelcome = computed(() => !activeConversation.value?.messages?.length)
const statusLabel = computed(() => {
  if (status.value.state === 'READY') return '服务就绪'
  if (status.value.state === 'MISCONFIGURED') return '服务配置不完整'
  return '服务未启用'
})
const statusTone = computed(() => {
  if (status.value.state === 'READY') return 'ready'
  if (status.value.state === 'MISCONFIGURED') return 'warning'
  return 'offline'
})
const inputPlaceholder = computed(() => status.value.available
  ? activeCapability.value.quickPrompts[0]
  : 'AI 服务暂不可用，请先检查服务状态')

function persistConversations() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(conversations.value.slice(0, 24)))
  } catch {
    // 浏览器存储不可用时，当前会话仍可正常使用。
  }
}

function ensureActiveConversation() {
  if (!activeConversation.value) {
    const next = createConversation()
    conversations.value.unshift(next)
    activeConversationId.value = next.id
  }
  return activeConversation.value
}

function newConversation(capabilityId = 'general') {
  let draft = conversations.value.find((item) => !item.archived && item.messages.length === 0)
  if (!draft) {
    draft = createConversation(capabilityId)
    conversations.value.unshift(draft)
  } else {
    draft.title = '新对话'
    draft.capabilityId = capabilityId
    draft.updatedAt = dayjs().toISOString()
  }
  activeConversationId.value = draft.id
  question.value = ''
  persistConversations()
  nextTick(() => composer.value?.focus?.())
}

function selectConversation(id) {
  const target = conversations.value.find((item) => item.id === id)
  if (!target) return
  if (target.archived) target.archived = false
  activeConversationId.value = id
  question.value = ''
  persistConversations()
  scrollToLatest()
}

function selectCapability(id) {
  const conversation = ensureActiveConversation()
  if (conversation.messages.length) {
    newConversation(id)
  } else {
    conversation.capabilityId = id
    conversation.updatedAt = dayjs().toISOString()
    persistConversations()
  }
  question.value = ''
  nextTick(() => composer.value?.focus?.())
}

function selectQuickPrompt(prompt) {
  question.value = prompt
  nextTick(() => composer.value?.focus?.())
}

function archiveConversation(id) {
  const conversation = conversations.value.find((item) => item.id === id)
  if (!conversation) return
  conversation.archived = true
  conversation.updatedAt = dayjs().toISOString()
  if (activeConversationId.value === id) newConversation()
  persistConversations()
}

async function removeConversation(id) {
  const conversation = conversations.value.find((item) => item.id === id)
  if (!conversation) return
  try {
    await ElMessageBox.confirm(`确定删除“${conversation.title}”吗？删除后无法恢复。`, '删除会话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  conversations.value = conversations.value.filter((item) => item.id !== id)
  if (!conversations.value.length) conversations.value.push(createConversation())
  if (activeConversationId.value === id) activeConversationId.value = conversations.value[0].id
  persistConversations()
}

function restoreConversation(id) {
  const conversation = conversations.value.find((item) => item.id === id)
  if (!conversation) return
  conversation.archived = false
  conversation.updatedAt = dayjs().toISOString()
  activeConversationId.value = id
  persistConversations()
}

function conversationTime(value) {
  const time = dayjs(value)
  if (!time.isValid()) return ''
  return time.isSame(dayjs(), 'day') ? time.format('HH:mm') : time.format('MM-DD')
}

function resolveAiErrorMessage(error) {
  const detail = error?.data?.message
  if (typeof detail === 'string' && detail.trim()) return detail.trim()
  if (typeof error?.displayMessage === 'string' && error.displayMessage.trim()) return error.displayMessage.trim()
  return '本次分析未完成，请稍后重试。'
}

async function loadStatus() {
  if (previewMode) {
    status.value = { ...status.value, available: false, state: 'DISABLED' }
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

function requestAi(capability, payload, signal) {
  if (capability.endpoint === 'dashboard') return getDashboardInsight(payload, signal)
  if (capability.endpoint === 'module') return getModuleSuggestion(payload, signal)
  return chatWithAi(payload, signal)
}

async function sendQuestion() {
  const content = question.value.trim()
  const conversation = ensureActiveConversation()
  if (!content || sending.value || !status.value.available || !conversation) return

  const capability = activeCapability.value
  const sentAt = dayjs()
  conversation.messages.push({
    id: `message-${Date.now()}`,
    role: 'user',
    content,
    time: sentAt.format('HH:mm')
  })
  if (conversation.title === '新对话') conversation.title = content.slice(0, 24)
  conversation.updatedAt = sentAt.toISOString()
  question.value = ''
  sending.value = true
  persistConversations()
  scrollToLatest()
  abortController = new AbortController()

  try {
    const result = await requestAi(capability, {
      question: content,
      module: capability.module,
      context: {
        source: 'ai-workbench',
        capability: capability.id
      }
    }, abortController.signal)
    const answer = result.data || {}
    conversation.messages.push({
      id: `message-${Date.now()}-answer`,
      role: 'assistant',
      content: answer.answer || '暂时没有生成可用内容。',
      suggestions: Array.isArray(answer.suggestions) ? answer.suggestions : [],
      disclaimer: answer.disclaimer || '智能建议，仅供参考；关键业务操作需人工确认。',
      model: answer.model || status.value.model,
      dataScope: capability.dataScope,
      generatedAt: answer.generatedAt || dayjs().toISOString(),
      time: dayjs(answer.generatedAt || undefined).format('HH:mm')
    })
  } catch (error) {
    if (error.code !== 'ERR_CANCELED') {
      conversation.messages.push({
        id: `message-${Date.now()}-error`,
        role: 'system',
        content: resolveAiErrorMessage(error),
        retryQuestion: content,
        time: dayjs().format('HH:mm')
      })
    }
  } finally {
    conversation.updatedAt = dayjs().toISOString()
    sending.value = false
    abortController = null
    persistConversations()
    scrollToLatest()
  }
}

function cancelRequest() {
  abortController?.abort()
}

function retryMessage(message) {
  if (sending.value || !status.value.available || !message.retryQuestion) return
  question.value = message.retryQuestion
  sendQuestion()
}

function handleKeydown(event) {
  if (event.isComposing || event.shiftKey || event.key !== 'Enter') return
  event.preventDefault()
  sendQuestion()
}

function scrollToLatest() {
  nextTick(() => {
    const wrap = conversationScroll.value?.wrapRef || conversationScroll.value?.$el?.querySelector('.el-scrollbar__wrap')
    if (wrap) wrap.scrollTop = wrap.scrollHeight
  })
}

onMounted(() => {
  activeConversationId.value = recentConversations.value[0]?.id || conversations.value[0]?.id || ''
  persistConversations()
  loadStatus()
})

onBeforeUnmount(() => {
  cancelRequest()
})
</script>

<template>
  <section class="ai-workbench">
    <div class="workbench-frame">
      <aside class="conversation-sidebar">
        <div class="sidebar-header">
          <el-button class="new-conversation" plain :disabled="sending" @click="newConversation()">
            <AppIcon name="edit" :size="16" />新建对话
          </el-button>
        </div>

        <div class="sidebar-section function-section">
          <div class="section-caption">AI 功能</div>
          <div class="capability-list">
            <button
              v-for="capability in capabilities"
              :key="capability.id"
              class="capability-item"
              :class="{ active: activeCapability.id === capability.id }"
              type="button"
              :disabled="sending"
              @click="selectCapability(capability.id)"
            >
              <span class="capability-icon"><AppIcon :name="capability.icon" :size="16" /></span>
              <span>{{ capability.shortLabel }}</span>
            </button>
          </div>
        </div>

        <div class="sidebar-section history-section">
          <div class="section-caption">会话记录</div>
          <el-scrollbar class="conversation-history">
            <div v-if="recentConversations.length" class="session-list">
              <div
                v-for="conversation in recentConversations"
                :key="conversation.id"
                class="session-row"
                :class="{ active: activeConversationId === conversation.id }"
              >
                <button class="session-main" type="button" @click="selectConversation(conversation.id)">
                  <span class="session-title">{{ conversation.title }}</span>
                  <small>{{ conversationTime(conversation.updatedAt) }}</small>
                </button>
                <div class="session-actions">
                  <el-tooltip content="归档会话" placement="top">
                    <button type="button" aria-label="归档会话" @click="archiveConversation(conversation.id)">
                      <AppIcon name="package" :size="13" />
                    </button>
                  </el-tooltip>
                  <el-tooltip content="删除会话" placement="top">
                    <button type="button" aria-label="删除会话" @click="removeConversation(conversation.id)">
                      <AppIcon name="trash" :size="13" />
                    </button>
                  </el-tooltip>
                </div>
              </div>
            </div>
            <div v-else class="history-empty">还没有对话记录</div>

            <template v-if="archivedConversations.length">
              <div class="archive-caption">已归档</div>
              <div class="session-list archived-list">
                <div v-for="conversation in archivedConversations" :key="conversation.id" class="session-row">
                  <button class="session-main" type="button" @click="selectConversation(conversation.id)">
                    <span class="session-title">{{ conversation.title }}</span>
                    <small>{{ conversationTime(conversation.updatedAt) }}</small>
                  </button>
                  <div class="session-actions">
                    <el-tooltip content="恢复会话" placement="top">
                      <button type="button" aria-label="恢复会话" @click="restoreConversation(conversation.id)">
                        <AppIcon name="refresh" :size="13" />
                      </button>
                    </el-tooltip>
                    <el-tooltip content="删除会话" placement="top">
                      <button type="button" aria-label="删除会话" @click="removeConversation(conversation.id)">
                        <AppIcon name="trash" :size="13" />
                      </button>
                    </el-tooltip>
                  </div>
                </div>
              </div>
            </template>
          </el-scrollbar>
        </div>
      </aside>

      <section class="chat-pane">
        <div v-if="!status.available" class="service-notice" :class="`is-${statusTone}`">
          <AppIcon name="alert" :size="17" />
          <div>
            <strong>{{ statusLabel }}</strong>
            <span>{{ previewMode ? '当前为框架预览，不会调用模型服务。' : '请检查后端 AI 开关和模型配置后重试。' }}</span>
          </div>
          <el-button link type="primary" @click="loadStatus">重新检查</el-button>
        </div>

        <el-scrollbar ref="conversationScroll" class="message-scroll">
          <main class="conversation-main">
            <section v-if="showWelcome" class="welcome-screen">
              <span class="welcome-avatar"><AppIcon name="bot" :size="28" /></span>
              <h2>你好，我是羽球智造 AI</h2>
              <p>选择业务场景后开始提问，我会提供可核对的分析、建议或文字草稿。</p>
              <div class="welcome-prompts">
                <button
                  v-for="prompt in activeCapability.quickPrompts"
                  :key="prompt"
                  type="button"
                  @click="selectQuickPrompt(prompt)"
                >{{ prompt }}</button>
              </div>
            </section>

            <section v-else class="message-list">
              <article
                v-for="message in activeConversation.messages"
                :key="message.id"
                class="chat-message"
                :class="`is-${message.role}`"
              >
                <span v-if="message.role !== 'user'" class="message-avatar">
                  <AppIcon :name="message.role === 'system' ? 'alert' : 'bot'" :size="16" />
                </span>
                <div class="message-body">
                  <p>{{ message.content }}</p>
                  <ul v-if="message.suggestions?.length" class="suggestion-list">
                    <li v-for="suggestion in message.suggestions" :key="suggestion">{{ suggestion }}</li>
                  </ul>
                  <button v-if="message.retryQuestion" class="retry-button" type="button" @click="retryMessage(message)">
                    <AppIcon name="refresh" :size="13" />重新尝试
                  </button>
                  <div class="message-meta">
                    <span>{{ message.time }}</span>
                    <span v-if="message.model">{{ message.model }}</span>
                  </div>
                </div>
              </article>

              <article v-if="sending" class="chat-message is-assistant">
                <span class="message-avatar"><AppIcon name="bot" :size="16" /></span>
                <div class="thinking-indicator"><i /><i /><i /><span>正在分析</span></div>
              </article>
            </section>
          </main>
        </el-scrollbar>

        <footer class="compose-area">
          <div class="compose-box" :class="{ disabled: !status.available }">
            <el-input
              ref="composer"
              v-model="question"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 5 }"
              maxlength="500"
              resize="none"
              :disabled="!status.available || sending"
              :placeholder="inputPlaceholder"
              @keydown="handleKeydown"
            />
            <div class="compose-actions">
              <span>{{ question.length }}/500</span>
              <el-tooltip :content="sending ? '停止生成' : '发送问题'" placement="top">
                <el-button
                  v-if="sending"
                  class="send-button"
                  type="danger"
                  circle
                  aria-label="停止生成"
                  @click="cancelRequest"
                ><AppIcon name="close" :size="16" /></el-button>
                <el-button
                  v-else
                  class="send-button"
                  type="primary"
                  circle
                  aria-label="发送问题"
                  :disabled="!status.available || !question.trim()"
                  @click="sendQuestion"
                ><AppIcon name="arrow" :size="16" /></el-button>
              </el-tooltip>
            </div>
          </div>
        </footer>
      </section>
    </div>
  </section>
</template>

<style scoped>
.ai-workbench {
  width: 100%;
  min-height: calc(100dvh - var(--mes-header-height) - var(--mes-tabs-height) - 36px);
}

.workbench-frame {
  display: grid;
  min-height: max(620px, calc(100dvh - var(--mes-header-height) - var(--mes-tabs-height) - 36px));
  grid-template-columns: 252px minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid var(--mes-border);
  border-radius: var(--mes-radius);
  background: var(--mes-surface);
  box-shadow: var(--mes-shadow-sm);
}

.conversation-sidebar {
  display: flex;
  min-width: 0;
  flex-direction: column;
  border-right: 1px solid var(--mes-border);
  background: #f7f9fb;
}

.workspace-brand {
  display: flex;
  height: 76px;
  flex: none;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid var(--mes-border);
  background: var(--mes-surface);
}

.workspace-mark,
.heading-icon,
.capability-card-icon,
.message-avatar {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
}

.workspace-mark {
  width: 37px;
  height: 37px;
  color: #fff;
  border: 1px solid rgb(var(--mes-primary-rgb) / 40%);
  border-radius: 5px;
  background: var(--mes-primary);
}

.workspace-brand > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.workspace-brand strong {
  color: var(--mes-text);
  font-size: 15px;
  font-weight: 680;
}

.workspace-brand span:not(.workspace-mark) {
  margin-top: 2px;
  color: var(--mes-text-muted);
  font-size: 10px;
}

.new-conversation.el-button {
  width: calc(100% - 28px);
  height: 36px;
  gap: 6px;
  margin: 13px 14px 8px;
}

.sidebar-section {
  padding: 10px 10px 0;
}

.section-caption,
.archive-caption {
  padding: 0 8px 7px;
  color: var(--mes-text-muted);
  font-size: 10px;
  font-weight: 700;
}

.capability-list {
  display: grid;
  gap: 2px;
}

.capability-item {
  display: flex;
  width: 100%;
  height: 35px;
  align-items: center;
  gap: 9px;
  padding: 0 8px;
  color: var(--mes-text-secondary);
  border: 0;
  border-radius: 4px;
  background: transparent;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
}

.capability-item:hover {
  color: var(--mes-primary);
  background: var(--mes-primary-soft);
}

.capability-item.active {
  color: var(--mes-primary);
  background: #e3f1fc;
  font-weight: 650;
}

.capability-icon {
  display: inline-flex;
  width: 21px;
  height: 21px;
  flex: none;
  align-items: center;
  justify-content: center;
  color: var(--mes-text-muted);
}

.capability-item.active .capability-icon {
  color: var(--mes-primary);
}

.history-section {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  padding-top: 18px;
  padding-bottom: 10px;
}

.conversation-history {
  min-height: 0;
  flex: 1;
  padding: 0 2px;
}

.session-list {
  display: grid;
  gap: 2px;
}

.session-row {
  display: grid;
  min-width: 0;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  margin: 0 4px;
  border-radius: 4px;
}

.session-row:hover,
.session-row.active {
  background: var(--mes-surface);
}

.session-main {
  display: grid;
  min-width: 0;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 4px;
  padding: 8px 4px 8px 8px;
  color: var(--mes-text-secondary);
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.session-row.active .session-main {
  color: var(--mes-text);
  font-weight: 620;
}

.session-title {
  overflow: hidden;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-main small {
  color: var(--mes-text-muted);
  font-size: 9px;
}

.session-actions {
  display: flex;
  align-items: center;
  padding-right: 5px;
  opacity: 0;
}

.session-row:hover .session-actions,
.session-row.active .session-actions {
  opacity: 1;
}

.session-actions button {
  display: inline-flex;
  width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  color: var(--mes-text-muted);
  border: 0;
  border-radius: 3px;
  background: transparent;
  cursor: pointer;
}

.session-actions button:hover {
  color: var(--mes-primary);
  background: var(--mes-primary-soft);
}

.session-actions button:last-child:hover {
  color: var(--mes-danger);
  background: var(--mes-danger-soft);
}

.archive-caption {
  margin-top: 15px;
}

.archived-list {
  opacity: .74;
}

.history-empty {
  padding: 20px 8px;
  color: var(--mes-text-muted);
  font-size: 11px;
  text-align: center;
}

.chat-pane {
  display: grid;
  min-width: 0;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
}

.chat-header {
  display: flex;
  min-height: 76px;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 0 24px;
  border-bottom: 1px solid var(--mes-border);
}

.chat-heading {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.heading-icon {
  width: 36px;
  height: 36px;
  color: var(--mes-primary);
  border: 1px solid var(--mes-primary-border);
  border-radius: 5px;
  background: var(--mes-primary-soft);
}

.chat-heading h1 {
  color: var(--mes-text);
  font-size: 16px;
  font-weight: 680;
}

.chat-heading p {
  overflow: hidden;
  margin-top: 3px;
  color: var(--mes-text-secondary);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-header-actions {
  display: flex;
  flex: none;
  align-items: center;
  gap: 7px;
}

.service-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--mes-text-muted);
  font-size: 11px;
}

.service-status i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #96a1ad;
}

.service-status.is-ready { color: var(--mes-success); }
.service-status.is-ready i { background: var(--mes-success); }
.service-status.is-warning { color: var(--mes-warning); }
.service-status.is-warning i { background: var(--mes-warning); }

.header-action-button.el-button {
  width: 32px;
  height: 32px;
  padding: 0;
  color: var(--mes-text-muted);
}

.service-notice {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 10px 24px;
  color: #846524;
  border-bottom: 1px solid #ead8b7;
  background: #fff8e9;
}

.service-notice > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.service-notice strong { font-size: 12px; }
.service-notice span { margin-top: 2px; color: #9a7b45; font-size: 10px; }

.message-scroll {
  min-height: 0;
  background: #fbfcfd;
}

.conversation-main {
  width: min(100%, 1050px);
  min-height: 100%;
  margin: 0 auto;
  padding: 28px 32px;
}

.welcome-view {
  max-width: 900px;
  margin: 6px auto 0;
}

.welcome-copy { max-width: 610px; }

.welcome-kicker {
  color: var(--mes-primary);
  font-size: 11px;
  font-weight: 700;
}

.welcome-copy h2 {
  margin-top: 6px;
  color: var(--mes-text);
  font-size: 23px;
  font-weight: 680;
  line-height: 1.25;
}

.welcome-copy p {
  margin-top: 9px;
  color: var(--mes-text-secondary);
  font-size: 13px;
  line-height: 1.75;
}

.capability-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 24px;
}

.capability-card {
  display: grid;
  min-height: 128px;
  grid-template-rows: auto auto 1fr;
  align-content: start;
  gap: 7px;
  padding: 15px;
  color: var(--mes-text-secondary);
  border: 1px solid var(--mes-border);
  border-radius: 5px;
  background: var(--mes-surface);
  text-align: left;
  cursor: pointer;
}

.capability-card:hover,
.capability-card.active {
  border-color: var(--mes-primary-border);
  box-shadow: 0 3px 11px rgb(var(--mes-primary-rgb) / 9%);
}

.capability-card.active { background: #f3f9fe; }

.capability-card-icon {
  width: 32px;
  height: 32px;
  color: var(--mes-primary);
  border: 1px solid var(--mes-primary-border);
  border-radius: 4px;
  background: var(--mes-primary-soft);
}

.capability-card strong { color: var(--mes-text); font-size: 13px; font-weight: 650; }
.capability-card > span:last-child { font-size: 11px; line-height: 1.55; }

.scope-note {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 18px;
  padding: 11px 12px;
  color: var(--mes-text-secondary);
  border-left: 3px solid var(--mes-primary);
  background: var(--mes-primary-soft);
  font-size: 11px;
  line-height: 1.55;
}

.scope-note > .app-icon { margin-top: 1px; color: var(--mes-primary); }

.message-list {
  display: grid;
  max-width: 820px;
  gap: 20px;
  margin: 0 auto;
}

.chat-message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.chat-message.is-user { justify-content: flex-end; }

.message-avatar {
  width: 30px;
  height: 30px;
  color: var(--mes-primary);
  border: 1px solid var(--mes-primary-border);
  border-radius: 50%;
  background: var(--mes-surface);
}

.is-system .message-avatar {
  color: var(--mes-danger);
  border-color: #f0c8c5;
  background: var(--mes-danger-soft);
}

.message-body { max-width: min(82%, 680px); }

.message-body > p {
  padding: 12px 14px;
  color: var(--mes-text);
  border: 1px solid var(--mes-border);
  border-radius: 3px 8px 8px 8px;
  background: var(--mes-surface);
  font-size: 13px;
  line-height: 1.75;
  white-space: pre-wrap;
}

.is-user .message-body > p {
  color: #fff;
  border-color: var(--mes-primary);
  border-radius: 8px 3px 8px 8px;
  background: var(--mes-primary);
}

.is-system .message-body > p {
  color: #9a4c47;
  border-color: #f0c8c5;
  background: #fff3f2;
}

.suggestion-list {
  margin: 7px 0 0;
  padding: 10px 13px 10px 29px;
  color: var(--mes-text-secondary);
  border: 1px solid var(--mes-border);
  border-radius: 4px;
  background: var(--mes-surface);
  font-size: 12px;
  line-height: 1.65;
}

.suggestion-list li + li { margin-top: 3px; }

.response-context {
  display: flex;
  flex-wrap: wrap;
  gap: 7px 14px;
  margin-top: 7px;
  color: var(--mes-text-muted);
  font-size: 10px;
}

.response-context span:first-child {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.message-meta {
  display: flex;
  gap: 9px;
  margin-top: 4px;
  color: var(--mes-text-muted);
  font-size: 10px;
}

.is-user .message-meta { justify-content: flex-end; }
.message-body > small { display: block; margin-top: 5px; color: var(--mes-text-muted); font-size: 10px; }

.retry-button {
  display: inline-flex;
  height: 27px;
  align-items: center;
  gap: 5px;
  margin-top: 7px;
  padding: 0 9px;
  color: var(--mes-primary);
  border: 1px solid var(--mes-primary-border);
  border-radius: 3px;
  background: var(--mes-surface);
  font-size: 11px;
  cursor: pointer;
}

.thinking-indicator {
  display: flex;
  height: 40px;
  align-items: center;
  gap: 5px;
  padding: 0 13px;
  color: var(--mes-text-secondary);
  border: 1px solid var(--mes-border);
  border-radius: 3px 8px 8px 8px;
  background: var(--mes-surface);
  font-size: 11px;
}

.thinking-indicator i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--mes-primary);
  animation: thinking 1.1s ease-in-out infinite;
}

.thinking-indicator i:nth-child(2) { animation-delay: .15s; }
.thinking-indicator i:nth-child(3) { animation-delay: .3s; }
.thinking-indicator span { margin-left: 2px; }

.compose-area {
  padding: 11px 24px 13px;
  border-top: 1px solid var(--mes-border);
  background: var(--mes-surface);
}

.compose-context {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 9px;
  margin-bottom: 8px;
  color: var(--mes-text-muted);
  font-size: 10px;
}

.compose-context span:first-child {
  display: inline-flex;
  flex: none;
  align-items: center;
  gap: 4px;
  color: var(--mes-primary);
  font-weight: 650;
}

.compose-context span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-prompts {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  margin-bottom: 8px;
  scrollbar-width: none;
}

.quick-prompts::-webkit-scrollbar { display: none; }

.quick-prompts button {
  height: 27px;
  flex: none;
  padding: 0 10px;
  color: var(--mes-text-secondary);
  border: 1px solid var(--mes-border);
  border-radius: 14px;
  background: var(--mes-surface-muted);
  font-size: 10px;
  cursor: pointer;
}

.quick-prompts button:hover { color: var(--mes-primary); border-color: var(--mes-primary-border); }
.quick-prompts button:disabled { color: var(--mes-text-muted); opacity: .65; cursor: not-allowed; }

.compose-box {
  position: relative;
  border: 1px solid var(--mes-border-strong);
  border-radius: var(--mes-radius);
  background: var(--mes-surface);
}

.compose-box:focus-within { border-color: var(--mes-primary); box-shadow: 0 0 0 3px rgb(var(--mes-primary-rgb) / 9%); }
.compose-box.disabled { background: var(--mes-surface-muted); }

.compose-box :deep(.el-textarea__inner) {
  min-height: 76px !important;
  padding: 11px 13px 34px;
  border: 0;
  background: transparent;
  box-shadow: none;
  font-size: 12px;
  line-height: 1.65;
}

.compose-actions {
  position: absolute;
  right: 8px;
  bottom: 7px;
  left: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  pointer-events: none;
}

.compose-actions > span { color: var(--mes-text-muted); font-size: 10px; }
.send-button.el-button { width: 29px; height: 29px; pointer-events: auto; }

.operation-notice {
  margin-top: 7px;
  color: var(--mes-text-muted);
  font-size: 10px;
  text-align: center;
}

@keyframes thinking {
  0%, 60%, 100% { transform: translateY(0); opacity: .45; }
  30% { transform: translateY(-3px); opacity: 1; }
}

@media (max-width: 1100px) {
  .workbench-frame { grid-template-columns: 218px minmax(0, 1fr); }
  .capability-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .conversation-main { padding: 24px; }
}

@media (max-width: 780px) {
  .ai-workbench { min-height: auto; }
  .workbench-frame { min-height: calc(100dvh - var(--mes-header-height) - var(--mes-tabs-height) - 20px); grid-template-columns: 1fr; }
  .conversation-sidebar { display: none; }
  .chat-header { min-height: 66px; padding: 0 15px; }
  .chat-heading p { max-width: 190px; }
  .service-status { font-size: 10px; }
  .conversation-main { padding: 20px 15px; }
  .capability-grid { grid-template-columns: 1fr; margin-top: 18px; }
  .capability-card { min-height: 104px; grid-template-columns: auto minmax(0, 1fr); grid-template-rows: auto 1fr; column-gap: 11px; }
  .capability-card-icon { grid-row: span 2; }
  .capability-card > span:last-child { grid-column: 2; }
  .compose-area { padding: 10px 14px 12px; }
  .message-body { max-width: 88%; }
  .service-notice { padding: 10px 15px; }
}
.ai-workbench {
  height: calc(100dvh - var(--mes-header-height) - var(--mes-tabs-height) - 36px);
  min-height: 0;
  overflow: hidden;
}

.workbench-frame {
  height: 100%;
  min-height: 0;
  grid-template-columns: 220px minmax(0, 1fr);
  border-color: #e4e8ed;
  border-radius: var(--mes-radius);
  box-shadow: none;
}

.conversation-sidebar {
  background: var(--mes-surface);
}

.sidebar-header {
  flex: none;
  padding: 12px 12px 8px;
  border-bottom: 1px solid #edf0f3;
}

.new-conversation.el-button {
  width: 100%;
  height: 34px;
  gap: 6px;
  margin: 0;
  color: var(--mes-primary);
  border: 1px dashed var(--mes-primary);
  border-radius: 6px;
  background: var(--mes-primary-soft);
  font-size: 12px;
}

.new-conversation.el-button:hover {
  color: var(--mes-primary-hover);
  border-color: var(--mes-primary-hover);
  background: #dceefd;
}

.history-section {
  padding: 14px 8px 8px;
}

.history-section .section-caption {
  padding: 0 8px 6px;
}

.session-row {
  margin: 0;
  border-radius: 6px;
}

.session-row:hover,
.session-row.active {
  background: #f4f7fa;
}

.session-row.active {
  background: var(--mes-primary-soft);
}

.session-main {
  padding: 9px 4px 9px 9px;
  font-size: 12px;
}

.chat-pane {
  display: flex;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  background: #f5f6f8;
}

.chat-header {
  min-height: 58px;
  flex: none;
  padding: 0 22px;
  border-bottom-color: #e5e9ed;
  background: var(--mes-surface);
}

.heading-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
}

.chat-heading h1 {
  font-size: 14px;
}

.chat-heading p {
  margin-top: 1px;
  font-size: 10px;
}

.message-scroll {
  min-height: 0;
  flex: 1;
  background: #f5f6f8;
}

.message-scroll :deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}

.conversation-main {
  display: flex;
  width: 100%;
  min-height: 100%;
  flex-direction: column;
  padding: 18px 28px 12px;
}

.welcome-screen {
  display: flex;
  min-height: 100%;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px 20px 42px;
  color: var(--mes-text);
  text-align: center;
}

.welcome-avatar {
  display: inline-flex;
  width: 58px;
  height: 58px;
  align-items: center;
  justify-content: center;
  color: #fff;
  border-radius: 50%;
  background: var(--mes-primary);
  box-shadow: 0 5px 16px rgb(var(--mes-primary-rgb) / 24%);
}

.welcome-screen h2 {
  margin-top: 15px;
  font-size: 21px;
  font-weight: 650;
}

.welcome-screen > p {
  max-width: 510px;
  margin-top: 8px;
  color: var(--mes-text-muted);
  font-size: 12px;
}

.welcome-prompts {
  display: flex;
  max-width: 620px;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 18px;
}

.welcome-prompts button {
  max-width: min(100%, 300px);
  padding: 8px 11px;
  color: var(--mes-text-secondary);
  border: 1px solid #dce2e8;
  border-radius: 6px;
  background: var(--mes-surface);
  font-size: 11px;
  line-height: 1.5;
  text-align: left;
  cursor: pointer;
}

.welcome-prompts button:hover {
  color: var(--mes-primary);
  border-color: var(--mes-primary-border);
  background: var(--mes-primary-soft);
}

.message-list {
  width: 100%;
  max-width: 900px;
  gap: 3px;
}

.chat-message {
  padding: 7px 0;
}

.message-body {
  max-width: 72%;
}

.message-body > p {
  padding: 10px 13px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.7;
}

.is-user .message-body > p {
  border-radius: 8px 3px 8px 8px;
}

.is-assistant .message-body > p {
  border-radius: 3px 8px 8px 8px;
  box-shadow: 0 1px 4px rgb(32 42 54 / 6%);
}

.message-avatar {
  width: 34px;
  height: 34px;
  color: #fff;
  border: 0;
  background: var(--mes-primary);
}

.is-system .message-avatar {
  color: var(--mes-danger);
  border: 1px solid #f0c8c5;
  background: var(--mes-danger-soft);
}

.response-context {
  margin-top: 6px;
}

.compose-area {
  flex: none;
  padding: 9px 28px 12px;
  border-top: 0;
  background: #f5f6f8;
}

.compose-context {
  max-width: 820px;
  margin: 0 auto 7px;
}

.compose-context span:last-child {
  color: var(--mes-text-muted);
}

.scene-select {
  width: 138px;
  flex: none;
}

.scene-select :deep(.el-select__wrapper) {
  min-height: 26px;
  border-radius: 4px;
  box-shadow: 0 0 0 1px var(--mes-border) inset;
}

.quick-prompts {
  max-width: 820px;
  margin: 0 auto 7px;
}

.compose-box {
  max-width: 820px;
  margin: 0 auto;
  border: 1.5px solid #d5dce4;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgb(32 42 54 / 5%);
}

.compose-box :deep(.el-textarea__inner) {
  min-height: 44px !important;
  padding: 10px 44px 10px 12px;
  font-size: 13px;
  line-height: 1.55;
}

.compose-actions {
  right: 8px;
  bottom: 8px;
  left: auto;
}

.compose-actions > span {
  display: none;
}

.send-button.el-button {
  width: 30px;
  height: 30px;
}

.operation-notice {
  max-width: 820px;
  margin: 5px auto 0;
  color: #9ca6b0;
}

@media (max-width: 780px) {
  .ai-workbench {
    height: calc(100dvh - var(--mes-header-height) - var(--mes-tabs-height) - 20px);
  }

  .workbench-frame {
    height: 100%;
    min-height: 0;
  }

  .chat-header {
    min-height: 54px;
  }

  .conversation-main {
    padding: 15px 13px 8px;
  }

  .compose-area {
    padding: 8px 12px 10px;
  }

  .message-body {
    max-width: 82%;
  }

  .compose-context span:last-child {
    display: none;
  }
}

.function-section {
  flex: none;
  padding: 12px 10px 10px;
  border-bottom: 1px solid #edf0f3;
}

.function-section .section-caption {
  padding-bottom: 6px;
}

.function-section .capability-list {
  gap: 1px;
}

.function-section .capability-item {
  height: 32px;
  border-radius: 5px;
  font-size: 12px;
}

.function-section .capability-item:disabled {
  opacity: .58;
  cursor: not-allowed;
}

.history-section {
  padding-top: 12px;
}

.conversation-main {
  padding-bottom: 6px;
}

.compose-area {
  padding: 10px 28px 12px;
  background: #f5f6f8;
}

.compose-box {
  display: flex;
  max-width: 820px;
  align-items: flex-end;
  gap: 10px;
  padding: 7px 9px 7px 14px;
  border: 1.5px solid #dcdfe6;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 12px rgb(32 42 54 / 5%);
}

.compose-box:focus-within {
  border-color: var(--mes-primary);
  box-shadow: 0 2px 12px rgb(var(--mes-primary-rgb) / 12%);
}

.compose-box :deep(.el-textarea) {
  min-width: 0;
  flex: 1;
}

.compose-box :deep(.el-textarea__inner) {
  min-height: 28px !important;
  padding: 4px 0;
  border: 0 !important;
  outline: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
  font-size: 14px;
  line-height: 1.6;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.compose-box :deep(.el-textarea__inner:hover),
.compose-box :deep(.el-textarea__inner:focus),
.compose-box :deep(.el-textarea__inner:focus-visible) {
  border: 0 !important;
  outline: 0 !important;
  box-shadow: none !important;
}

.compose-box :deep(.el-textarea__inner::-webkit-scrollbar) {
  width: 0;
  height: 0;
}

.compose-actions {
  position: static;
  display: flex;
  flex: none;
  pointer-events: auto;
}

.send-button.el-button {
  width: 36px;
  height: 36px;
  margin: 0;
  box-shadow: none;
}

.operation-notice,
.compose-context,
.quick-prompts {
  display: none;
}

@media (max-width: 780px) {
  .compose-area {
    padding: 8px 12px 10px;
  }
}

/* RuoYi-inspired chat surface: restrained blue accents and a calm working canvas. */
.ai-workbench {
  --ai-blue: #409eff;
  --ai-blue-hover: #2f8ff0;
  --ai-blue-soft: #ecf5ff;
  --ai-canvas: #f5f7fa;
  --ai-sidebar: #fafbfc;
  --ai-line: #e7eaf0;
  --ai-text: #303133;
  --ai-muted: #909399;
}

.workbench-frame {
  grid-template-columns: 236px minmax(0, 1fr);
  border-color: var(--ai-line);
  border-radius: 8px;
  background: var(--ai-canvas);
  box-shadow: 0 6px 24px rgb(31 45 61 / 7%);
}

.conversation-sidebar {
  border-right-color: var(--ai-line);
  background: var(--ai-sidebar);
}

.sidebar-header {
  padding: 14px 14px 10px;
  border-bottom: 0;
}

.new-conversation.el-button {
  height: 38px;
  color: #fff;
  border: 1px solid var(--ai-blue);
  border-radius: 6px;
  background: var(--ai-blue);
  box-shadow: 0 4px 12px rgb(64 158 255 / 18%);
  transition: background-color .2s ease, border-color .2s ease, box-shadow .2s ease, transform .2s ease;
}

.new-conversation.el-button:hover,
.new-conversation.el-button:focus-visible {
  color: #fff;
  border-color: var(--ai-blue-hover);
  background: var(--ai-blue-hover);
  box-shadow: 0 6px 16px rgb(64 158 255 / 24%);
  transform: translateY(-1px);
}

.function-section {
  padding: 12px 10px 13px;
  border-bottom-color: var(--ai-line);
}

.section-caption,
.archive-caption {
  color: #a1a7b0;
  font-size: 11px;
  font-weight: 600;
}

.function-section .capability-list {
  gap: 3px;
}

.function-section .capability-item {
  position: relative;
  height: 36px;
  padding: 0 10px;
  color: #606266;
  border-radius: 6px;
  font-size: 12px;
  transition: color .18s ease, background-color .18s ease, box-shadow .18s ease, transform .18s ease;
}

.function-section .capability-item:hover {
  color: var(--ai-blue);
  background: #f0f6fc;
  transform: translateX(2px);
}

.function-section .capability-item.active {
  color: var(--ai-blue);
  background: #fff;
  box-shadow: 0 2px 10px rgb(31 45 61 / 7%);
}

.function-section .capability-item.active::before {
  position: absolute;
  top: 9px;
  bottom: 9px;
  left: 0;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--ai-blue);
  content: '';
}

.capability-icon {
  color: #8d96a3;
  transition: color .18s ease, transform .18s ease;
}

.capability-item:hover .capability-icon,
.capability-item.active .capability-icon {
  color: var(--ai-blue);
  transform: scale(1.06);
}

.history-section {
  padding: 14px 8px 10px;
}

.session-list {
  gap: 3px;
}

.session-row {
  border: 1px solid transparent;
  border-radius: 6px;
  transition: border-color .18s ease, background-color .18s ease, transform .18s ease;
}

.session-row:hover {
  border-color: #edf0f4;
  background: #fff;
  transform: translateX(2px);
}

.session-row.active {
  border-color: #d9ecff;
  background: var(--ai-blue-soft);
  transform: none;
}

.session-main {
  padding: 9px 4px 9px 10px;
}

.session-title {
  font-size: 12px;
}

.session-main small {
  font-size: 10px;
}

.session-actions {
  transition: opacity .18s ease;
}

.chat-pane,
.message-scroll,
.compose-area {
  background: var(--ai-canvas);
}

.service-notice {
  margin: 12px 20px 0;
  padding: 9px 12px;
  border: 1px solid #faecd8;
  border-radius: 6px;
  background: #fdf6ec;
}

.message-scroll {
  position: relative;
}

.message-scroll::before {
  position: absolute;
  inset: 0 0 auto;
  height: 1px;
  background: #eef0f4;
  content: '';
  pointer-events: none;
}

.conversation-main {
  width: min(100%, 980px);
  padding: 22px 32px 10px;
}

.welcome-screen {
  padding-bottom: 54px;
  animation: ai-welcome-in .5s ease-out both;
}

.welcome-avatar {
  position: relative;
  width: 64px;
  height: 64px;
  border: 6px solid #fff;
  background: var(--ai-blue);
  box-shadow: 0 8px 24px rgb(64 158 255 / 22%);
  animation: ai-avatar-float 4s ease-in-out infinite;
}

.welcome-avatar::after {
  position: absolute;
  inset: -12px;
  border: 1px solid rgb(64 158 255 / 24%);
  border-radius: 50%;
  content: '';
  animation: ai-avatar-pulse 2.8s ease-out infinite;
}

.welcome-screen h2 {
  margin-top: 20px;
  color: var(--ai-text);
  font-size: 24px;
  font-weight: 600;
}

.welcome-screen > p {
  max-width: 540px;
  margin-top: 10px;
  color: var(--ai-muted);
  font-size: 13px;
  line-height: 1.7;
}

.welcome-prompts {
  display: grid;
  width: min(100%, 650px);
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 24px;
}

.welcome-prompts button {
  min-height: 50px;
  max-width: none;
  padding: 10px 13px;
  color: #606266;
  border-color: #e2e6ec;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 2px 8px rgb(31 45 61 / 4%);
  font-size: 12px;
  transition: color .2s ease, border-color .2s ease, box-shadow .2s ease, transform .2s ease;
}

.welcome-prompts button:hover,
.welcome-prompts button:focus-visible {
  color: var(--ai-blue);
  border-color: #b3d8ff;
  background: #fff;
  box-shadow: 0 7px 18px rgb(64 158 255 / 12%);
  transform: translateY(-2px);
}

.message-list {
  max-width: 860px;
  gap: 6px;
}

.chat-message {
  gap: 11px;
  padding: 8px 0;
  animation: ai-message-in .28s ease-out both;
}

.message-avatar {
  width: 34px;
  height: 34px;
  color: #fff;
  border: 4px solid #fff;
  background: var(--ai-blue);
  box-shadow: 0 3px 10px rgb(64 158 255 / 16%);
}

.message-body {
  max-width: min(76%, 680px);
}

.message-body > p {
  padding: 11px 14px;
  color: var(--ai-text);
  border-color: #e5e8ee;
  border-radius: 4px 8px 8px;
  background: #fff;
  box-shadow: 0 2px 8px rgb(31 45 61 / 5%);
}

.is-user .message-body > p {
  border-color: var(--ai-blue);
  border-radius: 8px 4px 8px 8px;
  background: var(--ai-blue);
  box-shadow: 0 4px 12px rgb(64 158 255 / 16%);
}

.suggestion-list {
  border-color: #e5e8ee;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 2px 8px rgb(31 45 61 / 4%);
}

.thinking-indicator {
  border-color: #e5e8ee;
  border-radius: 4px 8px 8px;
  background: #fff;
  box-shadow: 0 2px 8px rgb(31 45 61 / 5%);
}

.thinking-indicator i {
  background: var(--ai-blue);
}

.compose-area {
  position: relative;
  padding: 10px 32px 18px;
}

.compose-area::before {
  position: absolute;
  inset: 0 0 auto;
  height: 18px;
  background: linear-gradient(to bottom, rgb(245 247 250 / 0%), var(--ai-canvas));
  content: '';
  pointer-events: none;
  transform: translateY(-18px);
}

.compose-box {
  max-width: 860px;
  min-height: 52px;
  padding: 7px 8px 7px 15px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 5px 20px rgb(31 45 61 / 8%);
  transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease;
}

.compose-box:hover {
  border-color: #c6cbd3;
}

.compose-box:focus-within {
  border-color: var(--ai-blue);
  box-shadow: 0 7px 24px rgb(64 158 255 / 14%);
  transform: translateY(-1px);
}

.compose-box :deep(.el-textarea__inner) {
  color: var(--ai-text);
  font-size: 14px;
}

.compose-box :deep(.el-textarea__inner::placeholder) {
  color: #b0b4bb;
}

.send-button.el-button {
  width: 36px;
  height: 36px;
  border-color: var(--ai-blue);
  background: var(--ai-blue);
  transition: background-color .18s ease, border-color .18s ease, box-shadow .18s ease, transform .18s ease;
}

.send-button.el-button:not(:disabled):hover {
  border-color: var(--ai-blue-hover);
  background: var(--ai-blue-hover);
  box-shadow: 0 4px 10px rgb(64 158 255 / 22%);
  transform: scale(1.04);
}

.send-button.el-button.is-disabled {
  border-color: #c0c4cc;
  background: #c0c4cc;
}

@keyframes ai-welcome-in {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes ai-avatar-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

@keyframes ai-avatar-pulse {
  0% { opacity: .65; transform: scale(.9); }
  70%, 100% { opacity: 0; transform: scale(1.18); }
}

@keyframes ai-message-in {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1100px) {
  .workbench-frame { grid-template-columns: 218px minmax(0, 1fr); }
  .conversation-main { padding-right: 24px; padding-left: 24px; }
}

@media (max-width: 780px) {
  .workbench-frame {
    grid-template-columns: minmax(0, 1fr);
    border-radius: 6px;
  }

  .conversation-main {
    padding: 16px 13px 6px;
  }

  .welcome-screen {
    justify-content: flex-start;
    padding: 54px 8px 30px;
  }

  .welcome-avatar {
    width: 56px;
    height: 56px;
  }

  .welcome-screen h2 {
    font-size: 21px;
  }

  .welcome-prompts {
    grid-template-columns: minmax(0, 1fr);
  }

  .message-body {
    max-width: 84%;
  }

  .compose-area {
    padding: 8px 12px 12px;
  }

  .compose-box {
    min-height: 50px;
  }

  .service-notice {
    margin: 8px 10px 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .welcome-screen,
  .welcome-avatar,
  .welcome-avatar::after,
  .chat-message,
  .thinking-indicator i {
    animation: none !important;
  }

  .new-conversation.el-button,
  .capability-item,
  .session-row,
  .welcome-prompts button,
  .compose-box,
  .send-button.el-button {
    transition: none !important;
  }
}
</style>
