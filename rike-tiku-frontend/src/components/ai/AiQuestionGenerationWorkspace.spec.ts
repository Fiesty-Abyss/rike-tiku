// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AiGenerationClient } from '../../api/aiGeneration'
import AiQuestionGenerationWorkspace from './AiQuestionGenerationWorkspace.vue'

const messages=vi.hoisted(()=>({success:vi.fn(),error:vi.fn(),warning:vi.fn()}))
vi.mock('element-plus',()=>({ElMessage:messages}))
const stubs={ElButton:{template:'<button @click="$emit(\'click\')"><slot /></button>'},ElTag:{template:'<span><slot /></span>'},ElAlert:{props:['title'],template:'<div>{{title}}<slot /></div>'},ElForm:{template:'<form><slot /></form>'},ElFormItem:{props:['label'],template:'<label>{{label}}<slot /></label>'},ElSelect:{template:'<div><slot /></div>'},ElOption:true,ElInputNumber:true,ElInput:true,ElEmpty:true,ElDrawer:{props:['modelValue'],template:'<aside v-if="modelValue"><slot /></aside>'},ElDivider:{props:['contentPosition'],template:'<div><slot /></div>'},ElRadioGroup:{template:'<div><slot /></div>'},ElRadioButton:{template:'<span><slot /></span>'},ElRadio:{template:'<span><slot /></span>'}}
const candidate={questionId:19,taskId:3,stem:'教师授权学科候选题',questionType:'SINGLE_CHOICE',difficulty:2,status:'PENDING',variationSummary:'更换实验情境',duplicateWarning:'SUSPECTED_DUPLICATE',visionUsed:true,provider:'deepseek',model:'deepseek-v4-flash',correctAnswer:'{"optionLabels":["A"]}',standardAnalysis:'候选解析，待人工复核',knowledgePoints:[{id:11,name:'牛顿第二定律'}],quality:{reviewResult:'PENDING'}}
const task={id:3,motherQuestionId:1,creatorId:8,creatorRole:'TEACHER',questionType:'SINGLE_CHOICE',knowledgePointIds:[11],targetDifficulty:2,variationMode:'SCENARIO',requestedCount:1,requestHash:'h',provider:'deepseek',model:'deepseek-v4-flash',promptVersion:'v1',status:'SUCCESS',generatedCount:1,visionUsed:true,latencyMillis:90,createdAt:'',candidates:[candidate]}
function client():AiGenerationClient{return {fetchMothers:vi.fn().mockResolvedValue([{id:1,subjectId:1,subjectCode:'PHYSICS',stem:'牛顿第二定律母题',questionType:'SINGLE_CHOICE',difficulty:2}]),fetchTasks:vi.fn().mockResolvedValue([task]),fetchKnowledgePoints:vi.fn().mockResolvedValue([{id:11,name:'牛顿第二定律',path:'物理/力学/牛顿第二定律'}]),createTask:vi.fn().mockResolvedValue(task),reviewCandidate:vi.fn().mockResolvedValue({...candidate,status:'PUBLISHED'})}}

describe('教师 AI 候选共享工作区',()=>{
  beforeEach(()=>vi.clearAllMocks())
  it('完成母题加载、生成、候选查看和 APPROVE/REJECT 质量评价',async()=>{
    const api=client();const wrapper=mount(AiQuestionGenerationWorkspace,{props:{mode:'TEACHER',client:api},global:{stubs,directives:{loading:()=>undefined}}});await flushPromises()
    expect(api.fetchMothers).toHaveBeenCalledOnce();expect(api.fetchTasks).toHaveBeenCalledOnce();expect(wrapper.text()).toContain('仅显示已发布母题','疑似相似','教师授权学科候选题');expect(wrapper.text()).not.toContain('AI 模型管理','deepseek-v4-flash','Provider','使用视觉','技术结构')
    const vm=wrapper.vm as unknown as {form:{motherQuestionId?:number;knowledgePointIds:number[]};review:{reviewResult:string;reviewMinutes:number;reviewComment?:string};changeMother:()=>Promise<void>;generate:()=>Promise<void>;submitReview:()=>Promise<void>}
    vm.form.motherQuestionId=1;await vm.changeMother();vm.form.knowledgePointIds=[11];await vm.generate()
    expect(api.fetchKnowledgePoints).toHaveBeenCalledWith(1);expect(api.createTask).toHaveBeenCalledWith(expect.objectContaining({motherQuestionId:1,knowledgePointIds:[11]}))
    await wrapper.find('button.candidate-card').trigger('click')
    expect(wrapper.text()).toContain('候选答案','候选解析，待人工复核','牛顿第二定律','学科正确性','答案正确性','可解性','知识一致性','难度匹配','审核耗时（分钟）')
    vm.review.reviewResult='APPROVED';vm.review.reviewMinutes=6;await vm.submitReview();expect(api.reviewCandidate).toHaveBeenLastCalledWith(19,expect.objectContaining({reviewResult:'APPROVED',reviewMinutes:6}))
    await wrapper.find('button.candidate-card').trigger('click');vm.review.reviewResult='REJECTED';vm.review.reviewComment='知识点偏离';await vm.submitReview();expect(api.reviewCandidate).toHaveBeenLastCalledWith(19,expect.objectContaining({reviewResult:'REJECTED',reviewComment:'知识点偏离'}))
  })
  it('加载失败只显示受控错误',async()=>{const api=client();vi.mocked(api.fetchMothers).mockRejectedValue(new Error('network'));mount(AiQuestionGenerationWorkspace,{props:{mode:'TEACHER',client:api},global:{stubs,directives:{loading:()=>undefined}}});await flushPromises();expect(messages.error).toHaveBeenCalledWith('network')})
})
