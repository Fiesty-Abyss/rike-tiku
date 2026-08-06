<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const name = computed(() => auth.currentUser?.displayName || auth.currentUser?.username || '同学')
async function logout() {
  await ElMessageBox.confirm('退出后需要重新登录才能继续练习。', '确认退出', { type:'warning', confirmButtonText:'退出登录', cancelButtonText:'取消' })
  auth.logout()
  await router.replace('/login/student')
}
</script>

<template>
  <main class="student-shell">
    <header class="student-header"><router-link to="/student" class="student-brand">理科学习辅助系统</router-link><nav><router-link to="/student/practice">自主练习</router-link><router-link to="/student/wrong-questions">错题本</router-link><el-button text @click="logout">{{ name }} · 退出</el-button></nav></header>
    <section class="student-main"><router-view v-slot="{ Component }"><component :is="Component" /></router-view><section v-if="$route.path === '/student'" class="student-welcome"><div><h1>从一次练习开始</h1><p>题目集合会在创建时冻结。提交后才会显示标准答案与解析，并将错题沉淀到个人错题本。</p></div><div class="student-welcome-actions"><el-button type="primary" size="large" @click="router.push('/student/practice/new')">创建自主练习</el-button><el-button size="large" @click="router.push('/student/wrong-questions')">查看错题本</el-button></div></section></section>
  </main>
</template>
