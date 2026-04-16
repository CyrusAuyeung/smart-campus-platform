# 压测说明

## k6 抢票压测

用于验证活动抢票链路在并发请求下的返回情况与库存一致性。

### 运行方式

```bash
k6 run tests/performance/event-flash-sale.js \
  -e API_BASE=http://localhost:8080/api \
  -e EVENT_ID=eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee
```

### 观察点

- 返回状态中不应出现大量 5xx
- 活动健康快照中的失败数、确认数应与审计记录吻合
- `GET /api/v1/events/reconciliation` 应显示库存一致

### 赛题答辩建议

压测前后分别截图：

- 活动列表库存
- 活动健康快照
- 活动审计记录
- 库存对账结果
