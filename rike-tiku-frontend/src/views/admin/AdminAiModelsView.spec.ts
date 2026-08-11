// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminAiModelsView from './AdminAiModelsView.vue'

const api=vi.hoisted(()=>({fetchAiModels:vi.fn(),createAiModel:vi.fn(),updateAiModel:vi.fn(),clearAiModelKey:vi.fn(),testAiModel:vi.fn()}))
vi.mock('../../api/admin/aiModels',()=>api)
vi.mock('element-plus',()=>({ElMessage:{success:vi.fn(),error:vi.fn(),warning:vi.fn()},ElMessageBox:{confirm:vi.fn()}}))
const stubs={ElAlert:true,ElSwitch:true,ElForm:{template:'<form><slot /></form>'},ElFormItem:{template:'<label><slot /></label>'},ElInput:true,ElSelect:{template:'<div><slot /></div>'},ElOption:true,ElInputNumber:true,ElTag:{template:'<span><slot /></span>'},ElButton:{props:['loading'],template:'<button @click="$emit(\'click\')"><slot /></button>'}}
const config=(usage:'TEXT'|'VISION')=>({id:usage==='TEXT'?1:2,provider:usage==='TEXT'?'DEEPSEEK':'GLM',model:usage==='TEXT'?'deepseek-v4-flash':'glm-4.6v-flash',baseUrl:'https://safe.example',usage,enabled:true,defaultConfig:true,timeoutMillis:30000,maxTokens:1000,retryCount:1,apiKeyConfigured:true,lastTestStatus:'SUCCESS',lastTestLatencyMillis:88,lastTestAt:'2026-08-11T12:00:00',createdAt:'',updatedAt:''})
describe('AI 模型管理页',()=>{beforeEach(()=>{vi.clearAllMocks();api.fetchAiModels.mockResolvedValue({records:[config('TEXT'),config('VISION')]})})
  it('并列展示文本与视觉配置、Key 遮罩和安全连接状态',async()=>{const wrapper=mount(AdminAiModelsView,{global:{stubs,directives:{loading:()=>undefined}}});await flushPromises();expect(wrapper.text()).toContain('DeepSeek 文本推理','GLM 视觉理解','Key 已配置','连接正常');expect(wrapper.text()).not.toContain('text-secret','vision-secret');expect(api.fetchAiModels).toHaveBeenCalled()})
})
