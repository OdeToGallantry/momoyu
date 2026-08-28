import { apiRequest } from "../auth";

export function fetchZhihuHotList() {
  return apiRequest("/api/zhihu/hot");
}
