import http from '../http'

export interface StudentPaperRelease { id:number;paperId:number;paperName:string;subjectName:string;className:string;publishedAt:string;deadline:string;status:string;submissionStatus:string;score?:number;objectiveTotal?:number }
export interface StudentPaperAttachment { id:number;position:string;type:string;fileName:string;objectMarker?:string;description?:string;order:number;contentUrl:string }
export interface StudentPaperQuestion { itemId:number;order:number;score:number;type:string;stem:string;answerSlots:number;options:{label:string;content:string}[];submittedAnswer:unknown;correct?:boolean;awardedScore?:number;correctAnswer?:string;standardAnalysis?:string;knowledgePoints:string[];stemAttachments:StudentPaperAttachment[];analysisAttachments:StudentPaperAttachment[] }
export interface StudentPaperDetail { release:StudentPaperRelease;questions:StudentPaperQuestion[];answersVisible:boolean }
export const fetchStudentPapers=()=>http.get<StudentPaperRelease[]>('/student/papers').then(r=>r.data)
export const fetchStudentPaper=(id:number)=>http.get<StudentPaperDetail>(`/student/papers/${id}`).then(r=>r.data)
export const saveStudentPaperDraft=(id:number,answers:{itemId:number;answer:unknown}[])=>http.put(`/student/papers/${id}/draft`,{answers})
export const submitStudentPaper=(id:number,answers:{itemId:number;answer:unknown}[])=>http.post(`/student/papers/${id}/submit`,{answers}).then(r=>r.data)
