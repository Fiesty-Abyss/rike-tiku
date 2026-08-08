import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from './http'
import { createConversation, fetchConversations, fetchMessageContacts, fetchMessages, markConversationRead, sendMessage } from './messages'

vi.mock('./http', () => ({ default: { get: vi.fn(), post: vi.fn() } }))
const get = vi.mocked(http.get)
const post = vi.mocked(http.post)

describe('message api', () => {
  beforeEach(() => vi.clearAllMocks())

  it('loads contacts, conversations and messages', async () => {
    get.mockResolvedValueOnce({ data: [{ name: '物理教师' }] } as never)
      .mockResolvedValueOnce({ data: [{ id: 4 }] } as never)
      .mockResolvedValueOnce({ data: { conversation: { id: 4 }, messages: [] } } as never)
    expect(await fetchMessageContacts()).toEqual([{ name: '物理教师' }])
    expect(await fetchConversations()).toEqual([{ id: 4 }])
    expect((await fetchMessages(4)).conversation.id).toBe(4)
  })

  it('creates, sends and marks read without accepting sender identity', async () => {
    post.mockResolvedValueOnce({ data: { id: 8 } } as never)
      .mockResolvedValueOnce({ data: { id: 9, content: '请再讲一下' } } as never)
      .mockResolvedValueOnce({ data: { readCount: 1 } } as never)
    await createConversation(2, 7)
    await sendMessage(8, '请再讲一下')
    await markConversationRead(8)
    expect(post).toHaveBeenNthCalledWith(1, '/messages/conversations', { teachingAssignmentId: 2, studentId: 7 })
    expect(post).toHaveBeenNthCalledWith(2, '/messages/conversations/8/messages', { content: '请再讲一下' })
    expect(post).toHaveBeenNthCalledWith(3, '/messages/conversations/8/read')
  })
})
