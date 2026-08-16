<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchKnowledgeCardAttachment, fetchKnowledgeCards, updateKnowledgeCardState, type KnowledgeCard } from '../../api/student/knowledgeCards'
import ScientificText from '../../components/question/ScientificText.vue'
import StudentAiLearningPanel from '../../components/student/StudentAiLearningPanel.vue'

const cards=ref<KnowledgeCard[]>([])
const selected=ref<KnowledgeCard>()
const loading=ref(false)
const imageSources=ref<Record<number,string>>({})
const filters=reactive({subjectId:undefined as number|undefined,knowledgePointId:undefined as number|undefined,type:'',favorite:undefined as boolean|undefined,mastery:''})
const types=[['POINT','核心考点'],['FORMULA','公式'],['CHEMICAL_EQUATION','化学方程式'],['SECONDARY_CONCLUSION','二级结论'],['INSTRUMENT','实验器具'],['MNEMONIC','记忆口诀'],['TABLE','知识表格'],['NOTE','补充笔记']] as const
const subjects=computed(()=>[...new Map(cards.value.map(c=>[c.subjectId,{id:c.subjectId,name:c.subjectName}])).values()])
const points=computed(()=>[...new Map(cards.value.flatMap(c=>c.knowledgePoints).map(p=>[p.id,p])).values()])
const typeLabel=(value:string)=>types.find(item=>item[0]===value)?.[1] || value
async function load(){
  loading.value=true
  try { cards.value=await fetchKnowledgeCards({subjectId:filters.subjectId,knowledgePointId:filters.knowledgePointId,type:filters.type||undefined,favorite:filters.favorite,mastery:filters.mastery||undefined}) }
  catch(error){ElMessage.error((error as {message?:string}).message || '高频考点加载失败。')}
  finally{loading.value=false}
}
function clearImages(){Object.values(imageSources.value).forEach(URL.revokeObjectURL);imageSources.value={}}
async function open(card:KnowledgeCard){
  clearImages();selected.value=card
  for(const item of card.attachments)try{imageSources.value[item.id]=URL.createObjectURL(await fetchKnowledgeCardAttachment(card.id,item.id))}catch{ElMessage.warning(`${item.name} 暂不可读取。`)}
}
async function state(card:KnowledgeCard,favorite=card.favorite,mastery=card.mastery){selected.value=await updateKnowledgeCardState(card.id,{favorite,mastery});cards.value=cards.value.map(c=>c.id===card.id?selected.value!:c)}
onMounted(load)
onBeforeUnmount(clearImages)
</script>

<template>
  <main class="card-library">
    <header><p>REVIEWED SCIENCE POINTS</p><h1>物化生高频考点与二级结论</h1><span>按课程复习模块整理；页面不冒充基于完整真题样本的频次统计。</span></header>
    <section class="filters"><el-select v-model="filters.subjectId" clearable placeholder="学科"><el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" /></el-select><el-select v-model="filters.knowledgePointId" clearable filterable placeholder="知识点"><el-option v-for="p in points" :key="p.id" :label="p.path" :value="p.id" /></el-select><el-select v-model="filters.type" clearable placeholder="考点类型"><el-option v-for="t in types" :key="t[0]" :label="t[1]" :value="t[0]" /></el-select><el-select v-model="filters.mastery" clearable placeholder="掌握状态"><el-option label="学习中" value="LEARNING" /><el-option label="已掌握" value="MASTERED" /></el-select><el-button type="primary" @click="load">筛选</el-button></section>
    <section v-loading="loading" class="card-grid"><article v-for="card in cards" :key="card.id" tabindex="0" @click="open(card)" @keydown.enter="open(card)"><div><el-tag effect="plain">{{ typeLabel(card.type) }}</el-tag><span>{{ card.subjectName }}</span></div><h2>{{ card.title }}</h2><ScientificText :content="card.content" /><small>{{ card.knowledgePoints.map(p=>p.path).join(' · ') }}</small><footer><el-button link @click.stop="state(card,!card.favorite,card.mastery)">{{ card.favorite?'取消收藏':'收藏' }}</el-button><el-button link @click.stop="state(card,card.favorite,card.mastery==='MASTERED'?'LEARNING':'MASTERED')">{{ card.mastery==='MASTERED'?'改为学习中':'标记已掌握' }}</el-button></footer></article><el-empty v-if="!cards.length" description="暂无符合条件的已审核高频考点" /></section>
    <el-drawer v-model="selected" size="min(760px,100%)" title="高频考点详情"><template v-if="selected"><h2>{{ selected.title }}</h2><p>{{ selected.subjectName }} · {{ selected.knowledgePoints.map(p=>p.path).join('；') }}</p><section><h3>核心知识</h3><ScientificText :content="selected.content" /></section><section v-if="selected.latex"><h3>公式、化学式与科学表达</h3><ScientificText :content="selected.latex" /></section><section v-if="selected.applicableConditions"><h3>适用条件</h3><ScientificText :content="selected.applicableConditions" /></section><section v-if="selected.derivation"><h3>推导与二级结论</h3><ScientificText :content="selected.derivation" /></section><section v-if="selected.example"><h3>常见题型与考法</h3><ScientificText :content="selected.example" /></section><section v-if="selected.commonMistake"><h3>常见误区</h3><ScientificText :content="selected.commonMistake" /></section><section v-if="selected.mnemonic"><h3>记忆提醒</h3><ScientificText :content="selected.mnemonic" /></section><figure v-for="image in selected.attachments" :key="image.id"><img v-if="imageSources[image.id]" :src="imageSources[image.id]" :alt="image.name"><figcaption>{{ image.name }}</figcaption></figure><p class="source">来源：<a v-if="selected.sourceUrl" :href="selected.sourceUrl" target="_blank" rel="noopener noreferrer nofollow">{{ selected.sourceName }}</a><span v-else>{{ selected.sourceName || '项目整理' }}</span> · {{ selected.rightsStatus }}</p><StudentAiLearningPanel :knowledge-card-id="selected.id" /></template></el-drawer>
  </main>
</template>

<style scoped>
.card-library{max-width:1280px;margin:auto;padding:28px}.card-library>header{padding:28px;border-radius:24px;background:linear-gradient(135deg,#e7fbff,#f6f1ff)}.card-library header p{letter-spacing:.16em;color:#177b8f}.filters{display:flex;flex-wrap:wrap;gap:10px;margin:20px 0}.filters .el-select{width:190px}.card-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px}.card-grid>article{display:flex;flex-direction:column;gap:10px;padding:20px;border:1px solid var(--el-border-color-lighter);border-radius:18px;background:white;box-shadow:0 10px 30px rgba(23,72,88,.07);cursor:pointer}.card-grid>article:focus-visible{outline:3px solid var(--el-color-primary-light-5)}.card-grid article>div,.card-grid footer{display:flex;justify-content:space-between;gap:8px}.card-grid h2{margin:0;overflow-wrap:anywhere}.card-grid small,.source{color:var(--el-text-color-secondary);overflow-wrap:anywhere}figure img{max-width:100%;border-radius:14px}.card-library section>h3{margin-bottom:8px}.card-library section{margin-top:16px;line-height:1.75}@media(max-width:640px){.card-library{padding:14px}.filters .el-select,.filters .el-button{width:100%}}
</style>
