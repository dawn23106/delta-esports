import axios from 'axios'

const request = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '/api' })

request.interceptors.request.use(config => {
  const token = localStorage.getItem('adminToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  res => {
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body && body.code !== 200) {
      if (body.code === 401) {
        localStorage.removeItem('adminToken')
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
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('adminToken')
      window.location.href = `${import.meta.env.BASE_URL}login`
    }
    return Promise.reject(err)
  }
)

export default request
