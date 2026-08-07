<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { formatEnum, roleHome } from '../../utils/formatters'
const router=useRouter();const auth=useAuthStore();const roles=computed(()=>auth.roles)
async function select(role: typeof roles.value[number]){auth.selectRole(role);await router.replace(roleHome(role))}
</script>
<template><main class="single-panel-page"><section class="role-select"><h1>选择本次进入的身份</h1><p>一个账号可以拥有多个角色。仅可选择账号实际拥有的角色，后端权限仍以数据库为准。</p><div class="role-choices"><el-button v-for="role in roles" :key="role" size="large" @click="select(role)">{{ formatEnum(role) }}工作台</el-button></div></section></main></template>
