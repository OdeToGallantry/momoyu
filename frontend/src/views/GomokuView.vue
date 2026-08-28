<script setup>
import { computed } from "vue";
import GomokuBoard from "../components/GomokuBoard.vue";
import { useGomoku } from "../composables/useGomoku.js";

const {
  board,
  currentPlayer,
  winner,
  history,
  mode,
  difficulty,
  aiThinking,
  isGameOver,
  canUndo,
  placeStone,
  reset,
  undo,
  switchMode,
  switchDifficulty,
  BLACK,
  WHITE,
} = useGomoku();

const lastMove = computed(() => history.value[history.value.length - 1] || null);

const statusText = computed(() => {
  if (winner.value === -1) return "和棋啦～";
  if (winner.value === BLACK) return "黑棋胜利！";
  if (winner.value === WHITE) return "白棋胜利！";
  return currentPlayer.value === BLACK ? "轮到黑棋" : "轮到白棋";
});

const modeText = computed(() => (mode.value === "pvp" ? "双人对战" : "人机对战"));

const difficultyLabels = {
  easy: "简单",
  normal: "普通",
  hard: "困难",
};
</script>

<template>
  <section class="gomoku-page">
    <header class="gomoku-head">
      <div>
        <h1>五子棋</h1>
        <p class="desk-sub">{{ modeText }} · {{ statusText }}</p>
      </div>
      <div class="gomoku-actions">
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

    <div class="gomoku-stage">
      <GomokuBoard
        :board="board"
        :current-player="currentPlayer"
        :winner="winner"
        :last-move="lastMove"
        :disabled="aiThinking"
        @move="placeStone"
      />

      <aside class="gomoku-side">
        <div class="side-card">
          <strong>当前局势</strong>
          <div class="side-row">
            <span class="dot black" />
            <span>黑棋</span>
            <em>{{ currentPlayer === BLACK && !isGameOver ? '执子中' : '' }}</em>
          </div>
          <div class="side-row">
            <span class="dot white" />
            <span>白棋</span>
            <em>{{ currentPlayer === WHITE && !isGameOver ? '执子中' : '' }}</em>
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
