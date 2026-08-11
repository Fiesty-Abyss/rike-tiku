<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'

import type { ApiError } from '../../api/http'
import AquaBrand from '../../components/layout/AquaBrand.vue'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const errorMessage = ref('')
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/
const rules: FormRules<typeof form> = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, validator: (_rule, value, callback) => passwordPattern.test(value) && value.trim() ? callback() : callback(new Error('新密码需为8至64位，且同时包含字母和数字')), trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: (_rule, value, callback) => value === form.newPassword ? callback() : callback(new Error('两次输入的新密码不一致')), trigger: 'blur' }],
}
const username = computed(() => authStore.currentUser?.username ?? '')

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  errorMessage.value = ''
  try {
    await authStore.changeInitialPassword(form)
    await router.replace(authStore.getDefaultHome())
  } catch (error) {
    errorMessage.value = (error as ApiError).message ?? '修改密码失败，请重试。'
  } finally {
    loading.value = false
  }
}

function logout() {
  authStore.logout()
  void router.replace('/login/student')
}
</script>

<template>
  <main class="single-panel-page password-entry-page">
    <el-card class="password-card aero-solid" shadow="never">
      <AquaBrand class="password-brand" subtitle="账号安全入口" compact />
      <p class="role-label">首次登录安全设置</p>
      <h1>请先修改初始密码</h1>
      <p class="auth-description">{{ username }}，为了保护你的账号安全，请设置新的登录密码。</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="旧密码" prop="oldPassword"><el-input v-model="form.oldPassword" type="password" show-password autocomplete="current-password" /></el-form-item>
        <el-form-item label="新密码" prop="newPassword"><el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" /></el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword"><el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" /></el-form-item>
        <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon />
        <el-button class="submit-button" native-type="submit" type="primary" :loading="loading">保存新密码</el-button>
      </el-form>
      <el-button text class="logout-link" @click="logout">退出并返回登录页</el-button>
    </el-card>
  </main>
</template>
