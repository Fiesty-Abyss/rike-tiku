<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchSliderChallenge, type SliderChallenge } from '../../api/auth'

const emit = defineEmits<{
  verified: [challenge: SliderChallenge, offset: number]
  reset: []
}>()
const challenge = ref<SliderChallenge | null>(null)
const offset = ref(0)
const verified = ref(false)
const loading = ref(false)
const maxOffset = computed(() => Math.max(
  0,
  (challenge.value?.canvasWidth ?? 320) - (challenge.value?.targetWidth ?? 44),
))

async function refresh() {
  loading.value = true
  verified.value = false
  offset.value = 0
  emit('reset')
  try {
    challenge.value = await fetchSliderChallenge()
  } finally {
    loading.value = false
  }
}

function finish() {
  if (!challenge.value) return
  verified.value = true
  emit('verified', challenge.value, offset.value)
}

defineExpose({ refresh })
onMounted(() => void refresh())
</script>

<template>
  <div class="slider-verify" :class="{ verified }">
    <div
      class="slider-track"
      :style="{ width: `${challenge?.canvasWidth ?? 320}px` }"
      aria-label="滑块验证区域"
    >
      <span
        class="slider-target"
        :style="{ left: `${challenge?.targetDisplayOffset ?? 0}px`, width: `${challenge?.targetWidth ?? 44}px` }"
      ></span>
      <span
        class="slider-piece"
        :style="{ left: `${offset}px`, width: `${challenge?.targetWidth ?? 44}px` }"
      >◈</span>
    </div>
    <div class="slider-control">
      <el-slider
        v-model="offset"
        :min="0"
        :max="maxOffset"
        :disabled="loading || verified"
        @change="finish"
      />
      <el-button link :loading="loading" @click="refresh">刷新</el-button>
    </div>
    <p>{{ verified ? '滑块已完成，请登录。' : '拖动滑块，使图形与目标位置对齐。' }}</p>
  </div>
</template>
