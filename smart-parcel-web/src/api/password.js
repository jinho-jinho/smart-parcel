import http from "./http";

/**
 * ???? ???
 * POST /api/auth/password/reset
 * body: { email, code, newPassword }
 * (?? ??: sendCode({purpose:"RESET_PASSWORD"}) ? verifyCode(...))
 */
export async function resetPassword({ email, code, newPassword }) {
  const { data } = await http.post("/api/auth/password/reset", {
    email,
    code,
    newPassword,
  });
  return data; // { success, data, message }
}
