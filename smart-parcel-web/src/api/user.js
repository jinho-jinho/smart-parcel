import http from "./http";
import { authStore } from "../store/auth.store";

/**
 * ???�보
 * GET /user/me  (AT ?�요)
 */
export async function fetchMe() {
  const { data } = await http.get("/api/users/me");
  authStore.getState().setUser(data?.data ?? null);
  return data; // { success, data: UserResponseDto, message }
}
