import http from "./http";

const SORTING_HISTORY_PATH = "/api/sorting/history";
const ERROR_HISTORY_PATH = "/api/errors/history";

const toApiParams = (params = {}) => {
  const filtered = {};
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    filtered[key] = value;
  });
  return filtered;
};

const adaptSortingSummary = (item) => {
  if (!item) return item;
  return {
    id: item.id,
    itemName: item.itemName,
    lineName: item.lineName,
    processedAt: item.processedAt,
  };
};

const adaptSortingDetail = (item) => {
  if (!item) return item;
  return {
    id: item.id,
    itemName: item.itemName,
    lineName: item.lineName,
    processedAt: item.processedAt,
    images: item.images ?? null,
  };
};

const adaptErrorSummary = (item) => {
  if (!item) return item;
  return {
    id: item.id,
    itemName: item.itemName,
    lineName: item.lineName,
    errorCode: item.errorCode,
    occurredAt: item.occurredAt,
  };
};

const adaptErrorDetail = (item) => {
  if (!item) return item;
  return {
    id: item.id,
    itemName: item.itemName,
    lineName: item.lineName,
    errorCode: item.errorCode,
    occurredAt: item.occurredAt,
    images: item.images ?? null,
  };
};

const adaptPage = (data, mapper) => {
  if (!data?.data?.content) return data;
  return {
    ...data,
    data: {
      ...data.data,
      content: data.data.content.map(mapper),
    },
  };
};

export async function fetchSortingHistory(params = {}) {
  const { data } = await http.get(SORTING_HISTORY_PATH, {
    params: toApiParams({
      page: params.page,
      size: params.size,
      sort: params.sort,
      q: params.keyword,
      from: params.from,
      to: params.to,
      groupId: params.groupId,
    }),
  });
  return adaptPage(data, adaptSortingSummary);
}

export async function fetchSortingHistoryDetail(historyId) {
  const { data } = await http.get(`${SORTING_HISTORY_PATH}/${historyId}`);
  if (data?.data) {
    data.data = adaptSortingDetail(data.data);
  }
  return data;
}

export async function fetchErrorHistory(params = {}) {
  const { data } = await http.get(ERROR_HISTORY_PATH, {
    params: toApiParams({
      page: params.page,
      size: params.size,
      sort: params.sort,
      q: params.keyword,
      from: params.from,
      to: params.to,
      groupId: params.groupId,
    }),
  });
  return adaptPage(data, adaptErrorSummary);
}

export async function fetchErrorHistoryDetail(historyId) {
  const { data } = await http.get(`${ERROR_HISTORY_PATH}/${historyId}`);
  if (data?.data) {
    data.data = adaptErrorDetail(data.data);
  }
  return data;
}
