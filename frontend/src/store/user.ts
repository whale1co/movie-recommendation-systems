import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi, logoutApi, registerApi } from '../api/auth'
import { getUserInfo, updateProfile } from '../api/user'
import { accessToken, setAccessToken } from '../utils/authState'
import { refreshAccessToken } from '../utils/request'

export const useUserStore = defineStore('user', () => {
  const token = accessToken
  const username = ref<string>('')
  const role = ref<string>('')
  const preferences = ref<string>('')
  const createTime = ref<string>('')
  let initialized = false

  async function login(user: string, password: string) {
    const res: any = await loginApi(user, password)
    if (res.code === 200) {
      setAccessToken(res.data.accessToken)
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

  async function logout() {
    try {
      await logoutApi()
    } finally {
      clearSession()
    }
  }

  function clearSession() {
    setAccessToken('')
    username.value = ''
    role.value = ''
    preferences.value = ''
    createTime.value = ''
  }

  async function fetchUserInfo() {
    try {
      const res: any = await getUserInfo()
      if (res.code === 200) {
        username.value = res.data.username
        role.value = res.data.role || 'USER'
        preferences.value = res.data.preferences || ''
        createTime.value = res.data.createTime || ''
      }
    } catch {
      clearSession()
    }
  }

  async function initialize() {
    if (initialized) return Boolean(token.value)
    initialized = true
    try {
      if (!token.value) await refreshAccessToken()
      await fetchUserInfo()
      return Boolean(token.value)
    } catch {
      clearSession()
      return false
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
    initialize,
    fetchUserInfo,
    updateUserProfile
  }
})
