export interface MotherOption { id:number; subjectId:number; subjectCode:string; stem:string; questionType:string; difficulty:number }
export interface KnowledgePointOption { id:number; name:string; path:string }
export interface AiQuality { subjectCorrectness?:number; answerCorrectness?:number; solvability?:number; knowledgeConsistency?:number; difficultyMatch?:number; reviewResult:string; reviewMinutes?:number; reviewerId?:number; reviewComment?:string }
export interface AiCandidate { questionId:number; taskId:number; stem:string; questionType:string; difficulty:number; status:string; variationSummary:string; duplicateWarning:string; visionUsed:boolean; provider:string; model:string; correctAnswer:string; standardAnalysis:string; knowledgePoints:Array<{id:number;name:string}>; quality:AiQuality; noveltyDecision?:'ACCEPT'|'WARN'|'REJECT'; noveltyScore?:number; similarityScore?:number; noveltyReason?:string }
export interface AiGenerationTask { id:number; motherQuestionId:number; creatorId:number; creatorRole:string; questionType:string; knowledgePointIds:number[]; targetDifficulty:number; variationMode:string; requestedCount:number; requestHash:string; provider?:string; model?:string; promptVersion:string; status:string; generatedCount:number; visionUsed:boolean; failureCode?:string; latencyMillis?:number; createdAt:string; finishedAt?:string; candidates:AiCandidate[] }
export interface GenerateRequest { motherQuestionId:number; questionType:string; knowledgePointIds:number[]; targetDifficulty:number; variationMode:string; count:number }
export interface ReviewRequest { subjectCorrectness:number; answerCorrectness:number; solvability:number; knowledgeConsistency:number; difficultyMatch:number; reviewResult:'APPROVED'|'REJECTED'; reviewMinutes:number; reviewComment?:string }
export interface AiGenerationStats { tasks:number; successfulTasks:number; failedTasks:number; requested:number; generated:number; suspectedDuplicates:number; approved:number; rejected:number; averageLatencyMillis?:number; averageReviewMinutes?:number }

export interface AiGenerationClient {
  fetchMothers():Promise<MotherOption[]>
  fetchTasks():Promise<AiGenerationTask[]>
  fetchKnowledgePoints(subjectId:number):Promise<KnowledgePointOption[]>
  createTask(body:GenerateRequest):Promise<AiGenerationTask>
  reviewCandidate(id:number,body:ReviewRequest):Promise<AiCandidate>
  fetchStats?:()=>Promise<AiGenerationStats>
}
