import { ref, computed } from "vue";

const BOARD_SIZE = 15;
const EMPTY = 0;
const BLACK = 1;
const WHITE = 2;

const DIRECTIONS = [
  [0, 1], // 横向
  [1, 0], // 纵向
  [1, 1], // 主对角线
  [1, -1], // 副对角线
];

const SCORES = {
  five: 100000,
  openFour: 10000,
  closedFour: 1000,
  openThree: 1000,
  openTwo: 100,
  closedTwo: 10,
};

function createBoard() {
  return Array.from({ length: BOARD_SIZE }, () =>
    Array.from({ length: BOARD_SIZE }, () => EMPTY),
  );
}

function inBounds(r, c) {
  return r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE;
}

function checkWin(board, row, col, player) {
  for (const [dx, dy] of DIRECTIONS) {
    let count = 1;

    for (let i = 1; i < 5; i++) {
      const r = row + dx * i;
      const c = col + dy * i;
      if (inBounds(r, c) && board[r][c] === player) count++;
      else break;
    }

    for (let i = 1; i < 5; i++) {
      const r = row - dx * i;
      const c = col - dy * i;
      if (inBounds(r, c) && board[r][c] === player) count++;
      else break;
    }

    if (count >= 5) return true;
  }
  return false;
}

function getCandidateMoves(board, range = 2) {
  const moves = [];
  for (let r = 0; r < BOARD_SIZE; r++) {
    for (let c = 0; c < BOARD_SIZE; c++) {
      if (board[r][c] !== EMPTY) continue;
      let near = false;
      for (let dr = -range; dr <= range && !near; dr++) {
        for (let dc = -range; dc <= range && !near; dc++) {
          const nr = r + dr;
          const nc = c + dc;
          if (inBounds(nr, nc) && board[nr][nc] !== EMPTY) {
            near = true;
          }
        }
      }
      if (near) moves.push([r, c]);
    }
  }
  return moves.length ? moves : [[7, 7]];
}

function evaluateLine(count, openEnds) {
  if (count >= 5) return SCORES.five;
  if (count === 4)
    return openEnds === 2
      ? SCORES.openFour
      : openEnds === 1
        ? SCORES.closedFour
        : 0;
  if (count === 3)
    return openEnds === 2
      ? SCORES.openThree
      : openEnds === 1
        ? SCORES.closedTwo * 2
        : 0;
  if (count === 2)
    return openEnds === 2
      ? SCORES.openTwo
      : openEnds === 1
        ? SCORES.closedTwo
        : 0;
  return 0;
}

function evaluatePoint(board, r, c, player) {
  if (board[r][c] !== EMPTY) return -Infinity;

  let attack = 0;
  board[r][c] = player;
  for (const [dx, dy] of DIRECTIONS) {
    let count = 1;
    let openEnds = 0;

    for (let i = 1; i < 5; i++) {
      const rr = r + dx * i;
      const cc = c + dy * i;
      if (inBounds(rr, cc) && board[rr][cc] === player) count++;
      else {
        if (inBounds(rr, cc) && board[rr][cc] === EMPTY) openEnds++;
        break;
      }
    }

    for (let i = 1; i < 5; i++) {
      const rr = r - dx * i;
      const cc = c - dy * i;
      if (inBounds(rr, cc) && board[rr][cc] === player) count++;
      else {
        if (inBounds(rr, cc) && board[rr][cc] === EMPTY) openEnds++;
        break;
      }
    }

    attack += evaluateLine(count, openEnds);
  }
  board[r][c] = EMPTY;

  const opponent = player === BLACK ? WHITE : BLACK;
  let defense = 0;
  board[r][c] = opponent;
  for (const [dx, dy] of DIRECTIONS) {
    let count = 1;
    let openEnds = 0;

    for (let i = 1; i < 5; i++) {
      const rr = r + dx * i;
      const cc = c + dy * i;
      if (inBounds(rr, cc) && board[rr][cc] === opponent) count++;
      else {
        if (inBounds(rr, cc) && board[rr][cc] === EMPTY) openEnds++;
        break;
      }
    }

    for (let i = 1; i < 5; i++) {
      const rr = r - dx * i;
      const cc = c - dy * i;
      if (inBounds(rr, cc) && board[rr][cc] === opponent) count++;
      else {
        if (inBounds(rr, cc) && board[rr][cc] === EMPTY) openEnds++;
        break;
      }
    }

    defense += evaluateLine(count, openEnds);
  }
  board[r][c] = EMPTY;

  const centerBonus = 20 - (Math.abs(r - 7) + Math.abs(c - 7));
  return attack * 1.1 + defense * 1.0 + Math.max(0, centerBonus);
}

function pickRandom(moves) {
  const idx = Math.floor(Math.random() * moves.length);
  return [moves[idx].r, moves[idx].c];
}

