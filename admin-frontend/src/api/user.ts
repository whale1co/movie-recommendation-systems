import request from '../utils/request'

export function getUserInfo() {
  return request.get('/users/me')
}

export function updateProfile(data: { oldPassword?: string; newPassword?: string; preferences?: string }) {
  return request.patch('/users/me', data)
}

export function getMyRatings() {
  return request.get('/users/me/ratings')
}

export function getMyFavorites() {
  return request.get('/users/me/favorites')
}
