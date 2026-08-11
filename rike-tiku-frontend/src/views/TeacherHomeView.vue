<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { fetchTeachingScopes, type TeachingScope } from '../api/teacher'
import ChangePasswordDialog from '../components/auth/ChangePasswordDialog.vue'
import AquaBrand from '../components/layout/AquaBrand.vue'
import { useAuthStore } from '../stores/auth'
import { formatEnum } from '../utils/formatters'

const router = useRouter()
const auth = useAuthStore()
const scopes = ref<TeachingScope[]>([])
const loading = ref(false)
const passwordVisible = ref(false)
const name = computed(() => auth.currentUser?.displayName || auth.currentUser?.username || '教师')
const canSwitchRole = computed(() => auth.roles.length > 1)

onMounted(async () => {
  loading.value = true
  try {
    scopes.value = await fetchTeachingScopes()
  } catch {
    ElMessage.error('任教范围加载失败，请刷新后重试。')
  } finally {
    loading.value = false
  }
})

async function logout() {
  await ElMessageBox.confirm('退出后需要重新登录。', '确认退出', {
    type: 'warning',
    confirmButtonText: '退出登录',
    cancelButtonText: '取消',
  })
  auth.logout()
  await router.replace('/login')
}
</script>

<template>
  <main class="workspace-page">
    <header class="workspace-header teacher-header">
      <div class="teacher-header-context">
        <AquaBrand class="workspace-brand-aqua" to="/teacher" subtitle="教师科学工作台" compact />
        <h1>教师工作台</h1>
        <p>
          您好，{{ name }}
          <span v-if="auth.currentUser?.teacherNumber"> · 工号 {{ auth.currentUser.teacherNumber }}</span>
        </p>
      </div>
      <el-dropdown>
        <el-button class="user-menu-button">
          <el-avatar :size="28" :src="auth.profileAvatar || undefined">{{ name.slice(0, 1) }}</el-avatar>
          <span>{{ name }} · 教师</span>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-if="canSwitchRole" @click="router.push('/select-role')">切换身份</el-dropdown-item>
            <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
            <el-dropdown-item @click="passwordVisible = true">修改密码</el-dropdown-item>
            <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>
    <section class="workspace-content teacher-workspace">
      <div class="student-page-heading">
        <div>
          <h2>我的任教范围</h2>
          <p>仅展示当前教师档案关联的班级、科目三元任课关系。</p>
        </div>
        <el-button type="primary" plain @click="router.push('/messages')">消息</el-button>
      </div>
      <div v-loading="loading" class="teacher-scope-grid">
        <article
          v-for="scope in scopes"
          :key="scope.teachingAssignmentId"
          class="teacher-scope-card"
          :class="`teacher-scope-card--${scope.subjectCode.toLowerCase()}`"
          :data-subject="scope.subjectCode.toLowerCase()"
        >
          <span class="teacher-scope-ambient" aria-hidden="true"></span>
          <div class="teacher-scope-route"><span>{{ scope.grade }}</span><i></i><span>{{ scope.subjectName }}</span></div>
          <div><h3>{{ scope.className }}</h3><p>{{ scope.homeroomSubject ? '主任课教师' : '任课教师' }} · {{ formatEnum(scope.teachingStatus) }}</p></div>
          <el-button v-if="scope.teachingStatus === 'ACTIVE'" type="primary" plain @click="router.push(`/teacher/scopes/${scope.teachingAssignmentId}`)">进入班级学科工作台</el-button>
          <el-tag v-else type="info">当前范围不可进入</el-tag>
        </article>
        <el-empty v-if="!loading && !scopes.length" description="当前没有可展示的任教范围。" />
      </div>
      <el-alert
        class="teacher-roadmap"
        title="当前工作边界"
        type="info"
        :closable="false"
        show-icon
        description="本页提供任教范围、学生练习学情、高频考点与师生私信；任务发布和考试不属于当前版本。"
      />
    </section>
    <ChangePasswordDialog v-model="passwordVisible" />
  </main>
</template>
