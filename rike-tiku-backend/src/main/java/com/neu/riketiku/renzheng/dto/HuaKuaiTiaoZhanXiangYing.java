package com.neu.riketiku.renzheng.dto;

import java.time.Instant;

public record HuaKuaiTiaoZhanXiangYing(
        String challengeId,
        int canvasWidth,
        int targetWidth,
        int targetDisplayOffset,
        Instant expiresAt) {
}
