<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ApiError } from '../../api/http'
import { clearAiModelKey, createAiModel, fetchAiModels, testAiModel, updateAiModel, type AiModelConfig, type SaveAiModelConfig } from '../../api/admin/aiModels'

type Form=SaveAiModelConfig&{id?:number;apiKeyConfigured:boolean;lastTestStatus:string;lastTestLatencyMillis?:number;lastTestAt?:string}
const defaults:Form[]=[
  {provider:'DEEPSEEK',model:'deepseek-v4-flash',baseUrl:'https://api.deepseek.com',apiKey:'',usage:'TEXT',enabled:false,defaultConfig:true,timeoutMillis:30000,maxTokens:1200,retryCount:1,apiKeyConfigured:false,lastTestStatus:'NOT_TESTED'},
  {provider:'GLM',model:'glm-4.6v-flash',baseUrl:'https://open.bigmodel.cn/api/paas/v4',apiKey:'',usage:'VISION',enabled:false,defaultConfig:true,timeoutMillis:30000,maxTokens:1000,retryCount:1,apiKeyConfigured:false,lastTestStatus:'NOT_TESTED'},
  {provider:'XAI',model:'grok-4.5',baseUrl:'https://api.x.ai/v1',apiKey:'',usage:'VISION',enabled:false,defaultConfig:false,timeoutMillis:60000,maxTokens:1000,retryCount:1,apiKeyConfigured:false,lastTestStatus:'NOT_TESTED'},
  {provider:'GLM',model:'search_pro',baseUrl:'https://open.bigmodel.cn/api/paas/v4',apiKey:'',usage:'SEARCH',enabled:false,defaultConfig:true,timeoutMillis:10000,maxTokens:64,retryCount:1,apiKeyConfigured:false,lastTestStatus:'NOT_TESTED'},
]
const forms=reactive<Form[]>(structuredClone(defaults))
const loading=ref(false);const saving=ref<string>();const testing=ref<string>()
const errorText=(error:unknown,fallback:string)=>(error as ApiError).message||fallback
function apply(index:number,item:AiModelConfig){Object.assign(forms[index],item,{apiKey:'',apiKeyConfigured:item.apiKeyConfigured})}
const formIndex=(usage:string,provider?:string)=>usage==='VISION'?(provider==='XAI'?2:1):({TEXT:0,SEARCH:3} as Record<string,number>)[usage]
const usageTitle=(usage:string,provider:string)=>usage==='VISION'?(provider==='XAI'?'xAI 视觉理解':'GLM 视觉理解'):({TEXT:'DeepSeek 文本推理',SEARCH:'智谱联网搜索'} as Record<string,string>)[usage]
const usageKicker=(usage:string)=>({TEXT:'TEXT BRAIN',VISION:'VISION EYES',SEARCH:'SEARCH SOURCES'} as Record<string,string>)[usage]
const operationKey=(form:Form)=>`${form.usage}:${form.provider}`
async function load(){loading.value=true;try{const page=await fetchAiModels();for(const item of page.records){const index=formIndex(item.usage,item.provider);if(index!==undefined)apply(index,item)}}catch(e){ElMessage.error(errorText(e,'AI 模型配置加载失败。'))}finally{loading.value=false}}
async function save(form:Form){const key=operationKey(form);saving.value=key;try{const body:SaveAiModelConfig={provider:form.provider,model:form.model,baseUrl:form.baseUrl,apiKey:form.apiKey?.trim()||undefined,usage:form.usage,enabled:form.enabled,defaultConfig:form.defaultConfig,timeoutMillis:form.timeoutMillis,maxTokens:form.maxTokens,retryCount:form.retryCount};const item=form.id?await updateAiModel(form.id,body):await createAiModel(body);apply(formIndex(form.usage,form.provider),item);if(form.usage==='VISION'&&form.defaultConfig)forms.filter(item=>item.usage==='VISION'&&item.provider!==form.provider).forEach(item=>item.defaultConfig=false);ElMessage.success('模型配置已保存并立即生效。')}catch(e){ElMessage.error(errorText(e,'AI 模型配置保存失败。'))}finally{saving.value=undefined}}
async function clearKey(form:Form){if(!form.id)return;await ElMessageBox.confirm('清除后该模型将不可调用。','确认清除 API Key',{type:'warning'});try{apply(formIndex(form.usage,form.provider),await clearAiModelKey(form.id));ElMessage.success('API Key 已清除。')}catch(e){ElMessage.error(errorText(e,'API Key 清除失败。'))}}
async function test(form:Form){if(!form.id){ElMessage.warning('请先保存配置。');return}const key=operationKey(form);testing.value=key;try{const result=await testAiModel(form.id);await load();result.success?ElMessage.success(`${result.model} 连接成功，耗时 ${result.latencyMillis} ms。`):ElMessage.warning(`${result.httpStatus?`HTTP ${result.httpStatus} · `:''}${result.safeErrorCode||'UNKNOWN'} · ${result.safeError||'连接测试失败'}${result.retryAfterSeconds?` · ${result.retryAfterSeconds}s 后可重试`:''} · ${result.latencyMillis} ms`)}catch(e){ElMessage.error(errorText(e,'连接测试失败。'))}finally{testing.value=undefined}}
const statusLabel=(value:string)=>({NOT_TESTED:'尚未测试',SUCCESS:'连接正常',FAILED:'测试失败'} as Record<string,string>)[value]||value
onMounted(load)
</script>

