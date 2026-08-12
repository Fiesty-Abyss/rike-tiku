package com.neu.riketiku.ai.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.neu.riketiku.ai.config.AiRuntimeConfig;
import com.neu.riketiku.ai.config.AiRuntimeConfigurationService;
import com.neu.riketiku.ai.config.AiTextProviderFactory;
import com.neu.riketiku.ai.provider.AiModelProvider;
import com.neu.riketiku.ai.provider.FakeAiModelProvider;
import com.neu.riketiku.ai.vision.AiVisionProvider;
import com.neu.riketiku.ai.vision.AiVisionProviderFactory;
import com.neu.riketiku.ai.vision.FakeVisionProvider;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(AiModelConfigIntegrationTest.Config.class)
class AiModelConfigIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired AiModelConfigService service;
    @Autowired AiRuntimeConfigurationService runtime;
    @Autowired JdbcTemplate jdbc;

    @Test
    void savesMasksReloadsTestsAndClearsTextAndVisionKeys(){
        var text=service.create(save("DEEPSEEK","deepseek-v4-flash","TEXT","text-secret"));
        assertThat(text.apiKeyConfigured()).isTrue();
        assertThat(service.list().records().getFirst().toString()).doesNotContain("text-secret");
        assertThat(runtime.text()).satisfies(config->{assertThat(config.databaseBacked()).isTrue();assertThat(config.model()).isEqualTo("deepseek-v4-flash");});
        var preserved=service.update(text.id(),save("DEEPSEEK","deepseek-v4-flash","TEXT",null));
        assertThat(preserved.apiKeyConfigured()).isTrue();
        assertThat(service.test(text.id())).satisfies(result->{assertThat(result.success()).isTrue();assertThat(result.model()).isEqualTo("deepseek-v4-flash");});
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_diao_yong_ri_zhi WHERE yong_tu='ADMIN_CONNECTION_TEST' AND shi_fou_cheng_gong=1",Integer.class)).isEqualTo(1);
        assertThat(service.clearKey(text.id()).apiKeyConfigured()).isFalse();

        var vision=service.create(save("GLM","glm-4.6v-flash","VISION","vision-secret"));
        assertThat(runtime.vision()).extracting(AiRuntimeConfig::model).isEqualTo("glm-4.6v-flash");
        assertThat(service.test(vision.id())).satisfies(result->{assertThat(result.success()).isTrue();assertThat(result.visionSummaryPreview()).contains("测试图像摘要");});
        assertThat(service.list().records().toString()).doesNotContain("vision-secret");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_mo_xing_pei_zhi WHERE api_mi_yao IN ('text-secret','vision-secret')",Integer.class)).isEqualTo(1);
    }
    private AiModelConfigDtos.Save save(String provider,String model,String usage,String key){return new AiModelConfigDtos.Save(provider,model,usage.equals("TEXT")?"https://api.deepseek.com":"https://open.bigmodel.cn/api/paas/v4",key,usage,true,true,30000,usage.equals("TEXT")?1200:1000,1);}
    @TestConfiguration static class Config{
        @Bean @Primary AiTextProviderFactory fakeTextFactory(){return new AiTextProviderFactory(){@Override public AiModelProvider create(AiRuntimeConfig ignored){return FakeAiModelProvider.successful();}};}
        @Bean @Primary AiVisionProviderFactory fakeVisionFactory(){return new AiVisionProviderFactory(){@Override public AiVisionProvider create(AiRuntimeConfig ignored){return new FakeVisionProvider();}};}
    }
}
