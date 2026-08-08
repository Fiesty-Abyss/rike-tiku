import http from './http'
export interface TeachingScope { teachingAssignmentId:number; classId:number;className:string;grade:string;subjectId:number;subjectCode:string;subjectName:string;homeroomSubject:boolean;teachingStatus:string }
export const fetchTeachingScopes=()=>http.get<TeachingScope[]>('/teacher/teaching-scopes').then(response=>response.data)

export interface WorkspaceStudent { studentNumber:string; name:string; grade:string }
export interface HighFrequencyPoint { id:number; teachingAssignmentId:number; knowledgePointId:number; knowledgePointName:string; title:string; content:string; memoryTrick:string|null; commonMistake:string|null; sortOrder:number; status:'ACTIVE'|'DISABLED'; teacherName:string }
export interface KnowledgePointOption { id:number; name:string; path:string }
export interface TeacherWorkspace { teachingAssignmentId:number; classId:number; className:string; grade:string; subjectId:number; subjectName:string; teacherName:string; studentCount:number; students:WorkspaceStudent[]; highFrequencyPoints:HighFrequencyPoint[]; knowledgePoints:KnowledgePointOption[] }
export interface HighFrequencyPointCreateRequest { knowledgePointId:number; title:string; content:string; memoryTrick?:string; commonMistake?:string; sortOrder:number }
export interface HighFrequencyPointUpdateRequest { title:string; content:string; memoryTrick?:string; commonMistake?:string; sortOrder:number }

export const fetchTeacherWorkspace=(scopeId:number)=>http.get<TeacherWorkspace>(`/teacher/scopes/${scopeId}`).then(response=>response.data)
export const createHighFrequencyPoint=(scopeId:number,request:HighFrequencyPointCreateRequest)=>http.post<HighFrequencyPoint>(`/teacher/scopes/${scopeId}/high-frequency-points`,request).then(response=>response.data)
export const updateHighFrequencyPoint=(id:number,request:HighFrequencyPointUpdateRequest)=>http.put<HighFrequencyPoint>(`/teacher/high-frequency-points/${id}`,request).then(response=>response.data)
export const updateHighFrequencyPointStatus=(id:number,status:'ACTIVE'|'DISABLED')=>http.post<HighFrequencyPoint>(`/teacher/high-frequency-points/${id}/status`,{status}).then(response=>response.data)
