# 变更说明：Sprint 3 — 订单流程重构 + 资金闭环

- **日期**：2026-07-07
- **分支**：master
- **关联版本**：v2.0.0

## Why（为什么改）
- 陪陪完成订单直接 `completed`，缺少老板确认环节，无法处理纠纷
- 创建订单不校验/不扣余额，资金没有闭环流动
- 取消订单无退款，资金被「吞」
- 订单完成后不更新陪陪接单数和评分
- 结算记录在 admin confirm 时才创建，时机不合理

## What Changed

### 新订单状态流
```
pending → assigned → in_progress → submitted → done
                                               ↗ bossConfirmOrder()
                                submitOrder() ↗
```

### 新增文件
| 文件路径 | 说明 |
|----------|------|
| `entity/BalanceTransaction.java` | 资金流水实体（userId/orderId/amount/type/balanceAfter/remark） |
| `mapper/BalanceTransactionMapper.java` | 资金流水 Mapper |

### 修改文件
| 文件路径 | 变更摘要 |
|----------|----------|
| `schema.sql` | 新增 `t_balance_transaction` 表 |
| `service/OrderService.java` | **核心重构**：注入 UserMapper/ReviewMapper/BalanceTransactionMapper；`completeOrder` → `submitOrder`（in_progress→submitted）；新增 `bossConfirmOrder`（转账+结算+统计）；`createOrder` 增加余额校验和冻结；`cancelOrder` 增加退款；新增 `recordTransaction`/`updateBoosterStats` 私有方法；全部写操作加 `@Transactional` |
| `controller/OrderController.java` | `POST /complete` → `POST /submit`；新增 `POST /{id}/confirm`（老板确认） |

### 资金闭环流程
```
创建订单: boss.balance -= amount → FREEZE 流水
取消订单: boss.balance += amount → REFUND 流水
老板确认: booster.balance += amount → TRANSFER 流水 + 结算记录 + 更新陪陪统计
```

## 数据库变更
| 变更类型 | 表名 | 说明 |
|----------|------|------|
| 新增 | `t_balance_transaction` | 资金流水表，记录 FREEZE/REFUND/TRANSFER |

## Breaking Changes
| 接口 | 变更前 | 变更后 | 影响 |
|------|--------|--------|------|
| `POST /api/orders/complete` | 陪陪完成订单 (in_progress→completed) | `POST /api/orders/submit` (in_progress→submitted) | **前端需更新路径** |
| 订单状态 | completed 含义变化 | submitted 替代原来的 completed；completed 状态不再使用，改为 done | **影响状态判断逻辑** |

## 验证
- [x] `mvn clean compile` — BUILD SUCCESS (43 source files)
- [ ] 创建订单时余额不足返回 400
- [ ] 创建订单后余额被冻结
- [ ] 取消订单后余额退回
- [ ] 老板确认后陪陪收到钱
- [ ] 订单完成后陪陪 total_orders +1
