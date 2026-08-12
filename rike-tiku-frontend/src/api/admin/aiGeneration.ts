import http from '../http'
import { fetchKnowledgePoints } from './questions'
import type { AiCandidate, AiGenerationClient, AiGenerationStats, AiGenerationTask, GenerateRequest, MotherOption, ReviewRequest } from '../aiGeneration'

export type { AiCandidate, AiGenerationStats, AiGenerationTask, GenerateRequest, MotherOption, ReviewRequest } from '../aiGeneration'
export const fetchGenerationMothers=()=>http.get('/admin/ai-generation/mothers').then(r=>r.data as MotherOption[])
export const fetchGenerationTasks=()=>http.get('/admin/ai-generation/tasks').then(r=>r.data as AiGenerationTask[])
export const createGenerationTask=(body:GenerateRequest)=>http.post('/admin/ai-generation/tasks',body).then(r=>r.data as AiGenerationTask)
export const reviewAiCandidate=(id:number,body:ReviewRequest)=>http.post(`/admin/ai-generation/candidates/${id}/review`,body).then(r=>r.data as AiCandidate)
export const fetchGenerationStats=()=>http.get('/admin/ai-generation/stats').then(r=>r.data as AiGenerationStats)
export const adminAiGenerationClient:AiGenerationClient={
  fetchMothers:fetchGenerationMothers,
  fetchTasks:fetchGenerationTasks,
  fetchKnowledgePoints,
  createTask:createGenerationTask,
  reviewCandidate:reviewAiCandidate,
  fetchStats:fetchGenerationStats,
}
