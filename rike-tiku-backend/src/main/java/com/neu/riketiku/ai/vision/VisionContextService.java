package com.neu.riketiku.ai.vision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neu.riketiku.ai.config.AiRuntimeConfig;
import com.neu.riketiku.ai.config.AiRuntimeConfigurationService;
import com.neu.riketiku.ai.provider.AiProviderErrorType;
import com.neu.riketiku.tiku.fujian.QuestionAttachmentStorage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class VisionContextService {
    public static final String PROMPT_VERSION = "vision-context-v1";
    private static final long MAX_IMAGE_BYTES = 3L * 1024 * 1024;
    private static final long MAX_TOTAL_BYTES = 6L * 1024 * 1024;
    private final JdbcTemplate jdbc;
    private final QuestionAttachmentStorage storage;
    private final AiRuntimeConfigurationService configurations;
    private final AiVisionService vision;
    private final ObjectMapper mapper = new ObjectMapper();

    public VisionContextService(JdbcTemplate jdbc, QuestionAttachmentStorage storage,
                                AiRuntimeConfigurationService configurations, AiVisionService vision) {
        this.jdbc=jdbc; this.storage=storage; this.configurations=configurations; this.vision=vision;
    }

    public Resolution resolve(long questionId, boolean required) {
        List<Attachment> attachments = attachments(questionId);
        if (attachments.isEmpty()) return new Resolution(null, false, true, 0, false);
        Map<String, Attachment> unique = new LinkedHashMap<>();
        attachments.stream().sorted(Comparator.comparing(Attachment::hash)).forEach(item -> unique.putIfAbsent(item.hash(), item));
        if (unique.size() > 2) return failed(required, AiProviderErrorType.CONFIGURATION_ERROR, "VISION_IMAGE_LIMIT_EXCEEDED");
        List<AiVisionRequest.Image> images = new ArrayList<>();
        long total=0;
        for (Attachment item : unique.values()) {
            QuestionAttachmentStorage.StoredImage stored=storage.read(item.path(),item.hash());
            if (stored.bytes().length > MAX_IMAGE_BYTES) return failed(required,AiProviderErrorType.CONFIGURATION_ERROR,"VISION_IMAGE_TOO_LARGE");
            total += stored.bytes().length;
            if (total > MAX_TOTAL_BYTES) return failed(required,AiProviderErrorType.CONFIGURATION_ERROR,"VISION_TOTAL_TOO_LARGE");
            images.add(new AiVisionRequest.Image(stored.hash(),stored.mime(),stored.bytes()));
        }
        AiRuntimeConfig config=configurations.vision();
        String setHash=sha256(String.join("|",unique.keySet()));
        Resolution cached=cache(questionId,setHash,config);
        if (cached != null) return cached;
        if (!config.enabled() || config.apiKey()==null || config.apiKey().isBlank()) {
            return failed(required,AiProviderErrorType.CONFIGURATION_ERROR,"VISION_CONFIGURATION_MISSING");
        }
        try {
            AiVisionResult result=vision.analyze(new AiVisionRequest(questionId,images,"QUESTION_VISION_CONTEXT"));
            String json=mapper.writeValueAsString(result.context());
            jdbc.update("""
                    INSERT INTO ai_shi_jue_shang_xia_wen
                      (ti_mu_id,fu_jian_ji_he_ha_xi,provider_dai_ma,model_dai_ma,prompt_ban_ben,shi_jue_json,zhuang_tai)
                    VALUES (?,?,?,?,?,CAST(? AS JSON),'SUCCESS')
                    ON DUPLICATE KEY UPDATE shi_jue_json=VALUES(shi_jue_json),zhuang_tai='SUCCESS',cuo_wu_dai_ma=NULL
                    """,questionId,setHash,result.provider(),result.model(),PROMPT_VERSION,json);
            return new Resolution(json,true,true,images.size(),false);
        } catch (AiVisionException exception) {
            jdbc.update("""
                    INSERT INTO ai_shi_jue_shang_xia_wen
                      (ti_mu_id,fu_jian_ji_he_ha_xi,provider_dai_ma,model_dai_ma,prompt_ban_ben,zhuang_tai,cuo_wu_dai_ma)
                    VALUES (?,?,?,?,?,'FAILED',?)
                    ON DUPLICATE KEY UPDATE zhuang_tai='FAILED',cuo_wu_dai_ma=VALUES(cuo_wu_dai_ma),shi_jue_json=NULL
                    """,questionId,setHash,config.normalizedProvider(),config.model(),PROMPT_VERSION,exception.errorType().name());
            return failed(required,exception.errorType(),"VISION_"+exception.errorType().name());
        } catch (Exception exception) {
            return failed(required,AiProviderErrorType.INVALID_RESPONSE,"VISION_INVALID_RESPONSE");
        }
    }

    private Resolution cache(long questionId,String setHash,AiRuntimeConfig config){
        return jdbc.query("""
                SELECT CAST(shi_jue_json AS CHAR) FROM ai_shi_jue_shang_xia_wen
                WHERE ti_mu_id=? AND fu_jian_ji_he_ha_xi=? AND provider_dai_ma=? AND model_dai_ma=?
                  AND prompt_ban_ben=? AND zhuang_tai='SUCCESS'
                """,(rs,row)->new Resolution(rs.getString(1),true,true,0,true),questionId,setHash,
                config.normalizedProvider(),config.model(),PROMPT_VERSION).stream().findFirst().orElse(null);
    }
    private List<Attachment> attachments(long questionId){
        return jdbc.query("""
                SELECT xiang_dui_lu_jing,nei_rong_ha_xi FROM ti_mu_fu_jian
                WHERE ti_mu_id=? AND fu_jian_lei_xing='IMAGE' AND zhuang_tai='ACTIVE' AND yi_shan_chu=0
                  AND guan_lian_wei_zhi IN ('QUESTION','STANDARD_ANALYSIS')
                ORDER BY pai_xu,id
                """,(rs,row)->new Attachment(rs.getString(1),rs.getString(2)),questionId);
    }
    private Resolution failed(boolean required,AiProviderErrorType type,String code){
        if(required) throw new AiVisionException(type,code);
        return new Resolution(null,true,false,0,false);
    }
    private String sha256(String source){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception exception){throw new IllegalStateException("Unable to hash vision context",exception);}
    }
    public record Resolution(String contextJson,boolean used,boolean available,int imageCount,boolean cached){ }
    private record Attachment(String path,String hash){ }
}
