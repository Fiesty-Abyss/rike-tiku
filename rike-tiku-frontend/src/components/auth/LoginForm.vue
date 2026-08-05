<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

import type { RoleCode } from '../../api/auth'

const props = defineProps<{
  role: RoleCode
  loading: boolean
  errorMessage: string
}>()

const emit = defineEmits<{
  submit: [payload: { username: string; password: string; expectedRole: RoleCode }]
}>()

const formRef = ref<FormInstance>()
const form = reactive({ username: '', password: '' })
const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (valid) emit('submit', { ...form, expectedRole: props.role })
}
</script>

<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
    <el-form-item label="用户名" prop="username">
      <el-input v-model="form.username" autocomplete="username" placeholder="请输入用户名" size="large" />
    </el-form-item>
    <el-form-item label="密码" prop="password">
      <el-input v-model="form.password" type="password" show-password autocomplete="current-password" placeholder="请输入密码" size="large" />
    </el-form-item>
    <el-alert v-if="errorMessage" class="form-error" :title="errorMessage" type="error" :closable="false" show-icon />
    <el-button native-type="submit" type="primary" size="large" class="submit-button" :loading="loading">
      登录系统
    </el-button>
  </el-form>
</template>
