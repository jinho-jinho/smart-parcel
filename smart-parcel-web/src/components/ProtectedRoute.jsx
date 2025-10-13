// src/components/ProtectedRoute.jsx
import { useEffect, useRef, useState } from "react";
import { Navigate } from "react-router-dom";
import { authStore } from "../store/auth.store";
import { hasAnyRole } from "../utils/permission";
import { fetchMe } from "../api/user";

export default function ProtectedRoute({ children, roles }) {
  const accessToken = authStore((s) => s.accessToken);
  const user = authStore((s) => s.user);

  const [probing, setProbing] = useState(false);
  const tried = useRef(false);

  const authed = !!accessToken || !!user;

  useEffect(() => {
    if (authed || tried.current) return;
    tried.current = true;
    setProbing(true);
    fetchMe().catch(() => undefined).finally(() => setProbing(false));
  }, [authed]);

  // ?좏겙 ?덉쑝硫?利됱떆 ?듦낵
  if (accessToken) {
    if (roles && !hasAnyRole(authStore.getState().user, roles)) {
      return <Navigate to="/403" replace />;
    }
    return children;
  }

  // ?좏겙? ?놁?留?user媛 ?덉쑝硫??듦낵
  if (user) {
    if (roles && !hasAnyRole(user, roles)) {
      return <Navigate to="/403" replace />;
    }
    return children;
  }

  // ?꾨줈鍮?以묒뿏 ?뉗? 濡쒕뜑 (null 湲덉?)
  if (probing) {
    return (
      <div style={{
        minHeight: "100vh",
        background: "#F5F5F5",
        display: "grid",
        placeItems: "center",
        color: "#444"
      }}>
        濡쒓렇???곹깭 ?뺤씤 以묅?
      </div>
    );
  }

  // ?몄쬆 ?덈맖
  return <Navigate to="/login" replace />;
}
