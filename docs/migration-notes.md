# 迁移说明：Spring Boot 3 升级与错误响应统一

> 本文件记录 2.0.0 之后一次较大的技术升级，供提交 PR / 部署前查阅。
> 全部改动已通过 `mvnw test` 与三个前端（`frontend-admin` / `frontend-mobile` / `frontend-uniapp`）的构建验证。

## 1. Spring Boot 2.7 → 3.5.16

### 依赖版本变更（`backend/pom.xml`）

| 依赖 | 旧版本 | 新版本 |
| --- | --- | --- |
| `spring-boot-starter-parent` | 2.7.18 | 3.5.16 |
| MyBatis-Plus | `mybatis-plus-boot-starter` 3.5.3.1 | `mybatis-plus-spring-boot3-starter` 3.5.7 |
| springdoc | `springdoc-openapi-ui` 1.7.0 | `springdoc-openapi-starter-webmvc-ui` 2.6.0 |
| jjwt | 0.11.5 | 0.12.6 |

> 说明：Spring Boot 2.7 OSS 支持已于 2023-11 结束，本次升级消除该技术债；最终落在 3.5.16 以修复 Spring Framework 6.1.x 的已知 CVE（CVE-2025-22233 / 41234 / 41248 / 41249 等）。
> MyBatis-Plus 在 Spring Boot 3 下需改用独立的 `-spring-boot3-starter` 构件。

### `javax` → `jakarta` 迁移

受 Java EE 命名空间迁移影响，共 23 个文件将以下 import 替换为 `jakarta.*`：

- `javax.servlet.http.*` → `jakarta.servlet.http.*`
- `javax.validation.*` → `jakarta.validation.*`
- `javax.annotation.PostConstruct` → `jakarta.annotation.PostConstruct`

> `javax.crypto.SecretKey` 属于 JDK 自带包，不在迁移范围内，保持不变。

### jjwt 0.12 API 适配（`JwtUtils.java`）

- 生成：`setClaims/setSubject/setIssuedAt/setExpiration` → `claims/subject/issuedAt/expiration`；`signWith(key, SignatureAlgorithm.HS256)` → `signWith(key)`（算法由密钥长度自动推断）。
- 解析：`parserBuilder().setSigningKey().build().parseClaimsJws().getBody()` → `parser().verifyWith().build().parseSignedClaims().getPayload()`。

## 2. 错误响应统一为真实 HTTP 状态码

此前业务异常统一返回 `HTTP 200 + body.code`，与 JWT 拦截器直接返回的 `HTTP 401` 不一致，前端需同时兼容两条路径。

### 后端改动

- `GlobalExceptionHandler` 所有 `@ExceptionHandler` 改为返回 `ResponseEntity<Result<?>>`，携带真实状态码：
  - `BusinessException` → 按其 `code` 映射（400/401/403/404/409/429/500/502/503）
  - 参数校验 / 约束校验 / 请求体不可读 → 400
  - 未捕获异常 → 500
- `BusinessException(String)` 默认码由 **500 → 400**：原「余额不足」「手机号已注册」等客户端错误不再被标成 500。
- 控制器中直接 `return Result.error(...)` 的写法改为抛 `BusinessException`，统一走异常处理：
  - `AuthController.me`、`ServiceController.detail`、`OrderController.detail`
- 同步更新 `AuthControllerTest` 中 4 处错误场景断言（`isOk` → `isUnauthorized/isBadRequest/isForbidden`）。

### 前端影响

无需改动：三个前端的 `request` 封装同时兼容 `body.code`（成功回调）与 `err.response.status`（错误回调），调用处统一读取 `.response.data.message`（axios）/ `.data.message`（uni.request）。

### 契约变更（Breaking）

| 场景 | 旧 | 新 |
| --- | --- | --- |
| 业务错误 | HTTP 200 + `{code,message}` | HTTP 4xx/5xx + `{code,message}` |
| 成功 | HTTP 200 + `{code:200,data}` | 不变 |

## 3. 前端：刷新令牌自动续期

后端早已签发 `refreshToken` 并提供 `POST /api/auth/refresh`，但三个前端此前均未使用，access token 过期即强制登出。本次补齐：

