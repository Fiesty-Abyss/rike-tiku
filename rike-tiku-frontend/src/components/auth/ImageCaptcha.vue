<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import type { InputInstance } from 'element-plus'
import { fetchCaptchaChallenge, type CaptchaChallenge } from '../../api/auth'

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{
  'update:modelValue': [value: string]
  challenge: [challengeId: string]
}>()

const challenge = ref<CaptchaChallenge | null>(null)
const inputRef = ref<InputInstance>()
const loading = ref(false)

async function refresh() {
  if (loading.value) return
  loading.value = true
  const previousChallengeId = challenge.value?.challengeId
  emit('update:modelValue', '')
  try {
    challenge.value = await fetchCaptchaChallenge(previousChallengeId)
    emit('challenge', challenge.value.challengeId)
    await nextTick()
    inputRef.value?.focus()
  } finally {
    loading.value = false
  }
}

function updateCode(value: string) {
  emit('update:modelValue', value)
}

defineExpose({ refresh, focus: () => inputRef.value?.focus() })
onMounted(() => void refresh())
</script>

<template>
  <div class="image-captcha">
    <el-input
      ref="inputRef"
      :model-value="props.modelValue"
      autocomplete="off"
      maxlength="4"
      placeholder="请输入验证码"
      size="large"
      @update:model-value="updateCode"
    />
    <div class="captcha-picture">
      <button
        type="button"
        class="captcha-image-button"
        :disabled="loading || !challenge"
        title="看不清？换一张"
        aria-label="刷新验证码"
        @click="refresh"
      >
        <img v-if="challenge" :src="challenge.image" alt="图形验证码" />
        <span v-else class="captcha-loading">加载中</span>
      </button>
      <button type="button" class="captcha-refresh" :disabled="loading" @click="refresh">
        看不清？换一张
      </button>
    </div>
  </div>
</template>
