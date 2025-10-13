import React from "react";
import { Link, useLocation } from "react-router-dom";
import styles from "./Header.module.css";
import { authStore } from "../store/auth.store";
import { isManager } from "../utils/permission";
import { logout as apiLogout } from "../api/auth";
import { useNavigate } from "react-router-dom";
import logo from "../assets/icon.png";

export default function Header() {
  // Header.jsx
  const user = authStore((s) => s.user);
  const clear = authStore((s) => s.clear);

  const nav = useNavigate();
  const loc = useLocation();
  const admin = isManager(user);

  const onLogout = async (e) => {
    e.preventDefault();
    try {
      await apiLogout();
    } finally {
      clear();
      nav("/login", { replace: true });
    }
  };

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
          className={loc.pathname === "/history" ? styles.active : ""}
          to="/history"
        >
          History
        </Link>
        <Link
          className={loc.pathname === "/errors" ? styles.active : ""}
          to="/errors"
        >
          Errors
        </Link>
        <Link
          className={loc.pathname === "/stats" ? styles.active : ""}
          to="/stats"
        >
          Stats
        </Link>

        {admin && (
          <>
            <Link
              className={loc.pathname === "/admin/groups" ? styles.active : ""}
              to="/admin/groups"
            >
              Admin
            </Link>
          </>
        )}

        <Link className={loc.pathname === "/me" ? styles.active : ""} to="/me">
          Profile
        </Link>
      </nav>

      <div className={styles.right}>
        <Link to="/notifications" aria-label="Notifications">
          *
        </Link>
        <a href="/logout" onClick={onLogout} className={styles.btn}>
          Logout
        </a>
        <span className={styles.user}>{user?.name || "User"}</span>
      </div>
    </header>
  );
}

