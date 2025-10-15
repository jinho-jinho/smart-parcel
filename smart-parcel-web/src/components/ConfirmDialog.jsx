import React from "react";
import styles from "./ConfirmDialog.module.css";

export default function ConfirmDialog({
  title,
  description,
  confirmLabel = "확인",
  cancelLabel = "취소",
  onConfirm,
  onCancel,
  confirming = false,
}) {
  return (
    <div className={styles.backdrop} onClick={onCancel}>
      <div className={styles.dialog} onClick={(event) => event.stopPropagation()}>
        <h3 className={styles.title}>{title}</h3>
        {description && <p className={styles.description}>{description}</p>}
        <div className={styles.actions}>
          <button type="button" className={styles.cancel} onClick={onCancel} disabled={confirming}>
            {cancelLabel}
          </button>
          <button
            type="button"
            className={styles.confirm}
            onClick={onConfirm}
            disabled={confirming}
          >
            {confirming ? "진행 중…" : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
