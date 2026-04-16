# 部署步骤

以下步骤默认服务器上已有 Nginx，并且 `campus.blindot.org` 已解析到目标机器。

## 1. 拉取仓库

```bash
git clone https://github.com/CyrusAuyeung/smart-campus-platform.git
cd smart-campus-platform
```

## 2. 启动生产容器

```bash
docker compose -f docker-compose.deploy.yml up -d --build
```

说明：

- Web 仅绑定到 `127.0.0.1:3100`
- API 仅绑定到 `127.0.0.1:18081`
- PostgreSQL / Redis 默认不暴露公网端口
- RabbitMQ 管理端口仅绑定本机，使用 `127.0.0.1:15673`
- Prometheus 不暴露宿主机端口，避免与服务器现有监控冲突

## 3. 配置 Nginx

将 `deploy/nginx/campus.blindot.org.conf` 放到服务器的 Nginx 站点配置目录，然后 reload：

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## 4. 验证

- 访问 `http://campus.blindot.org`
- 检查首页是否能打开
- 检查 `/api/v1/system/health`
- 检查预约、活动抢票、库存对账页面

## 5. 压测

本地或服务器安装 k6 后，可执行：

```bash
k6 run tests/performance/event-flash-sale.js \
  -e API_BASE=http://127.0.0.1:18081/api \
  -e EVENT_ID=eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee
```
