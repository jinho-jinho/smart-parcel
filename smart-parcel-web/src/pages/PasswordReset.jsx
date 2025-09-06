import React, { useEffect, useState } from "react";
import styles from "./PasswordReset.module.css";
import { useNavigate } from "react-router-dom";
import { sendCode, verifyCode } from "../api/email.api";
import { resetPassword } from "../api/password.api";
import icon from "../assets/icon.png";

function fmt(sec) {
  const m = String(Math.floor(sec / 60)).padStart(2, "0");
  const s = String(sec % 60).padStart(2, "0");
  return `${m}:${s}`;
}

export default function ResetPassword() {
  const nav = useNavigate();

  const [step, setStep] = useState(1);

  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [pw, setPw] = useState("");
  const [pw2, setPw2] = useState("");

  const [showPw, setShowPw] = useState(false);
  const [showPw2, setShowPw2] = useState(false);

  const [remainSec, setRemainSec] = useState(0);
  const [msg, setMsg] = useState("");

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (remainSec <= 0) return;
    const id = setInterval(() => {
      setRemainSec((s) => (s > 0 ? s - 1 : 0));
    }, 1000);
    return () => clearInterval(id);
  }, [remainSec]);

  const handleSend = async () => {
    setMsg("");
    try {
      setLoading(true);
      const res = await sendCode({ email, purpose: "RESET_PASSWORD" });
      setStep(2);
      setRemainSec(300);
      setMsg(res?.message || "코드가 전송되었습니다.");
    } catch (e) {
      setMsg(e?.response?.data?.message || "코드 전송 실패");
    } finally {
      setLoading(false);
    }
  };

  const handleVerify = async () => {
    setMsg("");
    if (!code) return setMsg("코드를 입력해주세요.");
    try {
      const res = await verifyCode({ email, code, purpose: "RESET_PASSWORD" });
      setStep(3);
      setMsg(res?.message || "인증 성공");
    } catch (e) {
      setMsg(e?.response?.data?.message || "코드 확인 실패");
    }
  };

  const handleReset = async (e) => {
    e.preventDefault();
    setMsg("");
    if (pw.length < 8) return setMsg("비밀번호는 8자 이상이어야 합니다.");
    if (pw !== pw2) return setMsg("비밀번호가 일치하지 않습니다.");

    try {
      setLoading(true);
      const res = await resetPassword({ email, code, newPassword: pw });
      setMsg(res?.message || "비밀번호가 변경되었습니다.");
      setTimeout(() => nav("/login"), 1000);
    } catch (e) {
      setMsg(e?.response?.data?.message || "비밀번호 변경 실패");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.viewport}>
      <main className={styles.center}>
        <header className={styles.header}>
          <img className={styles.logo} src={icon} alt="Smart Parcel" />
          <h1 className={styles.brand}>Smart Parcel</h1>
        </header>

        <section className={styles.card}>
          {msg && <div className={styles.alert}>{msg}</div>}

          {/* STEP 1: 이메일 입력 */}
          <div className={styles.field}>
            <label>이메일</label>
            <input
              type="email"
              className={styles.input}
              placeholder="you@example.com"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                setStep(1);
              }}
            />
          </div>
          <button
            type="button"
            onClick={handleSend}
            disabled={!email || loading}
            className={styles.primary}
          >
            코드 전송
          </button>

          {/* STEP 2: 코드 입력 */}
          {step >= 2 && (
            <>
              <div className={styles.field}>
                <label>인증번호</label>
                <input
                  className={styles.input}
                  placeholder="이메일로 받은 6자리 코드"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                />
              </div>
              <button
                type="button"
                onClick={handleVerify}
                disabled={!code}
                className={styles.secondary}
              >
                코드 확인
              </button>
              <div className={styles.timer}>
                {remainSec > 0
                  ? `남은 시간 ${fmt(remainSec)}`
                  : "만료되었습니다. 다시 전송하세요."}
              </div>
            </>
          )}

          {/* STEP 3: 새 비밀번호 입력 */}
          {step >= 3 && (
            <form onSubmit={handleReset} className={styles.form}>
              <div className={styles.field}>
                <label>새 비밀번호 (8자 이상)</label>
                <div className={styles.inputWrap}>
                  <input
                    type={showPw ? "text" : "password"}
                    className={styles.input}
                    placeholder="새 비밀번호"
                    value={pw}
                    onChange={(e) => setPw(e.target.value)}
                  />
                  <button
                    type="button"
                    className={styles.eyeBtn}
                    onClick={() => setShowPw((v) => !v)}
                  >
                    {showPw ? "숨기기" : "표시"}
                  </button>
                </div>
              </div>
              <div className={styles.field}>
                <label>새 비밀번호 확인</label>
                <div className={styles.inputWrap}>
                  <input
                    type={showPw2 ? "text" : "password"}
                    className={styles.input}
                    placeholder="다시 입력"
                    value={pw2}
                    onChange={(e) => setPw2(e.target.value)}
                  />
                  <button
                    type="button"
                    className={styles.eyeBtn}
                    onClick={() => setShowPw2((v) => !v)}
                  >
                    {showPw2 ? "숨기기" : "표시"}
                  </button>
                </div>
              </div>
              <button
                type="submit"
                className={styles.primary}
                disabled={loading}
              >
                비밀번호 변경
              </button>
            </form>
          )}
        </section>
      </main>
    </div>
  );
}
