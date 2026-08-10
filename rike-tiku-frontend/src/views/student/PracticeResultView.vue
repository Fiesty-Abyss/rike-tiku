<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchPracticeResult, type PracticeResult } from '../../api/student/practice'
import { formatPracticeAnswer } from './practiceAnswerFormatter'
import type { ApiError } from '../../api/http'
import QuestionContent from '../../components/question/QuestionContent.vue'
import { subjectTheme } from '../../utils/subjectTheme'

const route = useRoute()
const router = useRouter()
const result = ref<PracticeResult | null>(null)
const loading = ref(true)
const onlyWrong = ref(false)
const currentQuestionId = ref<number | null>(null)
const analysisExpanded = ref(true)
const visibleQuestions = computed(() => result.value?.questions.filter(item => !onlyWrong.value || !item.correct) || [])
const currentIndex = computed(() => Math.max(0, visibleQuestions.value.findIndex(item => item.question.practiceQuestionId === currentQuestionId.value)))
const item = computed(() => visibleQuestions.value[currentIndex.value] || null)
const environment = computed(() => subjectTheme(result.value?.subjectCode))

function select(practiceQuestionId: number) {
  currentQuestionId.value = practiceQuestionId
}

function move(offset: number) {
  const target = visibleQuestions.value[currentIndex.value + offset]
  if (target) select(target.question.practiceQuestionId)
}

function openKnowledgePoint(pointId: number) {
  if (!result.value) return
  void router.push({ path: `/student/subjects/${result.value.subjectCode.toLowerCase()}`, query: { knowledgePointId: pointId } })
}

function similarPractice() {
  if (!result.value || !item.value) return
  const point = item.value.question.knowledgePoints[0]
  if (!point) return
  void router.push({ path: '/student/practice/new', query: { subjectCode: result.value.subjectCode, knowledgePointId: point.id, referenceQuestionId: item.value.question.questionId, count: 5 } })
}

watch(item, value => { analysisExpanded.value = value ? !value.correct : true })
watch(onlyWrong, () => {
  const first = visibleQuestions.value[0]
  currentQuestionId.value = first?.question.practiceQuestionId || null
})

onMounted(async () => {
  try {
    result.value = await fetchPracticeResult(Number(route.params.id))
    const firstWrong = result.value.questions.find(question => !question.correct)
    currentQuestionId.value = (firstWrong || result.value.questions[0])?.question.practiceQuestionId || null
  } catch (error) {
    const api = error as ApiError
    ElMessage.error(api.message || '练习结果加载失败。')
    await router.replace('/student/practice/new')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="student-page result-page" :data-subject="environment" v-loading="loading">
    <template v-if="result">
      <header class="result-summary">
        <div><span class="result-kicker">{{ result.subjectName }} · 本次练习</span><h1>{{ result.totalScore }} 分</h1><p>共 {{ result.totalCount }} 题，答对 {{ result.correctCount }} 题。</p></div>
        <div class="result-summary-actions"><el-switch v-model="onlyWrong" active-text="只看错题" :disabled="result.correctCount === result.totalCount" /><el-button type="primary" @click="router.push({path:'/student/practice/new',query:{subjectId:result.subjectId}})">再练一场</el-button></div>
      </header>
      <nav class="result-question-nav" aria-label="练习题号">
        <button v-for="record in visibleQuestions" :key="record.question.practiceQuestionId" type="button" :class="{ active: record.question.practiceQuestionId === item?.question.practiceQuestionId, wrong: !record.correct }" @click="select(record.question.practiceQuestionId)">{{ record.question.order }}</button>
      </nav>
      <el-empty v-if="!item" description="本次练习没有错题。" />
      <Transition v-else name="question-shift" mode="out-in">
      <article :key="item.question.practiceQuestionId" class="result-focus" :class="item.correct ? 'is-correct' : 'is-wrong'">
        <div class="result-focus-heading"><el-tag :type="item.correct ? 'success' : 'danger'">{{ item.correct ? '回答正确' : '需要复习' }}</el-tag><span>第 {{ item.question.order }} 题</span></div>
        <h2><QuestionContent :content="item.question.stem" :attachments="item.question.attachments" position="QUESTION" /></h2>
        <div class="answer-comparison"><div><span>你的答案</span><strong>{{ formatPracticeAnswer(item.question.questionType, item.studentAnswer) }}</strong></div><div><span>正确答案</span><strong>{{ formatPracticeAnswer(item.question.questionType, item.correctAnswer) }}</strong></div></div>
        <div class="knowledge-chip-row"><span>知识点</span><el-button v-for="point in item.question.knowledgePoints" :key="point.id" class="knowledge-chip" round plain @click="openKnowledgePoint(point.id)">{{ point.path }}</el-button></div>
        <section class="analysis-panel"><button type="button" class="analysis-toggle" :aria-expanded="analysisExpanded" @click="analysisExpanded = !analysisExpanded"><span>标准解析</span><span>{{ analysisExpanded ? '收起' : '展开' }}</span></button><div v-show="analysisExpanded" class="analysis-content"><QuestionContent :content="item.standardAnalysis" :attachments="item.question.attachments" position="STANDARD_ANALYSIS" /></div></section>
        <div class="result-next-actions"><el-button :disabled="currentIndex === 0" @click="move(-1)">上一题</el-button><el-button type="primary" plain @click="similarPractice">练习类似题</el-button><el-button :disabled="currentIndex >= visibleQuestions.length - 1" @click="move(1)">下一题</el-button></div>
      </article>
      </Transition>
    </template>
  </section>
</template>
