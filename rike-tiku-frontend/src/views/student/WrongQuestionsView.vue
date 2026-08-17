<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchPracticeOptions, fetchWrongQuestion, fetchWrongQuestions, retryWrongQuestion, type KnowledgePoint, type Subject, type WrongQuestion, type WrongQuestionDetail, type WrongQuestionFilterStatus } from '../../api/student/practice'
import type { ApiError } from '../../api/http'
import QuestionContent from '../../components/question/QuestionContent.vue'
import AnswerDisplay from '../../components/question/AnswerDisplay.vue'
import StandardAnalysis from '../../components/question/StandardAnalysis.vue'
import StudentAiLearningPanel from '../../components/student/StudentAiLearningPanel.vue'
import { subjectTheme } from '../../utils/subjectTheme'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const records = ref<WrongQuestion[]>([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const reviewStatus = ref<WrongQuestionFilterStatus>()
const subjects = ref<Subject[]>([])
const selectedSubjectId = ref<number>()
const knowledgePointId = ref<number>()
const knowledgePoints = ref<KnowledgePoint[]>([])
const detail = ref<WrongQuestionDetail | null>(null)
const visible = ref(false)
const routeSubjectCode = computed(() => String(route.query.subjectCode || '').trim().toUpperCase() || undefined)
const selectedSubject = computed(() => subjects.value.find(subject => subject.id === selectedSubjectId.value))
const subjectCode = computed(() => selectedSubject.value?.code)
const environment = computed(() => subjectTheme(detail.value?.wrongQuestion.subjectCode || subjectCode.value))
const state = (value: string) => ({ NEW: '新错题', REVIEWING: '复习中', MASTERED: '已掌握' } as Record<string, string>)[value] || value

async function load() {
  loading.value = true
  try { const data=await fetchWrongQuestions({subjectCode:subjectCode.value,knowledgePointId:knowledgePointId.value,status:reviewStatus.value,keyword:keyword.value||undefined,page:page.value-1,size:20});records.value=Array.isArray(data)?data:data.items;total.value=Array.isArray(data)?data.length:data.total }
  catch (error) { const api = error as ApiError; ElMessage.error(api.message || '错题本加载失败。') }
  finally { loading.value = false }
}

async function retry(item:WrongQuestion){try{const session=await retryWrongQuestion(item.questionId);await router.push({path:`/student/practice/${session.id}`,query:{fromWrongBook:'true',wrongQuestionId:String(item.questionId)}})}catch(error){ElMessage.error((error as ApiError).message||'暂时无法创建复习会话。')}}

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

async function loadKnowledgePoints(subjectId?: number) {
  if (!subjectId) { knowledgePoints.value = []; return }
  const data = await fetchPracticeOptions(subjectId)
  knowledgePoints.value = data.knowledgePoints
}

async function changeSubject(subjectId?: number) {
  selectedSubjectId.value = subjectId
  knowledgePointId.value = undefined
  page.value = 1
  await loadKnowledgePoints(subjectId)
  await load()
}

async function initializeFilters() {
  const data = await fetchPracticeOptions()
  subjects.value = data.subjects
  const routeSubject = subjects.value.find(subject => subject.code.toUpperCase() === routeSubjectCode.value)
  selectedSubjectId.value = routeSubject?.id
  await loadKnowledgePoints(routeSubject?.id)
  await load()
}

watch(routeSubjectCode, async code => {
  if (!subjects.value.length) return
  const routeSubject = subjects.value.find(subject => subject.code.toUpperCase() === code)
  if (routeSubject?.id === selectedSubjectId.value) return
  await changeSubject(routeSubject?.id)
})
onMounted(() => { void initializeFilters() })
</script>

<template>
  <section class="student-page wrong-book-page" :data-subject="environment">
    <div class="student-page-heading"><div><h1>{{ subjectCode ? '本学科错题' : '错题本' }}</h1><p>按学科、关键词、复习状态和知识点复习；只有再做正确后才会询问是否移出。</p></div><el-button @click="router.push({path:'/student/practice/new',query:subjectCode?{subjectCode}:undefined})">创建练习</el-button></div>
    <div class="wrong-book-filters"><el-select v-model="selectedSubjectId" data-testid="wrong-subject-filter" clearable placeholder="全部学科" @change="changeSubject"><el-option v-for="subject in subjects" :key="subject.id" :label="subject.name" :value="subject.id"/></el-select><el-input v-model="keyword" clearable placeholder="搜索题干关键词" @keyup.enter="page=1;load()"/><el-select v-model="reviewStatus" clearable placeholder="复习状态" @change="page=1;load()"><el-option label="活跃错题" value="ACTIVE"/><el-option label="已掌握/已归档" value="MASTERED"/></el-select><el-select v-model="knowledgePointId" data-testid="wrong-knowledge-filter" clearable filterable :disabled="!selectedSubjectId" :placeholder="selectedSubjectId ? '按知识点完整路径' : '请先选择学科'"><el-option v-for="point in knowledgePoints" :key="point.id" :label="point.path" :value="point.id"/></el-select><el-button type="primary" @click="page=1;load()">筛选</el-button><span>当前筛选 {{ total }} 题</span></div>
    <el-table v-loading="loading" :data="records" class="data-table" empty-text="当前筛选下没有错题。"><el-table-column prop="subjectName" label="学科" width="100"/><el-table-column prop="stemSummary" label="题干摘要" min-width="260" show-overflow-tooltip /><el-table-column label="知识点" min-width="220"><template #default="{row}"><span>{{ (row.knowledgePoints||[]).map((p:any)=>p.path).join('；')||'未关联' }}</span></template></el-table-column><el-table-column prop="errorCount" label="错误次数" width="90"/><el-table-column label="状态" width="100"><template #default="{row}"><el-tag>{{ state(row.status) }}</el-tag></template></el-table-column><el-table-column label="操作" width="180"><template #default="{row}"><el-button link type="primary" @click="show(row)">详情</el-button><el-button link type="primary" @click="retry(row)">再做一次</el-button></template></el-table-column></el-table>
    <el-pagination v-if="total>20" v-model:current-page="page" :page-size="20" :total="total" layout="prev, pager, next" @current-change="load"/>
    <el-drawer v-model="visible" title="错题详情" size="min(760px,100%)"><template v-if="detail"><h2><QuestionContent :content="detail.stem" :attachments="detail.attachments" position="QUESTION" /></h2><el-table v-if="detail.options.length" :data="detail.options" class="data-table"><el-table-column prop="label" label="选项" width="90"/><el-table-column label="内容"><template #default="{row}"><QuestionContent :content="row.content" :attachments="detail.attachments" position="OPTION" /></template></el-table-column></el-table><div class="answer-comparison"><div><span>最近答案</span><AnswerDisplay :question-type="detail.wrongQuestion.questionType" :value="detail.latestStudentAnswer" :options="detail.options" :attachments="detail.attachments" /></div><div><span>正确答案</span><AnswerDisplay :question-type="detail.wrongQuestion.questionType" :value="detail.correctAnswer" :options="detail.options" :attachments="detail.attachments" /></div></div><section class="analysis-panel"><h3>标准解析</h3><StandardAnalysis :content="detail.standardAnalysis" :attachments="detail.attachments" /></section><StudentAiLearningPanel :answer-fact-id="detail.aiAnalysisAnswerFactId" :wrong="true" /><div class="knowledge-chip-row"><span>知识点</span><el-button v-for="point in detail.knowledgePoints" :key="point.id" class="knowledge-chip" round plain @click="openKnowledgePoint(point.id)">{{ point.path }}</el-button></div><el-button type="primary" plain @click="similarPractice">练习类似题</el-button></template></el-drawer>
  </section>
</template>

<style scoped>
.wrong-book-filters{display:grid;grid-template-columns:150px minmax(180px,1.2fr) 150px minmax(220px,1.3fr) auto auto;gap:12px;align-items:center;margin:18px 0}.el-pagination{justify-content:flex-end;margin-top:18px}@media(max-width:1180px){.wrong-book-filters{grid-template-columns:repeat(4,minmax(0,1fr))}}@media(max-width:760px){.wrong-book-filters{grid-template-columns:1fr}.wrong-book-filters>*{width:100%}}
</style>
