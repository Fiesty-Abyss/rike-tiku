package com.neu.riketiku.aixuesheng;

import static org.assertj.core.api.Assertions.assertThat;

import com.neu.riketiku.ai.AiProviderService;
import com.neu.riketiku.ai.provider.AiMessage;
import com.neu.riketiku.ai.provider.AiModelRequest;
import com.neu.riketiku.ai.provider.AiThinkingMode;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class RealDeepSeekSmokeTest extends AdminQuestionIntegrationTestSupport {
    private static final Set<String> SAFE_LOG_COLUMNS = Set.of(
            "id", "provider_dai_ma", "model_dai_ma", "yong_tu", "ye_wu_guan_lian",
            "shi_fou_cheng_gong", "hao_shi_hao_miao", "shu_ru_token", "shu_chu_token",
            "cuo_wu_dai_ma", "chuang_jian_shi_jian");
    @Autowired AiProviderService providerService;
    @Autowired StudentAiService studentAiService;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void realProviderProperties(DynamicPropertyRegistry registry) {
        registry.add("app.ai.enabled", () -> true);
        registry.add("app.ai.provider", () -> "deepseek");
        registry.add("app.ai.base-url", () -> "https://api.deepseek.com");
        registry.add("app.ai.model", () -> "deepseek-v4-flash");
        registry.add("app.ai.api-key", () -> System.getenv("RIKE_TIKU_AI_API_KEY"));
    }

    @Test
    void realTextAndStructuredStudentAnalysisSmoke() {
        requireSafeEnvironment();

        var textResult = providerService.generate(new AiModelRequest(
                List.of(new AiMessage("user", "请用一句简短中文说明：速度是描述物体运动快慢的物理量。")),
                "REAL_PROVIDER_SMOKE_TEXT", "smoke:anonymous", false, 256, AiThinkingMode.DISABLED));
        assertThat(textResult.content()).isNotBlank();
        assertThat(textResult.providerCode()).isEqualTo("deepseek");
        assertThat(textResult.modelCode()).isEqualTo("deepseek-v4-flash");
        LogSnapshot textLog = latestLog("REAL_PROVIDER_SMOKE_TEXT");
        assertThat(textLog.success()).isTrue();
        assertThat(textLog.provider()).isEqualTo(textResult.providerCode());
        assertThat(textLog.model()).isEqualTo(textResult.modelCode());
        assertThat(textLog.latency()).isPositive();
        if (textResult.usage().inputTokens() != null) {
            assertThat(textLog.inputTokens()).isEqualTo(textResult.usage().inputTokens());
        }
        if (textResult.usage().outputTokens() != null) {
            assertThat(textLog.outputTokens()).isEqualTo(textResult.usage().outputTokens());
        }

        long userId = anonymousWrongAnswerFact();
        long answerFactId = jdbc.queryForObject("SELECT MAX(id) FROM xue_sheng_da_ti", Long.class);
        StudentAiDtos.Analysis analysis = studentAiService.generateAnalysis(userId, answerFactId);
        assertThat(analysis.status()).isEqualTo("SUCCESS");
        assertThat(analysis.errorType()).isIn(
                "CONCEPT_ERROR", "CALCULATION_ERROR", "READING_ERROR", "REASONING_ERROR",
                "MEMORY_ERROR", "CARELESS_ERROR", "ANSWER_FORMAT_ERROR", "UNKNOWN");
        assertThat(analysis.errorReason()).isNotBlank();
        assertThat(analysis.correctThinking()).isNotBlank();
        assertThat(analysis.commonMistakes()).isNotEmpty();
        assertThat(analysis.reviewSuggestions()).isNotEmpty();
        assertThat(jdbc.queryForObject("SELECT biao_zhun_jie_xi_kuai_zhao FROM lian_xi_ti_mu WHERE id=(SELECT lian_xi_ti_mu_id FROM xue_sheng_da_ti WHERE id=?)",
                String.class, answerFactId)).isEqualTo("STANDARD：先根据牛顿第二定律判断加速度方向，再分析速度变化。");

        List<LogSnapshot> analysisLogs = logs("STUDENT_ERROR_ANALYSIS");
        assertThat(analysisLogs).hasSizeBetween(1, 2).allMatch(LogSnapshot::success);
        assertThat(analysisLogs).allSatisfy(log -> {
            assertThat(log.provider()).isEqualTo("deepseek");
            assertThat(log.model()).isEqualTo("deepseek-v4-flash");
            assertThat(log.latency()).isPositive();
        });
        assertThat(jdbc.queryForMap("""
                SELECT zhuang_tai,provider_dai_ma,model_dai_ma FROM ai_cuo_ti_fen_xi
                WHERE xue_sheng_da_ti_id=?
                """, answerFactId)).containsEntry("zhuang_tai", "SUCCESS")
                .containsEntry("provider_dai_ma", "deepseek")
                .containsEntry("model_dai_ma", "deepseek-v4-flash");
        Set<String> actualColumns = Set.copyOf(jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema=DATABASE() AND table_name='ai_diao_yong_ri_zhi'
                """, String.class));
        assertThat(actualColumns).isEqualTo(SAFE_LOG_COLUMNS);

        LogSnapshot analysisLog = analysisLogs.getLast();
        System.out.printf(
                "REAL_DEEPSEEK_SMOKE model=%s http=2xx text_latency_ms=%d text_input_tokens=%s text_output_tokens=%s "
                        + "json_latency_ms=%d json_input_tokens=%s json_output_tokens=%s json_calls=%d parser=PASS log_redaction=PASS%n",
                textResult.modelCode(), textLog.latency(), token(textLog.inputTokens()), token(textLog.outputTokens()),
                analysisLog.latency(), token(analysisLog.inputTokens()), token(analysisLog.outputTokens()),
                analysisLogs.size());
    }

    private void requireSafeEnvironment() {
        Assumptions.assumeTrue(present("RIKE_TIKU_AI_API_KEY"), "RIKE_TIKU_AI_API_KEY must be set in this process");
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("RIKE_TIKU_AI_ENABLED")),
                "RIKE_TIKU_AI_ENABLED must be true");
        Assumptions.assumeTrue("deepseek".equalsIgnoreCase(System.getenv("RIKE_TIKU_AI_PROVIDER")),
                "RIKE_TIKU_AI_PROVIDER must be deepseek");
        Assumptions.assumeTrue("deepseek-v4-flash".equalsIgnoreCase(System.getenv("RIKE_TIKU_AI_MODEL")),
                "RIKE_TIKU_AI_MODEL must be deepseek-v4-flash");
    }

    private boolean present(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }

    private long anonymousWrongAnswerFact() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao,shi_fou_shou_ci_deng_lu) VALUES (?,?,0)",
                "real_smoke_" + suffix, "x".repeat(60));
        long userId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO xue_sheng_dang_an(yong_hu_id,xue_hao,xing_ming,nian_ji) VALUES (?,?,?,?)",
                userId, "SMOKE" + suffix, "匿名测试学生", "高一");
        long studentId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO lian_xi_hui_hua(xue_sheng_id,ke_mu_id,zhuang_tai,ti_mu_shu,ti_jiao_shi_jian) VALUES (?,1,'SUBMITTED',1,CURRENT_TIMESTAMP(3))",
                studentId);
        long sessionId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("""
                INSERT INTO lian_xi_ti_mu(lian_xi_hui_hua_id,ti_mu_id,ti_mu_shun_xu,fen_zhi,ti_mu_lei_xing,nan_du_kuai_zhao,
                  ti_gan_kuai_zhao,xuan_xiang_kuai_zhao,zheng_que_da_an_kuai_zhao,biao_zhun_jie_xi_kuai_zhao,zhi_shi_dian_kuai_zhao)
                VALUES (?,1,1,1,'SINGLE_CHOICE',1,'物体所受合力与速度同向时，速度如何变化？',
                  JSON_ARRAY(JSON_OBJECT('label','A','content','增大'),JSON_OBJECT('label','B','content','减小')),
                  JSON_OBJECT('schemaVersion',1,'type','SINGLE_CHOICE','optionLabels',JSON_ARRAY('A')),
                  'STANDARD：先根据牛顿第二定律判断加速度方向，再分析速度变化。',
                  JSON_ARRAY(JSON_OBJECT('id',1,'name','牛顿第二定律')))
                """, sessionId);
        long practiceQuestionId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("""
                INSERT INTO xue_sheng_da_ti(lian_xi_ti_mu_id,xue_sheng_id,xue_sheng_da_an,shi_fou_zheng_que,de_fen,ti_jiao_shi_jian)
                VALUES (?,?,JSON_QUOTE('B'),0,0,CURRENT_TIMESTAMP(3))
                """, practiceQuestionId, studentId);
        return userId;
    }

    private LogSnapshot latestLog(String purpose) {
        return logs(purpose).getLast();
    }

    private List<LogSnapshot> logs(String purpose) {
        return jdbc.query("""
                SELECT provider_dai_ma,model_dai_ma,shi_fou_cheng_gong,hao_shi_hao_miao,shu_ru_token,shu_chu_token
                FROM ai_diao_yong_ri_zhi WHERE yong_tu=? ORDER BY id
                """, (rs, row) -> new LogSnapshot(rs.getString(1), rs.getString(2), rs.getBoolean(3), rs.getLong(4),
                rs.getObject(5, Integer.class), rs.getObject(6, Integer.class)), purpose);
    }

    private String token(Integer value) { return value == null ? "not-returned" : value.toString(); }

    private record LogSnapshot(String provider, String model, boolean success, long latency,
                               Integer inputTokens, Integer outputTokens) { }
}
