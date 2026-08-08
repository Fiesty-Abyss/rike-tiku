import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import PracticeNewView from './PracticeNewView.vue'

const { push, createPracticeSession, fetchPracticeOptions } = vi.hoisted(() => ({
  push: vi.fn(),
  createPracticeSession: vi.fn(),
  fetchPracticeOptions: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: { subjectId: '1', knowledgePointId: '16', count: '5' } }),
  useRouter: () => ({ push }),
}))
vi.mock('../../api/student/practice', () => ({ createPracticeSession, fetchPracticeOptions }))
vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn(), success: vi.fn() } }))

describe('PracticeNewView 推荐练习预选', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    fetchPracticeOptions.mockImplementation(() => Promise.resolve({
      subjects: [{ id: 1, code: 'PHYSICS', name: '物理' }],
      knowledgePoints: [{ id: 16, name: '牛顿运动定律', path: '力学>运动和力>牛顿运动定律' }],
      questionTypes: [],
      difficulties: [],
    }))
    createPracticeSession.mockResolvedValue({ id: 99 })
  })

  it('并发加载后仍把学科、知识点和5题传入现有创建接口', async () => {
    const wrapper = mount(PracticeNewView, {
      global: {
        stubs: {
          ElForm: { template: '<form><slot /></form>', methods: { validate: () => Promise.resolve(true) } },
          ElFormItem: { template: '<div><slot /></div>' },
          ElSelect: { template: '<div><slot /></div>' },
          ElOption: true,
          ElCheckboxGroup: { template: '<div><slot /></div>' },
          ElCheckbox: { template: '<label><slot /></label>' },
          ElRadioGroup: { template: '<div><slot /></div>' },
          ElRadio: { template: '<label><slot /></label>' },
          ElInputNumber: true,
          ElButton: { template: '<button type="button" @click="$emit(\'click\')"><slot /></button>' },
        },
      },
    })
    await flushPromises()
    await wrapper.findAll('button').at(-1)?.trigger('click')
    await flushPromises()

    expect(createPracticeSession).toHaveBeenCalledWith({
      subjectId: 1,
      knowledgePointIds: [16],
      questionTypes: undefined,
      difficulty: undefined,
      count: 5,
    })
    expect(push).toHaveBeenCalledWith('/student/practice/99')
  })
})
