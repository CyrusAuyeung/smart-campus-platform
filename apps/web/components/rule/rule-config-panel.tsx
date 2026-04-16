"use client";

import { useEffect, useState } from "react";
import type { RuleConfigView } from "@/lib/types/booking";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "/api";

export function RuleConfigPanel() {
  const [rules, setRules] = useState<RuleConfigView[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;

    async function loadRules() {
      try {
        const response = await fetch(`${API_BASE}/v1/rules`, { cache: "no-store" });
        if (!response.ok) {
          throw new Error("无法加载规则配置");
        }
        const payload = (await response.json()) as RuleConfigView[];
        if (active) {
          setRules(payload);
        }
      } catch {
        if (active) {
          setRules([]);
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    void loadRules();
    return () => {
      active = false;
    };
  }, []);

  return (
    <section className="section" id="rules" aria-labelledby="rules-title">
      <div className="header">
        <div>
          <h2 id="rules-title">预约规则</h2>
          <p>当前系统正在使用的预约限制与约束条件。</p>
        </div>
      </div>
      <article className="panel">
        {loading ? <p>正在加载规则...</p> : null}
        {!loading && rules.length === 0 ? <p>当前没有可用规则。</p> : null}
        {rules.length > 0 ? (
          <div className="booking-list">
            {rules.map((rule) => (
              <div key={rule.ruleCode} className="booking-item">
                <strong>{rule.ruleCode}</strong>
                <span>优先级：{rule.priority}</span>
                <span>启用状态：{rule.enabled ? "启用" : "停用"}</span>
                <span>规则类型：预约规则</span>
                <span>配置：{rule.configJson}</span>
              </div>
            ))}
          </div>
        ) : null}
      </article>
    </section>
  );
}