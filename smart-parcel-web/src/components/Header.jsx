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
