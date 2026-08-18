package com.neu.riketiku.shijuan;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public final class PaperDtos {
    private PaperDtos() { }

    public record ItemInput(@NotNull Long questionId, @NotNull @DecimalMin("0.01") BigDecimal score) { }
    public record Save(@NotNull Long subjectId, @NotBlank @Size(max = 120) String name, @NotBlank String mode,
                       @NotEmpty List<ItemInput> items) { }
    public record Rule(@NotNull Long subjectId, @NotBlank @Size(max = 120) String name,
                       List<Long> knowledgePointIds, List<String> questionTypes, List<Integer> difficulties,
                       @NotNull @Min(1) @Max(100) Integer count,
                       @NotNull @DecimalMin("0.01") BigDecimal totalScore) { }
    public record KnowledgePoint(Long id, String path) { }
    public record Option(String label, String content) { }
    /** Live attachment metadata used only before a paper is released. */
    public record Attachment(Long id, String position, String type, String fileName, String objectMarker,
                             String description, int order, String contentUrl) { }
    public record QuestionOption(Long id, String type, String stem, int difficulty, String usageMode,
                                 String topicType, List<String> knowledgePoints,
                                 List<Attachment> stemAttachments) { }
    public record Question(Long id, int order, BigDecimal score, String type, String stem, int difficulty,
                           String usageMode, String topicType, List<Option> options, String correctAnswer,
                           String standardAnalysis, List<String> knowledgePoints,
                           List<Attachment> stemAttachments, List<Attachment> analysisAttachments) { }
    public record Paper(Long id, Long subjectId, String subjectName, String name, String mode,
                        BigDecimal totalScore, String status, List<Question> questions) { }
    public record ListItem(Long id, Long subjectId, String name, String mode, BigDecimal totalScore,
                           String status, int questionCount) { }
}
