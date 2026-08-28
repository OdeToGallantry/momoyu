<script setup>
import { computed, reactive, ref, watch } from "vue";

const props = defineProps({
  initial: {
    type: Object,
    default: null,
  },
  saving: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["submit", "cancel"]);

import { inferTaste } from "../taste";

const form = reactive({
  name: "",
  note: "",
  tags: [],
  favorite: false,
  spice: 1,
  salt: 2,
  light: 2,
});

const tagDraft = ref("");
const tagInput = ref(null);

watch(
  () => props.initial,
  (value) => {
    form.name = value?.name ?? "";
    form.note = value?.note ?? "";
    form.tags = splitTags(value?.tags ?? "");
    form.favorite = Boolean(value?.favorite);
    const inferred = inferTaste({
      name: value?.name ?? "",
      note: value?.note ?? "",
      tags: value?.tags ?? "",
    });
    const stored = (value?.spice ?? 0) + (value?.salt ?? 0) + (value?.light ?? 0);
    form.spice = stored ? value.spice : inferred.spice;
    form.salt = stored ? value.salt : inferred.salt;
    form.light = stored ? value.light : inferred.light;
    tagDraft.value = "";
  },
  { immediate: true },
);

const isEdit = computed(() => Boolean(props.initial?.id));

function splitTags(raw) {
  return raw
    .split(/[,，、]/)
    .map((t) => t.trim())
    .filter(Boolean);
}

function addTag() {
  const next = tagDraft.value.trim();
  if (!next) return;
  if (!form.tags.includes(next)) form.tags.push(next);
  tagDraft.value = "";
}

function onTagKey(event) {
  if (event.key === "Enter" || event.key === "," || event.key === "，") {
    event.preventDefault();
    addTag();
  }
  if (event.key === "Backspace" && !tagDraft.value && form.tags.length) {
    form.tags.pop();
  }
}

function removeTag(tag) {
  form.tags = form.tags.filter((t) => t !== tag);
}

function onSubmit() {
  addTag();
  emit("submit", {
    name: form.name.trim(),
    note: form.note.trim(),
    tags: form.tags.join(","),
    favorite: form.favorite,
    spice: form.spice,
    salt: form.salt,
    light: form.light,
  });
}
</script>

<template>
  <form class="kitchen-slip" @submit.prevent="onSubmit">
    <header class="slip-head">
      <div>
        <p class="form-kicker">{{ isEdit ? "改菜单" : "点菜单" }}</p>
        <h2>{{ isEdit ? "改这一道" : "加一道菜" }}</h2>
      </div>
      <button
        type="button"
        class="fav-stamp"
        :class="{ on: form.favorite }"
        @click="form.favorite = !form.favorite"
      >
        {{ form.favorite ? "常吃" : "标常吃" }}
      </button>
    </header>

    <label class="field">
      <span>菜名</span>
      <input v-model="form.name" required maxlength="80" placeholder="黄焖鸡、麻辣烫、那家拉面…" />
    </label>

    <label class="field">
      <span>备注</span>
      <textarea v-model="form.note" rows="3" placeholder="偏辣、要去冰、只在周三开…" />
    </label>

    <div class="field">
      <span>标签</span>
      <div class="chip-box" @click="tagInput?.focus()">
        <button
          v-for="tag in form.tags"
          :key="tag"
          type="button"
          class="chip"
          @click.stop="removeTag(tag)"
        >
          {{ tag }} ×
        </button>
        <input
          ref="tagInput"
          v-model="tagDraft"
          placeholder="回车或逗号添加"
          @keydown="onTagKey"
          @blur="addTag"
        />
      </div>
    </div>

    <fieldset class="taste-sliders">
      <legend>口味三维</legend>
      <label v-for="axis in [
        { key: 'spice', name: '辣' },
        { key: 'salt', name: '咸' },
        { key: 'light', name: '清淡' },
      ]" :key="axis.key">
        <span>{{ axis.name }} {{ form[axis.key] }}</span>
        <input v-model.number="form[axis.key]" type="range" min="0" max="5" step="1" />
      </label>
    </fieldset>

    <div class="actions">
      <button type="button" class="ghost" :disabled="saving" @click="emit('cancel')">
        先不点
      </button>
      <button type="submit" :disabled="saving">
        {{ saving ? "入册中…" : isEdit ? "改好了" : "写入菜单" }}
      </button>
    </div>
  </form>
</template>
