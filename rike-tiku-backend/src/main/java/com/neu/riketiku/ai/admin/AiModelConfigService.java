package com.neu.riketiku.ai.admin;

import com.neu.riketiku.ai.config.AiRuntimeConfig;
import com.neu.riketiku.ai.config.AiRuntimeConfigurationService;
import com.neu.riketiku.ai.config.AiTextProviderFactory;
import com.neu.riketiku.ai.log.AiCallLogWriter;
import com.neu.riketiku.ai.provider.AiMessage;
import com.neu.riketiku.ai.provider.AiModelProvider;
import com.neu.riketiku.ai.provider.AiModelRequest;
import com.neu.riketiku.ai.provider.AiModelResult;
import com.neu.riketiku.ai.provider.AiProviderErrorType;
import com.neu.riketiku.ai.provider.AiProviderException;
import com.neu.riketiku.ai.provider.AiThinkingMode;
import com.neu.riketiku.ai.search.OfficialWebSearchClient;
import com.neu.riketiku.ai.search.WebSearchException;
import com.neu.riketiku.ai.search.WebSearchRequest;
import com.neu.riketiku.ai.vision.AiVisionProvider;
import com.neu.riketiku.ai.vision.AiVisionException;
import com.neu.riketiku.ai.vision.AiVisionProviderFactory;
import com.neu.riketiku.ai.vision.AiVisionRequest;
import com.neu.riketiku.ai.vision.AiVisionResult;
import com.neu.riketiku.guanlicaozuorizhi.GuanLiCaoZuoRiZhiFuWu;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import java.time.Duration;
import java.time.LocalDateTime;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import javax.imageio.ImageIO;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiModelConfigService {
    private static final Set<String> TEXT_MODELS = Set.of("deepseek-v4-flash", "deepseek-v4-pro");
    private static final String VISION_MODEL = "glm-4.6v-flash";
    private static final byte[] SAFE_TEST_PNG = safeTestPng();
    private final JdbcTemplate jdbc;
    private final AiRuntimeConfigurationService runtimeConfigurations;
    private final AiTextProviderFactory textFactory;
    private final AiVisionProviderFactory visionFactory;
    private final AiCallLogWriter logWriter;
    private final GuanLiCaoZuoRiZhiFuWu auditLog;
    private final OfficialWebSearchClient webSearch;

    public AiModelConfigService(JdbcTemplate jdbc, AiRuntimeConfigurationService runtimeConfigurations,
                                AiTextProviderFactory textFactory, AiVisionProviderFactory visionFactory,
                                AiCallLogWriter logWriter, GuanLiCaoZuoRiZhiFuWu auditLog,
                                OfficialWebSearchClient webSearch) {
        this.jdbc=jdbc; this.runtimeConfigurations=runtimeConfigurations; this.textFactory=textFactory;
        this.visionFactory=visionFactory; this.logWriter=logWriter; this.auditLog=auditLog; this.webSearch=webSearch;
    }

    @Transactional(readOnly = true)
    public AiModelConfigDtos.Page list() {
        return new AiModelConfigDtos.Page(jdbc.query("""
                SELECT id,provider_dai_ma,mo_xing_dai_ma,api_di_zhi,yong_tu,shi_fou_qi_yong,
                       shi_fou_mo_ren,chao_shi_hao_miao,zui_da_token,retry_count,
                       api_mi_yao IS NOT NULL AND api_mi_yao<>'',zui_jin_ce_shi_zhuang_tai,
                       zui_jin_ce_shi_hao_shi,zui_jin_ce_shi_shi_jian,chuang_jian_shi_jian,geng_xin_shi_jian
                FROM ai_mo_xing_pei_zhi ORDER BY yong_tu,id
                """, (rs,row)->new AiModelConfigDtos.Item(rs.getLong(1),rs.getString(2),rs.getString(3),
                rs.getString(4),rs.getString(5),rs.getBoolean(6),rs.getBoolean(7),rs.getInt(8),
                rs.getInt(9),rs.getInt(10),rs.getBoolean(11),rs.getString(12),rs.getObject(13,Long.class),
                rs.getObject(14,LocalDateTime.class),rs.getObject(15,LocalDateTime.class),
                rs.getObject(16,LocalDateTime.class))));
    }

    @Transactional
    public AiModelConfigDtos.Item create(AiModelConfigDtos.Save request) {
        return auditLog.audited("AI_MODEL", "CREATE", null, "新增 AI 模型配置",
                () -> save(null,request), AiModelConfigDtos.Item::id);
    }

    @Transactional
    public AiModelConfigDtos.Item update(long id, AiModelConfigDtos.Save request) {
        return auditLog.audited("AI_MODEL", "UPDATE", id, "更新 AI 模型配置",
                () -> save(id,request));
    }

    @Transactional
    public AiModelConfigDtos.Item clearKey(long id) {
        return auditLog.audited("AI_MODEL", "CLEAR_KEY", id, "清除 AI API Key", () -> {
            require(id);
            jdbc.update("UPDATE ai_mo_xing_pei_zhi SET api_mi_yao=NULL,zui_jin_ce_shi_zhuang_tai='NOT_TESTED',zui_jin_ce_shi_hao_shi=NULL,zui_jin_ce_shi_shi_jian=NULL WHERE id=?",id);
            runtimeConfigurations.invalidate();
            return item(id);
        });
    }

    public AiModelConfigDtos.ConnectionResult test(long id) {
        ConfigRow row=config(id);
        AiRuntimeConfig runtime=row.runtime();
        if (runtime.apiKey()==null || runtime.apiKey().isBlank()) fail("AI_MODEL_KEY_MISSING","API Key 尚未配置",HttpStatus.BAD_REQUEST);
        long started=System.nanoTime();
        AiModelRequest logRequest=new AiModelRequest(List.of(new AiMessage("user","[CONNECTION_TEST_METADATA_ONLY]")),
                "ADMIN_CONNECTION_TEST","ai-model:"+id,false,64,AiThinkingMode.DISABLED);
        try {
            String preview=null;
            if ("TEXT".equals(runtime.usage())) {
                AiModelProvider provider=textFactory.create(runtime);
                AiModelResult result=provider.generate(new AiModelRequest(List.of(
                        new AiMessage("system","Reply with the single word RIKE."),new AiMessage("user","RIKE")),
                        "ADMIN_CONNECTION_TEST","ai-model:"+id,false,64,AiThinkingMode.DISABLED));
                logWriter.success(logRequest,new AiModelResult(result.providerCode(),result.modelCode(),"[REDACTED]",result.usage(),result.finishReason()),elapsed(started));
            } else if ("VISION".equals(runtime.usage())) {
                AiVisionProvider provider=visionFactory.create(runtime);
                AiVisionResult result=provider.analyze(new AiVisionRequest(id,List.of(
                        new AiVisionRequest.Image("safe-admin-test","image/png",SAFE_TEST_PNG)),"ADMIN_VISION_TEST"));
                preview=truncate(result.context().summary(),120);
                logWriter.success(logRequest,new AiModelResult(result.provider(),result.model(),"[REDACTED]",result.usage(),"stop"),elapsed(started));
            } else {
                var results=webSearch.search(runtime,new WebSearchRequest("高中物理 牛顿第二定律",1));
                preview=results.isEmpty()?"搜索服务已响应，未返回结果":truncate(results.getFirst().title(),120);
                logWriter.success(logRequest,new AiModelResult(runtime.provider(),runtime.model(),"[REDACTED]",null,"stop"),elapsed(started));
            }
            long latency=elapsed(started); updateTest(id,"SUCCESS",latency);
            return new AiModelConfigDtos.ConnectionResult(true,runtime.provider().toUpperCase(Locale.ROOT),
                    runtime.model(),latency,"SUCCESS",preview,null,null,null,null,null,LocalDateTime.now());
        } catch (AiProviderException exception) {
            long latency=elapsed(started); logWriter.failure(logRequest,runtime.normalizedProvider(),runtime.model(),exception.errorType(),latency);
            updateTest(id,"FAILED",latency);
            return new AiModelConfigDtos.ConnectionResult(false,runtime.provider().toUpperCase(Locale.ROOT),runtime.model(),latency,"FAILED",null,safe(exception.errorType()),exception.errorType().name(),null,null,null,LocalDateTime.now());
        } catch (AiVisionException exception) {
            long latency=elapsed(started); logWriter.failure(logRequest,runtime.normalizedProvider(),runtime.model(),exception.errorType(),latency);
            updateTest(id,"FAILED",latency);
                    return new AiModelConfigDtos.ConnectionResult(false,runtime.provider().toUpperCase(Locale.ROOT),runtime.model(),latency,"FAILED",null,exception.getMessage(),exception.errorType().name(),exception.httpStatus(),exception.providerErrorCode(),exception.retryAfterSeconds(),LocalDateTime.now());
        } catch (WebSearchException exception) {
            long latency=elapsed(started); updateTest(id,"FAILED",latency);
            return new AiModelConfigDtos.ConnectionResult(false,runtime.provider().toUpperCase(Locale.ROOT),runtime.model(),latency,"FAILED",null,"联网搜索暂不可用","SEARCH_UNAVAILABLE",null,null,null,LocalDateTime.now());
        } catch (RuntimeException exception) {
            long latency=elapsed(started); updateTest(id,"FAILED",latency);
            return new AiModelConfigDtos.ConnectionResult(false,runtime.provider().toUpperCase(Locale.ROOT),runtime.model(),latency,"FAILED",null,"AI 服务暂不可用","UNKNOWN",null,null,null,LocalDateTime.now());
        }
    }

    private AiModelConfigDtos.Item save(Long id,AiModelConfigDtos.Save request){
        String provider=request.provider().trim().toUpperCase(Locale.ROOT);
        String usage=request.usage().trim().toUpperCase(Locale.ROOT);
        String model=request.model().trim(); String base=request.baseUrl().trim();
        validate(provider,usage,model,base,request);
        if(id!=null) require(id);
        if(request.defaultConfig()) jdbc.update("UPDATE ai_mo_xing_pei_zhi SET shi_fou_mo_ren=0 WHERE yong_tu=?",usage);
        try {
            if(id==null){
                jdbc.update("""
                        INSERT INTO ai_mo_xing_pei_zhi(provider_dai_ma,mo_xing_dai_ma,api_di_zhi,api_mi_yao,yong_tu,
                          shi_fou_qi_yong,shi_fou_mo_ren,chao_shi_hao_miao,zui_da_token,retry_count)
                        VALUES (?,?,?,?,?,?,?,?,?,?)
                        """,provider,model,base,blank(request.apiKey()),usage,request.enabled(),request.defaultConfig(),
                        request.timeoutMillis(),request.maxTokens(),request.retryCount());
                id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
            } else {
                if(blank(request.apiKey())==null) jdbc.update("""
                        UPDATE ai_mo_xing_pei_zhi SET provider_dai_ma=?,mo_xing_dai_ma=?,api_di_zhi=?,yong_tu=?,
                          shi_fou_qi_yong=?,shi_fou_mo_ren=?,chao_shi_hao_miao=?,zui_da_token=?,retry_count=? WHERE id=?
                        """,provider,model,base,usage,request.enabled(),request.defaultConfig(),request.timeoutMillis(),request.maxTokens(),request.retryCount(),id);
                else jdbc.update("""
                        UPDATE ai_mo_xing_pei_zhi SET provider_dai_ma=?,mo_xing_dai_ma=?,api_di_zhi=?,api_mi_yao=?,yong_tu=?,
                          shi_fou_qi_yong=?,shi_fou_mo_ren=?,chao_shi_hao_miao=?,zui_da_token=?,retry_count=? WHERE id=?
                        """,provider,model,base,request.apiKey().trim(),usage,request.enabled(),request.defaultConfig(),request.timeoutMillis(),request.maxTokens(),request.retryCount(),id);
            }
        }catch(DuplicateKeyException exception){fail("AI_MODEL_CONFIG_DUPLICATE","相同 Provider、模型和用途的配置已存在",HttpStatus.CONFLICT);}
        runtimeConfigurations.invalidate(); return item(id);
    }

    private void validate(String provider,String usage,String model,String base,AiModelConfigDtos.Save request){
        if(!base.startsWith("https://")) fail("AI_MODEL_BASE_URL_INVALID","API 地址必须使用 HTTPS",HttpStatus.BAD_REQUEST);
        if("TEXT".equals(usage)){
            if(!"DEEPSEEK".equals(provider)||!TEXT_MODELS.contains(model)) fail("AI_TEXT_MODEL_INVALID","文本模型配置不受支持",HttpStatus.BAD_REQUEST);
        }else if("VISION".equals(usage)){
            if("GLM".equals(provider)){if(!VISION_MODEL.equals(model))fail("AI_VISION_MODEL_INVALID","GLM 视觉模型必须为 glm-4.6v-flash",HttpStatus.BAD_REQUEST);}
            else if("XAI".equals(provider)){if(!model.matches("grok-[A-Za-z0-9._-]+")||!officialXaiBase(base))
                fail("AI_VISION_MODEL_INVALID","xAI 视觉配置必须使用官方 HTTPS API 和管理员指定的 grok 模型",HttpStatus.BAD_REQUEST);}
            else fail("AI_VISION_MODEL_INVALID","视觉 Provider 仅支持 GLM 或 xAI",HttpStatus.BAD_REQUEST);
        }else if("SEARCH".equals(usage)){
            if(!"GLM".equals(provider)||!Set.of("search_std","search_pro","search_pro_sogou","search_pro_quark").contains(model))
                fail("AI_SEARCH_CONFIG_INVALID","搜索配置不受支持",HttpStatus.BAD_REQUEST);
        }else fail("AI_MODEL_USAGE_INVALID","AI 模型用途不受支持",HttpStatus.BAD_REQUEST);
        if(request.retryCount()>1) fail("AI_MODEL_RETRY_INVALID","最多重试一次",HttpStatus.BAD_REQUEST);
    }

    private ConfigRow config(long id){
        return jdbc.query("""
                SELECT id,provider_dai_ma,mo_xing_dai_ma,api_di_zhi,api_mi_yao,yong_tu,shi_fou_qi_yong,
                       zui_da_token,chao_shi_hao_miao,retry_count FROM ai_mo_xing_pei_zhi WHERE id=?
                """,(rs,row)->new ConfigRow(new AiRuntimeConfig(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),
                rs.getString(6),true,rs.getInt(8),Duration.ofMillis(rs.getInt(9)),rs.getInt(10),true)),id)
                .stream().findFirst().orElseThrow(()->new RenZhengYeWuYiChang("AI_MODEL_CONFIG_NOT_FOUND","AI 模型配置不存在",HttpStatus.NOT_FOUND));
    }
    private AiModelConfigDtos.Item item(long id){return list().records().stream().filter(item->item.id()==id).findFirst().orElseThrow();}
    private void require(long id){if(jdbc.queryForObject("SELECT COUNT(*) FROM ai_mo_xing_pei_zhi WHERE id=?",Long.class,id)==0)fail("AI_MODEL_CONFIG_NOT_FOUND","AI 模型配置不存在",HttpStatus.NOT_FOUND);}
    private void updateTest(long id,String status,long latency){jdbc.update("UPDATE ai_mo_xing_pei_zhi SET zui_jin_ce_shi_zhuang_tai=?,zui_jin_ce_shi_hao_shi=?,zui_jin_ce_shi_shi_jian=CURRENT_TIMESTAMP(3) WHERE id=?",status,latency,id);}
    private long elapsed(long started){return (System.nanoTime()-started)/1_000_000;}
    private String safe(AiProviderErrorType type){return switch(type){case AUTHENTICATION_ERROR->"认证失败";case RATE_LIMITED->"请求过于频繁";case TIMEOUT->"连接超时";case CONFIGURATION_ERROR,DISABLED->"配置不可用";default->"AI 服务暂不可用";};}
    private String blank(String value){return value==null||value.isBlank()?null:value.trim();}
    private String truncate(String value,int max){return value==null?null:value.substring(0,Math.min(max,value.length()));}
    private boolean officialXaiBase(String value){try{URI uri=URI.create(value);return "https".equalsIgnoreCase(uri.getScheme())&&"api.x.ai".equalsIgnoreCase(uri.getHost());}catch(Exception ignored){return false;}}
    private static byte[] safeTestPng(){try{BufferedImage image=new BufferedImage(128,128,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        g.setColor(Color.WHITE);g.fillRect(0,0,128,128);g.setColor(new Color(20,65,95));g.setStroke(new BasicStroke(5));g.drawRect(18,18,92,92);
        g.drawLine(28,88,64,40);g.drawLine(64,40,100,88);g.drawString("RIKE",48,108);g.dispose();ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();
    }catch(Exception exception){throw new ExceptionInInitializerError(exception);}}
    private void fail(String code,String message,HttpStatus status){throw new RenZhengYeWuYiChang(code,message,status);}
    private record ConfigRow(AiRuntimeConfig runtime){ }
}
