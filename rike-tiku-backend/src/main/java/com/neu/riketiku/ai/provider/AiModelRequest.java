package com.neu.riketiku.ai.provider;

import java.util.List;
import java.util.Objects;

public record AiModelRequest(List<AiMessage> messages, String purpose, String businessReference, boolean jsonOutput) {
    public AiModelRequest {
        messages = List.copyOf(Objects.requireNonNull(messages, "messages"));
        if (messages.isEmpty()) throw new IllegalArgumentException("AI request must contain at least one message");
        purpose = Objects.requireNonNull(purpose, "purpose").trim();
        if (purpose.isEmpty()) throw new IllegalArgumentException("AI purpose must not be blank");
        businessReference = businessReference == null || businessReference.isBlank() ? null : businessReference.trim();
    }

    public static AiModelRequest text(String purpose, String prompt) {
        return new AiModelRequest(List.of(new AiMessage("user", prompt)), purpose, null, false);
    }
}
