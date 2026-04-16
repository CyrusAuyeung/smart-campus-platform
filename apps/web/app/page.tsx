import { BookingPlayground } from "@/components/booking/booking-playground";
import { EventFlashSale } from "@/components/event/event-flash-sale";
import { ReviewPanel } from "@/components/review/review-panel";
import { RuleConfigPanel } from "@/components/rule/rule-config-panel";
import type { UserItem } from "@/lib/types/booking";

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


export default function HomePage() {
  return (
    <main>
      <section className="hero product-hero" aria-labelledby="hero-title">
        <div className="header">
          <div>
            <span className="kicker">智约校园</span>
            <h1 id="hero-title">智约校园</h1>
            <p>
              一个面向校园日常场景的统一入口，支持教室预约、体育场地预约和热门活动抢票。
            </p>
          </div>
          <div className="badge-row" aria-label="核心能力标签">
            <span className="badge">空教室预约</span>
            <span className="badge">体育场地</span>
            <span className="badge">活动抢票</span>
            <span className="badge">信用约束</span>
          </div>
        </div>
        <div className="metrics" aria-label="产品入口概览">
          <div className="metric">
            <strong>学术空间</strong>
            <span>讨论室、会议室按连续时间预约</span>
          </div>
          <div className="metric">
            <strong>体育设施</strong>
            <span>支持离散槽位和组合场地单元</span>
          </div>
          <div className="metric">
            <strong>校园活动</strong>
            <span>提供热门活动抢票和库存状态查看</span>
          </div>
        </div>
      </section>

      <BookingPlayground />

      <EventFlashSale users={demoUsers} />

      <RuleConfigPanel />

      <ReviewPanel />
    </main>
  );
}
