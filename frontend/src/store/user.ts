import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi, registerApi } from '../api/auth'
import { getUserInfo, updateProfile } from '../api/user'
import router from '../router'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const username = ref<string>('')
  const role = ref<string>(localStorage.getItem('role') || '')
  const preferences = ref<string>('')
  const createTime = ref<string>('')

  async function login(user: string, password: string) {
    const res: any = await loginApi(user, password)
    if (res.code === 200) {
      token.value = res.data.token
      localStorage.setItem('token', res.data.token)
      await fetchUserInfo()
      return true
    }
    throw new Error(res.message)
  }

  async function register(user: string, password: string, prefs: string) {
    const res: any = await registerApi(user, password, prefs)
    if (res.code === 201) {
      return true
    }
    throw new Error(res.message)
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = ''
    preferences.value = ''
    createTime.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    router.push('/login')
  }

  async function fetchUserInfo() {
    try {
      const res: any = await getUserInfo()
      if (res.code === 200) {
        username.value = res.data.username
        role.value = res.data.role || 'USER'
        localStorage.setItem('role', role.value)
        preferences.value = res.data.preferences || ''
        createTime.value = res.data.createTime || ''
      }
    } catch {
      logout()
    }
  }

  async function updateUserProfile(data: { oldPassword?: string; newPassword?: string; preferences?: string }) {
    const res: any = await updateProfile(data)
    if (res.code === 200) {
      if (res.data?.preferences !== undefined) {
        preferences.value = res.data.preferences || ''
      }
      return true
    }
    throw new Error(res.message)
  }

  return {
    token,
    username,
    role,
    preferences,
    createTime,
    login,
    register,
    logout,
    fetchUserInfo,
    updateUserProfile
  }
})
