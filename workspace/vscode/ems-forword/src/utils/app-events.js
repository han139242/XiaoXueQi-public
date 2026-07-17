// 跨组件事件名称统一放在这里，避免发送方和监听方各自手写字符串造成拼写不一致。
export const OPEN_AI_ASSISTANT_EVENT = 'mes:open-ai-assistant'

// 任意组件调用该函数都会在 window 上广播“打开 AI 助手”事件；布局组件负责监听并跳转。
export function openAiAssistant() {
  window.dispatchEvent(new CustomEvent(OPEN_AI_ASSISTANT_EVENT))
}
