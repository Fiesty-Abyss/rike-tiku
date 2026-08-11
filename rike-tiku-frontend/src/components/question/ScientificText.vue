<script setup lang="ts">
import { computed } from 'vue'
import MathFragment from './MathFragment.vue'
import { parseScientificText } from './scientificText'

const props = defineProps<{ content: string }>()
const segments = computed(() => parseScientificText(props.content))
</script>

<template>
  <span class="scientific-text">
    <template v-for="(segment, index) in segments" :key="index">
      <span v-if="segment.type === 'text'" class="scientific-text__plain">{{ segment.value }}</span>
      <MathFragment v-else :expression="segment.value" :display="segment.display" />
    </template>
  </span>
</template>
