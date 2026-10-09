import { ref } from 'vue'
const stored = sessionStorage.getItem('admin-access-token') || ''
export const accessToken = ref(stored)
export function setAccessToken(token: string) {
  accessToken.value = token
  if (token) sessionStorage.setItem('admin-access-token', token)
  else sessionStorage.removeItem('admin-access-token')
}
