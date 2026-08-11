<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import aquaWorld from '../../assets/aqua/rike-aqua-world.webp'
import AquaBrand from '../../components/layout/AquaBrand.vue'
import { useAuthStore } from '../../stores/auth'
import { formatEnum, roleHome } from '../../utils/formatters'
import { useEntranceMotion } from '../../utils/entranceMotion'

const router = useRouter()
const auth = useAuthStore()
const roles = computed(() => auth.roles)
const root = ref<HTMLElement>()
useEntranceMotion(root, '.role-select', 0)

async function select(role: typeof roles.value[number]) {
  auth.selectRole(role)
  await router.replace(roleHome(role))
}
</script>

<template>
  <main ref="root" class="single-panel-page role-selection-page">
    <img class="role-selection-world" :src="aquaWorld" width="1586" height="992" loading="eager" decoding="async" alt="RIKE 清水科学世界中的多角色入口" />
    <span class="role-selection-light" aria-hidden="true"></span>
    <section class="role-select aero-glass-heavy">
      <AquaBrand class="role-select-brand" subtitle="多角色权限入口" />
      <p class="role-select-kicker">ACCESS ORBIT</p>
      <h1>选择本次进入的身份</h1>
      <p>只显示账号实际拥有的角色；切换入口不会改变后端权限事实。</p>
      <div class="role-choices">
        <el-button
          v-for="role in roles"
          :key="role"
          size="large"
          @click="select(role)"
        >
          <span class="role-choice-code">{{ role }}</span>
          <span>{{ formatEnum(role) }}工作台</span>
          <i aria-hidden="true">进入</i>
        </el-button>
      </div>
    </section>
  </main>
</template>
