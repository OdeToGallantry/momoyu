<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { RouterLink } from "vue-router";
import DishForm from "../components/DishForm.vue";
import { isAdminUser } from "../auth";
import {
  createDish,
  deleteDish,
  downloadImportTemplate,
  importDishesExcel,
  listDishes,
  randomDish,
  updateDish,
} from "../api/dishes";

const canWrite = isAdminUser;

const dishes = ref([]);
const totalElements = ref(0);
const totalPages = ref(0);
const page = ref(0);
const pageSize = 20;
const query = ref("");
const favoriteOnly = ref(false);
const spinFavoriteOnly = ref(false);
const loading = ref(false);
const saving = ref(false);
const spinning = ref(false);
const error = ref("");
const showForm = ref(false);
const showImport = ref(false);
const importHint = ref("");
const importInput = ref(null);
const importFile = ref(null);
const dropActive = ref(false);
const editing = ref(null);
const picked = ref(null);
const slotName = ref("");
const pendingDelete = ref(null);
const deleteModalOpen = computed({
  get: () => pendingDelete.value != null,
  set: (open) => {
    if (!open) pendingDelete.value = null;
  },
});
let slotTimer = 0;

const countLabel = computed(() => {
  const n = totalElements.value;
  if (n === 0) return "菜单还空着";
  const pages = Math.max(totalPages.value, 1);
  return `${n} 道在册 · 第 ${page.value + 1}/${pages} 页`;
});

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const result = await listDishes({
      q: query.value.trim(),
      favoriteOnly: favoriteOnly.value,
      page: page.value,
      size: pageSize,
    });
    dishes.value = result.content ?? [];
    totalElements.value = result.totalElements ?? 0;
    totalPages.value = result.totalPages ?? 0;
    if (page.value > 0 && dishes.value.length === 0 && totalElements.value > 0) {
      page.value = Math.max(result.totalPages - 1, 0);
      await load();
    }
  } catch (e) {
    error.value = e.message || "加载失败";
  } finally {
    loading.value = false;
  }
}

function search() {
  page.value = 0;
  load();
}

function prevPage() {
  if (page.value <= 0) return;
  page.value -= 1;
  load();
}

function nextPage() {
  if (page.value + 1 >= totalPages.value) return;
  page.value += 1;
  load();
}

function openCreate() {
  editing.value = null;
  showImport.value = false;
  showForm.value = true;
}

function openEdit(item) {
  editing.value = { ...item };
  showImport.value = false;
  showForm.value = true;
}

function closeForm() {
  showForm.value = false;
  editing.value = null;
}

function openImport() {
  showForm.value = false;
  showImport.value = true;
  importHint.value = "";
  importFile.value = null;
}

async function onDownloadTemplate() {
  error.value = "";
  try {
    await downloadImportTemplate();
  } catch (e) {
    error.value = e.message || "下载失败";
  }
}

function closeImport() {
  showImport.value = false;
  importHint.value = "";
  importFile.value = null;
  dropActive.value = false;
  if (importInput.value) importInput.value.value = "";
}

async function onExcelSelected(event) {
  const file = event.target.files?.[0];
  event.target.value = "";
  if (file) await runImport(file);
}

function onDrop(event) {
  event.preventDefault();
  dropActive.value = false;
  const file = event.dataTransfer?.files?.[0];
  if (file) runImport(file);
}

async function runImport(file) {
  importFile.value = file;
  saving.value = true;
  error.value = "";
  importHint.value = "";
  try {
    const result = await importDishesExcel(file);
    importHint.value = `入册 ${result.created} 道，跳过 ${result.skipped} 道`;
    await load();
  } catch (e) {
    error.value = e.message || "导入失败";
    importFile.value = null;
  } finally {
    saving.value = false;
  }
}

async function onSubmit(payload) {
  saving.value = true;
  error.value = "";
  try {
    if (editing.value?.id) {
      await updateDish(editing.value.id, payload);
    } else {
      await createDish(payload);
    }
    closeForm();
    await load();
  } catch (e) {
    error.value = e.message || "保存失败";
  } finally {
    saving.value = false;
  }
}

