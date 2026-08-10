<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchPracticeSession, submitPracticeSession, type PracticeSession } from '../../api/student/practice'
import { answerComplete, answerPayload, initialAnswers, type PracticeAnswerState } from './practiceAnswers'
import { PracticeElapsedTimer } from './practiceElapsed'
import type { ApiError } from '../../api/http'
import QuestionContent from '../../components/question/QuestionContent.vue'
import { subjectTheme } from '../../utils/subjectTheme'

const route = useRoute()
const router = useRouter()
const session = ref<PracticeSession | null>(null)
const loading = ref(true)
const submitting = ref(false)
const answers = reactive<PracticeAnswerState>({})
const current = ref(0)
const elapsedTimer = new PracticeElapsedTimer()
const question = computed(() => session.value?.questions[current.value])
const answeredCount = computed(() => session.value?.questions.filter(item => answerComplete(item.questionType, answers[item.practiceQuestionId] ?? '')).length ?? 0)
const environment = computed(() => subjectTheme(session.value?.subjectCode))
const typeLabel = (type: string) => ({ SINGLE_CHOICE: '单选题', MULTIPLE_CHOICE: '多选题', FILL_BLANK: '填空题' } as Record<string, string>)[type] || type
const typeInstruction = (type: string) => ({ SINGLE_CHOICE: '请选择 1 项。', MULTIPLE_CHOICE: '请选择所有正确项；全部选对得分，错选或漏选不得分。', FILL_BLANK: '请按顺序填写每个空。' } as Record<string, string>)[type] || ''
const difficultyLabel = (value: number) => ({ 1: '简单', 2: '中等', 3: '困难' } as Record<number, string>)[value] || String(value)

function openKnowledgePoint(pointId: number) {
  if (!session.value) return
  void router.push({ path: `/student/subjects/${session.value.subjectCode.toLowerCase()}`, query: { knowledgePointId: pointId } })
}

function errorMessage(error: unknown) {
  const api = error as ApiError
  return ({ PRACTICE_ALREADY_SUBMITTED: '该练习已经提交，请查看结果。', PRACTICE_ANSWER_COUNT_INVALID: '请完成本场所有题目后再提交。' } as Record<string, string>)[api.code || ''] || api.message || '练习请求失败。'
}

async function load() {
  loading.value = true
  try {
    const data = await fetchPracticeSession(Number(route.params.id))
    session.value = data
    Object.assign(answers, initialAnswers(data.questions))
    if (data.status === 'SUBMITTED') await router.replace(`/student/practice/${data.id}/result`)
  } catch (error) {
    ElMessage.error(errorMessage(error))
    await router.replace('/student/practice')
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!session.value) return
  const incomplete = session.value.questions.filter(item => !answerComplete(item.questionType, answers[item.practiceQuestionId] ?? ''))
  if (incomplete.length) {
    ElMessage.warning(`还有 ${incomplete.length} 题未完成。`)
    return
  }
  try {
    await ElMessageBox.confirm('提交后不能再次修改本场答案。确认提交吗？', '确认提交', { type: 'warning', confirmButtonText: '提交并查看结果', cancelButtonText: '继续作答' })
  } catch {
    return
  }
  elapsedTimer.pause()
  submitting.value = true
  try {
    await submitPracticeSession(session.value.id, { answers: session.value.questions.map(item => ({ practiceQuestionId: item.practiceQuestionId, answer: answerPayload(item, answers[item.practiceQuestionId]), elapsedSeconds: elapsedTimer.seconds(item.practiceQuestionId) })) })
    await router.replace(`/student/practice/${session.value.id}/result`)
  } catch (error) {
    if (question.value) elapsedTimer.enter(question.value.practiceQuestionId)
    ElMessage.error(errorMessage(error))
  } finally {
    submitting.value = false
  }
}

watch(() => question.value?.practiceQuestionId, questionId => { if (questionId) elapsedTimer.enter(questionId) })
onMounted(() => void load())
</script>

<template>
  <section class="student-page" :data-subject="environment" v-loading="loading">
    <template v-if="session && question">
      <div class="practice-progress">
        <div><h1>{{ session.subjectName }}自主练习</h1><p>第 {{ current + 1 }} / {{ session.questionCount }} 题 · 已完成 {{ answeredCount }} 题</p></div>
        <el-button @click="router.push('/student/practice/new')">退出本场</el-button>
      </div>
      <el-progress :percentage="Math.round(((current + 1) / session.questionCount) * 100)" :show-text="false" />
      <article class="practice-question">
        <div class="question-meta"><el-tag>{{ typeLabel(question.questionType) }}</el-tag><span>{{ typeInstruction(question.questionType) }}</span><span>{{ difficultyLabel(question.difficulty) }} · {{ question.score }} 分</span></div>
        <h2>{{ question.order }}. <QuestionContent :content="question.stem" :attachments="question.attachments" position="QUESTION" /></h2>
        <el-radio-group v-if="question.questionType === 'SINGLE_CHOICE'" v-model="answers[question.practiceQuestionId]" class="answer-options">
          <el-radio v-for="option in question.options" :key="option.label" :value="option.label">{{ option.label }}. <QuestionContent :content="option.content" :attachments="question.attachments" position="OPTION" /></el-radio>
        </el-radio-group>
        <el-checkbox-group v-else-if="question.questionType === 'MULTIPLE_CHOICE'" v-model="answers[question.practiceQuestionId]" class="answer-options">
          <el-checkbox v-for="option in question.options" :key="option.label" :value="option.label">{{ option.label }}. <QuestionContent :content="option.content" :attachments="question.attachments" position="OPTION" /></el-checkbox>
        </el-checkbox-group>
        <div v-else class="blank-answer"><el-input v-for="(_, index) in (answers[question.practiceQuestionId] as string[])" :key="index" v-model="(answers[question.practiceQuestionId] as string[])[index]" :placeholder="`第 ${index + 1} 空答案`" /></div>
        <div class="knowledge-chip-row"><span>知识点</span><el-button v-for="point in question.knowledgePoints" :key="point.id" class="knowledge-chip" round plain @click="openKnowledgePoint(point.id)">{{ point.path }}</el-button></div>
      </article>
      <div class="practice-navigation">
        <el-button :disabled="current === 0" @click="current--">上一题</el-button>
        <div><el-button v-for="item in session.questions" :key="item.practiceQuestionId" :type="item.practiceQuestionId === question.practiceQuestionId ? 'primary' : 'default'" circle @click="current = session!.questions.indexOf(item)">{{ item.order }}</el-button></div>
        <el-button v-if="current < session.questionCount - 1" type="primary" @click="current++">下一题</el-button>
        <el-button v-else type="success" :loading="submitting" @click="submit">提交答案</el-button>
      </div>
    </template>
  </section>
</template>
