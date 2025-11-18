import React, { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import styles from "./Rules.module.css";
import Header from "../components/Header";
import { authStore } from "../store/auth.store";
import { isManager } from "../utils/permission";
import {
  fetchRules,
  createRule,
  updateRule,
  deleteRule,
} from "../api/rules";
import { fetchChutes, createChute, deleteChute } from "../api/chutes";
import { fetchGroups } from "../api/groups";
import ConfirmDialog from "../components/ConfirmDialog";

const DEFAULT_PAGE_STATE = {
  page: 0,
  size: 10,
  totalPages: 0,
  totalElements: 0,
};

const DEFAULT_RULE_FORM = {
  ruleName: "",
  itemName: "",
  inputType: "TEXT",
  inputValue: "",
  chuteIds: [],
  selectedChutes: [],
};

const DEFAULT_CHUTE_FORM = {
  chuteName: "",
  servoDeg: "",
};

const INPUT_TYPES = [
  { value: "TEXT", label: "텍스트" },
  { value: "COLOR", label: "색상" },
];

const COLOR_OPTIONS = [
  { value: "RED", label: "빨강", swatch: "#ef4444" },
  { value: "YELLOW", label: "노랑", swatch: "#facc15" },
  { value: "GREEN", label: "초록", swatch: "#22c55e" },
  { value: "BLUE", label: "파랑", swatch: "#3b82f6" },
];

const CHUTE_PAGE_SIZE = 100;

const formatDateTime = (iso) => {
  if (!iso) return "-";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "-";
  return date.toLocaleString("ko-KR", { hour12: false });
};

export default function Rules() {
  const { groupId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();

  const user = authStore((state) => state.user);
  const admin = isManager(user);

  const [group, setGroup] = useState(() => location.state?.group ?? null);
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [pageState, setPageState] = useState(DEFAULT_PAGE_STATE);

  const [showRuleModal, setShowRuleModal] = useState(false);
  const [ruleForm, setRuleForm] = useState(DEFAULT_RULE_FORM);
  const [editingRule, setEditingRule] = useState(null);
  const [ruleError, setRuleError] = useState("");
  const [savingRule, setSavingRule] = useState(false);

  const [showChuteModal, setShowChuteModal] = useState(false);
  const [chuteMode, setChuteMode] = useState("manage");
  const [chuteItems, setChuteItems] = useState([]);
  const [chutesLoading, setChutesLoading] = useState(false);
  const [chuteError, setChuteError] = useState("");
  const [chuteForm, setChuteForm] = useState(DEFAULT_CHUTE_FORM);
  const [creatingChute, setCreatingChute] = useState(false);
  const [chuteSelection, setChuteSelection] = useState([]);
  const [chuteDeleting, setChuteDeleting] = useState(false);
  const [chuteToDelete, setChuteToDelete] = useState(null);

  const [resolvedGroup, setResolvedGroup] = useState(Boolean(location.state?.group));
  const colorMap = useMemo(() => {
    const map = new Map();
    COLOR_OPTIONS.forEach((item) => map.set(item.value, item));
    return map;
  }, []);

  const combinedChuteLookup = useMemo(() => {
    const map = new Map();
    chuteItems.forEach((item) => map.set(item.id, item));
    (ruleForm.selectedChutes ?? []).forEach((item) => {
      if (!map.has(item.id)) map.set(item.id, item);
    });
    return map;
  }, [chuteItems, ruleForm.selectedChutes]);

  const hasRules = useMemo(() => rules.length > 0, [rules]);

  useEffect(() => {
    let active = true;
    const resolveGroupMeta = async () => {
      if (resolvedGroup || group?.name) return;
      try {
        const response = await fetchGroups({ page: 0, size: 50 });
        const found = response?.data?.content?.find(
          (item) => String(item.id) === String(groupId)
        );
        if (found && active) {
          setGroup(found);
        }
      } catch (err) {
        console.error(err);
      } finally {
        if (active) setResolvedGroup(true);
      }
    };

    resolveGroupMeta();
    return () => {
      active = false;
    };
  }, [group, groupId, resolvedGroup]);

  const loadRules = async (page = pageState.page) => {
    setLoading(true);
    setError("");
    try {
      const response = await fetchRules(groupId, {
        page,
        size: pageState.size,
        keyword: search.trim() || undefined,
      });
      const data = response?.data;
      setRules(data?.content ?? []);
      setPageState((prev) => ({
        page: data?.page ?? page,
        size: data?.size ?? prev.size,
        totalPages: data?.totalPages ?? prev.totalPages,
        totalElements: data?.totalElements ?? prev.totalElements,
      }));
    } catch (err) {
      console.error(err);
      setRules([]);
      setError(err?.response?.data?.message || "분류 기준을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRules(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search, groupId]);

  const loadChutes = async () => {
    setChutesLoading(true);
    setChuteError("");
    try {
      const response = await fetchChutes({ page: 0, size: CHUTE_PAGE_SIZE });
      const data = response?.data;
      setChuteItems(data?.content ?? []);
    } catch (err) {
      console.error(err);
      setChuteItems([]);
      setChuteError(err?.response?.data?.message || "분류 라인을 불러오지 못했습니다.");
    } finally {
      setChutesLoading(false);
    }
  };

  const ensureChutesLoaded = async () => {
    if (chuteItems.length > 0) return;
    await loadChutes();
  };

  const handleOpenCreateRule = () => {
    setRuleForm(DEFAULT_RULE_FORM);
    setEditingRule(null);
    setRuleError("");
    setShowRuleModal(true);
  };

  const handleEditRule = (rule) => {
    const chuteList = Array.isArray(rule.chutes) ? rule.chutes.filter(Boolean) : [];
    const limited = chuteList.slice(0, 1);
    setRuleForm({
      ruleName: rule.ruleName ?? rule.name ?? "",
      itemName: rule.itemName ?? "",
      inputType: rule.inputType ?? "TEXT",
      inputValue: rule.inputValue ?? "",
      chuteIds: limited.map((item) => item.id),
      selectedChutes: limited,
    });
    setEditingRule(rule);
    setRuleError("");
    setShowRuleModal(true);
  };

  const resetRuleModal = () => {
    setShowRuleModal(false);
    setRuleForm(DEFAULT_RULE_FORM);
    setEditingRule(null);
    setRuleError("");
    setSavingRule(false);
  };

  const handleSaveRule = async () => {
    if (!admin) return;
    const trimmedName = ruleForm.ruleName.trim();
    const trimmedItem = ruleForm.itemName.trim();
    const trimmedValue = ruleForm.inputType === "TEXT" ? ruleForm.inputValue.trim() : ruleForm.inputValue;

    if (!trimmedName) {
      setRuleError("기준 이름을 입력해주세요.");
      return;
    }
    if (!trimmedItem) {
      setRuleError("물품명을 입력해주세요.");
      return;
    }
    if (!trimmedValue) {
      setRuleError("분류 표식 값을 입력하거나 선택해주세요.");
      return;
    }
    if (!ruleForm.chuteIds || ruleForm.chuteIds.length === 0) {
      setRuleError("분류 라인을 최소 1개 이상 선택해주세요.");
      return;
    }

    setSavingRule(true);
    setRuleError("");
    const payload = {
      ruleName: trimmedName,
      itemName: trimmedItem,
      inputType: ruleForm.inputType,
      inputValue: trimmedValue,
      chuteIds: ruleForm.chuteIds,
    };

    try {
      if (editingRule) {
        await updateRule(editingRule.id, payload);
      } else {
        await createRule(groupId, payload);
      }
      resetRuleModal();
      await loadRules(editingRule ? pageState.page : 0);
    } catch (err) {
      console.error(err);
      setRuleError(err?.response?.data?.message || "분류 기준 저장에 실패했습니다.");
      setSavingRule(false);
    }
  };

  const handleDeleteRule = async (rule) => {
    if (!admin) return;
    const confirmed = window.confirm(
      `"${rule.ruleName ?? rule.itemName}" 분류 기준을 삭제하시겠습니까?`
    );
    if (!confirmed) return;

    try {
      await deleteRule(rule.id);
      const isLastOnPage = rules.length === 1 && pageState.page > 0;
      await loadRules(isLastOnPage ? pageState.page - 1 : pageState.page);
    } catch (err) {
      console.error(err);
      setError(err?.response?.data?.message || "분류 기준 삭제에 실패했습니다.");
    }
  };

  const handlePageChange = (offset) => {
    const nextPage = pageState.page + offset;
    if (nextPage < 0 || nextPage >= pageState.totalPages) return;
    loadRules(nextPage);
  };

  const openChuteModal = async (mode, initialSelection = []) => {
    setChuteMode(mode);
    const firstSelected = initialSelection.find((id) => id != null);
    setChuteSelection(firstSelected ? [firstSelected] : []);
    setChuteError("");
    await ensureChutesLoaded();
    setShowChuteModal(true);
  };

  const closeChuteModal = () => {
    setShowChuteModal(false);
    setChuteError("");
    setChuteForm(DEFAULT_CHUTE_FORM);
    setCreatingChute(false);
    setChuteSelection([]);
  };

  const handleCreateChute = async () => {
    if (!admin) return;
    const name = chuteForm.chuteName.trim();
    const angleValue = Number(chuteForm.servoDeg);

    if (!name) {
      setChuteError("분류 라인 이름을 입력해주세요.");
      return;
    }
    if (!Number.isFinite(angleValue)) {
      setChuteError("서보 각도는 숫자로 입력해주세요.");
      return;
    }
    if (angleValue < 0 || angleValue > 180) {
      setChuteError("서보 각도는 0° 이상 180° 이하로 입력해주세요.");
      return;
    }

    setCreatingChute(true);
    setChuteError("");
    try {
      const result = await createChute({ chuteName: name, servoDeg: angleValue });
      const created = result?.data;
      setChuteForm(DEFAULT_CHUTE_FORM);
      await loadChutes();
      if (created?.id && chuteMode === "select") {
        setChuteSelection([created.id]);
      }
    } catch (err) {
      console.error(err);
      setChuteError(err?.response?.data?.message || "분류 라인 생성에 실패했습니다.");
    } finally {
      setCreatingChute(false);
    }
  };

  const handleConfirmChuteSelection = () => {
    const selectedId = chuteSelection[0];
    const detail = selectedId ? combinedChuteLookup.get(selectedId) : null;
    setRuleForm((prev) => ({
      ...prev,
      chuteIds: selectedId ? [selectedId] : [],
      selectedChutes: detail ? [detail] : [],
    }));
    closeChuteModal();
  };

  const currentGroupName = group?.name || `그룹 #${groupId}`;

  return (
    <div className={styles.page}>
      <Header />
      <main className={styles.main}>
        <div className={styles.toolbar}>
          <div className={styles.breadcrumb}>
            <button
              type="button"
              className={styles.backLink}
              onClick={() => navigate("/groups")}
            >
              ← 분류 그룹 목록
            </button>
            <h1 className={styles.heading}>{currentGroupName}</h1>
            <p className={styles.subtitle}>분류 기준 설정</p>
          </div>
          <div className={styles.actions}>
            <input
              className={styles.search}
              placeholder="분류 기준 검색"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
            {admin && (
              <>
                <button
                  type="button"
                  className={styles.secondary}
                  onClick={() => openChuteModal("manage")}
                >
                  분류 라인 관리
                </button>
                <button
                  type="button"
                  className={styles.primary}
                  onClick={handleOpenCreateRule}
                >
                  분류 기준 추가
                </button>
              </>
            )}
          </div>
        </div>

        {error && <div className={styles.alert}>{error}</div>}

        {loading ? (
          <div className={styles.skeleton}>분류 기준을 불러오는 중입니다…</div>
        ) : !hasRules ? (
          <div className={styles.empty}>등록된 분류 기준이 없습니다.</div>
        ) : (
          <>
            <ul className={styles.ruleList}>
              {rules.map((rule) => {
                const typeInfo =
                  rule.inputType === "COLOR"
                    ? colorMap.get(rule.inputValue) ?? { label: rule.inputValue }
                    : null;
                return (
                  <li key={rule.id} className={styles.ruleCard}>
                    <div className={styles.ruleHeader}>
                      <div>
                        <h3 className={styles.ruleTitle}>
                          {rule.ruleName || rule.itemName || `규칙 #${rule.id}`}
                        </h3>
                        <p className={styles.ruleMeta}>
                          생성일 {formatDateTime(rule.createdAt)}
                        </p>
                      </div>
                      {admin && (
                        <div className={styles.ruleActions}>
                          <button
                            type="button"
                            className={styles.linkButton}
                            onClick={() => handleEditRule(rule)}
                          >
                            수정
                          </button>
                          <button
                            type="button"
                            className={`${styles.linkButton} ${styles.danger}`}
                            onClick={() => handleDeleteRule(rule)}
                          >
                            삭제
                          </button>
                        </div>
                      )}
                    </div>

                    <dl className={styles.ruleBody}>
                      <div className={styles.ruleRow}>
                        <dt>물품명</dt>
                        <dd>{rule.itemName || "-"}</dd>
                      </div>
                      <div className={styles.ruleRow}>
                        <dt>분류 표식</dt>
                        <dd className={styles.ruleBadgeRow}>
                          <span className={styles.badge}>
                            {
                              INPUT_TYPES.find((item) => item.value === rule.inputType)
                                ?.label ?? rule.inputType
                            }
                          </span>
                          {rule.inputType === "COLOR" ? (
                            <span className={styles.colorChip}>
                              <span
                                className={styles.colorSwatch}
                                style={{ backgroundColor: typeInfo?.swatch || rule.inputValue }}
                                aria-hidden="true"
                              />
                              <span>{typeInfo?.label ?? rule.inputValue}</span>
                            </span>
                          ) : (
                            <span className={styles.ruleValue}>{rule.inputValue}</span>
                          )}
                        </dd>
                      </div>
                      <div className={styles.ruleRow}>
                        <dt>분류 라인</dt>
                        <dd className={styles.chutePills}>
                          {Array.isArray(rule.chutes) && rule.chutes.length > 0 ? (
                            rule.chutes.map((chute) => (
                              <span key={chute.id} className={styles.chutePill}>
                                {chute.name || chute.chuteName}
                                <span className={styles.chuteAngle}>{chute.angle ?? chute.servoDeg}°</span>
                              </span>
                            ))
                          ) : (
                            <span>-</span>
                          )}
                        </dd>
                      </div>
                    </dl>
                  </li>
                );
              })}
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

      {showRuleModal && admin && (
        <div className={styles.modalBackdrop} onClick={resetRuleModal}>
          <div
            className={styles.modal}
            onClick={(event) => event.stopPropagation()}
          >
            <h3 className={styles.modalTitle}>
              {editingRule ? "분류 기준 수정" : "분류 기준 추가"}
            </h3>
            {ruleError && <div className={styles.modalError}>{ruleError}</div>}
            <label className={styles.field}>
              <span className={styles.fieldLabel}>기준 이름</span>
              <input
                className={styles.input}
                value={ruleForm.ruleName}
                onChange={(event) =>
                  setRuleForm((prev) => ({ ...prev, ruleName: event.target.value }))
                }
                placeholder="분류 기준 이름"
              />
            </label>
            <label className={styles.field}>
              <span className={styles.fieldLabel}>물품명</span>
              <input
                className={styles.input}
                value={ruleForm.itemName}
                onChange={(event) =>
                  setRuleForm((prev) => ({ ...prev, itemName: event.target.value }))
                }
                placeholder="물품명을 입력하세요"
              />
            </label>
            <div className={styles.fieldGrid}>
              <label className={styles.field}>
                <span className={styles.fieldLabel}>분류 표식 종류</span>
                <select
                  className={styles.select}
                  value={ruleForm.inputType}
                  onChange={(event) =>
                    setRuleForm((prev) => ({
                      ...prev,
                      inputType: event.target.value,
                      inputValue: event.target.value === "COLOR" ? COLOR_OPTIONS[0].value : "",
                    }))
                  }
                >
                  {INPUT_TYPES.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>

              <label className={styles.field}>
                <span className={styles.fieldLabel}>분류 표식 값</span>
                {ruleForm.inputType === "COLOR" ? (
                  <div className={styles.selectWrapper}>
                    <select
                      className={styles.select}
                      value={ruleForm.inputValue}
                      onChange={(event) =>
                        setRuleForm((prev) => ({
                          ...prev,
                          inputValue: event.target.value,
                        }))
                      }
                    >
                      {COLOR_OPTIONS.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                    <span
                      className={styles.colorPreview}
                      style={{
                        backgroundColor:
                          colorMap.get(ruleForm.inputValue)?.swatch || ruleForm.inputValue,
                      }}
                      aria-hidden="true"
                    />
                  </div>
                ) : (
                  <input
                    className={styles.input}
                    value={ruleForm.inputValue}
                    onChange={(event) =>
                      setRuleForm((prev) => ({
                        ...prev,
                        inputValue: event.target.value,
                      }))
                    }
                    placeholder="표식 값을 입력하세요"
                  />
                )}
              </label>
            </div>

            <div className={styles.field}>
              <span className={styles.fieldLabel}>분류 라인</span>
              <div className={styles.chuteSelectRow}>
                <div className={styles.chutePills}>
                  {ruleForm.selectedChutes.length > 0 ? (
                    ruleForm.selectedChutes.map((chute) => (
                      <span key={chute.id} className={styles.chutePill}>
                        {chute.name || chute.chuteName}
                        <span className={styles.chuteAngle}>
                          {chute.angle ?? chute.servoDeg}°
                        </span>
                      </span>
                    ))
                  ) : (
                    <span className={styles.placeholder}>선택된 분류 라인이 없습니다.</span>
                  )}
                </div>
                <button
                  type="button"
                  className={styles.secondary}
                  onClick={() => openChuteModal("select", ruleForm.chuteIds)}
                >
                  분류 라인 선택
                </button>
              </div>
            </div>

            <div className={styles.modalActions}>
              <button type="button" className={styles.secondary} onClick={resetRuleModal}>
                취소
              </button>
              <button
                type="button"
                className={styles.primary}
                onClick={handleSaveRule}
                disabled={savingRule}
              >
                {savingRule ? "저장 중…" : "저장"}
              </button>
            </div>
          </div>
        </div>
      )}

      {showChuteModal && (
        <div className={styles.modalBackdrop} onClick={closeChuteModal}>
          <div
            className={`${styles.modal} ${styles.chuteModal}`}
            onClick={(event) => event.stopPropagation()}
          >
            <h3 className={styles.modalTitle}>
              {chuteMode === "manage" ? "분류 라인 관리" : "분류 라인 선택"}
            </h3>
            {chuteError && <div className={styles.modalError}>{chuteError}</div>}

            <div className={styles.chuteList}>
              {chutesLoading ? (
                <div className={styles.skeletonSmall}>분류 라인을 불러오는 중입니다…</div>
              ) : chuteItems.length === 0 ? (
                <div className={styles.emptySmall}>등록된 분류 라인이 없습니다.</div>
              ) : (
                <ul>
                  {chuteItems.map((chute) => {
                    const checked = chuteSelection.includes(chute.id);
                    return (
                      <li key={chute.id} className={styles.chuteItem}>
                          {chuteMode === "select" ? (
                            <label className={styles.checkboxRow}>
                              <input
                                type="radio"
                                checked={checked}
                                onChange={() => setChuteSelection([chute.id])}
                              />
                              <span>
                                {chute.name || chute.chuteName}
                                <span className={styles.chuteItemAngle}>
                                  {chute.angle ?? chute.servoDeg}°
                                </span>
                            </span>
                          </label>
                        ) : (
                          <div className={styles.chuteInfo}>
                            <div>
                              <span className={styles.chuteName}>
                                {chute.name || chute.chuteName}
                              </span>
                              <span className={styles.chuteItemAngle}>
                                {chute.angle ?? chute.servoDeg}°
                              </span>
                            </div>
                            {admin && (
                              <button
                                type="button"
                                className={styles.chuteDelete}
                                onClick={() => setChuteToDelete(chute)}
                              >
                                삭제
                              </button>
                            )}
                          </div>
                        )}
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>

            {admin && chuteMode === "manage" && (
              <div className={styles.chuteForm}>
                <h4 className={styles.chuteFormTitle}>새 분류 라인 추가</h4>
                <div className={styles.fieldGrid}>
                  <label className={styles.field}>
                    <span className={styles.fieldLabel}>라인 이름</span>
                    <input
                      className={styles.input}
                      value={chuteForm.chuteName}
                      onChange={(event) =>
                        setChuteForm((prev) => ({ ...prev, chuteName: event.target.value }))
                      }
                      placeholder="예) 보급부대 A-01"
                    />
                  </label>
                  <label className={styles.field}>
                    <span className={styles.fieldLabel}>서보 각도 (°)</span>
                    <input
                      className={styles.input}
                      type="number"
                      min="0"
                      max="180"
                      value={chuteForm.servoDeg}
                      onChange={(event) =>
                        setChuteForm((prev) => ({ ...prev, servoDeg: event.target.value }))
                      }
                      placeholder="0 - 180"
                    />
                  </label>
                </div>
                <button
                  type="button"
                  className={styles.primary}
                  onClick={handleCreateChute}
                  disabled={creatingChute}
                >
                  {creatingChute ? "추가 중…" : "분류 라인 추가"}
                </button>
              </div>
            )}

            <div className={styles.modalActions}>
              <button type="button" className={styles.secondary} onClick={closeChuteModal}>
                닫기
              </button>
              {chuteMode === "select" && (
                <button
                  type="button"
                  className={styles.primary}
                  onClick={handleConfirmChuteSelection}
                  disabled={chuteSelection.length === 0}
                >
                  선택 완료
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {admin && chuteToDelete && (
        <ConfirmDialog
          title="분류 라인을 삭제할까요?"
          description={`"${chuteToDelete.name || chuteToDelete.chuteName}" 라인이 삭제되면 되돌릴 수 없습니다.`}
          confirmLabel="삭제"
          cancelLabel="취소"
          confirming={chuteDeleting}
          onCancel={() => {
            setChuteToDelete(null);
            setChuteDeleting(false);
          }}
          onConfirm={async () => {
            if (chuteDeleting) return;
            setChuteDeleting(true);
            try {
              await deleteChute(chuteToDelete.id);
              setChuteItems((prev) => prev.filter((item) => item.id !== chuteToDelete.id));
              setChuteSelection((prev) => prev.filter((id) => id !== chuteToDelete.id));
              setRuleForm((prev) => ({
                ...prev,
                chuteIds: prev.chuteIds.filter((id) => id !== chuteToDelete.id),
                selectedChutes: (prev.selectedChutes ?? []).filter(
                  (item) => item.id !== chuteToDelete.id
                ),
              }));
              setChuteToDelete(null);
            } catch (err) {
              console.error(err);
              setChuteError(err?.response?.data?.message || "분류 라인 삭제에 실패했습니다.");
            } finally {
              setChuteDeleting(false);
            }
          }}
        />
      )}
    </div>
  );
}
