// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import PracticeSessionView from './PracticeSessionView.vue'

const { fetchPracticeSession, submitPracticeSession, replace } = vi.hoisted(() => ({ fetchPracticeSession: vi.fn(), submitPracticeSession: vi.fn(), replace: vi.fn() }))
vi.mock('vue-router', () => ({ useRoute: () => ({ params: { id: '7' } }), useRouter: () => ({ push: vi.fn(), replace }) }))
vi.mock('../../api/student/practice', () => ({ fetchPracticeSession, submitPracticeSession }))
vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn(), warning: vi.fn() }, ElMessageBox: { confirm: vi.fn() } }))

const question = (id:number, order:number, type:string, difficulty:number) => ({ practiceQuestionId:id, questionId:100+id, order, questionType:type, stem:`题干${order}`, difficulty, score:10, blankCount:type==='FILL_BLANK'?1:0, options:[{label:'A',content:'选项A'},{label:'B',content:'选项B'}], knowledgePoints:[{id:1,name:'知识点',path:'模块>知识点'}], attachments:[] })

describe('练习题型与作答规则', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    fetchPracticeSession.mockResolvedValue({ id:7, subjectId:1, subjectCode:'PHYSICS', subjectName:'物理', status:'CREATED', questionCount:3, createdAt:'', questions:[question(1,1,'SINGLE_CHOICE',1),question(2,2,'MULTIPLE_CHOICE',2),question(3,3,'FILL_BLANK',3)] })
  })

  it('以中文显示单选、多选、填空说明和难度', async () => {
    const wrapper = mount(PracticeSessionView, { global: { directives:{loading:()=>undefined}, stubs:{
      ElButton:{props:['disabled'],template:'<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>'},
      ElProgress:true, ElTag:{template:'<span><slot /></span>'},
      ElRadioGroup:{template:'<div><slot /></div>'}, ElRadio:{template:'<label><slot /></label>'},
      ElCheckboxGroup:{template:'<div><slot /></div>'}, ElCheckbox:{template:'<label><slot /></label>'},
      ElInput:true, QuestionContent:{props:['content'],template:'<span>{{ content }}</span>'},
    } } })
    await flushPromises()
    expect(wrapper.text()).toContain('单选题')
    expect(wrapper.text()).toContain('请选择 1 项')
    expect(wrapper.text()).toContain('简单 · 10 分')

    await wrapper.findAll('button').find(button => button.text() === '2')!.trigger('click')
    expect(wrapper.text()).toContain('多选题')
    expect(wrapper.text()).toContain('全部选对得分，错选或漏选不得分')
    expect(wrapper.text()).toContain('中等 · 10 分')

    await wrapper.findAll('button').find(button => button.text() === '3')!.trigger('click')
    expect(wrapper.text()).toContain('填空题')
    expect(wrapper.text()).toContain('请按顺序填写每个空')
    expect(wrapper.text()).toContain('困难 · 10 分')
  })
})
