<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'

import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const userName = computed(() => authStore.currentUser?.displayName || authStore.currentUser?.username || '管理员')

async function logout() {
  await ElMessageBox.confirm('退出后需要重新登录才能继续管理。', '确认退出登录', { type: 'warning', confirmButtonText: '退出登录', cancelButtonText: '取消' })
  authStore.logout()
  await router.replace('/login/admin')
}
</script>

<template>
  <el-container class="admin-shell">
    <el-aside class="admin-sidebar" width="224px">
      <router-link class="admin-brand" to="/admin">
        <span class="admin-brand-mark">RL</span>
        <span>理科学习辅助系统<small>ADMIN CONSOLE</small></span>
      </router-link>
      <el-menu class="admin-menu" router :default-active="$route.path">
        <el-menu-item index="/admin"><span>工作台</span></el-menu-item>
        <el-menu-item index="/admin/classes"><span>班级管理</span></el-menu-item>
        <el-menu-item index="/admin/teachers"><span>教师与任课关系</span></el-menu-item>
        <el-menu-item index="/admin/questions"><span>题库审核发布</span></el-menu-item>
        <el-menu-item index="/admin/students/import"><span>学生 Excel 导入</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-topbar">
        <div><strong>教学组织管理</strong><span>管理员专用</span></div>
        <el-dropdown trigger="click">
          <el-button text>{{ userName }} · 管理员</el-button>
          <template #dropdown><el-dropdown-menu><el-dropdown-item @click="logout">退出登录</el-dropdown-item></el-dropdown-menu></template>
        </el-dropdown>
      </el-header>
      <el-main class="admin-main"><RouterView /></el-main>
    </el-container>
  </el-container>
</template>
