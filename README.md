# 游戏陪练订单撮合平台

一个可运行的多角色订单撮合系统：玩家发布需求，服务者并发抢单，客服负责派单和用户管理。

## 在线演示

部署后访问同一域名下的三个入口：

- `/`：演示导航页
- `/app/`：玩家与服务者移动端
- `/admin/`：客服管理端
- `/api/health`：健康检查

演示客服账号默认为 `13800000000 / cs123456`，可通过环境变量修改。演示环境仅使用模拟订单，不涉及真实交易或支付。

## 核心技术

- Spring Boot 3.3、MyBatis、MySQL/H2、Redis、JWT
- Vue 3、Vant、Element Plus、Vite
- 乐观并发控制：`UPDATE ... WHERE status = 'pending'`
- Redis `SETNX` 防止短时间重复下单
- Access Token + Refresh Token 续期与退出
- 数据库定时任务关闭 30 分钟未接订单
- GitHub Actions 自动测试、前端构建和容器构建

## 订单状态机

```text
pending -> assigned -> in_progress -> completed
    |          |              |
    +----------+--------------+-> cancelled
```

抢单通过带状态条件的原子更新执行。并发请求中只有第一个请求能把 `pending` 改为 `assigned`，其余请求得到 `409` 业务冲突。

## 一键启动演示模式

演示模式使用内嵌 H2，不要求安装 MySQL 或 Redis：

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="demo"
mvn spring-boot:run
```

健康检查：`http://localhost:8080/api/health`

## MySQL + Redis 本地开发

```powershell
docker compose up -d mysql redis
Copy-Item .env.example .env
cd backend
mvn spring-boot:run
```

数据库首次启动时会自动执行 `backend/src/main/resources/schema.sql`。前端分别运行：

```powershell
cd frontend-mobile
npm ci
npm run dev
```

```powershell
cd frontend-admin
npm ci
npm run dev
```

## 测试

```powershell
cd backend
mvn test

cd ../frontend-mobile
npm ci
npm run build

cd ../frontend-admin
npm ci
npm run build
```

后端测试覆盖：

- 乐观锁抢单成功与并发冲突
- 非服务者越权抢单
- 客服派单并发冲突
- 用户密码哈希不进入 JSON 响应
- 超时订单扫描任务

## 部署

仓库包含多阶段 `Dockerfile`，会构建两个 Vue 前端并打包进 Spring Boot。`render.yaml` 可用于 Render Blueprint 部署：

1. 在 Render 选择 **New Blueprint Instance**。
2. 连接本仓库。
3. Render 自动读取 `render.yaml` 并生成 JWT 密钥。
4. 部署成功后访问服务根地址。

免费演示配置使用 H2 文件数据库，实例重建后数据可能重置。正式环境应连接托管 MySQL 与 Redis，并替换全部默认凭据。

## 安全说明

- 数据库密码、JWT 密钥和 CORS 来源全部由环境变量提供。
- 用户密码使用 BCrypt 哈希，实体序列化时强制忽略密码字段。
- 后台接口统一检查客服角色。
- 玩家只能查询和取消自己的订单，服务者只能操作分配给自己的订单。
- 项目不接入真实支付，不处理真实陪练交易。
