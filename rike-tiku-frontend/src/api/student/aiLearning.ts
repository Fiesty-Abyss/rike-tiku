import http from '../http'

const AI_ANALYSIS_TIMEOUT = 90_000
const AI_VARIANT_TIMEOUT = 120_000

export type AiAnalysisStatus = 'NOT_GENERATED' | 'GENERATING' | 'SUCCESS' | 'FAILED'
export interface AiAnalysis {
  answerFactId?:number
  topicQuestionId?:number
  knowledgeCardId?:number
  contextType:'PRACTICE_RESULT'|'TOPIC_QUESTION'|'KNOWLEDGE_CARD'
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

export const fetchAiAnalysis = (answerFactId:number) => http.get(`/student/ai/analyses/${answerFactId}`, { timeout: AI_ANALYSIS_TIMEOUT }).then(r => r.data as AiAnalysis)
export const generateAiAnalysis = (answerFactId:number) => http.post(`/student/ai/analyses/${answerFactId}`, undefined, { timeout: AI_ANALYSIS_TIMEOUT }).then(r => r.data as AiAnalysis)
export interface AiConversationOptions { modelConfigId?:number; thinkingMode:'STANDARD'|'DEEP'; webSearch:boolean }
export const fetchAiModelOptions = () => http.get('/student/ai/model-options').then(r => r.data as AiModelOption[])
export const fetchAiCapabilities = () => http.get('/student/ai/capabilities').then(r => r.data as {webSearchAvailable:boolean})
export const createAiConversation = (answerFactId:number, options?:AiConversationOptions) => http.post('/student/ai/conversations', { answerFactId, ...options }, { timeout: AI_ANALYSIS_TIMEOUT }).then(r => r.data as AiConversation)
export const createTopicAiConversation = (topicQuestionId:number, options?:AiConversationOptions) => http.post('/student/ai/conversations', { topicQuestionId, contextType:'TOPIC_QUESTION', ...options }, { timeout: AI_ANALYSIS_TIMEOUT }).then(r => r.data as AiConversation)
export const createKnowledgeCardAiConversation = (knowledgeCardId:number, options?:AiConversationOptions) => http.post('/student/ai/conversations', { knowledgeCardId, contextType:'KNOWLEDGE_CARD', ...options }, { timeout: AI_ANALYSIS_TIMEOUT }).then(r => r.data as AiConversation)
export const fetchAiConversation = (conversationId:number) => http.get(`/student/ai/conversations/${conversationId}`, { timeout: AI_ANALYSIS_TIMEOUT }).then(r => r.data as AiConversation)
export const sendAiMessage = (conversationId:number, content:string) => http.post(`/student/ai/conversations/${conversationId}/messages`, { content }, { timeout: AI_ANALYSIS_TIMEOUT }).then(r => r.data as AiConversation)
export type VariationMode = 'SCENARIO_TRANSFER'|'CONDITION_RECOMBINATION'|'REPRESENTATION_SWITCH'|'MULTI_STEP_EXTENSION'|'DISTRACTOR_REDESIGN'|'COMBINED'
export interface AiVariant { id:number;answerFactId:number;motherQuestionId:number;questionId:number;status:'READY'|'ANSWERED'|'SUBMITTED_FOR_REVIEW'|'DISCARDED';questionType:string;stem:string;difficulty:number;options:{label:string;content:string}[];studentAnswer?:unknown;correct?:boolean;correctAnswer?:unknown;aiAnalysis?:string;reviewStatus:string;variationMode?:VariationMode }
export const generateAiVariant=(answerFactId:number,targetDifficulty?:number,variationMode:VariationMode='COMBINED')=>http.post<AiVariant>('/student/ai/variants',{answerFactId,targetDifficulty,variationMode},{ timeout: AI_VARIANT_TIMEOUT }).then(r=>r.data)
export const answerAiVariant=(id:number,answer:unknown)=>http.post<AiVariant>(`/student/ai/variants/${id}/answer`,{answer},{ timeout: AI_VARIANT_TIMEOUT }).then(r=>r.data)
export const submitAiVariantReview=(id:number)=>http.post<AiVariant>(`/student/ai/variants/${id}/submit-review`,undefined,{ timeout: AI_VARIANT_TIMEOUT }).then(r=>r.data)
export const discardAiVariant=(id:number)=>http.delete(`/student/ai/variants/${id}`)
