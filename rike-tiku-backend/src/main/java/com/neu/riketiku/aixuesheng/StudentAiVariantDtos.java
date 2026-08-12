package com.neu.riketiku.aixuesheng;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import tools.jackson.databind.JsonNode;

public final class StudentAiVariantDtos {
    private StudentAiVariantDtos(){}
    public record Generate(@NotNull Long answerFactId){}
    public record Answer(@NotNull JsonNode answer){}
    public record Option(String label,String content){}
    public record Variant(Long id,Long answerFactId,Long motherQuestionId,Long questionId,String status,
                          String questionType,String stem,int difficulty,List<Option> options,JsonNode studentAnswer,
                          Boolean correct,JsonNode correctAnswer,String aiAnalysis,String reviewStatus){}
}
