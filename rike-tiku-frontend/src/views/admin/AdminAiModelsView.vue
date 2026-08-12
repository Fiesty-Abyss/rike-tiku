<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ApiError } from '../../api/http'
import { clearAiModelKey, createAiModel, fetchAiModels, testAiModel, updateAiModel, type AiModelConfig, type SaveAiModelConfig } from '../../api/admin/aiModels'

type Form=SaveAiModelConfig&{id?:number;apiKeyConfigured:boolean;lastTestStatus:string;lastTestLatencyMillis?:number;lastTestAt?:string}
const defaults:Form[]=[
  {provider:'DEEPSEEK',model:'deepseek-v4-flash',baseUrl:'https://api.deepseek.com',apiKey:'',usage:'TEXT',enabled:false,defaultConfig:true,timeoutMillis:30000,maxTokens:1200,retryCount:1,apiKeyConfigured:false,lastTestStatus:'NOT_TESTED'},
  {provider:'GLM',model:'glm-4.6v-flash',baseUrl:'https://open.bigmodel.cn/api/paas/v4',apiKey:'',usage:'VISION',enabled:false,defaultConfig:true,timeoutMillis:30000,maxTokens:1000,retryCount:1,apiKeyConfigured:false,lastTestStatus:'NOT_TESTED'},
]
const forms=reactive<Form[]>(structuredClone(defaults))
const loading=ref(false);const saving=ref<string>();const testing=ref<string>()
const errorText=(error:unknown,fallback:string)=>(error as ApiError).message||fallback
function apply(index:number,item:AiModelConfig){Object.assign(forms[index],item,{apiKey:'',apiKeyConfigured:item.apiKeyConfigured})}
async function load(){loading.value=true;try{const page=await fetchAiModels();for(const item of page.records){const index=item.usage==='TEXT'?0:1;apply(index,item)}}catch(e){ElMessage.error(errorText(e,'AI 模型配置加载失败。'))}finally{loading.value=false}}
async function save(form:Form){saving.value=form.usage;try{const body:SaveAiModelConfig={provider:form.provider,model:form.model,baseUrl:form.baseUrl,apiKey:form.apiKey?.trim()||undefined,usage:form.usage,enabled:form.enabled,defaultConfig:form.defaultConfig,timeoutMillis:form.timeoutMillis,maxTokens:form.maxTokens,retryCount:form.retryCount};const item=form.id?await updateAiModel(form.id,body):await createAiModel(body);apply(form.usage==='TEXT'?0:1,item);ElMessage.success('模型配置已保存并立即生效。')}catch(e){ElMessage.error(errorText(e,'模型配置保存失败。'))}finally{saving.value=undefined}}
async function clearKey(form:Form){if(!form.id)return;await ElMessageBox.confirm('清除后该模型在没有环境变量回退时将不可调用。','确认清除 API Key',{type:'warning'});try{apply(form.usage==='TEXT'?0:1,await clearAiModelKey(form.id));ElMessage.success('API Key 已清除。')}catch(e){ElMessage.error(errorText(e,'API Key 清除失败。'))}}
async function test(form:Form){if(!form.id){ElMessage.warning('请先保存配置。');return}testing.value=form.usage;try{const result=await testAiModel(form.id);await load();result.success?ElMessage.success(`${result.model} 连接成功，耗时 ${result.latencyMillis} ms。`):ElMessage.warning(result.safeError||'连接测试失败。')}catch(e){ElMessage.error(errorText(e,'连接测试失败。'))}finally{testing.value=undefined}}
const statusLabel=(value:string)=>({NOT_TESTED:'尚未测试',SUCCESS:'连接正常',FAILED:'测试失败'} as Record<string,string>)[value]||value
onMounted(load)
</script>

