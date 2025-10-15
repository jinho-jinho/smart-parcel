import http from "./http";

const BASE_PATH = "/api/sorting-groups";

function adaptGroup(item) {
  if (!item) return item;
  return {
    ...item,
    name: item.groupName,
    active: item.enabled,
    lastUpdatedAt: item.updatedAt,
    managerId: item.managerId,
    managerName: item.managerName,
    processingCount:
      item.currentProcessingCount ??
      item.processingCount ??
      item.processingItemCount ??
      null,
  };
}

function adaptPageResponse(data) {
  if (!data?.data?.content) return data;
  return {
    ...data,
    data: {
      ...data.data,
      content: data.data.content.map(adaptGroup),
    },
  };
}

/** 분류 그룹 목록 조회 */
export async function fetchGroups({
  page = 0,
  size = 20,
  sort,
  keyword,
  enabled,
} = {}) {
  const params = { page, size };
  if (sort) params.sort = sort;
  if (keyword) params.q = keyword;
  if (enabled !== undefined && enabled !== null) params.enabled = enabled;

  const { data } = await http.get(BASE_PATH, { params });
  return adaptPageResponse(data);
}

/** 분류 그룹 생성 (관리자) */
export async function createGroup({ name, groupName }) {
  const payload = {
    groupName: (groupName ?? name ?? "").trim(),
  };
  const { data } = await http.post(BASE_PATH, payload);
  data.data = adaptGroup(data.data);
  return data;
}

/** 분류 그룹 수정 (관리자) */
export async function updateGroup(id, { name, groupName }) {
  const payload = {
    groupName: (groupName ?? name ?? "").trim(),
  };
  const { data } = await http.patch(`${BASE_PATH}/${id}`, payload);
  data.data = adaptGroup(data.data);
  return data;
}

/** 분류 그룹 삭제 (관리자) */
export async function deleteGroup(id) {
  const { data } = await http.delete(`${BASE_PATH}/${id}`);
  return data;
}

/** 분류 그룹 활성화 / 비활성화 */
export async function toggleActive(id, active) {
  const endpoint = active ? "enable" : "disable";
  const { data } = await http.post(`${BASE_PATH}/${id}/${endpoint}`);
  return data;
}
