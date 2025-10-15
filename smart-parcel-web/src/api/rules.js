import http from "./http";

const GROUPS_BASE = "/api/sorting-groups";
const RULES_BASE = "/api/sorting-rules";

function adaptRule(rule) {
  if (!rule) return rule;
  const chutes = Array.isArray(rule.chutes)
    ? rule.chutes.map((item) => ({
        id: item.id,
        name: item.chuteName,
        angle: item.servoDeg,
      }))
    : [];
  return {
    ...rule,
    name: rule.ruleName,
    inputType: rule.inputType,
    inputTypeLabel: rule.inputType,
    chutes,
  };
}

function adaptPageResponse(data) {
  if (!data?.data?.content) return data;
  return {
    ...data,
    data: {
      ...data.data,
      content: data.data.content.map(adaptRule),
    },
  };
}

export async function fetchRules(groupId, { page = 0, size = 20, sort, keyword, type } = {}) {
  const params = { page, size };
  if (sort) params.sort = sort;
  if (keyword) params.q = keyword;
  if (type) params.type = type;

  const { data } = await http.get(`${GROUPS_BASE}/${groupId}/rules`, { params });
  return adaptPageResponse(data);
}

export async function createRule(groupId, payload) {
  const body = {
    ruleName: (payload.ruleName ?? payload.name ?? "").trim(),
    inputType: payload.inputType,
    inputValue: payload.inputValue,
    itemName: payload.itemName,
    chuteIds: payload.chuteIds ?? [],
  };
  const { data } = await http.post(`${GROUPS_BASE}/${groupId}/rules`, body);
  data.data = adaptRule(data.data);
  return data;
}

export async function updateRule(ruleId, payload) {
  const body = { ...payload };
  if (payload.name && !payload.ruleName) {
    body.ruleName = payload.name;
    delete body.name;
  }
  const { data } = await http.patch(`${RULES_BASE}/${ruleId}`, body);
  data.data = adaptRule(data.data);
  return data;
}

export async function deleteRule(ruleId) {
  const { data } = await http.delete(`${RULES_BASE}/${ruleId}`);
  return data;
}
