import { createRouter, createWebHistory } from "vue-router";
import Hub from "../views/Hub.vue";
import DishList from "../views/DishList.vue";
import TasteRadar from "../views/TasteRadar.vue";
import Login from "../views/Login.vue";
import AiPond from "../views/AiPond.vue";
import GomokuView from "../views/GomokuView.vue";
import ChineseChessView from "../views/ChineseChessView.vue";
import { isLoggedIn } from "../auth";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/login",
      name: "login",
      component: Login,
      meta: { public: true, shell: "bare" },
    },
    { path: "/", name: "desk", component: Hub, meta: { shell: "desk" } },
    {
      path: "/eat",
      name: "eat",
      component: DishList,
      meta: { shell: "pond", pond: "今天吃什么" },
    },
    {
      path: "/radar",
      name: "radar",
      component: TasteRadar,
      meta: { shell: "pond", pond: "口味雷达" },
    },
    {
      path: "/ai",
      name: "ai",
      component: AiPond,
      meta: { shell: "pond", pond: "问 AI" },
    },
    {
      path: "/gomoku",
      name: "gomoku",
      component: GomokuView,
      meta: { shell: "pond", pond: "五子棋" },
    },
    {
      path: "/chinese-chess",
      name: "chinese-chess",
      component: ChineseChessView,
      meta: { shell: "pond", pond: "中国象棋" },
    },
  ],
  scrollBehavior() {
    return { top: 0 };
  },
});

router.beforeEach((to) => {
  if (to.meta.public) {
    if (to.name === "login" && isLoggedIn()) {
      return { path: "/" };
    }
    return true;
  }
  if (!isLoggedIn()) {
    return {
      name: "login",
      query: { redirect: to.fullPath },
    };
  }
  return true;
});

export default router;
