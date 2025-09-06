import React, { useEffect, useMemo, useState } from "react";
import styles from "./SignUp.module.css";
import { useNavigate } from "react-router-dom";
import { signup } from "../api/auth.api.js";
import { sendCode, verifyCode } from "../api/email.api.js";
import icon from "../assets/icon.png";

const ROLES = [
  { key: "ADMIN", label: "관리자" },
  { key: "STAFF", label: "직원" },
];

// mm:ss 포맷
function fmt(sec) {
  const m = String(Math.floor(sec / 60)).padStart(2, "0");
  const s = String(sec % 60).padStart(2, "0");
  return `${m}:${s}`;
}

export default function SignUp() {
  const nav = useNavigate();

  const [form, setForm] = useState({
    role: "STAFF",
    name: "",
    email: "",
    password: "",
    confirm: "",
    bizNumber: "",
  });

  const [showPw, setShowPw] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  // 이메일 인증 관련
  const [codeBoxVisible, setCodeBoxVisible] = useState(false);
  const [emailCode, setEmailCode] = useState("");
  const [emailVerified, setEmailVerified] = useState(false);
  const [remainSec, setRemainSec] = useState(0); // 5분 카운트다운
  const [sending, setSending] = useState(false);

  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState("");
    // 인증번호 확인
  const [verifying, setVerifying] = useState(false);

  const emailValid = useMemo(
    () => /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email.trim()),
    [form.email]
  );
  const pwValid = useMemo(
    () => form.password && form.password.length >= 8,
    [form.password]
  );

  // 카운트다운 타이머
  useEffect(() => {
    if (remainSec <= 0) return;
    const id = setInterval(() => {
      setRemainSec((s) => (s > 0 ? s - 1 : 0));
    }, 1000);
    return () => clearInterval(id);
  }, [remainSec]);

  const onChange = (k) => (e) => {
    const v = e.target.value;
    setForm((s) => ({ ...s, [k]: v }));
  };

  const handleSendCode = async () => {
    setMsg("");
    setEmailVerified(false);
    setEmailCode("");
  
    if (!emailValid) return setMsg("올바른 이메일을 입력하세요.");
  
    try {
      setSending(true);
      const res = await sendCode({ email: form.email, purpose: "SIGNUP" }); // { success, message }
      // ✅ 성공 응답에 맞춰 처리
      setCodeBoxVisible(true);
      setRemainSec(300);
      setMsg(res?.message || "인증번호가 전송되었습니다.");
    } catch (e) {
      // 실패 시에도 이유 보여주기
      setMsg(e?.response?.data?.message || "인증번호 전송 실패");
    } finally {
      setSending(false);
    }
  };
  



  const handleVerifyCode = async () => {
    setMsg("");
    if (!emailValid || !emailCode) return setMsg("이메일과 인증번호를 확인하세요.");
    if (remainSec === 0) return setMsg("인증 시간이 만료되었습니다. 다시 전송해주세요.");

    try {
      setVerifying(true);
      const res = await verifyCode({ email: form.email, code: emailCode, purpose: "SIGNUP" });
      setEmailVerified(true);
      setRemainSec(0);               // ✅ 타이머 정지
      setMsg(res?.message || "이메일 인증 성공");
    } catch (e) {
      setMsg(e?.response?.data?.message || "이메일 인증 실패");
    } finally {
      setVerifying(false);
    }
  };

  

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMsg("");

    if (!form.name.trim()) return setMsg("이름을 입력해주세요.");
    if (!emailValid) return setMsg("올바른 이메일을 입력하세요.");
    if (!emailVerified) return setMsg("이메일 인증을 완료해주세요.");
    if (!pwValid) return setMsg("비밀번호는 최소 8자 이상이어야 합니다.");
    if (form.password !== form.confirm) return setMsg("비밀번호가 일치하지 않습니다.");

    setLoading(true);
    try {
      await signup({
        role: form.role,
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
        bizNumber: form.bizNumber || null,
      });
      setMsg("회원가입 성공! 로그인 화면으로 이동합니다.");
      setTimeout(() => nav("/login"), 700);
    } catch (e2) {
      setMsg(e2?.response?.data?.message || "회원가입에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.viewport}>
      <main className={styles.center}>
        <header className={styles.header}>
          <img className={styles.logoImg} src={icon} alt="Smart Parcel" />
          <h1 className={styles.brand}>Smart Parcel</h1>
        </header>

        <form className={styles.card} onSubmit={handleSubmit} noValidate>
          {msg && <div className={styles.alert}>{msg}</div>}

          {/* 이름 */}
          <div className={styles.field}>
            <label className={styles.label} htmlFor="name">이름</label>
            <input
              id="name"
              className={styles.input}
              placeholder="홍길동"
              value={form.name}
              onChange={onChange("name")}
            />
          </div>

          {/* 이메일 + 인증 전송 */}
          <div className={styles.field}>
            <label className={styles.label} htmlFor="email">이메일</label>
            <div className={styles.row}>
              <input
                id="email"
                type="email"
                className={styles.input}
                placeholder="name@example.com"
                value={form.email}
                onChange={(e) => {
                  setEmailVerified(false);
                  setCodeBoxVisible(false);
                  setRemainSec(0);
                  setEmailCode("");
                  setForm((s) => ({ ...s, email: e.target.value }));
                }}
                autoComplete="email"
              />
              <button
                type="button"
                className={styles.secondary}
                onClick={handleSendCode}
                disabled={!emailValid || sending}
                title="인증번호 전송"
              >
                {sending ? "전송중" : "인증"}
              </button>
            </div>

            {/* 인증 코드 입력 (전송 후 표시) */}
            {codeBoxVisible && (
              <>
                <div className={styles.row}>
                  <input
                    className={styles.input}
                    placeholder="인증번호 입력"
                    value={emailCode}
                    onChange={(e) => setEmailCode(e.target.value)}
                    inputMode="numeric"
                    disabled={emailVerified} 
                  />
                  <button
                    type="button"
                    className={styles.secondary}
                    onClick={handleVerifyCode}
                    disabled={!emailValid || !emailCode || remainSec === 0 || verifying}
                    title="코드 확인"
                  >
                    {verifying ? "확인중" : "확인"}
                  </button>

                </div>
                <div className={styles.timer}>
                  {remainSec > 0
                    ? `남은 시간 ${fmt(remainSec)}`
                    : "인증 시간이 만료되었습니다."}
                </div>
                {emailVerified && <div className={styles.badge}>이메일 인증 완료</div>}
              </>
            )}
          </div>

          {/* 비밀번호 / 확인 */}
          <div className={styles.field}>
            <label className={styles.label} htmlFor="password">비밀번호</label>
            <div className={styles.inputWrap}>
              <input
                id="password"
                type={showPw ? "text" : "password"}
                className={styles.input}
                placeholder="최소 8자"
                value={form.password}
                onChange={onChange("password")}
                autoComplete="new-password"
              />
              <button
                type="button"
                className={styles.eyeBtn}
                onClick={() => setShowPw((s) => !s)}
              >
                {showPw ? "숨기기" : "표시"}
              </button>
            </div>
          </div>

          <div className={styles.field}>
            <label className={styles.label} htmlFor="confirm">비밀번호 확인</label>
            <div className={styles.inputWrap}>
              <input
                id="confirm"
                type={showConfirm ? "text" : "password"}
                className={styles.input}
                placeholder="비밀번호 확인"
                value={form.confirm}
                onChange={onChange("confirm")}
                autoComplete="new-password"
              />
              <button
                type="button"
                className={styles.eyeBtn}
                onClick={() => setShowConfirm((s) => !s)}
              >
                {showConfirm ? "숨기기" : "표시"}
              </button>
            </div>
          </div>

          {/* ✅ 역할 선택(비밀번호 확인 아래, 카드 반반) */}
          <div className={styles.field}>
            <div className={styles.roleGrid}>
              {ROLES.map((r) => (
                <button
                  key={r.key}
                  type="button"
                  className={`${styles.roleBtn} ${form.role === r.key ? styles.roleBtnActive : ""}`}
                  onClick={() => setForm((s) => ({ ...s, role: r.key }))}
                >
                  {r.label}
                </button>
              ))}
            </div>
          </div>

          {/* (선택) 사업자번호 필요 시 유지 */}
          {/* <div className={styles.field}>
            <label className={styles.label} htmlFor="biz">사업자번호(선택)</label>
            <input
              id="biz"
              className={styles.input}
              placeholder="하이픈 없이 (예: 1234567890)"
              value={form.bizNumber}
              onChange={onChange("bizNumber")}
              inputMode="numeric"
            />
          </div> */}

          <button
            type="submit"
            className={styles.primary}
            disabled={loading || !emailVerified || !pwValid}
          >
            {loading ? "처리 중…" : "회원가입"}
          </button>

          <div className={styles.links}>
            <span className={styles.muted}>이미 계정이 있으신가요?</span>
            <button type="button" className={styles.link} onClick={() => nav("/login")}>
              로그인 화면으로 이동하기
            </button>
          </div>
        </form>
      </main>
    </div>
  );
}
