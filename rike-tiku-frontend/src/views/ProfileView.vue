<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

import type { ApiError } from '../api/http'
import {
  deleteProfileAvatar,
  fetchProfile,
  updateProfile,
  uploadProfileAvatar,
  type ProfileResponse,
} from '../api/profile'
import ChangePasswordDialog from '../components/auth/ChangePasswordDialog.vue'
import AquaBrand from '../components/layout/AquaBrand.vue'
import { useAuthStore } from '../stores/auth'
import { formatEnum } from '../utils/formatters'

const router = useRouter()
const auth = useAuthStore()
const profile = ref<ProfileResponse | null>(null)
const introduction = ref('')
const loading = ref(false)
const saving = ref(false)
const avatarBusy = ref(false)
const passwordVisible = ref(false)
const fileInput = ref<HTMLInputElement>()

const displayInitial = computed(() => profile.value?.displayName?.trim().slice(0, 1) || '我')
const currentAvatar = computed(() => profile.value?.personal.avatarDataUrl || null)

onMounted(loadProfile)

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await fetchProfile()
    introduction.value = profile.value.personal.introduction || ''
    auth.setProfileAvatar(profile.value.personal.avatarDataUrl)
  } catch (error) {
    ElMessage.error((error as ApiError).message || '个人资料加载失败，请刷新后重试。')
  } finally {
    loading.value = false
  }
}

async function saveIntroduction() {
  saving.value = true
  try {
    profile.value = await updateProfile(introduction.value)
    introduction.value = profile.value.personal.introduction || ''
    ElMessage.success('个人简介已保存。')
  } catch (error) {
    ElMessage.error((error as ApiError).message || '个人简介保存失败。')
  } finally {
    saving.value = false
  }
}

function chooseAvatar() {
  fileInput.value?.click()
}

async function onAvatarSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/png', 'image/jpeg'].includes(file.type)) {
    ElMessage.error('头像仅支持 PNG 或 JPEG 图片。')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('头像文件不能超过 2MB。')
    return
  }
  avatarBusy.value = true
  try {
    const avatar = await uploadProfileAvatar(file)
    if (profile.value) profile.value.personal = { ...profile.value.personal, ...avatar }
    auth.setProfileAvatar(avatar.avatarDataUrl)
    ElMessage.success('头像已更新。')
  } catch (error) {
    ElMessage.error((error as ApiError).message || '头像上传失败。')
  } finally {
    avatarBusy.value = false
  }
}

async function removeAvatar() {
  await ElMessageBox.confirm('删除后将恢复默认头像。', '确认删除头像', {
    type: 'warning', confirmButtonText: '删除头像', cancelButtonText: '取消',
  })
  avatarBusy.value = true
  try {
    const avatar = await deleteProfileAvatar()
    if (profile.value) profile.value.personal = { ...profile.value.personal, ...avatar }
    auth.setProfileAvatar(null)
    ElMessage.success('已恢复默认头像。')
  } catch (error) {
    ElMessage.error((error as ApiError).message || '头像删除失败。')
  } finally {
    avatarBusy.value = false
  }
}

async function logout() {
  await ElMessageBox.confirm('退出后需要重新登录。', '确认退出', {
    type: 'warning', confirmButtonText: '退出登录', cancelButtonText: '取消',
  })
  auth.logout()
  await router.replace('/login')
}

function formatDate(value: string | null) {
  if (!value) return '暂无记录'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
  }).format(new Date(value))
}
</script>

