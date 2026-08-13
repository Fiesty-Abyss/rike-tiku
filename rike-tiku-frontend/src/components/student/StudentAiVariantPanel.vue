<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { answerAiVariant, discardAiVariant, generateAiVariant, submitAiVariantReview, type AiVariant } from '../../api/student/aiLearning'
import type { ApiError } from '../../api/http'
import AiScientificContent from './AiScientificContent.vue'
import AnswerDisplay from '../question/AnswerDisplay.vue'

const props = defineProps<{ answerFactId:number }>()
const variant = ref<AiVariant>()
const loading = ref(false)
const targetDifficulty = ref<number | undefined>()
const single = ref('')
const multiple = ref<string[]>([])
const blanks = ref<string[]>([''])
const submitted = computed(() => variant.value && variant.value.status !== 'READY')
const canAnswer = computed(() => variant.value?.questionType === 'SINGLE_CHOICE' ? !!single.value : variant.value?.questionType === 'MULTIPLE_CHOICE' ? multiple.value.length > 0 : blanks.value.some(value => value.trim()))
const safeMessages:Record<string,string> = {
  AI_PROVIDER_DISABLED:'AI Provider 尚未启用，请联系管理员。', AI_AUTHENTICATION_ERROR:'AI Provider 认证失败，请联系管理员。',
  AI_RATE_LIMITED:'AI 请求过于频繁，请稍后再试。', AI_TIMEOUT:'AI 生成超时，请稍后重试。',
  AI_INVALID_RESPONSE:'AI 返回的题目结构未通过校验，请换一题。', AI_PENDING_LIMIT_REACHED:'该母题待审核候选题已达上限。',
  AI_VARIANT_KNOWLEDGE_MISSING:'当前题缺少可用知识点，暂不能生成变式。', AI_VARIANT_GENERATION_FAILED:'AI 变式生成失败，未创建练习实例。',
}
function resetAnswer(){single.value='';multiple.value=[];blanks.value=['']}
function warn(error:unknown){const api=error as ApiError;ElMessage.warning(safeMessages[api.code || ''] || api.message || 'AI 变式暂不可用，请稍后重试。')}
async function generate(){loading.value=true;try{variant.value=await generateAiVariant(props.answerFactId,targetDifficulty.value);resetAnswer()}catch(error){warn(error)}finally{loading.value=false}}
async function answer(){if(!variant.value||!canAnswer.value)return;const value=variant.value.questionType==='SINGLE_CHOICE'?single.value:variant.value.questionType==='MULTIPLE_CHOICE'?multiple.value:blanks.value;try{variant.value=await answerAiVariant(variant.value.id,value)}catch(error){warn(error)}}
async function replace(){try{if(variant.value&&variant.value.status!=='SUBMITTED_FOR_REVIEW')await discardAiVariant(variant.value.id);variant.value=undefined;await generate()}catch(error){warn(error)}}
async function submit(){if(!variant.value)return;try{variant.value=await submitAiVariantReview(variant.value.id)}catch(error){warn(error)}}
async function leaveForNow(){if(!variant.value)return;try{await discardAiVariant(variant.value.id);variant.value=undefined;resetAnswer();ElMessage.info('本次变式已保留审计状态并结束。')}catch(error){warn(error)}}
</script>
<template>
  <section class="ai-variant">
    <header><div><h3>AI 变式练习</h3><p>每次生成 1 道可确定性判分的候选题；提交后仍是 PENDING，必须人工审核。</p></div></header>
    <div v-if="!variant" class="variant-start">
      <label>目标难度
        <select v-model="targetDifficulty">
          <option :value="undefined">保持母题难度</option><option :value="1">1 基础</option><option :value="2">2 较易</option><option :value="3">3 中等</option><option :value="4">4 较难</option><option :value="5">5 困难</option>
        </select>
      </label>
      <el-button type="primary" :loading="loading" @click="generate">生成变式题</el-button>
    </div>
    <article v-else>
      <div class="variant-meta"><el-tag>难度 {{ variant.difficulty }}</el-tag><span>AI 候选 · 尚未成为正式 STANDARD</span></div>
      <AiScientificContent :content="variant.stem"/>
      <el-checkbox-group v-if="variant.questionType==='MULTIPLE_CHOICE'" v-model="multiple" :disabled="submitted"><el-checkbox v-for="option in variant.options" :key="option.label" :value="option.label">{{option.label}}. {{option.content}}</el-checkbox></el-checkbox-group>
      <el-radio-group v-else-if="variant.questionType==='SINGLE_CHOICE'" v-model="single" :disabled="submitted"><el-radio v-for="option in variant.options" :key="option.label" :value="option.label">{{option.label}}. {{option.content}}</el-radio></el-radio-group>
      <div v-else><el-input v-for="(_,index) in blanks" :key="index" v-model="blanks[index]" :disabled="submitted" :placeholder="`第 ${index+1} 空`"/></div>
      <div v-if="variant.status==='READY'" class="variant-actions"><el-button @click="leaveForNow">暂不提交</el-button><el-button type="primary" :disabled="!canAnswer" @click="answer">提交答案</el-button></div>
      <div v-else class="variant-result">
        <el-tag :type="variant.correct?'success':'danger'">{{variant.correct?'回答正确':'回答有误'}}</el-tag>
        <div><strong>正确答案</strong><AnswerDisplay :question-type="variant.questionType" :value="variant.correctAnswer" :options="variant.options" /></div>
        <h4>AI 生成解析，仅用于本次练习，尚未成为正式 STANDARD</h4><AiScientificContent :content="variant.aiAnalysis||''"/>
        <div class="variant-actions"><el-button @click="replace">换一题</el-button><el-button v-if="variant.status==='ANSWERED'" type="primary" @click="submit">提交人工审核</el-button><el-tag v-else>已提交审核 · PENDING</el-tag></div>
      </div>
    </article>
  </section>
</template>
<style scoped>.ai-variant{margin-top:18px;padding:18px;border:1px solid var(--el-border-color);border-radius:16px}.ai-variant header{display:flex;justify-content:space-between;gap:16px}.variant-start,.variant-actions,.variant-meta{display:flex;align-items:center;justify-content:space-between;gap:12px}.variant-start label{display:flex;align-items:center;gap:10px}.variant-start select{min-width:190px;padding:8px 10px;border:1px solid var(--el-border-color);border-radius:10px;background:var(--el-bg-color)}.ai-variant article{display:grid;gap:14px}.variant-meta span{color:var(--el-text-color-secondary);font-size:13px}.ai-variant .el-checkbox-group,.ai-variant .el-radio-group{display:grid;gap:8px}.variant-result{display:grid;gap:12px}@media(max-width:640px){.variant-start,.variant-actions,.variant-meta{align-items:stretch;flex-direction:column}.variant-start select,.variant-start .el-button{width:100%}}</style>
