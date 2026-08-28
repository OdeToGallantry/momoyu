<script setup>
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { login, sanitizeRedirect } from "../auth";

const router = useRouter();
const route = useRoute();

const username = ref("admin");
const password = ref("");
const error = ref("");
const loading = ref(false);

async function onSubmit() {
  error.value = "";
  loading.value = true;
  try {
    await login(username.value.trim(), password.value);
    await router.replace(sanitizeRedirect(route.query.redirect));
  } catch (e) {
    error.value = e.message || "登录失败";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="login-stage">
    <h1 class="brand-hero">摸摸鱼</h1>
    <p class="login-sub">先报上名号，再决定摸哪条</p>

    <n-form class="login-form" @submit.prevent="onSubmit">
      <n-form-item label="账号" :show-feedback="false">
        <n-input
          v-model:value="username"
          name="username"
          autocomplete="username"
          placeholder="用户名"
          size="large"
        />
      </n-form-item>
      <n-form-item label="密码" :show-feedback="false">
        <n-input
          v-model:value="password"
          type="password"
          name="password"
          autocomplete="current-password"
          placeholder="密码"
          show-password-on="click"
          size="large"
          @keyup.enter="onSubmit"
        />
      </n-form-item>
      <n-alert v-if="error" type="error" :bordered="false" class="login-error">
        {{ error }}
      </n-alert>
      <n-button
        type="primary"
        attr-type="submit"
        size="large"
        block
        :loading="loading"
      >
        {{ loading ? "核名中…" : "开摸" }}
      </n-button>
    </n-form>
  </section>
</template>
