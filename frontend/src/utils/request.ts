import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10000
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
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
  (error) => {
    if (error.response) {
      const status = error.response.status
      const requestId = error.response.data?.requestId || error.response.headers?.['x-request-id']
      const suffix = requestId ? `（请求ID：${requestId}）` : ''
      const isAuthRequest = error.config?.url?.startsWith('/auth/')
      if (status === 401 && !isAuthRequest) {
        localStorage.removeItem('token')
        ElMessage.error(`登录已过期，请重新登录${suffix}`)
        router.push('/login')
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
    } else {
      ElMessage.error('网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
