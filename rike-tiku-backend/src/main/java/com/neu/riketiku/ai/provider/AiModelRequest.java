package com.neu.riketiku.ai.provider;

import java.util.List;
import java.util.Objects;

public record AiModelRequest(List<AiMessage> messages, String purpose, String businessReference, boolean jsonOutput,
                             Integer maxOutputTokens, AiThinkingMode thinkingMode, String reasoningEffort) {
    public AiModelRequest {
        messages = List.copyOf(Objects.requireNonNull(messages, "messages"));
        if (messages.isEmpty()) throw new IllegalArgumentException("AI request must contain at least one message");
        purpose = Objects.requireNonNull(purpose, "purpose").trim();
        if (purpose.isEmpty()) throw new IllegalArgumentException("AI purpose must not be blank");
        businessReference = businessReference == null || businessReference.isBlank() ? null : businessReference.trim();
        if (maxOutputTokens != null && (maxOutputTokens < 1 || maxOutputTokens > 8192)) {
            throw new IllegalArgumentException("AI max output tokens must be between 1 and 8192");
        }
        thinkingMode = thinkingMode == null ? AiThinkingMode.DEFAULT : thinkingMode;
        if (reasoningEffort != null && !reasoningEffort.equals("high") && !reasoningEffort.equals("max")) {
            throw new IllegalArgumentException("AI reasoning effort must be high or max");
        }
    }

    public AiModelRequest(List<AiMessage> messages, String purpose, String businessReference, boolean jsonOutput,
                          Integer maxOutputTokens, AiThinkingMode thinkingMode) {
        this(messages, purpose, businessReference, jsonOutput, maxOutputTokens, thinkingMode, null);
    }

    public AiModelRequest(List<AiMessage> messages, String purpose, String businessReference, boolean jsonOutput) {
        this(messages, purpose, businessReference, jsonOutput, null, AiThinkingMode.DEFAULT, null);
    }

    public static AiModelRequest text(String purpose, String prompt) {
        return new AiModelRequest(List.of(new AiMessage("user", prompt)), purpose, null, false,
                null, AiThinkingMode.DEFAULT, null);
    }
}
