package com.neu.riketiku.ai.provider;

import java.util.Locale;
import java.util.Objects;

public record AiMessage(String role, String content) {
    public AiMessage {
        role = Objects.requireNonNull(role, "role").trim().toLowerCase(Locale.ROOT);
        content = Objects.requireNonNull(content, "content");
        if (!role.equals("system") && !role.equals("user") && !role.equals("assistant")) {
            throw new IllegalArgumentException("AI message role must be system, user, or assistant");
        }
        if (content.isBlank()) throw new IllegalArgumentException("AI message content must not be blank");
    }
}
