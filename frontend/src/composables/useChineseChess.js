import { ref, computed } from "vue";

const ROWS = 10;
const COLS = 9;
const EMPTY = " ";

// 红方大写，黑方小写
const INITIAL_BOARD = [
  ["r", "n", "b", "a", "k", "a", "b", "n", "r"],
  [" ", " ", " ", " ", " ", " ", " ", " ", " "],
  [" ", "c", " ", " ", " ", " ", " ", "c", " "],
  ["p", " ", "p", " ", "p", " ", "p", " ", "p"],
  [" ", " ", " ", " ", " ", " ", " ", " ", " "],
  [" ", " ", " ", " ", " ", " ", " ", " ", " "],
  ["P", " ", "P", " ", "P", " ", "P", " ", "P"],
  [" ", "C", " ", " ", " ", " ", " ", "C", " "],
  [" ", " ", " ", " ", " ", " ", " ", " ", " "],
  ["R", "N", "B", "A", "K", "A", "B", "N", "R"],
];

const PIECE_VALUES = {
  k: 10000,
  a: 20,
  b: 20,
  n: 40,
  r: 90,
  c: 45,
  p: 10,
};

function cloneBoard(board) {
  return board.map((row) => row.slice());
}

function isRed(piece) {
  return piece !== EMPTY && piece === piece.toUpperCase();
}

function isBlack(piece) {
  return piece !== EMPTY && piece === piece.toLowerCase();
}

function sameSide(a, b) {
  if (a === EMPTY || b === EMPTY) return false;
  return isRed(a) === isRed(b);
}

function inBounds(r, c) {
  return r >= 0 && r < ROWS && c >= 0 && c < COLS;
}

function inPalace(r, c, isRedSide) {
  if (c < 3 || c > 5) return false;
  if (isRedSide) return r >= 7 && r <= 9;
  return r >= 0 && r <= 2;
}

function crossedRiver(r, isRedSide) {
  return isRedSide ? r < 5 : r > 4;
}

function countPiecesBetween(board, r1, c1, r2, c2) {
  let count = 0;
  if (r1 === r2) {
    const min = Math.min(c1, c2);
    const max = Math.max(c1, c2);
    for (let c = min + 1; c < max; c++) {
      if (board[r1][c] !== EMPTY) count++;
    }
  } else if (c1 === c2) {
    const min = Math.min(r1, r2);
    const max = Math.max(r1, r2);
    for (let r = min + 1; r < max; r++) {
      if (board[r][c1] !== EMPTY) count++;
    }
  }
  return count;
}

