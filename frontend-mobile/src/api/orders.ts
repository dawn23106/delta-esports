import request from './request'

export function createOrder(data: { serviceId: number; boosterId?: number; gameMap?: string; bossNote?: string }) {
  return request.post('/orders', data)
}

export function claimOrder(id: number) {
  return request.post(`/orders/${id}/claim`)
}

export function startOrder(id: number) {
  return request.post(`/orders/${id}/start`)
}

export function completeOrder(data: { orderId: number; isQualified: boolean; resultNote?: string; resultImages?: string }) {
  return request.post('/orders/complete', data)
}

export function cancelOrder(id: number) {
  return request.post(`/orders/${id}/cancel`)
}

export function getMyOrders(page = 1, size = 10, status?: string) {
  return request.get('/orders/my', { params: { page, size, status } })
}

export function getOrderPool(page = 1, size = 10) {
  return request.get('/orders/pool', { params: { page, size } })
}

export function getOrderDetail(id: number) {
  return request.get(`/orders/${id}`)
}
