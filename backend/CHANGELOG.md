# Changelog

本项目所有重要变更记录在此。

格式基于 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)，版本号遵循 [语义化版本](https://semver.org/spec/v2.0.0.html)。

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
