import http from '../http'

export type AiAnalysisStatus = 'NOT_GENERATED' | 'GENERATING' | 'SUCCESS' | 'FAILED'
export interface AiAnalysis {
  answerFactId:number
  status:AiAnalysisStatus
  errorType?:string
  errorReason?:string
  correctThinking?:string
  commonMistakes:string[]
  reviewSuggestions:string[]
  cached:boolean
  errorCode?:string
  createdAt?:string
  updatedAt?:string
}
export interface AiSource { title:string; url:string; publisher:string; publishDate?:string }
export interface AiMessage { id:number; role:'USER'|'ASSISTANT'; content:string; sequence:number; createdAt:string; sources:AiSource[] }
export interface AiModelOption { id:number; displayName:string; modelCode:string; available:boolean; defaultOption:boolean; capabilityTags:string[] }
export interface AiConversation {
  id:number
  answerFactId:number
  questionId:number
  status:'ACTIVE'|'LIMIT_REACHED'
  usedRounds:number
  maxRounds:number
  remainingRounds:number
  modelConfigId?:number
  thinkingMode:'STANDARD'|'DEEP'
  webSearch:boolean
  messages:AiMessage[]
}

export const fetchAiAnalysis = (answerFactId:number) => http.get(`/student/ai/analyses/${answerFactId}`).then(r => r.data as AiAnalysis)
export const generateAiAnalysis = (answerFactId:number) => http.post(`/student/ai/analyses/${answerFactId}`).then(r => r.data as AiAnalysis)
export interface AiConversationOptions { modelConfigId?:number; thinkingMode:'STANDARD'|'DEEP'; webSearch:boolean }
export const fetchAiModelOptions = () => http.get('/student/ai/model-options').then(r => r.data as AiModelOption[])
export const fetchAiCapabilities = () => http.get('/student/ai/capabilities').then(r => r.data as {webSearchAvailable:boolean})
export const createAiConversation = (answerFactId:number, options?:AiConversationOptions) => http.post('/student/ai/conversations', { answerFactId, ...options }).then(r => r.data as AiConversation)
export const fetchAiConversation = (conversationId:number) => http.get(`/student/ai/conversations/${conversationId}`).then(r => r.data as AiConversation)
export const sendAiMessage = (conversationId:number, content:string) => http.post(`/student/ai/conversations/${conversationId}/messages`, { content }).then(r => r.data as AiConversation)
export interface AiVariant { id:number;answerFactId:number;motherQuestionId:number;questionId:number;status:'READY'|'ANSWERED'|'SUBMITTED_FOR_REVIEW'|'DISCARDED';questionType:string;stem:string;difficulty:number;options:{label:string;content:string}[];studentAnswer?:unknown;correct?:boolean;correctAnswer?:unknown;aiAnalysis?:string;reviewStatus:string }
export const generateAiVariant=(answerFactId:number)=>http.post<AiVariant>('/student/ai/variants',{answerFactId}).then(r=>r.data)
export const answerAiVariant=(id:number,answer:unknown)=>http.post<AiVariant>(`/student/ai/variants/${id}/answer`,{answer}).then(r=>r.data)
export const submitAiVariantReview=(id:number)=>http.post<AiVariant>(`/student/ai/variants/${id}/submit-review`).then(r=>r.data)
export const discardAiVariant=(id:number)=>http.delete(`/student/ai/variants/${id}`)
