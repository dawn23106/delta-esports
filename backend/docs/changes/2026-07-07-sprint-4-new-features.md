# 变更说明：Sprint 4 — 评价系统 + 礼物赠送 + 公开公告

- **日期**：2026-07-07
- **分支**：master
- **关联版本**：v2.0.0

## Why（为什么改）
- Review 实体和 Mapper 存在但无 Service/Controller，评价功能空缺
- Gift 只有 admin 后台分页查询，用户无赠送礼物入口
- 公告只有 admin 管理接口，普通用户无法查看

## What Changed

### 新增文件
| 文件路径 | 说明 |
|----------|------|
| `service/ReviewService.java` | 评价服务：`createReview`（校验 done 状态+防重复+写评价+更新评分）、`getBoosterReviews`（分页查询陪陪评价） |
| `controller/ReviewController.java` | `POST /api/reviews`（创建评价）、`GET /api/reviews/booster/{id}`（查看陪陪评价） |
| `controller/GiftController.java` | `POST /api/gifts/send`（赠送礼物） |
| `controller/AnnouncementController.java` | `GET /api/announcements`（公开公告列表，无需认证） |

### 修改文件
| 文件路径 | 变更摘要 |
|----------|----------|
| `service/GiftService.java` | 注入 `UserMapper`；新增 `sendGift()`：校验余额→扣发送者→加接收者→记录 gift；加 `@Transactional` |

## 新增 API
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:--:|
| POST | `/api/reviews` | 创建评价 | ✅ |
| GET | `/api/reviews/booster/{id}` | 查看陪陪评价列表 | ✅ |
| POST | `/api/gifts/send` | 赠送礼物 | ✅ |
| GET | `/api/announcements` | 公告列表 | ❌ 公开 |

## 数据库变更
无

## Breaking Changes
无

## 验证
- [x] `mvn clean compile` — BUILD SUCCESS (47 source files)
- [ ] 对 done 订单创建评价成功
- [ ] 重复评价返回错误
- [ ] 赠送礼物后双方余额正确变动
- [ ] 公开获取公告列表无需 token
