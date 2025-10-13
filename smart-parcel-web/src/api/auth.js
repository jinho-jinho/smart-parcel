import http from "./http.js";
import { authStore } from "../store/auth.store";

/**
 * 회원가입
 * POST /api/auth/signup
 * body: { email, password, name, bizNumber?, role, managerEmail? }
 * - role: "MANAGER" | "STAFF"
 * - role === "STAFF" 이면 managerEmail 필수(백엔드 검증)
 */
export async function signup({ email, password, name, bizNumber, role = "STAFF", managerEmail }) {
  const { data } = await http.post("/api/auth/signup", {
    email,
    password,
    name,
    bizNumber: bizNumber ?? null,
    role,
    managerEmail: role === "STAFF" ? managerEmail : null,
  });
  return data; // { success, data: userId, message }
}

/**
 * 로그인: AT 응답 + RT 쿠키
 * POST /api/auth/login
 * body: { email, password }
 */
export async function login({ email, password }) {
  const { data } = await http.post("/api/auth/login", { email, password });
  const at = data?.data?.accessToken;
  if (at) {
    authStore.getState().setAccessToken(at);
  }
  return data; // { success, data:{accessToken, tokenType, expiresInMs}, message }
}

/**
 * 토큰 재발급(회전) — 보통은 인터셉터에서 자동 처리
 * POST /api/auth/token/refresh
 */
export async function refreshToken() {
  const { data } = await http.post("/api/auth/token/refresh", {});
  const at = data?.data?.accessToken;
  if (at) authStore.getState().setAccessToken(at);
  return data;
}

/**
 * 로그아웃 (서버: jti 삭제 + RT 쿠키 만료)
 * POST /api/auth/logout
 */
export async function logout() {
  try {
    await http.post("/api/auth/logout", {});
  } finally {
    authStore.getState().clear();
  }
}
