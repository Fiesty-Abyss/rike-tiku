package com.neu.riketiku.xuexizhangwo;

import java.math.BigDecimal;
import java.util.List;

public final class LearningMasteryDtos {
    private LearningMasteryDtos() {
    }

    public record SubjectResponse(Long id, String code, String name) {
    }

    public record OverallSummary(
            int practicedKnowledgePointCount,
            int totalKnowledgePointCount,
            int totalAnsweredCount,
            int totalCorrectCount,
            BigDecimal overallAccuracy,
            int weakKnowledgePointCount,
            int improvingKnowledgePointCount,
            int masteredKnowledgePointCount,
            int insufficientKnowledgePointCount,
            int notStartedKnowledgePointCount) {
    }

    public record KnowledgePointSummary(
            Long knowledgePointId,
            String knowledgePointName,
            String fullPath,
            int answeredCount,
            int correctCount,
            int wrongCount,
            BigDecimal accuracy,
            int activeWrongQuestionCount,
            String masteryLevel) {
    }

    public record PracticeParameters(Long subjectId, Long knowledgePointId, int count) {
    }

    public record Recommendation(
            Long knowledgePointId,
            String knowledgePointName,
            String reason,
            PracticeParameters practiceParameters) {
    }

    public record StudentLearningSummary(
            SubjectResponse subject,
            OverallSummary overall,
            List<KnowledgePointSummary> knowledgePoints,
            List<Recommendation> recommendations,
            String recommendationMessage) {
    }

    public record TeacherStudentLearningSummary(
            Long studentId,
            String studentNumber,
            String name,
            String grade,
            int answeredCount,
            int correctCount,
            BigDecimal accuracy,
            int weakKnowledgePointCount,
            int masteredKnowledgePointCount) {
    }

    public record TeacherScopeLearningSummary(
            Long teachingAssignmentId,
            String className,
            Long subjectId,
            String subjectName,
            List<TeacherStudentLearningSummary> students) {
    }
}
