import { useAuthStore } from "../store/auth"

// H5 通过开发服务器代理访问 /api；微信开发者工具访问本机后端。
// 真机联调时请改为已备案的 HTTPS API 域名，并配置微信 request 合法域名。
let baseUrl = import.meta.env.VITE_API_BASE_URL || "/api"
// #ifndef H5
baseUrl = import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8080/api"
// #endif

type RequestConfig = {
  url: string
  method?: "GET" | "POST" | "PUT" | "DELETE"
  data?: any
  params?: Record<string, any>
  header?: Record<string, string>
  _retried?: boolean
}

// 单飞刷新：并发多个 401 时只发一次 refresh，其余请求排队复用结果
let refreshPromise: Promise<string | null> | null = null

function doRefresh(): Promise<string | null> {
  const auth = useAuthStore()
  if (!auth.refreshToken) return Promise.resolve(null)
  if (!refreshPromise) {
    refreshPromise = new Promise<string | null>((resolve) => {
      uni.request({
        url: `${baseUrl}/auth/refresh?refreshToken=${encodeURIComponent(auth.refreshToken)}`,
        method: "POST",
        header: { "Content-Type": "application/json" },
        success: (res) => {
          const body: any = res.data
          if (res.statusCode === 200 && body && body.code === 200 && body.data) {
            auth.setAuth(body.data, auth.rememberMe)
            resolve(body.data.accessToken as string)
          } else {
            resolve(null)
          }
        },
        fail: () => resolve(null),
      })
    }).finally(() => { refreshPromise = null })
  }
  return refreshPromise
}

function request(config: RequestConfig): Promise<any> {
  return new Promise((resolve, reject) => {
    const auth = useAuthStore()
    const header: Record<string, string> = { "Content-Type": "application/json", ...config.header }
    if (auth.accessToken) header.Authorization = `Bearer ${auth.accessToken}`

    let url = baseUrl + config.url
    if (config.params) {
      const query = Object.entries(config.params)
        .filter(([, value]) => value !== undefined && value !== null && value !== "")
        .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
        .join("&")
      if (query) url += `?${query}`
    }

    uni.request({
      url,
      method: config.method || "GET",
      data: config.data,
      header,
      success: async (response) => {
        const body: any = response.data
        if (response.statusCode === 401) {
          if (!auth.isGuest && auth.refreshToken && !config._retried) {
            const newToken = await doRefresh()
            if (newToken) {
              config._retried = true
              request(config).then(resolve, reject)
              return
            }
          }
          if (!auth.isGuest) {
            auth.logout()
            uni.reLaunch({ url: "/pages/auth/login" })
          }
          reject({ ...response, data: body })
          return
        }
        if (response.statusCode >= 400 || (body?.code && body.code !== 200)) {
          reject({ ...response, data: body })
          return
        }
        resolve(body && typeof body === "object" && "data" in body ? body.data : body)
      },
      fail: reject,
    })
  })
}

export default {
  get(url: string, config?: { params?: Record<string, any> }) { return request({ url, method: "GET", ...config }) },
  post(url: string, data?: any, config?: { params?: Record<string, any> }) { return request({ url, method: "POST", data, ...config }) },
  put(url: string, data?: any, config?: { params?: Record<string, any> }) { return request({ url, method: "PUT", data, ...config }) },
  delete(url: string, config?: { params?: Record<string, any> }) { return request({ url, method: "DELETE", ...config }) },
}
