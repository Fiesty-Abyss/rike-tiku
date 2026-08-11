import http from '../http'

export interface TopicKnowledgePoint { id:number; name:string; path:string }
export interface TopicItem { id:number; subjectId:number; subjectCode:string; subjectName:string; title:string; difficulty:number; knowledgePoints:TopicKnowledgePoint[] }
export interface TopicDetail extends TopicItem { material:string; standardAnalysis:string }
export const fetchTopics=(subjectCode?:string)=>http.get<TopicItem[]>('/student/topic-learning',{params:subjectCode?{subjectCode}:undefined}).then(response=>response.data)
export const fetchTopic=(id:number)=>http.get<TopicDetail>(`/student/topic-learning/${id}`).then(response=>response.data)
