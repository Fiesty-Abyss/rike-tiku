package com.neu.riketiku.ai.config;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AiRuntimeConfigurationService {
    private static final Set<String> STUDENT_TEXT_MODELS = Set.of("deepseek-v4-flash", "deepseek-v4-pro");
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

    public AiRuntimeConfig search() { return database("SEARCH", "GLM"); }

    public boolean searchAvailable() {
        AiRuntimeConfig value=search();
        return value.enabled() && value.apiKey()!=null && !value.apiKey().isBlank();
    }

    public AiRuntimeConfig text(Long safeConfigId) {
        if (safeConfigId == null) return text();
        return safeTextOptions().stream().filter(option -> option.id().equals(safeConfigId) && option.available())
                .findFirst().map(option -> databaseById(option.id())).orElseThrow(() ->
                        new IllegalArgumentException("Student AI model option is unavailable"));
    }

    public List<SafeTextOption> safeTextOptions() {
        List<SafeTextOption> configured = jdbc.query("""
                SELECT id,mo_xing_dai_ma,shi_fou_qi_yong,shi_fou_mo_ren,
                       api_mi_yao IS NOT NULL AND TRIM(api_mi_yao)<>'' AND api_di_zhi IS NOT NULL AND TRIM(api_di_zhi)<>''
                FROM ai_mo_xing_pei_zhi
                WHERE yong_tu='TEXT' AND provider_dai_ma='DEEPSEEK'
                  AND mo_xing_dai_ma IN ('deepseek-v4-flash','deepseek-v4-pro')
                ORDER BY shi_fou_mo_ren DESC,id
                """, (rs, row) -> new SafeTextOption(rs.getLong(1), displayName(rs.getString(2)), rs.getString(2),
                rs.getBoolean(3) && rs.getBoolean(5), rs.getBoolean(4),
                rs.getString(2).endsWith("pro") ? List.of("深度推理", "复杂题") : List.of("快速", "日常答疑")));
        return configured.stream().filter(option -> STUDENT_TEXT_MODELS.contains(option.modelCode())).toList();
    }

    private AiRuntimeConfig databaseById(long id) {
        return jdbc.query("""
                SELECT id,provider_dai_ma,mo_xing_dai_ma,api_di_zhi,api_mi_yao,yong_tu,
                       shi_fou_qi_yong,zui_da_token,chao_shi_hao_miao,retry_count
                FROM ai_mo_xing_pei_zhi WHERE id=? AND yong_tu='TEXT' AND provider_dai_ma='DEEPSEEK'
                  AND shi_fou_qi_yong=1 AND mo_xing_dai_ma IN ('deepseek-v4-flash','deepseek-v4-pro')
                """, (rs, row) -> row(rs), id).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Student AI model option is unavailable"));
    }

    public void invalidate() {
        // Deliberately no shared cache: one indexed lookup per business call makes configuration changes immediate.
    }

    private AiRuntimeConfig database(String usage, String provider) {
        return jdbc.query("""
                SELECT id,provider_dai_ma,mo_xing_dai_ma,api_di_zhi,api_mi_yao,yong_tu,
                       shi_fou_qi_yong,zui_da_token,chao_shi_hao_miao,retry_count
                FROM ai_mo_xing_pei_zhi
                WHERE yong_tu=? AND provider_dai_ma=? AND shi_fou_qi_yong=1
                ORDER BY shi_fou_mo_ren DESC,id DESC LIMIT 1
                """, (rs, row) -> row(rs), usage, provider)
                .stream().findFirst().orElseGet(() -> fallback(usage));
    }

    private AiRuntimeConfig row(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AiRuntimeConfig(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getString(6), rs.getBoolean(7), rs.getInt(8), Duration.ofMillis(rs.getInt(9)),
                Math.min(1, rs.getInt(10)), true);
    }

    private String displayName(String model) { return model.endsWith("pro") ? "DeepSeek V4 Pro" : "DeepSeek V4 Flash"; }

    public record SafeTextOption(Long id, String displayName, String modelCode, boolean available,
                                 boolean defaultOption, List<String> capabilityTags) { }

    private AiRuntimeConfig fallback(String usage) {
        if ("TEXT".equals(usage)) {
            return new AiRuntimeConfig(null, textDefaults.getProvider(), textDefaults.getModel(),
                    textDefaults.getBaseUrl(), textDefaults.getApiKey(), usage, textDefaults.isEnabled(), 1200,
                    safe(textDefaults.getRequestTimeout()), textDefaults.getRetryCount(), false);
        }
        if ("SEARCH".equals(usage)) return new AiRuntimeConfig(null, "GLM", "search_pro",
                "https://open.bigmodel.cn/api/paas/v4", null, usage, false, 5, Duration.ofSeconds(8), 1, false);
        return new AiRuntimeConfig(null, visionDefaults.getProvider(), visionDefaults.getModel(),
                visionDefaults.getBaseUrl(), visionDefaults.getApiKey(), usage, visionDefaults.isEnabled(),
                visionDefaults.getMaxTokens(), safe(visionDefaults.getRequestTimeout()),
                visionDefaults.getRetryCount(), false);
    }

    private Duration safe(Duration value) {
        return value == null || value.isZero() || value.isNegative() ? Duration.ofSeconds(30) : value;
    }
}