function askDelete(item) {
  pendingDelete.value = item;
}

async function confirmDelete() {
  const item = pendingDelete.value;
  if (!item) return;
  error.value = "";
  try {
    await deleteDish(item.id);
    if (picked.value?.id === item.id) picked.value = null;
    pendingDelete.value = null;
    await load();
  } catch (e) {
    error.value = e.message || "删除失败";
    return false;
  }
}

async function toggleFavorite(item) {
  error.value = "";
  try {
    await updateDish(item.id, {
      name: item.name,
      note: item.note ?? "",
      tags: item.tags ?? "",
      favorite: !item.favorite,
      spice: item.spice,
      salt: item.salt,
      light: item.light,
    });
    await load();
  } catch (e) {
    error.value = e.message || "更新失败";
  }
}

function startSlot() {
  const pool = dishes.value.map((d) => d.name).filter(Boolean);
  if (pool.length === 0) return;
  let i = 0;
  window.clearInterval(slotTimer);
  slotTimer = window.setInterval(() => {
    slotName.value = pool[i % pool.length];
    i += 1;
  }, 70);
}

function stopSlot() {
  window.clearInterval(slotTimer);
  slotTimer = 0;
}

async function spin() {
  spinning.value = true;
  error.value = "";
  picked.value = null;
  startSlot();
  try {
    const [dish] = await Promise.all([
      randomDish(spinFavoriteOnly.value),
      new Promise((r) => setTimeout(r, 900)),
    ]);
    picked.value = dish;
    slotName.value = dish.name;
  } catch (e) {
    error.value = e.message || "抽取失败";
    slotName.value = "";
  } finally {
    stopSlot();
    spinning.value = false;
  }
}

onMounted(load);
onUnmounted(stopSlot);
</script>

