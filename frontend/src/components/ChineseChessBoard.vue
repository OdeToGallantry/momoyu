<script setup>
import { computed, ref, watch } from "vue";

const props = defineProps({
  board: { type: Array, required: true },
  selected: { type: Object, default: null },
  legalMoves: { type: Array, default: () => [] },
  lastMove: { type: Object, default: null },
  checkedSide: { type: Boolean, default: null },
  disabled: { type: Boolean, default: false },
});

const emit = defineEmits(["cell-click"]);

const pieceNames = {
  r: "車", n: "马", b: "象", a: "士", k: "将", c: "炮", p: "卒",
  R: "俥", N: "傌", B: "相", A: "仕", K: "帅", C: "炮", P: "兵",
};

const rows = computed(() => props.board.length);
const cols = computed(() => props.board[0]?.length || 0);
const rowRange = computed(() => Array.from({ length: rows.value }, (_, i) => i));
const colRange = computed(() => Array.from({ length: cols.value }, (_, i) => i));

const animatingMove = ref(null);
let clearAnimTimer = null;

watch(
  () => props.lastMove,
  (move) => {
    if (clearAnimTimer) {
      clearTimeout(clearAnimTimer);
      clearAnimTimer = null;
    }
    if (!move) {
      animatingMove.value = null;
      return;
    }
    animatingMove.value = {
      ...move,
      piece: props.board[move.toR][move.toC],
    };
    clearAnimTimer = setTimeout(() => {
      animatingMove.value = null;
      clearAnimTimer = null;
    }, 420);
  },
  { immediate: true },
);

const checkedKingPos = computed(() => {
  if (props.checkedSide === null || props.checkedSide === undefined) return null;
  const target = props.checkedSide ? "K" : "k";
  for (let r = 0; r < rows.value; r++) {
    for (let c = 0; c < cols.value; c++) {
      if (props.board[r][c] === target) return { r, c };
    }
  }
  return null;
});

function pieceClass(piece) {
  return {
    "piece-red": piece === piece.toUpperCase(),
    "piece-black": piece === piece.toLowerCase(),
  };
}

function isLegalMove(r, c) {
  return props.legalMoves.some(([lr, lc]) => lr === r && lc === c);
}

function isCheckedKing(r, c) {
  return checkedKingPos.value?.r === r && checkedKingPos.value?.c === c;
}

function onCellClick(r, c) {
  if (props.disabled) return;
  emit("cell-click", r, c);
}

function intersectionMark(r, c) {
  // 在棋盘上需要画标记的交叉点：兵/卒位置、炮位置
  const markRows = [0, 2, 3, 4, 5, 6, 7, 9];
  const markCols = [0, 2, 4, 6, 8];
  if (!markRows.includes(r)) return false;
  if (!markCols.includes(c)) return false;
  // 不画九宫格外斜线交叉点
  if ((r === 1 || r === 8) && (c === 1 || c === 7)) return false;
  return true;
}
</script>

<template>
  <div class="chinese-chess-board">
    <div class="cc-grid" :style="{ '--rows': rows, '--cols': cols }">
      <div
        v-if="animatingMove"
        class="cc-move-ghost"
        :style="{
          '--from-r': animatingMove.fromR,
          '--from-c': animatingMove.fromC,
          '--to-r': animatingMove.toR,
          '--to-c': animatingMove.toC,
        }"
      >
        <span class="cc-piece" :class="pieceClass(animatingMove.piece)">
          {{ pieceNames[animatingMove.piece] }}
        </span>
      </div>

      <div
        v-for="r in rowRange"
        :key="`row-${r}`"
        class="cc-row"
      >
        <button
          v-for="c in colRange"
          :key="`cell-${r}-${c}`"
          type="button"
          class="cc-cell"
          :class="{
            selected: selected?.r === r && selected?.c === c,
            'legal-move': isLegalMove(r, c),
            'inter-mark': intersectionMark(r, c),
            river: r === 4 || r === 5,
          }"
          :disabled="disabled"
          @click="onCellClick(r, c)"
          :aria-label="`第 ${r + 1} 行第 ${c + 1} 列`"
        >
          <span
            v-if="board[r][c] !== ' '"
            class="cc-piece"
            :class="[pieceClass(board[r][c]), { 'king-checked': isCheckedKing(r, c) }]"
          >
            {{ pieceNames[board[r][c]] }}
          </span>
          <span v-else-if="isLegalMove(r, c)" class="cc-dot" />
        </button>
      </div>
    </div>
  </div>
</template>
