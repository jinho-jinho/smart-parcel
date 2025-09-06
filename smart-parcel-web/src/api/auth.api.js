import http from "./http.js";
import { authStore } from "../store/auth.store";

/**
 * 회원가입
 * POST /user/signup
 * body: { email, password, name, bizNumber? }
 */
export async function signup({ email, password, name, bizNumber, role }) {
  // role은 서버 UserRole enum과 동일 문자열이어야 함: "ADMIN" | "STAFF" 등
  return http.post("/user/signup", {
    email,
    password,
    name,
    bizNumber: bizNumber || null,
    role, 
  });
}

/**
 * 로그인: AT 응답 + RT 쿠키
 * POST /user/login
 * body: { email, password }
 */
export async function login({ email, password }) {
  const { data } = await http.post("/user/login", { email, password });
  const at = data?.data?.accessToken;
  if (at) {
    authStore.getState().setAccessToken(at);
  }
  return data; // { success, data: { accessToken, tokenType, expiresInMs }, message }
}

/**
 * 내 정보
 * GET /user/me
 * (AT 필요)
 */
export async function fetchMe() {
  const { data } = await http.get("/user/me");
  authStore.getState().setUser(data?.data || null);
  return data; // { success, data: UserResponseDto, message }
}

/**
 * 토큰 재발급 (보통은 인터셉터에서 자동 처리)
 * POST /user/token/refresh
 */
export async function refreshToken() {
  const { data } = await http.post("/user/token/refresh", {});
  const at = data?.data?.accessToken;
  if (at) authStore.getState().setAccessToken(at);
  return data;
}

/**
 * 로그아웃 (서버: jti 삭제 + RT 쿠키 만료)
 * POST /user/logout
 */
export async function logout() {
  try {
    await http.post("/user/logout", {});
  } finally {
    authStore.getState().clear();
  }
}
