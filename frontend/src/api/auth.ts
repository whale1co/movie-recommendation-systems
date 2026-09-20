import request from '../utils/request'

export function loginApi(username: string, password: string) {
  return request.post('/auth/login', { username, password })
}

export function refreshApi() {
  return request.post('/auth/refresh')
}

export function logoutApi() {
  return request.post('/auth/logout')
}

export function registerApi(username: string, password: string, preferences: string) {
  return request.post('/auth/register', { username, password, preferences })
}
