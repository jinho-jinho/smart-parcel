import React, { useEffect, useMemo, useState } from "react";
import Header from "../components/Header";
import styles from "./StaffManagement.module.css";
import { fetchStaff, deleteStaff } from "../api/staff";

const DEFAULT_PAGE = { page: 0, size: 10, totalPages: 0, totalElements: 0 };

const ROLE_LABEL = {
  MANAGER: "관리자",
  STAFF: "직원",
};

export default function StaffManagement() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [keyword, setKeyword] = useState("");
  const [pageState, setPageState] = useState(DEFAULT_PAGE);
  const hasItems = useMemo(() => items.length > 0, [items]);

  const load = async (page = pageState.page) => {
    setLoading(true);
    setError("");
    try {
      const { data } = await fetchStaff({
        page,
        size: pageState.size,
        keyword: keyword.trim() || undefined,
      });
      setItems(data?.content ?? []);
      setPageState((prev) => ({
        ...prev,
        page,
        size: data?.size ?? prev.size,
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
      }));
    } catch (err) {
      setError(err?.response?.data?.message || "직원 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = (event) => {
    event.preventDefault();
    load(0);
  };

  const handleDelete = async (staffId, name) => {
    if (!window.confirm(`${name} 직원을 삭제하시겠습니까?`)) return;
    try {
      await deleteStaff(staffId);
      load(pageState.page);
    } catch (err) {
      setError(err?.response?.data?.message || "삭제에 실패했습니다.");
    }
  };

  const changePage = (diff) => {
    const next = pageState.page + diff;
    if (next < 0 || next >= pageState.totalPages) return;
    load(next);
  };

  return (
    <div className={styles.page}>
      <Header />
      <main className={styles.main}>
        <section className={styles.toolbar}>
          <div>
            <h1 className={styles.heading}>직원 관리</h1>
            <p className={styles.subtitle}>소속된 직원을 확인하고 제거할 수 있습니다.</p>
          </div>
          <form className={styles.searchForm} onSubmit={handleSearch}>
            <input
              className={styles.search}
              placeholder="이름 또는 이메일 검색"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
            <button type="submit" className={styles.primary}>
              검색
            </button>
          </form>
        </section>

        {error && <div className={styles.error}>{error}</div>}

        <section className={styles.card}>
          <header className={styles.cardHeader}>
            <span>총 {pageState.totalElements.toLocaleString("ko-KR")}명</span>
          </header>
          <div className={styles.tableWrapper}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>이름</th>
                  <th>이메일</th>
                  <th>권한/역할</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={4} className={styles.placeholder}>
                      불러오는 중...
                    </td>
                  </tr>
                ) : !hasItems ? (
                  <tr>
                    <td colSpan={4} className={styles.placeholder}>
                      등록된 직원이 없습니다.
                    </td>
                  </tr>
                ) : (
                  items.map((item) => (
                    <tr key={item.id}>
                      <td>{item.name}</td>
                      <td>{item.email}</td>
                      <td>{ROLE_LABEL[item.role] || item.role}</td>
                      <td>
                        <button
                          type="button"
                          className={styles.danger}
                          onClick={() => handleDelete(item.id, item.name)}
                        >
                          삭제
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {pageState.totalPages > 1 && (
            <div className={styles.pagination}>
              <button
                type="button"
                className={styles.secondary}
                onClick={() => changePage(-1)}
                disabled={pageState.page === 0}
              >
                이전
              </button>
              <span>
                {pageState.page + 1} / {pageState.totalPages}
              </span>
              <button
                type="button"
                className={styles.secondary}
                onClick={() => changePage(1)}
                disabled={pageState.page + 1 >= pageState.totalPages}
              >
                다음
              </button>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
