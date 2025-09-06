import { create } from "zustand";

export const authStore = create((set) => ({
  accessToken: localStorage.getItem("access_token") || "",
  user: null, // /user/me 결과 캐시 용도

  setAccessToken: (token) => {
    localStorage.setItem("access_token", token || "");
    set({ accessToken: token || "" });
  },
  setUser: (user) => set({ user }),

  clear: () => {
    localStorage.removeItem("access_token");
    set({ accessToken: "", user: null });
  },
}));
