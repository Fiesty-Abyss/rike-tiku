// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import StudentPapersView from './StudentPapersView.vue'

const api=vi.hoisted(()=>({list:vi.fn(),detail:vi.fn(),draft:vi.fn(),submit:vi.fn(),push:vi.fn()}))
const ui=vi.hoisted(()=>({success:vi.fn(),warning:vi.fn(),error:vi.fn(),confirm:vi.fn()}))
vi.mock('../../api/student/papers',()=>({fetchStudentPapers:api.list,fetchStudentPaper:api.detail,saveStudentPaperDraft:api.draft,submitStudentPaper:api.submit}))
vi.mock('vue-router',()=>({useRoute:()=>({params:{releaseId:'7'}}),useRouter:()=>({push:api.push})}))
vi.mock('element-plus',()=>({ElMessage:{success:ui.success,warning:ui.warning,error:ui.error},ElMessageBox:{confirm:ui.confirm}}))

const baseQuestion={itemId:3,order:1,score:10,type:'SINGLE_CHOICE',stem:'选择正确项',answerSlots:1,
  options:[{label:'A',content:'选项 A'},{label:'B',content:'选项 B'}],submittedAnswer:null,knowledgePoints:['力学']}
const stubs={ScientificText:{props:['content'],template:'<span>{{content}}</span>'},ElButton:{template:'<button><slot/></button>'},
  ElTag:{template:'<span><slot/></span>'},ElRadioGroup:{template:'<div><slot/></div>'},ElRadio:{template:'<label><slot/></label>'},
  ElCheckboxGroup:true,ElCheckbox:true,ElInput:true,ElEmpty:true,ElAlert:{props:['title'],template:'<span>{{title}}</span>'}}

