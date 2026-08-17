package com.neu.riketiku.aixuesheng;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import tools.jackson.databind.JsonNode;

public final class StudentAiVariantDtos {
    private StudentAiVariantDtos(){}
    public record Generate(@NotNull Long answerFactId, @Min(1) @Max(5) Integer targetDifficulty,String variationMode){}
    public record Answer(@NotNull JsonNode answer){}
    public record Option(String label,String content){}
    public record Variant(Long id,Long answerFactId,Long motherQuestionId,Long questionId,String status,String variationMode,
                          String questionType,String stem,int difficulty,List<Option> options,JsonNode studentAnswer,
                          Boolean correct,JsonNode correctAnswer,String aiAnalysis,String reviewStatus,
                          String noveltyDecision,Double noveltyScore,Double similarityScore,String noveltyReason){}
}
