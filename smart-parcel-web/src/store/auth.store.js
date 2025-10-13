// src/store/auth.store.js
import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";

export const authStore = create(
  persist(
    (set) => ({
      accessToken: null,
      user: null,

      setAccessToken: (token) => set({ accessToken: token || null }),
      setUser: (user) => set({ user: user || null }),
      clear: () => set({ accessToken: null, user: null }),
    }),
    {
      name: "sp-auth",
      storage: createJSONStorage(() => localStorage),
      partialize: (s) => ({ accessToken: s.accessToken, user: s.user }),
    }
  )
);
