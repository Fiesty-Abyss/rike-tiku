<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { fetchHealth, type HealthStatus } from '../api/health'
import { useAppStore } from '../stores/app'

const appStore = useAppStore()
const health = ref<HealthStatus>({ status: 'CHECKING', database: 'CHECKING' })
const errorMessage = ref('')
const loading = ref(false)

const isHealthy = computed(
  () => health.value.status === 'UP' && health.value.database === 'UP',
)

async function loadHealth() {
  loading.value = true
  errorMessage.value = ''
  try {
    health.value = await fetchHealth()
  } catch (error) {
    health.value = { status: 'DOWN', database: 'DOWN' }
    errorMessage.value = '无法连接后端健康接口，请确认后端和数据库已经启动。'
  } finally {
    loading.value = false
  }
}

onMounted(loadHealth)
</script>

<template>
  <main class="page-shell">
    <el-card class="health-card" shadow="never">
      <h1>{{ appStore.projectName }}</h1>
      <p class="subtitle">前后端基础工程连通性验证</p>

      <el-alert
        :title="isHealthy ? '系统连接正常' : '系统连接尚未就绪'"
        :type="isHealthy ? 'success' : 'warning'"
        :closable="false"
        show-icon
      />

      <el-descriptions class="status-list" :column="1" border>
        <el-descriptions-item label="后端状态">
          <el-tag :type="health.status === 'UP' ? 'success' : 'danger'">
            {{ health.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="数据库状态">
          <el-tag :type="health.database === 'UP' ? 'success' : 'danger'">
            {{ health.database }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
      <el-button type="primary" :loading="loading" @click="loadHealth">
        重新检查
      </el-button>
    </el-card>
  </main>
</template>

