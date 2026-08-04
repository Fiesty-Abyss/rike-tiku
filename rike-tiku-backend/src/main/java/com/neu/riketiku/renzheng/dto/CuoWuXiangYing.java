package com.neu.riketiku.renzheng.dto;

import java.time.Instant;

public record CuoWuXiangYing(
        String code,
        String message,
        Instant timestamp) {
}
