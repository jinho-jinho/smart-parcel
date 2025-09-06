import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import styles from "./Login.module.css";
import { login, fetchMe } from "../api/auth.api.js";
import icon from "../assets/icon.png";

export default function Login() {
  const nav = useNavigate();

  const [email, setEmail] = useState("");
  const [pw, setPw] = useState("");
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState("");

  const emailValid = (v) => /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(v.trim());

  const onSubmit = async (e) => {
    e.preventDefault();
    setMsg("");

    if (!emailValid(email)) return setMsg("올바른 이메일을 입력하세요.");
    if (!pw) return setMsg("비밀번호를 입력해주세요.");

    setLoading(true);
    try {
      await login({ email, password: pw }); // /user/login
      await fetchMe();                      // 선택: /user/me
      // TODO: 로그인 후 이동 경로 지정
      // nav("/dashboard");
    } catch (err) {
      setMsg(err?.response?.data?.message || "로그인에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.viewport}>
      <main className={styles.center}>
        {/* 로고/브랜드 */}
        <header className={styles.header}>
          {/* 실제 로고 이미지 쓰면 아래 div 대신 <img className={styles.logoImg} src="/logo.png" alt="Smart Parcel" /> */}
          <img className={styles.logoImg} src={icon} alt="Smart Parcel" />
          <h1 className={styles.brand}>Smart Parcel</h1>
        </header>

        {/* 카드 */}
        <form className={styles.card} onSubmit={onSubmit} noValidate>
          {msg && <div className={styles.alert}>{msg}</div>}

          {/* 이메일 */}
          <div className={styles.field}>
            <label htmlFor="email" className={styles.label}>이메일</label>
            <input
              id="email"
              type="email"
              placeholder="name@example.com"
              className={styles.input}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="email"
            />
          </div>

          {/* 비밀번호 */}
          <div className={styles.field}>
            <label htmlFor="password" className={styles.label}>비밀번호</label>
            <div className={styles.inputWrap}>
              <input
                id="password"
                type={showPw ? "text" : "password"}
                placeholder="비밀번호"
                className={styles.input}
                value={pw}
                onChange={(e) => setPw(e.target.value)}
                autoComplete="current-password"
              />
              <button
                type="button"
                className={styles.eyeBtn}
                onClick={() => setShowPw((s) => !s)}
                aria-label={showPw ? "비밀번호 숨기기" : "비밀번호 표시"}
              >
                {showPw ? "숨기기" : "표시"}
              </button>
            </div>
          </div>

          {/* 로그인 */}
          <button type="submit" className={styles.primary} disabled={loading}>
            {loading ? "로그인 중…" : "로그인"}
          </button>

          {/* 링크 */}
          <div className={styles.links}>
            <button type="button" className={styles.link} onClick={() => nav("/signup")}>
              회원가입
            </button>
            <button type="button" className={styles.link} onClick={() => nav("/reset-password")}>
              비밀번호 찾기
            </button>
          </div>
        </form>

        <footer className={styles.footer}>© 2025 SmartParcel</footer>
      </main>
    </div>
  );
}
