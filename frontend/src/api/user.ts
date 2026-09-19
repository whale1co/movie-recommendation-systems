import request from '../utils/request'

export function getUserInfo() {
  return request.get('/user/me')
}

export function updateProfile(data: { oldPassword?: string; newPassword?: string; preferences?: string }) {
  return request.put('/user/profile', data)
}

export function getMyRatings() {
  return request.get('/user/ratings')
}

export function getMyFavorites() {
  return request.get('/user/favorites')
}
