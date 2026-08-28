import { apiRequest, getToken, redirectToLogin } from "../auth";

export function listDishes({
  q = "",
  favoriteOnly = false,
  page = 0,
  size = 20,
} = {}) {
  const params = new URLSearchParams();
  if (q) params.set("q", q);
  if (favoriteOnly) params.set("favoriteOnly", "true");
  params.set("page", String(page));
  params.set("size", String(size));
  return apiRequest(`/api/dishes/list?${params}`);
}

export function getDish(id) {
  return apiRequest(`/api/dishes/detail/${id}`);
}

export function randomDish(favoriteOnly = false) {
  const query = favoriteOnly ? "?favoriteOnly=true" : "";
  return apiRequest(`/api/dishes/random${query}`);
}

export function createDish(payload) {
  return apiRequest("/api/dishes/create", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateDish(id, payload) {
  return apiRequest(`/api/dishes/update/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function deleteDish(id) {
  return apiRequest(`/api/dishes/delete/${id}`, { method: "DELETE" });
}

export function importDishesExcel(file) {
  const body = new FormData();
  body.append("file", file);
  return apiRequest("/api/dishes/import", { method: "POST", body });
}

export async function downloadImportTemplate() {
  const token = getToken();
  const res = await fetch("/api/dishes/import-template", {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (res.status === 401) {
    redirectToLogin();
    throw new Error("请先登录");
  }
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || "下载失败");
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "dishes-import.xlsx";
  a.click();
  URL.revokeObjectURL(url);
}
