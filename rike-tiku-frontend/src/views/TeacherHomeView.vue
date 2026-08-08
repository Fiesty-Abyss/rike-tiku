<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { fetchTeachingScopes, type TeachingScope } from '../api/teacher'
import ChangePasswordDialog from '../components/auth/ChangePasswordDialog.vue'
import { useAuthStore } from '../stores/auth'
import { formatEnum } from '../utils/formatters'

const router = useRouter()
const auth = useAuthStore()
const scopes = ref<TeachingScope[]>([])
const loading = ref(false)
const passwordVisible = ref(false)
const name = computed(() => auth.currentUser?.displayName || auth.currentUser?.username || '教师')

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
    <header class="workspace-header">
      <div>
        <h1>教师工作台</h1>
        <p>
          您好，{{ name }}
          <span v-if="auth.currentUser?.teacherNumber"> · 工号 {{ auth.currentUser.teacherNumber }}</span>
        </p>
      </div>
      <el-dropdown>
        <el-button class="user-menu-button">
          <el-avatar :size="28" :src="auth.profileAvatar || undefined">{{ name.slice(0, 1) }}</el-avatar>
          <span>账户设置</span>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
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
      <el-table
        v-loading="loading"
        :data="scopes"
        class="data-table"
        empty-text="当前没有可展示的任教范围。"
      >
        <el-table-column prop="className" label="班级" />
        <el-table-column prop="grade" label="年级" />
        <el-table-column prop="subjectName" label="科目" />
        <el-table-column label="主任课">
          <template #default="{ row }">{{ row.homeroomSubject ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }"><el-tag>{{ formatEnum(row.teachingStatus) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button
              v-if="row.teachingStatus === 'ACTIVE'"
              link
              type="primary"
              @click="router.push(`/teacher/scopes/${row.teachingAssignmentId}`)"
            >进入工作台</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-alert
        class="teacher-roadmap"
        title="后续建设说明"
        type="info"
        :closable="false"
        show-icon
        description="本页提供班级学科工作台与师生私信；成绩统计、练习统计、任务发布和考试尚未实现。"
      />
    </section>
    <ChangePasswordDialog v-model="passwordVisible" />
  </main>
</template>
