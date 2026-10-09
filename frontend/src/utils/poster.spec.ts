import { describe, expect, it } from 'vitest'
import { defaultPosterUrl, resolvePosterUrl } from './poster'

describe('resolvePosterUrl', () => {
  it('keeps an API-provided poster URL', () => {
    expect(resolvePosterUrl({ title: 'Movie', posterUrl: 'https://cdn.example/poster.jpg' }))
      .toBe('https://cdn.example/poster.jpg')
  })

  it('uses the known local fallback after trimming the title', () => {
    expect(resolvePosterUrl({ title: '  \u8001\u53cb\u8bb0  ', posterUrl: null }))
      .toBe('/api/posters/\u8001\u53cb\u8bb0.jpg')
  })

  it('uses the default poster for an unknown or missing movie', () => {
    expect(resolvePosterUrl({ title: 'Unknown' })).toBe(defaultPosterUrl)
    expect(resolvePosterUrl()).toBe(defaultPosterUrl)
  })
})
