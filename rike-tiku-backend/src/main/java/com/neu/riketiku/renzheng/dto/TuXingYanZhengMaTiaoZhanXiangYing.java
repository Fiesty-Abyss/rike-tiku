package com.neu.riketiku.renzheng.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

public record TuXingYanZhengMaTiaoZhanXiangYing(
        String challengeId,
        String image,
        Instant expiresAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) String testCode) {
}
