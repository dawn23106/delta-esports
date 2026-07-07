import request from './request'

export function getServices(category?: string) {
  return request.get('/services', { params: category ? { category } : {} })
}

export function getServiceDetail(id: number) {
  return request.get(`/services/${id}`)
}
