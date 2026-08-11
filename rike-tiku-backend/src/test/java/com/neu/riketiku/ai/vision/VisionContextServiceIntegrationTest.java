package com.neu.riketiku.ai.vision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neu.riketiku.ai.config.AiRuntimeConfig;
import com.neu.riketiku.ai.provider.AiTokenUsage;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import com.neu.riketiku.tiku.fujian.QuestionAttachmentStorage;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(VisionContextServiceIntegrationTest.Config.class)
class VisionContextServiceIntegrationTest extends AdminQuestionIntegrationTestSupport {
    private static final Path STORAGE=temp();
    @DynamicPropertySource static void storage(DynamicPropertyRegistry registry){registry.add("rike.tiku.attachment.storage-root",STORAGE::toString);}
    @Autowired VisionContextService service;@Autowired QuestionAttachmentStorage storage;@Autowired JdbcTemplate jdbc;@Autowired CountingVisionFactory factory;

    @Test @Transactional
    void skipsNoImageDeduplicatesShaReusesCacheAndRejectsThirdImage()throws Exception{
        jdbc.update("""
                INSERT INTO ai_mo_xing_pei_zhi(provider_dai_ma,mo_xing_dai_ma,api_di_zhi,api_mi_yao,yong_tu,
                    shi_fou_qi_yong,shi_fou_mo_ren,chao_shi_hao_miao,zui_da_token,retry_count)
                VALUES ('GLM','glm-4.6v-flash','https://open.bigmodel.cn/api/paas/v4','test-only','VISION',1,1,30000,1000,1)
                ON DUPLICATE KEY UPDATE api_di_zhi=VALUES(api_di_zhi),api_mi_yao=VALUES(api_mi_yao),
                    shi_fou_qi_yong=1,shi_fou_mo_ren=1,chao_shi_hao_miao=30000,zui_da_token=1000,retry_count=1
                """);
        assertThat(service.resolve(2,false)).satisfies(value->{assertThat(value.used()).isFalse();assertThat(value.available()).isTrue();});
        var first=storage.store("first.png",png(Color.BLUE));attachment(1,first,20,"I020");attachment(1,first,21,"I021");
        String standardBefore=jdbc.queryForObject("SELECT jie_xi_nei_rong FROM ti_mu_jie_xi WHERE ti_mu_id=1 AND jie_xi_lei_xing='STANDARD'",String.class);
        var resolved=service.resolve(1,true);var cached=service.resolve(1,true);
        assertThat(resolved.imageCount()).isEqualTo(1);assertThat(cached.cached()).isTrue();assertThat(factory.calls).hasValue(1);assertThat(factory.last.images()).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT CAST(shi_jue_json AS CHAR) FROM ai_shi_jue_shang_xia_wen WHERE ti_mu_id=1",String.class)).contains("确定性视觉摘要").doesNotContain("base64","test-only");
        assertThat(jdbc.queryForObject("SELECT jie_xi_nei_rong FROM ti_mu_jie_xi WHERE ti_mu_id=1 AND jie_xi_lei_xing='STANDARD'",String.class)).isEqualTo(standardBefore);
        attachment(1,storage.store("second.png",png(Color.RED)),22,"I022");attachment(1,storage.store("third.png",png(Color.GREEN)),23,"I023");
        assertThatThrownBy(()->service.resolve(1,true)).isInstanceOfSatisfying(AiVisionException.class,e->assertThat(e.getMessage()).isEqualTo("VISION_IMAGE_LIMIT_EXCEEDED"));
        assertThat(factory.calls).hasValue(1);
    }
    private void attachment(long questionId,QuestionAttachmentStorage.StoredImage image,int order,String marker){jdbc.update("INSERT INTO ti_mu_fu_jian(ti_mu_id,guan_lian_wei_zhi,fu_jian_lei_xing,yuan_shi_wen_jian_ming,xiang_dui_lu_jing,nei_rong_ha_xi,dui_xiang_biao_shi,zheng_wen_zi_fu_wei_zhi,pai_xu,zhuang_tai) VALUES (?,'QUESTION','IMAGE','test.png',?,?,?,?,?,'ACTIVE')",questionId,image.relativePath(),image.hash(),marker,1,order);}
    private static byte[] png(Color color)throws Exception{BufferedImage image=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);var graphics=image.createGraphics();graphics.setColor(color);graphics.fillRect(0,0,2,2);graphics.dispose();var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();}
    private static Path temp(){try{return Files.createTempDirectory("rike-vision-cache-");}catch(Exception e){throw new IllegalStateException(e);}}
    @TestConfiguration static class Config{@Bean @Primary CountingVisionFactory countingVisionFactory(){return new CountingVisionFactory();}}
    static class CountingVisionFactory extends AiVisionProviderFactory{final AtomicInteger calls=new AtomicInteger();AiVisionRequest last;@Override public AiVisionProvider create(AiRuntimeConfig ignored){return new AiVisionProvider(){public String providerCode(){return"glm";}public String modelCode(){return"glm-4.6v-flash";}public AiVisionResult analyze(AiVisionRequest request){calls.incrementAndGet();last=request;return new AiVisionResult("glm","glm-4.6v-flash",new AiVisionContext("DIAGRAM","确定性视觉摘要",List.of("A"),List.of("A-B"),List.of()),new AiTokenUsage(10,5,15));}};}}
}
