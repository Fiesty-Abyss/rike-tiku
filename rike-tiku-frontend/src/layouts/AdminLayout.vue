<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import ChangePasswordDialog from '../components/auth/ChangePasswordDialog.vue'
import AquaBrand from '../components/layout/AquaBrand.vue'

import { useAuthStore } from '../stores/auth'
import { fetchPasswordRecoveries } from '../api/admin/passwordRecovery'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const userName = computed(() => authStore.currentUser?.displayName || authStore.currentUser?.username || '管理员')
const canSwitchRole = computed(() => authStore.roles.length > 1)
const passwordVisible = ref(false)
const routeTitle = computed(() => String(route.meta.title || '管理员工作台'))
const routeSubtitle = computed(() => String(route.meta.subtitle || '系统管理'))
const pendingRecoveries=ref(0)
onMounted(async()=>{try{pendingRecoveries.value=(await fetchPasswordRecoveries()).pendingCount}catch{pendingRecoveries.value=0}})

async function logout() {
  await ElMessageBox.confirm('退出后需要重新登录才能继续管理。', '确认退出登录', { type: 'warning', confirmButtonText: '退出登录', cancelButtonText: '取消' })
  authStore.logout()
  await router.replace('/login')
}
</script>

<template>
  <el-container class="admin-shell">
    <el-aside class="admin-sidebar" width="224px">
      <div class="admin-brand"><AquaBrand class="admin-brand-aqua" to="/admin" subtitle="管理员工作台" compact /></div>
      <el-menu class="admin-menu" router :default-active="$route.path" aria-label="管理员工作区导航">
        <el-menu-item index="/admin"><span>工作台</span></el-menu-item>
        <el-menu-item index="/admin/classes"><span>班级管理</span></el-menu-item>
        <el-menu-item index="/admin/teachers"><span>教师与任课关系</span></el-menu-item>
        <el-menu-item index="/admin/questions"><span>题库审核发布</span></el-menu-item>
        <el-menu-item index="/admin/ai-generation"><span>AI 候选题</span></el-menu-item>
        <el-menu-item index="/admin/ai-models"><span>AI 模型管理</span></el-menu-item>
        <el-menu-item index="/admin/students"><span>学生管理</span></el-menu-item>
        <el-menu-item index="/admin/password-recovery"><span>密码恢复通知</span><el-badge v-if="pendingRecoveries" :value="pendingRecoveries" /></el-menu-item>
        <el-menu-item index="/admin/operation-logs"><span>操作日志</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-topbar">
        <div class="admin-context"><strong>{{ routeTitle }}</strong><span>{{ routeSubtitle }}</span></div>
        <el-dropdown trigger="click">
          <el-button text class="user-menu-button"><el-avatar :size="28" :src="authStore.profileAvatar || undefined">{{ userName.slice(0, 1) }}</el-avatar><span>{{ userName }} · 管理员</span></el-button>
          <template #dropdown><el-dropdown-menu><el-dropdown-item v-if="canSwitchRole" @click="router.push('/select-role')">切换身份</el-dropdown-item><el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item><el-dropdown-item @click="passwordVisible=true">修改密码</el-dropdown-item><el-dropdown-item divided @click="logout">退出登录</el-dropdown-item></el-dropdown-menu></template>
        </el-dropdown>
      </el-header>
      <el-main class="admin-main"><RouterView /></el-main><ChangePasswordDialog v-model="passwordVisible" />
    </el-container>
  </el-container>
</template>
