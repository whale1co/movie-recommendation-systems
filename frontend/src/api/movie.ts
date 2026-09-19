import request from '../utils/request'

export function searchMovies(q: string, page: number = 1, size: number = 10) {
  return request.get('/movies/search', { params: { q, page, size } })
}

export function getMovieDetail(id: number) {
  return request.get(`/movies/${id}`)
}

export function getHotMovies(limit: number = 20) {
  return request.get('/movies/hot', { params: { limit } })
}

export function getColdStart() {
  return request.get('/movies/cold-start')
}

export function getRecommendations() {
  return request.get('/recommendations')
}

export function getMovieList(page: number = 1, size: number = 20, genre?: string) {
  return request.get('/movies/list', { params: { page, size, genre } })
}
