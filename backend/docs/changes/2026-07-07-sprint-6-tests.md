# 变更说明：Sprint 6 — 测试覆盖

- **日期**：2026-07-07
- **分支**：master
- **关联版本**：v2.0.0

## Why（为什么改）
- 项目无任何单元测试，重构后需验证核心路径正确性

## What Changed

### 新增文件
| 文件路径 | 测试数 | 覆盖内容 |
|----------|:--:|------|
| `src/test/.../service/OrderServiceTest.java` | 4 | 创建订单冻结余额、完整订单流转（create→claim→start→submit→confirm）、取消退款、余额不足拒绝 |
| `src/test/.../service/UserServiceTest.java` | 5 | 注册成功、重复注册报错、登录成功、密码错误报错、Token刷新 |
| `src/test/.../controller/AuthControllerTest.java` | 8 | 登录成功/失败、手机号校验、无 token 被拒、有效 token 通过、过期 token 被拒、公开接口免认证、非 admin 被拒 |
| `src/test/.../config/JwtInterceptorTest.java` | 8 | 无 token→401、无效格式→401、过期→401、有效 pass、OPTIONS pass、auth 路径免认证、公开接口免认证 |

### 测试结果
```
Tests: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 数据库变更
无

## Breaking Changes
无
