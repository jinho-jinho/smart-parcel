import React, { useEffect, useMemo, useState } from "react";
import Header from "../components/Header";
import styles from "./Stats.module.css";
import {
  fetchStatsByChute,
  fetchStatsByErrorCode,
  fetchStatsDaily,
  fetchErrorRate,
} from "../api/stats";
import { fetchGroups } from "../api/groups";

const COLORS = ["#2563eb", "#f97316", "#10b981", "#fbbf24", "#8b5cf6", "#0ea5e9"];

const todayIso = () => new Date().toISOString().slice(0, 10);
const daysAgoIso = (days) => {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return date.toISOString().slice(0, 10);
};

const DEFAULT_FORM = {
  from: daysAgoIso(6),
  to: todayIso(),
  groupId: "",
};

export default function Stats() {
  const [form, setForm] = useState(DEFAULT_FORM);
  const [query, setQuery] = useState(DEFAULT_FORM);
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [stats, setStats] = useState({
    byChute: [],
    daily: [],
    byErrorCode: [],
    errorRate: { totalProcessed: 0, totalErrors: 0, errorRatePercent: 0 },
  });

  useEffect(() => {
    const loadGroups = async () => {
      try {
        const response = await fetchGroups({ page: 0, size: 100 });
        setGroups(response?.data?.content ?? []);
      } catch {
        // ignore dropdown errors
      }
    };
    loadGroups();
  }, []);

  useEffect(() => {
    const loadStats = async () => {
      setLoading(true);
      setError("");
      try {
        const params = normalizeParams(query);
        const [byChute, daily, byErrorCode, errorRate] = await Promise.all([
          fetchStatsByChute(params),
          fetchStatsDaily(params),
          fetchStatsByErrorCode(params),
          fetchErrorRate(params),
        ]);
        setStats({
          byChute: byChute ?? [],
          daily: daily ?? [],
          byErrorCode: byErrorCode ?? [],
          errorRate: errorRate ?? {
            totalProcessed: 0,
            totalErrors: 0,
            errorRatePercent: 0,
          },
        });
      } catch (err) {
        setError(err?.response?.data?.message || "통계 데이터를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    };
    loadStats();
  }, [query]);

  const handleInputChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleApplyFilters = (event) => {
    event.preventDefault();
    setQuery(form);
  };

  const handleReset = () => {
    setForm(DEFAULT_FORM);
    setQuery(DEFAULT_FORM);
  };

  const errorRateData = useMemo(() => {
    const processed = stats.errorRate.totalProcessed || 0;
    const errors = stats.errorRate.totalErrors || 0;
    return [
      { label: "오류", count: errors },
      { label: "정상", count: processed },
    ];
  }, [stats.errorRate]);

  return (
    <div className={styles.page}>
      <Header />
      <main className={styles.main}>
        <section className={styles.toolbar}>
          <div>
            <h1 className={styles.heading}>통계 대시보드</h1>
            <p className={styles.subtitle}>
              특정 기간과 그룹을 기준으로 분류 성과를 확인합니다.
            </p>
          </div>
          <form className={styles.filters} onSubmit={handleApplyFilters}>
            <div className={styles.filterField}>
              <label htmlFor="from">시작일</label>
              <input
                type="date"
                id="from"
                name="from"
                value={form.from}
                onChange={handleInputChange}
              />
            </div>
            <div className={styles.filterField}>
              <label htmlFor="to">종료일</label>
              <input
                type="date"
                id="to"
                name="to"
                value={form.to}
                onChange={handleInputChange}
              />
            </div>
            <div className={styles.filterField}>
              <label htmlFor="groupId">분류 그룹</label>
              <select
                id="groupId"
                name="groupId"
                value={form.groupId}
                onChange={handleInputChange}
              >
                <option value="">전체</option>
                {groups.map((group) => (
                  <option key={group.id} value={group.id}>
                    {group.groupName}
                  </option>
                ))}
              </select>
            </div>
            <div className={styles.filterActions}>
              <button type="button" onClick={handleReset}>
                초기화
              </button>
              <button type="submit" className={`${styles.primary} ${styles.applyButton}`}>
                적용
              </button>
            </div>
          </form>
        </section>

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.grid}>
          <ChartCard title="라인별 분류 건수" loading={loading} empty={!stats.byChute.length}>
            <HorizontalBarChart data={stats.byChute} />
          </ChartCard>

          <ChartCard title="일자별 분류 건수" loading={loading} empty={!stats.daily.length}>
            <VerticalBarChart data={stats.daily} />
          </ChartCard>

          <ChartCard title="오류 발생 건수" loading={loading} empty={!stats.byErrorCode.length}>
            <PieChart data={stats.byErrorCode} variant="doughnut" />
          </ChartCard>

          <ChartCard title="전체 처리 건수 중 오류 비율" loading={loading}>
            <ErrorRatePanel data={stats.errorRate} chartData={errorRateData} />
          </ChartCard>
        </div>
      </main>
    </div>
  );
}

function ChartCard({ title, loading, empty, children }) {
  return (
    <section className={styles.card}>
      <header className={styles.cardHeader}>
        <h3>{title}</h3>
      </header>
      <div className={styles.cardBody}>
        {loading ? (
          <div className={styles.placeholder}>불러오는 중...</div>
        ) : empty ? (
          <div className={styles.placeholder}>표시할 데이터가 없습니다.</div>
        ) : (
          children
        )}
      </div>
    </section>
  );
}

function HorizontalBarChart({ data }) {
  if (!data?.length) return null;
  const max = Math.max(...data.map((item) => item.count), 1);
  return (
    <div className={`${styles.chartArea} ${styles.centerY}`}>
      <div className={styles.scrollY}>
        <ul className={styles.hBarList}>
          {data.map((item, index) => {
            const percent = ((item.count || 0) / max) * 100;
            return (
              <li key={`${item.label}-${index}`}>
                <span className={styles.hBarLabel}>{item.label || "미지정"}</span>
                <div className={styles.hBarTrack}>
                  <div
                    className={styles.hBarValue}
                    style={{ width: `${percent}%` }}
                  />
                </div>
                <span className={styles.hBarCount}>{item.count.toLocaleString("ko-KR")}건</span>
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}

function VerticalBarChart({ data }) {
  if (!data?.length) return null;
  const max = Math.max(...data.map((item) => item.count), 1);
  const minWidth = Math.max(data.length * 60, 360);
  return (
    <div className={`${styles.chartArea} ${styles.bottomAlign}`}>
      <div className={styles.scrollX}>
        <div className={styles.vChart} style={{ minWidth }}>
          {data.map((item, index) => {
            const percent = ((item.count || 0) / max) * 100;
            return (
              <div key={`${item.date}-${index}`} className={styles.vBarWrapper}>
                <div className={styles.vBarTrack}>
                  <div
                    className={styles.vBarValue}
                    style={{ height: `${percent}%` }}
                  />
                </div>
                <span className={styles.vBarCount}>{(item.count || 0).toLocaleString("ko-KR")}건</span>
                <span className={styles.vBarLabel}>{formatDateLabel(item.date)}</span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

function ErrorRatePanel({ data, chartData }) {
  const processed = data.totalProcessed || 0;
  const errors = data.totalErrors || 0;
  const total = processed + errors;
  const rate = total === 0 ? 0 : (errors * 100) / total;
  return (
    <div className={styles.rateWrap}>
      <PieChart data={chartData} total={total} variant="ring" />
      <div className={styles.rateStats}>
        <div>
          <span className={styles.metricLabel}>총 처리</span>
          <strong>{total.toLocaleString("ko-KR")}건</strong>
        </div>
        <div>
          <span className={styles.metricLabel}>분류 이력(정상)</span>
          <strong>{processed.toLocaleString("ko-KR")}건</strong>
        </div>
        <div>
          <span className={styles.metricLabel}>오류 이력</span>
          <strong>{errors.toLocaleString("ko-KR")}건</strong>
        </div>
        <div>
          <span className={styles.metricLabel}>오류율</span>
          <strong>{rate.toFixed(2)}%</strong>
        </div>
      </div>
    </div>
  );
}

function PieChart({ data, total, variant = "pie" }) {
  if (!data?.length) return <div className={styles.placeholder}>데이터가 없습니다.</div>;
  const sum = total ?? data.reduce((acc, item) => acc + (item.count || 0), 0);
  if (!sum) {
    return <div className={styles.placeholder}>데이터가 없습니다.</div>;
  }

  let start = 0;
  const segments = data.map((item, index) => {
    const pct = ((item.count || 0) / sum) * 100;
    const end = start + pct;
    const segment = `${COLORS[index % COLORS.length]} ${start}% ${end}%`;
    start = end;
    return segment;
  });

  return (
    <div className={styles.pieWrap}>
      <div
        className={`${styles.pie} ${variant === "ring" ? styles.pieRing : ""}`}
        style={{ backgroundImage: `conic-gradient(${segments.join(",")})` }}
      >
        <div className={styles.pieCenter}>
          <span>총 {sum.toLocaleString("ko-KR")}건</span>
        </div>
      </div>
      <ul className={styles.legend}>
        {data.map((item, index) => {
          const pct = sum ? ((item.count || 0) / sum) * 100 : 0;
          return (
            <li key={`${item.label}-${index}`}>
              <span
                className={styles.legendDot}
                style={{ backgroundColor: COLORS[index % COLORS.length] }}
              />
              <span className={styles.legendLabel}>{item.label || "미지정"}</span>
              <span className={styles.legendCount}>
                {item.count.toLocaleString("ko-KR")}건 ({pct.toFixed(1)}%)
              </span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

function normalizeParams(query) {
  const params = {};
  if (query.from) {
    params.from = new Date(`${query.from}T00:00:00`).toISOString();
  }
  if (query.to) {
    params.to = new Date(`${query.to}T23:59:59`).toISOString();
  }
  if (query.groupId) {
    params.groupId = query.groupId;
  }
  return params;
}

function formatDateLabel(date) {
  if (!date) return "-";
  try {
    return new Date(date).toLocaleDateString("ko-KR", {
      month: "numeric",
      day: "numeric",
    });
  } catch {
    return date;
  }
}
