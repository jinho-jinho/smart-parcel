import React, { useEffect, useRef, useState } from "react";
import styles from "./Header.module.css";
import { fetchNotifications, markNotificationRead } from "../api/notifications";

export default function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [total, setTotal] = useState(0);
  const containerRef = useRef(null);

  useEffect(() => {
    const handler = (event) => {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setOpen(false);
      }
    };
    document.addEventListener("click", handler);
    return () => document.removeEventListener("click", handler);
  }, []);

  useEffect(() => {
    if (!open) return;
    loadNotifications();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const loadNotifications = async () => {
    setLoading(true);
    setError("");
    try {
      const { data } = await fetchNotifications({ unreadOnly: true, size: 5 });
      setItems(data?.content ?? []);
      setTotal(data?.totalElements ?? 0);
    } catch (err) {
      setError(err?.response?.data?.message || "알림을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = () => {
    setOpen((prev) => !prev);
  };

  const handleMarkRead = async (notificationId) => {
    try {
      await markNotificationRead(notificationId);
      loadNotifications();
    } catch (err) {
      setError(err?.response?.data?.message || "읽음 처리에 실패했습니다.");
    }
  };

  return (
    <div className={styles.notifyWrap} ref={containerRef}>
      <button type="button" className={styles.notifyButton} onClick={handleToggle}>
        🔔
        {total > 0 && <span className={styles.notifyBadge}>{total}</span>}
      </button>
      {open && (
        <div className={styles.notifyDropdown}>
          <div className={styles.notifyHeader}>
            알림 {total > 0 ? `( ${total} )` : ""}
          </div>
          {loading ? (
            <div className={styles.notifyPlaceholder}>불러오는 중...</div>
          ) : error ? (
            <div className={styles.notifyPlaceholder}>{error}</div>
          ) : items.length === 0 ? (
            <div className={styles.notifyPlaceholder}>새로운 알림이 없습니다.</div>
          ) : (
            <ul className={styles.notifyList}>
              {items.map((item) => (
                <li key={item.id}>
                  <button
                    type="button"
                    onClick={() => handleMarkRead(item.id)}
                    className={styles.notifyItem}
                  >
                    <div className={styles.notifyTitle}>
                      [{item.errorCode ?? "ERROR"}] {item.groupName ?? "미지정"}
                    </div>
                    <div className={styles.notifyMeta}>
                      {item.occurredAt ? formatDate(item.occurredAt) : ""}
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          )}
          <div className={styles.notifyFooter}>알림은 오류 발생 시 자동으로 생성됩니다.</div>
        </div>
      )}
    </div>
  );
}

function formatDate(iso) {
  if (!iso) return "";
  try {
    return new Date(iso).toLocaleString("ko-KR", {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}
