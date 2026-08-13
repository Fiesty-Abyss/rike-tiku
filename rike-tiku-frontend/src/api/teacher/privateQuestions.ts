import http from '../http';import type {Save} from '../admin/questions'
export interface PrivateQuestion{id:number;teachingAssignmentId:number;className:string;subjectName:string;questionType:string;stem:string;status:string}
export const fetchPrivateQuestions=()=>http.get<PrivateQuestion[]>('/teacher/private-questions').then(r=>r.data)
export const createPrivateQuestion=(scopeId:number,body:Save)=>http.post<PrivateQuestion>('/teacher/private-questions',body,{params:{teachingAssignmentId:scopeId}}).then(r=>r.data)
export const publishPrivateQuestion=(id:number)=>http.post<PrivateQuestion>(`/teacher/private-questions/${id}/publish`).then(r=>r.data)
