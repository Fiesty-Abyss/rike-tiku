package com.neu.riketiku.ai.config;

import java.time.Duration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AiRuntimeConfigurationService {
    private final JdbcTemplate jdbc;
    private final AiProviderProperties textDefaults;
    private final VisionProviderProperties visionDefaults;

    public AiRuntimeConfigurationService(JdbcTemplate jdbc, AiProviderProperties textDefaults,
                                         VisionProviderProperties visionDefaults) {
        this.jdbc = jdbc;
        this.textDefaults = textDefaults;
        this.visionDefaults = visionDefaults;
    }

    public AiRuntimeConfig text() {
        return database("TEXT", "DEEPSEEK");
    }

    public AiRuntimeConfig vision() {
        return database("VISION", "GLM");
    }

    public void invalidate() {
        // Deliberately no shared cache: one indexed lookup per business call keeps local demo changes immediate.
    }

    private AiRuntimeConfig database(String usage, String provider) {
        return jdbc.query("""
                SELECT id,provider_dai_ma,mo_xing_dai_ma,api_di_zhi,api_mi_yao,yong_tu,
                       shi_fou_qi_yong,zui_da_token,chao_shi_hao_miao,retry_count
                FROM ai_mo_xing_pei_zhi
                WHERE yong_tu=? AND provider_dai_ma=? AND shi_fou_qi_yong=1
                ORDER BY shi_fou_mo_ren DESC,id DESC LIMIT 1
                """, (rs, row) -> new AiRuntimeConfig(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getBoolean(7), rs.getInt(8),
                Duration.ofMillis(rs.getInt(9)), Math.min(1, rs.getInt(10)), true), usage, provider)
                .stream().findFirst().orElseGet(() -> fallback(usage));
    }

    private AiRuntimeConfig fallback(String usage) {
        if ("TEXT".equals(usage)) {
            return new AiRuntimeConfig(null, textDefaults.getProvider(), textDefaults.getModel(),
                    textDefaults.getBaseUrl(), textDefaults.getApiKey(), usage, textDefaults.isEnabled(), 1200,
                    safe(textDefaults.getRequestTimeout()), textDefaults.getRetryCount(), false);
        }
        return new AiRuntimeConfig(null, visionDefaults.getProvider(), visionDefaults.getModel(),
                visionDefaults.getBaseUrl(), visionDefaults.getApiKey(), usage, visionDefaults.isEnabled(),
                visionDefaults.getMaxTokens(), safe(visionDefaults.getRequestTimeout()),
                visionDefaults.getRetryCount(), false);
    }

    private Duration safe(Duration value) {
        return value == null || value.isZero() || value.isNegative() ? Duration.ofSeconds(30) : value;
    }
}
