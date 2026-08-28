<script setup>
import { ref, onMounted, computed } from "vue";
import { fetchZhihuHotList } from "../api/zhihu";

const items = ref([]);
const loading = ref(false);
const error = ref("");
const updatedAt = ref(null);

const visibleItems = computed(() => items.value.slice(0, 20));

const updatedText = computed(() => {
  if (!updatedAt.value) return "";
  const d = new Date(updatedAt.value);
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  return `${hh}:${mm}`;
});

async function load() {
  if (loading.value) return;
  loading.value = true;
  error.value = "";
  try {
    const data = await fetchZhihuHotList();
    items.value = data.items || [];
    updatedAt.value = data.updatedAt || new Date().toISOString();
  } catch (e) {
    error.value = e.message || "加载失败";
  } finally {
    loading.value = false;
  }
}

onMounted(() => load());
</script>

<template>
  <div class="zhihu-mini">
    <header class="zhihu-mini-head">
      <div class="zhihu-mini-brand">
        <span class="zhihu-mini-logo" aria-hidden="true">知</span>
        <div class="zhihu-mini-title-wrap">
          <strong class="zhihu-mini-title">知乎热榜</strong>
          <span v-if="updatedText" class="zhihu-mini-update">{{ updatedText }} 更新</span>
        </div>
      </div>
      <button
        type="button"
        class="zhihu-mini-refresh"
        :disabled="loading"
        aria-label="刷新"
        @click="load"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M23 4v6h-6M1 20v-6h6" />
          <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
        </svg>
      </button>
    </header>

    <div v-if="error" class="zhihu-mini-error">
      <span>没捞到热榜</span>
      <button type="button" @click="load">重试</button>
    </div>

    <div v-else-if="loading && items.length === 0" class="zhihu-mini-loading">
      <span>捞热榜中…</span>
    </div>

    <ul v-else class="zhihu-mini-list">
      <li
        v-for="(item, index) in visibleItems"
        :key="item.id || index"
        class="zhihu-mini-item"
        :class="{ top: index < 3 }"
      >
        <a
          :href="item.url"
          target="_blank"
          rel="noopener noreferrer"
          class="zhihu-mini-link"
        >
          <span class="zhihu-mini-rank">{{ index + 1 }}</span>
          <span class="zhihu-mini-text">{{ item.title }}</span>
          <span v-if="index < 3" class="zhihu-mini-flame" aria-hidden="true">🔥</span>
        </a>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.zhihu-mini {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
}

.zhihu-mini-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.6rem;
  flex-shrink: 0;
  margin-bottom: 0.5rem;
}

.zhihu-mini-brand {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  min-width: 0;
}

.zhihu-mini-logo {
  display: grid;
  place-items: center;
  width: 1.9rem;
  height: 1.9rem;
  border-radius: 6px;
  background: #0066ff;
  color: #fff;
  font-size: 1.1rem;
  font-weight: 600;
  line-height: 1;
  flex-shrink: 0;
}

.zhihu-mini-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 0.05rem;
  min-width: 0;
}

.zhihu-mini-title {
  font-size: 1rem;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: 0.02em;
}

.zhihu-mini-update {
  font-size: 0.65rem;
  color: var(--silt);
}

.zhihu-mini-refresh {
  display: grid;
  place-items: center;
  width: 1.6rem;
  height: 1.6rem;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: transparent;
  color: var(--silt);
  cursor: pointer;
  flex-shrink: 0;
  transition:
    color 0.15s ease,
    border-color 0.15s ease;
}

.zhihu-mini-refresh:hover {
  color: var(--koi);
  border-color: rgba(255, 106, 69, 0.45);
}

.zhihu-mini-refresh:disabled {
  opacity: 0.5;
  cursor: wait;
}

.zhihu-mini-refresh svg {
  width: 0.8rem;
  height: 0.8rem;
}

.zhihu-mini-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.08rem;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  padding-right: 0.2rem;
}

.zhihu-mini-item {
  border-radius: 6px;
  transition: background-color 0.12s ease;
}

.zhihu-mini-item:hover {
  background: rgba(255, 255, 255, 0.04);
}

.zhihu-mini-link {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.22rem 0.3rem;
  color: inherit;
  text-decoration: none;
  min-width: 0;
}

.zhihu-mini-rank {
  width: 1.1rem;
  flex-shrink: 0;
  text-align: center;
  font-family: "IBM Plex Mono", ui-monospace, monospace;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--silt);
  line-height: 1;
}

.zhihu-mini-item.top .zhihu-mini-rank {
  color: var(--koi);
}

.zhihu-mini-text {
  flex: 1;
  min-width: 0;
  font-size: 0.8rem;
  line-height: 1.35;
  color: var(--ink);
  white-space: normal;
  word-break: break-word;
}

.zhihu-mini-flame {
  flex-shrink: 0;
  font-size: 0.72rem;
  filter: grayscale(0.2);
}

.zhihu-mini-loading,
.zhihu-mini-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  flex: 1;
  color: var(--silt);
  font-size: 0.78rem;
  text-align: center;
}

.zhihu-mini-error {
  color: var(--koi);
}

.zhihu-mini-error button {
  padding: 0.2rem 0.55rem;
  font-size: 0.7rem;
  border-radius: 6px;
  background: transparent;
  color: var(--koi);
  border: 1px solid rgba(255, 106, 69, 0.35);
  cursor: pointer;
}

.zhihu-mini-error button:hover {
  background: rgba(255, 106, 69, 0.12);
}
</style>
