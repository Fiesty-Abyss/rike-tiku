// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MessageConversationView from './MessageConversationView.vue'

const { fetchMessages, markConversationRead, sendMessage, stopPolling, push } = vi.hoisted(() => ({
  fetchMessages: vi.fn(),
  markConversationRead: vi.fn(),
  sendMessage: vi.fn(),
  stopPolling: vi.fn(),
  push: vi.fn(),
}))

vi.mock('vue-router', () => ({ useRoute: () => ({ params: { id: '7' } }), useRouter: () => ({ push }) }))
vi.mock('../../stores/auth', () => ({ useAuthStore: () => ({ activeRole: 'STUDENT' }) }))
vi.mock('../../api/messages', () => ({ fetchMessages, markConversationRead, sendMessage }))
vi.mock('./messagePolling', () => ({ startMessagePolling: vi.fn(() => stopPolling) }))
vi.mock('element-plus', () => ({ ElMessage: { error: vi.fn(), warning: vi.fn() } }))

const page = {
  conversation: { id: 7, peerName: '物理管理员教师', className: '199班', subjectName: '物理', canSend: true },
  messages: [{ id: 1, senderName: '学生', content: '老师您好', mine: true, sentAt: '2026-08-08T10:00:00' }],
}

function mountView() {
  return mount(MessageConversationView, {
    global: {
      stubs: {
        ElButton: { props: ['disabled'], template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>' },
        ElInput: { props: ['modelValue', 'disabled'], emits: ['update:modelValue'], template: '<textarea :disabled="disabled" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />' },
        ElEmpty: true,
      },
      directives: { loading: () => undefined },
    },
  })
}

describe('私信对话页', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    fetchMessages.mockResolvedValue(page)
    markConversationRead.mockResolvedValue({ readCount: 1 })
    sendMessage.mockResolvedValue({ id: 2 })
  })

  it('loads messages and marks peer messages read', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('物理 · 物理管理员教师')
    expect(wrapper.text()).toContain('老师您好')
    expect(markConversationRead).toHaveBeenCalledWith(7)
  })

  it('sends trimmed content and refreshes immediately', async () => {
    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('textarea').setValue('  我再确认一下  ')
    const sendButton = wrapper.findAll('button').find((button) => button.text() === '发送消息')
    await sendButton?.trigger('click')
    await flushPromises()
    expect(sendMessage).toHaveBeenCalledWith(7, '我再确认一下')
    expect(fetchMessages.mock.calls.length).toBeGreaterThanOrEqual(2)
  })

  it('stops polling after leaving the page', async () => {
    const wrapper = mountView()
    await flushPromises()
    wrapper.unmount()
    expect(stopPolling).toHaveBeenCalledOnce()
  })
})
