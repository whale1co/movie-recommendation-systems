import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { useUserStore } from './user'
import type { AiAdvisorResult } from '../api/ai'

interface PersistedAdvisorState {
  question: string
  lastQuestion: string
  result: AiAdvisorResult | null
  selectedHistoryId: number | null
}

export const useAiAdvisorStore = defineStore('aiAdvisor', () => {
  const question = ref('')
  const lastQuestion = ref('')
  const result = ref<AiAdvisorResult | null>(null)
  const selectedHistoryId = ref<number | null>(null)
  const userStore = useUserStore()
  const storageKey = computed(() => userStore.userId ? `movie-rec:ai-advisor:${userStore.userId}` : '')

  function persist() {
    if (!storageKey.value) return
    const state: PersistedAdvisorState = {
      question: question.value,
      lastQuestion: lastQuestion.value,
      result: result.value,
      selectedHistoryId: selectedHistoryId.value
    }
    sessionStorage.setItem(storageKey.value, JSON.stringify(state))
  }

  function hydrate() {
    if (!storageKey.value) return
    try {
      const raw = sessionStorage.getItem(storageKey.value)
      if (!raw) return
      const state = JSON.parse(raw) as Partial<PersistedAdvisorState>
      question.value = state.question || ''
      lastQuestion.value = state.lastQuestion || ''
      result.value = state.result || null
      selectedHistoryId.value = state.selectedHistoryId ?? null
    } catch {
      sessionStorage.removeItem(storageKey.value)
    }
  }

  function setResult(nextQuestion: string, nextResult: AiAdvisorResult, historyId: number | null = null) {
    question.value = nextQuestion
    lastQuestion.value = nextQuestion
    result.value = nextResult
    selectedHistoryId.value = historyId
    persist()
  }

  function clear() {
    if (storageKey.value) sessionStorage.removeItem(storageKey.value)
    question.value = ''
    lastQuestion.value = ''
    result.value = null
    selectedHistoryId.value = null
  }

  return { question, lastQuestion, result, selectedHistoryId, hydrate, persist, setResult, clear }
})