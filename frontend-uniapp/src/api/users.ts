import request from "./request"

/** 获取当前用户信息 */
export function getUserProfile() {
  return request.get("/users/me")
}

/** 获取在线陪玩列表 */
export function getBoosters(page = 1, size = 50) {
  return request.get("/users/boosters", { params: { page, size } }).then((result: any) => result?.records || result || [])
}

/** 切换陪玩状态 */
export function toggleBoosterStatus(status: string) {
  return request.put("/users/booster/status", null, { params: { status } })
}

/** 修改密码 */
export function changePassword(oldPassword: string, newPassword: string) {
  return request.put("/users/me/password", { oldPassword, newPassword })
}

/** 获取聊天消息 */
export function getMessages(orderId: number) {
  return request.get(`/messages/${orderId}`)
}

/** 发送消息 */
export function sendMessage(orderId: number, content: string, type = "text") {
  return request.post("/messages", null, { params: { orderId, content, type } })
}

/** 送出礼物 */
export function sendGift(boosterId: number, giftName: string, amount: number, message?: string) {
  return request.post("/gifts", null, { params: { boosterId, giftName, amount, message } })
}

/** 我送出的礼物 */
export function getSentGifts(page = 1, size = 20) {
  return request.get("/gifts/sent", { params: { page, size } }).then((result: any) => result?.records || result || [])
}

/** 我的评价 */
export function getMyReviews(page = 1, size = 20) {
  return request.get("/reviews/my", { params: { page, size } }).then((result: any) => result?.records || result || [])
}

/** 公告列表 */
export function getAnnouncements(page = 1, size = 20) {
  return request.get("/announcements", { params: { page, size } }).then((result: any) => result?.records || result || [])
}
