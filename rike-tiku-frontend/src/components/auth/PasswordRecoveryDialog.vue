<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { requestPasswordRecovery } from '../../api/auth'
import type { ApiError } from '../../api/http'
import ImageCaptcha from './ImageCaptcha.vue'
const visible=defineModel<boolean>({required:true})
const username=ref('');const captchaCode=ref('');const challengeId=ref('');const captcha=ref<InstanceType<typeof ImageCaptcha>>();const loading=ref(false)
async function submit(){if(!username.value.trim()||!captchaCode.value.trim()||!challengeId.value)return;loading.value=true;try{const result=await requestPasswordRecovery({username:username.value.trim(),challengeId:challengeId.value,captchaCode:captchaCode.value});ElMessage.success(result.message);visible.value=false} catch(error){ElMessage.warning((error as ApiError).message||'提交失败，请刷新验证码后重试。');captchaCode.value='';void captcha.value?.refresh()}finally{loading.value=false}}
</script>
<template><el-dialog v-model="visible" title="忘记密码" width="min(440px, 94vw)"><p>提交后由管理员核验并恢复默认密码。系统不会在此处判断账号是否存在。</p><el-form label-position="top" @submit.prevent="submit"><el-form-item label="用户名"><el-input v-model="username" maxlength="64" autocomplete="username" /></el-form-item><el-form-item><ImageCaptcha ref="captcha" v-model="captchaCode" @challenge="challengeId=$event" /></el-form-item><el-button type="primary" native-type="submit" :loading="loading" :disabled="!username.trim()||!captchaCode.trim()||!challengeId">提交请求</el-button></el-form></el-dialog></template>
