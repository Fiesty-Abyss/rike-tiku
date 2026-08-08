import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from './http'
import { createHighFrequencyPoint, fetchTeacherLearningSummary, fetchTeacherWorkspace, fetchTeachingScopes, updateHighFrequencyPoint, updateHighFrequencyPointStatus } from './teacher'

describe('教师班级学科工作台 API', () => {
  const adapter = vi.fn().mockResolvedValue({ data: {} })

  beforeEach(() => {
    adapter.mockClear()
    http.defaults.adapter = adapter
  })

  it('任教范围和工作台使用真实任课关系 id', async () => {
    await fetchTeachingScopes()
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'get', url: '/teacher/teaching-scopes' }))
    await fetchTeacherWorkspace(12)
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ url: '/teacher/scopes/12' }))
    await fetchTeacherLearningSummary(12)
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ url: '/teacher/scopes/12/learning-summary' }))
  })

  it('高频考点维护接口只提交业务字段', async () => {
    await createHighFrequencyPoint(12, { knowledgePointId: 3, title: '标题', content: '正文', sortOrder: 1 })
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'post', url: '/teacher/scopes/12/high-frequency-points', data: expect.stringContaining('knowledgePointId') }))
    await updateHighFrequencyPoint(7, { title: '新标题', content: '新正文', sortOrder: 2 })
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'put', url: '/teacher/high-frequency-points/7' }))
    await updateHighFrequencyPointStatus(7, 'DISABLED')
    expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'post', url: '/teacher/high-frequency-points/7/status', data: JSON.stringify({ status: 'DISABLED' }) }))
  })
})
