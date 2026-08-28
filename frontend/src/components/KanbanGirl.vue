<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from "vue";

const props = defineProps({
  mood: {
    type: String,
    default: "idle",
  },
});

const POKES = [
  "诶？突然戳人家…",
  "马尾不是门铃啦。",
  "摸鱼可以，乱戳要排队。",
  "再戳就罚你请奶茶。",
  "有话直说嘛，我在听。",
  "……轻轻的可以。",
  "手感好吗？自己心里清楚。",
  "再往下一点……嗯，到此为止。",
  "你盯着哪里看呢？",
  "脸这么近，呼吸都喷到了。",
  "想摸就直说，装什么正经。",
  "胸口别按，会乱跳的。",
  "裙子边边碰不得哦。",
  "今天胆子不小啊。",
  "被你戳热了……讨厌。",
  "嘴唇别看太久，我会咬你。",
  "耳尖红了？……是你的。",
  "再戳我就坐你腿上了。",
  "嗯……别停太突然。",
  "私下玩可以，外面别说。",
  "你手好烫，松开一点。",
  "腰这里……敏感，轻点。",
  "想听我喘一声吗？不给。",
  "盯着锁骨发呆很久了哦。",
  "再摸下去，工位要着火了。",
];

const rootRef = ref(null);
const ready = ref(false);
const failed = ref(false);

let oml2d = null;
let pokeIndex = 0;
let clickBound = false;
let resizeObserver = null;

function teardown() {
  resizeObserver?.disconnect();
  resizeObserver = null;
  clickBound = false;
  const root = rootRef.value;
  if (!root) return;
  const canvas = root.querySelector("#oml2d-canvas");
  canvas?.removeEventListener("pointerup", onPoke);
  root
    .querySelectorAll(
      "#oml2d-stage, #oml2d-tips, #oml2d-menus, #oml2d-statusBar",
    )
    .forEach((el) => el.remove());
}

function onPoke() {
  if (!oml2d) return;
  oml2d.tipsMessage(POKES[pokeIndex], 2200, 9);
  pokeIndex = (pokeIndex + 1) % POKES.length;
}

function bindCanvasClick() {
  const canvas = rootRef.value?.querySelector("#oml2d-canvas");
  if (!canvas || clickBound) return;
  clickBound = true;
  canvas.addEventListener("pointerup", onPoke);
}

function fitModel() {
  const el = rootRef.value;
  if (!el || !oml2d) return;
  const { width, height } = el.getBoundingClientRect();
  if (width < 40 || height < 80) return;

  oml2d.setStageStyle({ width, height });
  oml2d.setModelAnchor({ x: 0.5, y: 0.38 });
  oml2d.setModelPosition({
    x: width * 0.5,
    y: height * 0.58,
  });
  const scale = Math.min(width / 1080, height / 1680) * 0.92;
  oml2d.setModelScale(Math.max(0.12, Math.min(0.22, scale)));
}

