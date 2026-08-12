import http from '../http'

export type AiUsage = 'TEXT' | 'VISION'
export interface AiModelConfig {
  id:number; provider:'DEEPSEEK'|'GLM'; model:string; baseUrl:string; usage:AiUsage
  enabled:boolean; defaultConfig:boolean; timeoutMillis:number; maxTokens:number; retryCount:number
  apiKeyConfigured:boolean; lastTestStatus:'NOT_TESTED'|'SUCCESS'|'FAILED'; lastTestLatencyMillis?:number
  lastTestAt?:string; createdAt:string; updatedAt:string
}
export interface SaveAiModelConfig {
  provider:'DEEPSEEK'|'GLM'; model:string; baseUrl:string; apiKey?:string; usage:AiUsage
  enabled:boolean; defaultConfig:boolean; timeoutMillis:number; maxTokens:number; retryCount:number
}
export interface AiConnectionResult {
  success:boolean; provider:string; model:string; latencyMillis:number; status:string
  visionSummaryPreview?:string; safeError?:string
}
export const fetchAiModels=()=>http.get('/admin/ai-models').then(r=>r.data as {records:AiModelConfig[]})
export const createAiModel=(body:SaveAiModelConfig)=>http.post('/admin/ai-models',body).then(r=>r.data as AiModelConfig)
export const updateAiModel=(id:number,body:SaveAiModelConfig)=>http.put(`/admin/ai-models/${id}`,body).then(r=>r.data as AiModelConfig)
export const clearAiModelKey=(id:number)=>http.delete(`/admin/ai-models/${id}/api-key`).then(r=>r.data as AiModelConfig)
export const testAiModel=(id:number)=>http.post(`/admin/ai-models/${id}/test`).then(r=>r.data as AiConnectionResult)
