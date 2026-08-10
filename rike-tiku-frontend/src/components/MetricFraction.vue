<script setup lang="ts">
defineProps<{ numerator: number | string; denominator: number | string; label?: string }>()
</script>

<template>
  <span class="metric-fraction" role="math" :aria-label="label || `${numerator} / ${denominator}`">
    <span class="metric-fraction__numerator">{{ numerator }}</span>
    <span class="metric-fraction__bar" aria-hidden="true"></span>
    <span class="metric-fraction__denominator">{{ denominator }}</span>
  </span>
</template>
