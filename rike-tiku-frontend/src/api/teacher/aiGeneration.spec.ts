import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from '../http'
import { createTeacherGenerationTask, fetchTeacherGenerationKnowledgePoints, fetchTeacherGenerationMothers, fetchTeacherGenerationTask, fetchTeacherGenerationTasks, reviewTeacherAiCandidate, teacherAiGenerationClient } from './aiGeneration'

describe('教师 AI 候选题 API',()=>{
  const adapter=vi.fn().mockResolvedValue({data:[]})
  beforeEach(()=>{adapter.mockClear();http.defaults.adapter=adapter})
  it('只使用教师授权端点且不请求全局 stats 或管理员配置',async()=>{await fetchTeacherGenerationMothers();await fetchTeacherGenerationKnowledgePoints(2);await fetchTeacherGenerationTasks();await fetchTeacherGenerationTask(7);await createTeacherGenerationTask({} as never);await reviewTeacherAiCandidate(19,{} as never);expect(adapter.mock.calls.map(call=>`${call[0].method} ${call[0].url}`)).toEqual(['get /teacher/ai-generation/mothers','get /teacher/ai-generation/knowledge-points','get /teacher/ai-generation/tasks','get /teacher/ai-generation/tasks/7','post /teacher/ai-generation/tasks','post /teacher/ai-generation/candidates/19/review']);expect(adapter.mock.calls[1][0].params).toEqual({subjectId:2});expect(teacherAiGenerationClient.fetchStats).toBeUndefined();expect(adapter.mock.calls.flatMap(call=>[call[0].url])).not.toContain('/teacher/ai-generation/stats','/admin/ai-models')})
})
