import http from '../http'
export interface KnowledgeCardPoint{id:number;name:string;path:string}
export interface KnowledgeCardAttachment{id:number;name:string;mime:string;size:number;contentUrl:string}
export interface KnowledgeCard{id:number;subjectId:number;subjectCode:string;subjectName:string;teachingScopeId:number;className:string;type:string;title:string;knowledgePoints:KnowledgeCardPoint[];content:string;latex?:string;applicableConditions?:string;derivation?:string;commonMistake?:string;example?:string;mnemonic?:string;sourceName?:string;sourceUrl?:string;rightsStatus:string;status:string;sortOrder:number;favorite:boolean;mastery:'LEARNING'|'MASTERED';attachments:KnowledgeCardAttachment[]}
export interface Frequency{window:string;occurrences:number;paperCount:number;sampleNotice?:string}
export interface KnowledgeCardQuery{subjectId?:number;knowledgePointId?:number;type?:string;favorite?:boolean;mastery?:string}
export const fetchKnowledgeCards=(query:KnowledgeCardQuery={})=>http.get<KnowledgeCard[]>('/student/knowledge-cards',{params:query}).then(r=>r.data)
export const updateKnowledgeCardState=(id:number,state:{favorite:boolean;mastery:'LEARNING'|'MASTERED'})=>http.put<KnowledgeCard>(`/student/knowledge-cards/${id}/state`,state).then(r=>r.data)
export const fetchKnowledgeCardFrequency=(id:number)=>http.get<Frequency[]>(`/student/knowledge-cards/${id}/frequency`).then(r=>r.data)
export const fetchKnowledgeCardAttachment=(cardId:number,attachmentId:number)=>http.get<Blob>(`/student/knowledge-cards/${cardId}/attachments/${attachmentId}/content`,{responseType:'blob'}).then(r=>r.data)
