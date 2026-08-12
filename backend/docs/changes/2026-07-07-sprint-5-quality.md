# 变更说明：Sprint 5 — 工程质量打磨

- **日期**：2026-07-07
- **分支**：master
- **关联版本**：v2.0.0

## Why（为什么改）
- 多数 Service 方法缺少 @Transactional，数据一致性无保障
- 分页接口返回原始 MyBatis-Plus Page 对象，暴露内部结构
- AuthController 返回 Map 而非结构化 DTO
- /api/auth/me 返回完整 User（含 password）
- 无 Swagger API 文档
- 无操作日志记录

## What Changed

### 新增文件
| 文件路径 | 说明 |
|----------|------|
| `config/OperationLogAspect.java` | AOP 切面，环绕通知记录每个接口调用的 method/uri/user/耗时/状态 |

### 修改文件
| 文件路径 | 变更摘要 |
|----------|----------|
| `DeltaEsportsApplication.java` | 添加 `@OpenAPIDefinition`（Swagger 标题+描述） |
| `service/UserService.java` | 返回类型 LoginResponse/UserResponse；分页返回 PageResult；register/updateStatus 加 @Transactional |
| `service/ServiceItemService.java` | create/update/toggleActive 加 @Transactional |
| `service/AnnouncementService.java` | create/update/delete 加 @Transactional；page 返回 PageResult |
| `service/SettlementService.java` | updateStatus 加 @Transactional；page 返回 PageResult |
| `controller/AuthController.java` | login/register/refresh 返回 `Result<LoginResponse>`；/me 返回 `Result<UserResponse>`（不含 password） |
| `controller/OrderController.java` | 添加 @Tag + @Operation 注解 |
| `controller/ServiceController.java` | 添加 @Tag + @Operation 注解 |
| `controller/AdminController.java` | 添加 @Tag + @Operation 注解 |
| `controller/ReviewController.java` | 添加 @Tag + @Operation 注解 |
| `controller/GiftController.java` | 添加 @Tag + @Operation 注解 |
| `controller/AnnouncementController.java` | 添加 @Tag + @Operation 注解 |

## 数据库变更
无

## Breaking Changes
| 接口 | 变更前 | 变更后 |
|------|--------|--------|
| `GET /api/auth/me` | 返回 User 实体（含 password 字段） | 返回 UserResponse（不含 password） |
| 分页接口 | 返回 MyBatis-Plus Page 对象 | 返回 PageResult 对象（records/total/page/size/pages） |

## 验证
- [x] `mvn clean compile` — BUILD SUCCESS (48 source files)
- [ ] 启动后访问 http://localhost:8080/swagger-ui.html 显示 API 文档
- [ ] /api/auth/me 响应不含 password
- [ ] 分页接口返回 PageResult 格式
- [ ] 控制台有操作日志输出
