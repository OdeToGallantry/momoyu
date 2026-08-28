<script setup>
import { computed } from "vue";

const props = defineProps({
  board: { type: Array, required: true },
  currentPlayer: { type: Number, default: 1 },
  winner: { type: Number, default: 0 },
  lastMove: { type: Object, default: null },
  disabled: { type: Boolean, default: false },
});

const emit = defineEmits(["move"]);

const boardSize = computed(() => props.board.length);
const range = computed(() => Array.from({ length: boardSize.value }, (_, i) => i));

function onCellClick(r, c) {
  if (props.disabled || props.winner !== 0 || props.board[r][c] !== 0) return;
  emit("move", r, c);
}

function cellClass(r, c) {
  const stone = props.board[r][c];
  return {
    "cell-empty": stone === 0,
    "cell-black": stone === 1,
    "cell-white": stone === 2,
    "cell-last": props.lastMove?.row === r && props.lastMove?.col === c,
  };
}
</script>

<template>
  <div class="gomoku-board" :style="{ '--size': boardSize }">
    <div class="grid-lines">
      <div
        v-for="r in range"
        :key="`row-${r}`"
        class="grid-row"
      >
        <button
          v-for="c in range"
          :key="`cell-${r}-${c}`"
          type="button"
          class="cell"
          :class="cellClass(r, c)"
          :disabled="disabled"
          @click="onCellClick(r, c)"
          :aria-label="`第 ${r + 1} 行第 ${c + 1} 列`"
        />
      </div>
    </div>
  </div>
</template>
