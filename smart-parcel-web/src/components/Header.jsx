import React from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import styles from "./Header.module.css";
import NotificationBell from "./NotificationBell";
import { authStore } from "../store/auth.store";
import { isManager } from "../utils/permission";
import { logout as apiLogout } from "../api/auth";
import logo from "../assets/icon.png";

export default function Header() {
  const user = authStore((state) => state.user);
  const clear = authStore((state) => state.clear);

  const navigate = useNavigate();
  const location = useLocation();
  const admin = isManager(user);

  const onLogout = async (event) => {
    event.preventDefault();
    try {
      await apiLogout();
    } finally {
      clear();
      navigate("/login", { replace: true });
    }
  };

  const isActive = (path) =>
    location.pathname === path || location.pathname.startsWith(`${path}/`);

  return (
    <header className={styles.wrap}>
      <div className={styles.left}>
        <Link to="/">
          <img src={logo} alt="SmartParcel" className={styles.logo} />
        </Link>
        <span className={styles.brand}>Smart Parcel</span>
        <div className={styles.guideWrap} tabIndex={-1}>
          <button
            type="button"
            className={styles.guideButton}
            aria-label="Smart Parcel 이용 안내"
          >
            <svg
              className={styles.guideIcon}
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              aria-hidden="true"
            >
              <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="2" />
              <path
                d="M12 11v5"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
              />
              <circle cx="12" cy="7.5" r="1.4" fill="currentColor" />
            </svg>
          </button>
          <div className={styles.guidePanel}>
            <div className={styles.guideTitle}>Smart Parcel 이용 안내</div>
            <ol className={styles.guideList}>
              <li>텍스트 표식은 흰색 사각 라벨지를 사용해 주세요.</li>
              <li>텍스트 표식의 글자수는 OCR 성능을 위해 최소 2글자, 최대 4글자로 작성해 주세요.</li>
              <li>
                분류 표식은 물체의 오른쪽 하단부에 깔끔하게 부착해 주세요. (접히거나 주름질 경우 잘 읽지 못할 수 있습니다.)
              </li>
              <li>표식은 매번 비슷한 위치에 붙여주시면 인식 정확도가 더 좋아집니다.</li>
              <li>색상 분류 표식은 물체 색과 겹치지 않는 대비되는 색으로 선택해 주세요.</li>
              <li>카메라는 포토센서와 거의 동일한 위치에 설치해 주세요. (포토센서가 인식된 직후 바로 촬영이 이루어집니다.)</li>
              <li>시작 전, 카메라 높이를 조정해 표식이 화면에 정확히 들어오도록 맞춰 주세요.</li>
              <li>
                조명이 너무 어두워 표식이 잘 보이지 않을 경우, 휴대용 조명 등으로 표식 부분만 가볍게 밝혀 주세요.
              </li>
              <li>정확한 분류를 위해 물체는 컨베이어 중앙에 맞춰 올려 주세요.</li>
            </ol>
          </div>
        </div>
      </div>

      <nav className={styles.nav}>
        <Link
          className={isActive("/history") ? styles.active : ""}
          to="/history"
        >
          분류 이력
        </Link>
        <Link className={isActive("/errors") ? styles.active : ""} to="/errors">
          오류 이력
        </Link>
        <Link className={isActive("/stats") ? styles.active : ""} to="/stats">
          통계 대시보드
        </Link>
        {admin && (
          <>
            <Link
              className={isActive("/admin/staff") ? styles.active : ""}
              to="/admin/staff"
            >
              직원 관리
            </Link>
          </>
        )}
        <Link className={isActive("/me") ? styles.active : ""} to="/me">
          내 정보
        </Link>
      </nav>

      <div className={styles.right}>
        <NotificationBell />
        <a href="/logout" onClick={onLogout} className={styles.btn}>
          로그아웃
        </a>
        <span className={styles.user}>{user?.name || "User"}</span>
      </div>
    </header>
  );
}
