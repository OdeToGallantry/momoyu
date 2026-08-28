import { getToken, redirectToLogin } from "../auth";

/**
 * 流式对话。事件：onDelta(text) / onDone() / onError(message)
 */
export async function streamChat(conversationId, content, { onDelta, onDone, onError, signal } = {}) {
  const token = getToken();
  const res = await fetch("/api/ai/chat", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ conversationId, content }),
    signal,
  });

  if (res.status === 401) {
    redirectToLogin();
    throw new Error("请先登录");
  }

  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error(data.message || `请求失败 (${res.status})`);
  }

  if (!res.body) {
    throw new Error("浏览器不支持流式响应");
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, "\n").replace(/\r/g, "\n");

    let splitAt;
    while ((splitAt = buffer.indexOf("\n\n")) >= 0) {
      const rawEvent = buffer.slice(0, splitAt);
      buffer = buffer.slice(splitAt + 2);
      const ended = handleSseBlock(rawEvent, { onDelta, onDone, onError });
      if (ended) {
        await reader.cancel().catch(() => {});
        return;
      }
    }
  }

  if (buffer.trim()) {
    handleSseBlock(buffer, { onDelta, onDone, onError });
  }
}

function handleSseBlock(rawEvent, { onDelta, onDone, onError }) {
  const lines = rawEvent.split(/\r?\n/);
  let eventName = "message";
  const dataLines = [];

  for (const line of lines) {
    if (line.startsWith("event:")) {
      eventName = line.slice(6).trim();
    } else if (line.startsWith("data:")) {
      dataLines.push(line.slice(5).trim());
    }
  }

  if (!dataLines.length) return false;
  const data = dataLines.join("\n");
  let payload = {};
  try {
    payload = JSON.parse(data);
  } catch {
    if (eventName === "delta" && data) {
      onDelta?.(data);
    }
    return false;
  }

  if (eventName === "delta" && typeof payload.text === "string") {
    onDelta?.(payload.text);
  } else if (eventName === "error") {
    onError?.(payload.message || "对话失败");
    return true;
  } else if (eventName === "done") {
    onDone?.();
    return true;
  }
  return false;
}