function weightedPick(moves) {
  const weights = moves.map((m) => Math.max(1, m.score));
  const total = weights.reduce((sum, w) => sum + w, 0);
  let threshold = Math.random() * total;
  for (let i = 0; i < moves.length; i++) {
    threshold -= weights[i];
    if (threshold <= 0) return [moves[i].r, moves[i].c];
  }
  return [moves[0].r, moves[0].c];
}

function aiMove(board, aiPlayer, difficulty = "normal") {
  const moves = getCandidateMoves(board);
  const opponent = aiPlayer === BLACK ? WHITE : BLACK;

  // 1. 自己下一步能赢，直接落子（任何难度都不放水，否则太蠢）
  for (const [r, c] of moves) {
    board[r][c] = aiPlayer;
    const win = checkWin(board, r, c, aiPlayer);
    board[r][c] = EMPTY;
    if (win) return [r, c];
  }

  // 2. 对手下一步能赢，必须防守。简单模式也会防守
  if (difficulty !== "easy" || Math.random() < 0.85) {
    for (const [r, c] of moves) {
      board[r][c] = opponent;
      const win = checkWin(board, r, c, opponent);
      board[r][c] = EMPTY;
      if (win) return [r, c];
    }
  }

  // 3. 评估所有候选点
  const scoredMoves = moves.map(([r, c]) => ({
    r,
    c,
    score: evaluatePoint(board, r, c, aiPlayer),
  }));
  scoredMoves.sort((a, b) => b.score - a.score);

  // 4. 根据难度从高分候选里选
  if (difficulty === "easy") {
    // 前 5 名里随机，有基本防守，但进攻不精准
    const topN = Math.min(5, scoredMoves.length);
    const top = scoredMoves.slice(0, topN);
    return pickRandom(top);
  }

  if (difficulty === "normal") {
    // 70% 选最高分，30% 从前 3 名里按分数加权随机
    if (Math.random() < 0.6) {
      return [scoredMoves[0].r, scoredMoves[0].c];
    }
    const topN = Math.min(4, scoredMoves.length);
    const top = scoredMoves.slice(0, topN);
    return weightedPick(top);
  }

  // hard：选最高分
  return [scoredMoves[0].r, scoredMoves[0].c];
}

export function useGomoku() {
  const board = ref(createBoard());
  const currentPlayer = ref(BLACK);
  const winner = ref(EMPTY);
  const history = ref([]);
  const mode = ref("pvp"); // 'pvp' | 'pve'
  const difficulty = ref("normal"); // 'easy' | 'normal' | 'hard'
  const aiThinking = ref(false);

  const isGameOver = computed(() => winner.value !== EMPTY);
  const canUndo = computed(() => history.value.length > 0 && !aiThinking.value);

  function reset() {
    board.value = createBoard();
    currentPlayer.value = BLACK;
    winner.value = EMPTY;
    history.value = [];
    aiThinking.value = false;
  }

  function switchMode(newMode) {
    mode.value = newMode;
    reset();
  }

  function switchDifficulty(newDifficulty) {
    difficulty.value = newDifficulty;
    reset();
  }

  function placeStone(r, c) {
    if (
      isGameOver.value ||
      aiThinking.value ||
      !inBounds(r, c) ||
      board.value[r][c] !== EMPTY
    ) {
      return false;
    }

    board.value[r][c] = currentPlayer.value;
    history.value.push({ row: r, col: c, player: currentPlayer.value });

    if (checkWin(board.value, r, c, currentPlayer.value)) {
      winner.value = currentPlayer.value;
      return true;
    }

    if (history.value.length >= BOARD_SIZE * BOARD_SIZE) {
      winner.value = -1; // 和棋
      return true;
    }

    currentPlayer.value = currentPlayer.value === BLACK ? WHITE : BLACK;

    if (mode.value === "pve" && currentPlayer.value === WHITE) {
      aiThinking.value = true;
      // 用 setTimeout 让 UI 先更新，避免卡顿
      setTimeout(() => {
        const [ar, ac] = aiMove(board.value, WHITE, difficulty.value);
        board.value[ar][ac] = WHITE;
        history.value.push({ row: ar, col: ac, player: WHITE });

        if (checkWin(board.value, ar, ac, WHITE)) {
          winner.value = WHITE;
        } else if (history.value.length >= BOARD_SIZE * BOARD_SIZE) {
          winner.value = -1;
        } else {
          currentPlayer.value = BLACK;
        }
        aiThinking.value = false;
      }, 120);
    }

    return true;
  }

  function undo() {
    if (!canUndo.value) return;

    if (mode.value === "pve") {
      // 人机模式下悔棋：撤销 AI 和玩家各一步
      const steps = history.value.length >= 2 ? 2 : 1;
      for (let i = 0; i < steps; i++) {
        const last = history.value.pop();
        if (last) board.value[last.row][last.col] = EMPTY;
      }
      currentPlayer.value = BLACK;
    } else {
      const last = history.value.pop();
      if (last) {
        board.value[last.row][last.col] = EMPTY;
        currentPlayer.value = last.player;
      }
    }

    winner.value = EMPTY;
    aiThinking.value = false;
  }

  return {
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
    EMPTY,
  };
}