function getRawMoves(board, r, c) {
  const piece = board[r][c];
  if (piece === EMPTY) return [];

  const red = isRed(piece);
  const type = piece.toLowerCase();
  const moves = [];

  const add = (nr, nc) => {
    if (inBounds(nr, nc) && !sameSide(piece, board[nr][nc])) {
      moves.push([nr, nc]);
    }
  };

  if (type === "r") {
    // 車：横竖直线，不能越子
    const dirs = [[0, 1], [0, -1], [1, 0], [-1, 0]];
    for (const [dr, dc] of dirs) {
      for (let i = 1; i < Math.max(ROWS, COLS); i++) {
        const nr = r + dr * i;
        const nc = c + dc * i;
        if (!inBounds(nr, nc)) break;
        if (board[nr][nc] === EMPTY) {
          moves.push([nr, nc]);
        } else {
          if (!sameSide(piece, board[nr][nc])) moves.push([nr, nc]);
          break;
        }
      }
    }
  } else if (type === "n") {
    // 马：日字，蹩马腿
    const jumps = [
      [-2, -1], [-2, 1], [2, -1], [2, 1],
      [-1, -2], [-1, 2], [1, -2], [1, 2],
    ];
    const blocks = {
      "-2,-1": [-1, 0], "-2,1": [-1, 0],
      "2,-1": [1, 0], "2,1": [1, 0],
      "-1,-2": [0, -1], "-1,2": [0, 1],
      "1,-2": [0, -1], "1,2": [0, 1],
    };
    for (const [dr, dc] of jumps) {
      const br = r + blocks[`${dr},${dc}`][0];
      const bc = c + blocks[`${dr},${dc}`][1];
      if (inBounds(br, bc) && board[br][bc] === EMPTY) {
        add(r + dr, c + dc);
      }
    }
  } else if (type === "b") {
    // 象：田字，不能过河，塞象眼
    const jumps = red ? [[-2, -2], [-2, 2]] : [[2, -2], [2, 2]];
    for (const [dr, dc] of jumps) {
      const nr = r + dr;
      const nc = c + dc;
      if (!inBounds(nr, nc)) continue;
      if (red && nr < 5) continue;
      if (!red && nr > 4) continue;
      const eyeR = r + dr / 2;
      const eyeC = c + dc / 2;
      if (board[eyeR][eyeC] === EMPTY) add(nr, nc);
    }
  } else if (type === "a") {
    // 士：斜向一格，九宫内
    const dirs = [[-1, -1], [-1, 1], [1, -1], [1, 1]];
    for (const [dr, dc] of dirs) {
      const nr = r + dr;
      const nc = c + dc;
      if (inPalace(nr, nc, red)) add(nr, nc);
    }
  } else if (type === "k") {
    // 将：横竖一格，九宫内
    const dirs = [[0, 1], [0, -1], [1, 0], [-1, 0]];
    for (const [dr, dc] of dirs) {
      const nr = r + dr;
      const nc = c + dc;
      if (inPalace(nr, nc, red)) add(nr, nc);
    }
    // 将帅照面
    const enemyKing = red ? "k" : "K";
    for (let er = 0; er < ROWS; er++) {
      for (let ec = 0; ec < COLS; ec++) {
        if (board[er][ec] === enemyKing && ec === c) {
          if (countPiecesBetween(board, r, c, er, ec) === 0) {
            moves.push([er, ec]);
          }
        }
      }
    }
  } else if (type === "c") {
    // 炮：横竖直线，移动时不越子，吃子时隔一个子
    const dirs = [[0, 1], [0, -1], [1, 0], [-1, 0]];
    for (const [dr, dc] of dirs) {
      let found = false;
      for (let i = 1; i < Math.max(ROWS, COLS); i++) {
        const nr = r + dr * i;
        const nc = c + dc * i;
        if (!inBounds(nr, nc)) break;
        if (!found) {
          if (board[nr][nc] === EMPTY) {
            moves.push([nr, nc]);
          } else {
            found = true;
          }
        } else {
          if (board[nr][nc] !== EMPTY) {
            if (!sameSide(piece, board[nr][nc])) moves.push([nr, nc]);
            break;
          }
        }
      }
    }
  } else if (type === "p") {
    // 兵：过河前直进，过河后可横进，不能后退
    const forward = red ? -1 : 1;
    add(r + forward, c);
    if (crossedRiver(r, red)) {
      add(r, c + 1);
      add(r, c - 1);
    }
  }

  return moves;
}

function findKing(board, red) {
  const target = red ? "K" : "k";
  for (let r = 0; r < ROWS; r++) {
    for (let c = 0; c < COLS; c++) {
      if (board[r][c] === target) return [r, c];
    }
  }
  return null;
}

function isChecked(board, red) {
  const kingPos = findKing(board, red);
  if (!kingPos) return true;
  const [kr, kc] = kingPos;

  for (let r = 0; r < ROWS; r++) {
    for (let c = 0; c < COLS; c++) {
      const piece = board[r][c];
      if (piece === EMPTY) continue;
      if (isRed(piece) === red) continue;
      const moves = getRawMoves(board, r, c);
      for (const [mr, mc] of moves) {
        if (mr === kr && mc === kc) return true;
      }
    }
  }
  return false;
}

function getLegalMovesForPiece(board, r, c) {
  const piece = board[r][c];
  if (piece === EMPTY) return [];
  const red = isRed(piece);
  const raw = getRawMoves(board, r, c);
  const legal = [];

  for (const [nr, nc] of raw) {
    const next = cloneBoard(board);
    next[nr][nc] = piece;
    next[r][c] = EMPTY;
    if (!isChecked(next, red)) {
      legal.push([nr, nc]);
    }
  }

  return legal;
}

