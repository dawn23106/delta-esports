import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({ baseURL: '/api', timeout: 10000 })

http.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  res => {
    const d = res.data
    if (d.code !== 200) { ElMessage.error(d.msg || '请求失败'); return Promise.reject(d) }
    return d
  },
  err => {
    if (err.response?.status === 401) { localStorage.clear(); window.location.href = '/login' }
    ElMessage.error('网络错误')
    return Promise.reject(err)
  }
)

export default http