- `frontend-mobile/src/api/request.ts`（axios）
- `frontend-uniapp/src/api/request.ts`（`uni.request`）
- `frontend-admin/src/api/request.ts`（axios）
- `frontend-admin/src/views/Login.vue`（登录时额外持久化 `adminRefreshToken`）

逻辑：请求遇 401 时，若有 refreshToken 则静默调用 `/auth/refresh` 换新并重放原请求；并发 401 通过「单飞队列」只发一次刷新；刷新失败才登出。

## 4. 其他后端优化

- **JWT 单次解析**：`JwtInterceptor` 改用新增的 `JwtUtils.parseValidAccessToken()`，一次解析完成签名/过期/类型校验（原请求最多解析 4 次）。
- **分页参数归一化**：新增 `PageSupport`，统一将 `page` 归一为 ≥1、`size` 夹紧为 1~100，应用到订单/评价/礼物/公告/结算/用户等分页接口，防止恶意超大 `size` 拉全表。
- **限流 IP 识别修复**：`RateLimitInterceptor` 不再信任可伪造的整段 `X-Forwarded-For`，改用 `getRemoteAddr()`（应用已开 `server.forward-headers-strategy=framework`）。
- **移除死配置**：`application.yml` 中无实际作用的 `logic-delete-*` 配置（无任何实体/表含 `deleted` 列）。
- **404 语义修正**：订单/服务项目「不存在」由默认 500 改为 404。
- **热点索引**：`schema.sql` 建表语句内补充 `t_user(role,status)`、`t_order(boss_id/booster_id/status,created_at)`、`t_order_message(order_id,created_at)`、`t_balance_transaction(user_id,created_at)`、`t_gift(sender_id/receiver_id)`、`t_review(booster_id/boss_id)`、`t_settlement(booster_id)` 索引；`docs/release-migration.sql` 同步追加对应 `CREATE INDEX` 供存量生产库执行。

## 5. 前端清理

- 删除 23 个无引用的脚手架/备份/特效组件文件（`HelloWorld.vue`、`Home.vue.bak`、`vite.svg`/`vue.svg`/`hero.png`、未使用的 shadcn 风格组件等）。
- `frontend-mobile` 移除 4 个已无引用的依赖（`motion-v`、`class-variance-authority`、`clsx`、`tailwind-merge`），并同步 `package-lock.json`。
- `start-all.cmd` 修正小程序输出目录文案（`dist/build/mp-weixin` → `dist/dev/mp-weixin`）。

## 6. 分布式限流与订单超时清理

### 分布式限流（Redis，可回退单机内存）

- 新增 `spring-boot-starter-data-redis` 依赖与 `RateLimitService`（`backend/src/main/java/com/delta/esports/config/RateLimitService.java`）。
- `RateLimitInterceptor`（登录/注册 10 次/分钟/IP）与 `PaymentService` 的支付查询节流改为通过 `RateLimitService` 计数。
- 行为：`app.rate-limit.redis-enabled=true` 时用 Redis `INCR + EXPIRE` 做多实例共享计数；未启用或 Redis 不可用时自动回退单机内存计数，功能不中断。
- 配置：
  - dev 默认 `RATE_LIMIT_REDIS_ENABLED=false`（本地无 Redis 也能跑）
  - prod 默认 `true`，`docker-compose.yml` 已新增 `redis:7-alpine` 服务并作为 `backend` 依赖

### 订单超时自动取消

- 新增 `OrderTimeoutScheduler`（`@EnableScheduling` + `@Scheduled`），周期扫描创建超过 `app.order-timeout.minutes`（默认 30 分钟）仍未支付的 `pending_payment` 订单并取消，避免待支付订单堆积。
- 取消逻辑复用 `OrderMapper.cancelUnpaid` 原子更新；非 mock 支付且已预下单时会顺带调用支付网关 `close` 关闭预下单（失败不阻塞）。
- 配置：`ORDER_TIMEOUT_MINUTES`、`ORDER_TIMEOUT_SCAN_MS`（默认 60000）、`ORDER_TIMEOUT_INITIAL_DELAY_MS`（默认 60000）。

## 7. 平台抽成、提现与 WebSocket 推送

