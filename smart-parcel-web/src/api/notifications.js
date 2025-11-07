import http from "./http";

const BASE_PATH = "/api/notifications";

const toParams = (params = {}) => {
  const filtered = {};
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    filtered[key] = value;
  });
  return filtered;
};

export async function fetchNotifications({ page = 0, size = 10, unreadOnly } = {}) {
  const { data } = await http.get(BASE_PATH, {
    params: toParams({ page, size, unreadOnly }),
  });
  return data;
}

export async function markNotificationRead(notificationId) {
  const { data } = await http.patch(`${BASE_PATH}/${notificationId}/read`);
  return data;
}
