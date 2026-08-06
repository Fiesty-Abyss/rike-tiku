package com.neu.riketiku.jiaoshi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JiaoShiChuangJianQingQiu(
        @NotBlank @Size(max = 64) String employeeNumber,
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Size(max = 64) String username,
        @Size(max = 128) String displayPosition,
        @Size(min = 8, max = 64) String initialPassword,
        @NotBlank @Pattern(regexp = "ENABLED|DISABLED|LOCKED") String accountStatus) {
}
