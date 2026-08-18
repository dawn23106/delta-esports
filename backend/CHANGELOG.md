# Changelog

本项目所有重要变更记录在此。

格式基于 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)，版本号遵循 [语义化版本](https://semver.org/spec/v2.0.0.html)。

## [Unreleased] — Spring Boot 3 升级与错误响应统一

### Changed
- **Spring Boot 2.7.18 → 3.3.5**，全量完成 `javax.* → jakarta.*` 迁移（23 个文件）
- MyBatis-Plus 改用 `mybatis-plus-spring-boot3-starter` 3.5.7；springdoc 升级为 `springdoc-openapi-starter-webmvc-ui` 2.6.0；jjwt 升级 0.12.6 并适配新 API
- **错误响应统一为真实 HTTP 状态码**：业务异常不再返回 `HTTP 200 + code`，改为 400/401/403/404/409/429/500/502/503 对应真实状态
- `BusinessException(String)` 默认码由 500 改为 400，客户端错误不再被标成服务器错误
- `JwtInterceptor` 改为单次解析 JWT（新增 `JwtUtils.parseValidAccessToken()`）
- 分页参数统一归一化（新增 `PageSupport`，`size` 夹紧 1~100，`page` 归一 ≥1）
- 订单/服务项目「不存在」由默认 500 修正为 404

### Added
- `PageSupport` 分页参数归一化工具
- `t_user` / `t_order` / `t_order_message` / `t_balance_transaction` / `t_gift` / `t_review` / `t_settlement` 热点索引（见 `docs/release-migration.sql`）
- **分布式限流**：`RateLimitService` 支持 Redis 计数，未启用/不可用时回退单机内存；`docker-compose.yml` 新增 `redis` 服务
- **订单超时清理**：`OrderTimeoutScheduler` 周期取消超时未支付的 `pending_payment` 订单

### Security
- `RateLimitInterceptor` 客户端 IP 识别改用 `getRemoteAddr()`，不再信任可伪造的整段 `X-Forwarded-For`
- 移除 `application.yml` 中无实际作用的 `logic-delete-*` 死配置

### Breaking
- 业务错误响应契约变更：`HTTP 200 + {code,message}` → 真实 HTTP 状态码 + `{code,message}`（详见 `docs/migration-notes.md`）

---

## [Unreleased] — v2.0.0 全面重构

### Added
- 分页响应统一包装 `PageResult`，所有分页接口返回格式标准化
- 用户响应 DTO `UserResponse`，安全返回用户信息（不含密码）
- 结构化登录响应 DTO `LoginResponse`
- **接口限流**：登录/注册接口 10次/分钟/IP 防暴力破解
- **Maven Wrapper**：无需预装 Maven，`./mvnw` 即可构建
- **资金闭环**：创建订单冻结余额，取消自动退款，确认完成自动转账
- **资金流水表** `t_balance_transaction`：记录每笔 FREEZE/REFUND/TRANSFER 流水
- **陪陪统计更新**：订单完成后自动更新 total_orders 和 rating
- **评价系统**：老板可对已完成订单评价陪陪，支持评分/内容/标签，陪陪评分自动更新
- **礼物赠送**：用户可向陪陪赠送礼物，余额即时扣减并转账
- **公开公告**：无需登录即可查看平台公告列表
- **Swagger API 文档**：访问 `/swagger-ui.html` 查看完整接口文档
- **操作日志**：所有接口调用自动记录 method/uri/user/耗时/状态
- **单元测试**：25 个测试覆盖核心路径（订单流转/资金闭环/认证鉴权/拦截器）

### Changed
- 所有实体类（7个）全面使用 Lombok `@Data`，删除约 440 行手写 getter/setter
- 所有请求 DTO（4个）增加参数校验，非法入参返回 400 + 明确错误信息
- 全局异常处理增加参数校验异常（`MethodArgumentNotValidException`、`ConstraintViolationException`）处理
- **JwtInterceptor 改为严格模式**：受保护接口无有效 token 直接返回 401，不再放行
- **权限控制统一为 @RequireRole 注解 + AOP**：替代手动 checkAdmin() 调用
- **CORS 修复**：allowedOriginPatterns 从 `*` 改为配置化域名列表
- **JWT secret 环境变量化**：dev 使用默认值，prod 强制设置 `JWT_SECRET`
- **订单流程重构**：新增 `submitted` 状态，陪陪提交成果后需老板确认才算完成，确认时自动转账
- **所有 Service 写操作加 @Transactional**，确保数据一致性
- **分页响应标准化**：所有分页接口统一返回 PageResult 格式
- **登录/用户信息接口使用结构化 DTO**：LoginResponse / UserResponse

### Fixed
- 修复 `OrderController.getUserId()` 使用 `RuntimeException` 而非 `BusinessException` 的异常类型不一致问题

### Security
- JWT secret 从硬编码改为环境变量，startup 时校验长度 ≥32
- CORS `allowCredentials(true)` 不再与 wildcard 组合使用

### Breaking
- `POST /api/orders/complete` → `POST /api/orders/submit`：陪陪提交后需老板确认
- `GET /api/auth/me` 返回 `UserResponse`（去掉 password 字段）
- 所有受保护接口需携带有效 JWT token，否则返回 401

---

## [1.0.0] - 2026-07-06

### Added
- 初始版本：沧月电竞游戏陪玩代练服务平台
- 用户认证（登录/注册/JWT Token）
- 服务项目管理
- 订单系统（创建/接单/开始/完成/取消）
- 管理后台（用户管理/订单管理/公告管理/结算管理/礼物记录）
