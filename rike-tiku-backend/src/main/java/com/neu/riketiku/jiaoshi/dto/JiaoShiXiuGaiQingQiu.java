package com.neu.riketiku.jiaoshi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JiaoShiXiuGaiQingQiu(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 128) String displayPosition,
        @NotBlank @Pattern(regexp = "ENABLED|DISABLED|LOCKED") String accountStatus,
        @NotBlank @Pattern(regexp = "ACTIVE|DISABLED") String profileStatus) {
}
