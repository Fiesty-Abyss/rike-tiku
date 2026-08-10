<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchTopic, fetchTopics, type TopicDetail, type TopicItem } from '../../api/student/topicLearning'
import type { ApiError } from '../../api/http'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const topics = ref<TopicItem[]>([])
const detail = ref<TopicDetail | null>(null)
const analysisVisible = ref(false)
const subjectCode = ref(String(route.query.subjectCode || ''))
const subjectOptions = [{label:'全部学科',value:''},{label:'物理',value:'PHYSICS'},{label:'化学',value:'CHEMISTRY'},{label:'生物',value:'BIOLOGY'}]
const grouped = computed(() => subjectOptions.slice(1).map(subject => ({...subject,items:topics.value.filter(item=>item.subjectCode===subject.value)})).filter(group=>group.items.length))
const difficultyLabel=(value:number)=>({1:'基础',2:'进阶',3:'挑战'} as Record<number,string>)[value]||String(value)

async function loadList() {
  loading.value = true
  try { topics.value = await fetchTopics(subjectCode.value || undefined) }
  catch (error) { ElMessage.error((error as ApiError).message || '专题列表加载失败。') }
  finally { loading.value = false }
}

async function loadDetail() {
  const id=Number(route.params.id)
  if(!id){detail.value=null;analysisVisible.value=false;return}
  loading.value=true
  try { detail.value=await fetchTopic(id);analysisVisible.value=false }
  catch(error){ElMessage.error((error as ApiError).message||'专题内容加载失败。');await router.replace('/student/topics')}
  finally{loading.value=false}
}

function chooseSubject(){void router.push({path:'/student/topics',query:subjectCode.value?{subjectCode:subjectCode.value}:undefined});void loadList()}
function openTopic(item:TopicItem){void router.push({path:`/student/topics/${item.id}`,query:subjectCode.value?{subjectCode:subjectCode.value}:undefined})}
function openKnowledgePoint(pointId:number){if(!detail.value)return;void router.push({path:`/student/subjects/${detail.value.subjectCode.toLowerCase()}`,query:{knowledgePointId:pointId}})}
function practiceKnowledgePoint(pointId:number){if(!detail.value)return;void router.push({path:'/student/practice/new',query:{subjectCode:detail.value.subjectCode,knowledgePointId:pointId,count:5}})}

watch(()=>route.params.id,()=>void loadDetail())
onMounted(async()=>{await loadList();await loadDetail()})
</script>

<template>
  <section class="student-page topic-learning-page" v-loading="loading">
    <div class="student-page-heading"><div><span class="page-kicker">TOPIC 18</span><h1>综合题专题学习</h1><p>每科 6 道原创综合题。这里只做材料阅读与解析学习，不提交、不评分。</p></div><el-select v-model="subjectCode" class="topic-subject-select" @change="chooseSubject"><el-option v-for="option in subjectOptions" :key="option.value" :label="option.label" :value="option.value"/></el-select></div>
    <div class="topic-learning-layout">
      <aside class="topic-index"><section v-for="group in grouped" :key="group.value"><h2>{{ group.label }}</h2><button v-for="item in group.items" :key="item.id" type="button" :class="{active:item.id===detail?.id}" @click="openTopic(item)"><span>{{ item.title }}</span><small>{{ difficultyLabel(item.difficulty) }} · {{ item.knowledgePoints[0]?.name }}</small></button></section></aside>
      <main class="topic-reader">
        <el-empty v-if="!detail" description="从左侧选择一道综合题开始阅读。" />
        <article v-else><header><span>{{ detail.subjectName }} · {{ difficultyLabel(detail.difficulty) }}</span><h2>{{ detail.title }}</h2></header><div class="topic-material"><h3>材料与问题</h3><p>{{ detail.material }}</p></div><div class="knowledge-chip-row"><span>关联知识点</span><el-button v-for="point in detail.knowledgePoints" :key="point.id" class="knowledge-chip" round plain @click="openKnowledgePoint(point.id)">{{ point.path }}</el-button></div><el-button v-if="!analysisVisible" type="primary" class="topic-reveal" @click="analysisVisible=true">查看标准解析</el-button><transition name="analysis-reveal"><section v-if="analysisVisible" class="topic-analysis"><div class="section-title-row"><div><h3>标准解析</h3><p>按条件、依据和结论逐步展开。</p></div><el-button link @click="analysisVisible=false">收起</el-button></div><p>{{ detail.standardAnalysis }}</p><el-button v-if="detail.knowledgePoints[0]" type="primary" plain @click="practiceKnowledgePoint(detail.knowledgePoints[0].id)">练习相关知识点</el-button></section></transition></article>
      </main>
    </div>
  </section>
</template>
