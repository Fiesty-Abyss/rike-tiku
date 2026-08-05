<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import type { RoleCode } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'

const props = defineProps<{ role: RoleCode; title: string }>()
const router = useRouter()
const authStore = useAuthStore()
const userName = computed(() => authStore.currentUser?.displayName || authStore.currentUser?.username || '')
const roles = computed(() => authStore.roles)

function logout() {
  authStore.logout()
  void router.replace('/login/student')
}
</script>

<template>
  <main class="workspace-page">
    <header class="workspace-header">
      <div>
        <p class="school-mark">RIKE · LEARNING</p>
        <h1>{{ title }}</h1>
      </div>
      <el-button plain @click="logout">退出登录</el-button>
    </header>
    <section class="workspace-content">
      <el-card shadow="never">
        <p class="role-label">当前身份</p>
        <h2>{{ userName }}</h2>
        <p>已验证角色：{{ roles.join('、') }}</p>
        <el-empty description="功能开发中，本轮仅完成认证导航基础。" :image-size="92" />
      </el-card>
    </section>
  </main>
</template>
