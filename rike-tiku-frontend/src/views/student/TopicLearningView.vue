<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { discardTopicVariant, fetchTopic, fetchTopicUnit, fetchTopicUnits, generateTopicVariants, submitTopicVariant, type TopicDetail, type TopicUnitDetail, type TopicUnitItem, type TopicVariantTask } from '../../api/student/topicLearning'
import type { ApiError } from '../../api/http'
import StandardAnalysis from '../../components/question/StandardAnalysis.vue'
import QuestionContent from '../../components/question/QuestionContent.vue'
import StudentAiLearningPanel from '../../components/student/StudentAiLearningPanel.vue'
import { subjectTheme } from '../../utils/subjectTheme'
import { questionTypeLabel, topicTypeLabel } from '../../utils/questionLabels'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const units = ref<TopicUnitItem[]>([])
const unit = ref<TopicUnitDetail | null>(null)
const unitIndex = ref(0)
const detail = ref<TopicDetail | null>(null)
const analysisVisible = ref(false)
const draft = ref('')
const variantVisible = ref(false)
const variantLoading = ref(false)
const variantTask = ref<TopicVariantTask | null>(null)
const variantForm = ref({ targetDifficulty:3, variationMode:'SCENARIO_TRANSFER', count:1, requireVisualContext:false, keepPrimaryKnowledgePoint:true })
const subjectCode = ref(String(route.query?.subjectCode || ''))
const subjectOptions = [{label:'全部学科',value:''},{label:'物理',value:'PHYSICS'},{label:'化学',value:'CHEMISTRY'},{label:'生物',value:'BIOLOGY'}]
const variationModes = [{label:'情境迁移',value:'SCENARIO_TRANSFER'},{label:'条件重组',value:'CONDITION_RECOMBINATION'},{label:'表达形式转换',value:'REPRESENTATION_SWITCH'},{label:'多步骤扩展',value:'MULTI_STEP_EXTENSION'},{label:'干扰项重构',value:'DISTRACTOR_REDESIGN'},{label:'综合变式',value:'COMBINED'}]
const environment = computed(() => subjectTheme(detail.value?.subjectCode || subjectCode.value))
const activeUnitQuestion = computed(() => unit.value?.questions[unitIndex.value])
const stageLabel = (value:string) => ({ FOUNDATION:'基础理解', TRANSFER:'情境迁移', ADVANCED:'综合提升' } as Record<string,string>)[value] || value
const difficultyLabel = (value:number) => ({1:'基础',2:'进阶',3:'挑战',4:'较难',5:'困难'} as Record<number,string>)[value] || String(value)

function resetQuestion(){ analysisVisible.value=false; draft.value=''; variantTask.value=null; variantVisible.value=false }
async function loadList(){
  try { units.value = await fetchTopicUnits(subjectCode.value || undefined) }
  catch (error) { ElMessage.error((error as ApiError).message || '专题单元加载失败。') }
}
async function loadDetail(){
  const unitId = Number(route.params.unitId)
  loading.value = true
  try {
    if(unitId){
      unit.value = await fetchTopicUnit(unitId)
      unitIndex.value = 0
      resetQuestion()
      const first = unit.value.questions[0]
      detail.value = first ? await fetchTopic(first.question.id) : null
    } else {
      unit.value = null
      const id = Number(route.params.id)
      detail.value = id ? await fetchTopic(id) : null
      resetQuestion()
    }
  } catch(error) {
    ElMessage.error((error as ApiError).message || '专题内容加载失败。')
    await router.replace('/student/topics')
  } finally { loading.value = false }
}
async function chooseUnitQuestion(index:number){
  if(!unit.value || index<0 || index>=unit.value.questions.length || index===unitIndex.value)return
  unitIndex.value=index;resetQuestion();loading.value=true
  try { detail.value=await fetchTopic(unit.value.questions[index].question.id) }
  catch(error){ElMessage.error((error as ApiError).message || '专题题目加载失败。')}
  finally{loading.value=false}
}
function chooseSubject(){ void router.push({path:'/student/topics',query:subjectCode.value?{subjectCode:subjectCode.value}:undefined}); void loadList() }
function openKnowledgePoint(pointId:number){if(!detail.value)return;void router.push({path:`/student/subjects/${detail.value.subjectCode.toLowerCase()}`,query:{knowledgePointId:pointId}})}
function practiceKnowledgePoint(pointId:number){if(!detail.value)return;void router.push({path:'/student/practice/new',query:{subjectCode:detail.value.subjectCode,knowledgePointId:pointId,count:5}})}
async function generateVariants(){
  if(!detail.value || variantLoading.value)return
  variantLoading.value=true
  try { variantTask.value=await generateTopicVariants(detail.value.id,variantForm.value); variantVisible.value=true; ElMessage.success('候选题已生成，请先检查后再提交任课老师审核。') }
  catch(error){ElMessage.error((error as ApiError).message || '专题变式生成失败。')}
  finally{variantLoading.value=false}
}
async function submitCandidate(questionId:number){
  try { variantTask.value=await submitTopicVariant(questionId); ElMessage.success('候选题已提交任课老师审核，尚未发布。') }
  catch(error){ElMessage.error((error as ApiError).message || '提交任课老师审核失败。')}
}
async function discardCandidate(questionId:number){
  try { await discardTopicVariant(questionId); variantTask.value=null; ElMessage.info('候选题已丢弃，未进入题库或审核队列。') }
  catch(error){ElMessage.error((error as ApiError).message || '丢弃候选题失败。')}
}
watch(() => [route.params.id,route.params.unitId], () => void loadDetail())
watch(() => route.query?.subjectCode, value => { subjectCode.value=String(value || ''); void loadList() })
onMounted(async()=>{await loadList();await loadDetail()})
</script>

