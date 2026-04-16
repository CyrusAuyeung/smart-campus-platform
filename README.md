# 智约校园

面向高校空间预约与热门活动抢票的综合平台。

当前仓库目标是直接作为参赛成品工程仓库使用，重点覆盖：

- 多态空间预约：学术空间连续时间 + 隐形缓冲，体育设施离散槽位 + 组合场地
- 热门活动抢票：Redis 预热、Lua 原子扣减、MQ 异步下单、失败回滚、库存对账与修复
- 订单状态机：支付确认、超时取消、爽约扣分、人工复核入口
- 规则配置化：本科生时长、爽约惩罚、信用分规则从 `rule_definition` 读取配置

## 项目结构

```text
apps/
  web/    Next.js 15 前端
  api/    Spring Boot 后端
docs/
  architecture.md
infra/
  prometheus/
  grafana/
tests/
  performance/
```

## 技术栈

- 前端：Next.js 15、React 19、TypeScript
- 后端：Spring Boot、PostgreSQL、Redis、RabbitMQ、Flyway
- 工程化：Docker Compose、GitHub Actions、Prometheus、Grafana、k6

## 核心能力

### 1. 空间预约

- 学术空间支持连续时间预约，并自动插入前后 5 分钟缓冲
- 体育设施支持离散槽位和组合场地单元
- 订单支持待支付、已确认、已取消、已爽约等状态

### 2. 活动抢票

- Redis Lua 原子扣减库存
- RabbitMQ 异步消费者落单
- 失败回滚 Redis 与数据库库存
- 活动健康快照、库存对账、手动修复与修复历史

### 3. 规则与信用

- 本科生预约时长限制可配置
- 爽约惩罚规则可配置
- 信用分规则可配置
- 信用事件与近期爽约次数入库并展示

## 运行方式

### 方式一：Docker Compose

```bash
docker compose up --build
```

默认服务：

- Web: <http://localhost:3000>
- API: <http://localhost:8080/api>
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- RabbitMQ 管理台: <http://localhost:15672>
- Grafana: <http://localhost:3001>

### 生产部署

生产环境建议使用：

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

部署说明与 Nginx 配置模板见：

- [deploy/docs/deployment.md](deploy/docs/deployment.md)
- [deploy/nginx/campus.blindot.org.conf](deploy/nginx/campus.blindot.org.conf)

### 方式二：本地分开运行

前端：

```bash
npm install
npm run dev:web
```

后端：

```bash
cd apps/api
mvn spring-boot:run
```

## 关键接口

### 预约

- `POST /api/v1/bookings/academic`
- `POST /api/v1/bookings/sport`
- `GET /api/v1/bookings/users/{userId}`
- `PATCH /api/v1/bookings/{orderId}/cancel/users/{userId}`
- `PATCH /api/v1/bookings/{orderId}/no-show/users/{userId}`

### 支付与复核

- `POST /api/v1/payments/bookings/{orderId}/confirm`
- `GET /api/v1/reviews/payments`

### 活动

- `GET /api/v1/events`
- `POST /api/v1/events/reserve`
- `GET /api/v1/events/health`
- `GET /api/v1/events/audits`
- `GET /api/v1/events/reconciliation`
- `POST /api/v1/events/{eventId}/reconcile`
- `GET /api/v1/events/repairs`

### 规则与信用

- `GET /api/v1/rules`
- `GET /api/v1/credits/users/{userId}`
- `GET /api/v1/catalog/bootstrap`

## 压测

项目内置了一个最小 `k6` 抢票压测脚本：

```bash
k6 run tests/performance/event-flash-sale.js \
  -e API_BASE=http://localhost:8080/api \
  -e EVENT_ID=eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee
```

补充说明见 [tests/performance/README.md](tests/performance/README.md)。

## GitHub 仓库使用建议

初始化 Git 仓库后，建议直接推送以下内容：

- `apps/` 前后端源码
- `docker-compose.yml`
- `.github/workflows/ci.yml`
- `tests/performance/`
- `docs/architecture.md`
- `README.md`

当前仓库已经适合直接作为 GitHub 项目仓库基础版本使用。
