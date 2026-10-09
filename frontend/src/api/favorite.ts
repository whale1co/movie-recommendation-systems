import request from '../utils/request'

export function addFavorite(movieId: number) {
  return request.post('/favorites', { movieId })
}

export function removeFavorite(movieId: number) {
  return request.delete(`/favorites/${movieId}`)
}

export function getUserFavorites() {
  return request.get('/users/me/favorites')
}
