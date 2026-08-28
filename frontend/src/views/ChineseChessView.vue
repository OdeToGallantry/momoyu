<script setup>
import { computed, watch } from "vue";
import ChineseChessBoard from "../components/ChineseChessBoard.vue";
import { useChineseChess } from "../composables/useChineseChess.js";
import { useToast } from "../composables/useToast.js";

const toast = useToast();
const {
  board,
  currentSide,
  winner,
  mode,
  difficulty,
  aiThinking,
  selected,
  lastMove,
  checkedSide,
  isGameOver,
  canUndo,
  legalMovesForSelected,
  handleCellClick,
  reset,
  undo,
  switchMode,
  switchDifficulty,
} = useChineseChess();

const sideText = computed(() => (currentSide.value ? "红方" : "黑方"));
const modeText = computed(() => (mode.value === "pvp" ? "双人对战" : "人机对战"));

const statusText = computed(() => {
  if (winner.value === "red") return "红方胜利！";
  if (winner.value === "black") return "黑方胜利！";
  if (winner.value === "draw") return "和棋！";
  if (checkedSide.value !== null) return `${checkedSide.value ? "红方" : "黑方"}被将军！`;
  return `轮到${sideText.value}`;
});

watch(checkedSide, (side, prevSide) => {
  if (side !== null && side !== prevSide) {
    toast(`⚠️ ${side ? "红方" : "黑方"}被将军！必须立即解将`, "error");
  }
});

const difficultyLabels = {
  easy: "简单",
  normal: "普通",
  hard: "困难",
};
</script>

<template>
  <section class="chinese-chess-page">
    <header class="cc-head">
      <div>
        <h1>中国象棋</h1>
        <p
          class="desk-sub"
          :class="{ 'check-warning': checkedSide !== null && !isGameOver }"
        >
          {{ modeText }} · {{ statusText }}
        </p>
      </div>
      <div class="cc-actions">
        <button
          class="ghost compact"
          :class="{ on: mode === 'pvp' }"
          @click="switchMode('pvp')"
        >
          双人
        </button>
        <button
          class="ghost compact"
          :class="{ on: mode === 'pve' }"
          @click="switchMode('pve')"
        >
          人机
        </button>
        <template v-if="mode === 'pve'">
          <button
            v-for="lvl in ['easy', 'normal', 'hard']"
            :key="lvl"
            class="ghost compact"
            :class="{ on: difficulty === lvl }"
            @click="switchDifficulty(lvl)"
          >
            {{ difficultyLabels[lvl] }}
          </button>
        </template>
        <button class="ghost compact" :disabled="!canUndo" @click="undo">
          悔棋
        </button>
        <button class="primary compact" @click="reset">
          重置
        </button>
      </div>
    </header>

    <div class="cc-stage">
      <ChineseChessBoard
        :board="board"
        :selected="selected"
        :legal-moves="legalMovesForSelected"
        :last-move="lastMove"
        :checked-side="checkedSide"
        :disabled="aiThinking"
        @cell-click="handleCellClick"
      />

      <aside class="cc-side">
        <div v-if="checkedSide !== null && !isGameOver" class="side-card check-alert">
          <strong>⚠️ 将军！</strong>
          <p>{{ checkedSide ? "红方" : "黑方" }}被将军，必须立即解将</p>
        </div>

        <div class="side-card">
          <strong>当前局势</strong>
          <div class="side-row">
            <span class="dot red" />
            <span>红方</span>
            <em>{{ currentSide && !isGameOver ? '执子中' : '' }}</em>
          </div>
          <div class="side-row">
            <span class="dot black" />
            <span>黑方</span>
            <em>{{ !currentSide && !isGameOver ? '执子中' : '' }}</em>
          </div>
          <p v-if="aiThinking" class="thinking">电脑思考中……</p>
        </div>

        <div v-if="isGameOver" class="side-card result">
          <strong>{{ statusText }}</strong>
          <button class="primary" @click="reset">再来一局</button>
        </div>
      </aside>
    </div>
  </section>
</template>
