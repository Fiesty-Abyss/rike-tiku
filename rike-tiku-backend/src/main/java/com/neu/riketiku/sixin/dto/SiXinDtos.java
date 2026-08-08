package com.neu.riketiku.sixin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class SiXinDtos {
    private SiXinDtos() {
    }

    public record ConversationCreateRequest(
            @NotNull Long teachingAssignmentId,
            Long studentId) {
    }

    public record MessageSendRequest(
            @NotBlank(message = "消息内容不能为空")
            @Size(max = 1000, message = "消息内容最多1000个字符") String content) {
    }

    public record ContactResponse(
            Long teachingAssignmentId,
            Long studentId,
            String name,
            String className,
            String subjectName) {
    }

    public record ConversationResponse(
            Long id,
            Long teachingAssignmentId,
            Long studentId,
            String peerName,
            String className,
            String subjectName,
            String latestMessage,
            LocalDateTime latestMessageTime,
            long unreadCount,
            boolean canSend) {
    }

    public record MessageResponse(
            Long id,
            Long senderUserId,
            String senderName,
            String content,
            boolean mine,
            boolean read,
            LocalDateTime sentAt,
            LocalDateTime readAt) {
    }

    public record MessagePageResponse(
            ConversationResponse conversation,
            List<MessageResponse> messages) {
    }

    public record ReadResponse(long readCount) {
    }
}
