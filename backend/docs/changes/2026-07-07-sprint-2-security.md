# 变更说明：Sprint 2 — 安全加固

- **日期**：2026-07-07
- **分支**：master
- **关联版本**：v2.0.0

## Why（为什么改）
- JWT secret 在 application.yml 中硬编码，提交到 Git 后密钥泄露
- CORS 使用 `allowedOriginPatterns("*")` + `allowCredentials(true)`，浏览器会拒绝此组合
- JwtInterceptor 无 token 也放行，权限全靠 Controller 自觉调用 checkAdmin()
- 每个 Admin 接口都重复 `checkAdmin(request)` 代码
- 登录/注册接口无防暴力破解保护

## What Changed

### 新增文件
| 文件路径 | 说明 |
|----------|------|
| `config/RequireRole.java` | 自定义注解 `@RequireRole({"admin"})`，支持 METHOD/TYPE |
| `config/RoleAspect.java` | AOP 切面，拦截 @RequireRole 注解，从 request.role 校验权限 |
| `config/RateLimitInterceptor.java` | Guava Cache 实现限流，登录/注册 10次/分钟/IP |

### 修改文件
| 文件路径 | 变更摘要 |
|----------|----------|
| `application.yml` | dev: JWT secret 改为带默认值的 `${JWT_SECRET:...}`；prod: 强制 `${JWT_SECRET}` 无默认值；新增 cors.allowed-origins 配置项；新增 springdoc 配置 |
| `DeltaEsportsApplication.java` | 添加 `@EnableAspectJAutoProxy` |
| `JwtUtils.java` | 添加 `@PostConstruct validateSecret()`：secret 为空/不足32字符时启动即报错 |
| `JwtInterceptor.java` | **严格模式**：无 token → 401 JSON 响应；过期 token → 401 JSON 响应；不再放行未认证请求 |
| `WebMvcConfig.java` | CORS 从 `"*"` 改为 `cors.allowed-origins` 配置项；注册 RateLimitInterceptor（仅 auth 路径）；更新 excludePathPatterns（新增公开接口和 swagger 路径） |
| `AdminController.java` | 类级别加 `@RequireRole("admin")`；删除所有 `checkAdmin()` 调用和私有方法；删除 `HttpServletRequest` 参数；`/orders/{id}/confirm` 改名 `force-done` |

## 数据库变更
无

## Breaking Changes
| 接口/功能 | 变更前 | 变更后 | 影响 |
|-----------|--------|--------|------|
| 受保护接口 | 无 token 可访问（由 Controller 自行判断） | 无 token 直接返回 401 | **前端必须携带 Authorization header** |
| CORS | `allowedOriginPatterns("*")` | 仅允许配置的域名列表 | 需在 application.yml 配置前端地址 |
| Admin API | `POST /api/admin/orders/{id}/confirm` | `POST /api/admin/orders/{id}/force-done` | 路径变更 |

## 验证
- [x] `mvn clean compile` — BUILD SUCCESS (41 source files)
- [ ] 启动应用，无 token 访问受保护接口返回 401
- [ ] 携带有效 token 正常访问
- [ ] 非 admin 用户访问 /api/admin/* 返回 403
- [ ] 连续快速调用 login > 10次返回 429
