import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from '../http'
import { fetchStudentLearningSummary } from './learningMastery'

describe('学生知识点掌握度 API', () => {
  const adapter = vi.fn().mockResolvedValue({ data: {} })

  beforeEach(() => {
    adapter.mockClear()
    http.defaults.adapter = adapter
  })

  it('只提交学科 id，不接受 studentId', async () => {
    await fetchStudentLearningSummary(2)
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({
      method: 'get',
      url: '/student/learning-summary',
      params: { subjectId: 2 },
    }))
  })
})
