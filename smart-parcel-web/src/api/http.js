import axios from "axios";
import { authStore } from "../store/auth.store";

// API base URL
const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const http = axios.create({
  baseURL,
  withCredentials: true, // RT 쿠키 전송/수신
});

// ------- 요청 인터셉터: AT 붙이기 -------
http.interceptors.request.use((config) => {
  const { accessToken } = authStore.getState();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// ------- 401 처리: 토큰 재발급(회전) + 재시도 -------
let isRefreshing = false;
let queue = []; // 재발급 도중 대기 중인 요청들

const processQueue = (error, token = null) => {
  queue.forEach(({ resolve, reject }) => {
    if (error) reject(error);
    else resolve(token);
  });
  queue = [];
};

http.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error?.config;
    const status = error?.response?.status;

    // (1) 401이고, (2) 재시도 플래그가 아직 없으면 재발급 시도
    if (status === 401 && !original?._retry) {
      original._retry = true;

      if (isRefreshing) {
        // 이미 재발급 중이면 큐에 넣고 대기
        return new Promise((resolve, reject) => {
          queue.push({
            resolve: (token) => {
              // 새 AT로 헤더 교체 후 재시도
              if (token) original.headers.Authorization = `Bearer ${token}`;
              resolve(http(original));
            },
            reject: (err) => reject(err),
          });
        });
      }

      isRefreshing = true;
      try {
        // /user/token/refresh (RT는 쿠키에서 읽힘)
        const refreshRes = await axios.post(
          `${baseURL}/user/token/refresh`,
          {},
          { withCredentials: true }
        );
        const newAt = refreshRes?.data?.data?.accessToken;
        if (!newAt) throw new Error("No accessToken in refresh response");

        // 상태 저장
        authStore.getState().setAccessToken(newAt);

        // 대기중인 요청들 처리
        processQueue(null, newAt);

        // 현재 요청 재시도
        original.headers.Authorization = `Bearer ${newAt}`;
        return http(original);
      } catch (e) {
        processQueue(e, null);
        // 재발급 실패 → 로그아웃 처리
        authStore.getState().clear();
        return Promise.reject(e);
      } finally {
        isRefreshing = false;
      }
    }
    return Promise.reject(error);
  }
);

export default http;
