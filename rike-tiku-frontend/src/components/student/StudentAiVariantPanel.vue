<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { answerAiVariant, discardAiVariant, generateAiVariant, submitAiVariantReview, type AiVariant, type VariationMode } from '../../api/student/aiLearning'
import type { ApiError } from '../../api/http'
import AiScientificContent from './AiScientificContent.vue'
import AnswerDisplay from '../question/AnswerDisplay.vue'

const props = defineProps<{ answerFactId:number }>()
const variant = ref<AiVariant>()
const loading = ref(false)
const targetDifficulty = ref<number | undefined>()
const variationMode = ref<VariationMode>('COMBINED')
const single = ref('')
const multiple = ref<string[]>([])
const blanks = ref<string[]>([''])
const submitted = computed(() => variant.value && variant.value.status !== 'READY')
const canAnswer = computed(() => variant.value?.questionType === 'SINGLE_CHOICE' ? !!single.value : variant.value?.questionType === 'MULTIPLE_CHOICE' ? multiple.value.length > 0 : blanks.value.some(value => value.trim()))
const safeMessages:Record<string,string> = {
  AI_PROVIDER_DISABLED:'AI 服务尚未启用，请联系管理员。', AI_AUTHENTICATION_ERROR:'AI 服务连接失败，请联系管理员。',
  AI_RATE_LIMITED:'AI 请求过于频繁，请稍后再试。', AI_TIMEOUT:'AI 生成超时，请稍后重试。',
  AI_CANDIDATE_EMPTY_CONTENT:'生成内容不完整，请重新生成。',AI_CANDIDATE_FIELD_MISSING:'生成内容不完整，请重新生成。',AI_CANDIDATE_QUESTION_TYPE_INVALID:'生成题型与选择不一致，请重新选择变化方式。',AI_CANDIDATE_ANSWER_OPTION_MISMATCH:'答案与选项不一致，请重新生成。',AI_CANDIDATE_ANSWER_INVALID:'答案与选项不一致，请重新生成。',AI_CANDIDATE_CHANGED_DIMENSIONS_INSUFFICIENT:'变化幅度不足，请更换变化方式。',AI_CANDIDATE_SIMILARITY_HIGH:'与原题过于接近，请更换变化方式。',AI_CANDIDATE_VARIATION_DIMENSIONS_INSUFFICIENT:'变化幅度与所选方式不匹配，请重新选择。',AI_CANDIDATE_DIFFICULTY_COMPLEXITY_MISMATCH:'目标难度不匹配，请调整难度或变化方式。', AI_PENDING_LIMIT_REACHED:'待审核变式题已达上限，请稍后再试。',
  AI_VARIANT_KNOWLEDGE_MISSING:'当前题缺少可用知识点，暂不能生成变式。', AI_VARIANT_GENERATION_FAILED:'AI 变式生成失败，未创建练习实例。',
}
const variationModeLabel=(value:string)=>({SCENARIO_TRANSFER:'情境迁移',CONDITION_RECOMBINATION:'条件重组',REPRESENTATION_SWITCH:'表达形式转换',MULTI_STEP_EXTENSION:'多步骤扩展',DISTRACTOR_REDESIGN:'干扰项重构',COMBINED:'综合变式'} as Record<string,string>)[value]||value
function resetAnswer(){single.value='';multiple.value=[];blanks.value=['']}
function warn(error:unknown){const api=error as ApiError;const code=api.code||'';ElMessage.warning(safeMessages[code] || (code.startsWith('AI_CANDIDATE_REPAIR_FAILED_ANSWER')?'答案与选项不一致，请重新生成。':code.startsWith('AI_CANDIDATE_REPAIR_FAILED_')?'生成内容不完整，请重新生成。':api.message) || 'AI 变式暂不可用，请稍后重试。')}
async function generate(){loading.value=true;try{variant.value=await generateAiVariant(props.answerFactId,targetDifficulty.value,variationMode.value);resetAnswer()}catch(error){warn(error)}finally{loading.value=false}}
async function answer(){if(!variant.value||!canAnswer.value)return;const value=variant.value.questionType==='SINGLE_CHOICE'?single.value:variant.value.questionType==='MULTIPLE_CHOICE'?multiple.value:blanks.value;try{variant.value=await answerAiVariant(variant.value.id,value)}catch(error){warn(error)}}
async function replace(){try{if(variant.value&&variant.value.status!=='SUBMITTED_FOR_REVIEW')await discardAiVariant(variant.value.id);variant.value=undefined;await generate()}catch(error){warn(error)}}
async function submit(){if(!variant.value)return;try{variant.value=await submitAiVariantReview(variant.value.id)}catch(error){warn(error)}}
async function leaveForNow(){if(!variant.value)return;try{await discardAiVariant(variant.value.id);variant.value=undefined;resetAnswer();ElMessage.info('已结束本次变式练习。')}catch(error){warn(error)}}
</script>
<template>
  <section class="ai-variant">
    <header><div><h3>AI 变式练习</h3><p>每次生成 1 道变式题，完成后可提交教师审核。</p></div></header>
    <div v-if="!variant" class="variant-start">
      <label>目标难度
        <select v-model="targetDifficulty">
          <option :value="undefined">保持母题难度</option><option :value="1">1 基础</option><option :value="2">2 较易</option><option :value="3">3 中等</option><option :value="4">4 较难</option><option :value="5">5 困难</option>
        </select>
      </label>
      <label>变化方式
        <select v-model="variationMode"><option value="SCENARIO_TRANSFER">情境迁移</option><option value="CONDITION_RECOMBINATION">条件重组</option><option value="REPRESENTATION_SWITCH">表达形式转换</option><option value="MULTI_STEP_EXTENSION">多步骤扩展</option><option value="DISTRACTOR_REDESIGN">干扰项重构</option><option value="COMBINED">综合变式</option></select>
      </label>
      <p v-if="loading" class="ai-waiting">正在生成，可能需要几十秒。</p><el-button type="primary" :loading="loading" :disabled="loading" @click="generate">生成变式题</el-button>
    </div>
    <article v-else>
      <div class="variant-meta"><el-tag>难度 {{ variant.difficulty }}</el-tag><el-tag effect="plain">{{ variationModeLabel(variant.variationMode) }}</el-tag><el-tag v-if="variant.noveltyDecision==='ACCEPT'" type="success">内容检查通过</el-tag><el-tag v-else-if="variant.noveltyDecision==='WARN'" type="warning">请检查相似内容</el-tag></div>
      <el-alert v-if="variant.noveltyDecision==='WARN'" title="这道题与原题较为相似，请仔细检查后再提交审核。" type="warning" :closable="false" />
      <AiScientificContent :content="variant.stem"/>
      <el-checkbox-group v-if="variant.questionType==='MULTIPLE_CHOICE'" v-model="multiple" :disabled="submitted"><el-checkbox v-for="option in variant.options" :key="option.label" :value="option.label">{{option.label}}. {{option.content}}</el-checkbox></el-checkbox-group>
      <el-radio-group v-else-if="variant.questionType==='SINGLE_CHOICE'" v-model="single" :disabled="submitted"><el-radio v-for="option in variant.options" :key="option.label" :value="option.label">{{option.label}}. {{option.content}}</el-radio></el-radio-group>
      <div v-else><el-input v-for="(_,index) in blanks" :key="index" v-model="blanks[index]" :disabled="submitted" :placeholder="`第 ${index+1} 空`"/></div>
      <div v-if="variant.status==='READY'" class="variant-actions"><el-button @click="leaveForNow">暂不提交</el-button><el-button type="primary" :disabled="!canAnswer" @click="answer">提交答案</el-button></div>
      <div v-else class="variant-result">
        <el-tag :type="variant.correct?'success':'danger'">{{variant.correct?'回答正确':'回答有误'}}</el-tag>
        <div><strong>正确答案</strong><AnswerDisplay :question-type="variant.questionType" :value="variant.correctAnswer" :options="variant.options" /></div>
        <h4>本次练习解析</h4><AiScientificContent :content="variant.aiAnalysis||''"/>
        <div class="variant-actions"><el-button @click="replace">换一题</el-button><el-button v-if="variant.status==='ANSWERED'" type="primary" @click="submit">提交教师审核</el-button><el-tag v-else>已提交审核</el-tag></div>
      </div>
    </article>
  </section>
</template>
<style scoped>.ai-variant{margin-top:18px;padding:18px;border:1px solid var(--el-border-color);border-radius:16px}.ai-variant header{display:flex;justify-content:space-between;gap:16px}.variant-start,.variant-actions,.variant-meta{display:flex;align-items:center;justify-content:space-between;gap:12px}.variant-start label{display:flex;align-items:center;gap:10px}.variant-start select{min-width:190px;padding:8px 10px;border:1px solid var(--el-border-color);border-radius:10px;background:var(--el-bg-color)}.ai-variant article{display:grid;gap:14px}.variant-meta span{color:var(--el-text-color-secondary);font-size:13px}.ai-variant .el-checkbox-group,.ai-variant .el-radio-group{display:grid;gap:8px}.variant-result{display:grid;gap:12px}@media(max-width:640px){.variant-start,.variant-actions,.variant-meta{align-items:stretch;flex-direction:column}.variant-start select,.variant-start .el-button{width:100%}}</style>
