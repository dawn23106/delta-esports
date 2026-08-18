import axios from 'axios'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
const request = axios.create({ baseURL: BASE_URL })

request.interceptors.request.use(config => {
  const token = localStorage.getItem('adminToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 单飞刷新：并发多个 401 时只发一次 refresh，其余请求排队复用结果
let refreshPromise: Promise<string | null> | null = null

function refreshAccessToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem('adminRefreshToken')
  if (!refreshToken) return Promise.resolve(null)
  if (!refreshPromise) {
    refreshPromise = axios
      .post(`${BASE_URL}/auth/refresh`, null, { params: { refreshToken } })
      .then(res => {
        const body = res.data
        if (body && typeof body === 'object' && body.code === 200 && body.data) {
          localStorage.setItem('adminToken', body.data.accessToken)
          localStorage.setItem('adminRefreshToken', body.data.refreshToken)
          return body.data.accessToken as string
        }
        return null
      })
      .catch(() => null)
      .finally(() => { refreshPromise = null })
  }
  return refreshPromise
}

request.interceptors.response.use(
  res => {
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body && body.code !== 200) {
      if (body.code === 401) {
        localStorage.removeItem('adminToken')
        localStorage.removeItem('adminRefreshToken')
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
    const original = (err.config || {}) as any
    if (err.response?.status === 401 && !original._retried && localStorage.getItem('adminRefreshToken')) {
      const newToken = await refreshAccessToken()
      if (newToken) {
        original._retried = true
        return request(original)
      }
    }
    if (err.response?.status === 401) {
      localStorage.removeItem('adminToken')
      localStorage.removeItem('adminRefreshToken')
      window.location.href = `${import.meta.env.BASE_URL}login`
    }
    return Promise.reject(err)
  }
)

export default request
