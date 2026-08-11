// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SubjectDashboardView from './SubjectDashboardView.vue'

const { push, fetchPracticeOptions, createPracticeSession, fetchHighFrequency, fetchMastery } = vi.hoisted(() => ({
  push: vi.fn(),
  fetchPracticeOptions: vi.fn(),
  createPracticeSession: vi.fn(),
  fetchHighFrequency: vi.fn(),
  fetchMastery: vi.fn(),
}))

vi.mock('vue-router', () => ({ useRoute: () => ({ params: { subjectCode: 'PHYSICS' } }), useRouter: () => ({ push, replace: vi.fn() }) }))
vi.mock('../../api/student/practice', () => ({ fetchPracticeOptions, createPracticeSession }))
vi.mock('../../api/student/highFrequency', () => ({ fetchStudentHighFrequencyPoints: fetchHighFrequency }))
vi.mock('../../api/student/learningMastery', () => ({ fetchStudentLearningSummary: fetchMastery }))
vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn(), success: vi.fn() } }))

const summary = {
  subject: { id: 1, code: 'PHYSICS', name: '物理' },
  overall: { practicedKnowledgePointCount: 0, totalKnowledgePointCount: 38, totalAnsweredCount: 8, totalCorrectCount: 5, overallAccuracy: 62.5, weakKnowledgePointCount: 1, improvingKnowledgePointCount: 1, masteredKnowledgePointCount: 0, insufficientKnowledgePointCount: 0, notStartedKnowledgePointCount: 38 },
  knowledgePoints: [{ knowledgePointId: 7, knowledgePointName: '牛顿运动定律', fullPath: '力学>运动和力>牛顿运动定律', answeredCount: 4, correctCount: 2, wrongCount: 2, accuracy: 50, activeWrongQuestionCount: 1, masteryLevel: 'WEAK' }],
  recommendations: [{ knowledgePointId: 7, knowledgePointName: '牛顿运动定律', reason: '该知识点仍有未完成复习的错题。', practiceParameters: { subjectId: 1, knowledgePointId: 7, count: 5 } }],
  recommendationMessage: null,
}

function mountView() {
  return mount(SubjectDashboardView, {
    global: {
      stubs: {
        ElButton: { template: '<button @click="$emit(\'click\')"><slot /></button>' },
        ElProgress: { props: ['percentage'], template: '<span>{{ percentage }}%</span>' },
        ElTag: { template: '<span><slot /></span>' },
        ElAlert: { props: ['title'], template: '<p>{{ title }}</p>' },
        ElTable: { props: ['data'], template: '<div class="table-data">{{ JSON.stringify(data) }}</div>' },
        ElTableColumn: true,
        ElEmpty: true,
      },
    },
  })
}

describe('学生学科页掌握度与推荐', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    fetchPracticeOptions.mockImplementation((subjectId?: number) => Promise.resolve(subjectId ? { subjects: [], knowledgePoints: [{ id: 7 }] } : { subjects: [{ id: 1, code: 'PHYSICS', name: '物理' }], knowledgePoints: [] }))
    fetchHighFrequency.mockResolvedValue([])
    fetchMastery.mockResolvedValue(summary)
  })

  it('显示真实正确率、中文等级和规则原因', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('总体正确率')
    expect(wrapper.text()).toContain('62.5%')
    expect(wrapper.text()).toContain('薄弱')
    expect(wrapper.text()).toContain('该知识点仍有未完成复习的错题。')
    expect(wrapper.find('.table-data').text()).toContain('WEAK')
    expect(wrapper.attributes('data-subject')).toBe('physics')
    expect(wrapper.find('.mastery-inline-ratio').text()).toBe('0 / 38')
    expect(wrapper.find('.mastery-inline-ratio').attributes('aria-label')).toBe('已练习知识点 0 / 38')
    expect(wrapper.find('.metric-fraction').exists()).toBe(false)
  })

  it('开始巩固复用条件练习并预选学科、知识点和五题', async () => {
    const wrapper = mountView()
    await flushPromises()
    const button = wrapper.findAll('button').find(item => item.text().includes('开始巩固'))
    await button!.trigger('click')
    expect(push).toHaveBeenCalledWith({ path: '/student/practice/new', query: { subjectId: 1, knowledgePointId: 7, count: 5 } })
  })

  it('无作答时显示暂无练习数据和入门推荐', async () => {
    fetchMastery.mockResolvedValue({ ...summary, overall: { ...summary.overall, totalAnsweredCount: 0, totalCorrectCount: 0, overallAccuracy: null }, recommendations: [{ ...summary.recommendations[0], reason: '该知识点尚未开始练习。' }] })
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('暂无练习数据')
    expect(wrapper.text()).toContain('该知识点尚未开始练习。')
  })
})
