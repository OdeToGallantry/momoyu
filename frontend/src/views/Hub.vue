<script setup>
import { ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import DeskChat from "../components/DeskChat.vue";
import KanbanGirl from "../components/KanbanGirl.vue";
import ZhihuMini from "../components/ZhihuMini.vue";
import { useToast } from "../composables/useToast";
import { currentUser, logout } from "../auth";

const router = useRouter();
const user = currentUser;
const toast = useToast();
const girlMood = ref("idle");

const dives = [
  {
    id: "eat",
    title: "今天吃什么",
    blurb: "纠结就抽一道",
    hint: "进池子",
    size: "wide",
    to: "/eat",
  },
  {
    id: "zhihu",
    title: "知乎热榜",
    blurb: "看看热闹",
    hint: "实时",
    size: "tall",
    live: true,
  },
  {
    id: "gomoku",
    title: "五子棋",
    blurb: "来一局人机对战",
    hint: "开玩",
    size: "chip",
    to: "/gomoku",
  },
  {
    id: "chinese-chess",
    title: "中国象棋",
    blurb: "来一局楚河汉界",
    hint: "开战",
    size: "chip",
    to: "/chinese-chess",
  },
];

function onWaiting(title) {
  toast(`「${title}」还在捞，晚点再来。`);
}

function onLogout() {
  logout();
  router.replace({ name: "login" });
}
</script>

<template>
  <section class="desk">
    <header class="desk-head">
      <div>
        <h1 class="brand-hero desk-title">摸摸鱼</h1>
        <p class="desk-sub">今天摸哪条</p>
      </div>
      <div class="mast-user">
        <span
          >{{ user?.username }} ·
          {{ user?.role === "ADMIN" ? "管理员" : "只读" }}</span
        >
        <n-button quaternary size="small" @click="onLogout">退出</n-button>
      </div>
    </header>

    <nav class="pond-grid" aria-label="摸鱼入口">
      <div class="pad ai hero chat-pad companion-pad" aria-label="小克">
        <KanbanGirl :mood="girlMood" />
        <DeskChat @mood-change="girlMood = $event" />
      </div>

      <template v-for="item in dives" :key="item.id">
        <div
          v-if="item.live"
          class="pad zhihu-live"
          :class="[item.id, item.size]"
        >
          <ZhihuMini />
        </div>
        <RouterLink
          v-else-if="item.to"
          class="pad"
          :class="[item.id, item.size]"
          :to="item.to"
        >
          <span class="pad-mark" aria-hidden="true" />
          <strong>{{ item.title }}</strong>
          <span>{{ item.blurb }}</span>
          <em>{{ item.hint }}</em>
        </RouterLink>
        <a
          v-else-if="item.href"
          class="pad"
          :class="[item.id, item.size]"
          :href="item.href"
          target="_blank"
          rel="noopener noreferrer"
        >
          <span class="pad-mark" aria-hidden="true" />
          <strong>{{ item.title }}</strong>
          <span>{{ item.blurb }}</span>
          <em>{{ item.hint }}</em>
        </a>
        <button
          v-else
          type="button"
          class="pad waiting"
          :class="[item.id, item.size]"
          @click="onWaiting(item.title)"
        >
          <span class="pad-mark" aria-hidden="true" />
          <strong>{{ item.title }}</strong>
          <span>{{ item.blurb }}</span>
          <em>{{ item.hint }}</em>
        </button>
      </template>
    </nav>
  </section>
</template>
