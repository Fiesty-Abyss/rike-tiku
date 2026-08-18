import http from '../http'

export interface PaperAttachment { id:number; position:string; type:string; fileName:string; objectMarker?:string; description?:string; order:number; contentUrl:string }
export interface PaperQuestionOption { id:number; type:string; stem:string; difficulty:number; usageMode:string; topicType?:string; knowledgePoints:string[]; stemAttachments:PaperAttachment[] }
export interface PaperQuestion { id:number; order:number; score:number; type:string; stem:string; difficulty:number; usageMode:string; topicType?:string; options:{label:string;content:string}[]; correctAnswer:string; standardAnalysis:string; knowledgePoints:string[]; stemAttachments:PaperAttachment[]; analysisAttachments:PaperAttachment[] }
export interface Paper { id:number; subjectId:number; subjectName:string; name:string; mode:string; totalScore:number; status:string; questions:PaperQuestion[] }
export interface PaperRelease { id:number;paperId:number;paperName:string;subjectName:string;className:string;publishedAt:string;deadline:string;status:string;submissionStatus?:string;score?:number;objectiveTotal?:number }
export interface PaperReleaseOverview { releaseId:number;paperId:number;paperName:string;subjectId:number;subjectName:string;teachingScopeId:number;classId:number;className:string;publishedAt:string;deadline:string;status:string }
export interface PaperReleasePage { items:PaperReleaseOverview[];total:number }
export interface PaperClassStats { assigned:number;submitted:number;unsubmitted:number;averageScore:number;weakPoints:string[];questions:{itemId:number;order:number;answered:number;correct:number;accuracy:number}[];knowledgePoints:{knowledgePoint:string;answered:number;correct:number;accuracy:number}[] }
export interface PaperSubmissionRow { studentId:number;studentNumber:string;studentName:string;status:string;objectiveScore?:number;objectiveTotal?:number;submittedAt?:string;subjectivePendingCount:number }
export interface PaperQualityAssessment { status:string;notice:string;coverage:string[];risks:string[];suggestions:string[] }
export interface AiPaperQualityAssessment { status:string;notice:string;provider:string;model:string;content:string;deterministicFacts:PaperQualityAssessment }

export const fetchPapers=()=>http.get('/teacher/papers').then(r=>r.data)
export const deletePaper=(paperId:number)=>http.post(`/teacher/papers/${paperId}/delete`).then(r=>r.data)
export const fetchPaperQuestions=(params:Record<string,unknown>)=>http.get<PaperQuestionOption[]>('/teacher/papers/questions',{params}).then(r=>r.data)
export const fetchPaper=(id:number)=>http.get<Paper>(`/teacher/papers/${id}`).then(r=>r.data)
export const createPaper=(body:unknown)=>http.post<Paper>('/teacher/papers',body).then(r=>r.data)
export const createRulePaper=(body:unknown)=>http.post<Paper>('/teacher/papers/rule',body).then(r=>r.data)
export const publishPaper=(paperId:number,body:{teachingScopeId:number;deadline:string})=>http.post<PaperRelease>(`/teacher/papers/${paperId}/releases`,body).then(r=>r.data)
export const fetchPaperStats=(releaseId:number)=>http.get<PaperClassStats>(`/teacher/papers/releases/${releaseId}/stats`).then(r=>r.data)
export const fetchPaperReleases=(paperId:number)=>http.get<PaperRelease[]>(`/teacher/papers/${paperId}/releases`).then(r=>r.data)
export const fetchTeacherPaperReleases=(params:{teachingScopeId?:number;status?:string;keyword?:string;page?:number;size?:number})=>http.get<PaperReleasePage>('/teacher/papers/releases',{params}).then(r=>r.data)
export const fetchPaperSubmissions=(releaseId:number)=>http.get<PaperSubmissionRow[]>(`/teacher/papers/releases/${releaseId}/submissions`).then(r=>r.data)
export const fetchTeacherSubmission=(releaseId:number,studentId:number)=>http.get(`/teacher/papers/releases/${releaseId}/students/${studentId}/submission`).then(r=>r.data)
export const cancelPaperRelease=(releaseId:number)=>http.post<PaperRelease>(`/teacher/papers/releases/${releaseId}/cancel`).then(r=>r.data)
export const fetchPaperQuality=(paperId:number)=>http.get<PaperQualityAssessment>(`/teacher/papers/${paperId}/quality-assessment`).then(r=>r.data)
export const requestAiPaperQuality=(paperId:number)=>http.post<AiPaperQualityAssessment>(`/teacher/papers/${paperId}/quality-assessment/ai`).then(r=>r.data)
export const createRandomPaper=(body:unknown)=>http.post<Paper>('/teacher/papers/random',body).then(r=>r.data)
export const fetchPaperKnowledgePoints=(subjectId:number)=>http.get<{id:number;path:string}[]>('/teacher/papers/knowledge-points',{params:{subjectId}}).then(r=>r.data)
