// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TeacherScopeWorkspaceView from './TeacherScopeWorkspaceView.vue'

const { fetchWorkspace, fetchLearning, fetchConversations, push } = vi.hoisted(() => ({
  fetchWorkspace: vi.fn(),
  fetchLearning: vi.fn(),
  fetchConversations: vi.fn(),
  push: vi.fn(),
}))

vi.mock('vue-router', () => ({ useRoute: () => ({ params: { scopeId: '11' } }), useRouter: () => ({ push }) }))
vi.mock('../../api/teacher', () => ({
  fetchTeacherWorkspace: fetchWorkspace,
  fetchTeacherLearningSummary: fetchLearning,
  createHighFrequencyPoint: vi.fn(),
  updateHighFrequencyPoint: vi.fn(),
  updateHighFrequencyPointStatus: vi.fn(),
}))
vi.mock('../../api/messages', () => ({ fetchConversations, createConversation: vi.fn() }))
vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn(), success: vi.fn() } }))

function mountView() {
  return mount(TeacherScopeWorkspaceView, {
    global: {
      stubs: {
        ElButton: { template: '<button @click="$emit(\'click\')"><slot /></button>' },
        ElTable: { props: ['data'], template: '<div class="table-data">{{ JSON.stringify(data) }}<slot /></div>' },
        ElTableColumn: true,
        ElTag: true,
        ElDialog: { props: ['modelValue'], template: '<div v-if="modelValue" class="dialog"><slot /><slot name="footer" /></div>' },
        ElForm: { template: '<form><slot /></form>', methods: { validate: () => Promise.resolve(true) } },
        ElFormItem: { template: '<div><slot /></div>' },
        ElSelect: { props: ['modelValue', 'placeholder'], template: '<div class="knowledge-select" :data-value="modelValue" :data-placeholder="placeholder"><slot /></div>' },
        ElOption: { props: ['label'], template: '<span>{{ label }}</span>' },
        ElInput: true,
        ElInputNumber: true,
      },
      directives: { loading: () => undefined },
    },
  })
}

describe('教师班级学习情况', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    fetchWorkspace.mockResolvedValue({ teachingAssignmentId: 11, classId: 3, className: '199班', grade: '高三', subjectId: 1, subjectCode: 'PHYSICS', subjectName: '物理', teacherName: '物理管理员教师', studentCount: 5, students: [], highFrequencyPoints: [], knowledgePoints: [{ id: 7, name: '牛顿运动定律', path: '力学>运动和力>牛顿运动定律' }] })
    fetchLearning.mockResolvedValue({ teachingAssignmentId: 11, className: '199班', subjectId: 1, subjectName: '物理', students: [{ studentId: 8, studentNumber: 'DEMO_199_01', name: '199班学生01', grade: '高三', answeredCount: 8, correctCount: 5, accuracy: 62.5, weakKnowledgePointCount: 1, masteredKnowledgePointCount: 0 }] })
    fetchConversations.mockResolvedValue([])
  })

  it('按当前 scope 加载并显示学生本科目统计且不排名', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(fetchLearning).toHaveBeenCalledWith(11)
    expect(wrapper.text()).toContain('班级学习情况')
    expect(wrapper.text()).toContain('199班学生01')
    expect(wrapper.text()).toContain('62.5')
    expect(wrapper.text()).not.toContain('第1名')
    expect(wrapper.attributes('data-subject')).toBe('physics')
  })

  it.each([
    ['PHYSICS', 'physics'],
    ['CHEMISTRY', 'chemistry'],
    ['BIOLOGY', 'biology'],
  ])('使用响应中的 subjectCode 解析 %s 教师环境', async (subjectCode, theme) => {
    fetchWorkspace.mockResolvedValue({
      teachingAssignmentId: 11, classId: 3, className: '199班', grade: '高三', subjectId: 1,
      subjectCode, subjectName: subjectCode, teacherName: '演示教师', studentCount: 0,
      students: [], highFrequencyPoints: [], knowledgePoints: [],
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.attributes('data-subject')).toBe(theme)
  })

  it('消息接口失败时不阻塞班级工作台主数据', async () => {
    fetchConversations.mockRejectedValue(new Error('消息服务暂不可用'))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('班级学习情况')
    expect(wrapper.text()).toContain('199班学生01')
  })

  it('新增高频考点时不显示 0，并使用完整知识点路径和明确占位', async () => {
    const wrapper = mountView()
    await flushPromises()
    await wrapper.findAll('button').find(button => button.text() === '新增高频考点')!.trigger('click')

    const select = wrapper.find('.knowledge-select')
    expect(select.attributes('data-value')).toBeUndefined()
    expect(select.attributes('data-placeholder')).toBe('请选择当前学科的启用知识点')
    expect(wrapper.text()).toContain('力学>运动和力>牛顿运动定律')
  })
})
