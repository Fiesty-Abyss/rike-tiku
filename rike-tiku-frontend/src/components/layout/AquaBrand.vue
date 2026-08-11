<script setup lang="ts">
withDefaults(defineProps<{
  to?: string
  subtitle?: string
  compact?: boolean
}>(), {
  to: '/',
  subtitle: '理科学习辅助系统',
  compact: false,
})
</script>

<template>
  <RouterLink class="aqua-brand" :class="{ 'aqua-brand--compact': compact }" :to="to" aria-label="RIKE 理科学习辅助系统">
    <span class="aqua-brand__mark aero-orb" aria-hidden="true"><i></i></span>
    <span class="aqua-brand__copy"><strong>RIKE</strong><small>{{ subtitle }}</small></span>
  </RouterLink>
</template>
