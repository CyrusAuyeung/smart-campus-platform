"use client";

import { useEffect, useState } from "react";
import type { ReviewCase } from "@/lib/types/booking";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "/api";

export function ReviewPanel() {
  const [cases, setCases] = useState<ReviewCase[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;

    async function loadReviewCases() {
      try {
        const response = await fetch(`${API_BASE}/v1/reviews/payments`, { cache: "no-store" });
        if (!response.ok) {
          throw new Error("无法加载人工复核列表");
        }
        const payload = (await response.json()) as ReviewCase[];
        if (active) {
          setCases(payload);
        }
      } catch {
        if (active) {
          setCases([]);
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    void loadReviewCases();
    return () => {
      active = false;
    };
  }, []);

  return (
    <section className="section" id="review" aria-labelledby="review-title">
      <div className="header">
        <div>
          <h2 id="review-title">人工复核队列</h2>
          <p>展示支付确认与超时取消发生竞争后进入复核的记录。</p>
        </div>
      </div>
      <article className="panel">
        {loading ? <p>正在加载复核列表...</p> : null}
        {!loading && cases.length === 0 ? <p>当前没有待处理的人工复核记录。</p> : null}
        {cases.length > 0 ? (
          <div className="booking-list">
            {cases.map((item) => (
              <div key={item.transactionNo} className="booking-item">
                <strong>{item.transactionNo}</strong>
                <span>订单：{item.orderId}</span>
                <span>用户：{item.userId}</span>
                <span>状态：{item.status}</span>
                <span>负载：{item.callbackPayload}</span>
                <span>创建时间：{item.createdAt}</span>
              </div>
            ))}
          </div>
        ) : null}
      </article>
    </section>
  );
}