import React, { useEffect, useState } from "react";
import styles from "./Groups.module.css";
import Header from "../components/Header";
import { authStore } from "../store/auth.store";
import { isManager } from "../utils/permission";
import { fetchGroups, createGroup } from "../api/groups";

export default function Groups() {
  const user = authStore((s) => s.user);
  const admin = isManager(user);

  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [msg, setMsg] = useState("");
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ name: "", code: "", description: "" });

  async function load() {
    setLoading(true);
    try {
      const res = await fetchGroups();
      setItems(res?.data?.content ?? []);
    } catch (e) {
      setMsg(e?.response?.data?.message || "목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  const onCreate = async () => {
    try {
      await createGroup(form);
      setShowCreate(false);
      setForm({ name: "", code: "", description: "" });
      load();
    } catch (e) {
      setMsg(e?.response?.data?.message || "생성에 실패했습니다.");
    }
  };

  return (
    <div className={styles.page}>
      <Header />

      <main className={styles.main}>
        <div className={styles.toolbar}>
          <h2>분류그룹</h2>
          {admin && (
            <button
              className={styles.primary}
              onClick={() => setShowCreate(true)}
            >
              새 분류그룹 추가 +
            </button>
          )}
        </div>

        {msg && <div className={styles.alert}>{msg}</div>}

        {loading ? (
          <div className={styles.skeleton}>불러오는 중…</div>
        ) : (
          <ul className={styles.list}>
            {items.map((g) => (
              <li key={g.id} className={styles.card}>
                <div className={styles.status}>
                  <span
                    className={g.active ? styles.dotOn : styles.dotOff}
                  ></span>
                  {g.active ? "활성" : "비활성"}
                </div>
                <div className={styles.title}>{g.name}</div>
                <div className={styles.meta}>
                  현재 처리 건수: {g.currentCount ?? 0}
                </div>
                <div className={styles.metaSub}>
                  마지막 업데이트 시각: {g.lastUpdatedAt || "YYYY-MM-DD hh:mm"}
                </div>
                <div className={styles.more}>자세히 &gt;</div>
              </li>
            ))}
          </ul>
        )}
      </main>

      {showCreate && admin && (
        <div
          className={styles.modalBackdrop}
          onClick={() => setShowCreate(false)}
        >
          <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
            <h3>분류그룹 추가</h3>
            <input
              className={styles.input}
              placeholder="이름"
              value={form.name}
              onChange={(e) => setForm((s) => ({ ...s, name: e.target.value }))}
            />
            <input
              className={styles.input}
              placeholder="코드(ex. A-01)"
              value={form.code}
              onChange={(e) => setForm((s) => ({ ...s, code: e.target.value }))}
            />
            <textarea
              className={styles.textarea}
              placeholder="설명(선택)"
              value={form.description}
              onChange={(e) =>
                setForm((s) => ({ ...s, description: e.target.value }))
              }
            />
            <div className={styles.modalActions}>
              <button
                className={styles.secondary}
                onClick={() => setShowCreate(false)}
              >
                취소
              </button>
              <button className={styles.primary} onClick={onCreate}>
                저장
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
