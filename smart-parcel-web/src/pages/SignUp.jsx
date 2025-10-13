import React, { useEffect, useMemo, useState } from "react";
import styles from "./SignUp.module.css";
import { useNavigate } from "react-router-dom";
import { signup } from "../api/auth";
import { sendCode, verifyCode } from "../api/email-verify";
import icon from "../assets/icon.png";

const ROLES = [
  { key: "MANAGER", label: "관리자" },
  { key: "STAFF", label: "직원" },
];

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
    managerEmail: "",
  });

  const [showPw, setShowPw] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const [codeBoxVisible, setCodeBoxVisible] = useState(false);
  const [emailCode, setEmailCode] = useState("");
  const [emailVerified, setEmailVerified] = useState(false);
  const [remainSec, setRemainSec] = useState(0);
  const [sending, setSending] = useState(false);
  const [verifying, setVerifying] = useState(false);

  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState("");

  const emailValid = useMemo(
    () => /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email.trim()),
    [form.email]
  );
  const managerEmailValid = useMemo(
    () =>
      !form.managerEmail ||
      /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.managerEmail.trim()),
    [form.managerEmail]
  );
  const pwValid = useMemo(
    () => form.password && form.password.length >= 8,
    [form.password]
  );

  useEffect(() => {
    if (remainSec <= 0) return;
    const id = setInterval(
      () => setRemainSec((s) => (s > 0 ? s - 1 : 0)),
      1000
    );
    return () => clearInterval(id);
  }, [remainSec]);

  const onChange = (k) => (e) =>
    setForm((s) => ({ ...s, [k]: e.target.value }));

  const handleSendCode = async () => {
    setMsg("");
    setEmailVerified(false);
    setEmailCode("");
    if (!emailValid) return setMsg("올바른 이메일을 입력하세요.");
    try {
      setSending(true);
      const res = await sendCode({ email: form.email, purpose: "SIGNUP" });
      setCodeBoxVisible(true);
      setRemainSec(300);
      setMsg(res?.message || "인증번호를 전송했습니다.");
    } catch (e) {
      setMsg(e?.response?.data?.message || "인증번호 전송 실패");
    } finally {
      setSending(false);
    }
  };

  const handleVerifyCode = async () => {
    setMsg("");
    if (!emailValid || !emailCode)
      return setMsg("이메일과 인증번호를 확인하세요.");
    if (remainSec === 0) return setMsg("인증 시간이 만료되었습니다.");
    try {
      setVerifying(true);
      const res = await verifyCode({
        email: form.email,
        code: emailCode,
        purpose: "SIGNUP",
      });
      setEmailVerified(true);
      setRemainSec(0);
      setMsg(res?.message || "이메일 인증 완료");
    } catch (e) {
      setMsg(e?.response?.data?.message || "이메일 인증 실패");
    } finally {
      setVerifying(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMsg("");
    if (!form.name.trim()) return setMsg("이름을 입력하세요.");
    if (!emailValid) return setMsg("올바른 이메일을 입력하세요.");
    if (!emailVerified) return setMsg("이메일 인증을 완료하세요.");
    if (!pwValid) return setMsg("비밀번호는 최소 8자입니다.");
    if (form.password !== form.confirm)
      return setMsg("비밀번호가 일치하지 않습니다.");
    if (
      form.role === "STAFF" &&
      (!form.managerEmail.trim() || !managerEmailValid)
    ) {
      return setMsg("직원은 관리자 이메일이 필요합니다.");
    }

    setLoading(true);
    try {
      await signup({
        role: form.role,
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
        bizNumber: form.role === "MANAGER" ? form.bizNumber.trim() : undefined,
        managerEmail:
          form.role === "STAFF" ? form.managerEmail.trim() : undefined,
      });
      setMsg("회원가입 성공");
      setTimeout(() => nav("/login"), 800);
    } catch (e) {
      setMsg(e?.response?.data?.message || "회원가입 실패");
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

        <form className={styles.card} onSubmit={handleSubmit} noValidate>
          {msg && <div className={styles.alert}>{msg}</div>}

          {/* 이름 */}
          <div className={styles.field}>
            <label className={styles.label} htmlFor="name">
              이름
            </label>
            <input
              id="name"
              className={styles.input}
              value={form.name}
              onChange={onChange("name")}
            />
          </div>

          {/* 이메일 + 인증 */}
          <div className={styles.field}>
            <label className={styles.label} htmlFor="email">
              이메일
            </label>
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
              >
                {sending ? "전송중" : "인증"}
              </button>
            </div>
            {codeBoxVisible && (
              <>
                <div className={styles.row}>
                  <input
                    className={styles.input}
                    placeholder="인증번호"
                    value={emailCode}
                    onChange={(e) => setEmailCode(e.target.value)}
                    inputMode="numeric"
                    disabled={emailVerified}
                  />
                  <button
                    type="button"
                    className={styles.secondary}
                    onClick={handleVerifyCode}
                    disabled={
                      !emailValid || !emailCode || remainSec === 0 || verifying
                    }
                  >
                    {verifying ? "확인중" : "확인"}
                  </button>
                </div>
                <div className={styles.timer}>
                  {remainSec > 0
                    ? `남은 시간 ${fmt(remainSec)}`
                    : "인증 시간이 만료되었습니다."}
                </div>
                {emailVerified && (
                  <div className={styles.badge}>이메일 인증 완료</div>
                )}
              </>
            )}
          </div>

          {/* 비밀번호/확인 */}
          <div className={styles.field}>
            <label className={styles.label} htmlFor="password">
              비밀번호
            </label>
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
            <label className={styles.label} htmlFor="confirm">
              비밀번호 확인
            </label>
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

          {/* 역할 선택 */}
          <div className={styles.field}>
            <div className={styles.roleGrid}>
              {ROLES.map((r) => (
                <button
                  key={r.key}
                  type="button"
                  className={`${styles.roleBtn} ${
                    form.role === r.key ? styles.roleBtnActive : ""
                  }`}
                  onClick={() => setForm((s) => ({ ...s, role: r.key }))}
                >
                  {r.label}
                </button>
              ))}
            </div>
          </div>

          {/* 직원 전용: 관리자 이메일 */}
          {form.role === "STAFF" && (
            <div className={styles.field}>
              <label className={styles.label} htmlFor="managerEmail">
                관리자 이메일 <span className={styles.required}>*</span>
              </label>
              <input
                id="managerEmail"
                type="email"
                className={styles.input}
                placeholder="관리자 이메일을 입력하세요"
                value={form.managerEmail}
                onChange={onChange("managerEmail")}
                autoComplete="email"
              />
              {!managerEmailValid && form.managerEmail && (
                <div className={styles.helperError}>
                  이메일 형식을 확인하세요
                </div>
              )}
            </div>
          )}

          {/* 관리자 전용: 사업자번호 */}
          {form.role === "MANAGER" && (
            <div className={styles.field}>
              <label className={styles.label} htmlFor="biz">
                사업자번호(선택)
              </label>
              <input
                id="biz"
                className={styles.input}
                placeholder="숫자만 입력 (예: 1234567890)"
                value={form.bizNumber}
                onChange={onChange("bizNumber")}
                inputMode="numeric"
                autoComplete="off"
              />
            </div>
          )}

          <button
            type="submit"
            className={styles.primary}
            disabled={
              loading ||
              !emailVerified ||
              !pwValid ||
              (form.role === "STAFF" &&
                (!form.managerEmail.trim() || !managerEmailValid))
            }
          >
            {loading ? "처리 중" : "회원가입"}
          </button>

          <div className={styles.links}>
            <span className={styles.muted}>이미 계정이 있나요?</span>
            <button
              type="button"
              className={styles.link}
              onClick={() => nav("/login")}
            >
              로그인 화면으로 이동하기
            </button>
          </div>
        </form>
      </main>
    </div>
  );
}