<template>
  <section class="student-page topic-learning-page" :data-subject="environment" v-loading="loading">
    <div class="student-page-heading"><div><span class="page-kicker">TOPIC LEARNING</span><h1>主观专题学习</h1><p>专题页按单元组织 2 至 3 道主观题；草稿只保留在本页，不提交、不评分。</p></div><el-select v-model="subjectCode" class="topic-subject-select" @change="chooseSubject"><el-option v-for="option in subjectOptions" :key="option.value" :label="option.label" :value="option.value" /></el-select></div>
    <div class="topic-learning-layout">
      <aside class="topic-index"><section><h2>专题单元</h2><button v-for="item in units" :key="item.id" type="button" :class="{active:item.id===unit?.id}" @click="router.push({path:`/student/topics/units/${item.id}`,query:subjectCode?{subjectCode}:undefined})"><span>{{ item.title }}</span><small>{{ item.subjectName }} · {{ item.questionCount }} 题 · {{ difficultyLabel(item.difficulty) }}</small><small>{{ item.primaryKnowledgePoint.path }}</small></button><el-empty v-if="!units.length" description="暂无可访问的专题单元" /></section></aside>
      <main class="topic-reader">
        <el-empty v-if="!detail" description="从左侧选择专题单元开始阅读。" />
        <article v-else>
          <header><span v-if="unit">{{ unit.subjectName }} · {{ stageLabel(activeUnitQuestion?.stage || '') }} · 第 {{ unitIndex + 1 }} / {{ unit.questions.length }} 题</span><span v-else>{{ detail.subjectName }} · 主观专题</span><h2>{{ unit?.title || detail.title }}</h2><p v-if="unit">{{ unit.introduction }}</p><el-tag type="warning" effect="plain">{{ topicTypeLabel(detail.topicType) }}</el-tag></header>
          <div class="topic-material"><h3>材料与问题</h3><QuestionContent :content="detail.material" :attachments="detail.stemAttachments" position="QUESTION" /></div>
          <div class="topic-draft"><h3>自我作答草稿</h3><p>切换专题题目即清空，不上传、不自动评分。</p><el-input v-model="draft" type="textarea" :rows="8" maxlength="5000" show-word-limit placeholder="在这里整理计算、流程或分析步骤……" /></div>
          <div class="knowledge-chip-row"><span>关联知识点</span><el-button v-for="point in detail.knowledgePoints" :key="point.id" class="knowledge-chip" round plain @click="openKnowledgePoint(point.id)">{{ point.path }}</el-button></div>
          <div class="topic-actions"><el-button v-if="!analysisVisible" type="primary" @click="analysisVisible=true">查看标准解析</el-button><el-button v-else plain @click="analysisVisible=false">收起标准解析</el-button><el-button plain @click="variantVisible=true">生成专题变式</el-button><el-button v-if="detail.knowledgePoints[0]" plain @click="practiceKnowledgePoint(detail.knowledgePoints[0].id)">练习相关知识点</el-button></div>
          <section v-if="analysisVisible" class="topic-analysis"><h3>标准解析</h3><StandardAnalysis :content="detail.standardAnalysis" :attachments="detail.analysisAttachments" /></section>
          <div v-if="unit" class="topic-unit-navigation"><el-button :disabled="unitIndex===0" @click="chooseUnitQuestion(unitIndex-1)">上一题</el-button><el-button @click="router.push({path:'/student/topics',query:subjectCode?{subjectCode}:undefined})">返回专题单元</el-button><el-button :disabled="unitIndex===unit.questions.length-1" type="primary" @click="chooseUnitQuestion(unitIndex+1)">下一题</el-button></div>
          <StudentAiLearningPanel :topic-question-id="detail.id" />
        </article>
      </main>
    </div>
    <el-dialog v-model="variantVisible" title="专题变式候选预览" width="min(680px,calc(100vw - 24px))" append-to-body>
      <el-alert title="AI 候选解析，待人工审核。未提交前仅当前学生可见，不进入题库、练习、推荐或教师/管理员队列。" type="info" :closable="false" />
      <el-form label-position="top"><el-form-item label="目标难度"><el-slider v-model="variantForm.targetDifficulty" :min="1" :max="5" show-stops /></el-form-item><el-form-item label="变化方式"><el-select v-model="variantForm.variationMode"><el-option v-for="item in variationModes" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-form>
      <section v-if="variantTask?.candidates?.length" class="topic-variant-result"><article v-for="candidate in variantTask.candidates" :key="candidate.questionId"><div class="variant-meta"><el-tag type="warning">{{ candidate.status === 'DRAFT' ? '待审核候选' : '已提交审核' }}</el-tag><span>{{ questionTypeLabel(candidate.questionType) }} · 难度 {{ candidate.difficulty }} · {{ candidate.variationSummary }}</span></div><h3>候选题干</h3><QuestionContent :content="candidate.stem" position="QUESTION" /><h3>候选解析</h3><p class="candidate-analysis-label">AI 候选解析，待人工审核</p><StandardAnalysis :content="candidate.standardAnalysis" /><p v-if="candidate.knowledgePoints?.length">关联知识点：{{ candidate.knowledgePoints.map(item => item.name).join('、') }}</p><StudentAiLearningPanel :topic-question-id="candidate.questionId" /><div v-if="candidate.status === 'DRAFT'" class="variant-actions"><el-button @click="discardCandidate(candidate.questionId)">丢弃候选</el-button><el-button type="primary" @click="submitCandidate(candidate.questionId)">提交任课老师审核</el-button></div><el-tag v-else type="success">已提交任课老师审核，等待人工审核</el-tag></article></section>
      <el-empty v-else description="尚未生成候选题" />
      <template #footer><span v-if="variantLoading" class="topic-variant-loading-hint">正在生成，真实模型可能需要几十秒</span><el-button @click="variantVisible=false">关闭</el-button><el-button type="primary" :loading="variantLoading" :disabled="variantLoading" @click="generateVariants">生成候选</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.topic-learning-page{max-width:1440px}.topic-learning-layout{display:grid;grid-template-columns:minmax(230px,280px) minmax(0,1fr);gap:22px}.topic-index{padding:16px;border:1px solid var(--el-border-color-lighter);border-radius:18px;background:var(--el-bg-color)}.topic-index h2{margin:0 0 12px}.topic-index button{display:grid;width:100%;gap:4px;margin:8px 0;padding:12px;border:1px solid transparent;border-radius:12px;background:var(--el-fill-color-lighter);text-align:left;cursor:pointer}.topic-index button.active{border-color:var(--el-color-primary);background:var(--el-color-primary-light-9)}.topic-index small{color:var(--el-text-color-secondary);overflow-wrap:anywhere}.topic-reader>article{padding:22px;border:1px solid var(--el-border-color-lighter);border-radius:20px;background:var(--el-bg-color)}.topic-reader header h2{margin:8px 0}.topic-material,.topic-draft,.topic-analysis{margin-top:18px;padding:16px;border-radius:14px;background:var(--el-fill-color-lighter)}.topic-material :deep(.question-content),.topic-variant-result :deep(.question-content){overflow-wrap:anywhere}.topic-actions,.topic-unit-navigation,.variant-actions{display:flex;flex-wrap:wrap;gap:10px;margin-top:18px}.topic-unit-navigation{justify-content:center}.candidate-analysis-label{color:var(--el-color-warning);font-weight:650}.topic-variant-result{display:grid;gap:18px;margin-top:18px}.topic-variant-result article{padding:18px;border:1px solid var(--el-border-color);border-radius:14px}.variant-meta{display:flex;flex-wrap:wrap;justify-content:space-between;gap:10px;color:var(--el-text-color-secondary)}@media(max-width:900px){.topic-learning-layout{grid-template-columns:1fr}.topic-index{order:2}.topic-reader{order:1}}@media(max-width:640px){.topic-reader>article,.topic-index{padding:14px}.topic-actions .el-button,.topic-unit-navigation .el-button{width:100%}}
</style>
