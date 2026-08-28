<script setup>
import { computed } from "vue";
import {
  NConfigProvider,
  NDialogProvider,
  NMessageProvider,
  darkTheme,
} from "naive-ui";
import { RouterLink, RouterView, useRoute, useRouter } from "vue-router";
import FishPond from "./components/FishPond.vue";
import { currentUser, loggedIn, logout } from "./auth";
import { naiveThemeOverrides } from "./theme/naive";

const router = useRouter();
const route = useRoute();

const shell = computed(() => route.meta.shell || "pond");
const pondName = computed(() =>
  typeof route.meta.pond === "string" ? route.meta.pond : "",
);
const user = currentUser;
const signedIn = loggedIn;

function onLogout() {
  logout();
  router.replace({ name: "login" });
}
</script>

<template>
  <n-config-provider :theme="darkTheme" :theme-overrides="naiveThemeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <div class="app-shell" :class="shell">
          <FishPond />
          <header v-if="shell === 'pond'" class="pond-bar">
            <RouterLink class="back" to="/">回桌面</RouterLink>
            <RouterLink class="brand-mini" to="/">摸摸鱼</RouterLink>
            <p v-if="pondName" class="pond-name">{{ pondName }}</p>
            <div v-if="signedIn" class="mast-user">
              <span>{{ user?.username }} · {{ user?.role === "ADMIN" ? "管理员" : "只读" }}</span>
              <n-button quaternary size="small" @click="onLogout">退出</n-button>
            </div>
          </header>

          <RouterView />
        </div>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>
