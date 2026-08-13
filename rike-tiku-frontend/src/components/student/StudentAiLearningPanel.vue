<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { ApiError } from '../../api/http'
import AiScientificContent from './AiScientificContent.vue'
import StudentAiVariantPanel from './StudentAiVariantPanel.vue'
import {
  createAiConversation, createTopicAiConversation, fetchAiAnalysis, fetchAiCapabilities, fetchAiModelOptions, generateAiAnalysis, sendAiMessage,
  type AiAnalysis, type AiConversation, type AiModelOption,
} from '../../api/student/aiLearning'

const props = withDefaults(defineProps<{ answerFactId?:number; topicQuestionId?:number; wrong?:boolean }>(),{wrong:false})
const topicMode = !!props.topicQuestionId
const analysis = ref<AiAnalysis | null>(null)
const analysisLoading = ref(false)
const analysisUnavailable = ref(false)
const chatVisible = ref(false)
const conversation = ref<AiConversation | null>(null)
const chatLoading = ref(false)
const sending = ref(false)
const draft = ref('')
const messageList = ref<HTMLElement | null>(null)
const modelOptions = ref<AiModelOption[]>([])
const selectedModelId = ref<number>()
const thinkingMode = ref<'STANDARD'|'DEEP'>('STANDARD')
const webSearch = ref(false)
const webSearchAvailable = ref(false)

const safeError = (error:unknown, fallback:string) => (error as ApiError).message || fallback
const errorTypeLabel = (value?:string) => ({
  CONCEPT_ERROR:'概念理解', CALCULATION_ERROR:'计算过程', READING_ERROR:'审题阅读', REASONING_ERROR:'推理过程',
  MEMORY_ERROR:'记忆遗漏', CARELESS_ERROR:'粗心疏漏', ANSWER_FORMAT_ERROR:'作答格式', UNKNOWN:'暂未归类',
} as Record<string,string>)[value || 'UNKNOWN'] || '暂未归类'

async function loadAnalysis() {
  analysis.value = null
  analysisUnavailable.value = false
  if (!props.wrong || !props.answerFactId) return
  try { analysis.value = await fetchAiAnalysis(props.answerFactId) }
  catch { analysisUnavailable.value = true }
}

async function generate() {
  analysisLoading.value = true
  analysisUnavailable.value = false
  if(!props.answerFactId)return
  try { analysis.value = await generateAiAnalysis(props.answerFactId) }
  catch (error) {
    analysisUnavailable.value = true
    ElMessage.warning(safeError(error, 'AI 暂不可用，标准解析仍可正常查看。'))
  } finally { analysisLoading.value = false }
}

async function openTutor() {
  chatVisible.value = true
  if (conversation.value) return
  chatLoading.value = true
  try {
    const [models, capabilities] = await Promise.all([fetchAiModelOptions(), fetchAiCapabilities()])
    modelOptions.value = models
    webSearchAvailable.value = capabilities.webSearchAvailable
    selectedModelId.value = models.find(item => item.defaultOption && item.available)?.id || models.find(item => item.available)?.id
  }
  catch (error) { ElMessage.warning(safeError(error, '当前题目答疑暂不可用。')) }
  finally { chatLoading.value = false }
}

async function startConversation() {
  chatLoading.value = true
  try {
    const options={ modelConfigId:selectedModelId.value, thinkingMode:thinkingMode.value, webSearch:webSearch.value }
    conversation.value = topicMode && props.topicQuestionId
      ? await createTopicAiConversation(props.topicQuestionId,options)
      : await createAiConversation(props.answerFactId!,options)
  }
  catch (error) { ElMessage.warning(safeError(error, '当前题目答疑暂不可用。')) }
  finally { chatLoading.value = false }
}

async function send() {
  const content = draft.value.trim()
  if (!conversation.value || !content || sending.value) return
  sending.value = true
  try {
    conversation.value = await sendAiMessage(conversation.value.id, content)
    draft.value = ''
    await nextTick()
    messageList.value?.scrollTo({ top: messageList.value.scrollHeight, behavior: 'smooth' })
  } catch (error) { ElMessage.warning(safeError(error, '发送失败，学习记录和标准解析不受影响。')) }
  finally { sending.value = false }
}

