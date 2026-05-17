import http from './request'

export function createOrder(game: string, serviceType: string, detail: string, price: number, sourceChannel?: string) {
  return http.post('/orders', { game, serviceType, detail, price, sourceChannel })
}

export function getMyOrders(page = 1, pageSize = 20) {
  return http.get('/orders/my', { params: { page, pageSize } })
}

export function getPool(game?: string, page = 1, pageSize = 20) {
  return http.get('/orders/pool', { params: { game, page, pageSize } })
}

export function getBoosterOrders(page = 1, pageSize = 20) {
  return http.get('/orders/my-booster', { params: { page, pageSize } })
}

export function claimOrder(id: number) {
  return http.post(`/orders/${id}/claim`)
}

export function startOrder(id: number) {
  return http.post(`/orders/${id}/start`)
}

export function completeOrder(id: number) {
  return http.post(`/orders/${id}/complete`)
}

export function cancelOrder(id: number) {
  return http.post(`/orders/${id}/cancel`)
}
