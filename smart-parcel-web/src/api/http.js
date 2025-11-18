import axios from "axios";
import { authStore } from "../store/auth.store";

export const baseURL = import.meta.env.VITE_API_BASE_URL || "https://smartparcel-api.azurewebsites.net";

const http = axios.create({
  baseURL,
  withCredentials: true, // 서버의 httpOnly RT 쿠키 송수신
});

// ===== 요청 인터셉터: AT 붙이기 =====
http.interceptors.request.use((config) => {
  const { accessToken } = authStore.getState();
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
  return config;
});

// ===== 401 처리: 토큰 재발급(회전) + 대기열 =====
let isRefreshing = false;
let queue = []; // 재발급 중 대기중인 요청들

const processQueue = (error, token = null) => {
  queue.forEach(({ resolve, reject }) => (error ? reject(error) : resolve(token)));
  queue = [];
};

http.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error?.config;
    const status = error?.response?.status;
    const isRefreshCall = original?.url?.endsWith("/api/auth/token/refresh");

    // 401이고, 아직 재시도 안 했고, 리프레시 요청 자체가 아니라면
    if (status === 401 && !original?._retry && !isRefreshCall) {
      original._retry = true;

      if (isRefreshing) {
        // 재발급 중이면 큐에 넣고 기다렸다가 재시도
        return new Promise((resolve, reject) => {
          queue.push({
            resolve: (token) => {
              if (token) original.headers.Authorization = `Bearer ${token}`;
              resolve(http(original));
            },
            reject,
          });
        });
      }

      isRefreshing = true;
      try {
        // RT는 쿠키에서 읽힘
        const refreshRes = await axios.post(
          `${baseURL}/api/auth/token/refresh`,
          {},
          { withCredentials: true }
        );
        const newAt = refreshRes?.data?.data?.accessToken;
        if (!newAt) throw new Error("No accessToken in refresh response");

        // 상태 저장
        authStore.getState().setAccessToken(newAt);

        // 대기중 요청 처리
        processQueue(null, newAt);

        // 현재 요청 재시도
        original.headers.Authorization = `Bearer ${newAt}`;
        return http(original);
      } catch (e) {
        processQueue(e, null);
        authStore.getState().clear(); // 로그인 상태 초기화
        return Promise.reject(e);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default http;
