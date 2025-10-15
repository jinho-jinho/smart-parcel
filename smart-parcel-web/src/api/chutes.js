import http from "./http";

const BASE_PATH = "/api/chutes";

const adaptChute = (item) => {
  if (!item) return item;
  return {
    ...item,
    name: item.chuteName,
    angle: item.servoDeg,
  };
};

const adaptPageResponse = (payload) => {
  if (!payload?.data?.content) return payload;
  return {
    ...payload,
    data: {
      ...payload.data,
      content: payload.data.content.map(adaptChute),
    },
  };
};

export async function fetchChutes({ page = 0, size = 20, sort, keyword, groupId } = {}) {
  const params = { page, size };
  if (sort) params.sort = sort;
  if (keyword) params.q = keyword;
  if (groupId) params.groupId = groupId;

  const { data } = await http.get(BASE_PATH, { params });
  return adaptPageResponse(data);
}

export async function createChute(payload) {
  const body = {
    chuteName: (payload.chuteName ?? payload.name ?? "").trim(),
    servoDeg: payload.servoDeg,
  };
  const { data } = await http.post(BASE_PATH, body);
  data.data = adaptChute(data.data);
  return data;
}

export async function updateChute(id, payload) {
  const body = { ...payload };
  if (payload.name && !payload.chuteName) {
    body.chuteName = payload.name;
    delete body.name;
  }
  const { data } = await http.patch(`${BASE_PATH}/${id}`, body);
  data.data = adaptChute(data.data);
  return data;
}

export async function deleteChute(id) {
  const { data } = await http.delete(`${BASE_PATH}/${id}`);
  return data;
}