watch(() => [props.answerFactId,props.topicQuestionId], () => { conversation.value = null; chatVisible.value = false; void loadAnalysis() })
onMounted(() => void loadAnalysis())
</script>

<template>
  <section class="student-ai-panel" aria-label="AI 学习辅助">
    <div class="student-ai-heading">
      <div><span class="student-ai-label">{{ topicMode ? 'AI 专题讲解' : 'AI 辅助分析' }}</span><p>{{ topicMode ? '基于题干、STANDARD 与知识点讲解；不提交、不评分、不修改 STANDARD。' : '基于本次正式答案提供个性化提示，不替代标准解析与正式判分。' }}</p></div>
      <el-button type="primary" plain @click="openTutor">{{ topicMode ? '当前专题答疑' : '当前题目答疑' }}</el-button>
    </div>
    <template v-if="!topicMode && wrong">
      <div v-if="analysisLoading" class="student-ai-state"><el-skeleton :rows="3" animated /><span>正在生成错因分析…</span></div>
      <div v-else-if="analysis?.status === 'SUCCESS'" class="student-ai-result">
        <el-tag effect="plain">{{ errorTypeLabel(analysis.errorType) }}</el-tag>
        <h4>错因定位</h4><AiScientificContent :content="analysis.errorReason || ''" />
        <h4>正确思路</h4><AiScientificContent :content="analysis.correctThinking || ''" />
        <div class="student-ai-columns"><div><h4>常见误区</h4><ul><li v-for="item in analysis.commonMistakes" :key="item"><AiScientificContent :content="item" /></li></ul></div><div><h4>复习建议</h4><ul><li v-for="item in analysis.reviewSuggestions" :key="item"><AiScientificContent :content="item" /></li></ul></div></div>
      </div>
      <div v-else class="student-ai-state">
        <p>{{ analysisUnavailable || analysis?.status === 'FAILED' ? 'AI 暂不可用，标准解析仍然有效。' : '尚未生成本题的个性化错因分析。' }}</p>
        <el-button type="primary" :loading="analysisLoading" @click="generate">{{ analysisUnavailable || analysis?.status === 'FAILED' ? '重试生成' : '生成 AI 错因分析' }}</el-button>
      </div>
    </template>
    <p v-else-if="!topicMode" class="student-ai-correct">本题已答对；仍可围绕当前题目继续提问。</p>
    <StudentAiVariantPanel v-if="answerFactId" :answer-fact-id="answerFactId" />

    <el-drawer v-model="chatVisible" title="RIKE 理科学习助手" size="min(520px, 100%)" append-to-body>
      <div class="student-ai-chat" v-loading="chatLoading">
        <p class="student-ai-chat-note"><strong>已绑定{{ topicMode ? '当前专题' : '当前题目' }}</strong><br>仅围绕本题，最多 10 轮；STANDARD 答案与解析不会被 AI 修改。</p>
        <div v-if="!conversation" class="student-ai-controls">
          <label>回答模型<select v-model="selectedModelId"><option v-for="item in modelOptions" :key="item.id" :value="item.id" :disabled="!item.available">{{ item.displayName }}{{ item.available ? '' : '（管理员尚未启用）' }}</option></select></label>
          <fieldset><legend>回答方式</legend><label><input v-model="thinkingMode" type="radio" value="STANDARD">标准回答</label><label><input v-model="thinkingMode" type="radio" value="DEEP">深度思考</label></fieldset>
          <label><input v-model="webSearch" type="checkbox" :disabled="!webSearchAvailable"> 联网搜索</label><small>{{ webSearchAvailable ? '开启后本轮可能更慢，并产生一次额外搜索调用。' : '管理员尚未配置联网搜索。' }}</small>
          <el-button type="primary" :disabled="!selectedModelId" @click="startConversation">开始答疑</el-button>
        </div>
        <div ref="messageList" class="student-ai-messages" aria-live="polite">
          <div v-if="!conversation?.messages.length" class="student-ai-empty">可以问：为什么这一步要这样推导？</div>
          <div v-for="message in conversation?.messages || []" :key="message.id" class="student-ai-message" :class="message.role === 'USER' ? 'is-user' : 'is-assistant'">
            <span>{{ message.role === 'USER' ? '我' : 'RIKE 理科学习助手' }}</span><AiScientificContent :content="message.content" />
            <ul v-if="message.sources?.length" class="student-ai-sources"><li v-for="source in message.sources" :key="source.url"><a :href="source.url" target="_blank" rel="noopener noreferrer nofollow">{{ source.title || source.publisher }}</a><small>{{ source.publisher }}{{ source.publishDate ? ` · ${source.publishDate}` : '' }}</small></li></ul>
          </div>
        </div>
        <div class="student-ai-composer">
          <el-input v-model="draft" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="只提问与当前题目相关的内容" :disabled="!conversation || conversation.status === 'LIMIT_REACHED'" @keydown.ctrl.enter.prevent="send" />
          <div><span>剩余 {{ conversation?.remainingRounds ?? 10 }} / 10 轮</span><el-button type="primary" :loading="sending" :disabled="!draft.trim() || !conversation || conversation.status === 'LIMIT_REACHED'" @click="send">发送</el-button></div>
        </div>
      </div>
    </el-drawer>
  </section>
