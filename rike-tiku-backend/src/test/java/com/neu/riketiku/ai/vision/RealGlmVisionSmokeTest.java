package com.neu.riketiku.ai.vision;

import static org.assertj.core.api.Assertions.assertThat;

import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RIKE_TIKU_GLM_API_KEY", matches = ".+")
class RealGlmVisionSmokeTest extends AdminQuestionIntegrationTestSupport {
    private static final String API_KEY_ENV = "RIKE_TIKU_GLM_API_KEY";

    @DynamicPropertySource
    static void visionProperties(DynamicPropertyRegistry registry) {
        registry.add("app.ai.vision.enabled", () -> true);
        registry.add("app.ai.vision.provider", () -> "glm");
        registry.add("app.ai.vision.base-url", () -> "https://open.bigmodel.cn/api/paas/v4");
        registry.add("app.ai.vision.model", () -> "glm-4.6v-flash");
        registry.add("app.ai.vision.api-key", () -> requiredKey());
        registry.add("app.ai.vision.request-timeout", () -> "60s");
        registry.add("app.ai.vision.retry-count", () -> 1);
        registry.add("app.ai.vision.max-tokens", () -> 1000);
    }

    @Autowired private AiVisionService visionService;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void callsRealGlmAndPersistsOnlySafeMetadata() throws Exception {
        byte[] image = privacyFreeCircuitImage();
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(image));

        AiVisionResult result = visionService.analyze(new AiVisionRequest(1,
                List.of(new AiVisionRequest.Image(hash, "image/png", image)),
                "QUESTION_VISION_CONTEXT"));

        assertThat(result.provider()).isEqualTo("glm");
        assertThat(result.model()).isEqualTo("glm-4.6v-flash");
        assertThat(result.context().diagramType()).isNotBlank();
        assertThat(result.context().summary()).isNotBlank();
        assertThat(controlledLength(result.context())).isLessThanOrEqualTo(1500);

        SafeLogRow log = jdbc.queryForObject("""
                SELECT provider_dai_ma,model_dai_ma,yong_tu,shi_fou_cheng_gong,
                       hao_shi_hao_miao,shu_ru_token,shu_chu_token
                FROM ai_diao_yong_ri_zhi ORDER BY id DESC LIMIT 1
                """, (rs, row) -> new SafeLogRow(rs.getString(1), rs.getString(2), rs.getString(3),
                rs.getBoolean(4), rs.getLong(5), nullableInt(rs, 6), nullableInt(rs, 7)));
        assertThat(log).isNotNull();
        assertThat(log.provider()).isEqualTo("glm");
        assertThat(log.model()).isEqualTo("glm-4.6v-flash");
        assertThat(log.purpose()).isEqualTo("QUESTION_VISION_CONTEXT");
        assertThat(log.success()).isTrue();
        assertThat(log.latencyMillis()).isPositive();

        Set<String> columns = Set.copyOf(jdbc.queryForList("""
                SELECT COLUMN_NAME FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ai_diao_yong_ri_zhi'
                """, String.class));
        assertThat(columns).containsExactlyInAnyOrder("id", "provider_dai_ma", "model_dai_ma", "yong_tu",
                "ye_wu_guan_lian", "shi_fou_cheng_gong", "hao_shi_hao_miao", "shu_ru_token",
                "shu_chu_token", "cuo_wu_dai_ma", "chuang_jian_shi_jian");
        assertThat(columns).noneMatch(name -> name.contains("prompt") || name.contains("output")
                || name.contains("api") || name.contains("mi_yao") || name.contains("base64"));

        System.out.printf("REAL_GLM_VISION_SMOKE model=%s http=2xx latency_ms=%d input_tokens=%s output_tokens=%s parser=PASS log_redaction=PASS%n",
                result.model(), log.latencyMillis(), safeToken(log.inputTokens()), safeToken(log.outputTokens()));
    }

    private static int controlledLength(AiVisionContext context) {
        return context.diagramType().length() + context.summary().length()
                + context.visibleText().stream().mapToInt(String::length).sum()
                + context.relations().stream().mapToInt(String::length).sum()
                + context.uncertainty().stream().mapToInt(String::length).sum();
    }

    private static Integer nullableInt(java.sql.ResultSet rs, int column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static String safeToken(Integer value) {
        return value == null ? "unavailable" : value.toString();
    }

    private static String requiredKey() {
        String value = System.getenv(API_KEY_ENV);
        if (value == null || value.isBlank()) throw new IllegalStateException(API_KEY_ENV + " is required");
        return value;
    }

    private static byte[] privacyFreeCircuitImage() throws Exception {
        BufferedImage image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(new Color(24, 52, 78));
        graphics.setStroke(new BasicStroke(6));
        graphics.drawRect(100, 80, 440, 200);
        graphics.drawLine(280, 80, 280, 55);
        graphics.drawLine(330, 80, 330, 45);
        graphics.drawLine(280, 55, 330, 55);
        graphics.drawOval(430, 165, 70, 70);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        graphics.drawString("S", 190, 70);
        graphics.drawString("L", 453, 210);
        graphics.drawString("6 V", 285, 335);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private record SafeLogRow(String provider, String model, String purpose, boolean success,
                              long latencyMillis, Integer inputTokens, Integer outputTokens) { }
}
