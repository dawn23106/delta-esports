# 三角洲陪玩接单助手

三方撮合平台：**玩家下单 → 打手接单 → 客服管理**。

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.3 + MyBatis + MySQL + Redis + JWT |
| 前端(移动) | Vue 3 + Vite + Vant 4 (玩家端 & 打手端) |
| 前端(管理) | Vue 3 + Vite + Element Plus (客服后台) |
| 部署 | Docker Compose (MySQL + Redis) + 内网穿透 |

## 快速启动

### 1. 启动数据库
```bash
docker compose up -d
```

### 2. 初始化数据库
```bash
mysql -h localhost -u root -proot < backend/src/main/resources/schema.sql
```

### 3. 启动后端
```bash
cd backend
mvn spring-boot:run
# 后端运行在 http://localhost:8080
```

### 4. 启动前端
```bash
# 移动端 (玩家 & 打手)
cd frontend-mobile
npm install
npm run dev
# → http://localhost:5173

# 管理后台 (客服)
cd frontend-admin
npm install
npm run dev
# → http://localhost:5174
```

## 预置账号

| 角色 | 手机号 | 密码 |
|------|--------|------|
| 客服 | 13800000000 | cs123456 |
| 玩家/打手 | 自行注册 | - |

注意：新注册用户默认为玩家，客服可在管理后台将用户设为打手。

## 目录结构

```
delta-helper-java/
├── README.md
├── docker-compose.yml          # MySQL + Redis
├── docs/                        # 设计文档 & 学习笔记
├── backend/                     # Spring Boot 后端
│   └── src/main/java/com/delta/
│       ├── auth/                # 认证 (JWT)
│       ├── user/                # 用户
│       ├── order/               # 订单核心
│       ├── config/              # 配置 (Redis/CORS/JWT)
│       └── common/              # 通用 (Result/异常)
├── frontend-mobile/             # Vue 3 移动端 H5
└── frontend-admin/              # Vue 3 管理后台
```

## 订单状态流转

```
pending(待接单) → assigned(已接单) → in_progress(进行中) → completed(已完成)
    ↓                  ↓                  ↓
cancelled(已取消)  cancelled          cancelled
```

## API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 注册 |
| POST | /api/auth/login | 登录 |
| GET | /api/users/me | 当前用户信息 |
| POST | /api/orders | 玩家下单 |
| GET | /api/orders/my | 我的订单 |
| GET | /api/orders/pool | 订单池(打手) |
| POST | /api/orders/{id}/claim | 接单 |
| POST | /api/orders/{id}/start | 开始 |
| POST | /api/orders/{id}/complete | 完成 |
| POST | /api/orders/{id}/cancel | 取消 |
| GET | /api/admin/orders | 客服全量订单 |
| POST | /api/admin/orders | 客服创建订单 |
| POST | /api/admin/orders/{id}/assign | 派单 |

## 外网访问

学生零成本方案：用 frp 或 ngrok 内网穿透。

```bash
# ngrok (最简单)
ngrok http 8080   # 后端
ngrok http 5173   # 移动端
```

把生成的后端 URL 填到前端 vite.config.ts 的 proxy 里即可。
