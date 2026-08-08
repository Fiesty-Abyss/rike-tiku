import http from '../http'

export type MasteryLevel = 'NOT_STARTED' | 'INSUFFICIENT' | 'WEAK' | 'IMPROVING' | 'MASTERED'

export interface LearningOverallSummary {
  practicedKnowledgePointCount: number
  totalKnowledgePointCount: number
  totalAnsweredCount: number
  totalCorrectCount: number
  overallAccuracy: number | null
  weakKnowledgePointCount: number
  improvingKnowledgePointCount: number
  masteredKnowledgePointCount: number
  insufficientKnowledgePointCount: number
  notStartedKnowledgePointCount: number
}

export interface KnowledgePointMastery {
  knowledgePointId: number
  knowledgePointName: string
  fullPath: string
  answeredCount: number
  correctCount: number
  wrongCount: number
  accuracy: number | null
  activeWrongQuestionCount: number
  masteryLevel: MasteryLevel
}

export interface LearningRecommendation {
  knowledgePointId: number
  knowledgePointName: string
  reason: string
  practiceParameters: { subjectId: number; knowledgePointId: number; count: number }
}

export interface StudentLearningSummary {
  subject: { id: number; code: string; name: string }
  overall: LearningOverallSummary
  knowledgePoints: KnowledgePointMastery[]
  recommendations: LearningRecommendation[]
  recommendationMessage: string | null
}

export const fetchStudentLearningSummary = (subjectId: number) =>
  http.get<StudentLearningSummary>('/student/learning-summary', { params: { subjectId } }).then(response => response.data)
