package com.neu.riketiku.ai.log;

import com.neu.riketiku.ai.provider.AiModelRequest;
import com.neu.riketiku.ai.provider.AiModelResult;
import com.neu.riketiku.ai.provider.AiProviderErrorType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JdbcAiCallLogWriter implements AiCallLogWriter {
    private final JdbcTemplate jdbc;

    public JdbcAiCallLogWriter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void success(AiModelRequest request, AiModelResult result, long latencyMillis) {
        insert(result.providerCode(), result.modelCode(), request, true, latencyMillis,
                result.usage().inputTokens(), result.usage().outputTokens(), null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failure(AiModelRequest request, String provider, String model,
                        AiProviderErrorType errorType, long latencyMillis) {
        insert(provider, model, request, false, latencyMillis, null, null, errorType.name());
    }

    private void insert(String provider, String model, AiModelRequest request, boolean success, long latencyMillis,
                        Integer inputTokens, Integer outputTokens, String errorCode) {
        jdbc.update("""
                INSERT INTO ai_diao_yong_ri_zhi
                    (provider_dai_ma,model_dai_ma,yong_tu,ye_wu_guan_lian,shi_fou_cheng_gong,
                     hao_shi_hao_miao,shu_ru_token,shu_chu_token,cuo_wu_dai_ma)
                VALUES (?,?,?,?,?,?,?,?,?)
                """, safe(provider, 64), safe(model, 128), safe(request.purpose(), 96),
                safe(request.businessReference(), 128), success, Math.max(0, latencyMillis),
                inputTokens, outputTokens, safe(errorCode, 64));
    }

    private String safe(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }
}
