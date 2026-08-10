---
tags:
  - deployment
  - backend
  - frontend
  - learning
created: 2026-07-07
---

# 上线部署完全指南

## 先理解你的项目长什么样

在讲部署之前，先把你的项目拆开看：

```
┌─────────────────────────────────────────────────┐
│                  用户的浏览器                     │
│  https://www.yjzdev.cn                          │
└──────────────┬──────────────────────────────────┘
               │ ① 访问网站
               ▼
┌─────────────────────────────────────────────────┐
│  nginx (Web 服务器)                               │
│  - 返回 HTML/CSS/JS 静态文件（前端页面）            │
│  - 把 /api/* 请求转发给后端                         │
└──────┬──────────────────────┬───────────────────┘
       │ ② 静态文件            │ ③ /api/orders ...
       ▼                       ▼
┌──────────────┐    ┌─────────────────────────────┐
│  dist/       │    │  Java Spring Boot           │
│  index.html  │    │  监听 localhost:8080          │
│  *.js, *.css │    │  处理业务逻辑、数据库读写       │
└──────────────┘    └──────────┬──────────────────┘
                               │ ④ SQL 查询
                               ▼
                    ┌─────────────────────────────┐
                    │  MySQL 数据库                 │
                    │  存储用户、订单、消息等数据       │
                    └─────────────────────────────┘
```

你的项目有三个部分：

| 部分 | 技术 | 产物 | 运行时 |
|------|------|------|--------|
| **移动端** | Vue 3 + Vite | `dist/` 里的静态文件 | 不需要运行，浏览器直接加载 |
| **管理后台** | Vue 3 + Vite | `dist/` 里的静态文件 | 同上 |
| **后端 API** | Spring Boot | `target/*.jar` | 需要 JVM 一直跑着 |
| **数据库** | MySQL | SQL 文件 | 需要 MySQL 服务一直跑着 |

**关键认知**：前端代码构建完只是一堆 `.html`、`.js`、`.css` 文件，谁拿到都能打开。后端代码需要 Java 虚拟机（JVM）持续运行，因为要处理请求、连数据库、做计算。

---

## 为什么需要 nginx？

你可能会问：**"Spring Boot 不能直接返回 HTML 吗？Tomcat 不就是干这个的？"**

技术上可以，但生产环境不会这样做。原因：

### 1. 动静分离 — 各干各的

```
用户请求 → nginx → /api/* → 转发给 Java (慢，需要查数据库)
                  → /*     → 直接返回文件 (快，纯 I/O)
```

- **nginx 处理静态文件**：C 语言写的，单机能扛几万并发，吃内存极少
- **Java 处理业务逻辑**：查数据库、算价格、校验权限，这些必须 Java 做
- 要是让 Java 同时干这两件事，线程池被静态文件占满，真正的业务请求就排队了

### 2. 反向代理 — 藏起后端

```
用户只访问 www.yjzdev.cn
不知道后端在 localhost:8080
```

nginx 挡在前面，用户永远不知道后端跑在哪个端口、哪台机器。你可以随时换后端、加机器、改端口，用户无感知。

### 3. HTTPS 终结

SSL 证书配在 nginx 上，后端 Java 不需要关心加密。内部通信走 HTTP，外部走 HTTPS。

### 4. 静态资源可以做 CDN

`dist/` 里的 `.js`、`.css` 文件名带 hash（`Home-DuRrOtkQ.js`），说明内容不变文件名就不变。nginx 可以给这些文件加 `Cache-Control: max-age=31536000`，浏览器一年不用重新下载。

---

## 三种部署方式

### 方式一：手动部署（最直观）

> **适用**：一台 VPS，想搞清楚每一步在干什么

#### 你需要的东西

- 一台云服务器（阿里云 ECS / 腾讯云 CVM），最低 2C4G
- 一个域名，DNS 解析到服务器 IP
- 服务器装好：JDK 21、nginx、MySQL 8.0

#### 步骤

```bash
# ===== 在开发机上 =====

# 1. 构建后端
cd backend
mvn clean package -DskipTests
# 产物：target/delta-esports-0.0.1-SNAPSHOT.jar

# 2. 构建前端
cd frontend-mobile && npm run build   # 产物：dist/
cd frontend-admin && npm run build    # 产物：dist/

# 3. 上传到服务器
scp backend/target/*.jar user@server:/opt/delta-esports/
scp -r frontend-mobile/dist/* user@server:/opt/delta-esports/static/m/
scp -r frontend-admin/dist/* user@server:/opt/delta-esports/static/admin/

# ===== 在服务器上 =====

# 4. 准备数据库
mysql -u root -p
CREATE DATABASE delta_esports CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'delta'@'localhost' IDENTIFIED BY '你的密码';
GRANT ALL ON delta_esports.* TO 'delta'@'localhost';

# 5. 环境变量
cat > /opt/delta-esports/env.sh << 'EOF'
export JWT_SECRET="$(openssl rand -base64 48)"
export MYSQL_PASSWORD="你的数据库密码"
export CORS_ORIGINS="https://你的域名.com"
export SPRING_PROFILES_ACTIVE="prod"
EOF
source /opt/delta-esports/env.sh

# 6. 启动后端（先手动试一次）
java -jar /opt/delta-esports/delta-esports-0.0.1-SNAPSHOT.jar

# 7. 配 nginx
sudo vim /etc/nginx/sites-available/delta-esports
```

