<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchTopic, fetchTopics, fetchTopicUnit, fetchTopicUnits, generateTopicVariants, type TopicDetail, type TopicItem, type TopicUnitDetail, type TopicUnitItem, type TopicVariantTask } from '../../api/student/topicLearning'
import type { ApiError } from '../../api/http'
import StandardAnalysis from '../../components/question/StandardAnalysis.vue'
import QuestionContent from '../../components/question/QuestionContent.vue'
import StudentAiLearningPanel from '../../components/student/StudentAiLearningPanel.vue'
import { subjectTheme } from '../../utils/subjectTheme'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const topics = ref<TopicItem[]>([])
const detail = ref<TopicDetail | null>(null)
const units=ref<TopicUnitItem[]>([]);const unit=ref<TopicUnitDetail|null>(null)
const analysisVisible = ref(false)
const draft = ref('')
const variantVisible=ref(false);const variantLoading=ref(false);const variantTask=ref<TopicVariantTask|null>(null)
const variantForm=ref({targetDifficulty:3,variationMode:'SCENARIO_TRANSFER',count:1,requireVisualContext:false,keepPrimaryKnowledgePoint:true})
const variationModes=[{label:'情境迁移',value:'SCENARIO_TRANSFER'},{label:'条件重组',value:'CONDITION_RECOMBINATION'},{label:'表达形式转换',value:'REPRESENTATION_SWITCH'},{label:'多步骤扩展',value:'MULTI_STEP_EXTENSION'},{label:'干扰项重构',value:'DISTRACTOR_REDESIGN'},{label:'综合变式',value:'COMBINED'}]
const subjectCode = ref(String(route.query.subjectCode || ''))
const subjectOptions = [{label:'全部学科',value:''},{label:'物理',value:'PHYSICS'},{label:'化学',value:'CHEMISTRY'},{label:'生物',value:'BIOLOGY'}]
const topicType=ref('');const topicTypeOptions=[{label:'全部类型',value:''},{label:'计算题',value:'CALCULATION'},{label:'实验题',value:'EXPERIMENT'},{label:'流程题',value:'PROCESS'},{label:'材料分析题',value:'MATERIAL_ANALYSIS'},{label:'综合题',value:'COMPREHENSIVE'}]
const topicTypeLabel=(value:string)=>topicTypeOptions.find(item=>item.value===value)?.label||'综合题'
const grouped = computed(() => subjectOptions.slice(1).map(subject => ({...subject,items:topics.value.filter(item=>item.subjectCode===subject.value&&(!topicType.value||item.topicType===topicType.value))})).filter(group=>group.items.length))
const environment = computed(() => subjectTheme(detail.value?.subjectCode || subjectCode.value))
const difficultyLabel=(value:number)=>({1:'基础',2:'进阶',3:'挑战'} as Record<number,string>)[value]||String(value)
const stageLabel=(value:string)=>({FOUNDATION:'基础理解',TRANSFER:'情境迁移',ADVANCED:'综合提升'} as Record<string,string>)[value]||value

async function loadList() {
  loading.value = true
  try { [topics.value,units.value] = await Promise.all([fetchTopics(subjectCode.value || undefined),fetchTopicUnits(subjectCode.value || undefined)]) }
  catch (error) { ElMessage.error((error as ApiError).message || '专题列表加载失败。') }
  finally { loading.value = false }
}

async function loadDetail() {
  const unitId=Number(route.params.unitId)
  if(unitId){loading.value=true;try{unit.value=await fetchTopicUnit(unitId);detail.value=null}catch(error){ElMessage.error((error as ApiError).message||'专题单元加载失败。');await router.replace('/student/topics')}finally{loading.value=false}return}
  unit.value=null
  const id=Number(route.params.id)
  if(!id){detail.value=null;analysisVisible.value=false;return}
  loading.value=true
  try { detail.value=await fetchTopic(id);analysisVisible.value=false;draft.value='' }
  catch(error){ElMessage.error((error as ApiError).message||'专题内容加载失败。');await router.replace('/student/topics')}
  finally{loading.value=false}
}

function chooseSubject(){void router.push({path:'/student/topics',query:subjectCode.value?{subjectCode:subjectCode.value}:undefined});void loadList()}
function openTopic(item:TopicItem){void router.push({path:`/student/topics/${item.id}`,query:subjectCode.value?{subjectCode:subjectCode.value}:undefined})}
function openUnit(item:TopicUnitItem){void router.push({path:`/student/topics/units/${item.id}`,query:subjectCode.value?{subjectCode:subjectCode.value}:undefined})}
function openKnowledgePoint(pointId:number){if(!detail.value)return;void router.push({path:`/student/subjects/${detail.value.subjectCode.toLowerCase()}`,query:{knowledgePointId:pointId}})}
function practiceKnowledgePoint(pointId:number){if(!detail.value)return;void router.push({path:'/student/practice/new',query:{subjectCode:detail.value.subjectCode,knowledgePointId:pointId,count:5}})}
async function generateVariants(){if(!detail.value)return;variantLoading.value=true;try{variantTask.value=await generateTopicVariants(detail.value.id,variantForm.value);ElMessage.success('专题候选已生成并进入 PENDING 人工审核。')}catch(error){ElMessage.error((error as ApiError).message||'专题变式生成失败。')}finally{variantLoading.value=false}}

watch(()=>[route.params.id,route.params.unitId],()=>void loadDetail())
onMounted(async()=>{await loadList();await loadDetail()})
</script>

