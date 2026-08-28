<script setup>
const models = [
  {
    id: "gpt",
    name: "ChatGPT",
    blurb: "什么都先问它",
    href: "https://chatgpt.com",
  },
  {
    id: "claude",
    name: "Claude",
    blurb: "长文和代码",
    href: "https://claude.ai",
  },
  {
    id: "deepseek",
    name: "DeepSeek",
    blurb: "国内也能开",
    href: "https://chat.deepseek.com",
  },
];
</script>

<template>
  <section class="ai-pond">
    <h1>丢给模型</h1>
    <p class="desk-sub">新标签打开。聊完再游回来。</p>

    <nav class="model-grid" aria-label="AI 入口">
      <a
        v-for="item in models"
        :key="item.id"
        class="pad"
        :class="item.id"
        :href="item.href"
        target="_blank"
        rel="noopener noreferrer"
      >
        <span class="pad-mark" aria-hidden="true" />
        <strong>{{ item.name }}</strong>
        <span>{{ item.blurb }}</span>
        <em>新标签</em>
      </a>
    </nav>
  </section>
</template>
