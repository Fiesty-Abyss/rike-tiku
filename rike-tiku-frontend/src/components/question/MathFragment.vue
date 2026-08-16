<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import katex from 'katex'
import 'katex/contrib/mhchem'

const props = withDefaults(defineProps<{ expression: string; display?: boolean }>(), { display: false })
const host = ref<HTMLElement>()

function renderMath() {
  if (!host.value) return
  host.value.replaceChildren()
  try {
    katex.render(props.expression, host.value, {
      displayMode: props.display,
      output: 'htmlAndMathml',
      throwOnError: true,
      strict: 'ignore',
      trust: false,
      maxExpand: 1000,
      maxSize: 20,
    })
    host.value.dataset.renderStatus = 'rendered'
  } catch {
    host.value.textContent = props.expression
    host.value.dataset.renderStatus = 'fallback'
  }
}

watch(() => [props.expression, props.display], renderMath)
onMounted(renderMath)
</script>

<template>
  <span
    ref="host"
    class="math-fragment"
    :class="{ 'math-fragment--display': display }"
    :aria-label="expression"
  />
</template>
