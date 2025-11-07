import http from "./http";

const BASE_PATH = "/api/stats";

const toApiParams = (params = {}) => {
  const filtered = {};
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    filtered[key] = value;
  });
  return filtered;
};

const get = async (path, params) => {
  const { data } = await http.get(`${BASE_PATH}${path}`, {
    params: toApiParams(params),
  });
  return data?.data;
};

export async function fetchStatsByChute(params = {}) {
  return (await get("/by-chute", params)) ?? [];
}

export async function fetchStatsDaily(params = {}) {
  return (await get("/daily", params)) ?? [];
}

export async function fetchStatsByErrorCode(params = {}) {
  return (await get("/by-error-code", params)) ?? [];
}

export async function fetchErrorRate(params = {}) {
  return (
    (await get("/error-rate", params)) ?? {
      totalProcessed: 0,
      totalErrors: 0,
      errorRatePercent: 0,
    }
  );
}
