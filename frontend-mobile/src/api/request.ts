import axios from 'axios'
import { useAuthStore } from '../store/auth'

const BASE_URL = '/api'
const request = axios.create({ baseURL: BASE_URL })

// 请求拦截器：自动带 Token
request.interceptors.request.use(config => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

// 响应拦截器：统一错误处理
request.interceptors.response.use(
  res => {
    const body = res.data
    // 后端统一返回 Result<T> { code, message, data }
    // 直接解包到 data 层，调用处直接拿业务数据
    if (body && typeof body === 'object' && 'data' in body) {
      return body.data
    }
    return body
  },
  err => {
    if (err.response?.status === 401) {
      const auth = useAuthStore()
      // 游客模式下 401 不跳转登录页，静默失败
      if (!auth.isGuest) {
        auth.logout()
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  }
)

export default request
