<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { formatEnum, roleHome } from '../../utils/formatters'
import { useEntranceMotion } from '../../utils/entranceMotion'

const router = useRouter()
const auth = useAuthStore()
const roles = computed(() => auth.roles)
const root = ref<HTMLElement>()
useEntranceMotion(root, '.role-select > *', 0.08)

async function select(role: typeof roles.value[number]) {
  auth.selectRole(role)
  await router.replace(roleHome(role))
}
</script>

<template>
  <main ref="root" class="single-panel-page role-selection-page">
    <section class="role-select">
      <RouterLink class="role-select-brand" to="/">
        <span aria-hidden="true">理科</span>
        <strong>RIKE 学习辅助系统</strong>
      </RouterLink>
      <h1>选择本次进入的身份</h1>
      <p>一个账号可以拥有多个角色。仅可选择账号实际拥有的角色，后端权限仍以数据库为准。</p>
      <div class="role-choices">
        <el-button
          v-for="role in roles"
          :key="role"
          size="large"
          @click="select(role)"
        >
          {{ formatEnum(role) }}工作台
        </el-button>
      </div>
    </section>
  </main>
</template>
