<script setup lang="ts">
import { useRouter } from 'vue-router'
import { teacherAiGenerationClient } from '../../api/teacher/aiGeneration'
import AiQuestionGenerationWorkspace from '../../components/ai/AiQuestionGenerationWorkspace.vue'
import AquaBrand from '../../components/layout/AquaBrand.vue'

const router=useRouter()
</script>

<template>
  <main class="workspace-page teacher-ai-page">
    <header class="workspace-header teacher-header">
      <AquaBrand class="workspace-brand-aqua" to="/teacher" subtitle="教师科学工作台" compact />
      <el-button type="primary" plain @click="router.push('/teacher')">返回教师工作台</el-button>
    </header>
    <section class="workspace-content">
      <AiQuestionGenerationWorkspace mode="TEACHER" :client="teacherAiGenerationClient" />
    </section>
  </main>
</template>

<style scoped>
.teacher-ai-page{min-height:100vh}.workspace-content{max-width:1440px;margin:0 auto;padding:28px clamp(18px,4vw,54px) 48px}
</style>
