import { useEffect, useMemo, useState } from "react";
import Header from "../components/Header";
import styles from "./History.module.css";

const PAGE_SIZE = 12;
const DATE_FORMATTER = new Intl.DateTimeFormat("ko-KR", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  hour12: false,
});

const pad = (value) => String(value).padStart(2, "0");

const toInputValue = (date) => {
  if (!date) return "";
  const d = new Date(date);
  if (Number.isNaN(d.getTime())) return "";
  const year = d.getFullYear();
  const month = pad(d.getMonth() + 1);
  const day = pad(d.getDate());
  const hour = pad(d.getHours());
  const minute = pad(d.getMinutes());
  return `${year}-${month}-${day}T${hour}:${minute}`;
};

const toIsoString = (value) => {
  if (!value) return undefined;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return undefined;
  return date.toISOString();
};

const formatDateTime = (value) => {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return DATE_FORMATTER.format(date);
};

const toInitialRange = () => {
  const end = new Date();
  const start = new Date(end);
  start.setDate(start.getDate() - 7);
  start.setHours(0, 0, 0, 0);
  end.setHours(23, 59, 0, 0);
  return { from: toInputValue(start), to: toInputValue(end) };
};

const buildPages = (current, total, max = 5) => {
  if (total <= 1) return [0];
  const start = Math.max(0, Math.min(current - Math.floor(max / 2), total - max));
  const pages = [];
  for (let i = start; i < Math.min(total, start + max); i += 1) {
    pages.push(i);
  }
  return pages;
};

