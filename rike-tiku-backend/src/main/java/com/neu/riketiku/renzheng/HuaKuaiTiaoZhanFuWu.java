package com.neu.riketiku.renzheng;

import com.neu.riketiku.renzheng.dto.HuaKuaiTiaoZhanXiangYing;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Lightweight local demonstration challenge. It deliberately stores no login data. */
@Service
public class HuaKuaiTiaoZhanFuWu {
    private static final int CANVAS_WIDTH = 320;
    private static final int TARGET_WIDTH = 44;
    private static final int TOLERANCE = 6;
    private static final Duration TTL = Duration.ofMinutes(2);
    private final ConcurrentHashMap<String, Challenge> challenges = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public HuaKuaiTiaoZhanXiangYing create() {
        cleanExpired();
        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        int offset = 28 + random.nextInt(CANVAS_WIDTH - TARGET_WIDTH - 56);
        Instant expiresAt = Instant.now().plus(TTL);
        challenges.put(id, new Challenge(offset, expiresAt));
        return new HuaKuaiTiaoZhanXiangYing(id, CANVAS_WIDTH, TARGET_WIDTH, offset, expiresAt);
    }

    public void verify(String challengeId, Integer sliderOffset) {
        if (challengeId == null || challengeId.isBlank() || sliderOffset == null) {
            throw error("SLIDER_CHALLENGE_REQUIRED", "请先完成滑块验证", HttpStatus.BAD_REQUEST);
        }
        Challenge challenge = challenges.remove(challengeId);
        if (challenge == null) {
            throw error("SLIDER_CHALLENGE_REUSED", "滑块挑战已使用或不存在，请刷新后重试", HttpStatus.BAD_REQUEST);
        }
        if (challenge.expiresAt().isBefore(Instant.now())) {
            throw error("SLIDER_CHALLENGE_EXPIRED", "滑块挑战已过期，请刷新后重试", HttpStatus.BAD_REQUEST);
        }
        if (Math.abs(challenge.offset() - sliderOffset) > TOLERANCE) {
            throw error("SLIDER_CHALLENGE_INVALID", "滑块位置不正确，请刷新后重试", HttpStatus.BAD_REQUEST);
        }
    }

    private void cleanExpired() {
        Instant now = Instant.now();
        challenges.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private RenZhengYeWuYiChang error(String code, String message, HttpStatus status) {
        return new RenZhengYeWuYiChang(code, message, status);
    }

    private record Challenge(int offset, Instant expiresAt) {
    }
}
