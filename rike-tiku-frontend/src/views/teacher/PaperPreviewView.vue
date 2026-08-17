<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { fetchPaper, type Paper } from '../../api/teacher/papers'
import QuestionContent from '../../components/question/QuestionContent.vue'
import StandardAnalysis from '../../components/question/StandardAnalysis.vue'
import AnswerDisplay from '../../components/question/AnswerDisplay.vue'
import { questionTypeLabel, topicTypeLabel } from '../../utils/questionLabels'

const route = useRoute()
const paper = ref<Paper>()
const answer = computed(() => route.params.version === 'answer')
onMounted(async () => { paper.value = await fetchPaper(Number(route.params.id)) })
</script>

<template>
  <main v-if="paper" class="paper-preview">
    <header>
      <div><h1>{{ paper.name }}</h1><p>{{ paper.subjectName }} · 总分 {{ paper.totalScore }} 分</p></div>
      <button type="button" @click="window.print()">打印 / 另存为 PDF</button>
    </header>
    <article v-for="question in paper.questions" :key="question.id" :class="['paper-question', { subjective: question.type === 'SUBJECTIVE' }]">
      <div class="question-meta"><strong>{{ question.order }}.</strong><el-tag size="small" effect="plain">{{ questionTypeLabel(question.type) }}</el-tag><el-tag v-if="question.topicType" size="small" type="warning" effect="plain">{{ topicTypeLabel(question.topicType) }}</el-tag><span>（{{ question.score }} 分）</span></div>
      <QuestionContent :content="question.stem" :attachments="question.stemAttachments" position="QUESTION" />
      <ol v-if="question.options.length" class="options"><li v-for="option in question.options" :key="option.label">{{ option.label }}. <QuestionContent :content="option.content" position="QUESTION" /></li></ol>
      <div v-else class="answer-space"><span v-if="question.type === 'SUBJECTIVE'">请分步作答：</span></div>
      <section v-if="answer" class="paper-answer"><strong>正确答案</strong><AnswerDisplay :question-type="question.type" :value="question.correctAnswer" :options="question.options" /><strong>STANDARD 解析</strong><StandardAnalysis :content="question.standardAnalysis" :attachments="question.analysisAttachments" /><p>知识点：{{ question.knowledgePoints.join('、') }} · 难度 {{ question.difficulty }}</p></section>
    </article>
  </main>
</template>

<style scoped>
@page{size:A4;margin:16mm}.paper-preview{max-width:850px;margin:auto;padding:28px;color:#111;background:#fff}.paper-preview header{display:flex;justify-content:space-between;gap:20px;border-bottom:2px solid #111}.paper-question{break-inside:avoid;margin:22px 0}.question-meta{display:flex;align-items:center;flex-wrap:wrap;gap:8px;margin-bottom:9px;font-size:16px}.options{display:grid;grid-template-columns:1fr 1fr;gap:8px}.answer-space{height:64px;margin-top:12px;padding-top:14px;border-bottom:1px solid #bbb;color:#666}.paper-question.subjective .answer-space{height:220px;line-height:2;background:repeating-linear-gradient(to bottom,transparent 0,transparent 31px,#d7d7d7 32px)}.paper-answer{break-inside:avoid;margin-top:12px;padding:12px;border-left:3px solid #236b64;background:#f3f7f6}.paper-answer>:deep(.question-content__image),.paper-question>:deep(.question-content__image){max-width:100%;max-height:500px;break-inside:avoid;page-break-inside:avoid}@media print{header button{display:none}.paper-preview{padding:0;max-width:none}.paper-answer{print-color-adjust:exact}.paper-question :deep(img){break-inside:avoid;page-break-inside:avoid}}
</style>