function getAllLegalMoves(board, red) {
  const all = [];
  for (let r = 0; r < ROWS; r++) {
    for (let c = 0; c < COLS; c++) {
      const piece = board[r][c];
      if (piece === EMPTY) continue;
      if (isRed(piece) !== red) continue;
      const moves = getLegalMovesForPiece(board, r, c);
      for (const [nr, nc] of moves) {
        all.push({ fromR: r, fromC: c, toR: nr, toC: nc, piece });
      }
    }
  }
  return all;
}

function isCheckmate(board, red) {
  if (!isChecked(board, red)) return false;
  return getAllLegalMoves(board, red).length === 0;
}

function isStalemate(board, red) {
  if (isChecked(board, red)) return false;
  return getAllLegalMoves(board, red).length === 0;
}

function evaluateBoard(board) {
  let score = 0;
  for (let r = 0; r < ROWS; r++) {
    for (let c = 0; c < COLS; c++) {
      const piece = board[r][c];
      if (piece === EMPTY) continue;
      const value = PIECE_VALUES[piece.toLowerCase()] || 0;
      // 位置加成
      let posBonus = 0;
      const red = isRed(piece);
      const type = piece.toLowerCase();
      if (type === "p") {
        // 兵/卒过河加分，越靠前加分越多
        if (crossedRiver(r, red)) posBonus += 30 + (red ? 5 - r : r - 4) * 5;
      } else if (type === "n") {
        // 马在中央更活跃
        posBonus += (4 - Math.abs(c - 4)) * 2;
      } else if (type === "c") {
        posBonus += (4 - Math.abs(c - 4)) * 2;
      }
      score += red ? value + posBonus : -(value + posBonus);
    }
  }
  // 将军额外加分
  if (isChecked(board, false)) score += 80;
  if (isChecked(board, true)) score -= 80;
  return score;
}

function applyMove(board, fromR, fromC, toR, toC) {
  const next = cloneBoard(board);
  next[toR][toC] = next[fromR][fromC];
  next[fromR][fromC] = EMPTY;
  return next;
}

function aiMove(board, red, difficulty = "normal") {
  const moves = getAllLegalMoves(board, red);
  if (moves.length === 0) return null;

  // 寻找立刻获胜的移动（吃掉对方将/帅）
  for (const move of moves) {
    const target = board[move.toR][move.toC];
    if (target.toLowerCase() === "k") return move;
  }

  // 评估每个移动
  const scored = moves.map((move) => {
    const next = applyMove(board, move.fromR, move.fromC, move.toR, move.toC);
    return { move, score: evaluateBoard(next) };
  });

  // 红方 AI 最大化分数，黑方 AI 最小化分数
  scored.sort((a, b) => (red ? b.score - a.score : a.score - b.score));

  if (difficulty === "easy") {
    // 前 5 名随机
    const topN = Math.min(5, scored.length);
    const idx = Math.floor(Math.random() * topN);
    return scored[idx].move;
  }

  if (difficulty === "normal") {
    // 70% 选最优，30% 前 3 名随机
    if (Math.random() < 0.7) return scored[0].move;
    const topN = Math.min(3, scored.length);
    const idx = Math.floor(Math.random() * topN);
    return scored[idx].move;
  }

  // hard
  return scored[0].move;
}

