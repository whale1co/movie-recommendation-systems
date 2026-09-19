import request from '../utils/request'

export function addRating(movieId: number, score: number) {
  return request.post('/ratings', { movieId, score })
}

export function updateRating(id: number, score: number) {
  return request.put(`/ratings/${id}`, { score })
}

export function deleteRating(id: number) {
  return request.delete(`/ratings/${id}`)
}

export function getUserRatings() {
  return request.get('/user/ratings')
}
