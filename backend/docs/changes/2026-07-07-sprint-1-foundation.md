# 变更说明：Sprint 1 — 基础设施重构

- **日期**：2026-07-07
- **分支**：master (初始重构)
- **关联版本**：v2.0.0

## Why（为什么改）
- 所有 Entity/DTO 手写 getter/setter，Lombok 已在 pom 中引入但未使用，代码冗余
- DTO 无参数校验，非法数据直接穿透到 Service 层
- 分页查询直接返回 MyBatis-Plus Page 对象，暴露内部结构
- /api/auth/me 返回完整 User 实体，密码哈希有泄露风险
- OrderController.getUserId() 抛 RuntimeException，与其他异常处理不一致

## What Changed（具体改了什么）

### 新增文件
| 文件路径 | 说明 |
|----------|------|
| `.gitignore` | Git 忽略规则（target/、.idea/、*.class 等） |
| `common/PageResult.java` | 泛型分页响应包装器，含 `of(Page<T>)` 静态工厂方法 |
| `dto/UserResponse.java` | 用户响应 DTO，不含 password 字段，含 `from(User)` 静态工厂 |
| `dto/LoginResponse.java` | 登录响应 DTO，替代原 Map 返回值 |

### 修改文件
| 文件路径 | 变更摘要 |
|----------|----------|
| `pom.xml` | 添加 4 个依赖：spring-boot-starter-validation、spring-boot-starter-aop、guava 31.1-jre、springdoc-openapi-ui 1.7.0 |
| `entity/User.java` | 添加 `@Data`，删除 14 个手写 getter/setter 方法 |
| `entity/Order.java` | 添加 `@Data`，删除 18 个手写 getter/setter 方法 |
| `entity/ServiceItem.java` | 添加 `@Data`，删除 16 个手写 getter/setter 方法 |
| `entity/Announcement.java` | 添加 `@Data`，删除 10 个手写 getter/setter 方法 |
| `entity/Gift.java` | 添加 `@Data`，删除 12 个手写 getter/setter 方法 |
| `entity/Review.java` | 添加 `@Data`，删除 12 个手写 getter/setter 方法 |
| `entity/Settlement.java` | 添加 `@Data`，删除 14 个手写 getter/setter 方法 |
| `dto/LoginRequest.java` | 添加 `@Data`、`@NotBlank`、`@Pattern`（手机号）、`@Size`（密码） |
| `dto/RegisterRequest.java` | 添加 `@Data`、`@NotBlank`、`@Pattern`（手机号）、`@Size`（密码/昵称） |
| `dto/CreateOrderRequest.java` | 添加 `@Data`、`@NotNull`（serviceId）、`@NotBlank`（区服/段位/地图）、`@Size`（备注） |
| `dto/CompleteOrderRequest.java` | 添加 `@Data`、`@NotNull`（orderId/isQualified）、`@Size`（备注） |
| `common/Result.java` | 添加 `@Data`、`@NoArgsConstructor(access = PRIVATE)`，删除 4 个手写 getter/setter |
| `common/GlobalExceptionHandler.java` | 新增 3 个异常处理器：`MethodArgumentNotValidException`（400）、`ConstraintViolationException`（400）、`HttpMessageNotReadableException`（400） |
| `controller/OrderController.java` | `getUserId()` 中 `RuntimeException` → `BusinessException(401, ...)`；`@RequestBody` 参数添加 `@Valid` |
| `controller/AuthController.java` | `@RequestBody` 参数添加 `@Valid` |
| `controller/ServiceController.java` | `@RequestBody` 参数添加 `@Valid` |

### 删除
无文件删除。

### 代码量变化
- 删除手写 getter/setter 约 560 行
- 新增代码约 120 行
- 净减少约 440 行

## 数据库变更
无

## Breaking Changes
| 接口/功能 | 变更前 | 变更后 | 影响范围 |
|-----------|--------|--------|----------|
| 无（本次仅内部重构，外部 API 行为不变） | - | - | - |

## 验证
- [x] Git 仓库已初始化，初始 commit 完成
- [ ] `mvn clean compile` — 需在 IDE 中验证
- [ ] 启动应用，H2 初始化正常
- [ ] 参数校验生效（非法手机号返回 400）
