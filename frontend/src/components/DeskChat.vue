<script setup>
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { streamChat } from "../api/ai";
import {
  createConversation,
  deleteConversation,
  getConversation,
  listConversations,
} from "../api/conversations";
import { currentUser } from "../auth";
import ChatHistory from "./ChatHistory.vue";

const emit = defineEmits(["moodChange"]);

const messages = ref([]);
const draft = ref("");
const streaming = ref(false);
const focused = ref(false);
const error = ref("");
const listEl = ref(null);
const historyOpen = ref(false);
const conversations = ref([]);
const activeConversationId = ref(null);
const loadingSessions = ref(false);
let abortController = null;

const mood = computed(() => {
  if (streaming.value) {
    const last = messages.value[messages.value.length - 1];
    if (last?.role === "assistant" && last.content) return "talk";
    return "think";
  }
  if (focused.value || draft.value.trim()) return "listen";
  return "idle";
});

const statusLine = computed(() => {
  switch (mood.value) {
    case "talk":
      return "哼哼";
    case "think":
      return "唔…想想怎么怼";
    case "listen":
      return "勉强在听";
    default:
      return "才没在等你";
  }
});

function isTypingBubble(index) {
  return (
    streaming.value &&
    index === messages.value.length - 1 &&
    messages.value[index]?.role === "assistant" &&
    !messages.value[index]?.content
  );
}

watch(mood, (value) => emit("moodChange", value), { immediate: true });

watch(historyOpen, (open) => {
  if (open) refreshConversationList();
});

function buildLocalTitle(text) {
  const trimmed = text.trim().replace(/\s+/g, " ");
  if (!trimmed) return "新对话";
  return trimmed.length <= 20 ? trimmed : `${trimmed.slice(0, 20)}…`;
}

function patchConversationTitle(conversationId, text) {
  const item = conversations.value.find((c) => c.id === conversationId);
  if (!item || item.title !== "新对话") return;
  item.title = buildLocalTitle(text);
}

onMounted(() => {
  reloadForCurrentUser();
});

watch(
  () => currentUser.value?.username || "",
  (username, prev) => {
    if (username === prev) return;
    reloadForCurrentUser();
  },
);

async function reloadForCurrentUser() {
  if (streaming.value) {
    abortController?.abort();
    streaming.value = false;
    abortController = null;
  }
  error.value = "";
  draft.value = "";
  messages.value = [];
  conversations.value = [];
  activeConversationId.value = null;
  historyOpen.value = false;

  if (!currentUser.value?.username) return;

  loadingSessions.value = true;
  try {
    const list = await listConversations();
    conversations.value = list;
    await startNewConversation({ refreshList: false });
  } catch (e) {
    error.value = e.message || "加载对话失败";
  } finally {
    loadingSessions.value = false;
  }
}

async function refreshConversationList() {
  conversations.value = await listConversations();
}

async function loadMessages(conversationId) {
  const detail = await getConversation(conversationId);
  messages.value = detail.messages.map(({ role, content }) => ({
    role,
    content,
  }));
  await scrollToBottom();
}

async function selectConversation(
  id,
  { refreshList = true, closeHistory = false } = {},
) {
  if (streaming.value || id === activeConversationId.value) return;
  error.value = "";
  activeConversationId.value = id;
  try {
    await loadMessages(id);
    if (refreshList) await refreshConversationList();
    if (closeHistory) historyOpen.value = false;
  } catch (e) {
    error.value = e.message || "加载对话失败";
  }
}

function onHistorySelect(id) {
  selectConversation(id, { closeHistory: true });
}

async function startNewConversation({ refreshList = true } = {}) {
  if (streaming.value) return;
  error.value = "";
  try {
    const previousId = activeConversationId.value;
    const previousHadUserInput = messages.value.some((m) => m.role === "user");
    const created = await createConversation();
    activeConversationId.value = created.id;
    messages.value = [];
    if (previousId && !previousHadUserInput) {
      await deleteConversation(previousId).catch(() => {});
    }
    if (refreshList) await refreshConversationList();
  } catch (e) {
    error.value = e.message || "新建对话失败";
  }
}

async function onDeleteConversation(id) {
  if (streaming.value) return;
  error.value = "";
  try {
    await deleteConversation(id);
    conversations.value = conversations.value.filter((c) => c.id !== id);
    if (activeConversationId.value === id) {
      if (conversations.value.length) {
        await selectConversation(conversations.value[0].id, {
          refreshList: false,
        });
      } else {
        await startNewConversation({ refreshList: false });
      }
    }
    await refreshConversationList();
  } catch (e) {
    error.value = e.message || "删除失败";
  }
}

async function scrollToBottom() {
  await nextTick();
  const el = listEl.value;
  if (el) el.scrollTop = el.scrollHeight;
}

