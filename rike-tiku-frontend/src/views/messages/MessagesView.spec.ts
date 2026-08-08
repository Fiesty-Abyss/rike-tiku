// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MessagesView from './MessagesView.vue'

const { push, createConversation, fetchConversations, fetchMessageContacts, showError } = vi.hoisted(() => ({
  push: vi.fn(),
  createConversation: vi.fn(),
  fetchConversations: vi.fn(),
  fetchMessageContacts: vi.fn(),
  showError: vi.fn(),
}))

vi.mock('vue-router', () => ({ useRouter: () => ({ push }) }))
vi.mock('../../stores/auth', () => ({ useAuthStore: () => ({ activeRole: 'STUDENT' }) }))
vi.mock('../../api/messages', () => ({ createConversation, fetchConversations, fetchMessageContacts }))
vi.mock('element-plus', () => ({ ElMessage: { error: showError } }))

function mountView() {
  return mount(MessagesView, {
    global: {
      stubs: {
        ElButton: { template: '<button @click="$emit(\'click\')"><slot /></button>' },
        ElBadge: { props: ['value'], template: '<i>{{ value }}</i>' },
        ElEmpty: { props: ['description'], template: '<p>{{ description }}</p>' },
      },
      directives: { loading: () => undefined },
    },
  })
}

describe('师生消息列表', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    fetchConversations.mockResolvedValue([{ id: 4, peerName: '物理管理员教师', className: '199班', subjectName: '物理', latestMessage: '请再看一下', latestMessageTime: '2026-08-08T10:00:00', unreadCount: 2 }])
    fetchMessageContacts.mockResolvedValue([{ teachingAssignmentId: 11, studentId: null, name: '物理管理员教师', className: '199班', subjectName: '物理' }])
  })

  it('renders conversation, unread count and current-class teacher contact', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('物理管理员教师')
    expect(wrapper.text()).toContain('199班 · 物理')
    expect(wrapper.text()).toContain('请再看一下')
    expect(wrapper.text()).toContain('2')
  })

  it('creates a conversation from the contact and opens it', async () => {
    createConversation.mockResolvedValue({ id: 9 })
    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('.contact-row').trigger('click')
    await flushPromises()
    expect(createConversation).toHaveBeenCalledWith(11, undefined)
    expect(push).toHaveBeenCalledWith('/messages/9')
  })

  it('shows a Chinese empty state', async () => {
    fetchConversations.mockResolvedValue([])
    fetchMessageContacts.mockResolvedValue([])
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('当前暂无私信会话。')
    expect(wrapper.text()).toContain('当前没有可联系的教学关系。')
  })
})
