import { apiRequest } from "../auth";

export function listConversations() {
  return apiRequest("/api/conversations");
}

export function createConversation() {
  return apiRequest("/api/conversations", { method: "POST" });
}

export function getConversation(id) {
  return apiRequest(`/api/conversations/${id}`);
}

export function renameConversation(id, title) {
  return apiRequest(`/api/conversations/${id}`, {
    method: "PATCH",
    body: JSON.stringify({ title }),
  });
}

export function deleteConversation(id) {
  return apiRequest(`/api/conversations/${id}`, { method: "DELETE" });
}
