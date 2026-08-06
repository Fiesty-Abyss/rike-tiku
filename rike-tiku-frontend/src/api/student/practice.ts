import http from '../http'

export type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'FILL_BLANK'
export interface Subject { id:number; code:string; name:string }
export interface KnowledgePoint { id:number; name:string; path:string }
export interface Option { label:string; content:string }
export interface PracticeQuestion { practiceQuestionId:number; order:number; questionType:QuestionType; stem:string; difficulty:number; score:number; blankCount:number; options:Option[]; knowledgePoints:KnowledgePoint[] }
export interface PracticeSession { id:number; subjectId:number; subjectCode:string; subjectName:string; status:'CREATED'|'SUBMITTED'; questionCount:number; createdAt:string; submittedAt?:string; questions:PracticeQuestion[] }
export interface PracticeResultQuestion { question:PracticeQuestion; studentAnswer:unknown; correctAnswer:unknown; standardAnalysis:string; correct:boolean; score:number }
export interface PracticeResult { sessionId:number; totalCount:number; correctCount:number; totalScore:number; submittedAt:string; questions:PracticeResultQuestion[] }
export interface WrongQuestion { questionId:number; subjectCode:string; subjectName:string; questionType:QuestionType; stemSummary:string; errorCount:number; consecutiveCorrectCount:number; status:'NEW'|'REVIEWING'|'MASTERED'; lastWrongAt:string }
export interface WrongQuestionDetail { wrongQuestion:WrongQuestion; stem:string; options:Option[]; latestStudentAnswer:unknown; correctAnswer:unknown; standardAnalysis:string; knowledgePoints:KnowledgePoint[]; attachments:Array<{id:number;position:string;type:string;fileName:string;objectMarker?:string;status:string}> }
export interface CreatePracticeRequest { subjectId:number; knowledgePointIds?:number[]; questionTypes?:QuestionType[]; difficulty?:number; count:number }
export interface SubmitPracticeRequest { answers:Array<{practiceQuestionId:number;answer:unknown;elapsedSeconds?:number}> }

export const fetchPracticeOptions=(subjectId?:number)=>http.get('/student/practice-options',{params:subjectId?{subjectId}:undefined}).then(r=>r.data as {subjects:Subject[];knowledgePoints:KnowledgePoint[]})
export const createPracticeSession=(body:CreatePracticeRequest)=>http.post('/student/practice-sessions',body).then(r=>r.data as PracticeSession)
export const fetchPracticeSession=(id:number)=>http.get(`/student/practice-sessions/${id}`).then(r=>r.data as PracticeSession)
export const submitPracticeSession=(id:number,body:SubmitPracticeRequest)=>http.post(`/student/practice-sessions/${id}/submit`,body).then(r=>r.data as PracticeResult)
export const fetchPracticeResult=(id:number)=>http.get(`/student/practice-sessions/${id}/result`).then(r=>r.data as PracticeResult)
export const fetchWrongQuestions=()=>http.get('/student/wrong-questions').then(r=>r.data as WrongQuestion[])
export const fetchWrongQuestion=(questionId:number)=>http.get(`/student/wrong-questions/${questionId}`).then(r=>r.data as WrongQuestionDetail)
