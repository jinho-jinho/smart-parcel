import { useCallback } from "react";
import { authStore } from "../store/auth.store";
import { login as apiLogin, logout as apiLogout, fetchMe } from "../api/auth.api";

export function useAuth() {
  const { accessToken, user } = authStore();

  const login = useCallback(async (email, password) => {
    await apiLogin({ email, password });
    await fetchMe();
  }, []);

  const logout = useCallback(async () => {
    await apiLogout();
  }, []);

  return { accessToken, user, login, logout, fetchMe };
}
