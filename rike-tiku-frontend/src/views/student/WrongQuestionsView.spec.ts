// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import WrongQuestionsView from './WrongQuestionsView.vue'

const { fetchWrongQuestions } = vi.hoisted(() => ({ fetchWrongQuestions: vi.fn() }))
vi.mock('vue-router', () => ({ useRoute: () => ({ query: { subjectCode:'BIOLOGY' } }), useRouter: () => ({ push:vi.fn() }) }))
vi.mock('../../api/student/practice', () => ({ fetchWrongQuestions, fetchWrongQuestion:vi.fn() }))
vi.mock('element-plus', () => ({ ElMessage:{error:vi.fn()} }))

describe('错题本实时学科筛选', () => {
  beforeEach(() => { vi.clearAllMocks(); fetchWrongQuestions.mockResolvedValue([{questionId:3,subjectCode:'BIOLOGY',subjectName:'生物',questionType:'FILL_BLANK',stemSummary:'细胞结构',errorCount:1,consecutiveCorrectCount:0,status:'NEW',lastWrongAt:''}]) })

  it('进入生物错题时按 subjectCode 立即读取，不依赖固定数据库 ID', async () => {
    const wrapper = mount(WrongQuestionsView,{global:{directives:{loading:()=>undefined},stubs:{
      ElButton:{template:'<button><slot /></button>'},ElTable:{props:['data'],template:'<div>{{ JSON.stringify(data) }}</div>'},ElTableColumn:true,ElTag:{template:'<span><slot /></span>'},ElDrawer:true,QuestionContent:true,
    }}})
    await flushPromises()
    expect(fetchWrongQuestions).toHaveBeenCalledTimes(1)
    expect(fetchWrongQuestions).toHaveBeenCalledWith('BIOLOGY')
    expect(wrapper.text()).toContain('细胞结构')
    expect(wrapper.text()).not.toContain('PHYSICS')
  })
})
