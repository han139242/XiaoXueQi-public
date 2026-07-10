export const OPEN_AI_ASSISTANT_EVENT = 'mes:open-ai-assistant'

export function openAiAssistant() {
  window.dispatchEvent(new CustomEvent(OPEN_AI_ASSISTANT_EVENT))
}