<template>
  <section class="student-page topic-learning-page" :data-subject="environment" v-loading="loading">
    <div class="student-page-heading"><div><span class="page-kicker">TOPIC LEARNING</span><h1>主观专题学习</h1><p>本地草稿不上传、不评分；STANDARD 始终是权威解析。</p></div><div><el-select v-model="subjectCode" class="topic-subject-select" @change="chooseSubject"><el-option v-for="option in subjectOptions" :key="option.value" :label="option.label" :value="option.value"/></el-select><el-select v-model="topicType"><el-option v-for="option in topicTypeOptions" :key="option.value" :label="option.label" :value="option.value"/></el-select></div></div>
    <div class="topic-learning-layout">
      <aside class="topic-index"><section v-if="units.length"><h2>专题单元</h2><button v-for="item in units" :key="`unit-${item.id}`" type="button" :class="{active:item.id===unit?.id}" @click="openUnit(item)"><span>{{ item.title }}</span><small>{{ item.questionCount }} 题 · {{ item.primaryKnowledgePoint.path }}</small></button></section><section v-for="group in grouped" :key="group.value"><h2>{{ group.label }}</h2><button v-for="item in group.items" :key="item.id" type="button" :class="{active:item.id===detail?.id}" @click="openTopic(item)"><span>{{ item.title }}</span><small>{{ difficultyLabel(item.difficulty) }} · {{ item.knowledgePoints[0]?.name }}</small></button></section></aside>
      <main class="topic-reader">
        <article v-if="unit" class="topic-unit-reader"><header><span>{{ unit.subjectName }} · {{ difficultyLabel(unit.difficulty) }}</span><h2>{{ unit.title }}</h2><p>{{ unit.introduction }}</p></header><ol class="topic-unit-steps"><li v-for="entry in unit.questions" :key="entry.question.id"><button type="button" @click="openTopic(entry.question)"><strong>{{ stageLabel(entry.stage) }}</strong><span>{{ entry.question.title }}</span></button></li></ol><p class="topic-boundary">专题单元只编排现有 PUBLISHED 题；本地草稿不上传，主观题不自动评分，STANDARD 不被 AI 修改。</p></article>
        <el-empty v-else-if="!detail" description="从左侧选择专题单元或一道综合题开始阅读。" />
        <article v-else><header><span>{{ detail.subjectName }} · {{ topicTypeLabel(detail.topicType) }} · {{ difficultyLabel(detail.difficulty) }}</span><h2>{{ detail.title }}</h2></header><div class="topic-material"><h3>材料与问题</h3><QuestionContent :content="detail.material" :attachments="detail.stemAttachments" position="QUESTION" /></div><div class="topic-draft"><h3>自我作答草稿</h3><p>只保留在当前页面，切换题目即清空，不提交、不评分。</p><el-input v-model="draft" type="textarea" :rows="10" maxlength="5000" show-word-limit placeholder="在这里整理计算、流程或分析步骤……" /></div><div class="knowledge-chip-row"><span>关联知识点</span><el-button v-for="point in detail.knowledgePoints" :key="point.id" class="knowledge-chip" round plain @click="openKnowledgePoint(point.id)">{{ point.path }}</el-button></div><el-button v-if="!analysisVisible" type="primary" class="topic-reveal" @click="analysisVisible=true">查看标准解析</el-button><el-button plain @click="variantVisible=true">生成专题变式</el-button><transition name="analysis-reveal"><section v-if="analysisVisible" class="topic-analysis"><div class="section-title-row"><div><h3>标准解析</h3><p>按条件、依据和结论逐步展开。</p></div><el-button link @click="analysisVisible=false">收起</el-button></div><StandardAnalysis :content="detail.standardAnalysis" :attachments="detail.analysisAttachments" /><el-button v-if="detail.knowledgePoints[0]" type="primary" plain @click="practiceKnowledgePoint(detail.knowledgePoints[0].id)">练习相关知识点</el-button></section></transition><StudentAiLearningPanel :topic-question-id="detail.id" /></article>
      </main>
    </div>
    <el-dialog v-model="variantVisible" title="生成专题变式" width="min(560px,calc(100vw - 24px))" append-to-body><el-alert title="候选题不自动评分，生成后只能进入 PENDING，必须由教师或管理员人工审核。" type="info" :closable="false"/><el-form label-position="top"><el-form-item label="数量"><el-input-number v-model="variantForm.count" :min="1" :max="3"/></el-form-item><el-form-item label="目标难度"><el-slider v-model="variantForm.targetDifficulty" :min="1" :max="5" show-stops/></el-form-item><el-form-item label="变化方式"><el-select v-model="variantForm.variationMode"><el-option v-for="item in variationModes" :key="item.value" :label="item.label" :value="item.value"/></el-select></el-form-item><el-checkbox v-model="variantForm.requireVisualContext">需要视觉情境（仅在安全视觉 Provider 成功时使用）</el-checkbox><el-checkbox v-model="variantForm.keepPrimaryKnowledgePoint">保持主知识点</el-checkbox></el-form><section v-if="variantTask" class="topic-variant-result"><el-tag type="warning">PENDING</el-tag><article v-for="candidate in variantTask.candidates" :key="candidate.questionId"><h4>{{ candidate.stem }}</h4><p>{{ candidate.variationSummary }}</p></article></section><template #footer><el-button @click="variantVisible=false">关闭</el-button><el-button type="primary" :loading="variantLoading" @click="generateVariants">生成候选</el-button></template></el-dialog>
  </section>
</template>
