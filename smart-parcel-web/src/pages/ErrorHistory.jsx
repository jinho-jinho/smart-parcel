import HistoryPage from "./HistoryPage";
import { fetchErrorHistory, fetchErrorHistoryDetail } from "../api/history";

const columns = [
  { key: "id", label: "오류 ID", width: "120px" },
  { key: "itemName", label: "물품명" },
  { key: "lineName", label: "라인명", width: "160px" },
  { key: "errorCode", label: "오류코드", type: "badge", width: "160px", color: "#D0342C" },
  { key: "occurredAt", label: "처리일시", type: "datetime", width: "220px" },
];

const detailFields = [
  { key: "id", label: "오류 ID" },
  { key: "itemName", label: "물품명" },
  { key: "lineName", label: "라인명" },
  { key: "errorCode", label: "오류코드" },
  { key: "occurredAt", label: "처리일시", type: "datetime" },
];

export default function ErrorHistory() {
  return (
    <HistoryPage
      title="오류 이력"
      searchPlaceholder="오류 ID, 물품명, 라인명, 오류코드 검색"
      columns={columns}
      detailFields={detailFields}
      fetchList={fetchErrorHistory}
      fetchDetail={fetchErrorHistoryDetail}
      defaultSort="id,DESC"
      emptyMessage="표시할 오류 이력이 없습니다."
    />
  );
}
