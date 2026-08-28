import { computed, ref } from "vue";

const TOKEN_KEY = "eatchoice.token";
const USER_KEY = "eatchoice.user";

const tokenRef = ref(localStorage.getItem(TOKEN_KEY) || "");
const userRef = ref(readUser());

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || "null");
  } catch {
    return null;
  }
}

function syncFromStorage() {
  tokenRef.value = localStorage.getItem(TOKEN_KEY) || "";
  userRef.value = readUser();
}

export function getToken() {
  return tokenRef.value;
}

export function getUser() {
  return userRef.value;
}

export const currentUser = computed(() => userRef.value);
export const loggedIn = computed(() => Boolean(tokenRef.value));
export const isAdminUser = computed(() => userRef.value?.role === "ADMIN");

export function isLoggedIn() {
  return Boolean(tokenRef.value);
}

export function isAdmin() {
  return userRef.value?.role === "ADMIN";
}

export function setSession({ token, username, role }) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify({ username, role }));
  syncFromStorage();
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  syncFromStorage();
}

export async function login(username, password) {
  const res = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.message || "登录失败");
  setSession(data);
  return data;
}

export function logout() {
  clearSession();
}

/** 只允许站内路径，避免 `?redirect=//evil.com` 一类跳转 */
export function sanitizeRedirect(raw) {
  if (typeof raw !== "string" || !raw.startsWith("/") || raw.startsWith("//") || raw.includes("\\")) {
    return "/";
  }
  return raw;
}

export function redirectToLogin() {
  clearSession();
  if (window.location.pathname === "/login") return;
  const from = `${window.location.pathname}${window.location.search}`;
  window.location.assign(`/login?redirect=${encodeURIComponent(sanitizeRedirect(from))}`);
}

export async function apiRequest(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (options.body && !(options.body instanceof FormData) && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  const res = await fetch(path, { ...options, headers });
  if (res.status === 401) {
    redirectToLogin();
    throw new Error("请先登录");
  }
  if (res.status === 204) return null;
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.message || res.statusText);
  return data;
}
