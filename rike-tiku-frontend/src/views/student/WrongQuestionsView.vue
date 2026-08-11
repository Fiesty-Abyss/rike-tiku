<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchWrongQuestion, fetchWrongQuestions, type WrongQuestion, type WrongQuestionDetail } from '../../api/student/practice'
import type { ApiError } from '../../api/http'
import QuestionContent from '../../components/question/QuestionContent.vue'
import AnswerDisplay from '../../components/question/AnswerDisplay.vue'
import StandardAnalysis from '../../components/question/StandardAnalysis.vue'
import { subjectTheme } from '../../utils/subjectTheme'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const records = ref<WrongQuestion[]>([])
const detail = ref<WrongQuestionDetail | null>(null)
const visible = ref(false)
const subjectCode = computed(() => String(route.query.subjectCode || '').trim().toUpperCase() || undefined)
const environment = computed(() => subjectTheme(detail.value?.wrongQuestion.subjectCode || subjectCode.value))
const state = (value: string) => ({ NEW: '新错题', REVIEWING: '复习中', MASTERED: '已掌握' } as Record<string, string>)[value] || value

async function load() {
  loading.value = true
  try { records.value = await fetchWrongQuestions(subjectCode.value) }
  catch (error) { const api = error as ApiError; ElMessage.error(api.message || '错题本加载失败。') }
  finally { loading.value = false }
}

async function show(item: WrongQuestion) {
  try { detail.value = await fetchWrongQuestion(item.questionId); visible.value = true }
  catch (error) { const api = error as ApiError; ElMessage.error(api.message || '错题详情加载失败。') }
}

function openKnowledgePoint(pointId: number) {
  if (!detail.value) return
  void router.push({ path: `/student/subjects/${detail.value.wrongQuestion.subjectCode.toLowerCase()}`, query: { knowledgePointId: pointId } })
}

function similarPractice() {
  if (!detail.value) return
  const point = detail.value.knowledgePoints[0]
  if (!point) return
  visible.value = false
  void router.push({ path: '/student/practice/new', query: { subjectCode: detail.value.wrongQuestion.subjectCode, knowledgePointId: point.id, referenceQuestionId: detail.value.wrongQuestion.questionId, count: 5 } })
}

watch(subjectCode, () => void load())
onMounted(() => void load())
</script>

<template>
  <section class="student-page wrong-book-page" :data-subject="environment">
    <div class="student-page-heading"><div><h1>{{ subjectCode ? '本学科错题' : '错题本' }}</h1><p>提交后实时更新；连续两次答对后标为已掌握，但不删除历史记录。</p></div><el-button @click="router.push({path:'/student/practice/new',query:subjectCode?{subjectCode}:undefined})">创建练习</el-button></div>
    <el-table v-loading="loading" :data="records" class="data-table" empty-text="暂时没有错题，继续保持。"><el-table-column prop="subjectName" label="学科" /><el-table-column prop="stemSummary" label="题干摘要" min-width="300" show-overflow-tooltip /><el-table-column prop="errorCount" label="错误次数" /><el-table-column prop="consecutiveCorrectCount" label="连续正确" /><el-table-column label="状态"><template #default="{row}"><el-tag>{{ state(row.status) }}</el-tag></template></el-table-column><el-table-column label="操作"><template #default="{row}"><el-button link type="primary" @click="show(row)">查看详情</el-button></template></el-table-column></el-table>
    <el-drawer v-model="visible" title="错题详情" size="min(760px,100%)"><template v-if="detail"><h2><QuestionContent :content="detail.stem" :attachments="detail.attachments" position="QUESTION" /></h2><el-table v-if="detail.options.length" :data="detail.options" class="data-table"><el-table-column prop="label" label="选项" width="90"/><el-table-column label="内容"><template #default="{row}"><QuestionContent :content="row.content" :attachments="detail.attachments" position="OPTION" /></template></el-table-column></el-table><div class="answer-comparison"><div><span>最近答案</span><AnswerDisplay :question-type="detail.wrongQuestion.questionType" :value="detail.latestStudentAnswer" :options="detail.options" :attachments="detail.attachments" /></div><div><span>正确答案</span><AnswerDisplay :question-type="detail.wrongQuestion.questionType" :value="detail.correctAnswer" :options="detail.options" :attachments="detail.attachments" /></div></div><section class="analysis-panel"><h3>标准解析</h3><StandardAnalysis :content="detail.standardAnalysis" :attachments="detail.attachments" /></section><div class="knowledge-chip-row"><span>知识点</span><el-button v-for="point in detail.knowledgePoints" :key="point.id" class="knowledge-chip" round plain @click="openKnowledgePoint(point.id)">{{ point.path }}</el-button></div><el-button type="primary" plain @click="similarPractice">练习类似题</el-button></template></el-drawer>
  </section>
</template>
