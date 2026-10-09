import request from '../utils/request'

export interface MovieIntent {
  genres: string[]
  excludedGenres: string[]
  maxRuntime: number | null
  minRating: number | null
  minYear: number | null
  maxYear: number | null
  keywords: string[]
}

export interface AiMovieRecommendation {
  movieId: number
  title: string
  posterUrl: string | null
  genre: string | null
  runtime: number | null
  releaseDate: string | null
  rating: number | null
  reason: string
  matchedCriteria: string[]
  detailPath: string
}

export interface AiAdvisorResult {
  answer: string
  intent: MovieIntent
  recommendations: AiMovieRecommendation[]
  aiGenerated: boolean
  degraded: boolean
  provider: string
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  requestId: string
  timestamp: string
}

export function askAiAdvisor(question: string, signal?: AbortSignal) {
  return request.post<unknown, ApiResponse<AiAdvisorResult>>(
    '/ai/advisor',
    { question },
    {
      signal,
      timeout: 70000,
      skipGlobalError: true
    } as any
  )
}

export interface AiAdvisorHistorySummary {
  id: number
  question: string
  answerPreview: string
  recommendationCount: number
  aiGenerated: boolean
  degraded: boolean
  createdAt: string
}

export interface AiAdvisorHistoryDetail {
  id: number
  question: string
  result: AiAdvisorResult
  createdAt: string
}

export interface PageResponse<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export function getAiAdvisorHistory(page = 1, size = 20) {
  return request.get<unknown, ApiResponse<PageResponse<AiAdvisorHistorySummary>>>('/ai/advisor/history', { params: { page, size } })
}

export function getAiAdvisorHistoryDetail(id: number) {
  return request.get<unknown, ApiResponse<AiAdvisorHistoryDetail>>(`/ai/advisor/history/${id}`)
}

export function deleteAiAdvisorHistory(id: number) {
  return request.delete<unknown, ApiResponse<null>>(`/ai/advisor/history/${id}`)
}