export function useChineseChess() {
  const board = ref(cloneBoard(INITIAL_BOARD));
  const currentSide = ref(true); // true = 红方
  const winner = ref(null); // null | 'red' | 'black' | 'draw'
  const history = ref([]);
  const mode = ref("pvp"); // 'pvp' | 'pve'
  const difficulty = ref("normal");
  const aiThinking = ref(false);
  const selected = ref(null);
  const lastMove = ref(null);

  const isGameOver = computed(() => winner.value !== null);
  const canUndo = computed(() => history.value.length > 0 && !aiThinking.value);
  const checkedSide = computed(() => {
    if (isGameOver.value) return null;
    return isChecked(board.value, currentSide.value) ? currentSide.value : null;
  });

  function reset() {
    board.value = cloneBoard(INITIAL_BOARD);
    currentSide.value = true;
    winner.value = null;
    history.value = [];
    aiThinking.value = false;
    selected.value = null;
    lastMove.value = null;
  }

  function switchMode(newMode) {
    mode.value = newMode;
    reset();
  }

  function switchDifficulty(newDifficulty) {
    difficulty.value = newDifficulty;
    reset();
  }

  function selectPiece(r, c) {
    const piece = board.value[r][c];
    if (piece === EMPTY) {
      selected.value = null;
      return;
    }
    if (isRed(piece) !== currentSide.value) {
      selected.value = null;
      return;
    }
    selected.value = { r, c };
  }

  function tryMove(toR, toC) {
    if (
      isGameOver.value ||
      aiThinking.value ||
      !selected.value
    ) return false;

    const { r: fromR, c: fromC } = selected.value;
    const legal = getLegalMovesForPiece(board.value, fromR, fromC);
    const valid = legal.some(([lr, lc]) => lr === toR && lc === toC);
    if (!valid) return false;

    const piece = board.value[fromR][fromC];
    const captured = board.value[toR][toC];

    board.value[toR][toC] = piece;
    board.value[fromR][fromC] = EMPTY;
    history.value.push({
      fromR,
      fromC,
      toR,
      toC,
      piece,
      captured,
      side: currentSide.value,
    });
    lastMove.value = { fromR, fromC, toR, toC };
    selected.value = null;

    const opponent = !currentSide.value;
    if (isCheckmate(board.value, opponent)) {
      winner.value = currentSide.value ? "red" : "black";
      return true;
    }
    if (isStalemate(board.value, opponent)) {
      winner.value = "draw";
      return true;
    }

    currentSide.value = opponent;

    if (mode.value === "pve" && !currentSide.value) {
      aiThinking.value = true;
      setTimeout(() => {
        const move = aiMove(board.value, false, difficulty.value);
        if (move) {
          const aiPiece = board.value[move.fromR][move.fromC];
          const aiCaptured = board.value[move.toR][move.toC];
          board.value[move.toR][move.toC] = aiPiece;
          board.value[move.fromR][move.fromC] = EMPTY;
          history.value.push({
            ...move,
            piece: aiPiece,
            captured: aiCaptured,
            side: false,
          });
          lastMove.value = move;

          if (isCheckmate(board.value, true)) {
            winner.value = "black";
          } else if (isStalemate(board.value, true)) {
            winner.value = "draw";
          } else {
            currentSide.value = true;
          }
        }
        aiThinking.value = false;
      }, 150);
    }

    return true;
  }

  function handleCellClick(r, c) {
    if (isGameOver.value || aiThinking.value) return;

    if (selected.value) {
      const { r: fromR, c: fromC } = selected.value;
      if (fromR === r && fromC === c) {
        selected.value = null;
        return;
      }
      const moved = tryMove(r, c);
      if (!moved) {
        selectPiece(r, c);
      }
    } else {
      selectPiece(r, c);
    }
  }

  function undo() {
    if (!canUndo.value) return;

    if (mode.value === "pve") {
      // 撤销 AI 和玩家各一步
      const steps = history.value.length >= 2 ? 2 : 1;
      for (let i = 0; i < steps; i++) {
        const last = history.value.pop();
        if (last) {
          board.value[last.fromR][last.fromC] = last.piece;
          board.value[last.toR][last.toC] = last.captured;
        }
      }
      currentSide.value = true;
    } else {
      const last = history.value.pop();
      if (last) {
        board.value[last.fromR][last.fromC] = last.piece;
        board.value[last.toR][last.toC] = last.captured;
        currentSide.value = last.side;
      }
    }

    winner.value = null;
    aiThinking.value = false;
    selected.value = null;
    lastMove.value = history.value[history.value.length - 1] || null;
  }

  const legalMovesForSelected = computed(() => {
    if (!selected.value) return [];
    return getLegalMovesForPiece(board.value, selected.value.r, selected.value.c);
  });

  return {
    board,
    currentSide,
    winner,
    history,
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
  };
}
