<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import SliderVerification from './SliderVerification.vue'
import type { SliderChallenge } from '../../api/auth'

defineProps<{ loading:boolean; errorMessage:string }>()
const emit = defineEmits<{ submit:[payload:{username:string;password:string;challengeId:string;sliderOffset:number}] }>()
const formRef = ref<FormInstance>(); const sliderRef = ref<InstanceType<typeof SliderVerification>>(); const slider = ref<{challenge:SliderChallenge;offset:number}|null>(null)
const form = reactive({ username:'', password:'' })
const rules:FormRules<typeof form>={username:[{required:true,message:'请输入用户名',trigger:'blur'}],password:[{required:true,message:'请输入密码',trigger:'blur'}]}
async function submit(){const valid=await formRef.value?.validate().catch(()=>false);if(valid && slider.value) emit('submit',{...form,challengeId:slider.value.challenge.challengeId,sliderOffset:slider.value.offset})}
function refreshSlider(){slider.value=null;void sliderRef.value?.refresh()}
defineExpose({ refreshSlider })
</script>
<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
    <el-form-item label="用户名" prop="username"><el-input v-model="form.username" autocomplete="username" placeholder="请输入用户名" size="large" /></el-form-item>
    <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password autocomplete="current-password" placeholder="请输入密码" size="large" /></el-form-item>
    <el-form-item label="安全验证"><SliderVerification ref="sliderRef" @verified="(challenge, offset) => slider = { challenge, offset }" @reset="slider=null" /></el-form-item>
    <el-alert v-if="errorMessage" class="form-error" :title="errorMessage" type="error" :closable="false" show-icon />
    <el-button native-type="submit" type="primary" size="large" class="submit-button" :loading="loading" :disabled="!slider">登录系统</el-button>
  </el-form>
</template>
