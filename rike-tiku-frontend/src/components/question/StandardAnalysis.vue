<script setup lang="ts">
import { computed } from 'vue'
import type { Attachment } from '../../api/student/practice'
import QuestionContent from './QuestionContent.vue'

const props = withDefaults(defineProps<{ content: string; attachments?: Attachment[] }>(), {
  attachments: () => [],
})

const heading = /^(解题思路|结论|易错点|关键依据|已知量|使用规律|代入|单位|结果|物理意义|流程目标|材料信息|推理依据|分步判断|步骤\s*[一二三四五六七八九十\d]+)(?:\s*[：:]\s*(.*))?$/
const blocks = computed(() => props.content
  .replace(/\r\n?/g, '\n')
  .split(/\n+/)
  .map(value => value.trim())
  .filter(Boolean)
  .map((value) => {
    const match = value.match(heading)
    return match ? { heading: match[1], content: match[2] || '' } : { content: value }
  }))
</script>

<template>
  <div class="standard-analysis" aria-label="标准解析正文">
    <section v-for="(block, index) in blocks" :key="`${index}-${block.heading || ''}`" class="standard-analysis__block">
      <h4 v-if="block.heading">{{ block.heading }}</h4>
      <p v-if="block.content">
        <QuestionContent :content="block.content" :attachments="attachments" position="STANDARD_ANALYSIS" />
      </p>
    </section>
  </div>
</template>
