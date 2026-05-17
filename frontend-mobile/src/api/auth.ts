import http from './request'

export function login(phone: string, password: string) {
  return http.post('/auth/login', { phone, password })
}

export function register(phone: string, password: string) {
  return http.post('/auth/register', { phone, password })
}

export function getMe() {
  return http.get('/users/me')
}
