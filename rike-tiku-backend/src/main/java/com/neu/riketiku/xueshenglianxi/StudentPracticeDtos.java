package com.neu.riketiku.xueshenglianxi;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class StudentPracticeDtos {
    private StudentPracticeDtos() {
    }

    public record CreateRequest(
            @NotNull Long subjectId,
            List<Long> knowledgePointIds,
            List<String> questionTypes,
            @Min(1) @Max(3) Integer difficulty,
            @NotNull @Min(1) @Max(50) Integer count) {
    }

    public record Answer(@NotNull Long practiceQuestionId, @NotNull JsonNode answer, @Min(0) Integer elapsedSeconds) {
    }

    public record SubmitRequest(@NotEmpty List<@Valid Answer> answers) {
    }

    public record Subject(Long id, String code, String name) {
    }

    public record KnowledgePoint(Long id, String name, String path) {
    }

    public record Options(List<Subject> subjects, List<KnowledgePoint> knowledgePoints) {
    }

    public record Option(String label, String content) {
    }

    public record SessionQuestion(
            Long practiceQuestionId,
            int order,
            String questionType,
            String stem,
            int difficulty,
            BigDecimal score,
            int blankCount,
            List<Option> options,
            List<KnowledgePoint> knowledgePoints) {
    }

    public record Session(
            Long id,
            Long subjectId,
            String subjectCode,
            String subjectName,
            String status,
            int questionCount,
            LocalDateTime createdAt,
            LocalDateTime submittedAt,
            List<SessionQuestion> questions) {
    }

    public record ResultQuestion(
            SessionQuestion question,
            JsonNode studentAnswer,
            JsonNode correctAnswer,
            String standardAnalysis,
            boolean correct,
            BigDecimal score) {
    }

    public record Result(
            Long sessionId,
            int totalCount,
            int correctCount,
            BigDecimal totalScore,
            LocalDateTime submittedAt,
            List<ResultQuestion> questions) {
    }

    public record WrongQuestionItem(
            Long questionId,
            String subjectCode,
            String subjectName,
            String questionType,
            String stemSummary,
            int errorCount,
            int consecutiveCorrectCount,
            String status,
            LocalDateTime lastWrongAt) {
    }

    public record Attachment(Long id, String position, String type, String fileName, String objectMarker, String status) {
    }

    public record WrongQuestionDetail(
            WrongQuestionItem wrongQuestion,
            String stem,
            List<Option> options,
            JsonNode latestStudentAnswer,
            JsonNode correctAnswer,
            String standardAnalysis,
            List<KnowledgePoint> knowledgePoints,
            List<Attachment> attachments) {
    }
}