<template>
  <section class="admin-page ai-model-page" v-loading="loading">
    <div class="page-heading"><div><span class="page-kicker">AI PROVIDERS</span><h1>AI 模型管理</h1><p>统一管理文本推理、视觉理解与联网搜索。密钥只保存在服务端 MySQL；页面加载、接口响应和调用日志均不回显完整值。</p></div></div>
    <div class="ai-model-grid">
      <article v-for="form in forms" :key="`${form.usage}:${form.provider}`" class="ai-model-card">
        <header><div><span>{{ usageKicker(form.usage) }}</span><h2>{{ usageTitle(form.usage,form.provider) }}</h2></div><el-switch v-model="form.enabled" inline-prompt active-text="启用" inactive-text="停用" /></header>
        <el-form label-position="top" @submit.prevent="save(form)">
          <div class="ai-model-fields"><el-form-item label="Base URL"><el-input v-model="form.baseUrl" /></el-form-item><el-form-item label="模型 / 引擎"><el-select v-if="form.usage!=='VISION'||form.provider==='GLM'" v-model="form.model"><template v-if="form.usage==='TEXT'"><el-option label="deepseek-v4-flash" value="deepseek-v4-flash" /><el-option label="deepseek-v4-pro" value="deepseek-v4-pro" /></template><el-option v-else-if="form.usage==='VISION'" label="glm-4.6v-flash" value="glm-4.6v-flash" /><template v-else><el-option label="search_std" value="search_std" /><el-option label="search_pro" value="search_pro" /><el-option label="search_pro_sogou" value="search_pro_sogou" /><el-option label="search_pro_quark" value="search_pro_quark" /></template></el-select><el-input v-else v-model="form.model" placeholder="管理员从 xAI 官方模型列表确认后填写" /></el-form-item></div>
          <el-form-item v-if="form.usage==='VISION'"><el-switch v-model="form.defaultConfig" active-text="当前视觉 Provider（不会隐式切换）" /></el-form-item>
          <el-form-item label="API Key"><el-input v-model="form.apiKey" type="password" show-password autocomplete="new-password" :placeholder="form.apiKeyConfigured?'•••••••• 已配置；留空保持不变':'输入 API Key'" /></el-form-item>
          <div class="ai-number-fields"><el-form-item label="超时（ms）"><el-input-number v-model="form.timeoutMillis" :min="1000" :max="120000" :step="1000" /></el-form-item><el-form-item label="重试"><el-input-number v-model="form.retryCount" :min="0" :max="1" /></el-form-item><el-form-item label="最大输出 Token"><el-input-number v-model="form.maxTokens" :min="64" :max="8192" :step="64" /></el-form-item></div>
          <div class="ai-model-status"><div><span>最近测试</span><strong>{{ statusLabel(form.lastTestStatus) }}</strong><small v-if="form.lastTestAt">{{ form.lastTestLatencyMillis }} ms · {{ new Date(form.lastTestAt).toLocaleString() }}</small></div><el-tag :type="form.apiKeyConfigured?'success':'info'">{{ form.apiKeyConfigured?'Key 已配置':'Key 未配置' }}</el-tag></div>
          <footer><el-button type="primary" native-type="submit" :loading="saving===operationKey(form)">保存配置</el-button><el-button :loading="testing===operationKey(form)" @click="test(form)">测试连接</el-button><el-button v-if="form.apiKeyConfigured" type="danger" plain @click="clearKey(form)">清除 Key</el-button></footer>
        </el-form>
      </article>
    </div>
  </section>
</template>

<style scoped>
.ai-model-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:20px}.ai-model-card{padding:26px;border:1px solid var(--color-border,#dce7f1);border-radius:18px;background:var(--color-surface,#fff);box-shadow:0 14px 36px rgba(42,69,97,.07)}.ai-model-card header,.ai-model-card footer,.ai-model-status{display:flex;justify-content:space-between;align-items:center;gap:16px}.ai-model-card header{margin-bottom:22px}.ai-model-card header span,.ai-model-status span{color:var(--el-text-color-secondary);font-size:11px;letter-spacing:.1em}.ai-model-card h2{margin:4px 0 0;color:#214363}.ai-model-fields{display:grid;grid-template-columns:1.5fr 1fr;gap:14px}.ai-number-fields{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}.ai-number-fields :deep(.el-input-number){width:100%}.ai-model-status{padding:14px;margin:4px 0 18px;border-radius:12px;background:var(--el-fill-color-light)}.ai-model-status strong,.ai-model-status small{display:block;margin-top:3px}.ai-model-status small{color:var(--el-text-color-secondary)}
@media(max-width:960px){.ai-model-grid{grid-template-columns:1fr}}@media(max-width:640px){.ai-model-fields,.ai-number-fields{grid-template-columns:1fr}.ai-model-card footer{align-items:stretch;flex-direction:column}.ai-model-card footer .el-button{width:100%;margin-left:0}}
</style>