<template>
  <section class="admin-page ai-model-page" v-loading="loading">
    <div class="page-heading"><div><span class="page-kicker">LOCAL DEMO PROVIDERS</span><h1>AI 模型管理</h1><p>本地毕设演示配置。密钥只保存在服务端本地 MySQL；页面加载、接口响应和调用日志均不回显完整值。</p></div></div>
    <el-alert title="安全边界" type="info" :closable="false" show-icon description="保存后只显示“已配置”。如需替换，请重新输入；不要把真实 Key 写入代码、文档或截图。" />
    <div class="ai-model-grid">
      <article v-for="form in forms" :key="form.usage" class="ai-model-card">
        <header><div><span>{{ form.usage==='TEXT'?'TEXT BRAIN':'VISION EYES' }}</span><h2>{{ form.usage==='TEXT'?'DeepSeek 文本推理':'GLM 视觉理解' }}</h2></div><el-switch v-model="form.enabled" inline-prompt active-text="启用" inactive-text="停用" /></header>
        <el-form label-position="top" @submit.prevent="save(form)">
          <div class="ai-model-fields"><el-form-item label="Base URL"><el-input v-model="form.baseUrl" /></el-form-item><el-form-item label="模型"><el-select v-model="form.model"><el-option v-if="form.usage==='TEXT'" label="deepseek-v4-flash" value="deepseek-v4-flash" /><el-option v-if="form.usage==='TEXT'" label="deepseek-v4-pro" value="deepseek-v4-pro" /><el-option v-else label="glm-4.6v-flash" value="glm-4.6v-flash" /></el-select></el-form-item></div>
          <el-form-item label="API Key"><el-input v-model="form.apiKey" type="password" show-password autocomplete="new-password" :placeholder="form.apiKeyConfigured?'•••••••• 已配置；留空保持不变':'输入本地演示 API Key'" /></el-form-item>
          <div class="ai-number-fields"><el-form-item label="超时（ms）"><el-input-number v-model="form.timeoutMillis" :min="1000" :max="120000" :step="1000" /></el-form-item><el-form-item label="重试"><el-input-number v-model="form.retryCount" :min="0" :max="1" /></el-form-item><el-form-item label="最大输出 Token"><el-input-number v-model="form.maxTokens" :min="64" :max="8192" :step="64" /></el-form-item></div>
          <div class="ai-model-status"><div><span>最近测试</span><strong>{{ statusLabel(form.lastTestStatus) }}</strong><small v-if="form.lastTestAt">{{ form.lastTestLatencyMillis }} ms · {{ new Date(form.lastTestAt).toLocaleString() }}</small></div><el-tag :type="form.apiKeyConfigured?'success':'info'">{{ form.apiKeyConfigured?'Key 已配置':'Key 未配置' }}</el-tag></div>
          <footer><el-button type="primary" native-type="submit" :loading="saving===form.usage">保存配置</el-button><el-button :loading="testing===form.usage" @click="test(form)">{{ form.usage==='TEXT'?'测试连接':'测试视觉' }}</el-button><el-button v-if="form.apiKeyConfigured" type="danger" plain @click="clearKey(form)">清除 Key</el-button></footer>
        </el-form>
      </article>
    </div>
  </section>
</template>

<style scoped>
.ai-model-page>.el-alert{margin-bottom:20px}.ai-model-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:20px}.ai-model-card{padding:26px;border:1px solid var(--color-border,#dce7f1);border-radius:18px;background:var(--color-surface,#fff);box-shadow:0 14px 36px rgba(42,69,97,.07)}.ai-model-card header,.ai-model-card footer,.ai-model-status{display:flex;justify-content:space-between;align-items:center;gap:16px}.ai-model-card header{margin-bottom:22px}.ai-model-card header span,.ai-model-status span{color:var(--el-text-color-secondary);font-size:11px;letter-spacing:.1em}.ai-model-card h2{margin:4px 0 0;color:#214363}.ai-model-fields{display:grid;grid-template-columns:1.5fr 1fr;gap:14px}.ai-number-fields{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}.ai-number-fields :deep(.el-input-number){width:100%}.ai-model-status{padding:14px;margin:4px 0 18px;border-radius:12px;background:var(--el-fill-color-light)}.ai-model-status strong,.ai-model-status small{display:block;margin-top:3px}.ai-model-status small{color:var(--el-text-color-secondary)}
@media(max-width:960px){.ai-model-grid{grid-template-columns:1fr}}@media(max-width:640px){.ai-model-fields,.ai-number-fields{grid-template-columns:1fr}.ai-model-card footer{align-items:stretch;flex-direction:column}.ai-model-card footer .el-button{width:100%;margin-left:0}}
</style>
