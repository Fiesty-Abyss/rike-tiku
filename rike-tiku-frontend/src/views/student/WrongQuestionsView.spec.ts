// @vitest-environment jsdom
import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import WrongQuestionsView from './WrongQuestionsView.vue'

const { fetchWrongQuestions, fetchWrongQuestion, fetchPracticeOptions, retryWrongQuestion, push } = vi.hoisted(() => ({ fetchWrongQuestions: vi.fn(), fetchWrongQuestion:vi.fn(), fetchPracticeOptions:vi.fn(), retryWrongQuestion:vi.fn(), push:vi.fn() }))
vi.mock('vue-router', () => ({ useRoute: () => ({ query: { subjectCode:'BIOLOGY' } }), useRouter: () => ({ push }) }))
vi.mock('../../api/student/practice', () => ({ fetchWrongQuestions, fetchWrongQuestion, fetchPracticeOptions, retryWrongQuestion }))
vi.mock('element-plus', () => ({ ElMessage:{error:vi.fn(),success:vi.fn()}, ElMessageBox:{confirm:vi.fn()} }))

const subjects = [
  { id:1, code:'PHYSICS', name:'物理' },
  { id:2, code:'CHEMISTRY', name:'化学' },
  { id:3, code:'BIOLOGY', name:'生物' },
]
const biologyPoints = [{id:71,name:'细胞结构',path:'分子与细胞>细胞结构'}]
const physicsPoints = [{id:11,name:'牛顿定律',path:'力学>牛顿定律'}]
const row = {questionId:3,subjectCode:'BIOLOGY',subjectName:'生物',questionType:'FILL_BLANK',stemSummary:'细胞结构',errorCount:1,consecutiveCorrectCount:0,status:'NEW',lastWrongAt:'',knowledgePoints:biologyPoints}

const ElSelect = defineComponent({
  name:'ElSelect', inheritAttrs:false,
  props:{modelValue:[Number,String],disabled:Boolean,placeholder:String}, emits:['update:modelValue','change'],
  template:'<select v-bind="$attrs" :disabled="disabled" :data-placeholder="placeholder" :value="modelValue" @change="pick"><slot /></select>',
  methods:{pick(event:Event){const value=(event.target as HTMLSelectElement).value;const parsed=value===''?undefined:Number(value);this.$emit('update:modelValue',parsed);this.$emit('change',parsed)}},
})
const ElOption = defineComponent({name:'ElOption',props:{label:String,value:[Number,String]},template:'<option :value="value">{{ label }}</option>'})
const commonStubs = {
  ElSelect, ElOption,
  ElInput:{template:'<input />'}, ElDatePicker:true,
  ElButton:{template:'<button @click="$emit(\'click\')"><slot /></button>'},
  ElTable:{props:['data'],template:'<div>{{ JSON.stringify(data) }}<slot /></div>'}, ElTableColumn:true,
  ElTag:{template:'<span><slot /></span>'}, ElDrawer:true, ElPagination:true, QuestionContent:true,
}

