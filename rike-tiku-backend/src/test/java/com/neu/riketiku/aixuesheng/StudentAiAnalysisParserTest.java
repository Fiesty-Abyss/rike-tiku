package com.neu.riketiku.aixuesheng;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StudentAiAnalysisParserTest {
    private final StudentAiAnalysisParser parser = new StudentAiAnalysisParser();

    @Test
    void acceptsOnlyBoundedControlledShape() {
        var value = parser.parse("""
                {"errorType":"CONCEPT_ERROR","errorReason":"混淆了概念","correctThinking":"先确定受力关系",
                 "commonMistakes":["直接套公式"],"reviewSuggestions":["复习受力分析"]}
                """);
        assertThat(value.errorType()).isEqualTo(StudentAiErrorType.CONCEPT_ERROR);
        assertThat(value.commonMistakes()).containsExactly("直接套公式");
    }

    @Test
    void rejectsExtraFieldsUnknownTypesEmptyArraysAndOversizedValues() {
        assertThatThrownBy(() -> parser.parse("""
                {"errorType":"MODEL_CREATED","errorReason":"x","correctThinking":"y","commonMistakes":["z"],"reviewSuggestions":["r"]}
                """)).isInstanceOf(StudentAiAnalysisParser.InvalidAnalysisException.class);
        assertThatThrownBy(() -> parser.parse("""
                {"errorType":"UNKNOWN","errorReason":"x","correctThinking":"y","commonMistakes":[],"reviewSuggestions":["r"],"prompt":"secret"}
                """)).isInstanceOf(StudentAiAnalysisParser.InvalidAnalysisException.class);
        String oversized = "x".repeat(1201);
        assertThatThrownBy(() -> parser.parse("{\"errorType\":\"UNKNOWN\",\"errorReason\":\"" + oversized
                + "\",\"correctThinking\":\"y\",\"commonMistakes\":[\"z\"],\"reviewSuggestions\":[\"r\"]}"))
                .isInstanceOf(StudentAiAnalysisParser.InvalidAnalysisException.class);
    }
}