**nginx 配置**：

```nginx
server {
    listen 80;
    server_name 你的域名.com;
    return 301 https://$host$request_uri;  # 强制 HTTPS
}

server {
    listen 443 ssl http2;
    server_name 你的域名.com;

    ssl_certificate     /etc/letsencrypt/live/你的域名/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/你的域名/privkey.pem;

    # 移动端
    location /m {
        alias /opt/delta-esports/static/m;
        try_files $uri $uri/ /m/index.html;
    }

    # 管理后台
    location /admin {
        alias /opt/delta-esports/static/admin;
        try_files $uri $uri/ /admin/index.html;
    }

    # API 反向代理
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 静态资源长期缓存
    location ~* \.(js|css|png|jpg|svg|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

```bash
# 8. 启动 nginx
sudo nginx -t && sudo systemctl reload nginx

# 9. 把 Java 做成系统服务（保证重启后自动启动）
sudo vim /etc/systemd/system/delta-esports.service
```

```ini
[Unit]
Description=Delta Esports Backend
After=network.target mysql.service

[Service]
Type=simple
User=你的用户名
WorkingDirectory=/opt/delta-esports
EnvironmentFile=/opt/delta-esports/env.sh
ExecStart=/usr/bin/java -Xmx512m -jar /opt/delta-esports/delta-esports-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable delta-esports
sudo systemctl start delta-esports
```

#### 优势
- 每一步你都看得见，出问题知道去哪查
- 不引入额外工具，服务器上只有必需的软件
- 适合 1-2 台服务器的小项目

#### 劣势
- 更新版本要手动跑脚本
- 没有回滚能力（除非你手动备份 jar）
- 服务器挂了要重建环境（很痛苦）

---

### 方式二：Docker Compose（推荐）

> **适用**：想一键启动、不想在服务器上装 JDK/nginx/MySQL

#### 为什么 Docker？

Docker 把应用和它的运行环境**打包在一起**。你不是在服务器上装 JDK，而是给 Java 一个"集装箱"——里面有 JDK、有 jar、有所有依赖。这个集装箱在哪都能跑：你的电脑、云服务器、任何 Linux。

```
传统方式：                  Docker 方式：
┌─────────┐              ┌─────────────────┐
│ 服务器    │              │ 服务器            │
│ ├─ JDK   │              │ ├─ Docker        │
│ ├─ nginx │              │ │  ├─ 容器1: Java │
│ ├─ MySQL │              │ │  ├─ 容器2: nginx│
│ └─ 你的App│              │ │  └─ 容器3: MySQL│
└─────────┘              │ └─────────────────┘
                         │ Docker 统一管理所有容器
```

**核心优势**：开发环境 = 测试环境 = 生产环境。不会出现"我电脑上好好的"。

#### 文件结构

```
delta-esports/
├── backend/
│   └── Dockerfile          # 后端镜像
├── frontend-mobile/
│   └── Dockerfile          # 前端构建 + nginx
├── frontend-admin/
│   └── Dockerfile
├── docker-compose.yml      # 一键编排所有服务
├── nginx/
│   └── nginx.conf          # 总 nginx 配置
└── .env                    # 环境变量（不提交 git）
```

#### 配置文件

**`backend/Dockerfile`**：
```dockerfile
# 第一阶段：构建
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# 第二阶段：运行（更小的镜像）
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
```

> **为什么两阶段构建？** Maven 镜像 700MB，JRE 镜像 200MB。只用 Maven 编译，把产物拷到 JRE 镜像运行，最终镜像只有 200MB。

**`frontend-mobile/Dockerfile`**：
```dockerfile
# 第一阶段：构建
FROM node:22-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# 第二阶段：用 nginx 托管静态文件
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html/m
COPY nginx-default.conf /etc/nginx/conf.d/default.conf
```

**`docker-compose.yml`**：
```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: delta_esports
      MYSQL_CHARSET: utf8mb4
    volumes:
      - mysql_data:/var/lib/mysql   # 数据持久化
    restart: unless-stopped

  backend:
    build: ./backend
    environment:
      SPRING_PROFILES_ACTIVE: prod
      JWT_SECRET: ${JWT_SECRET}
      MYSQL_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      CORS_ORIGINS: ${CORS_ORIGINS}
    depends_on:
      mysql:
        condition: service_healthy
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - mobile_dist:/usr/share/nginx/html/m
      - admin_dist:/usr/share/nginx/html/admin
      - ./ssl:/etc/nginx/ssl    # SSL 证书
    depends_on:
      - backend
    restart: unless-stopped

