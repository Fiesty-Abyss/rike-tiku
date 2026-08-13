<script setup lang="ts">
import { computed } from 'vue'
import type { Attachment, Option, QuestionType } from '../../api/student/practice'
import QuestionContent from './QuestionContent.vue'

const props = withDefaults(defineProps<{
  questionType: QuestionType
  value: unknown
  options?: Option[]
  attachments?: Attachment[]
}>(), {
  options: () => [],
  attachments: () => [],
})

const choiceAnswers = computed(() => {
  if (props.questionType === 'FILL_BLANK') return []
  const optionMap = new Map(props.options.map(option => [option.label.trim().toUpperCase(), option.content]))
  return optionLabels(props.value).map(label => ({ label, content: optionMap.get(label) }))
})

const blankAnswers = computed(() => {
  if (props.questionType !== 'FILL_BLANK') return []
  if (Array.isArray(props.value)) return props.value.map(value => text(value))
  const blanks = object(props.value)?.blanks
  if (!Array.isArray(blanks)) return []
  // acceptedAnswers 的首项是人工审核后的 canonical answer；其余仅参与确定性判分。
  return blanks.map(blank => {
    const accepted = object(blank)?.acceptedAnswers
    return Array.isArray(accepted) ? text(accepted[0]) : ''
  })
})
const malformed = computed(()=>props.questionType!=='SUBJECTIVE'&&typeof props.value==='string'&&props.value.trim().startsWith('{')&&choiceAnswers.value.length===0&&blankAnswers.value.length===0)

function optionLabels(value: unknown) {
  const raw = typeof value === 'string'
    ? [value]
    : Array.isArray(value)
      ? value
      : object(value)?.optionLabels
  return Array.isArray(raw)
    ? raw.map(text).map(label => label.trim().toUpperCase()).filter(Boolean)
    : []
}

function object(value: unknown): Record<string, unknown> | null {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
    ? value as Record<string, unknown>
    : null
}

function text(value: unknown) {
  return typeof value === 'string' ? value : ''
}
</script>

<template>
  <div class="answer-display" :aria-label="questionType === 'FILL_BLANK' ? '填空答案' : '选择题答案'">
    <span v-if="questionType==='SUBJECTIVE'" class="answer-display__subjective">本题不自动判分，请查看标准解析</span>
    <span v-else-if="malformed" class="answer-display__error">答案结构异常，禁止通过审核</span>
    <ol v-if="questionType !== 'FILL_BLANK'" class="answer-display__choices">
      <li v-for="answer in choiceAnswers" :key="answer.label">
        <b>{{ answer.label }}.</b>
        <QuestionContent
          v-if="answer.content"
          class="answer-display__content"
          :content="answer.content"
          :attachments="attachments"
          position="OPTION"
        />
        <span v-else class="answer-display__fallback" aria-label="选项快照内容缺失"></span>
      </li>
    </ol>
    <ol v-else class="answer-display__blanks">
      <li v-for="(answer, index) in blankAnswers" :key="index">
        <span>第 {{ index + 1 }} 空</span>
        <QuestionContent :content="answer" :attachments="attachments" position="ANSWER" />
      </li>
    </ol>
    <span v-if="questionType !== 'FILL_BLANK' && questionType!=='SUBJECTIVE' && !choiceAnswers.length && !malformed" class="answer-display__empty">不存在答案</span>
    <span v-if="questionType === 'FILL_BLANK' && !blankAnswers.length" class="answer-display__empty">未作答</span>
  </div>
</template>
