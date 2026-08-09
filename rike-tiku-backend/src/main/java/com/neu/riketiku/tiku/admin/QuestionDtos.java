package com.neu.riketiku.tiku.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public final class QuestionDtos {
    private QuestionDtos() { }
    public record Option(String label, String content, boolean correct) { }
    public record Source(String contentType, String sourceType, String sourceName, String rightsStatus, String sourceAddress, Integer year, String region, String paperName, String questionNumber, String rightsBasis) { }
    public record Save(@NotNull Long subjectId, @NotBlank String questionType, @NotBlank String usageMode, @NotBlank String stem,
                       @NotBlank String correctAnswer, @NotNull Integer difficulty, String difficultyDescription, @NotNull Boolean autoGradable,
                       List<Option> options, @NotBlank String standardAnalysis, List<Long> knowledgePointIds, List<Source> sources) { }
    public record Action(String opinion) { }
    public record Item(Long id, String subjectCode, String subjectName, String questionType, String usageMode, String stemSummary,
                       Integer difficulty, boolean autoGradable, String status, String rightsStatus, LocalDateTime createdAt, LocalDateTime updatedAt) { }
    public record Page(List<Item> records, long total, long page, long size, long pages) { }
    public record Detail(Item question, String stem, String correctAnswer, List<Option> options, String standardAnalysis, List<KnowledgePoint> knowledgePoints,
                         List<Source> sources, List<Attachment> attachments, List<Review> reviews, List<String> allowedActions) { }
    public record KnowledgePoint(Long id, String code, String name, String path) { }
    public record Attachment(Long id, String position, String type, String fileName, String objectMarker, String status,
                             String renderStatus, String contentUrl) { }
    public record Review(Long id, String action, String fromStatus, String toStatus, Long reviewerId, String opinion, LocalDateTime createdAt) { }
}