volumes:
  mysql_data:
  mobile_dist:
  admin_dist:
```

#### 日常操作

```bash
# 首次启动
docker compose up -d

# 更新代码后重新部署
git pull
docker compose up -d --build    # --build 重新构建有变化的镜像

# 查看日志
docker compose logs -f backend  # -f 实时跟踪

# 进入容器调试
docker compose exec backend sh

# 数据库备份
docker compose exec mysql mysqldump -u root -p delta_esports > backup.sql

# 停止
docker compose down
```

#### 优势
- `docker compose up -d` 一条命令启动全套服务
- 不依赖服务器预装软件（只要装了 Docker）
- 开发环境和生产环境完全一致
- 数据在 volume 里，容器删了数据还在
- 更新版本 `docker compose up -d --build` 即可

#### 劣势
- 需要学 Docker 基础概念（镜像、容器、volume）
- 多一层抽象，出问题要多排查一层

---

### 方式三：分离部署 + CI/CD

> **适用**：前端放 CDN / 对象存储，后端放云服务器。适合流量大或想省钱

#### 为什么分离？

前端构建产物就是静态文件。这些文件：

- 不会变（文件名带 hash）
- 不需要服务器"运行"
- 用户从全世界访问都需要快

所以最优解是：**前端放 CDN/对象存储，后端放服务器**。

```
用户（北京） → CDN 边缘节点（北京有缓存） → 秒开
用户（纽约） → CDN 边缘节点（纽约有缓存） → 秒开
用户发请求  → api.yjzdev.cn → 腾讯云轻量应用服务器 → MySQL
```

#### 前端部署到阿里云 OSS

```bash
# 构建
cd frontend-mobile && npm run build

# 安装 ossutil
# https://help.aliyun.com/document_detail/120075.html

# 上传
ossutil cp -r dist/ oss://delta-esports/m/ --cache-control "max-age=31536000"

# 用 CDN 加速（阿里云 CDN 回源到 OSS）
# 域名 cdn.yjzdev.cn → 对象存储 bucket
```

#### 后端还是用 JAR

和方式一一样，但 nginx 只做反向代理（不管静态文件了）。

#### 优势
- 前端加载极快（CDN 就近访问）
- 服务器只跑后端，压力小，可以用更便宜的机器
- CDN + OSS 极便宜，按流量付费
- 前后端独立部署，改前端不需要重启后端

#### 劣势
- 多了一个云服务（OSS/CDN）要管理
- 需要配置 DNS（前端域名、API 域名分离）

---

## 环境变量说明

你的 `application.yml` 里定义了这些需要设置的环境变量：

| 变量 | 用途 | 怎么生成 |
|------|------|---------|
| `JWT_SECRET` | JWT token 签名密钥 | `openssl rand -base64 48` |
| `MYSQL_PASSWORD` | 数据库密码 | 自己设置，至少 16 位 |
| `CORS_ORIGINS` | 允许跨域的前端域名 | `https://你的域名.com` |
| `SPRING_PROFILES_ACTIVE` | 环境切换 | 生产环境固定 `prod` |

**为什么 JWT_SECRET 必须改？** 你的 `application.yml` 里有默认值 `delta-esports-dev-secret-key-min-32-chars`。不换的话，任何看到你代码的人都能伪造登录 token。

**永远不要把 `.env` 提交到 git。**

---

## 更新部署的方式对比

| 方式 | 更新步骤 | 是否停机 | 能否回滚 |
|------|---------|---------|---------|
| 手动 | scp → systemctl restart | 是（几秒） | 手动替换 jar |
| Docker | git pull → docker compose up -d --build | 是（几秒） | `git revert` + 重新构建 |
| CI/CD | push → GitHub Actions → 自动部署 | 看配置 | 一键回滚 |

---

## 推荐路线

```
现在 → 方式一（手动部署）
     → 理解每步在干什么
     → 熟悉后迁移到方式二（Docker Compose）
     → 流量大了考虑方式三（前后端分离 + CDN）
```

**不推荐一开始就上 Docker**：出问题你看不懂容器日志，不知道是网络问题、权限问题还是代码问题。先手动跑一遍，知道每个部分怎么工作的，再封装。

---

## 检查清单

上线前逐项确认：

- [ ] `JWT_SECRET` 已改为随机字符串
- [ ] MySQL 密码不是 `root`
- [ ] `spring.profiles.active` = `prod`（关掉 H2 和 SQL 日志）
- [ ] CORS 只允许自己的域名
- [ ] HTTPS 证书已配（Let's Encrypt 免费）
- [ ] 后端不暴露 8080 端口（只监听 localhost）
- [ ] 防火墙只开 80/443
- [ ] systemd 或 Docker 配了 `restart`（机器重启后自动恢复）
- [ ] 数据库有定时备份
- [ ] `.env` 不在 git 仓库里
