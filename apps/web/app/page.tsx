import { BookingPlayground } from "@/components/booking/booking-playground";
import { EventFlashSale } from "@/components/event/event-flash-sale";
import { FlowStep } from "@/components/flow-step";
import { ModuleCard } from "@/components/module-card";
import { ReviewPanel } from "@/components/review/review-panel";
import { RuleConfigPanel } from "@/components/rule/rule-config-panel";
import type { UserItem } from "@/lib/types/booking";

const modules = [
  {
    title: "多态空间预约",
    description: "用两套底层模型统一服务校园空间预约，前台统一体验，后台显式处理资源差异。",
    highlights: [
      "学术空间使用连续时间区间，自动插入前后 5 分钟缓冲",
      "体育设施使用离散 1 小时槽位，支持单场地与组合场地预订",
      "统一订单层只聚合业务上下文，不掩盖资源本体差异"
    ]
  },
  {
    title: "热门活动抢票",
    description: "高并发活动链路默认走缓存预热和消息队列，不把洪峰直接灌进数据库。",
    highlights: [
      "Redis 预热库存与限购规则",
      "Lua 原子扣减确保不超卖",
      "RabbitMQ 异步落单与补偿对账"
    ]
  },
  {
    title: "状态机与规则引擎",
    description: "订单状态迁移和预约规则完全解耦，避免核心代码堆积 if-else。",
    highlights: [
      "订单状态统一走状态机应用服务",
      "乐观锁 + 延迟消息防止幽灵支付",
      "责任链规则引擎支持额度、身份、信用分与爽约惩罚"
    ]
  }
] as const;

const demoUsers: UserItem[] = [
  {
    id: "11111111-1111-1111-1111-111111111111",
    studentNo: "20240001",
    displayName: "林知远",
    roleCode: "UNDERGRADUATE",
    creditScore: 100,
    recentNoShowCount: 0
  },
  {
    id: "22222222-2222-2222-2222-222222222222",
    studentNo: "20240002",
    displayName: "沈语禾",
    roleCode: "POSTGRADUATE",
    creditScore: 92,
    recentNoShowCount: 0
  }
];

const flows = [
  {
    step: "01",
    title: "预约建模",
    description: "同一个平台支持两种时间模型和两种空间拓扑，不靠一张万能预约表硬糊过去。",
    bullets: [
      "学术空间：PostgreSQL 时间区间 + 冲突拦截",
      "体育设施：场地单元 + 组合拓扑 + 槽位占用",
      "下单前统一经过规则链校验"
    ]
  },
  {
    step: "02",
    title: "高并发防超卖",
    description: "活动开抢时只让 Redis 和 MQ 吃峰值，数据库负责最终事实与审计。",
    bullets: [
      "请求先经过原子脚本校验库存和限购",
      "成功后异步落库生成订单",
      "异常通过补偿任务回滚库存并修正状态"
    ]
  },
  {
    step: "03",
    title: "一致性与可解释性",
    description: "支付、取消、爽约和规则命中都能给出稳定状态和明确原因。",
    bullets: [
      "支付回调与超时取消竞态由乐观锁仲裁",
      "状态日志保留完整迁移轨迹",
      "前端直接展示规则命中原因和风控反馈"
    ]
  }
] as const;

export default function HomePage() {
  return (
    <main>
      <section className="hero" aria-labelledby="hero-title">
        <span className="kicker">UniKorn Web Competition</span>
        <div className="header">
          <div>
            <h1 id="hero-title">智约校园</h1>
            <p>
              面向高校日常预约与热门活动抢票的综合平台，重点解决多态空间建模、秒杀不超卖、订单状态一致性和规则可配置问题。
            </p>
          </div>
          <div className="badge-row" aria-label="核心能力标签">
            <span className="badge">连续时间缓冲</span>
            <span className="badge">离散槽位组合</span>
            <span className="badge">Redis + MQ</span>
            <span className="badge">FSM + 规则链</span>
          </div>
        </div>
        <div className="hero-actions">
          <a className="button primary" href="#modules">查看核心模块</a>
          <a className="button secondary" href="#flows">查看系统链路</a>
        </div>
        <div className="metrics" aria-label="项目关键指标目标">
          <div className="metric">
            <strong>LCP ≤ 2.5s</strong>
            <span>首屏加载围绕核心 Web 指标设计</span>
          </div>
          <div className="metric">
            <strong>0 超卖</strong>
            <span>秒杀库存通过原子扣减与异步补偿兜底</span>
          </div>
          <div className="metric">
            <strong>全链路容器化</strong>
            <span>前端、后端、数据库、缓存、队列一键启动</span>
          </div>
        </div>
      </section>

      <section className="section" id="modules" aria-labelledby="modules-title">
        <div className="header">
          <div>
            <h2 id="modules-title">三大核心模块</h2>
            <p>整套系统围绕赛题中的领域建模、高并发和逻辑解耦三条主线组织。</p>
          </div>
        </div>
        <div className="module-grid">
          {modules.map((item) => (
            <ModuleCard key={item.title} {...item} />
          ))}
        </div>
      </section>

      <section className="section" id="flows" aria-labelledby="flows-title">
        <div className="header">
          <div>
            <h2 id="flows-title">系统主链路</h2>
            <p>当前骨架已经按后续实现路径把最关键的三条链路固定下来，便于持续迭代。</p>
          </div>
        </div>
        <div className="flow-grid">
          {flows.map((item) => (
            <FlowStep key={item.step} {...item} />
          ))}
        </div>
      </section>

      <BookingPlayground />

      <EventFlashSale users={demoUsers} />

      <RuleConfigPanel />

      <ReviewPanel />
    </main>
  );
}