onMounted(async () => {
  await nextTick();
  const parent = rootRef.value;
  if (!parent) return;

  localStorage.removeItem("OML2D_STATUS");
  localStorage.removeItem("OML2D_MODEL_INDEX");
  teardown();

  try {
    const { loadOml2d } = await import("oh-my-live2d");

    oml2d = loadOml2d({
      parentElement: parent,
      sayHello: false,
      mobileDisplay: true,
      dockedPosition: "left",
      primaryColor: "#ff6a45",
      transitionTime: 220,
      menus: { disable: true },
      statusBar: { disable: true },
      stageStyle: {
        position: "absolute",
        left: "0",
        right: "auto",
        bottom: "0",
        width: "100%",
        height: "100%",
        zIndex: "2",
      },
      models: [
        {
          name: "hiyori",
          path: "/live2d/Hiyori/Hiyori.model3.json",
          scale: 0.15,
          mobileScale: 0.13,
          anchor: [0.5, 0.38],
          position: [90, 200],
          mobilePosition: [66, 180],
          volume: 0,
          motionPreloadStrategy: "IDLE",
        },
      ],
      tips: {
        messageLine: 3,
        style: {
          minHeight: "0",
          width: "92%",
          fontSize: "12px",
          lineHeight: "1.4",
          padding: "6px 8px",
          top: "6px",
          color: "#e8e6e3",
          background: "#2b2b31",
          border: "1px solid rgba(255,255,255,0.08)",
          borderRadius: "10px",
          filter: "none",
          boxShadow: "0 10px 24px rgba(0,0,0,0.28)",
        },
        mobileStyle: {
          minHeight: "0",
          width: "92%",
          fontSize: "11px",
          padding: "5px 7px",
          top: "4px",
        },
        idleTips: {
          interval: 16000,
          duration: 2800,
          message: [
            "今天摸哪条？",
            "有事就说，我在听。",
            "马尾别乱扯啦。",
            "偷偷看我干嘛，过来。",
            "工位这么近，心跳都听得见。",
            "困了就靠一会……肩膀借你。",
            "嘴唇干了？……我没有润唇膏给你。",
            "想摸鱼还是想摸我？说清楚。",
            "再盯着领口，扣子要自己开了。",
            "晚上留下？……看你表现。",
            "耳边说话的话，我会发抖的。",
            "腿并紧是怕你乱看。",
            "无聊就逗我，别装忙。",
            "手心出汗了？握紧点。",
          ],
        },
        welcomeTips: {
          duration: 3000,
          message: {
            daybreak: "这么早就来？……粘人。",
            morning: "早。先看我一眼再摸鱼。",
            noon: "午饭想好了吗？不会选就看我。",
            afternoon: "午后犯困？靠过来一点。",
            dusk: "快下班了，再腻一会儿。",
            night: "晚上好。工位只剩我们了。",
            lateNight: "这么晚还不走？想干嘛直说。",
            weeHours: "……还不睡？过来，我陪你。",
          },
        },
        copyTips: { message: [] },
      },
    });

    oml2d.onLoad((status) => {
      if (status === "success") {
        ready.value = true;
        failed.value = false;
      } else if (status === "fail") {
        failed.value = true;
      }
    });

    oml2d.onStageSlideIn(() => {
      fitModel();
      bindCanvasClick();
      resizeObserver = new ResizeObserver(() => fitModel());
      resizeObserver.observe(parent);
    });
  } catch {
    failed.value = true;
  }
});

onUnmounted(() => {
  teardown();
  oml2d = null;
});

watch(
  () => props.mood,
  (mood, previous) => {
    if (!oml2d || mood === previous) return;
    if (mood === "think") {
      oml2d.tipsMessage("唔…想想怎么怼你。", 1600, 6);
    }
  },
);
</script>

<template>
  <div
    ref="rootRef"
    class="kanban-girl"
    role="img"
    aria-label="小克，摸摸鱼看板娘"
  >
    <p v-if="!ready && !failed" class="kanban-status">小克来了…</p>
    <p v-else-if="failed" class="kanban-status">模特没跟上。</p>
  </div>
</template>

<style scoped>
.kanban-girl {
  position: relative;
  align-self: stretch;
  width: 100%;
  min-height: 300px;
  overflow: hidden;
  user-select: none;
}

.kanban-status {
  position: absolute;
  inset: auto 8% 42%;
  z-index: 1;
  margin: 0;
  color: var(--silt);
  font-size: 0.78rem;
  text-align: center;
}

:deep(#oml2d-stage) {
  position: absolute !important;
  left: 0 !important;
  right: auto !important;
  bottom: 0 !important;
  width: 100% !important;
  height: 100% !important;
  z-index: 2 !important;
  transform: none !important;
}

:deep(#oml2d-canvas) {
  width: 100% !important;
  height: 100% !important;
  z-index: 2 !important;
  cursor: pointer;
}

:deep(#oml2d-tips) {
  z-index: 3 !important;
  min-height: 0 !important;
  filter: none !important;
}
</style>
