<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import type { ApiError } from '../../api/http'
import aquaWorld from '../../assets/aqua/rike-aqua-world.webp'
import { resolvePostLoginPath } from '../../auth/postLoginRoute'
import LoginForm from '../../components/auth/LoginForm.vue'
import PasswordRecoveryDialog from '../../components/auth/PasswordRecoveryDialog.vue'
import AquaBrand from '../../components/layout/AquaBrand.vue'
import { useAuthStore } from '../../stores/auth'
import { useEntranceMotion } from '../../utils/entranceMotion'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const errorMessage = ref('')
const loginForm = ref<InstanceType<typeof LoginForm>>()
const root = ref<HTMLElement>()
const recoveryVisible=ref(false)
useEntranceMotion(root, '.auth-world-copy, .auth-optic, .auth-panel', 0.08)

const messages: Record<string, string> = {
  INVALID_CREDENTIALS: '用户名或密码错误，请重新输入。',
  ACCOUNT_DISABLED: '账号已被停用，请联系管理员。',
  ACCOUNT_LOCKED: '账号已被锁定，请联系管理员。',
  CAPTCHA_CHALLENGE_REQUIRED: '请先输入验证码。',
  CAPTCHA_CHALLENGE_EXPIRED: '验证码已过期，请重新输入。',
  CAPTCHA_INCORRECT: '验证码不正确，请重新输入。',
  CAPTCHA_CHALLENGE_REUSED: '验证码已经使用，请重新获取。',
}

async function handleLogin(payload: {
  username: string
  password: string
  challengeId: string
  captchaCode: string
}) {
  loading.value = true
  errorMessage.value = ''
  try {
    await auth.login(payload)
    await router.replace(resolvePostLoginPath(
      auth.mustChangePassword,
      auth.roles.length,
      auth.getDefaultHome(),
    ))
  } catch (error) {
    const api = error as ApiError
    errorMessage.value = messages[api.code || ''] || api.message || '登录失败，请稍后重试。'
    loginForm.value?.refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>
<template>
  <main ref="root" class="auth-page">
    <section class="auth-introduction" aria-label="系统说明">
      <img class="auth-world" :src="aquaWorld" width="1586" height="992" loading="eager" fetchpriority="high" decoding="async" alt="清水与日光中的 RIKE 透明科学仪器世界" />
      <span class="auth-caustics" aria-hidden="true"></span>
      <div class="auth-world-copy">
        <p class="school-mark">RIKE AQUA FUTURE</p>
        <h1>进入你的<br />科学工作台</h1>
        <p class="auth-product-truth">高中理科学习与教学管理</p>
        <p>物理的场与波、化学的变化、生物的生命系统，共用一套可复验的学习事实。</p>
        <div class="subject-line" aria-label="支持学科"><span>FIELD</span><span>EQUILIBRIUM</span><span>LIFE</span></div>
      </div>
      <div class="auth-optic" aria-hidden="true"><i></i><b></b></div>
    </section>
    <section class="auth-panel aero-glass-heavy" aria-labelledby="login-title">
      <RouterLink class="auth-home-link" to="/">返回首页</RouterLink>
      <AquaBrand class="auth-brand" subtitle="安全登录入口" />
      <h2 id="login-title">欢迎登录</h2>
      <p class="auth-description">输入账号、密码和图形验证码后登录。</p>
      <LoginForm ref="loginForm" :loading="loading" :error-message="errorMessage" @submit="handleLogin" />
      <button class="login-note login-note-button" type="button" @click="recoveryVisible=true">忘记密码？</button>
      <PasswordRecoveryDialog v-model="recoveryVisible" />
    </section>
  </main>
</template>
