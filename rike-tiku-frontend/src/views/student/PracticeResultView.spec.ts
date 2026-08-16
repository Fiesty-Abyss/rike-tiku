// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import PracticeResultView from './PracticeResultView.vue'

const { fetchPracticeResult, archiveWrongQuestion, push, replace, confirm } = vi.hoisted(() => ({ fetchPracticeResult: vi.fn(), archiveWrongQuestion:vi.fn(), push: vi.fn(), replace:vi.fn(), confirm:vi.fn() }))
const routeState = { params: { id: '8' }, query: {} as Record<string,string>, path:'/student/practice/8/result' }
vi.mock('vue-router', () => ({ useRoute: () => routeState, useRouter: () => ({ push, replace }) }))
vi.mock('../../api/student/practice', () => ({ fetchPracticeResult, archiveWrongQuestion }))
vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn(), success:vi.fn() }, ElMessageBox: { confirm } }))

const record = (id:number, order:number, correct:boolean) => ({ answerFactId:1000+id,question:{practiceQuestionId:id,questionId:100+id,order,questionType:'SINGLE_CHOICE',stem:`题干${order}`,difficulty:2,score:10,blankCount:0,options:[{label:'A',content:'速度增大'},{label:'B',content:'速度减小'}],knowledgePoints:[{id:9,name:'力学',path:'力学>运动'}],attachments:[]},studentAnswer:'B',correctAnswer:'A',standardAnalysis:`结论：选择 A。\n\nA. 速度增大：正确。\nB. 速度减小：错误。\n\n关键依据：合力做正功。`,correct,score:correct?10:0 })

describe('练习结果逐题模式', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routeState.query = {}
    replace.mockImplementation(async (location:{query?:Record<string,string>}) => { if(location.query) routeState.query=location.query; return undefined })
    fetchPracticeResult.mockResolvedValue({ sessionId:8,subjectId:1,subjectCode:'PHYSICS',subjectName:'物理',totalCount:3,correctCount:2,totalScore:20,submittedAt:'',questions:[record(1,1,true),record(2,2,false),record(3,3,true)] })
  })

  it('默认定位第一道错题且一次只展示一道，并可前后切换', async () => {
    const wrapper = mount(PracticeResultView, { global:{directives:{loading:()=>undefined},stubs:{
      ElButton:{props:['disabled'],template:'<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>'},
      ElSwitch:true, ElEmpty:true, ElTag:{template:'<span><slot /></span>'},
      QuestionContent:{props:['content'],template:'<span class="question-content">{{ content }}</span>'}, StudentAiLearningPanel:true,
    }}})
    await flushPromises()
    expect(wrapper.text()).toContain('第 2 题')
    expect(wrapper.text()).toContain('题干2')
    expect(wrapper.text()).not.toContain('题干1')
    expect(wrapper.text()).toContain('B.速度减小')
    expect(wrapper.text()).toContain('A.速度增大')
    expect(wrapper.text()).toContain('关键依据')

    await wrapper.findAll('button').find(button => button.text() === '下一题')!.trigger('click')
    expect(wrapper.text()).toContain('题干3')
    expect(wrapper.text()).not.toContain('题干2')
  })

  it('错误答案和正确答案并列使用会话内冻结选项，不请求实时题库', async () => {
    const wrapper = mount(PracticeResultView, { global:{directives:{loading:()=>undefined},stubs:{
      ElButton:{template:'<button @click="$emit(\'click\')"><slot /></button>'}, ElSwitch:true, ElEmpty:true,
      ElTag:{template:'<span><slot /></span>'}, QuestionContent:{props:['content'],template:'<span>{{ content }}</span>'}, StudentAiLearningPanel:true,
    }}})
    await flushPromises()
    const columns = wrapper.findAll('.answer-comparison > div')
    expect(columns[0].text()).toContain('你的答案B.速度减小')
    expect(columns[1].text()).toContain('正确答案A.速度增大')
    expect(fetchPracticeResult).toHaveBeenCalledTimes(1)
  })

  it('知识点和类似练习保留真实学科、知识点及参考题', async () => {
    const wrapper = mount(PracticeResultView, { global:{directives:{loading:()=>undefined},stubs:{
      ElButton:{template:'<button @click="$emit(\'click\')"><slot /></button>'}, ElSwitch:true, ElEmpty:true,
      ElTag:{template:'<span><slot /></span>'}, QuestionContent:{props:['content'],template:'<span>{{ content }}</span>'}, StudentAiLearningPanel:true,
    }}})
    await flushPromises()
    await wrapper.findAll('button').find(button => button.text().includes('力学>运动'))!.trigger('click')
    expect(push).toHaveBeenCalledWith({ path:'/student/subjects/physics', query:{ knowledgePointId:9 } })
    await wrapper.findAll('button').find(button => button.text() === '练习类似题')!.trigger('click')
    expect(push).toHaveBeenCalledWith({ path:'/student/practice/new', query:{ subjectCode:'PHYSICS', knowledgePointId:9, referenceQuestionId:102, count:5 } })
  })

  it('错题再做答对后居中询问移出，并清理一次性 URL 参数', async () => {
    routeState.query = { fromWrongBook:'true', wrongQuestionId:'103' }
    fetchPracticeResult.mockResolvedValue({ sessionId:8,subjectId:1,subjectCode:'PHYSICS',subjectName:'物理',totalCount:1,correctCount:1,totalScore:10,submittedAt:'',questions:[record(3,1,true)] })
    confirm.mockResolvedValue(undefined)
    const wrapper = mount(PracticeResultView, { global:{directives:{loading:()=>undefined},stubs:{ ElButton:{template:'<button @click="$emit(\'click\')"><slot /></button>'}, ElSwitch:true, ElEmpty:true, ElTag:{template:'<span><slot /></span>'}, QuestionContent:{props:['content'],template:'<span>{{ content }}</span>'}, StudentAiLearningPanel:true } } })
    await flushPromises()
    expect(confirm).toHaveBeenCalledWith('本次已经回答正确，是否从活跃错题本移出？','错题复习完成',expect.objectContaining({center:true,confirmButtonText:'移出错题本',cancelButtonText:'继续保留'}))
    expect(archiveWrongQuestion).toHaveBeenCalledWith(103)
    expect(routeState.query).toEqual({})
    expect(wrapper.text()).toContain('回答正确')
  })

  it('错题再做答错不弹移出确认但仍清理一次性 URL 参数', async () => {
    routeState.query = { fromWrongBook:'true', wrongQuestionId:'102' }
    fetchPracticeResult.mockResolvedValue({ sessionId:8,subjectId:1,subjectCode:'PHYSICS',subjectName:'物理',totalCount:1,correctCount:0,totalScore:0,submittedAt:'',questions:[record(2,1,false)] })
    const wrapper = mount(PracticeResultView, { global:{directives:{loading:()=>undefined},stubs:{ ElButton:{template:'<button @click="$emit(\'click\')"><slot /></button>'}, ElSwitch:true, ElEmpty:true, ElTag:{template:'<span><slot /></span>'}, QuestionContent:{props:['content'],template:'<span>{{ content }}</span>'}, StudentAiLearningPanel:true } } })
    await flushPromises()
    expect(confirm).not.toHaveBeenCalled()
    expect(archiveWrongQuestion).not.toHaveBeenCalled()
    expect(routeState.query).toEqual({})
    expect(wrapper.text()).toContain('需要复习')
  })
})
