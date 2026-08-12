package com.neu.riketiku.zhanghao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class PasswordRecoveryDtos {
    private PasswordRecoveryDtos() { }
    public record Request(@NotBlank @Size(max=64) String username, @NotBlank String challengeId,
                          @NotBlank String captchaCode) { }
    public record Accepted(String message) { }
    public record Item(Long id,Long userId,String username,String name,String role,String status,
                       LocalDateTime requestedAt,LocalDateTime handledAt,String result) { }
    public record Page(List<Item> records,long pendingCount) { }
    public record Reject(@NotBlank @Size(max=500) String reason) { }
    public record Resolution(Long id,String status) { }
}
