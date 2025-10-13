export const ROLES = { MANAGER: "MANAGER", STAFF: "STAFF" };

const normalizeRole = (r) => {
  if (!r) return null;
  const s = String(r).toUpperCase();
  return s.startsWith("ROLE_") ? s.slice(5) : s;
};

export function isManager(user) {
  return normalizeRole(user?.role) === ROLES.MANAGER;
}
export function hasAnyRole(user, roles = []) {
  const role = normalizeRole(user?.role);
  return !!role && roles.map(normalizeRole).includes(role);
}
