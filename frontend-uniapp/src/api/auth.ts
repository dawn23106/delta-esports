import request from "./request"

export function login(phone: string, password: string) {
  return request.post("/auth/login", { phone, password })
}

export function register(phone: string, password: string, nickname: string, role: string) {
  return request.post("/auth/register", { phone, password, nickname, role })
}

export function refreshToken(token: string) {
  return request.post("/auth/refresh", null, { params: { refreshToken: token } })
}

export function wxLogin(code: string) {
  return request.post("/auth/wx-login", null, { params: { code } })
}
