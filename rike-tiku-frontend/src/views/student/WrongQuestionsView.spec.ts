// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import WrongQuestionsView from './WrongQuestionsView.vue'

const { fetchWrongQuestions, fetchWrongQuestion, fetchPracticeOptions } = vi.hoisted(() => ({ fetchWrongQuestions: vi.fn(), fetchWrongQuestion:vi.fn(), fetchPracticeOptions:vi.fn() }))
vi.mock('vue-router', () => ({ useRoute: () => ({ query: { subjectCode:'BIOLOGY' } }), useRouter: () => ({ push:vi.fn() }) }))
vi.mock('../../api/student/practice', () => ({ fetchWrongQuestions, fetchWrongQuestion, fetchPracticeOptions, archiveWrongQuestion:vi.fn(), retryWrongQuestion:vi.fn() }))
vi.mock('element-plus', () => ({ ElMessage:{error:vi.fn()} }))

describe('错题本实时学科筛选', () => {
  const row = {questionId:3,subjectCode:'BIOLOGY',subjectName:'生物',questionType:'FILL_BLANK',stemSummary:'细胞结构',errorCount:1,consecutiveCorrectCount:0,status:'NEW',lastWrongAt:''}
  beforeEach(() => {
    vi.clearAllMocks()
    fetchWrongQuestions.mockResolvedValue({items:[row],total:1,page:0,size:20})
    fetchPracticeOptions.mockResolvedValue({subjects:[],knowledgePoints:[{id:71,name:'细胞结构',path:'分子与细胞>细胞结构'}]})
    fetchWrongQuestion.mockResolvedValue({aiAnalysisAnswerFactId:501,wrongQuestion:row,stem:'细胞结构题',options:[],latestStudentAnswer:['线粒体'],correctAnswer:['叶绿体'],standardAnalysis:'STANDARD 解析',knowledgePoints:[],attachments:[]})
  })

  it('进入生物错题时按 subjectCode 立即读取，不依赖固定数据库 ID', async () => {
    const wrapper = mount(WrongQuestionsView,{global:{directives:{loading:()=>undefined},stubs:{
      ElButton:{template:'<button><slot /></button>'},ElTable:{props:['data'],template:'<div>{{ JSON.stringify(data) }}</div>'},ElTableColumn:true,ElTag:{template:'<span><slot /></span>'},ElDrawer:true,QuestionContent:true,
    }}})
    await flushPromises()
    expect(fetchWrongQuestions).toHaveBeenCalledTimes(1)
    expect(fetchWrongQuestions).toHaveBeenCalledWith(expect.objectContaining({subjectCode:'BIOLOGY',page:0,size:20}))
    expect(wrapper.text()).toContain('细胞结构')
    expect(wrapper.text()).not.toContain('PHYSICS')
  })

  it('错题详情把后端选择的最近错误事实传给 AI，而不是最近正确作答事实', async () => {
    const wrapper = mount(WrongQuestionsView,{global:{directives:{loading:()=>undefined},stubs:{
      ElButton:{template:'<button @click="$emit(\'click\')"><slot /></button>'},
      ElTable:{props:['data'],template:'<div><slot /></div>'},
      ElTableColumn:{template:'<div><slot :row="row" /></div>',setup:()=>({row})},
      ElTag:{template:'<span><slot /></span>'},ElDrawer:{template:'<aside><slot /></aside>'},QuestionContent:true,
      AnswerDisplay:true,StandardAnalysis:{props:['content'],template:'<div>{{ content }}</div>'},
      StudentAiLearningPanel:{props:['answerFactId','wrong'],template:'<div data-testid="wrong-ai-fact">{{ answerFactId }} / {{ wrong }}</div>'},
    }}})
    await flushPromises()
    await wrapper.findAll('button').find(button => button.text() === '详情')!.trigger('click')
    await flushPromises()
    expect(fetchWrongQuestion).toHaveBeenCalledWith(3)
    expect(wrapper.get('[data-testid="wrong-ai-fact"]').text()).toBe('501 / true')
    expect(wrapper.text()).toContain('STANDARD 解析')
  })
})
