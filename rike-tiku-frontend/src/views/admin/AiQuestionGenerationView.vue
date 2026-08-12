<script setup lang="ts">
import { adminAiGenerationClient } from '../../api/admin/aiGeneration'
import AiQuestionGenerationWorkspace from '../../components/ai/AiQuestionGenerationWorkspace.vue'
</script>

<template>
  <AiQuestionGenerationWorkspace mode="ADMIN" :client="adminAiGenerationClient" />
</template>
