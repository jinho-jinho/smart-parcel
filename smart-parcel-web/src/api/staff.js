import http from "./http";

const BASE_PATH = "/api/admin/staff";

const toParams = (params = {}) => {
  const filtered = {};
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    filtered[key] = value;
  });
  return filtered;
};

export async function fetchStaff({ page = 0, size = 20, keyword } = {}) {
  const { data } = await http.get(BASE_PATH, {
    params: toParams({ page, size, q: keyword }),
  });
  return data;
}

export async function deleteStaff(staffId) {
  const { data } = await http.delete(`${BASE_PATH}/${staffId}`);
  return data;
}
