<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import type { ApiError } from '../../api/http'

const visible = defineModel<boolean>({ default: false })
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pattern = /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/
const rules: FormRules<typeof form> = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{
    required: true,
    validator: (_rule, value, callback) => pattern.test(value) && value.trim()
      ? callback()
      : callback(new Error('新密码需为8至64位，且同时包含字母和数字')),
    trigger: 'blur',
  }],
  confirmPassword: [{
    required: true,
    validator: (_rule, value, callback) => value === form.newPassword
      ? callback()
      : callback(new Error('两次输入的新密码不一致')),
    trigger: 'blur',
  }],
}

watch(visible, (value) => {
  if (value) {
    Object.assign(form, { oldPassword: '', newPassword: '', confirmPassword: '' })
  }
})

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await auth.changePassword(form)
    ElMessage.success('密码已修改，请使用新密码继续登录。')
    visible.value = false
  } catch (error) {
    ElMessage.error((error as ApiError).message || '修改密码失败。')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="修改密码"
    width="min(460px, calc(100% - 32px))"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
      <el-form-item label="旧密码" prop="oldPassword">
        <el-input v-model="form.oldPassword" type="password" show-password autocomplete="current-password" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <p class="form-hint">密码需为8至64位，且同时包含字母和数字。</p>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">确认修改</el-button>
    </template>
  </el-dialog>
</template>
