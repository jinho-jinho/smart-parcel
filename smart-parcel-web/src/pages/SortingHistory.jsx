import HistoryPage from "./HistoryPage";
import {
  fetchSortingHistory,
  fetchSortingHistoryDetail,
} from "../api/history";

const columns = [
  { key: "id", label: "분류 ID", width: "120px" },
  { key: "itemName", label: "물품명" },
  { key: "lineName", label: "라인명", width: "160px" },
  { key: "processedAt", label: "처리일시", type: "datetime", width: "220px" },
];

const detailFields = [
  { key: "id", label: "분류 ID" },
  { key: "itemName", label: "물품명" },
  { key: "lineName", label: "라인명" },
  { key: "processedAt", label: "처리일시", type: "datetime" },
];

export default function SortingHistory() {
  return (
    <HistoryPage
      title="분류 이력"
      searchPlaceholder="분류 ID, 물품명, 라인명 검색"
      columns={columns}
      detailFields={detailFields}
      fetchList={fetchSortingHistory}
      fetchDetail={fetchSortingHistoryDetail}
      defaultSort="id,DESC"
      emptyMessage="표시할 분류 이력이 없습니다."
    />
  );
}