async function onSubmit() {
  const text = draft.value.trim();
  if (!text || streaming.value || !activeConversationId.value) return;

  error.value = "";
  draft.value = "";
  messages.value.push({ role: "user", content: text });
  messages.value.push({ role: "assistant", content: "" });
  streaming.value = true;
  await scrollToBottom();

  abortController = new AbortController();
  const conversationId = activeConversationId.value;
  patchConversationTitle(conversationId, text);

  try {
    await streamChat(conversationId, text, {
      signal: abortController.signal,
      onDelta(chunk) {
        if (!streaming.value) return;
        const last = messages.value[messages.value.length - 1];
        if (last?.role === "assistant") {
          last.content += chunk;
          scrollToBottom();
        }
      },
      onDone() {
        streaming.value = false;
      },
      onError(message) {
        error.value = message;
        streaming.value = false;
      },
    });
    await refreshConversationList();
  } catch (e) {
    if (e.name !== "AbortError") {
      error.value = e.message || "对话失败";
      const last = messages.value[messages.value.length - 1];
      if (last?.role === "assistant" && !last.content) {
        messages.value.pop();
      }
      const userLast = messages.value[messages.value.length - 1];
      if (userLast?.role === "user" && userLast.content === text) {
        messages.value.pop();
      }
    }
  } finally {
    streaming.value = false;
    abortController = null;
    const last = messages.value[messages.value.length - 1];
    if (last?.role === "assistant" && !last.content) {
      messages.value.pop();
    }
    await scrollToBottom();
  }
}

function onStop() {
  abortController?.abort();
  streaming.value = false;
}
</script>

<template>
  <div class="desk-chat" @click.stop>
    <div class="desk-chat-main">
      <header class="desk-chat-head">
        <div class="desk-chat-head-title">
          <strong>小克</strong>
          <span>{{ statusLine }}</span>
        </div>
        <div class="desk-chat-head-actions">
          <n-popover
            v-model:show="historyOpen"
            trigger="click"
            placement="bottom-end"
            :show-arrow="true"
            content-class="chat-history-popover"
            :content-style="{ padding: 0 }"
          >
            <template #trigger>
              <n-button
                quaternary
                size="tiny"
                class="desk-chat-icon-btn"
                aria-label="历史"
                :aria-expanded="historyOpen"
              >
                <svg
                  class="desk-chat-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  aria-hidden="true"
                >
                  <line x1="8" y1="6" x2="21" y2="6" />
                  <line x1="8" y1="12" x2="21" y2="12" />
                  <line x1="8" y1="18" x2="21" y2="18" />
                  <line x1="3" y1="6" x2="3.01" y2="6" />
                  <line x1="3" y1="12" x2="3.01" y2="12" />
                  <line x1="3" y1="18" x2="3.01" y2="18" />
                </svg>
              </n-button>
            </template>
            <ChatHistory
              :conversations="conversations"
              :active-id="activeConversationId"
              :loading="loadingSessions"
              @select="onHistorySelect"
              @delete="onDeleteConversation"
            />
          </n-popover>
          <n-button
            quaternary
            size="tiny"
            class="desk-chat-icon-btn"
            aria-label="新对话"
            :disabled="streaming"
            @click="startNewConversation"
          >
            <svg
              class="desk-chat-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
          </n-button>
        </div>
      </header>

      <div ref="listEl" class="desk-chat-log" aria-live="polite">
        <div v-if="loadingSessions" class="desk-chat-bubble assistant">
          <p>翻你那些黑历史呢…别紧张。</p>
        </div>
        <div v-else-if="!messages.length" class="desk-chat-bubble assistant">
          <p>才没有在等你。有事就说，别磨叽。</p>
        </div>
        <template v-else>
          <div
            v-for="(m, i) in messages"
            :key="i"
            class="desk-chat-bubble"
            :class="[m.role, { typing: isTypingBubble(i) }]"
          >
            <p v-if="m.content">{{ m.content }}</p>
            <p
              v-else-if="isTypingBubble(i)"
              class="desk-chat-typing"
              aria-label="输入中"
            >
              <span class="desk-chat-typing-label">输入中</span>
              <span class="desk-chat-typing-dots" aria-hidden="true">
                <span></span><span></span><span></span>
              </span>
            </p>
          </div>
        </template>
      </div>

      <p v-if="error" class="desk-chat-error" role="alert">{{ error }}</p>

      <form class="desk-chat-form" @submit.prevent="onSubmit">
        <n-input
          v-model:value="draft"
          type="text"
          maxlength="4000"
          placeholder="有话快说，我听着呢…"
          :disabled="streaming || loadingSessions || !activeConversationId"
          autocomplete="off"
          @focus="focused = true"
          @blur="focused = false"
          @keyup.enter="onSubmit"
        />
        <n-button v-if="streaming" quaternary size="small" @click="onStop">
          先停
        </n-button>
        <n-button
          type="primary"
          attr-type="submit"
          size="small"
          :disabled="
            streaming ||
            loadingSessions ||
            !draft.trim() ||
            !activeConversationId
          "
        >
          说
        </n-button>
      </form>
    </div>
  </div>
</template>
