import { beforeEach, describe, expect, it, vi } from 'vitest'
import http from '../http'
import { clearAiModelKey, createAiModel, fetchAiModels, testAiModel, updateAiModel } from './aiModels'
import { createGenerationTask, fetchGenerationMothers, fetchGenerationStats, fetchGenerationTasks, reviewAiCandidate } from './aiGeneration'

describe('管理员 AI 配置与候选题 API',()=>{
  const adapter=vi.fn().mockResolvedValue({data:{records:[]}})
  beforeEach(()=>{adapter.mockClear();http.defaults.adapter=adapter})
  it('模型配置使用遮罩友好的 CRUD 与连接测试端点',async()=>{await fetchAiModels();await createAiModel({} as never);await updateAiModel(7,{} as never);await clearAiModelKey(7);await testAiModel(7);expect(adapter.mock.calls.map(call=>`${call[0].method} ${call[0].url}`)).toEqual(['get /admin/ai-models','post /admin/ai-models','put /admin/ai-models/7','delete /admin/ai-models/7/api-key','post /admin/ai-models/7/test'])})
  it('生成、列表、统计和人工评价均使用专用端点',async()=>{await fetchGenerationMothers();await fetchGenerationTasks();await fetchGenerationStats();await createGenerationTask({} as never);await reviewAiCandidate(19,{} as never);expect(adapter.mock.calls.map(call=>`${call[0].method} ${call[0].url}`)).toEqual(['get /admin/ai-generation/mothers','get /admin/ai-generation/tasks','get /admin/ai-generation/stats','post /admin/ai-generation/tasks','post /admin/ai-generation/candidates/19/review'])})
})
