package com.neu.riketiku.zhuantixuexi;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class TopicLearningDtos {
    private TopicLearningDtos() {
    }

    public record KnowledgePoint(Long id, String name, String path) {
    }

    public record Attachment(Long id, String position, String type, String fileName, String objectMarker,
                             String status, String renderStatus, String description, int order, String contentUrl) {
    }

    public record UnitItem(Long id, Long subjectId, String subjectCode, String subjectName,
                           String title, String introduction, int difficulty, KnowledgePoint primaryKnowledgePoint,
                           int questionCount) {
    }

    public record UnitQuestion(String stage, int order, TopicItem question) {
    }

    public record UnitDetail(Long id, Long subjectId, String subjectCode, String subjectName,
                             String title, String introduction, int difficulty,
                             KnowledgePoint primaryKnowledgePoint, List<UnitQuestion> questions) {
    }

    public record VariantRequest(@NotNull @Min(1) @Max(5) Integer targetDifficulty,
                                 @NotBlank String variationMode,@NotNull @Min(1) @Max(3) Integer count,
                                 @NotNull Boolean requireVisualContext,@NotNull Boolean keepPrimaryKnowledgePoint) { }

    public record TopicItem(Long id, Long subjectId, String subjectCode, String subjectName,
                            String title, String topicType, int difficulty, List<KnowledgePoint> knowledgePoints) {
    }

    public record TopicDetail(Long id, Long subjectId, String subjectCode, String subjectName,
                              String title, String material, String topicType, int difficulty, String standardAnalysis,
                              List<KnowledgePoint> knowledgePoints, List<Attachment> stemAttachments,
                              List<Attachment> analysisAttachments) {
    }
}
