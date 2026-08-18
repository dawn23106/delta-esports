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

// 单飞刷新：并发多个 401 时只发一次 refresh，其余请求排队复用结果
let refreshPromise: Promise<string | null> | null = null

function refreshAccessToken(): Promise<string | null> {
  const auth = useAuthStore()
  if (!auth.refreshToken) return Promise.resolve(null)
  if (!refreshPromise) {
    refreshPromise = axios
      .post(`${BASE_URL}/auth/refresh`, null, { params: { refreshToken: auth.refreshToken } })
      .then(res => {
        const body = res.data
        if (body && typeof body === 'object' && body.code === 200 && body.data) {
          auth.setAuth(body.data, auth.rememberMe)
          return body.data.accessToken as string
        }
        return null
      })
      .catch(() => null)
      .finally(() => { refreshPromise = null })
  }
  return refreshPromise
}

// 响应拦截器：统一错误处理 + 401 自动续期
request.interceptors.response.use(
  res => {
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body && body.code !== 200) {
      const auth = useAuthStore()
      if (body.code === 401 && !auth.isGuest) {
        auth.logout()
        window.location.href = `${import.meta.env.BASE_URL}login`
      }
      return Promise.reject({ response: { status: body.code, data: body } })
    }
    // 后端统一返回 Result<T> { code, message, data }
    // 直接解包到 data 层，调用处直接拿业务数据
    if (body && typeof body === 'object' && 'data' in body) {
      return body.data
    }
    return body
  },
  async err => {
    const auth = useAuthStore()
    const original = (err.config || {}) as any
    if (err.response?.status === 401 && !auth.isGuest && auth.refreshToken && !original._retried) {
      const newToken = await refreshAccessToken()
      if (newToken) {
        original._retried = true
        // 重试时请求拦截器会重新从 store 读取新 accessToken 写入 header
        return request(original)
      }
    }
    if (err.response?.status === 401 && !auth.isGuest) {
      auth.logout()
      window.location.href = `${import.meta.env.BASE_URL}login`
    }
    return Promise.reject(err)
  }
)

export default request
