<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { listDishes } from "../api/dishes";
import { AXES, averageTaste, tasteOf } from "../taste";

const dishes = ref([]);
const loading = ref(false);
const error = ref("");
const selectedId = ref(null);

const avg = computed(() => averageTaste(dishes.value));
const selected = computed(() => dishes.value.find((d) => d.id === selectedId.value) ?? null);
const overlay = computed(() => (selected.value ? tasteOf(selected.value) : null));

const cx = 160;
const cy = 168;
const r = 108;
const angles = [-90, 30, 150].map((deg) => (deg * Math.PI) / 180);

function point(index, value, max = 5) {
  const t = (value ?? 0) / max;
  return [
    cx + Math.cos(angles[index]) * r * t,
    cy + Math.sin(angles[index]) * r * t,
  ];
}

function polygon(scores) {
  return AXES.map((axis, i) => point(i, scores[axis.key]).join(",")).join(" ");
}

const grid = [1, 2, 3, 4, 5].map((level) =>
  AXES.map((_, i) => point(i, level).join(",")).join(" "),
);

const axisEnds = AXES.map((_, i) => point(i, 5));
const labels = AXES.map((axis, i) => {
  const [x, y] = point(i, 5.55);
  return { ...axis, x, y };
});

onMounted(async () => {
  loading.value = true;
  try {
    const result = await listDishes({ page: 0, size: 100 });
    dishes.value = result.content ?? [];
  } catch (e) {
    error.value = e.message || "加载失败";
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <section class="radar-page">
    <header class="radar-head">
      <RouterLink class="back" to="/eat">回抽菜</RouterLink>
      <p class="form-kicker">口味雷达</p>
      <h1>今晚的味型</h1>
      <p class="count">辣 / 咸 / 清淡，看整本菜单偏哪一口</p>
    </header>

    <p v-if="error" class="error">{{ error }}</p>

    <div v-else class="radar-stage">
      <div class="radar-col">
        <div class="radar-plate">
          <svg viewBox="0 0 320 320" role="img" aria-label="口味雷达">
            <polygon
              v-for="(ring, i) in grid"
              :key="i"
              :points="ring"
              class="grid-ring"
            />
            <line
              v-for="(end, i) in axisEnds"
              :key="'a' + i"
              :x1="cx"
              :y1="cy"
              :x2="end[0]"
              :y2="end[1]"
              class="grid-axis"
            />
            <polygon :points="polygon(avg)" class="avg-fill" />
            <polygon
              v-if="overlay"
              :points="polygon(overlay)"
              class="pick-fill"
            />
            <text
              v-for="label in labels"
              :key="label.key"
              :x="label.x"
              :y="label.y"
              class="axis-label"
              text-anchor="middle"
              dominant-baseline="middle"
            >
              {{ label.label }}
            </text>
          </svg>
        </div>
        <p class="radar-caption">
          <template v-if="!dishes.length">菜单空着，雷达也空</template>
          <template v-else-if="selected">对照「{{ selected.name }}」与整本均值</template>
          <template v-else>潮水是整本均值，点右侧菜名叠一层锦鲤色</template>
        </p>
      </div>

      <ul class="radar-list">
        <li v-if="loading" class="empty">在称味…</li>
        <li v-else-if="!dishes.length" class="empty">
          <strong>还没有菜</strong>
          <span>先回抽菜加几道，雷达才转得起来。</span>
        </li>
        <li v-for="item in dishes" :key="item.id">
          <button
            type="button"
            class="taste-row"
            :class="{ on: selectedId === item.id }"
            @click="selectedId = selectedId === item.id ? null : item.id"
          >
            <strong>{{ item.name }}</strong>
            <span>
              辣 {{ tasteOf(item).spice }} · 咸 {{ tasteOf(item).salt }} · 清淡
              {{ tasteOf(item).light }}
            </span>
          </button>
        </li>
      </ul>
    </div>
  </section>
</template>
