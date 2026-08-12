// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AiQuestionGenerationView from './AiQuestionGenerationView.vue'

const api=vi.hoisted(()=>({
  client:{fetchMothers:vi.fn(),fetchTasks:vi.fn(),fetchKnowledgePoints:vi.fn(),createTask:vi.fn(),reviewCandidate:vi.fn(),fetchStats:vi.fn()},
}))
vi.mock('../../api/admin/aiGeneration',()=>({adminAiGenerationClient:api.client}))
vi.mock('element-plus',()=>({ElMessage:{success:vi.fn(),error:vi.fn(),warning:vi.fn()}}))
const stubs={ElButton:{template:'<button @click="$emit(\'click\')"><slot /></button>'},ElTag:{template:'<span><slot /></span>'},ElAlert:{props:['title'],template:'<div>{{title}}<slot /></div>'},ElForm:{template:'<form><slot /></form>'},ElFormItem:{props:['label'],template:'<label>{{label}}<slot /></label>'},ElSelect:{template:'<div><slot /></div>'},ElOption:true,ElInputNumber:true,ElInput:true,ElEmpty:true,ElDrawer:{props:['modelValue'],template:'<aside v-if="modelValue"><slot /></aside>'},ElDivider:true,ElRadioGroup:{template:'<div><slot /></div>'},ElRadioButton:{template:'<span><slot /></span>'},ElRadio:{template:'<span><slot /></span>'}}
const candidate={questionId:19,taskId:3,stem:'受力变式题',questionType:'SINGLE_CHOICE',difficulty:2,status:'PENDING',variationSummary:'改变情境',duplicateWarning:'SUSPECTED_DUPLICATE',visionUsed:true,provider:'deepseek',model:'deepseek-v4-flash',correctAnswer:'{"optionLabels":["A"]}',standardAnalysis:'候选解析',knowledgePoints:[{id:1,name:'受力'}],quality:{reviewResult:'PENDING'}}

describe('管理员 AI 候选题页面',()=>{
  beforeEach(()=>{vi.clearAllMocks();api.client.fetchMothers.mockResolvedValue([]);api.client.fetchStats.mockResolvedValue({tasks:1,generated:1,suspectedDuplicates:1,approved:0,rejected:0});api.client.fetchTasks.mockResolvedValue([{id:3,motherQuestionId:1,creatorId:1,creatorRole:'ADMIN',questionType:'SINGLE_CHOICE',knowledgePointIds:[1],targetDifficulty:2,variationMode:'SCENARIO',requestedCount:1,requestHash:'h',provider:'deepseek',model:'deepseek-v4-flash',promptVersion:'v1',status:'SUCCESS',generatedCount:1,visionUsed:true,latencyMillis:90,createdAt:'',candidates:[candidate]}])})
  it('保留管理员全局统计、PENDING、重复和视觉审核视图',async()=>{const wrapper=mount(AiQuestionGenerationView,{global:{stubs,directives:{loading:()=>undefined}}});await flushPromises();expect(api.client.fetchStats).toHaveBeenCalledOnce();expect(wrapper.text()).toContain('生成任务','所有结果只进入 PENDING','疑似相似','使用视觉','deepseek-v4-flash');await wrapper.find('button.candidate-card').trigger('click');expect(wrapper.text()).toContain('候选内容不是系统 STANDARD','候选解析','质量评价')})
})
