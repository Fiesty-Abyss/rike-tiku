import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from '../http'
import { fetchTopic, fetchTopics } from './topicLearning'

describe('专题学习 API', () => {
  const adapter = vi.fn().mockResolvedValue({ data: [] })
  beforeEach(() => { adapter.mockClear(); http.defaults.adapter = adapter })

  it('按稳定学科编码读取 Topic18 列表', async () => {
    await fetchTopics('CHEMISTRY')
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method:'get', url:'/student/topic-learning', params:{ subjectCode:'CHEMISTRY' } }))
  })

  it('专题详情使用题目资源 ID 且不提交答案', async () => {
    await fetchTopic(18)
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method:'get', url:'/student/topic-learning/18' }))
  })
})
