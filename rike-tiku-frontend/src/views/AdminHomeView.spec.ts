// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminHomeView from './AdminHomeView.vue'

const { fetchAdminDashboard, push } = vi.hoisted(() => ({ fetchAdminDashboard: vi.fn(), push: vi.fn() }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))
vi.mock('../api/admin/dashboard', () => ({ fetchAdminDashboard }))
vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn() } }))

describe('管理员系统总览', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    fetchAdminDashboard.mockResolvedValue({
      activeClassCount: 3, enabledStudentCount: 9, enabledTeacherCount: 4,
      publishedQuestionCount: 378, pendingQuestionCount: 2,
      physicsQuestionCount: 126, chemistryQuestionCount: 126, biologyQuestionCount: 126,
      recentOperationLogs: [{ id: 1, operatorUsername: 'demo_admin', module: 'TEACHER', action: 'RESET_PASSWORD', result: 'SUCCESS', summary: '重置教师密码', createdAt: '2026-08-10T12:00:00' }],
    })
  })

  it('只展示后端返回的真实指标、待办和最近操作', async () => {
    const wrapper = mount(AdminHomeView, { global: { directives: { loading: () => undefined }, stubs: {
      ElButton: { template: '<button @click="$emit(\'click\')"><slot /></button>' },
      ElProgress: { props: ['percentage'], template: '<span>{{ percentage }}%</span>' },
      ElEmpty: true,
      ElTag: { template: '<span><slot /></span>' },
    } } })
    await flushPromises()

    expect(wrapper.text()).toContain('有效班级3')
    expect(wrapper.text()).toContain('已发布题目378')
    expect(wrapper.text()).toContain('2道题等待审核')
    expect(wrapper.text()).toContain('demo_admin · TEACHER')
    expect(fetchAdminDashboard).toHaveBeenCalledTimes(1)
  })
})
