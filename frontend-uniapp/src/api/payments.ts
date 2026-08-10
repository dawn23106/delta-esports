import request from "./request"

export function prepareOrderPayment(orderId: number, loginCode = "") {
  return request.post(`/payments/orders/${orderId}/prepare`, { loginCode })
}

export function queryOrderPayment(orderId: number) {
  return request.post(`/payments/orders/${orderId}/query`)
}

export function confirmMockPayment(orderId: number) {
  return request.post(`/payments/orders/${orderId}/mock-confirm`)
}

function getWechatLoginCode(): Promise<string> {
  // #ifndef MP-WEIXIN
  return Promise.resolve("")
  // #endif
  // #ifdef MP-WEIXIN
  return new Promise((resolve, reject) => {
    uni.login({ provider: "weixin", success: (result: any) => resolve(result.code || ""), fail: reject })
  })
  // #endif
}

function requestWechatPayment(params: Record<string, any>): Promise<void> {
  return new Promise((resolve, reject) => {
    uni.requestPayment({
      provider: "wxpay",
      timeStamp: String(params.timeStamp || params.timestamp || ""),
      nonceStr: params.nonceStr,
      package: params.package,
      signType: params.signType || "RSA",
      paySign: params.paySign,
      success: () => resolve(),
      fail: reject,
    } as any)
  })
}

/** 完整支付流程：wx.login -> 后端签名下单 -> wx.requestPayment -> 后端主动查单。 */
export async function payOrder(orderId: number) {
  const loginCode = await getWechatLoginCode()
  const prepared: any = await prepareOrderPayment(orderId, loginCode)
  if (prepared.mock) {
    return confirmMockPayment(orderId)
  }
  if (!prepared.paymentParams) throw new Error("未获取到微信支付参数")
  await requestWechatPayment(prepared.paymentParams)

  // YunGouOS 主动查单接口限流为 10 秒 1 次。支付成功以后只查一次；
  // 若异步回调尚未到达，订单页可由用户稍后手动刷新，避免密集轮询触发限流。
  return queryOrderPayment(orderId)
}