describe('学生冻结试卷',()=>{
  beforeEach(()=>{vi.clearAllMocks();api.list.mockResolvedValue([]);api.draft.mockResolvedValue(undefined);ui.confirm.mockResolvedValue(true)})

  it('作答前隐藏 STANDARD 并显示确定性判分边界',async()=>{
    api.detail.mockResolvedValue({release:{id:7,paperName:'199班物理',subjectName:'物理',className:'199班',submissionStatus:'NOT_STARTED'},answersVisible:false,questions:[baseQuestion]})
    const wrapper=mount(StudentPapersView,{global:{stubs:{...stubs,AnswerDisplay:true}}})
    await flushPromises()
    expect(wrapper.text()).toContain('199班物理')
    expect(wrapper.text()).toContain('确定性规则判分')
    expect(wrapper.find('.standard').exists()).toBe(false)
  })

  it('提交后通过真实 AnswerDisplay 显示人类可读答案和 STANDARD',async()=>{
    api.detail.mockResolvedValue({release:{id:7,paperName:'199班物理',subjectName:'物理',className:'199班',submissionStatus:'SUBMITTED'},answersVisible:true,
      questions:[{...baseQuestion,correct:true,awardedScore:10,correctAnswer:{type:'SINGLE_CHOICE',optionLabels:['A']},standardAnalysis:'由冻结条件可知应选择 A。'}]})
    const wrapper=mount(StudentPapersView,{global:{stubs}})
    await flushPromises()
    expect(wrapper.findComponent({name:'AnswerDisplay'}).exists()).toBe(true)
    expect(wrapper.text()).toContain('A.')
    expect(wrapper.text()).toContain('选项 A')
    expect(wrapper.text()).not.toContain('"optionLabels"')
    expect(wrapper.text()).toContain('STANDARD')
    expect(wrapper.text()).toContain('由冻结条件可知应选择 A。')
    expect(wrapper.text()).toContain('本题得分 10 / 10')
  })

  it('主观大题保留输入区并明确不自动评分',async()=>{
    api.detail.mockResolvedValue({release:{id:7,paperName:'混合试卷',subjectName:'物理',className:'199班',submissionStatus:'NOT_STARTED'},answersVisible:false,
      questions:[{...baseQuestion,type:'SUBJECTIVE',stem:'请完成分步计算',options:[],stemAttachments:[],analysisAttachments:[]}]})
    const wrapper=mount(StudentPapersView,{global:{stubs:{...stubs,AnswerDisplay:true}}})
    await flushPromises()
    expect(wrapper.text()).toContain('主观大题')
    expect(wrapper.text()).toContain('系统不进行 AI 或规则自动评分')
    expect(wrapper.text()).not.toContain('SINGLE_CHOICE')
  })

  it('客观题未完成时显示具体题号、标记题卡且不发出提交请求',async()=>{
    api.detail.mockResolvedValue({release:{id:7,paperName:'混合试卷',subjectName:'物理',className:'199班',submissionStatus:'NOT_STARTED'},answersVisible:false,
      questions:[baseQuestion,{...baseQuestion,itemId:4,order:2,type:'MULTIPLE_CHOICE'}]})
    const wrapper=mount(StudentPapersView,{global:{stubs:{...stubs,AnswerDisplay:true}}})
    await flushPromises()
    await (wrapper.vm as any).submit()
    expect(api.submit).not.toHaveBeenCalled()
    expect(ui.warning).toHaveBeenCalledWith('还有 2 道客观题未完成：第1、2题。')
    expect(wrapper.findAll('.paper-question--missing')).toHaveLength(2)
  })

  it('完成客观题后提交当前完整答案并重新加载服务端结果',async()=>{
    const before={release:{id:7,paperName:'混合试卷',subjectName:'物理',className:'199班',submissionStatus:'NOT_STARTED'},answersVisible:false,questions:[baseQuestion]}
    const after={release:{...before.release,submissionStatus:'SUBMITTED'},answersVisible:true,questions:[{...baseQuestion,submittedAnswer:'A',correct:true,awardedScore:10,correctAnswer:{type:'SINGLE_CHOICE',optionLabels:['A']},standardAnalysis:'解析'}]}
    api.detail.mockResolvedValueOnce(before).mockResolvedValueOnce(after)
    api.submit.mockResolvedValue({objectiveScore:10,objectiveTotal:10,subjectivePendingCount:0})
    const wrapper=mount(StudentPapersView,{global:{stubs}})
    await flushPromises()
    ;(wrapper.vm as any).answers[3]='A'
    await (wrapper.vm as any).submit()
    expect(api.submit).toHaveBeenCalledWith(7,[{itemId:3,answer:'A'}])
    expect(api.detail).toHaveBeenCalledTimes(2)
    expect(ui.success).toHaveBeenCalledWith('提交成功：客观题自动得分 10/10')
    expect(wrapper.text()).toContain('本题得分 10 / 10')
  })

  it('提交 API 失败时显示受控业务提示而不是静默拒绝',async()=>{
    api.detail.mockResolvedValue({release:{id:7,paperName:'混合试卷',subjectName:'物理',className:'199班',submissionStatus:'NOT_STARTED'},answersVisible:false,questions:[baseQuestion]})
    api.submit.mockRejectedValue({code:'PAPER_CLOSED',message:'后端原始消息'})
    const wrapper=mount(StudentPapersView,{global:{stubs:{...stubs,AnswerDisplay:true}}})
    await flushPromises()
    ;(wrapper.vm as any).answers[3]='A'
    await (wrapper.vm as any).submit()
    expect(ui.error).toHaveBeenCalledWith('试卷已截止，不能继续提交')
  })

  it('提交前等待正在执行的草稿保存并以当前内存答案为准',async()=>{
    vi.useFakeTimers()
    let finishDraft:()=>void=()=>{}
    api.draft.mockImplementation(()=>new Promise<void>(resolve=>{finishDraft=resolve}))
    api.detail.mockResolvedValue({release:{id:7,paperName:'混合试卷',subjectName:'物理',className:'199班',submissionStatus:'IN_PROGRESS'},answersVisible:false,questions:[baseQuestion]})
    api.submit.mockResolvedValue({objectiveScore:10,objectiveTotal:10,subjectivePendingCount:0})
    const wrapper=mount(StudentPapersView,{global:{stubs:{...stubs,AnswerDisplay:true}}})
    await flushPromises()
    ;(wrapper.vm as any).answers[3]='A'
    ;(wrapper.vm as any).scheduleSave()
    await vi.advanceTimersByTimeAsync(700)
    expect(api.draft).toHaveBeenCalled()
    const submitting=(wrapper.vm as any).submit()
    await flushPromises()
    expect(api.submit).not.toHaveBeenCalled()
    finishDraft()
    await submitting
    expect(api.submit).toHaveBeenCalledWith(7,[{itemId:3,answer:'A'}])
    wrapper.unmount()
    vi.useRealTimers()
  })
})
