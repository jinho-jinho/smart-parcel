import React, { useEffect, useState } from "react";
import Header from "../components/Header";
import styles from "./Me.module.css";
import { authStore } from "../store/auth.store";
import { fetchMe } from "../api/user";
import { useNavigate } from "react-router-dom";

const ROLE_LABEL = {
  MANAGER: "관리자",
  STAFF: "직원",
};

export default function Me() {
  const [profile, setProfile] = useState(authStore.getState().user);
  const [loading, setLoading] = useState(!profile);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    if (profile) return;
    const load = async () => {
      setLoading(true);
      setError("");
      try {
        const { data } = await fetchMe();
        setProfile(data ?? null);
      } catch (err) {
        setError(err?.response?.data?.message || "내 정보를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [profile]);

  const handleResetPassword = () => {
    navigate("/reset-password");
  };

  return (
    <div className={styles.page}>
      <Header />
      <main className={styles.main}>
        <section className={styles.card}>
          <h1 className={styles.heading}>내 정보</h1>
          {loading ? (
            <div className={styles.placeholder}>불러오는 중...</div>
          ) : error ? (
            <div className={styles.error}>{error}</div>
          ) : (
            <>
              <dl className={styles.list}>
                <div>
                  <dt>이름</dt>
                  <dd>{profile?.name ?? "-"}</dd>
                </div>
                <div>
                  <dt>이메일</dt>
                  <dd>{profile?.email ?? "-"}</dd>
                </div>
                <div>
                  <dt>역할</dt>
                  <dd>{ROLE_LABEL[profile?.role] || profile?.role || "-"}</dd>
                </div>
                <div>
                  <dt>가입일</dt>
                  <dd>{formatDate(profile?.createdAt)}</dd>
                </div>
              </dl>
              <button type="button" className={styles.primary} onClick={handleResetPassword}>
                비밀번호 재설정
              </button>
            </>
          )}
        </section>
      </main>
    </div>
  );
}

function formatDate(iso) {
  if (!iso) return "-";
  try {
    return new Date(iso).toLocaleString("ko-KR", { dateStyle: "medium", timeStyle: "short" });
  } catch {
    return iso;
  }
}
