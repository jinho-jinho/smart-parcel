import http from "./http";

/**
 * 비밀번호 재설정
 * POST /user/password/reset
 * body: { email, code, newPassword }
 * (사전단계: sendCode({purpose:"RESET_PASSWORD"}) → verifyCode({purpose:"RESET_PASSWORD"}))
 */
export async function resetPassword({ email, code, newPassword }) {
  const { data } = await http.post("/user/password/reset", {
    email,
    code,
    newPassword,
  });
  return data; // { success, data:null, message:"비밀번호가 변경되었습니다." }
}
