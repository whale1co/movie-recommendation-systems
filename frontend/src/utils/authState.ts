import { ref } from 'vue'

export const accessToken = ref('')

export function setAccessToken(token: string) {
  accessToken.value = token
}
