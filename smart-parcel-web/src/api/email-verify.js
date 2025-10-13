import http from "./http";

/** 인증코드 전송
 * POST /api/auth/send-code
 * body: { email, purpose }  // purpose: "SIGNUP" | "RESET_PASSWORD"
 */
export async function sendCode({ email, purpose = "SIGNUP" }) {
  const { data } = await http.post("/api/auth/send-code", { email, purpose });
  return data; // { success, data, message }
}

/** 인증코드 검증
 * POST /api/auth/verify-code
 * body: { email, code, purpose }
 */
export async function verifyCode({ email, code, purpose = "SIGNUP" }) {
  const { data } = await http.post("/api/auth/verify-code", { email, code, purpose });
  return data; // { success, data, message }
}
