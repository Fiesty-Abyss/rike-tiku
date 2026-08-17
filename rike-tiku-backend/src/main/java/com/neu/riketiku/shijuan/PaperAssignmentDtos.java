package com.neu.riketiku.shijuan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import tools.jackson.databind.JsonNode;

public final class PaperAssignmentDtos {
    private PaperAssignmentDtos() {}

    public record Publish(@NotNull Long teachingScopeId, @NotNull @Future LocalDateTime deadline) {}
    public record Release(Long id, Long paperId, String paperName, String subjectName, String className,
                          LocalDateTime publishedAt, LocalDateTime deadline, String status,
                          String submissionStatus, BigDecimal score, BigDecimal objectiveTotal) {}
    public record Option(String label, String content) {}
    public record Attachment(Long id, String position, String type, String fileName, String objectMarker,
                             String description, int order, String contentUrl) {}
    public record Question(Long itemId, int order, BigDecimal score, String type, String stem, int answerSlots,
                           List<Option> options, JsonNode submittedAnswer, Boolean correct,
                           BigDecimal awardedScore, String correctAnswer, String standardAnalysis,
                           List<String> knowledgePoints, List<Attachment> stemAttachments,
                           List<Attachment> analysisAttachments) {}
    public record Detail(Release release, List<Question> questions, boolean answersVisible) {}
    public record DraftAnswer(@NotNull Long itemId, JsonNode answer) {}
    public record SaveDraft(@NotEmpty List<@Valid DraftAnswer> answers) {}
    public record Submit(@NotEmpty List<@Valid DraftAnswer> answers) {}
    public record SubmitResult(Long submissionId, BigDecimal objectiveScore, BigDecimal objectiveTotal,
                               int correctCount, int objectiveCount, int subjectivePendingCount) {}
    public record QuestionMetric(Long itemId, int order, long answered, long correct, BigDecimal accuracy) {}
    public record KnowledgeMetric(String knowledgePoint, long answered, long correct, BigDecimal accuracy) {}
    public record ClassStats(long assigned, long submitted, long unsubmitted, BigDecimal averageScore,
                             List<QuestionMetric> questions, List<KnowledgeMetric> knowledgePoints,
                             List<String> weakPoints) {}
    public record StudentTrend(Long releaseId, String paperName, LocalDateTime submittedAt,
                               BigDecimal score, BigDecimal total, BigDecimal rate) {}
    public record StudentProfile(Long studentId, List<StudentTrend> trend, List<String> weakTypes,
                                 List<String> weakPoints, List<String> recommendedReviewOrder) {}
    public record QualityAssessment(String status, String notice, List<String> coverage,
                                    List<String> risks, List<String> suggestions) {}
    public record AiQualityAssessment(String status, String notice, String provider, String model,
                                      String content, QualityAssessment deterministicFacts) {}
}