<template>
  <main class="profile-page" v-loading="loading">
    <header class="profile-topbar">
      <AquaBrand class="profile-brand-aqua" :to="auth.getDefaultHome()" subtitle="个人中心" compact />
      <div class="profile-topbar-actions">
        <el-button @click="router.push(auth.getDefaultHome())">返回工作台</el-button>
        <el-dropdown trigger="click">
          <el-button text>账户设置</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="passwordVisible = true">修改密码</el-dropdown-item>
              <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div v-if="profile" class="profile-layout">
      <aside class="profile-identity" aria-label="个人身份">
        <el-avatar :size="112" :src="currentAvatar || undefined">{{ displayInitial }}</el-avatar>
        <div>
          <h1>{{ profile.displayName }}</h1>
          <p>@{{ profile.account.username }}</p>
        </div>
        <div class="profile-role-list" aria-label="账号角色">
          <el-tag v-for="role in profile.account.roles" :key="role" effect="plain">
            {{ formatEnum(role) }}
          </el-tag>
        </div>
        <input
          ref="fileInput"
          class="visually-hidden"
          type="file"
          accept="image/png,image/jpeg"
          @change="onAvatarSelected"
        >
        <div class="avatar-actions">
          <el-button type="primary" :loading="avatarBusy" @click="chooseAvatar">上传头像</el-button>
          <el-button v-if="currentAvatar" :disabled="avatarBusy" @click="removeAvatar">删除头像</el-button>
        </div>
        <small>支持 PNG、JPEG，文件不超过 2MB。</small>
      </aside>

      <div class="profile-content">
        <section class="profile-section">
          <div class="profile-section-heading">
            <div>
              <h2>我的资料</h2>
              <p>姓名、编号和教学组织由管理员维护，个人中心仅供查看。</p>
            </div>
            <el-tag :type="profile.account.accountStatus === 'ENABLED' ? 'success' : 'warning'">
              {{ formatEnum(profile.account.accountStatus) }}
            </el-tag>
          </div>

          <dl class="profile-facts account-facts">
            <div><dt>用户名</dt><dd>{{ profile.account.username }}</dd></div>
            <div><dt>账号角色</dt><dd>{{ profile.account.roles.map(formatEnum).join('、') }}</dd></div>
          </dl>

          <template v-if="profile.studentProfile">
            <h3>学生档案</h3>
            <dl class="profile-facts">
              <div><dt>姓名</dt><dd>{{ profile.studentProfile.name }}</dd></div>
              <div><dt>学号</dt><dd>{{ profile.studentProfile.studentNumber }}</dd></div>
              <div><dt>年级</dt><dd>{{ profile.studentProfile.grade }}</dd></div>
              <div><dt>当前主班级</dt><dd>{{ profile.studentProfile.currentClass || '暂未分班' }}</dd></div>
            </dl>
          </template>

          <template v-if="profile.teacherProfile">
            <h3>教师档案</h3>
            <dl class="profile-facts">
              <div><dt>姓名</dt><dd>{{ profile.teacherProfile.name }}</dd></div>
              <div><dt>工号</dt><dd>{{ profile.teacherProfile.teacherNumber }}</dd></div>
              <div><dt>现实职务</dt><dd>{{ profile.teacherProfile.title || '未填写' }}</dd></div>
            </dl>
            <div class="teaching-scope-summary">
              <span>当前任课</span>
              <ul v-if="profile.teacherProfile.teachingScopes.length">
                <li v-for="scope in profile.teacherProfile.teachingScopes" :key="scope.teachingAssignmentId">
                  {{ scope.className }} · {{ scope.subjectName }}
                </li>
              </ul>
              <p v-else>当前没有 ACTIVE 任课关系。</p>
            </div>
          </template>
        </section>

        <section class="profile-section">
          <div class="profile-section-heading">
            <div>
              <h2>个人简介</h2>
              <p>可以写下学习方向、教学侧重点或工作说明。</p>
            </div>
          </div>
          <el-input
            v-model="introduction"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="写一段简短的个人简介"
          />
          <div class="profile-save-row">
            <el-button type="primary" :loading="saving" @click="saveIntroduction">保存简介</el-button>
          </div>
        </section>

        <section class="profile-section profile-security">
          <div class="profile-section-heading">
            <div>
              <h2>账号安全</h2>
              <p>修改密码继续使用现有安全流程，不会建立第二套账号机制。</p>
            </div>
            <el-button type="primary" plain @click="passwordVisible = true">修改密码</el-button>
          </div>
          <dl class="profile-facts">
            <div><dt>最近登录</dt><dd>{{ formatDate(profile.account.lastLoginAt) }}</dd></div>
            <div><dt>最近修改密码</dt><dd>{{ formatDate(profile.account.passwordChangedAt) }}</dd></div>
          </dl>
        </section>
      </div>
    </div>
    <ChangePasswordDialog v-model="passwordVisible" />
  </main>
</template>
