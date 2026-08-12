import http from '../http'
import type { AiCandidate, AiGenerationClient, AiGenerationTask, GenerateRequest, KnowledgePointOption, MotherOption, ReviewRequest } from '../aiGeneration'

export const fetchTeacherGenerationMothers=()=>http.get('/teacher/ai-generation/mothers').then(r=>r.data as MotherOption[])
export const fetchTeacherGenerationTasks=()=>http.get('/teacher/ai-generation/tasks').then(r=>r.data as AiGenerationTask[])
export const fetchTeacherGenerationTask=(id:number)=>http.get(`/teacher/ai-generation/tasks/${id}`).then(r=>r.data as AiGenerationTask)
export const fetchTeacherGenerationKnowledgePoints=(subjectId:number)=>http.get('/teacher/ai-generation/knowledge-points',{params:{subjectId}}).then(r=>r.data as KnowledgePointOption[])
export const createTeacherGenerationTask=(body:GenerateRequest)=>http.post('/teacher/ai-generation/tasks',body).then(r=>r.data as AiGenerationTask)
export const reviewTeacherAiCandidate=(id:number,body:ReviewRequest)=>http.post(`/teacher/ai-generation/candidates/${id}/review`,body).then(r=>r.data as AiCandidate)

export const teacherAiGenerationClient:AiGenerationClient={
  fetchMothers:fetchTeacherGenerationMothers,
  fetchTasks:fetchTeacherGenerationTasks,
  fetchKnowledgePoints:fetchTeacherGenerationKnowledgePoints,
  createTask:createTeacherGenerationTask,
  reviewCandidate:reviewTeacherAiCandidate,
}
