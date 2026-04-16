"use client";

import { useEffect, useMemo, useState } from "react";
import type { BookingPaymentConfirmRequest, BookingReceipt, BootstrapPayload, CreditEvent, SportUnitItem } from "@/lib/types/booking";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "/api";

type ApiError = {
  message?: string;
};

function toDatetimeLocalValue(date: Date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
}

export function BookingPlayground() {
  const [bootstrap, setBootstrap] = useState<BootstrapPayload | null>(null);
  const [selectedUserId, setSelectedUserId] = useState("");
  const [selectedAcademicSpaceId, setSelectedAcademicSpaceId] = useState("");
  const [selectedFacilityId, setSelectedFacilityId] = useState("");
  const [selectedUnitIds, setSelectedUnitIds] = useState<string[]>([]);
  const [selectedSlots, setSelectedSlots] = useState<number[]>([1]);
  const [academicStart, setAcademicStart] = useState(toDatetimeLocalValue(new Date(Date.now() + 2 * 60 * 60 * 1000)));
  const [academicEnd, setAcademicEnd] = useState(toDatetimeLocalValue(new Date(Date.now() + 3 * 60 * 60 * 1000)));
  const [sportDate, setSportDate] = useState(new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().slice(0, 10));
  const [receipt, setReceipt] = useState<BookingReceipt | null>(null);
  const [bookings, setBookings] = useState<BookingReceipt[]>([]);
  const [creditEvents, setCreditEvents] = useState<CreditEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    async function loadBootstrap() {
      try {
        const response = await fetch(`${API_BASE}/v1/catalog/bootstrap`, { cache: "no-store" });
        if (!response.ok) {
          throw new Error("无法加载目录数据");
        }

        const payload = (await response.json()) as BootstrapPayload;
        if (!active) {
          return;
        }

        setBootstrap(payload);
        setSelectedUserId(payload.users[0]?.id ?? "");
        setSelectedAcademicSpaceId(payload.academicSpaces[0]?.id ?? "");
        setSelectedFacilityId(payload.sportFacilities[0]?.id ?? "");
      } catch (loadError) {
        if (active) {
          setError(loadError instanceof Error ? loadError.message : "目录加载失败");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    void loadBootstrap();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!selectedUserId) {
      setBookings([]);
      return;
    }

    let active = true;
    async function loadBookings() {
      try {
        const response = await fetch(`${API_BASE}/v1/bookings/users/${selectedUserId}`, { cache: "no-store" });
        if (!response.ok) {
          throw new Error("无法加载用户预约记录");
        }
        const payload = (await response.json()) as BookingReceipt[];
        if (active) {
          setBookings(payload);
        }

        const creditResponse = await fetch(`${API_BASE}/v1/credits/users/${selectedUserId}`, { cache: "no-store" });
        if (creditResponse.ok) {
          const creditPayload = (await creditResponse.json()) as CreditEvent[];
          if (active) {
            setCreditEvents(creditPayload);
          }
        }
      } catch (loadError) {
        if (active) {
          setError(loadError instanceof Error ? loadError.message : "预约记录加载失败");
        }
      }
    }

    void loadBookings();
    return () => {
      active = false;
    };
  }, [selectedUserId]);

  const availableUnits = useMemo(() => {
    if (!bootstrap) {
      return [] as SportUnitItem[];
    }
    return bootstrap.sportUnits.filter((item) => item.facilityId === selectedFacilityId);
  }, [bootstrap, selectedFacilityId]);

  const selectedUser = useMemo(() => bootstrap?.users.find((item) => item.id === selectedUserId) ?? null, [bootstrap, selectedUserId]);

  function toggleUnit(unitId: string) {
    setSelectedUnitIds((current) =>
      current.includes(unitId) ? current.filter((item) => item !== unitId) : [...current, unitId]
    );
  }

  function toggleSlot(slot: number) {
    setSelectedSlots((current) =>
      current.includes(slot) ? current.filter((item) => item !== slot).sort((a, b) => a - b) : [...current, slot].sort((a, b) => a - b)
    );
  }

  async function handleAcademicSubmit() {
    if (!selectedUserId || !selectedAcademicSpaceId) {
      setError("请先选择用户和学术空间");
      return;
    }

    setSubmitting(true);
    setError("");
    try {
      const response = await fetch(`${API_BASE}/v1/bookings/academic`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId: selectedUserId,
          spaceId: selectedAcademicSpaceId,
          startAt: new Date(academicStart).toISOString(),
          endAt: new Date(academicEnd).toISOString()
        })
      });

      if (!response.ok) {
        const payload = (await response.json()) as ApiError;
        throw new Error(payload.message ?? "学术空间预约失败");
      }

      const payload = (await response.json()) as BookingReceipt;
      setReceipt(payload);
      await refreshBookings(selectedUserId);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "学术空间预约失败");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleSportSubmit() {
    if (!selectedUserId || !selectedFacilityId || selectedUnitIds.length === 0 || selectedSlots.length === 0) {
      setError("请先选择用户、体育设施、场地单元和槽位");
      return;
    }

    setSubmitting(true);
    setError("");
    try {
      const response = await fetch(`${API_BASE}/v1/bookings/sport`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId: selectedUserId,
          facilityId: selectedFacilityId,
          unitIds: selectedUnitIds,
          bookingDate: sportDate,
          slotIndices: selectedSlots
        })
      });

      if (!response.ok) {
        const payload = (await response.json()) as ApiError;
        throw new Error(payload.message ?? "体育设施预约失败");
      }

      const payload = (await response.json()) as BookingReceipt;
      setReceipt(payload);
      await refreshBookings(selectedUserId);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "体育设施预约失败");
    } finally {
      setSubmitting(false);
    }
  }

  async function refreshBookings(userId: string) {
    const response = await fetch(`${API_BASE}/v1/bookings/users/${userId}`, { cache: "no-store" });
    if (!response.ok) {
      throw new Error("刷新预约记录失败");
    }
    const payload = (await response.json()) as BookingReceipt[];
    setBookings(payload);

    const creditResponse = await fetch(`${API_BASE}/v1/credits/users/${userId}`, { cache: "no-store" });
    if (creditResponse.ok) {
      setCreditEvents((await creditResponse.json()) as CreditEvent[]);
    }
  }

  async function updateBooking(orderId: string, action: "confirm" | "cancel" | "no-show") {
    if (!selectedUserId) {
      return;
    }

    setSubmitting(true);
    setError("");
    try {
      const response = action === "confirm"
        ? await fetch(`${API_BASE}/v1/payments/bookings/${orderId}/confirm`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              userId: selectedUserId,
              transactionNo: `TX-${Date.now()}`
            } satisfies BookingPaymentConfirmRequest)
          })
        : action === "no-show"
          ? await fetch(`${API_BASE}/v1/bookings/${orderId}/no-show/users/${selectedUserId}`, {
              method: "PATCH"
            })
        : await fetch(`${API_BASE}/v1/bookings/${orderId}/cancel/users/${selectedUserId}`, {
            method: "PATCH"
          });
      if (!response.ok) {
        const payload = (await response.json()) as ApiError;
        throw new Error(payload.message ?? "订单状态更新失败");
      }

      const payload = (await response.json()) as BookingReceipt;
      setReceipt(payload);
      await refreshBookings(selectedUserId);
    } catch (updateError) {
      setError(updateError instanceof Error ? updateError.message : "订单状态更新失败");
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <section className="panel"><p>正在加载联调目录数据...</p></section>;
  }

  if (!bootstrap) {
    return <section className="panel"><p>目录数据不可用。请先启动后端 API。</p></section>;
  }

  return (
    <section className="section" id="playground" aria-labelledby="playground-title">
      <div className="header">
        <div>
          <h2 id="playground-title">预约联调面板</h2>
          <p>直接调用后端预约接口，验证用户画像、规则校验、缓冲冲突和组合场地逻辑。</p>
        </div>
      </div>
      <div className="playground-grid">
        <article className="panel form-panel">
          <h3>联调上下文</h3>
          <label className="field">
            <span>用户</span>
            <select value={selectedUserId} onChange={(event) => setSelectedUserId(event.target.value)}>
              {bootstrap.users.map((user) => (
                <option key={user.id} value={user.id}>
                  {user.displayName} / {user.studentNo} / {user.roleCode}
                </option>
              ))}
            </select>
          </label>
          {selectedUser ? (
            <div className="summary-box">
              <strong>{selectedUser.displayName}</strong>
              <span>身份：{selectedUser.roleCode}</span>
              <span>信用分：{selectedUser.creditScore}</span>
              <span>近期爽约：{selectedUser.recentNoShowCount}</span>
              <span>
                预约状态：{selectedUser.recentNoShowCount >= 3
                  ? "暂停预约"
                  : selectedUser.recentNoShowCount >= 2
                    ? "预约时长受限"
                    : "正常"}
              </span>
            </div>
          ) : null}
          {selectedUser ? (
            <div className="summary-box">
              <strong>信用总览</strong>
              <span>信用等级：{selectedUser.creditScore >= 90 ? "稳定" : selectedUser.creditScore >= 80 ? "观察" : "受限"}</span>
              <span>近期待处理：{selectedUser.recentNoShowCount > 0 ? "需关注爽约影响" : "无异常"}</span>
            </div>
          ) : null}
          {error ? (
            <div className="rule-list">
              <span className="rule-pill reject">{error}</span>
            </div>
          ) : null}
        </article>

        <article className="panel form-panel">
          <h3>学术空间预约</h3>
          <label className="field">
            <span>学术空间</span>
            <select value={selectedAcademicSpaceId} onChange={(event) => setSelectedAcademicSpaceId(event.target.value)}>
              {bootstrap.academicSpaces.map((space) => (
                <option key={space.id} value={space.id}>
                  {space.name} / {space.code} / 容量 {space.capacity}
                </option>
              ))}
            </select>
          </label>
          <div className="field-row">
            <label className="field">
              <span>开始时间</span>
              <input type="datetime-local" value={academicStart} onChange={(event) => setAcademicStart(event.target.value)} />
            </label>
            <label className="field">
              <span>结束时间</span>
              <input type="datetime-local" value={academicEnd} onChange={(event) => setAcademicEnd(event.target.value)} />
            </label>
          </div>
          <button className="button primary" type="button" onClick={() => void handleAcademicSubmit()} disabled={submitting}>
            提交学术空间预约
          </button>
        </article>

        <article className="panel form-panel">
          <h3>体育设施预约</h3>
          <label className="field">
            <span>体育设施</span>
            <select
              value={selectedFacilityId}
              onChange={(event) => {
                setSelectedFacilityId(event.target.value);
                setSelectedUnitIds([]);
              }}
            >
              {bootstrap.sportFacilities.map((facility) => (
                <option key={facility.id} value={facility.id}>
                  {facility.name} / {facility.code}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>预约日期</span>
            <input type="date" value={sportDate} onChange={(event) => setSportDate(event.target.value)} />
          </label>
          <div className="choice-group">
            <span>场地单元</span>
            <div className="chip-row">
              {availableUnits.map((unit) => (
                <button
                  key={unit.id}
                  type="button"
                  className={`chip ${selectedUnitIds.includes(unit.id) ? "active" : ""}`}
                  onClick={() => toggleUnit(unit.id)}
                >
                  {unit.name}
                </button>
              ))}
            </div>
          </div>
          <div className="choice-group">
            <span>槽位</span>
            <div className="chip-row">
              {[1, 2, 3, 4, 5, 6].map((slot) => (
                <button
                  key={slot}
                  type="button"
                  className={`chip ${selectedSlots.includes(slot) ? "active" : ""}`}
                  onClick={() => toggleSlot(slot)}
                >
                  第 {slot} 槽
                </button>
              ))}
            </div>
          </div>
          <button className="button primary" type="button" onClick={() => void handleSportSubmit()} disabled={submitting}>
            提交体育设施预约
          </button>
        </article>
      </div>

      <div className="playground-grid secondary-grid">
        <article className="panel">
          <h3>最近一次返回</h3>
          {receipt ? (
            <div className="receipt-box">
              <strong>{receipt.orderNo}</strong>
              <span>{receipt.summary}</span>
              <span>状态：{receipt.status}</span>
              {receipt.displayStartAt ? <span>展示时间：{receipt.displayStartAt} 至 {receipt.displayEndAt}</span> : null}
              {receipt.effectiveStartAt ? <span>系统锁定：{receipt.effectiveStartAt} 至 {receipt.effectiveEndAt}</span> : null}
              {receipt.bookingDate ? <span>预约日期：{receipt.bookingDate}</span> : null}
              {receipt.slotIndices.length > 0 ? <span>槽位：{receipt.slotIndices.join(", ")}</span> : null}
              {receipt.ruleResults.length > 0 ? (
                <div className="rule-list">
                  {receipt.ruleResults.map((result) => (
                    <span key={`${result.ruleCode}-${result.message}`} className={`rule-pill ${result.allowed ? "pass" : "reject"}`}>
                      {result.message}
                    </span>
                  ))}
                </div>
              ) : null}
            </div>
          ) : (
            <p>提交任一预约后，这里会展示后端回执。</p>
          )}
        </article>

        <article className="panel">
          <h3>当前用户预约记录</h3>
          {bookings.length > 0 ? (
            <div className="booking-list">
              {bookings.map((item) => (
                <div key={item.orderId} className="booking-item">
                  <strong>{item.orderNo}</strong>
                  <span>{item.summary}</span>
                  <span>{item.businessType} / {item.status}</span>
                  {item.displayStartAt ? <span>{item.displayStartAt} 至 {item.displayEndAt}</span> : null}
                  {item.bookingDate ? <span>{item.bookingDate} / 槽位 {item.slotIndices.join(", ")}</span> : null}
                  {item.status === "PENDING_PAYMENT" ? (
                    <div className="chip-row">
                      <button className="button secondary" type="button" onClick={() => void updateBooking(item.orderId, "confirm")}>
                        确认支付
                      </button>
                      <button className="button secondary" type="button" onClick={() => void updateBooking(item.orderId, "cancel")}>
                        取消预约
                      </button>
                    </div>
                  ) : null}
                  {item.status === "CONFIRMED" ? (
                    <div className="chip-row">
                      <button className="button secondary" type="button" onClick={() => void updateBooking(item.orderId, "no-show")}>
                        标记爽约
                      </button>
                    </div>
                  ) : null}
                </div>
              ))}
            </div>
          ) : (
            <p>当前用户还没有预约记录。</p>
          )}
        </article>

        <article className="panel">
          <h3>信用事件</h3>
          {creditEvents.length > 0 ? (
            <div className="booking-list">
              {creditEvents.map((item) => (
                <div key={item.id} className="booking-item">
                  <strong>{item.eventType}</strong>
                  <span>分值变化：{item.scoreDelta}</span>
                  <span>原因：{item.reason}</span>
                  <span>时间：{item.createdAt}</span>
                </div>
              ))}
            </div>
          ) : (
            <p>当前用户还没有信用事件记录。</p>
          )}
        </article>
      </div>
    </section>
  );
}
