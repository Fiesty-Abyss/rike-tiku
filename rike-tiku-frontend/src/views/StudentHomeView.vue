<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchPracticeOptions, type Subject } from '../api/student/practice'
import ChangePasswordDialog from '../components/auth/ChangePasswordDialog.vue'
import { useAuthStore } from '../stores/auth'
import { formatEnum } from '../utils/formatters'

const router = useRouter()
const auth = useAuthStore()
const subjects = ref<Subject[]>([])
const points = ref<Record<number, number>>({})
const passwordVisible = ref(false)
const name = computed(() => auth.currentUser?.displayName || auth.currentUser?.username || '同学')

onMounted(async () => {
  try {
    const data = await fetchPracticeOptions()
    subjects.value = data.subjects
    await Promise.all(data.subjects.map(async (subject) => {
      points.value[subject.id] = (await fetchPracticeOptions(subject.id)).knowledgePoints.length
    }))
  } catch {
    ElMessage.error('学习工作台加载失败，请刷新后重试。')
  }
})

async function logout() {
  await ElMessageBox.confirm('退出后需要重新登录才能继续练习。', '确认退出', {
    type: 'warning',
    confirmButtonText: '退出登录',
    cancelButtonText: '取消',
  })
  auth.logout()
  await router.replace('/login')
}
</script>

<template>
  <main class="student-shell">
    <header class="student-header">
      <router-link to="/student" class="student-brand">理科学习辅助系统</router-link>
      <nav>
        <router-link to="/student">三科工作台</router-link>
        <router-link to="/student/practice/new">自主练习</router-link>
        <router-link to="/student/wrong-questions">错题本</router-link>
        <el-dropdown>
          <el-button text>{{ name }}</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="passwordVisible = true">修改密码</el-dropdown-item>
              <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </nav>
    </header>
    <section class="student-main">
      <router-view />
      <section v-if="$route.path === '/student'" class="student-dashboard">
        <div class="student-dashboard-title">
          <h1>选择一门学科，开始今天的练习</h1>
          <p>系统不会展示虚构的学习统计；请从真实可用的知识点、练习与错题记录开始。</p>
        </div>
        <div class="subject-cards">
          <article v-for="subject in subjects" :key="subject.id" class="subject-card">
            <div>
              <span class="subject-code">{{ formatEnum(subject.code) }}学习</span>
              <h2>{{ subject.name }}</h2>
              <p>{{ {
                PHYSICS: '从力学、电磁与实验思维中拆解问题。',
                CHEMISTRY: '在概念、反应与物质变化中建立联系。',
                BIOLOGY: '用结构、过程与调节理解生命现象。',
              }[subject.code] }}</p>
              <small>可用知识点 {{ points[subject.id] ?? '—' }} 个</small>
            </div>
            <div class="subject-card-actions">
              <el-button
                type="primary"
                @click="router.push(`/student/subjects/${subject.code.toLowerCase()}`)"
              >
                进入{{ subject.name }}
              </el-button>
              <el-button
                link
                @click="router.push({ path: '/student/wrong-questions', query: { subjectId: subject.id } })"
              >
                查看本科错题
              </el-button>
            </div>
          </article>
        </div>
      </section>
    </section>
    <ChangePasswordDialog v-model="passwordVisible" />
  </main>
</template>
