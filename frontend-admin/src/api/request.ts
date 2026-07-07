import axios from 'axios'

const request = axios.create({ baseURL: 'http://localhost:8080/api' })

request.interceptors.request.use(config => {
  const token = localStorage.getItem('adminToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

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
      localStorage.removeItem('adminToken')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default request
