import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from '../http'
import { createAiConversation, fetchAiAnalysis, fetchAiConversation, generateAiAnalysis, sendAiMessage } from './aiLearning'

describe('学生 AI 学习 API', () => {
  const adapter = vi.fn().mockResolvedValue({ data: {} })
  beforeEach(() => { adapter.mockClear(); http.defaults.adapter = adapter })
  it('分析只使用正式答题事实资源路径', async () => {
    await fetchAiAnalysis(19)
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method:'get', url:'/student/ai/analyses/19' }))
    await generateAiAnalysis(19)
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method:'post', url:'/student/ai/analyses/19' }))
  })
  it('会话不接受 studentId 或可替换 questionId', async () => {
    await createAiConversation(19)
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ url:'/student/ai/conversations', data:JSON.stringify({ answerFactId:19 }) }))
    await fetchAiConversation(7)
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method:'get', url:'/student/ai/conversations/7' }))
    await sendAiMessage(7, '为什么？')
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method:'post', url:'/student/ai/conversations/7/messages', data:JSON.stringify({ content:'为什么？' }) }))
  })
})
