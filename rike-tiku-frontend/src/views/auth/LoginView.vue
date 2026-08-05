<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import type { RoleCode } from '../../api/auth'
import type { ApiError } from '../../api/http'
import LoginForm from '../../components/auth/LoginForm.vue'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const errorMessage = ref('')

const role = computed<RoleCode>(() => String(route.params.role).toUpperCase() as RoleCode)
const roleText = computed(() => ({ STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' })[role.value])
const roleDescription = computed(() => ({
  STUDENT: '查看练习、错题与学习建议。',
  TEACHER: '进入教学与题库管理入口。',
  ADMIN: '进入账号与系统管理入口。',
})[role.value])

function readableError(error: ApiError) {
  const messages: Record<string, string> = {
    INVALID_CREDENTIALS: '用户名或密码错误，请重新输入。',
    ROLE_MISMATCH: '该账号不具备当前入口角色，请选择正确入口。',
    ACCOUNT_DISABLED: '该账号已被停用，请联系管理员。',
    ACCOUNT_LOCKED: '该账号已被锁定，请联系管理员。',
  }
  return (error.code && messages[error.code]) || error.message || '登录失败，请稍后重试。'
}

async function handleLogin(payload: { username: string; password: string; expectedRole: RoleCode }) {
  loading.value = true
  errorMessage.value = ''
  try {
    await authStore.login(payload)
    await router.replace(authStore.mustChangePassword ? '/change-initial-password' : authStore.getDefaultHome())
  } catch (error) {
    errorMessage.value = readableError(error as ApiError)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-introduction" aria-label="系统说明">
      <p class="school-mark">RIKE · LEARNING</p>
      <h1>集成大模型智能答疑的在线题库实训管理系统</h1>
      <p>面向高中物理、化学、生物学习场景的统一入口。</p>
      <div class="subject-line" aria-label="支持学科">
        <span>物理</span><span>化学</span><span>生物</span>
      </div>
    </section>
    <section class="auth-panel" aria-labelledby="login-title">
      <p class="role-label">{{ roleText }}入口</p>
      <h2 id="login-title">欢迎回来</h2>
      <p class="auth-description">{{ roleDescription }}</p>
      <LoginForm :role="role" :loading="loading" :error-message="errorMessage" @submit="handleLogin" />
      <nav class="login-switch" aria-label="切换登录入口">
        <RouterLink v-for="item in [{ key: 'student', text: '学生' }, { key: 'teacher', text: '教师' }, { key: 'admin', text: '管理员' }]" :key="item.key" :to="`/login/${item.key}`" :class="{ active: item.key === route.params.role }">
          {{ item.text }}
        </RouterLink>
      </nav>
    </section>
  </main>
</template>