<template>
  <section class="stage">
    <div class="theater">
      <div class="plate" :class="{ spinning }">
        <svg v-if="spinning" class="ripple-svg" viewBox="0 0 100 100" preserveAspectRatio="none">
          <circle class="ripple-circle" cx="50" cy="50" r="10" />
          <circle class="ripple-circle" cx="50" cy="50" r="10" />
          <circle class="ripple-circle" cx="50" cy="50" r="10" />
        </svg>
        <span class="ring outer" />
        <span class="ring inner" />
        <p class="plate-kicker">今天吃什么</p>
        <p class="spin-result">
          <template v-if="spinning">{{ slotName || "挑选中" }}</template>
          <template v-else-if="picked">{{ picked.name }}</template>
          <template v-else>水面还静着</template>
        </p>
        <p v-if="picked?.note && !spinning" class="spin-note">{{ picked.note }}</p>
        <p v-else class="spin-note">{{ spinning ? "石子打漂" : "纠结就丢一颗石子" }}</p>
      </div>

      <div class="spin-actions">
        <label class="check">
          <input v-model="spinFavoriteOnly" type="checkbox" />
          只从常吃里抽
        </label>
        <button type="button" class="primary" :disabled="spinning" @click="spin">
          {{ spinning ? "打漂中…" : "帮我选" }}
        </button>
      </div>
    </div>

    <div class="ledger">
      <div class="ledger-head">
        <div>
          <h1>菜单</h1>
          <p class="count">{{ countLabel }}</p>
          <RouterLink class="radar-link" to="/radar">口味雷达</RouterLink>
        </div>
        <div v-if="canWrite" class="head-actions">
          <button type="button" class="ghost" @click="openImport">导入</button>
          <button type="button" @click="openCreate">加菜</button>
        </div>
      </div>

      <div class="toolbar">
        <input
          v-model="query"
          type="search"
          placeholder="搜菜名或标签，回车"
          @keyup.enter="search"
        />
        <label class="check">
          <input v-model="favoriteOnly" type="checkbox" @change="search" />
          常吃
        </label>
        <button type="button" class="ghost" :disabled="loading" @click="load">
          {{ loading ? "刷新中" : "刷新" }}
        </button>
      </div>

      <p v-if="error" class="error">{{ error }}</p>

      <ul class="list">
        <li v-if="!loading && dishes.length === 0" class="empty">
          <strong>菜单空空</strong>
          <span>先加黄焖鸡或麻辣烫，转盘才转得起来。</span>
        </li>
        <li
          v-for="item in dishes"
          :key="item.id"
          class="ticket"
          :class="{ picked: picked?.id === item.id }"
        >
          <div class="meta">
            <div class="title-row">
              <strong>{{ item.name }}</strong>
              <button
                v-if="canWrite"
                type="button"
                class="star"
                :class="{ on: item.favorite }"
                :title="item.favorite ? '取消常吃' : '标为常吃'"
                @click="toggleFavorite(item)"
              >
                {{ item.favorite ? "★" : "☆" }}
              </button>
              <span v-else class="star" :class="{ on: item.favorite }" aria-hidden="true">
                {{ item.favorite ? "★" : "☆" }}
              </span>
            </div>
            <p v-if="item.note" class="note">{{ item.note }}</p>
            <p v-if="item.tags" class="tags">{{ item.tags }}</p>
          </div>
          <div v-if="canWrite" class="row-actions">
            <button type="button" class="ghost compact" @click="openEdit(item)">改</button>
            <button type="button" class="danger compact" @click="askDelete(item)">删</button>
          </div>
        </li>
      </ul>
      <nav v-if="totalPages > 1" class="pager">
        <button type="button" class="ghost compact" :disabled="page <= 0 || loading" @click="prevPage">
          上一页
        </button>
        <span>{{ page + 1 }} / {{ totalPages }}</span>
        <button
          type="button"
          class="ghost compact"
          :disabled="page + 1 >= totalPages || loading"
          @click="nextPage"
        >
          下一页
        </button>
      </nav>
    </div>
  </section>

  <n-modal
    v-model:show="showForm"
    :mask-closable="!saving"
    transform-origin="center bottom"
    class="dish-sheet-modal"
  >
    <DishForm
      :initial="editing"
      :saving="saving"
      @submit="onSubmit"
      @cancel="closeForm"
    />
  </n-modal>

  <n-modal
    v-model:show="showImport"
    :mask-closable="!saving"
    transform-origin="center bottom"
    class="dish-sheet-modal"
  >
    <div class="kitchen-slip import-slip">
      <header class="slip-head">
        <div>
          <p class="form-kicker">批量入册</p>
          <h2>把菜单倒进来</h2>
        </div>
        <n-button text type="primary" class="template-link" @click="onDownloadTemplate">
          领空白菜单
        </n-button>
      </header>

      <ol class="col-legend">
        <li><em>名称</em>必填</li>
        <li><em>标签</em>可空</li>
        <li><em>备注</em>可空</li>
        <li><em>收藏</em>填 是 / 1</li>
      </ol>

      <label
        class="dropzone"
        :class="{ active: dropActive, busy: saving, done: Boolean(importHint) }"
        @dragenter.prevent="dropActive = true"
        @dragover.prevent="dropActive = true"
        @dragleave.prevent="dropActive = false"
        @drop="onDrop"
      >
        <input
          ref="importInput"
          type="file"
          accept=".xlsx,.xls,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel"
          :disabled="saving"
          @change="onExcelSelected"
        />
        <span class="drop-mark" aria-hidden="true">xls</span>
        <strong v-if="saving">后厨在拆表…</strong>
        <strong v-else-if="importFile">{{ importFile.name }}</strong>
        <strong v-else>把 Excel 拖到这只盘子上</strong>
        <span>{{ saving ? "请稍候" : "或点这里从文件夹挑一份" }}</span>
      </label>

      <p v-if="importHint" class="import-receipt">{{ importHint }}</p>

      <div class="actions">
        <n-button quaternary :disabled="saving" @click="closeImport">
          {{ importHint ? "好了" : "先不导" }}
        </n-button>
      </div>
    </div>
  </n-modal>

  <n-modal
    v-model:show="deleteModalOpen"
    preset="dialog"
    title="出菜单"
    :content="pendingDelete ? `删掉「${pendingDelete.name}」？这道菜会从转盘里消失，不能撤销。` : ''"
    positive-text="删掉"
    negative-text="留着"
    type="warning"
    @positive-click="confirmDelete"
  />
</template>