### 平台抽成

- 订单完成结算拆分为 `amount`（订单总额）/ `commission`（平台抽成）/ `net_amount`（打手净收入）/ `commission_rate`（比例）。
- 抽成比例可配置 `app.commission.rate`（`COMMISSION_RATE` 环境变量，默认 **0.15**）。打手余额只入账净收入，抽成记录在 `t_settlement` 中（平台暂不建立专门账户，抽成金额仅留痕备查）。
- `t_settlement` 新增 3 列，存量库执行 `docs/release-migration.sql` 中对应的 `ALTER TABLE`。

### 提现（结算满 7 天可提）

- 新增 `t_withdrawal` 表与 `WithdrawalService`：陪陪申请提现时冻结余额，管理员 `approve / reject / paid` 审核，驳回自动退回冻结余额。
- 可提现金额 = 「结算满 `app.withdrawal.lock-days`（默认 **7** 天）的净收入」−「已冻结（pending/approved/paid）提现额」。
- 接口：`POST /api/withdrawals`（陪陪申请）、`GET /api/withdrawals/my`、`GET /api/admin/withdrawals`、`PUT /api/admin/withdrawals/{id}`。

### WebSocket 推送（尽力实现，前端可后续接入）

- 新增 `spring-boot-starter-websocket` 与极简推送：`/ws/orders?userId={userId}`。
- 握手从 query 解析 userId（**尚未做鉴权，生产上线前需接入 token 校验**）；`OrderPushService` 在订单状态变化与新消息时向老板/打手推送 `ORDER_EVENT` / `ORDER_MESSAGE` JSON。
- 前端未接入时为空操作，现有 REST 轮询不受影响。

## 8. 服务列表 Redis 缓存

- `GET /api/services` 改为 Cache-Aside 三步曲：先读 Redis（key `services:all`，10 分钟 TTL）→ 没有查 MySQL → 查完回填。
- 分类筛选改为在内存中过滤（与数据库过滤结果一致），顺带天然防「缓存穿透」：乱传分类不会打到 MySQL。
- 后台 `create/update/toggleActive` 会自动删除缓存键，保证改动立即可见；`findAllForAdmin` 不走缓存（需要最新含下架数据）。
- 任何 Redis 异常均回源 MySQL（fail-open），本地开发不启动 Redis 也能正常运行。
- 后续可按同一模式缓存公告、打手列表。

## 部署注意事项

1. 生产环境升级前，先在存量库执行 `docs/release-migration.sql` 中新增的 `CREATE INDEX` 语句（MySQL 不支持 `CREATE INDEX IF NOT EXISTS`，请确认同名索引不存在）。
2. 本次为框架大版本升级，建议先在与生产同构的 MySQL 环境跑一遍完整回归（下单 → 支付 → 抢单 → 完成 → 结算 → 退款）。
3. 错误响应契约已变更：任何依赖「HTTP 200 + body.code」的旧客户端/脚本需要适配为读取真实 HTTP 状态码。
4. 分布式限流依赖 Redis：`docker compose up` 会自动拉起 `redis` 服务；如需关闭分布式限流回退单机内存，设 `RATE_LIMIT_REDIS_ENABLED=false`。
5. 抽成与提现：存量库需执行 `docs/release-migration.sql` 中 `t_settlement` 的 `ALTER TABLE` 与 `t_withdrawal` 建表；抽成比例与提现锁定期分别用 `COMMISSION_RATE`、`WITHDRAWAL_LOCK_DAYS` 配置。
6. WebSocket 推送握手尚未鉴权，上线前需在握手阶段校验 JWT，并收紧 `WebSocketConfig` 的 `setAllowedOrigins`。
7. **依赖安全现状**：`frontend-admin` / `frontend-mobile` 审计为 0 漏洞；后端已升到 Spring Boot 3.5.16（修复 Spring Framework CVE）；`frontend-uniapp` 剩余约 66 个漏洞全部来自 DCloud 构建工具链（devDependencies：vite/esbuild/express/jest/jimp/ws 等），**不随小程序包发布给终端用户**，需待 DCloud 发布新版后整体升级 `@dcloudio/*` 才能消除。
