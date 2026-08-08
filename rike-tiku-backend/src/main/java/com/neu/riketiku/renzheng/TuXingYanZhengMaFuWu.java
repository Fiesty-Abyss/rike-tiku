package com.neu.riketiku.renzheng;

import com.neu.riketiku.renzheng.dto.TuXingYanZhengMaTiaoZhanXiangYing;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 本科毕设演示用的一次性图形验证码；仅在内存中保存验证码和值的有效期。 */
@Service
public class TuXingYanZhengMaFuWu {
    private static final String CHARACTERS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 4;
    private static final int IMAGE_WIDTH = 120;
    private static final int IMAGE_HEIGHT = 40;
    private static final Duration TTL = Duration.ofMinutes(2);

    private final ConcurrentHashMap<String, Challenge> challenges = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final boolean exposeTestCode;

    public TuXingYanZhengMaFuWu(
            @Value("${app.auth.captcha.expose-test-code:false}") boolean exposeTestCode) {
        this.exposeTestCode = exposeTestCode;
    }

    public TuXingYanZhengMaTiaoZhanXiangYing create(String previousChallengeId) {
        invalidate(previousChallengeId);
        cleanExpired();

        byte[] idBytes = new byte[18];
        random.nextBytes(idBytes);
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(idBytes);
        String code = randomCode();
        Instant expiresAt = Instant.now().plus(TTL);
        challenges.put(id, new Challenge(code, expiresAt));

        return new TuXingYanZhengMaTiaoZhanXiangYing(
                id,
                renderDataUrl(code),
                expiresAt,
                exposeTestCode ? code : null);
    }

    public void verify(String challengeId, String captchaCode) {
        if (challengeId == null || challengeId.isBlank()
                || captchaCode == null || captchaCode.isBlank()) {
            throw error("CAPTCHA_CHALLENGE_REQUIRED", "请先输入验证码。", HttpStatus.BAD_REQUEST);
        }

        Challenge challenge = challenges.remove(challengeId);
        if (challenge == null) {
            throw error("CAPTCHA_CHALLENGE_REUSED", "验证码已经使用，请重新获取。", HttpStatus.BAD_REQUEST);
        }
        if (challenge.expiresAt().isBefore(Instant.now())) {
            throw error("CAPTCHA_CHALLENGE_EXPIRED", "验证码已过期，请重新输入。", HttpStatus.BAD_REQUEST);
        }
        if (!challenge.code().equals(captchaCode.trim().toUpperCase(Locale.ROOT))) {
            throw error("CAPTCHA_INCORRECT", "验证码不正确，请重新输入。", HttpStatus.BAD_REQUEST);
        }
    }

    void expireForTest(String challengeId) {
        challenges.computeIfPresent(
                challengeId,
                (id, challenge) -> new Challenge(challenge.code(), Instant.now().minusSeconds(1)));
    }

    private void invalidate(String challengeId) {
        if (challengeId != null && !challengeId.isBlank()) {
            challenges.remove(challengeId);
        }
    }

    private String randomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }

    private String renderDataUrl(String code) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(246, 249, 252));
            graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
            drawInterference(graphics);
            drawCharacters(graphics, code);
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("生成验证码图片失败", exception);
        }
    }

    private void drawInterference(Graphics2D graphics) {
        graphics.setStroke(new BasicStroke(1.1f));
        for (int i = 0; i < 4; i++) {
            graphics.setColor(randomLightColor());
            graphics.drawLine(
                    random.nextInt(IMAGE_WIDTH), random.nextInt(IMAGE_HEIGHT),
                    random.nextInt(IMAGE_WIDTH), random.nextInt(IMAGE_HEIGHT));
        }
        for (int i = 0; i < 28; i++) {
            graphics.setColor(randomLightColor());
            int x = random.nextInt(IMAGE_WIDTH);
            int y = random.nextInt(IMAGE_HEIGHT);
            graphics.fillOval(x, y, 2, 2);
        }
    }

    private void drawCharacters(Graphics2D graphics, String code) {
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        for (int i = 0; i < code.length(); i++) {
            AffineTransform original = graphics.getTransform();
            double angle = Math.toRadians(random.nextInt(17) - 8);
            int x = 12 + i * 27;
            int y = 28 + random.nextInt(5) - 2;
            graphics.rotate(angle, x + 8, y - 10);
            graphics.setColor(randomDarkColor());
            graphics.drawString(String.valueOf(code.charAt(i)), x, y);
            graphics.setTransform(original);
        }
    }

    private Color randomLightColor() {
        return new Color(145 + random.nextInt(70), 145 + random.nextInt(70), 145 + random.nextInt(70));
    }

    private Color randomDarkColor() {
        return new Color(25 + random.nextInt(65), 45 + random.nextInt(65), 75 + random.nextInt(75));
    }

    private void cleanExpired() {
        Instant cleanupBefore = Instant.now().minus(TTL);
        challenges.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(cleanupBefore));
    }

    private RenZhengYeWuYiChang error(String code, String message, HttpStatus status) {
        return new RenZhengYeWuYiChang(code, message, status);
    }

    private record Challenge(String code, Instant expiresAt) {
    }
}
