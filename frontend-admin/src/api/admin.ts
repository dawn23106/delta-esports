import http from './request'

export function login(phone: string, password: string) {
  return http.post('/auth/login', { phone, password })
}

export function getOrders(params: any) {
  return http.get('/admin/orders', { params })
}

export function createOrder(data: any) {
  return http.post('/admin/orders', data)
}

export function assignOrder(id: number, boosterId: number) {
  return http.post(`/admin/orders/${id}/assign`, { boosterId })
}

export function cancelOrder(id: number) {
  return http.post(`/orders/${id}/cancel`)
}

export function getBoosters() {
  return http.get('/admin/boosters')
}

export function getUsers(page: number, pageSize: number) {
  return http.get('/admin/users', { params: { page, pageSize } })
}

export function updateUserRole(id: number, role: string) {
  return http.put(`/admin/users/${id}/role?role=${role}`)
}
