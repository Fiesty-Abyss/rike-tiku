import http from './http'
export interface TeachingScope { classId:number;className:string;grade:string;subjectId:number;subjectCode:string;subjectName:string;homeroomSubject:boolean;teachingStatus:string }
export const fetchTeachingScopes=()=>http.get<TeachingScope[]>('/teacher/teaching-scopes').then(response=>response.data)
