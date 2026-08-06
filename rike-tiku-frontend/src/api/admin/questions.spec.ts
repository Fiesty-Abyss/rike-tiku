import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from '../http'
import { createQuestion, fetchKnowledgePoints, fetchQuestion, fetchQuestions, questionAction, updateQuestion } from './questions'

describe('管理员题库 API', () => {
  const adapter = vi.fn().mockResolvedValue({ data: { records: [], total: 0 } })
  beforeEach(() => { adapter.mockClear(); http.defaults.adapter = adapter })
  it('列表使用 GET 并携带全部筛选与分页参数', async () => { await fetchQuestions({ page: 2, size: 20, subjectCode: 'PHYSICS', questionType: 'SINGLE_CHOICE', usageMode: 'ONLINE_PRACTICE', difficulty: 2, status: 'PENDING', keyword: '波', rightsStatus: 'AUTHORIZED' }); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'get', url: '/admin/questions', params: expect.objectContaining({ rightsStatus: 'AUTHORIZED', keyword: '波', page: 2 }) })) })
  it('详情使用带 id 的 GET 路径', async () => { await fetchQuestion(7); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'get', url: '/admin/questions/7' })) })
  it('创建使用 POST', async () => { await createQuestion({} as never); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'post', url: '/admin/questions' })) })
  it('草稿编辑使用 PUT', async () => { await updateQuestion(7, {} as never); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'put', url: '/admin/questions/7' })) })
  it('状态动作使用专用 POST 路径', async () => { await questionAction(7, 'submit-review'); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'post', url: '/admin/questions/7/submit-review' })) })
  it('退回提交审核意见而非敏感前端状态', async () => { await questionAction(7, 'return', '请补齐授权'); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'post', url: '/admin/questions/7/return', data: JSON.stringify({ opinion: '请补齐授权' }) })) })
  it('知识点按真实学科 id 获取', async () => { await fetchKnowledgePoints(3); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method: 'get', url: '/admin/knowledge-points', params: { subjectId: 3 } })) })
})
