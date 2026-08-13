package com.neu.riketiku.aixuesheng;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neu.riketiku.ai.AiProviderService;
import com.neu.riketiku.ai.config.AiProviderProperties;
import com.neu.riketiku.ai.log.AiCallLogWriter;
import com.neu.riketiku.ai.provider.AiModelProvider;
import com.neu.riketiku.ai.provider.AiModelRequest;
import com.neu.riketiku.ai.provider.AiModelResult;
import com.neu.riketiku.ai.provider.AiTokenUsage;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.util.ArrayDeque;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Import(StudentAiVariantServiceIntegrationTest.Config.class)
class StudentAiVariantServiceIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired StudentAiVariantService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired QueueProvider provider;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach void reset() { provider.answers.clear(); }

    @Test void singleChoiceGeneratesAnswersGradesAndSubmitsPendingWithoutChangingMotherStandard() {
        Fixture f=fixture("SINGLE_CHOICE",2);
        provider.answers.add(candidate("SINGLE_CHOICE",3,f.pointId,"[\"A\"]"));
        var generated=service.generate(f.userId,f.factId,3);
        assertThat(generated.status()).isEqualTo("READY");
        assertThat(generated.difficulty()).isEqualTo(3);
        assertThat(generated.correctAnswer()).isNull();
        var answered=service.answer(f.userId,generated.id(),mapper.readTree("\"A\""));
        assertThat(answered.correct()).isTrue();
        assertThat(answered.correctAnswer()).isNotNull();
        var submitted=service.submit(f.userId,generated.id());
        assertThat(submitted.status()).isEqualTo("SUBMITTED_FOR_REVIEW");
        assertThat(submitted.reviewStatus()).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT jie_xi_nei_rong FROM ti_mu_jie_xi WHERE ti_mu_id=? AND jie_xi_lei_xing='STANDARD'",String.class,f.motherId)).isEqualTo("母题 STANDARD");
        assertThat(jdbc.queryForObject("SELECT chuang_jian_ren_jiao_se FROM ai_sheng_cheng_ren_wu WHERE id=(SELECT ai_sheng_cheng_ren_wu_id FROM ai_xue_sheng_bian_shi_shi_li WHERE id=?)",String.class,generated.id())).isEqualTo("STUDENT");
    }

    @Test void supportsMultipleChoiceFillBlankAllDifficultiesAndMotherDifficultyDefault() {
        for(int difficulty=1;difficulty<=5;difficulty++){
            Fixture f=fixture("MULTIPLE_CHOICE",difficulty);
            provider.answers.add(candidate("MULTIPLE_CHOICE",difficulty,f.pointId,"[\"A\",\"C\"]"));
            var v=service.generate(f.userId,f.factId,difficulty);
            assertThat(service.answer(f.userId,v.id(),mapper.readTree("[\"A\",\"C\"]")).correct()).isTrue();
        }
        Fixture fill=fixture("FILL_BLANK",4);
        provider.answers.add(candidate("FILL_BLANK",4,fill.pointId,"[\"9.8\"]"));
        var v=service.generate(fill.userId,fill.factId,null);
        assertThat(v.difficulty()).isEqualTo(4);
        assertThat(service.answer(fill.userId,v.id(),mapper.readTree("[\"9.8\"]")).correct()).isTrue();
    }

    @Test void invalidProviderStructureLeavesFailedTaskButNoOrphanCandidateOrInstance() {
        Fixture f=fixture("SINGLE_CHOICE",2); provider.answers.add("{broken");
        assertThatThrownBy(()->service.generate(f.userId,f.factId,2))
                .isInstanceOfSatisfying(RenZhengYeWuYiChang.class,e->assertThat(e.getCode()).isEqualTo("AI_INVALID_RESPONSE"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_sheng_cheng_ren_wu WHERE chuang_jian_ren_id=? AND zhuang_tai='FAILED'",Integer.class,f.userId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_xue_sheng_bian_shi_shi_li WHERE xue_sheng_da_ti_id=?",Integer.class,f.factId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu WHERE fu_ti_mu_id=?",Integer.class,f.motherId)).isZero();
    }

    private Fixture fixture(String type,int difficulty){
        String suffix=UUID.randomUUID().toString().replace("-","").substring(0,10);
        jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao,shi_fou_shou_ci_deng_lu) VALUES (?,?,0)","variant_"+suffix,"x".repeat(60));
        long user=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        jdbc.update("INSERT INTO xue_sheng_dang_an(yong_hu_id,xue_hao,xing_ming,nian_ji) VALUES (?,?,?,?)",user,"V"+suffix,"匿名学生","高二");long student=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        long point=jdbc.queryForObject("SELECT id FROM zhi_shi_dian WHERE ke_mu_id=1 AND zhuang_tai='ACTIVE' LIMIT 1",Long.class);
        String answer=type.equals("FILL_BLANK")?"{\"schemaVersion\":1,\"type\":\"FILL_BLANK\",\"blanks\":[{\"acceptedAnswers\":[\"9.8\"],\"caseSensitive\":false}]}":"{\"schemaVersion\":1,\"type\":\""+type+"\",\"optionLabels\":[\"A\"]}";
        jdbc.update("INSERT INTO ti_mu(ke_mu_id,ti_mu_lei_xing,shi_yong_mo_shi,ti_gan,zheng_que_da_an,nan_du,shi_fou_ke_zi_dong_pan_fen,zhuang_tai,nei_rong_ha_xi) VALUES (1,?,'ONLINE_PRACTICE',?,CAST(? AS JSON),?,1,'PUBLISHED',?)",type,"匿名母题"+suffix,answer,difficulty,UUID.randomUUID().toString().replace("-",""));
        long mother=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        if(!type.equals("FILL_BLANK"))jdbc.update("INSERT INTO ti_mu_xuan_xiang(ti_mu_id,xuan_xiang_biao_shi,xuan_xiang_nei_rong,shi_fou_zheng_que,pai_xu) VALUES (?,'A','正确',1,1),(?,'B','错误',0,2),(?,'C','条件三',0,3)",mother,mother,mother);
        jdbc.update("INSERT INTO ti_mu_jie_xi(ti_mu_id,jie_xi_lei_xing,jie_xi_nei_rong,ban_ben_hao,zhuang_tai) VALUES (?,'STANDARD','母题 STANDARD',1,'PUBLISHED')",mother);
        jdbc.update("INSERT INTO ti_mu_zhi_shi_dian(ti_mu_id,zhi_shi_dian_id,pai_xu) VALUES (?,?,1)",mother,point);
        jdbc.update("INSERT INTO lian_xi_hui_hua(xue_sheng_id,ke_mu_id,zhuang_tai,ti_mu_shu,ti_jiao_shi_jian) VALUES (?,1,'SUBMITTED',1,CURRENT_TIMESTAMP(3))",student);long session=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        jdbc.update("INSERT INTO lian_xi_ti_mu(lian_xi_hui_hua_id,ti_mu_id,ti_mu_shun_xu,fen_zhi,ti_mu_lei_xing,nan_du_kuai_zhao,ti_gan_kuai_zhao,xuan_xiang_kuai_zhao,zheng_que_da_an_kuai_zhao,biao_zhun_jie_xi_kuai_zhao,zhi_shi_dian_kuai_zhao) VALUES (?,?,1,1,?,?,?,JSON_ARRAY(),CAST(? AS JSON),'母题 STANDARD',JSON_ARRAY(JSON_OBJECT('id',?,'name','匿名知识点'))) ",session,mother,type,difficulty,"匿名母题"+suffix,answer,point);long frozen=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        jdbc.update("INSERT INTO xue_sheng_da_ti(lian_xi_ti_mu_id,xue_sheng_id,xue_sheng_da_an,shi_fou_zheng_que,de_fen,ti_jiao_shi_jian) VALUES (?,?,JSON_ARRAY(),0,0,CURRENT_TIMESTAMP(3))",frozen,student);long fact=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        return new Fixture(user,fact,mother,point);
    }

    private String candidate(String type,int difficulty,long point,String labels){
        String answer=type.equals("FILL_BLANK")?"{\"schemaVersion\":1,\"type\":\"FILL_BLANK\",\"blanks\":[{\"acceptedAnswers\":[\"9.8\"],\"caseSensitive\":false}]}":"{\"schemaVersion\":1,\"type\":\""+type+"\",\"optionLabels\":"+labels+"}";
        String options=type.equals("FILL_BLANK")?"[]":"[{\"label\":\"A\",\"content\":\"正确\",\"correct\":true},{\"label\":\"B\",\"content\":\"错误\",\"correct\":false},{\"label\":\"C\",\"content\":\"条件三\",\"correct\":"+labels.contains("C")+"}]";
        return "{\"candidates\":[{\"stem\":\"匿名变式"+UUID.randomUUID()+"\",\"questionType\":\""+type+"\",\"difficulty\":"+difficulty+",\"options\":"+options+",\"correctAnswer\":"+answer+",\"standardAnalysis\":\"AI 解析，仅用于本次练习\",\"knowledgePoints\":["+point+"],\"variationSummary\":\"受控变式\"}]}";
    }
    private record Fixture(long userId,long factId,long motherId,long pointId){}
    @TestConfiguration static class Config{
        @Bean @Primary QueueProvider queueProvider(AiProviderProperties p,AiModelProvider actual,AiCallLogWriter log){return new QueueProvider(p,actual,log);}
    }
    static class QueueProvider extends AiProviderService{
        final ArrayDeque<String> answers=new ArrayDeque<>();QueueProvider(AiProviderProperties p,AiModelProvider actual,AiCallLogWriter log){super(p,actual,log);}
        @Override public AiModelResult generate(AiModelRequest request){return new AiModelResult("fake-deepseek","deepseek-v4-flash",answers.removeFirst(),new AiTokenUsage(10,20,30),"stop");}
    }
}
