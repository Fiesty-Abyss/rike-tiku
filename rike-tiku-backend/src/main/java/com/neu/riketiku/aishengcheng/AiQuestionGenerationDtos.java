package com.neu.riketiku.aishengcheng;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class AiQuestionGenerationDtos {
    private AiQuestionGenerationDtos() { }
    public record Generate(@NotNull Long motherQuestionId, @NotBlank String questionType,
                           @NotEmpty List<Long> knowledgePointIds,
                           @NotNull @Min(1) @Max(5) Integer targetDifficulty,
                           @NotBlank String variationMode,
                           @NotNull @Min(1) @Max(3) Integer count) { }
    public record Task(Long id, Long motherQuestionId, Long creatorId, String creatorRole,
                       String questionType, List<Long> knowledgePointIds, int targetDifficulty,
                       String variationMode, int requestedCount, String requestHash,
                       String provider, String model, String promptVersion, String status,
                       int generatedCount, boolean visionUsed, String failureCode,
                       Long latencyMillis, LocalDateTime createdAt, LocalDateTime finishedAt,
                       List<Candidate> candidates) { }
    public record Candidate(Long questionId, Long taskId, String stem, String questionType,
                            int difficulty, String status, String variationSummary,
                            String duplicateWarning, boolean visionUsed, String provider,
                            String model, String correctAnswer, String standardAnalysis,
                            List<KnowledgePoint> knowledgePoints, Quality quality) { }
    public record KnowledgePoint(Long id, String name) { }
    public record Quality(Integer subjectCorrectness, Integer answerCorrectness,
                          Integer solvability, Integer knowledgeConsistency,
                          Integer difficultyMatch, String reviewResult,
                          Integer reviewMinutes, Long reviewerId, String reviewComment) { }
    public record Review(@NotNull @Min(0) @Max(1) Integer subjectCorrectness,
                         @NotNull @Min(0) @Max(1) Integer answerCorrectness,
                         @NotNull @Min(0) @Max(1) Integer solvability,
                         @NotNull @Min(0) @Max(1) Integer knowledgeConsistency,
                         @NotNull @Min(0) @Max(1) Integer difficultyMatch,
                         @NotBlank String reviewResult,
                         @NotNull @Min(0) @Max(10080) Integer reviewMinutes,
                         @Size(max=2000) String reviewComment) { }
    public record Stats(long tasks, long successfulTasks, long failedTasks, long requested,
                        long generated, long suspectedDuplicates, long approved, long rejected,
                        Double averageLatencyMillis, Double averageReviewMinutes) { }
    public record MotherOption(Long id, Long subjectId, String subjectCode, String stem,
                               String questionType, int difficulty) { }
}
