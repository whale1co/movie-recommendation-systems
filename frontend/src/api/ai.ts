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
