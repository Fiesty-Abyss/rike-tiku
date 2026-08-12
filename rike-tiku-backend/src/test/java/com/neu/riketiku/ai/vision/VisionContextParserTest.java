package com.neu.riketiku.ai.vision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.riketiku.ai.provider.AiProviderErrorType;
import org.junit.jupiter.api.Test;

class VisionContextParserTest {
    private static final String JSON = """
            {"diagramType":"CIRCUIT","summary":"电路图","visibleText":[],"relations":[],"uncertainty":[]}
            """;
    private final VisionContextParser parser = new VisionContextParser(new ObjectMapper());

    @Test
    void acceptsACompleteJsonCodeFenceFromVisionTextOutput() {
        assertThat(parser.parse("```json\n" + JSON + "\n```").summary()).isEqualTo("电路图");
    }

    @Test
    void rejectsAnyTextOutsideTheSingleJsonFence() {
        assertThatThrownBy(() -> parser.parse("结果如下：\n```json\n" + JSON + "\n```"))
                .isInstanceOfSatisfying(AiVisionException.class,
                        error -> assertThat(error.errorType()).isEqualTo(AiProviderErrorType.INVALID_RESPONSE));
    }

    @Test
    void keepsStrictFieldsAfterRemovingTheFence() {
        String extra = JSON.replace("}", ",\"answer\":\"A\"}");
        assertThatThrownBy(() -> parser.parse("```json\n" + extra + "\n```"))
                .isInstanceOfSatisfying(AiVisionException.class,
                        error -> assertThat(error.errorType()).isEqualTo(AiProviderErrorType.INVALID_RESPONSE));
    }
}
