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
        ElButton: { template: '<button><slot /></button>' },
        ElTable: { props: ['data'], template: '<div class="table-data">{{ JSON.stringify(data) }}<slot /></div>' },
        ElTableColumn: true,
        ElTag: true,
        ElDialog: true,
      },
      directives: { loading: () => undefined },
    },
  })
}

describe('教师班级学习情况', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    fetchWorkspace.mockResolvedValue({ teachingAssignmentId: 11, classId: 3, className: '199班', grade: '高三', subjectId: 1, subjectName: '物理', teacherName: '物理管理员教师', studentCount: 5, students: [], highFrequencyPoints: [], knowledgePoints: [] })
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
  })
})
