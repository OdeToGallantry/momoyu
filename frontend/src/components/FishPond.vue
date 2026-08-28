<script setup>
import { onMounted, onUnmounted, ref } from "vue";

const canvasRef = ref(null);

const KOI = ["#ff6a45", "#ff825f", "#f4c7b8", "#e8e6e3", "#6ec8c0"];

let raf = 0;
let ctx = null;
let dpr = 1;
let width = 0;
let height = 0;
let fishes = [];
let ripples = [];
let dragging = false;
let pointer = { x: 0, y: 0, active: false };
let downAt = { x: 0, y: 0 };
let reduced = false;

function rand(min, max) {
  return min + Math.random() * (max - min);
}

function spawn(count) {
  fishes = Array.from({ length: count }, () => {
    const size = rand(22, 52);
    return {
      x: rand(0, width),
      y: rand(0, height),
      vx: rand(-1.2, 1.2),
      vy: rand(-1.2, 1.2),
      size,
      color: KOI[Math.floor(Math.random() * KOI.length)],
      phase: rand(0, Math.PI * 2),
      turn: rand(0.01, 0.03),
    };
  });
}

function resize() {
  const canvas = canvasRef.value;
  if (!canvas) return;
  dpr = Math.min(window.devicePixelRatio || 1, 2);
  width = window.innerWidth;
  height = window.innerHeight;
  canvas.width = Math.floor(width * dpr);
  canvas.height = Math.floor(height * dpr);
  canvas.style.width = `${width}px`;
  canvas.style.height = `${height}px`;
  canvas.style.userSelect = "none";
  ctx = canvas.getContext("2d");
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  if (fishes.length === 0) spawn(5);
}

function scatter(x, y) {
  ripples.push({ x, y, r: 8, life: 1 });
  for (const fish of fishes) {
    const dx = fish.x - x;
    const dy = fish.y - y;
    const dist = Math.hypot(dx, dy) || 1;
    const force = Math.min(420 / dist, 9);
    fish.vx += (dx / dist) * force;
    fish.vy += (dy / dist) * force;
  }
}

function onPointerDown(event) {
  if (event.button !== 0) return;
  dragging = true;
  pointer.active = true;
  pointer.x = event.clientX;
  pointer.y = event.clientY;
  downAt.x = event.clientX;
  downAt.y = event.clientY;
  canvasRef.value?.setPointerCapture(event.pointerId);
}

function onPointerMove(event) {
  pointer.x = event.clientX;
  pointer.y = event.clientY;
  pointer.active = true;
}

function onPointerUp(event) {
  const moved = Math.hypot(event.clientX - downAt.x, event.clientY - downAt.y);
  if (moved < 8) scatter(event.clientX, event.clientY);
  dragging = false;
  try {
    canvasRef.value?.releasePointerCapture(event.pointerId);
  } catch {
    /* already released */
  }
}

function onPointerLeave() {
  pointer.active = false;
  dragging = false;
}

function step() {
  if (!ctx) {
    raf = window.requestAnimationFrame(step);
    return;
  }

  ctx.clearRect(0, 0, width, height);

  for (const ripple of ripples) {
    ripple.r += 3.2;
    ripple.life -= 0.025;
    ctx.beginPath();
    ctx.arc(ripple.x, ripple.y, ripple.r, 0, Math.PI * 2);
    ctx.strokeStyle = `rgba(255, 106, 69, ${Math.max(ripple.life, 0) * 0.45})`;
    ctx.lineWidth = 1.4;
    ctx.stroke();
  }
  ripples = ripples.filter((r) => r.life > 0);

  for (const fish of fishes) {
    fish.phase += 0.18;

    if (dragging) {
      const dx = pointer.x - fish.x;
      const dy = pointer.y - fish.y;
      const dist = Math.hypot(dx, dy) || 1;
      const pull = Math.min(0.18, 28 / dist);
      fish.vx += (dx / dist) * pull;
      fish.vy += (dy / dist) * pull;
    } else if (pointer.active) {
      const dx = fish.x - pointer.x;
      const dy = fish.y - pointer.y;
      const dist = Math.hypot(dx, dy) || 1;
      if (dist < 140) {
        const nudge = (140 - dist) / 1400;
        fish.vx += (dx / dist) * nudge;
        fish.vy += (dy / dist) * nudge;
      }
    }

    fish.vx += Math.cos(fish.phase) * fish.turn;
    fish.vy += Math.sin(fish.phase * 0.7) * fish.turn;

    const speed = Math.hypot(fish.vx, fish.vy) || 0.01;
    const max = reduced ? 1.2 : 3.6;
    const min = 0.55;
    if (speed > max) {
      fish.vx = (fish.vx / speed) * max;
      fish.vy = (fish.vy / speed) * max;
    } else if (speed < min) {
      fish.vx = (fish.vx / speed) * min;
      fish.vy = (fish.vy / speed) * min;
    }

    fish.x += fish.vx;
    fish.y += fish.vy;

    const m = 40;
    if (fish.x < -m) fish.x = width + m;
    if (fish.x > width + m) fish.x = -m;
    if (fish.y < -m) fish.y = height + m;
    if (fish.y > height + m) fish.y = -m;

    drawFish(fish);
  }

  raf = window.requestAnimationFrame(step);
}

function drawFish(fish) {
  const angle = Math.atan2(fish.vy, fish.vx);
  const wag = Math.sin(fish.phase * 2.2) * 0.45;
  const s = fish.size;

  ctx.save();
  ctx.translate(fish.x, fish.y);
  ctx.rotate(angle);
  ctx.fillStyle = fish.color;

  ctx.beginPath();
  ctx.moveTo(s * 0.95, 0);
  ctx.quadraticCurveTo(s * 0.2, -s * 0.42, -s * 0.55, 0);
  ctx.quadraticCurveTo(s * 0.2, s * 0.42, s * 0.95, 0);
  ctx.fill();

  ctx.beginPath();
  ctx.moveTo(-s * 0.5, 0);
  ctx.lineTo(-s * 1.15, s * 0.38 + wag * s * 0.3);
  ctx.lineTo(-s * 0.85, 0);
  ctx.lineTo(-s * 1.15, -s * 0.38 - wag * s * 0.3);
  ctx.closePath();
  ctx.fill();

  ctx.fillStyle = "#1a1a1d";
  ctx.beginPath();
  ctx.arc(s * 0.55, -s * 0.08, Math.max(1.4, s * 0.07), 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();
}

function onVisibility() {
  if (document.hidden) {
    window.cancelAnimationFrame(raf);
    raf = 0;
  } else if (!raf) {
    raf = window.requestAnimationFrame(step);
  }
}

onMounted(() => {
  reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  resize();
  raf = window.requestAnimationFrame(step);
  window.addEventListener("resize", resize);
  document.addEventListener("visibilitychange", onVisibility);
});

onUnmounted(() => {
  window.cancelAnimationFrame(raf);
  window.removeEventListener("resize", resize);
  document.removeEventListener("visibilitychange", onVisibility);
});
</script>

<template>
  <canvas
    ref="canvasRef"
    class="fish-pond"
    aria-hidden="true"
    @pointerdown="onPointerDown"
    @pointermove="onPointerMove"
    @pointerup="onPointerUp"
    @pointercancel="onPointerUp"
    @pointerleave="onPointerLeave"
  />
</template>
