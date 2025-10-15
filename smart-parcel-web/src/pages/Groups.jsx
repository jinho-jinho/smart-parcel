import React, { useEffect, useMemo, useState } from "react";
import styles from "./Groups.module.css";
import Header from "../components/Header";
import { authStore } from "../store/auth.store";
import { isManager } from "../utils/permission";
import { useNavigate } from "react-router-dom";
import {
  fetchGroups,
  createGroup,
  updateGroup,
  toggleActive,
  deleteGroup,
} from "../api/groups";

const INITIAL_FORM = { groupName: "" };
const DEFAULT_PAGE_STATE = {
  page: 0,
  size: 12,
  totalPages: 0,
  totalElements: 0,
};

const formatNumber = (value) => {
  const num = Number(value);
  if (!Number.isFinite(num)) return "0";
  return num.toLocaleString("ko-KR");
};

const formatDate = (iso) => {
  if (!iso) return "-";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "-";
  return date.toLocaleString("ko-KR", { hour12: false });
};

export default function Groups() {
  const user = authStore((state) => state.user);
  const admin = isManager(user);
  const navigate = useNavigate();

  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState(INITIAL_FORM);
  const [keyword, setKeyword] = useState("");
  const [pageState, setPageState] = useState(DEFAULT_PAGE_STATE);

  const hasItems = useMemo(() => items.length > 0, [items]);

  const load = async (page = pageState.page) => {
    setLoading(true);
    setError("");
    try {
      const response = await fetchGroups({
        page,
        size: pageState.size,
        keyword: keyword.trim() || undefined,
      });
      const data = response?.data;

      setItems(data?.content ?? []);
      setPageState((prev) => ({
        page: data?.page ?? page,
        size: data?.size ?? prev.size,
        totalPages: data?.totalPages ?? prev.totalPages,
        totalElements: data?.totalElements ?? prev.totalElements,
      }));
    } catch (err) {
      console.error(err);
      setItems([]);
      setError(err?.response?.data?.message || "분류 그룹을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [keyword]);

  useEffect(() => {
    if (!showCreate) return undefined;
    const handleKeyDown = (event) => {
      if (event.key === "Escape") {
        setShowCreate(false);
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [showCreate]);

  const handleCreate = async () => {
    const trimmed = form.groupName.trim();
    if (!trimmed) {
      setError("분류 그룹 이름을 입력해주세요.");
      return;
    }

    try {
      await createGroup({ groupName: trimmed });
      setShowCreate(false);
      setForm(INITIAL_FORM);
      await load(0);
    } catch (err) {
      console.error(err);
      setError(err?.response?.data?.message || "분류 그룹 생성에 실패했습니다.");
    }
  };

  const handleToggle = async (group) => {
    try {
      await toggleActive(group.id, !group.active);
      await load(pageState.page);
    } catch (err) {
      console.error(err);
      setError(err?.response?.data?.message || "상태 변경에 실패했습니다.");
    }
  };

  const handleRename = async (group) => {
    const next = window.prompt("새 분류 그룹 이름을 입력하세요.", group.name ?? "");
    if (next === null) return;
    const trimmed = next.trim();
    if (!trimmed || trimmed === group.name) return;

    try {
      await updateGroup(group.id, { groupName: trimmed });
      await load(pageState.page);
    } catch (err) {
      console.error(err);
      setError(err?.response?.data?.message || "이름 변경에 실패했습니다.");
    }
  };

  const handleDelete = async (group) => {
    const confirmed = window.confirm(
      `"${group.name}" 그룹을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.`
    );
    if (!confirmed) return;

    try {
      await deleteGroup(group.id);
      const isLastItemOnPage = items.length === 1 && pageState.page > 0;
      await load(isLastItemOnPage ? pageState.page - 1 : pageState.page);
    } catch (err) {
      console.error(err);
      setError(err?.response?.data?.message || "분류 그룹 삭제에 실패했습니다.");
    }
  };

  const handlePageChange = (offset) => {
    const nextPage = pageState.page + offset;
    if (nextPage < 0 || nextPage >= pageState.totalPages) return;
    load(nextPage);
  };

  const handleOpenGroup = (group) => {
    navigate(`/groups/${group.id}/rules`, {
      state: { group },
    });
  };

  const handleCardKeyDown = (event, group) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      handleOpenGroup(group);
    }
  };

  return (
    <div className={styles.page}>
      <Header />
      <main className={styles.main}>
        <div className={styles.toolbar}>
          <div>
            <h1 className={styles.heading}>분류 그룹</h1>
            <p className={styles.subtitle}>
              총 {formatNumber(pageState.totalElements)}개의 그룹
            </p>
          </div>
          <div className={styles.actions}>
            <input
              className={styles.search}
              placeholder="그룹 이름 검색"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
            {admin && (
              <button
                type="button"
                className={styles.primary}
                onClick={() => {
                  setForm(INITIAL_FORM);
                  setShowCreate(true);
                  setError("");
                }}
              >
                새 분류그룹 추가
              </button>
            )}
          </div>
        </div>

        {error && <div className={styles.alert}>{error}</div>}

        {loading ? (
          <div className={styles.skeleton}>분류 그룹을 불러오는 중입니다…</div>
        ) : !hasItems ? (
          <div className={styles.empty}>등록된 분류 그룹이 없습니다.</div>
        ) : (
          <>
            <ul className={styles.list}>
              {items.map((group) => (
                <li
                  key={group.id}
                  className={`${styles.card} ${styles.cardInteractive}`}
                  role="button"
                  tabIndex={0}
                  onClick={() => handleOpenGroup(group)}
                  onKeyDown={(event) => handleCardKeyDown(event, group)}
                >
                  <div className={styles.cardTop}>
                    <div className={styles.status}>
                      <span
                        className={group.active ? styles.dotOn : styles.dotOff}
                        aria-hidden="true"
                      />
                      <span className={styles.statusLabel}>
                        {group.active ? "활성" : "비활성"}
                      </span>
                    </div>

                    <div className={styles.cardHeaderRight}>
                      <button
                        type="button"
                        className={styles.linkButton}
                        onClick={(event) => {
                          event.stopPropagation();
                          handleOpenGroup(group);
                        }}
                      >
                        분류 기준 보기
                      </button>
                      {admin && (
                        <div
                          className={styles.cardActions}
                          onClick={(event) => event.stopPropagation()}
                        >
                          <button
                            type="button"
                            className={styles.linkButton}
                            onClick={() => handleToggle(group)}
                          >
                            {group.active ? "비활성화" : "활성화"}
                          </button>
                          <button
                            type="button"
                            className={styles.linkButton}
                            onClick={() => handleRename(group)}
                          >
                            이름 변경
                          </button>
                          <button
                            type="button"
                            className={`${styles.linkButton} ${styles.linkButtonDanger}`}
                            onClick={() => handleDelete(group)}
                          >
                            삭제
                          </button>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className={styles.title}>{group.name}</div>

                  <div className={styles.metaRow}>
                    <span className={styles.metaItem}>
                      현재 처리 건수 (건) :
                      <strong>{formatNumber(group.processingCount)}</strong>
                    </span>
                    <span className={styles.metaItem}>
                      마지막 업데이트 시간:
                      <time dateTime={group.lastUpdatedAt || undefined}>
                        {formatDate(group.lastUpdatedAt)}
                      </time>
                    </span>
                  </div>

                  <div className={styles.metaSub}>
                    담당자: {group.managerName || "-"}
                  </div>
                </li>
              ))}
            </ul>

            {pageState.totalPages > 1 && (
              <div className={styles.pagination}>
                <button
                  type="button"
                  className={styles.secondary}
                  onClick={() => handlePageChange(-1)}
                  disabled={pageState.page === 0}
                >
                  이전
                </button>
                <span className={styles.pageIndicator}>
                  {pageState.page + 1} / {pageState.totalPages}
                </span>
                <button
                  type="button"
                  className={styles.secondary}
                  onClick={() => handlePageChange(1)}
                  disabled={pageState.page + 1 >= pageState.totalPages}
                >
                  다음
                </button>
              </div>
            )}
          </>
        )}
      </main>

      {showCreate && admin && (
        <div
          className={styles.modalBackdrop}
          onClick={() => setShowCreate(false)}
        >
          <div
            className={styles.modal}
            onClick={(event) => event.stopPropagation()}
          >
            <h3 className={styles.modalTitle}>새 분류 그룹 만들기</h3>
            <input
              className={styles.input}
              placeholder="분류 그룹 이름"
              value={form.groupName}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, groupName: event.target.value }))
              }
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  handleCreate();
                }
              }}
              autoFocus
            />
            <div className={styles.modalActions}>
              <button
                type="button"
                className={styles.secondary}
                onClick={() => setShowCreate(false)}
              >
                취소
              </button>
              <button type="button" className={styles.primary} onClick={handleCreate}>
                저장
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
