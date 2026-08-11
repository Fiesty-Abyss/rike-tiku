package com.neu.riketiku.ai.log;

import static org.assertj.core.api.Assertions.assertThat;

import com.neu.riketiku.ai.provider.AiModelRequest;
import com.neu.riketiku.ai.provider.AiModelResult;
import com.neu.riketiku.ai.provider.AiProviderErrorType;
import com.neu.riketiku.ai.provider.AiTokenUsage;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class JdbcAiCallLogWriterIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private JdbcAiCallLogWriter writer;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void v12PersistsSuccessAndFailureMetadataWithoutPromptOrSecrets() {
        AiModelRequest sensitive = AiModelRequest.text("student-help", "SECRET PROMPT AND JWT");
        writer.success(sensitive, new AiModelResult("deepseek", "deepseek-v4-flash", "SECRET OUTPUT",
                new AiTokenUsage(12, 8, 20), "stop"), 45);
        writer.failure(new AiModelRequest(sensitive.messages(), "student-help", "question:42", false),
                "deepseek", "deepseek-v4-flash", AiProviderErrorType.TIMEOUT, 101);

        assertThat(jdbc.queryForObject("SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success=1",
                Integer.class)).isEqualTo(13);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_diao_yong_ri_zhi", Integer.class)).isEqualTo(2);
        Map<String, Object> success = jdbc.queryForMap("SELECT * FROM ai_diao_yong_ri_zhi WHERE shi_fou_cheng_gong=1");
        assertThat(success).containsEntry("provider_dai_ma", "deepseek")
                .containsEntry("model_dai_ma", "deepseek-v4-flash")
                .containsEntry("yong_tu", "student-help")
                .containsEntry("hao_shi_hao_miao", 45L)
                .containsEntry("shu_ru_token", 12)
                .containsEntry("shu_chu_token", 8);
        assertThat(success.toString()).doesNotContain("SECRET", "JWT", "PROMPT", "OUTPUT");

        Map<String, Object> failure = jdbc.queryForMap("SELECT * FROM ai_diao_yong_ri_zhi WHERE shi_fou_cheng_gong=0");
        assertThat(failure).containsEntry("ye_wu_guan_lian", "question:42")
                .containsEntry("cuo_wu_dai_ma", "TIMEOUT");

        assertThat(jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema=DATABASE() AND table_name='ai_diao_yong_ri_zhi'
                """, String.class)).noneMatch(name -> name.contains("prompt") || name.contains("key")
                        || name.contains("nei_rong") || name.contains("token_zhi"));
    }
}
