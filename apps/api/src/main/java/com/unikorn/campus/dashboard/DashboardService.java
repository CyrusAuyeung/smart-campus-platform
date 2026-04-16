package com.unikorn.campus.dashboard;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    public DashboardSummary loadSummary() {
        return new DashboardSummary(
            "智约校园",
            "多态空间调度与高并发活动综合平台",
            List.of(
                new MetricCard("学术空间缓冲", "前后各 5 分钟", "底层强制防重叠，前端额度计算不可见"),
                new MetricCard("体育设施占用", "1 小时离散槽位", "支持单场地与组合场地预订"),
                new MetricCard("活动抢票", "Redis + MQ", "先削峰再落库，防止洪峰直击数据库")
            ),
            List.of(
                "订单状态通过乐观锁与延迟消息防止幽灵支付",
                "规则引擎使用责任链模式支持身份、额度、信用分和爽约惩罚",
                "项目默认按 Docker Compose 一键拉起完整环境"
            )
        );
    }
}