</template>

<style scoped>
.student-ai-panel{margin-top:18px;padding:18px;border:1px solid color-mix(in srgb,var(--el-color-primary) 25%,transparent);border-radius:18px;background:color-mix(in srgb,var(--el-color-primary-light-9) 48%,white)}
.student-ai-heading{display:flex;align-items:flex-start;justify-content:space-between;gap:16px}.student-ai-heading p,.student-ai-state p,.student-ai-correct{margin:6px 0;color:var(--el-text-color-secondary)}
.student-ai-label{font-size:16px;font-weight:750}.student-ai-result{margin-top:16px}.student-ai-result h4{margin:14px 0 6px}.student-ai-result p,.student-ai-result ul{margin:0;line-height:1.75}.student-ai-columns{display:grid;grid-template-columns:1fr 1fr;gap:20px}
.student-ai-state{padding:18px 0 2px}.student-ai-chat{height:calc(100vh - 130px);display:flex;flex-direction:column}.student-ai-chat-note{margin:0 0 12px;padding:10px 12px;border-radius:12px;background:var(--el-fill-color-light);color:var(--el-text-color-secondary);font-size:13px}.student-ai-messages{flex:1;overflow:auto;padding:4px}.student-ai-empty{text-align:center;color:var(--el-text-color-placeholder);padding:50px 12px}.student-ai-message{max-width:88%;margin:12px 0}.student-ai-message>span{font-size:12px;color:var(--el-text-color-secondary)}.student-ai-message :deep(.ai-scientific-content){margin:4px 0;padding:10px 13px;border-radius:14px;background:var(--el-fill-color-light);text-align:left}.student-ai-message.is-user{margin-left:auto;text-align:right}.student-ai-message.is-user :deep(.ai-scientific-content){background:var(--el-color-primary-light-8)}.student-ai-composer{padding-top:12px;border-top:1px solid var(--el-border-color-lighter)}.student-ai-composer>div{display:flex;justify-content:space-between;align-items:center;margin-top:8px;color:var(--el-text-color-secondary);font-size:13px}
.student-ai-controls{display:grid;gap:10px;margin-bottom:12px;padding:12px;border:1px solid var(--el-border-color-lighter);border-radius:12px}.student-ai-controls label{display:flex;gap:8px;align-items:center}.student-ai-controls select{min-width:190px;padding:6px}.student-ai-controls fieldset{display:flex;gap:16px;border:0;padding:0;margin:0}.student-ai-controls small,.student-ai-sources small{color:var(--el-text-color-secondary)}.student-ai-sources{margin:7px 0;padding:9px 12px 9px 28px;border-left:3px solid var(--el-color-primary-light-5);background:var(--el-fill-color-lighter);text-align:left}.student-ai-sources li+li{margin-top:5px}.student-ai-sources a{display:block;color:var(--el-color-primary);overflow-wrap:anywhere}
@media(max-width:640px){.student-ai-heading{flex-direction:column}.student-ai-columns{grid-template-columns:1fr}.student-ai-heading .el-button{width:100%}}
</style>
