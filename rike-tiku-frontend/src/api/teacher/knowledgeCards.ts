import http from '../http'
import type {KnowledgeCard} from '../student/knowledgeCards'
export interface KnowledgeCardSave{type:string;title:string;knowledgePointIds:number[];content:string;latex?:string;applicableConditions?:string;derivation?:string;commonMistake?:string;example?:string;mnemonic?:string;sourceName?:string;sourceUrl?:string;rightsStatus:string;sortOrder:number;aiDraft:boolean}
export const fetchTeacherKnowledgeCards=(scopeId:number)=>http.get<KnowledgeCard[]>(`/teacher/scopes/${scopeId}/knowledge-cards`).then(r=>r.data)
export const createKnowledgeCard=(scopeId:number,data:KnowledgeCardSave)=>http.post<KnowledgeCard>(`/teacher/scopes/${scopeId}/knowledge-cards`,data).then(r=>r.data)
export const reviewKnowledgeCard=(id:number,action:'APPROVE'|'REJECT'|'DISABLE',comment?:string)=>http.post<KnowledgeCard>(`/teacher/knowledge-cards/${id}/review`,{action,comment}).then(r=>r.data)
export const uploadKnowledgeCardImage=(id:number,file:File)=>{const data=new FormData();data.append('file',file);return http.post(`/teacher/knowledge-cards/${id}/attachments`,data,{headers:{'Content-Type':'multipart/form-data'}}).then(r=>r.data)}
