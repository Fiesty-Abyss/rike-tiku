import http from '../http'
export type QuestionType='SINGLE_CHOICE'|'MULTIPLE_CHOICE'|'FILL_BLANK'|'SUBJECTIVE'
export interface Option { label:string; content:string; correct:boolean }
export interface Source { contentType:string; sourceType:string; sourceName:string; rightsStatus:string; sourceAddress?:string; year?:number; region?:string; paperName?:string; questionNumber?:string; rightsBasis?:string }
export interface QuestionItem { id:number; subjectCode:string; subjectName:string; questionType:QuestionType; usageMode:string; stemSummary:string; difficulty:number; autoGradable:boolean; status:string; rightsStatus:string; createdAt:string; updatedAt:string }
export interface Detail { question:QuestionItem; stem:string; correctAnswer:string; options:Option[]; standardAnalysis:string; knowledgePoints:Array<{id:number;code:string;name:string;path:string}>; sources:Source[]; attachments:Array<{id:number;position:string;type:string;fileName:string;objectMarker?:string;status:string;renderStatus?:string;contentUrl?:string}>; reviews:Array<{id:number;action:string;fromStatus:string;toStatus:string;reviewerId?:number;opinion?:string;createdAt:string}>; allowedActions:string[] }
export interface Save { subjectId:number; questionType:QuestionType; usageMode:string; stem:string; correctAnswer:string; difficulty:number; difficultyDescription?:string; autoGradable:boolean; options:Option[]; standardAnalysis:string; knowledgePointIds:number[]; sources:Source[] }
export const fetchQuestions=(params:Record<string,unknown>)=>http.get('/admin/questions',{params}).then(r=>r.data)
export const fetchQuestion=(id:number)=>http.get(`/admin/questions/${id}`).then(r=>r.data as Detail)
export const createQuestion=(body:Save)=>http.post('/admin/questions',body).then(r=>r.data as Detail)
export const updateQuestion=(id:number,body:Save)=>http.put(`/admin/questions/${id}`,body).then(r=>r.data as Detail)
export const questionAction=(id:number,action:string,opinion?:string)=>http.post(`/admin/questions/${id}/${action}`,opinion===undefined?undefined:{opinion}).then(r=>r.data as Detail)
export const fetchKnowledgePoints=(subjectId:number)=>http.get('/admin/knowledge-points',{params:{subjectId}}).then(r=>r.data as Array<{id:number;code:string;name:string;path:string}>)
