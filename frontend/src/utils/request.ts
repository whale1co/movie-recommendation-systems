import axios from 'axios'
import { ElMessage } from 'element-plus'
import { accessToken, setAccessToken } from './authState'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
  withCredentials: true
})

const refreshClient = axios.create({ baseURL: '/api/v1', timeout: 10000, withCredentials: true })
let refreshPromise: Promise<string> | null = null

export function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = refreshClient.post('/auth/refresh')
      .then((response) => {
        const token = response.data?.data?.accessToken || ''
        setAccessToken(token)
        return token
      })
      .finally(() => { refreshPromise = null })
  }
  return refreshPromise
}

request.interceptors.request.use(
  (config) => {
    if (accessToken.value) {
      config.headers.Authorization = `Bearer ${accessToken.value}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    return response.data
  },
  async (error) => {
    if (error.response) {
      const status = error.response.status
      const requestId = error.response.data?.requestId || error.response.headers?.['x-request-id']
      const suffix = requestId ? `（请求ID：${requestId}）` : ''
      const original = error.config as typeof error.config & { _retry?: boolean }
      const skipGlobalError = Boolean((original as any)?.skipGlobalError)
      const isAuthRequest = original?.url?.startsWith('/auth/')
      if (status === 401 && !isAuthRequest && !original?._retry) {
        original._retry = true
        try {
          const token = await refreshAccessToken()
          ;(original.headers as any).Authorization = `Bearer ${token}`
          return request(original)
        } catch {
          setAccessToken('')
          ElMessage.error(`登录已过期，请重新登录${suffix}`)
          const redirect = encodeURIComponent(window.location.pathname + window.location.search)
          window.location.assign(`/login?redirect=${redirect}`)
        }
      } else if (skipGlobalError) {
        // Some feature pages provide richer, actionable inline error states.
      } else if (status === 403) {
        ElMessage.error(`无权访问${suffix}`)
      } else if (status === 404) {
        ElMessage.error(`资源不存在${suffix}`)
      } else if (status === 409) {
        ElMessage.error(`${error.response.data?.message || '数据冲突'}${suffix}`)
      } else if (status === 429) {
        ElMessage.error(`请求过于频繁，请稍后重试${suffix}`)
      } else if (status === 503) {
        ElMessage.error(`服务暂时不可用，请稍后重试${suffix}`)
      } else {
        ElMessage.error(`${error.response.data?.message || '请求失败'}${suffix}`)
      }
    } else if (!(error.config as any)?.skipGlobalError && error.code !== 'ERR_CANCELED') {
      ElMessage.error('网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
