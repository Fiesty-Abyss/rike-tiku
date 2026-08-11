package com.neu.riketiku.aixuesheng;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class StudentAiDtos {
    private StudentAiDtos() { }

    public record Analysis(
            Long answerFactId,
            String status,
            String errorType,
            String errorReason,
            String correctThinking,
            List<String> commonMistakes,
            List<String> reviewSuggestions,
            boolean cached,
            String errorCode,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) { }

    public record CreateConversationRequest(@NotNull Long answerFactId) { }

    public record SendMessageRequest(
            @NotBlank(message = "追问内容不能为空")
            @Size(max = 500, message = "单条追问最多500字") String content) { }

    public record Message(Long id, String role, String content, int sequence, LocalDateTime createdAt) { }

    public record Conversation(
            Long id,
            Long answerFactId,
            Long questionId,
            String status,
            int usedRounds,
            int maxRounds,
            int remainingRounds,
            List<Message> messages) { }
}
