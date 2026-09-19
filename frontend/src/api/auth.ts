import request from '../utils/request'

export function loginApi(username: string, password: string) {
  return request.post('/user/login', { username, password })
}

export function registerApi(username: string, password: string, preferences: string) {
  return request.post('/user/register', { username, password, preferences })
}
