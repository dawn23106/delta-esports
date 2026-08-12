# 微信小程序支付接入

项目业务层只依赖 `PaymentGateway`，当前部署使用 YunGouOS 适配器。小程序、订单服务和用户界面均不感知具体支付服务商，将来可以增加微信直连等适配器并通过 `PAYMENT_PROVIDER` 切换。

当前适配器调用：

- 支付下单：`POST /api/pay/wxpay/v3/minAppPay`
- 主动查单：`GET /api/system/order/getPayOrderInfo`
- 退款：`POST /api/pay/wxpay/refundOrder`
- 小程序端仅调用本项目后端并使用 `wx.requestPayment`，支付密钥只保存在后端。

## 订单状态

`pending_payment`（待支付）→ `pending`（待接单）→ `assigned` → `in_progress` → `submitted` → `done`

已支付订单取消时进入 `refund_pending`，收到验签通过的退款回调后变成 `cancelled`。未支付订单不会出现在打手接单池。

## 本地开发

`dev` 环境默认启用模拟支付，不会请求外部支付通道，也不会产生真实扣款。创建订单后，小程序会调用仅开发环境开放的模拟确认接口，验证完整状态流。

真实支付联调使用通用配置名：

```text
PAYMENT_ENABLED=true
PAYMENT_MOCK_ENABLED=false
PAYMENT_PROVIDER=yungouos
PAYMENT_API_BASE_URL=https://api.pay.yungouos.com
PAYMENT_MERCHANT_ID=微信支付商户号
PAYMENT_API_KEY=支付通道密钥
WECHAT_MINIPROGRAM_APP_ID=your_wechat_miniprogram_app_id
WECHAT_MINIPROGRAM_APP_SECRET=微信公众平台AppSecret
PAYMENT_PAY_NOTIFY_URL=https://你的域名/api/payments/callbacks/provider/pay
PAYMENT_REFUND_NOTIFY_URL=https://你的域名/api/payments/callbacks/provider/refund
MYSQL_USERNAME=生产数据库用户
MYSQL_PASSWORD=生产数据库强密码
CORS_ORIGINS=https://www.yjzdev.cn,https://admin.yjzdev.cn
```

回调地址必须是公网 HTTPS，且无需 JWT 登录。接口内部会验证签名、商户号、订单号和金额，并保证重复回调不会重复更新订单。

## 小程序配置

复制 `frontend-uniapp/.env.example` 为对应构建环境配置，将 `VITE_API_BASE_URL` 改为正式 HTTPS API 地址。该域名还需要加入微信公众平台的 `request` 合法域名。

当前使用 YunGouOS 时，需要在服务商后台完成商户进件、绑定小程序 AppID、开通原生小程序支付，并将通用回调地址配置到服务商后台。

## 生产数据库升级

生产环境不会自动执行 `schema.sql`。首次部署前，手动执行 `docs/payment-migration.sql` 和
`docs/release-migration.sql`，并先备份数据库。

## 上线核对

- 生产环境必须保持 `PAYMENT_MOCK_ENABLED=false`。
- 不得把 `PAYMENT_API_KEY` 或 `WECHAT_MINIPROGRAM_APP_SECRET` 写入前端或提交到 Git。
- 用 0.01 元真实订单完成一次支付、查单、取消和退款验证。
- 确认支付成功前订单不进入接单池，退款完成前不显示“已退款”。
