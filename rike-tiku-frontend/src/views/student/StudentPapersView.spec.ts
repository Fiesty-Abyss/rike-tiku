// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import StudentPapersView from './StudentPapersView.vue'

const api=vi.hoisted(()=>({list:vi.fn(),detail:vi.fn(),draft:vi.fn(),submit:vi.fn(),push:vi.fn()}))
vi.mock('../../api/student/papers',()=>({fetchStudentPapers:api.list,fetchStudentPaper:api.detail,saveStudentPaperDraft:api.draft,submitStudentPaper:api.submit}))
vi.mock('vue-router',()=>({useRoute:()=>({params:{releaseId:'7'}}),useRouter:()=>({push:api.push})}))
vi.mock('element-plus',()=>({ElMessage:{success:vi.fn()},ElMessageBox:{confirm:vi.fn().mockResolvedValue(true)}}))

const baseQuestion={itemId:3,order:1,score:10,type:'SINGLE_CHOICE',stem:'选择正确项',answerSlots:1,
  options:[{label:'A',content:'选项 A'},{label:'B',content:'选项 B'}],submittedAnswer:null,knowledgePoints:['力学']}
const stubs={ScientificText:{props:['content'],template:'<span>{{content}}</span>'},ElButton:{template:'<button><slot/></button>'},
  ElTag:{template:'<span><slot/></span>'},ElRadioGroup:{template:'<div><slot/></div>'},ElRadio:{template:'<label><slot/></label>'},
  ElCheckboxGroup:true,ElCheckbox:true,ElInput:true,ElEmpty:true}

describe('学生冻结试卷',()=>{
  beforeEach(()=>{vi.clearAllMocks();api.list.mockResolvedValue([])})

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
      questions:[{...baseQuestion,correct:true,correctAnswer:{type:'SINGLE_CHOICE',optionLabels:['A']},standardAnalysis:'由冻结条件可知应选择 A。'}]})
    const wrapper=mount(StudentPapersView,{global:{stubs}})
    await flushPromises()
    expect(wrapper.findComponent({name:'AnswerDisplay'}).exists()).toBe(true)
    expect(wrapper.text()).toContain('A.')
    expect(wrapper.text()).toContain('选项 A')
    expect(wrapper.text()).not.toContain('"optionLabels"')
    expect(wrapper.text()).toContain('STANDARD')
    expect(wrapper.text()).toContain('由冻结条件可知应选择 A。')
  })
})
