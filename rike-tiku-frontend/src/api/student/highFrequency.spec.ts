import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from '../http'
import { fetchStudentHighFrequencyPoints } from './highFrequency'

describe('学生高频考点 API', () => {
  const adapter = vi.fn().mockResolvedValue({ data: [] })

  beforeEach(() => {
    adapter.mockClear()
    http.defaults.adapter = adapter
  })

  it('只提交当前学科 id，由服务端推导学生班级', async () => {
    await fetchStudentHighFrequencyPoints(1)
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'get', url: '/student/high-frequency-points', params: { subjectId: 1 } }))
  })
})
