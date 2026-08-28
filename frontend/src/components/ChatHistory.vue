<script setup>
import { computed } from "vue";

const props = defineProps({
  conversations: { type: Array, default: () => [] },
  activeId: { type: Number, default: null },
  loading: { type: Boolean, default: false },
});

const emit = defineEmits(["select", "delete"]);

function dayKey(iso) {
  const d = new Date(iso);
  const now = new Date();
  const startOfToday = new Date(
    now.getFullYear(),
    now.getMonth(),
    now.getDate(),
  );
  const startOfDate = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  const diffDays = Math.floor((startOfToday - startOfDate) / 86400000);
  if (diffDays === 0) return "today";
  if (diffDays === 1) return "yesterday";
  return "older";
}

const groups = computed(() => {
  const buckets = {
    today: { label: "今天", items: [] },
    yesterday: { label: "昨天", items: [] },
    older: { label: "更早", items: [] },
  };
  for (const item of props.conversations) {
    buckets[dayKey(item.updatedAt)].items.push(item);
  }
  return Object.values(buckets).filter((g) => g.items.length);
});

function onDelete(event, id) {
  event.stopPropagation();
  emit("delete", id);
}
</script>

<template>
  <div class="chat-history" @click.stop>
    <div v-if="loading && !conversations.length" class="chat-history-empty">
      加载中…
    </div>
    <div v-else-if="!conversations.length" class="chat-history-empty">
      还没有记录
    </div>

    <n-scrollbar v-else class="chat-history-scroll">
      <div class="chat-history-groups">
        <section v-for="group in groups" :key="group.label">
          <h3>{{ group.label }}</h3>
          <ul>
            <li v-for="item in group.items" :key="item.id">
              <button
                type="button"
                class="chat-history-item"
                :class="{ active: item.id === activeId }"
                @click="emit('select', item.id)"
              >
                <span class="chat-history-title">{{ item.title }}</span>
                <button
                  type="button"
                  class="chat-history-delete"
                  aria-label="删除"
                  @click="onDelete($event, item.id)"
                >
                  <svg
                    class="chat-history-delete-icon"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    aria-hidden="true"
                  >
                    <polyline points="3 6 5 6 21 6" />
                    <path
                      d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"
                    />
                    <path d="M10 11v6" />
                    <path d="M14 11v6" />
                    <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
                  </svg>
                </button>
              </button>
            </li>
          </ul>
        </section>
      </div>
    </n-scrollbar>
  </div>
</template>
