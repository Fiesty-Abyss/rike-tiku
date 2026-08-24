// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TeacherPaperBuilderView from './TeacherPaperBuilderView.vue'

const api = vi.hoisted(() => ({
  fetchTeachingScopes: vi.fn(), createPaper: vi.fn(), createRandomPaper: vi.fn(), createRulePaper: vi.fn(),
  fetchPaperKnowledgePoints: vi.fn(), fetchPaperQuality: vi.fn(), fetchPaperQuestions: vi.fn(), fetchPapers: vi.fn(),
  publishPaper: vi.fn(), requestAiPaperQuality: vi.fn(), fetchPaperReleases: vi.fn(), fetchPaperStats: vi.fn(),
  fetchPaperSubmissions: vi.fn(), fetchTeacherSubmission: vi.fn(), cancelPaperRelease: vi.fn(), deletePaper: vi.fn(),
  fetchTeacherPaperReleases: vi.fn(), push: vi.fn(),
}))
const ui = vi.hoisted(() => ({ success: vi.fn(), warning: vi.fn(), error: vi.fn(), confirm: vi.fn() }))
vi.mock('../../api/teacher', () => ({ fetchTeachingScopes: api.fetchTeachingScopes }))
vi.mock('../../api/teacher/papers', () => api)
vi.mock('vue-router', () => ({ useRouter: () => ({ push: api.push }) }))
vi.mock('element-plus', () => ({ ElMessage: { success: ui.success, warning: ui.warning, error: ui.error }, ElMessageBox: { confirm: ui.confirm } }))

const stubs = {
  ScientificText: { props: ['content'], template: '<span>{{content}}</span>' },
  QuestionContent: { props: ['content'], template: '<span>{{content}}</span>' }, ElSegmented: true,
  ElForm: { template: '<form><slot/></form>' }, ElFormItem: { template: '<label><slot/></label>' },
  ElSelect: { template: '<div><slot/></div>' }, ElOption: { props: ['label'], template: '<span>{{label}}</span>' }, ElInput: true, ElInputNumber: true,
  ElButton: { template: '<button @click="$emit(\'click\')"><slot/></button>' }, ElTag: { template: '<span><slot/></span>' },
  ElEmpty: true, ElTable: { template: '<div><slot/></div>' }, ElTableColumn: { template: '<div><slot :row="{id:1,subjectId:1}"/></div>' },
  ElCheckboxGroup: { template: '<div><slot/></div>' }, ElCheckbox: true, ElDialog: { template: '<div><slot/><slot name="footer"/></div>' }, ElAlert: { props: ['title'], template: '<span>{{title}}</span>' }, ElDatePicker: true,
  ElDropdown: { template: '<div><slot/><slot name="dropdown"/></div>' }, ElDropdownMenu: { template: '<div><slot/></div>' }, ElDropdownItem: { template: '<button><slot/></button>' }, ElPagination: true,
}

describe('教师组卷', () => {
  beforeEach(() => {
    vi.useRealTimers(); vi.clearAllMocks(); ui.confirm.mockResolvedValue(true)
    api.fetchTeachingScopes.mockResolvedValue([{ teachingAssignmentId: 9, subjectId: 1, subjectName: '物理', className: '199班', teachingStatus: 'ACTIVE' }, { teachingAssignmentId: 10, subjectId: 1, subjectName: '物理', className: '200班', teachingStatus: 'ACTIVE' }])
    api.fetchPapers.mockResolvedValue([]); api.fetchPaperQuestions.mockResolvedValue([]); api.fetchPaperKnowledgePoints.mockResolvedValue([])
    api.fetchPaperQuality.mockResolvedValue({ notice: '辅助建议，不代替教师审核', coverage: [], risks: [], suggestions: [] })
    api.fetchTeacherPaperReleases.mockResolvedValue({ items: [], total: 0 }); api.fetchPaperReleases.mockResolvedValue([])
    api.fetchPaperStats.mockResolvedValue({ assigned: 3, submitted: 1, unsubmitted: 2, averageScore: 20, weakPoints: [], questions: [], knowledgePoints: [] })
    api.fetchPaperSubmissions.mockResolvedValue([{ studentId: 1, studentNumber: '20260001', studentName: '学生', status: 'SUBMITTED', objectiveScore: 20, objectiveTotal: 30, subjectivePendingCount: 1 }])
  })
  it('提供主观大题手动检索，并将随机规则边界说明给教师', async () => {
    const wrapper = mount(TeacherPaperBuilderView, { global: { stubs } })
    await flushPromises()
    expect(wrapper.text()).toContain('主观大题')
    expect(wrapper.text()).toContain('题篮')
    ;(wrapper.vm as any).mode = 'RANDOM'
    await flushPromises()
    expect(wrapper.text()).toContain('随机与规则组卷默认只抽取可确定性判分的客观题')
    expect(wrapper.text()).toContain('学生版')
    expect(wrapper.text()).toContain('答案解析版')
  })

  it('以唯一任课关系区分同学科班级，并提供集中发布记录入口', async () => {
    const wrapper = mount(TeacherPaperBuilderView, { global: { stubs } })
    await flushPromises()
    expect(wrapper.text()).toContain('物理（199班）')
    expect(wrapper.text()).toContain('物理（200班）')
    expect(wrapper.text()).toContain('班级发布记录')
    expect(wrapper.text()).toContain('删除试卷')
    await (wrapper.vm as any).openReleaseHistory()
    expect(api.fetchTeacherPaperReleases).toHaveBeenCalledWith(expect.objectContaining({ page: 1, size: 20 }))
  })

  it('把发布管理与危险删除归入唯一的更多操作菜单', async () => {
    api.fetchPapers.mockResolvedValue([{ id: 1, subjectId: 1, name: '物理周测', questionCount: 4, totalScore: 50, status: 'DRAFT', mode: 'MANUAL' }])
    const wrapper = mount(TeacherPaperBuilderView, { global: { stubs } })
    await flushPromises()
    expect(wrapper.text()).toContain('更多操作 ▾')
    expect(wrapper.text()).toContain('发布管理')
    expect(wrapper.text()).toContain('删除试卷')
    expect(wrapper.findAll('button').filter(button => button.text() === '发布管理')).toHaveLength(1)
    await (wrapper.vm as any).handlePaperCommand('releases', { id: 1 })
    expect(api.fetchPaperReleases).toHaveBeenCalledWith(1)
  })

  it('作答情况同时刷新统计和学生列表，并在关闭后停止轮询', async () => {
    vi.useFakeTimers()
    const wrapper = mount(TeacherPaperBuilderView, { global: { stubs } })
    await flushPromises()
    await (wrapper.vm as any).openStats({ id: 7 })
    expect(api.fetchPaperStats).toHaveBeenCalledWith(7)
    expect(api.fetchPaperSubmissions).toHaveBeenCalledWith(7)
    api.fetchPaperStats.mockClear(); api.fetchPaperSubmissions.mockClear()
    await vi.advanceTimersByTimeAsync(5000)
    expect(api.fetchPaperStats).toHaveBeenCalledTimes(1)
    expect(api.fetchPaperSubmissions).toHaveBeenCalledTimes(1)
    ;(wrapper.vm as any).statsVisible=false
    await flushPromises()
    api.fetchPaperStats.mockClear(); api.fetchPaperSubmissions.mockClear()
    await vi.advanceTimersByTimeAsync(5000)
    expect(api.fetchPaperStats).not.toHaveBeenCalled()
    expect(api.fetchPaperSubmissions).not.toHaveBeenCalled()
    wrapper.unmount()
  })
})
