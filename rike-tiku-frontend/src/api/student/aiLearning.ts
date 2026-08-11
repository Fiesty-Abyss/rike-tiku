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
export interface AiMessage { id:number; role:'USER'|'ASSISTANT'; content:string; sequence:number; createdAt:string }
export interface AiConversation {
  id:number
  answerFactId:number
  questionId:number
  status:'ACTIVE'|'LIMIT_REACHED'
  usedRounds:number
  maxRounds:number
  remainingRounds:number
  messages:AiMessage[]
}

export const fetchAiAnalysis = (answerFactId:number) => http.get(`/student/ai/analyses/${answerFactId}`).then(r => r.data as AiAnalysis)
export const generateAiAnalysis = (answerFactId:number) => http.post(`/student/ai/analyses/${answerFactId}`).then(r => r.data as AiAnalysis)
export const createAiConversation = (answerFactId:number) => http.post('/student/ai/conversations', { answerFactId }).then(r => r.data as AiConversation)
export const fetchAiConversation = (conversationId:number) => http.get(`/student/ai/conversations/${conversationId}`).then(r => r.data as AiConversation)
export const sendAiMessage = (conversationId:number, content:string) => http.post(`/student/ai/conversations/${conversationId}/messages`, { content }).then(r => r.data as AiConversation)
