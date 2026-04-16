"use client";

import { useEffect, useState } from "react";
import type { EventHealthSnapshot, EventItem, EventReconciliationSnapshot, EventRepairAction, EventReservationAudit, EventReserveReceipt, UserItem } from "@/lib/types/booking";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";

type EventFlashSaleProps = {
  users: UserItem[];
};

type ApiError = {
  message?: string;
};

export function EventFlashSale({ users }: EventFlashSaleProps) {
  const [events, setEvents] = useState<EventItem[]>([]);
  const [selectedUserId, setSelectedUserId] = useState(users[0]?.id ?? "");
  const [receipt, setReceipt] = useState<EventReserveReceipt | null>(null);
  const [health, setHealth] = useState<EventHealthSnapshot | null>(null);
  const [audits, setAudits] = useState<EventReservationAudit[]>([]);
  const [reconciliation, setReconciliation] = useState<EventReconciliationSnapshot[]>([]);
  const [repairs, setRepairs] = useState<EventRepairAction[]>([]);
  const [loading, setLoading] = useState(true);
  const [submittingEventId, setSubmittingEventId] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    async function loadEvents() {
      try {
        const response = await fetch(`${API_BASE}/v1/events`, { cache: "no-store" });
        if (!response.ok) {
          throw new Error("无法加载活动列表");
        }
        const payload = (await response.json()) as EventItem[];
        if (active) {
          setEvents(payload);
        }

        const healthResponse = await fetch(`${API_BASE}/v1/events/health`, { cache: "no-store" });
        if (healthResponse.ok && active) {
          setHealth((await healthResponse.json()) as EventHealthSnapshot);
        }

        const auditResponse = await fetch(`${API_BASE}/v1/events/audits`, { cache: "no-store" });
        if (auditResponse.ok && active) {
          setAudits((await auditResponse.json()) as EventReservationAudit[]);
        }

        const reconciliationResponse = await fetch(`${API_BASE}/v1/events/reconciliation`, { cache: "no-store" });
        if (reconciliationResponse.ok && active) {
          setReconciliation((await reconciliationResponse.json()) as EventReconciliationSnapshot[]);
        }

        const repairResponse = await fetch(`${API_BASE}/v1/events/repairs`, { cache: "no-store" });
        if (repairResponse.ok && active) {
          setRepairs((await repairResponse.json()) as EventRepairAction[]);
        }
      } catch (loadError) {
        if (active) {
          setError(loadError instanceof Error ? loadError.message : "活动加载失败");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    void loadEvents();
    return () => {
      active = false;
    };
  }, []);

  async function handleReserve(eventId: string) {
    if (!selectedUserId) {
      setError("请先选择参与抢票的用户");
      return;
    }

    setSubmittingEventId(eventId);
    setError("");
    try {
      const response = await fetch(`${API_BASE}/v1/events/reserve`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId: selectedUserId,
          eventId
        })
      });

      if (!response.ok) {
        const payload = (await response.json()) as ApiError;
        throw new Error(payload.message ?? "抢票失败");
      }

      const payload = (await response.json()) as EventReserveReceipt;
      setReceipt(payload);

      const refresh = await fetch(`${API_BASE}/v1/events`, { cache: "no-store" });
      if (refresh.ok) {
        setEvents((await refresh.json()) as EventItem[]);
      }

      const healthRefresh = await fetch(`${API_BASE}/v1/events/health`, { cache: "no-store" });
      if (healthRefresh.ok) {
        setHealth((await healthRefresh.json()) as EventHealthSnapshot);
      }

      const auditRefresh = await fetch(`${API_BASE}/v1/events/audits`, { cache: "no-store" });
      if (auditRefresh.ok) {
        setAudits((await auditRefresh.json()) as EventReservationAudit[]);
      }

      const reconciliationRefresh = await fetch(`${API_BASE}/v1/events/reconciliation`, { cache: "no-store" });
      if (reconciliationRefresh.ok) {
        setReconciliation((await reconciliationRefresh.json()) as EventReconciliationSnapshot[]);
      }

      const repairRefresh = await fetch(`${API_BASE}/v1/events/repairs`, { cache: "no-store" });
      if (repairRefresh.ok) {
        setRepairs((await repairRefresh.json()) as EventRepairAction[]);
      }
    } catch (reserveError) {
      setError(reserveError instanceof Error ? reserveError.message : "抢票失败");
    } finally {
      setSubmittingEventId("");
    }
  }

  async function handleReconcile(eventId: string) {
    setError("");
    try {
      const response = await fetch(`${API_BASE}/v1/events/${eventId}/reconcile`, {
        method: "POST"
      });
      if (!response.ok) {
        throw new Error("库存修复失败");
      }

      const reconciliationRefresh = await fetch(`${API_BASE}/v1/events/reconciliation`, { cache: "no-store" });
      if (reconciliationRefresh.ok) {
        setReconciliation((await reconciliationRefresh.json()) as EventReconciliationSnapshot[]);
      }

      const repairRefresh = await fetch(`${API_BASE}/v1/events/repairs`, { cache: "no-store" });
      if (repairRefresh.ok) {
        setRepairs((await repairRefresh.json()) as EventRepairAction[]);
      }
    } catch (reconcileError) {
      setError(reconcileError instanceof Error ? reconcileError.message : "库存修复失败");
    }
  }

  return (
    <section className="section" id="events" aria-labelledby="events-title">
      <div className="header">
        <div>
          <h2 id="events-title">热门活动抢票</h2>
          <p>演示 Redis 库存预热、原子扣减与 MQ 异步落单的第一版链路。</p>
        </div>
      </div>
      <div className="playground-grid secondary-grid">
        <article className="panel form-panel">
          <h3>抢票入口</h3>
          <label className="field">
            <span>参与用户</span>
            <select value={selectedUserId} onChange={(event) => setSelectedUserId(event.target.value)}>
              {users.map((user) => (
                <option key={user.id} value={user.id}>
                  {user.displayName} / {user.studentNo}
                </option>
              ))}
            </select>
          </label>
          {loading ? <p>正在加载活动...</p> : null}
          {error ? <p className="error-text">{error}</p> : null}
          <div className="booking-list">
            {events.map((event) => (
              <div key={event.id} className="booking-item">
                <strong>{event.title}</strong>
                <span>{event.eventCode}</span>
                <span>库存：{event.availableStock} / {event.totalStock}</span>
                <span>限购：每人 {event.limitPerUser} 张</span>
                <button
                  className="button primary"
                  type="button"
                  onClick={() => void handleReserve(event.id)}
                  disabled={submittingEventId === event.id}
                >
                  {submittingEventId === event.id ? "提交中..." : "立即抢票"}
                </button>
              </div>
            ))}
          </div>
        </article>

        <article className="panel">
          <h3>活动回执</h3>
          {receipt ? (
            <div className="receipt-box">
              <strong>{receipt.status}</strong>
              <span>{receipt.message}</span>
              <span>请求号：{receipt.requestId}</span>
              <span>剩余库存：{receipt.remainingStock}</span>
            </div>
          ) : (
            <p>发起一次抢票后，这里会展示排队回执。</p>
          )}
          {health ? (
            <div className="receipt-box">
              <strong>活动健康</strong>
              <span>待处理：{health.pendingOrders}</span>
              <span>已确认：{health.confirmedOrders}</span>
              <span>失败：{health.failedOrders}</span>
            </div>
          ) : null}
          {reconciliation.length > 0 ? (
            <div className="receipt-box">
              <strong>库存对账</strong>
              {reconciliation.map((item) => (
                <div key={item.eventId} className="booking-item">
                  <span>{item.eventId}</span>
                  <span>DB {item.databaseAvailableStock} / Cache {item.cacheAvailableStock}</span>
                  <span>{item.consistent ? "一致" : "不一致"}</span>
                  {!item.consistent ? (
                    <button className="button secondary" type="button" onClick={() => void handleReconcile(item.eventId)}>
                      修复库存
                    </button>
                  ) : null}
                </div>
              ))}
            </div>
          ) : null}
        </article>
      </div>

      <article className="panel section">
        <h3>抢票审计</h3>
        {audits.length > 0 ? (
          <div className="booking-list">
            {audits.map((item) => (
              <div key={item.requestId} className="booking-item">
                <strong>{item.requestId}</strong>
                <span>活动：{item.eventId}</span>
                <span>用户：{item.userId}</span>
                <span>状态：{item.status}</span>
                {item.failureReason ? <span>失败原因：{item.failureReason}</span> : null}
                <span>时间：{item.createdAt}</span>
              </div>
            ))}
          </div>
        ) : (
          <p>当前还没有抢票审计记录。</p>
        )}
      </article>

      <article className="panel section">
        <h3>修复历史</h3>
        {repairs.length > 0 ? (
          <div className="booking-list">
            {repairs.map((item, index) => (
              <div key={`${item.eventId}-${item.createdAt}-${index}`} className="booking-item">
                <strong>{item.actionType}</strong>
                <span>活动：{item.eventId}</span>
                <span>缓存库存：{item.previousCacheStock ?? "无"} -> 数据库存量：{item.databaseStock}</span>
                <span>执行者：{item.operator}</span>
                <span>时间：{item.createdAt}</span>
              </div>
            ))}
          </div>
        ) : (
          <p>当前还没有修复历史。</p>
        )}
      </article>
    </section>
  );
}