export default function HistoryPage({
  title,
  searchPlaceholder,
  columns,
  detailFields,
  fetchList,
  fetchDetail,
  defaultSort,
  emptyMessage,
}) {
  const initialRange = useMemo(() => toInitialRange(), []);

  const [searchInput, setSearchInput] = useState("");
  const [filters, setFilters] = useState({
    keyword: "",
    useDateRange: true,
    from: initialRange.from,
    to: initialRange.to,
    sort: defaultSort,
  });

  const [items, setItems] = useState([]);
  const [pageState, setPageState] = useState({
    page: 0,
    size: PAGE_SIZE,
    totalPages: 0,
    totalElements: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [selectedRow, setSelectedRow] = useState(null);
  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState("");

  const pages = useMemo(
    () => buildPages(pageState.page, pageState.totalPages),
    [pageState.page, pageState.totalPages],
  );

  const load = async (page = 0, overrides = {}) => {
    const nextFilters = { ...filters, ...overrides };
    setLoading(true);
    setError("");
    try {
      const response = await fetchList({
        page,
        size: PAGE_SIZE,
        sort: nextFilters.sort,
        keyword: nextFilters.keyword?.trim() || undefined,
        from:
          nextFilters.useDateRange && nextFilters.from
            ? toIsoString(nextFilters.from)
            : undefined,
        to:
          nextFilters.useDateRange && nextFilters.to
            ? toIsoString(nextFilters.to)
            : undefined,
      });
      const data = response?.data;

      setItems(data?.content ?? []);
      setPageState({
        page: data?.page ?? page,
        size: data?.size ?? PAGE_SIZE,
        totalPages: data?.totalPages ?? 0,
        totalElements: data?.totalElements ?? 0,
      });
      setFilters(nextFilters);
    } catch (err) {
      console.error(err);
      setItems([]);
      setError(err?.response?.data?.message || "데이터를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = () => {
    load(0, { keyword: searchInput });
  };

  const handleToggleDate = () => {
    const nextUse = !filters.useDateRange;
    load(0, { useDateRange: nextUse });
  };

  const handleDateChange = (key, value) => {
    load(0, { [key]: value });
  };

  const handlePageChange = (next) => {
    if (next < 0 || next >= pageState.totalPages) return;
    load(next);
  };

  const closeDetail = () => {
    setSelectedRow(null);
    setDetail(null);
    setDetailError("");
    setDetailLoading(false);
  };

  const openDetail = async (row) => {
    setSelectedRow(row);
    setDetail(null);
    setDetailError("");
    setDetailLoading(true);
    try {
      const response = await fetchDetail(row.id);
      setDetail(response?.data ?? null);
    } catch (err) {
      console.error(err);
      setDetailError(err?.response?.data?.message || "상세 정보를 불러오지 못했습니다.");
    } finally {
      setDetailLoading(false);
    }
  };

  const renderCell = (row, column) => {
    const value = row[column.key];
    if (column.type === "datetime") {
      return formatDateTime(value);
    }
    if (column.type === "badge") {
      return (
        <span style={{ color: column.color || "#D0342C", fontWeight: 600 }}>
          {value ?? "-"}
        </span>
      );
    }
    return value ?? "-";
  };

  const activeDetail = detail ?? selectedRow;
  const images = activeDetail?.images;
  const primaryImage =
    images?.original ?? images?.thumbnail ?? images?.snapshot ?? null;
  const availableImages = primaryImage
    ? [{ key: "primary", label: "이미지", url: primaryImage }]
    : [];

  return (
    <div className={styles.wrap}>
      <Header />
      <main className={styles.main}>
        <h1 className={styles.title}>{title}</h1>

        <section className={styles.toolbar}>
          <div className={styles.searchRow}>
            <input
              className={styles.searchInput}
              placeholder={searchPlaceholder}
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") handleSearch();
              }}
            />
            <button
              type="button"
              className={styles.searchButton}
              onClick={handleSearch}
            >
              검색
            </button>
          </div>

          <div className={styles.filtersRow}>
            <label className={styles.switchLabel}>
              <input
                type="checkbox"
                className={styles.switchInput}
                checked={filters.useDateRange}
                onChange={handleToggleDate}
              />
              <span className={styles.switchVisual} />
              기간 설정
            </label>

            <div className={styles.dateInputs}>
              <input
                type="datetime-local"
                className={styles.dateInput}
                value={filters.from}
                onChange={(event) => handleDateChange("from", event.target.value)}
                disabled={!filters.useDateRange}
              />
              <span>~</span>
              <input
                type="datetime-local"
                className={styles.dateInput}
                value={filters.to}
                onChange={(event) => handleDateChange("to", event.target.value)}
                disabled={!filters.useDateRange}
              />
            </div>
          </div>

          {error && <div className={styles.error}>{error}</div>}
        </section>

        <section className={styles.tableCard}>
          {loading ? (
            <div className={styles.loading}>불러오는 중입니다...</div>
          ) : items.length === 0 ? (
            <div className={styles.empty}>{emptyMessage}</div>
          ) : (
            <table className={styles.tableWrap}>
              <thead>
                <tr>
                  {columns.map((column) => (
                    <th key={column.key} style={{ width: column.width }}>
                      {column.label}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {items.map((row) => (
                  <tr key={row.id} onClick={() => openDetail(row)}>
                    {columns.map((column) => (
                      <td key={column.key}>{renderCell(row, column)}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>

        {pageState.totalPages > 1 && (
          <div className={styles.pagination}>
            <button
              type="button"
              className={styles.pageButton}
              onClick={() => handlePageChange(pageState.page - 1)}
              disabled={pageState.page === 0}
            >
              ◀ 이전
            </button>

            {pages.map((page) => (
              <button
                key={page}
                type="button"
                className={`${styles.pageNumber} ${
                  page === pageState.page ? styles.pageNumberActive : ""
                }`}
                onClick={() => handlePageChange(page)}
              >
                {page + 1}
              </button>
            ))}

            <button
              type="button"
              className={styles.pageButton}
              onClick={() => handlePageChange(pageState.page + 1)}
              disabled={pageState.page + 1 >= pageState.totalPages}
            >
              다음 ▶
            </button>
          </div>
        )}
      </main>

      {selectedRow && (
        <div className={styles.modalBackdrop} onClick={closeDetail} role="presentation">
          <div
            className={styles.modal}
            onClick={(event) => event.stopPropagation()}
            role="presentation"
          >
            <div className={styles.modalHeader}>
              <h2 className={styles.modalTitle}>{title} 상세</h2>
              <button type="button" className={styles.closeButton} onClick={closeDetail}>
                ×
              </button>
            </div>

            {detailError && <div className={styles.detailError}>{detailError}</div>}

            <div className={styles.metaGrid}>
              {detailFields.map((field) => {
                const value =
                  field.type === "datetime"
                    ? formatDateTime(activeDetail?.[field.key])
                    : activeDetail?.[field.key] ?? "-";
                return (
                  <div key={field.key} className={styles.metaItem}>
                    <span className={styles.metaLabel}>{field.label}</span>
                    <span className={styles.metaValue}>{value}</span>
                  </div>
                );
              })}
            </div>

            {detailLoading ? (
              <div className={styles.loading} style={{ padding: "24px 0" }}>
                이미지를 불러오는 중입니다...
              </div>
            ) : availableImages.length > 0 ? (
              <div className={styles.imageGrid}>
                {availableImages.map((img) => (
                  <div key={img.key} className={styles.imageCard}>
                    <img src={img.url} alt={img.label} loading="lazy" />
                    <span className={styles.imageLabel}>{img.label}</span>
                  </div>
                ))}
              </div>
            ) : (
              <div className={styles.imageFallback}>표시할 이미지가 없습니다.</div>
            )}

            <div className={styles.modalFooter}>
              <button type="button" className={styles.modalAction} onClick={closeDetail}>
                닫기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
