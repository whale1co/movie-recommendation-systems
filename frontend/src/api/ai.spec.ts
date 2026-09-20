import { beforeEach, describe, expect, it, vi } from 'vitest'
import request from '../utils/request'
import { askAiAdvisor } from './ai'

vi.mock('../utils/request', () => ({
  default: {
    post: vi.fn()
  }
}))

describe('askAiAdvisor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('uses the guarded advisor endpoint and its long-running request options', async () => {
    const response = { code: 200, data: { answer: 'test' } }
    vi.mocked(request.post).mockResolvedValue(response)
    const controller = new AbortController()

    await expect(askAiAdvisor('two-hour comedy', controller.signal)).resolves.toBe(response)
    expect(request.post).toHaveBeenCalledWith(
      '/ai/advisor',
      { question: 'two-hour comedy' },
      {
        signal: controller.signal,
        timeout: 70000,
        skipGlobalError: true
      }
    )
  })
})
