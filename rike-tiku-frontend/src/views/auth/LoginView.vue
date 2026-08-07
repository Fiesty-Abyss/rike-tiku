<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import type { ApiError } from '../../api/http'
import LoginForm from '../../components/auth/LoginForm.vue'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const errorMessage = ref('')
const loginForm = ref<InstanceType<typeof LoginForm>>()

const messages: Record<string, string> = {
  INVALID_CREDENTIALS: '用户名或密码错误，请重新输入。',
  ACCOUNT_DISABLED: '账号已被停用，请联系管理员。',
  ACCOUNT_LOCKED: '账号已被锁定，请联系管理员。',
  SLIDER_CHALLENGE_REQUIRED: '请先完成滑块验证。',
  SLIDER_CHALLENGE_EXPIRED: '滑块已过期，请刷新后重试。',
  SLIDER_CHALLENGE_INVALID: '滑块位置不正确，请刷新后重试。',
  SLIDER_CHALLENGE_REUSED: '滑块已使用，请刷新后重试。',
}

async function handleLogin(payload: { username: string; password: string; challengeId: string; sliderOffset: number }) {
  loading.value = true
  errorMessage.value = ''
  try {
    await auth.login(payload)
    await router.replace(auth.mustChangePassword ? '/change-initial-password' : auth.roles.length > 1 ? '/select-role' : auth.getDefaultHome())
  } catch (error) {
    const api = error as ApiError
    errorMessage.value = messages[api.code || ''] || api.message || '登录失败，请稍后重试。'
    loginForm.value?.refreshSlider()
  } finally {
    loading.value = false
  }
}
</script>
<template>
  <main class="auth-page">
    <section class="auth-introduction" aria-label="系统说明">
      <p class="school-mark">RIKE · LEARNING</p>
      <h1>理科学习，从清晰的练习开始。</h1>
      <p>统一进入高中物理、化学、生物的题库实训空间；根据账号真实角色进入对应工作台。</p>
      <div class="subject-line"><span>物理</span><span>化学</span><span>生物</span></div>
    </section>
    <section class="auth-panel" aria-labelledby="login-title">
      <h2 id="login-title">欢迎登录</h2>
      <p class="auth-description">输入账号和密码，完成安全验证后继续。</p>
      <LoginForm ref="loginForm" :loading="loading" :error-message="errorMessage" @submit="handleLogin" />
      <p class="login-note">忘记密码请联系管理员</p>
    </section>
  </main>
</template>
