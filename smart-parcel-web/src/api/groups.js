import http from "./http";

/** 그룹 목록 */
export async function fetchGroups({ page = 0, size = 20 } = {}) {
  const { data } = await http.get("/api/groups", { params: { page, size } });
  return data; // { success, data: { content: [...], totalElements, ... } }
}

/** 단건 조회 */
export async function fetchGroup(id) {
  const { data } = await http.get(`/api/groups/${id}`);
  return data;
}

/** 생성(관리자) */
export async function createGroup(payload) {
  // payload: { name, code, description? }
  const { data } = await http.post("/api/groups", payload);
  return data;
}

/** 활성/비활성 토글(관리자) */
export async function toggleActive(id, active) {
  const { data } = await http.patch(`/api/groups/${id}/active`, { active });
  return data;
}

/** 이름/설명 수정(관리자) */
export async function updateGroup(id, payload) {
  const { data } = await http.patch(`/api/groups/${id}`, payload);
  return data;
}