describe('错题本真实学科与知识点筛选', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    fetchWrongQuestions.mockResolvedValue({items:[row],total:1,page:0,size:20})
    fetchPracticeOptions.mockImplementation((subjectId?:number) => Promise.resolve(
      subjectId === 3 ? {subjects,knowledgePoints:biologyPoints}
        : subjectId === 1 ? {subjects,knowledgePoints:physicsPoints}
          : {subjects,knowledgePoints:[]},
    ))
    fetchWrongQuestion.mockResolvedValue({aiAnalysisAnswerFactId:501,wrongQuestion:row,stem:'细胞结构题',options:[],latestStudentAnswer:['线粒体'],correctAnswer:['叶绿体'],standardAnalysis:'STANDARD 解析',knowledgePoints:biologyPoints,attachments:[]})
  })

  it('首次取得三科学科、映射路由 BIOLOGY，并在切换 PHYSICS 时清空旧知识点后重新加载', async () => {
    const wrapper = mount(WrongQuestionsView,{global:{directives:{loading:()=>undefined},stubs:commonStubs}})
    await flushPromises()

    expect(fetchPracticeOptions.mock.calls).toEqual([[],[3]])
    const subject = wrapper.get('[data-testid="wrong-subject-filter"]')
    expect((subject.element as HTMLSelectElement).value).toBe('3')
    expect(subject.text()).toContain('物理')
    expect(subject.text()).toContain('化学')
    expect(subject.text()).toContain('生物')
    expect(wrapper.get('[data-testid="wrong-knowledge-filter"]').text()).toContain('分子与细胞>细胞结构')
    expect(fetchWrongQuestions).toHaveBeenCalledWith(expect.objectContaining({subjectCode:'BIOLOGY',page:0,size:20}))

    await wrapper.get('[data-testid="wrong-knowledge-filter"]').setValue('71')
    await wrapper.get('[data-testid="wrong-subject-filter"]').setValue('1')
    await flushPromises()

    expect(fetchPracticeOptions).toHaveBeenLastCalledWith(1)
    const knowledge = wrapper.get('[data-testid="wrong-knowledge-filter"]')
    expect((knowledge.element as HTMLSelectElement).value).toBe('')
    expect(knowledge.text()).toContain('力学>牛顿定律')
    expect(knowledge.text()).not.toContain('分子与细胞>细胞结构')
    expect(fetchWrongQuestions).toHaveBeenLastCalledWith(expect.objectContaining({subjectCode:'PHYSICS',knowledgePointId:undefined}))
  })

  it('错题详情把后端选择的最近错误事实传给 AI，而不是最近正确作答事实', async () => {
    const wrapper = mount(WrongQuestionsView,{global:{directives:{loading:()=>undefined},stubs:{...commonStubs,
      ElTable:{props:['data'],template:'<div><slot /></div>'},
      ElTableColumn:{template:'<div><slot :row="row" /></div>',setup:()=>({row})},
      ElDrawer:{template:'<aside><slot /></aside>'}, AnswerDisplay:true,
      StandardAnalysis:{props:['content'],template:'<div>{{ content }}</div>'},
      StudentAiLearningPanel:{props:['answerFactId','wrong'],template:'<div data-testid="wrong-ai-fact">{{ answerFactId }} / {{ wrong }}</div>'},
    }}})
    await flushPromises()
    await wrapper.findAll('button').find(button => button.text() === '详情')!.trigger('click')
    await flushPromises()
    expect(fetchWrongQuestion).toHaveBeenCalledWith(3)
    expect(wrapper.get('[data-testid="wrong-ai-fact"]').text()).toBe('501 / true')
    expect(wrapper.text()).toContain('STANDARD 解析')
  })

  it('学生端不展示日期筛选、最近错误列或直接移出按钮，再做一次携带错题上下文', async () => {
    retryWrongQuestion.mockResolvedValue({ id:77 })
    const wrapper = mount(WrongQuestionsView,{global:{directives:{loading:()=>undefined},stubs:{...commonStubs,
      ElTable:{props:['data'],template:'<div><slot /></div>'},
      ElTableColumn:{template:'<div><slot :row="row" /></div>',setup:()=>({row})},
    }}})
    await flushPromises()
    expect(wrapper.findComponent({name:'ElDatePicker'}).exists()).toBe(false)
    expect(wrapper.text()).not.toContain('最近错误')
    expect(wrapper.findAll('button').some((button) => button.text().trim() === '移出')).toBe(false)
    await wrapper.findAll('button').find(button=>button.text().includes('再做一次'))!.trigger('click')
    await flushPromises()
    expect(retryWrongQuestion).toHaveBeenCalledWith(3)
    expect(push).toHaveBeenCalledWith({path:'/student/practice/77',query:{fromWrongBook:'true',wrongQuestionId:'3'}})
  })
})
