import http from '../http'

export interface TopicKnowledgePoint { id:number; name:string; path:string }
export interface TopicAttachment { id:number; position:string; type:string; fileName:string; objectMarker?:string; status:string; renderStatus:string; description?:string; order:number; contentUrl:string }
export interface TopicItem { id:number; subjectId:number; subjectCode:string; subjectName:string; title:string; topicType:'CALCULATION'|'EXPERIMENT'|'PROCESS'|'MATERIAL_ANALYSIS'|'COMPREHENSIVE'; difficulty:number; knowledgePoints:TopicKnowledgePoint[] }
export interface TopicDetail extends TopicItem { material:string; standardAnalysis:string; stemAttachments:TopicAttachment[]; analysisAttachments:TopicAttachment[] }
export interface TopicUnitItem { id:number; subjectId:number; subjectCode:string; subjectName:string; title:string; introduction:string; difficulty:number; primaryKnowledgePoint:TopicKnowledgePoint; questionCount:number }
export interface TopicUnitQuestion { stage:'FOUNDATION'|'TRANSFER'|'ADVANCED'; order:number; question:TopicItem }
export interface TopicUnitDetail extends TopicUnitItem { questions:TopicUnitQuestion[] }
export interface TopicVariantTask { id:number; status:'GENERATING'|'SUCCESS'|'FAILED'; generatedCount:number; candidates:Array<{questionId:number;stem:string;difficulty:number;status:string;variationSummary:string;standardAnalysis:string}> }
export const fetchTopics=(subjectCode?:string)=>http.get<TopicItem[]>('/student/topic-learning',{params:subjectCode?{subjectCode}:undefined}).then(response=>response.data)
export const fetchTopic=(id:number)=>http.get<TopicDetail>(`/student/topic-learning/${id}`).then(response=>response.data)
export const fetchTopicUnits=(subjectCode?:string)=>http.get<TopicUnitItem[]>('/student/topic-learning/units',{params:subjectCode?{subjectCode}:undefined}).then(response=>response.data)
export const fetchTopicUnit=(id:number)=>http.get<TopicUnitDetail>(`/student/topic-learning/units/${id}`).then(response=>response.data)
export const generateTopicVariants=(id:number,body:{targetDifficulty:number;variationMode:string;count:number;requireVisualContext:boolean;keepPrimaryKnowledgePoint:boolean})=>http.post<TopicVariantTask>(`/student/topic-learning/${id}/variants`,body).then(response=>response.data)
