import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from '../http'
import { createPracticeSession, fetchPracticeOptions, fetchPracticeResult, fetchPracticeSession, fetchWrongQuestion, fetchWrongQuestions, submitPracticeSession } from './practice'

describe('学生练习 API', () => {
  const adapter = vi.fn().mockResolvedValue({ data: {} })
  beforeEach(() => { adapter.mockClear(); http.defaults.adapter = adapter })
  it('按可选学科读取练习配置', async () => { await fetchPracticeOptions(3); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method:'get', url:'/student/practice-options', params:{ subjectId:3 } })) })
  it('创建会话使用受控筛选字段', async () => { await createPracticeSession({ subjectId:1, knowledgePointIds:[3], questionTypes:['SINGLE_CHOICE'], difficulty:2, count:5 }); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method:'post', url:'/student/practice-sessions', data:expect.stringContaining('subjectId') })) })
  it('会话与结果使用资源 id 路径', async () => { await fetchPracticeSession(8); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ url:'/student/practice-sessions/8' })); await fetchPracticeResult(8); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ url:'/student/practice-sessions/8/result' })) })
  it('提交整场答案到专用接口', async () => { await submitPracticeSession(8, { answers:[{ practiceQuestionId:9, answer:'A' }] }); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ method:'post', url:'/student/practice-sessions/8/submit', data:expect.stringContaining('practiceQuestionId') })) })
  it('错题列表和详情使用学生作用域路径', async () => { await fetchWrongQuestions(); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ url:'/student/wrong-questions' })); await fetchWrongQuestion(5); expect(adapter).toHaveBeenCalledWith(expect.objectContaining({ url:'/student/wrong-questions/5' })) })
})
