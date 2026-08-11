package com.neu.riketiku.aishengcheng;

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
import com.neu.riketiku.tiku.admin.QuestionAdminService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(AiQuestionGenerationIntegrationTest.Config.class)
class AiQuestionGenerationIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired AiQuestionGenerationService service; @Autowired QueueAiProvider provider;
    @Autowired JdbcTemplate jdbc; @Autowired QuestionAdminService questions;
    @BeforeEach void reset(){provider.reset();}

    @Test @Transactional
    void generatesOnlyPendingThenRequiresQualityReviewBeforePublishing(){
        long admin=user("admin");long point=point();long mother=mother("牛顿第二定律母题");
        String motherStem=jdbc.queryForObject("SELECT ti_gan FROM ti_mu WHERE id=?",String.class,mother);
        provider.answer(candidate(motherStem+"变",point));
        var request=request(mother,point,"SCENARIO",1);
        var task=service.generate(admin,"ADMIN",request);
        assertThat(task.status()).isEqualTo("SUCCESS");assertThat(task.generatedCount()).isEqualTo(1);
        var candidate=task.candidates().getFirst();
        assertThat(candidate.status()).isEqualTo("PENDING");assertThat(candidate.duplicateWarning()).isEqualTo("SUSPECTED_DUPLICATE");
        assertThat(jdbc.queryForObject("SELECT fu_ti_mu_id FROM ti_mu WHERE id=?",Long.class,candidate.questionId())).isEqualTo(mother);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu_lai_yuan WHERE ti_mu_id=? AND lai_yuan_lei_xing='AI_GENERATED'",Integer.class,candidate.questionId())).isEqualTo(3);
        assertThatThrownBy(()->questions.transition(candidate.questionId(),"APPROVED","PENDING","PUBLISHED",null,admin))
                .isInstanceOfSatisfying(RenZhengYeWuYiChang.class,e->assertThat(e.getCode()).isEqualTo("AI_QUALITY_REVIEW_REQUIRED"));
        var reviewed=service.review(admin,"ADMIN",candidate.questionId(),new AiQuestionGenerationDtos.Review(1,1,1,1,1,"APPROVED",6,"人工复核通过"));
        assertThat(reviewed.status()).isEqualTo("PUBLISHED");assertThat(reviewed.quality().reviewResult()).isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject("SELECT jie_xi_nei_rong FROM ti_mu_jie_xi WHERE ti_mu_id=? AND jie_xi_lei_xing='STANDARD'",String.class,mother)).isEqualTo("母题 STANDARD 不可修改");
        assertThatThrownBy(()->service.generate(admin,"ADMIN",request)).isInstanceOfSatisfying(RenZhengYeWuYiChang.class,e->assertThat(e.getCode()).isEqualTo("DUPLICATE_GENERATION_REQUEST"));
    }

    @Test @Transactional
    void rejectsInvalidJsonExactBatchAndPendingLimitsWithoutPublishing(){
        long admin=user("limits");long point=point();long mother=mother("电路母题");
        assertThatThrownBy(()->service.generate(admin,"ADMIN",new AiQuestionGenerationDtos.Generate(mother,"SINGLE_CHOICE",List.of(point),2,"SCENARIO",4))).isInstanceOf(RenZhengYeWuYiChang.class);
        provider.answer("{bad");
        assertThatThrownBy(()->service.generate(admin,"ADMIN",request(mother,point,"SCENARIO",1))).isInstanceOf(RenZhengYeWuYiChang.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu WHERE fu_ti_mu_id=?",Integer.class,mother)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_sheng_cheng_ren_wu WHERE zhuang_tai='FAILED'",Integer.class)).isEqualTo(1);

        provider.answer(candidate("电路母题增加电阻条件后的分析",point));
        var first=service.generate(admin,"ADMIN",request(mother,point,"DISTRACTOR",1));
        provider.answer(candidate("电路母题增加电阻条件后的分析",point));
        assertThatThrownBy(()->service.generate(admin,"ADMIN",request(mother,point,"COMBINED",1))).isInstanceOfSatisfying(RenZhengYeWuYiChang.class,e->assertThat(e.getCode()).isEqualTo("QUESTION_DUPLICATE"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu WHERE fu_ti_mu_id=?",Integer.class,mother)).isEqualTo(1);
        seedPending(mother,admin,5);
        assertThatThrownBy(()->service.generate(admin,"ADMIN",request(mother,point,"KNOWLEDGE_ANGLE",1))).isInstanceOfSatisfying(RenZhengYeWuYiChang.class,e->assertThat(e.getCode()).isEqualTo("AI_PENDING_LIMIT_REACHED"));
        assertThat(first.candidates()).hasSize(1);
    }

    @Test @Transactional
    void enforcesPublishedMotherAndTeacherSubjectScope(){
        long teacher=user("teacher");long point=point();long published=mother("生物母题");long draft=mother("未发布母题");jdbc.update("UPDATE ti_mu SET zhuang_tai='DRAFT' WHERE id=?",draft);
        assertThatThrownBy(()->service.generate(teacher,"TEACHER",request(published,point,"SCENARIO",1))).isInstanceOfSatisfying(RenZhengYeWuYiChang.class,e->assertThat(e.getStatus().value()).isEqualTo(403));
        assertThatThrownBy(()->service.generate(teacher,"TEACHER",request(draft,point,"SCENARIO",1))).isInstanceOfSatisfying(RenZhengYeWuYiChang.class,e->assertThat(e.getCode()).isEqualTo("AI_MOTHER_QUESTION_UNAVAILABLE"));
        jdbc.update("INSERT INTO jiao_shi_dang_an(yong_hu_id,gong_hao,xing_ming,zhuang_tai) VALUES (?,?,?,'ACTIVE')",teacher,"T"+teacher,"匿名教师");long teacherId=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        jdbc.update("INSERT INTO ban_ji(ban_ji_bian_ma,ban_ji_ming_cheng,nian_ji,ru_xue_nian_fen) VALUES (?,?,?,2025)","AIG"+teacher,"AI测试班","高二");long classId=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        jdbc.update("INSERT INTO ren_ke_guan_xi(jiao_shi_id,ban_ji_id,ke_mu_id,shi_fou_zhu_ren_ke,zhuang_tai,kai_shi_shi_jian) VALUES (?,?,1,0,'ACTIVE',CURRENT_TIMESTAMP(3))",teacherId,classId);
        provider.answer(candidate("生物母题更换实验情境后的判断",point));
        assertThat(service.generate(teacher,"TEACHER",request(published,point,"SCENARIO",1)).status()).isEqualTo("SUCCESS");
    }

    @Test @Transactional
    void safelyReusesFailedRequestTaskButStillRejectsEffectiveDuplicate(){
        long admin=user("retry");long point=point();long mother=mother("动量守恒母题");
        var request=request(mother,point,"KNOWLEDGE_ANGLE",1);
        provider.answer("{bad");
        assertThatThrownBy(()->service.generate(admin,"ADMIN",request)).isInstanceOf(RenZhengYeWuYiChang.class);
        long failedTask=jdbc.queryForObject("SELECT id FROM ai_sheng_cheng_ren_wu WHERE zhuang_tai='FAILED'",Long.class);
        provider.answer(candidate("动量守恒母题改变知识角度后的判断",point));
        var retried=service.generate(admin,"ADMIN",request);
        assertThat(retried.id()).isEqualTo(failedTask);assertThat(retried.status()).isEqualTo("SUCCESS");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_sheng_cheng_ren_wu WHERE mu_ti_mu_id=?",Integer.class,mother)).isEqualTo(1);
        assertThatThrownBy(()->service.generate(admin,"ADMIN",request)).isInstanceOfSatisfying(RenZhengYeWuYiChang.class,e->assertThat(e.getCode()).isEqualTo("DUPLICATE_GENERATION_REQUEST"));
    }

    private AiQuestionGenerationDtos.Generate request(long mother,long point,String mode,int count){return new AiQuestionGenerationDtos.Generate(mother,"SINGLE_CHOICE",List.of(point),2,mode,count);}
    private String candidate(String stem,long point){return "{\"candidates\":[{\"stem\":\""+stem+"\",\"questionType\":\"SINGLE_CHOICE\",\"difficulty\":2,\"options\":[{\"label\":\"A\",\"content\":\"正确\",\"correct\":true},{\"label\":\"B\",\"content\":\"错误\",\"correct\":false}],\"correctAnswer\":{\"schemaVersion\":1,\"type\":\"SINGLE_CHOICE\",\"optionLabels\":[\"A\"]},\"standardAnalysis\":\"候选解析，必须人工复核\",\"knowledgePoints\":["+point+"],\"variationSummary\":\"改变情境并保持知识点一致\"}]}";}
    private long user(String prefix){String name=prefix+UUID.randomUUID().toString().replace("-","").substring(0,10);jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao,shi_fou_shou_ci_deng_lu) VALUES (?,?,0)",name,"x".repeat(60));return jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
    private long point(){return jdbc.queryForObject("SELECT id FROM zhi_shi_dian WHERE ke_mu_id=1 AND zhuang_tai='ACTIVE' LIMIT 1",Long.class);}
    private long mother(String stem){String hash=UUID.randomUUID().toString().replace("-","");jdbc.update("INSERT INTO ti_mu(ke_mu_id,ti_mu_lei_xing,shi_yong_mo_shi,ti_gan,zheng_que_da_an,nan_du,shi_fou_ke_zi_dong_pan_fen,zhuang_tai,nei_rong_ha_xi) VALUES (1,'SINGLE_CHOICE','ONLINE_PRACTICE',?,'{\"schemaVersion\":1,\"type\":\"SINGLE_CHOICE\",\"optionLabels\":[\"A\"]}',2,1,'PUBLISHED',?)",stem+hash.substring(0,5),hash);long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);jdbc.update("INSERT INTO ti_mu_xuan_xiang(ti_mu_id,xuan_xiang_biao_shi,xuan_xiang_nei_rong,shi_fou_zheng_que,pai_xu) VALUES (?,'A','正确',1,1),(?,'B','错误',0,2)",id,id);jdbc.update("INSERT INTO ti_mu_jie_xi(ti_mu_id,jie_xi_lei_xing,jie_xi_nei_rong,ban_ben_hao,zhuang_tai) VALUES (?,'STANDARD','母题 STANDARD 不可修改',1,'PUBLISHED')",id);return id;}
    private void seedPending(long mother,long admin,int number){for(int i=0;i<number;i++){String hash=UUID.randomUUID().toString().replace("-","");jdbc.update("INSERT INTO ti_mu(ke_mu_id,fu_ti_mu_id,ti_mu_lei_xing,shi_yong_mo_shi,ti_gan,zheng_que_da_an,nan_du,shi_fou_ke_zi_dong_pan_fen,zhuang_tai,nei_rong_ha_xi) VALUES (1,?,'SINGLE_CHOICE','ONLINE_PRACTICE',?,'{\"schemaVersion\":1,\"type\":\"SINGLE_CHOICE\",\"optionLabels\":[\"A\"]}',2,1,'PENDING',?)",mother,"已有候选"+hash,hash);long q=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);jdbc.update("INSERT INTO ti_mu_lai_yuan(ti_mu_id,nei_rong_lei_xing,lai_yuan_lei_xing,lai_yuan_ming_cheng,quan_li_zhuang_tai,quan_li_yi_ju) VALUES (?,'QUESTION','AI_GENERATED','测试','USER_PROVIDED','测试')",q);}}
    @TestConfiguration static class Config{
        @Bean @Primary QueueAiProvider queueAiProvider(AiProviderProperties properties,AiModelProvider actual,AiCallLogWriter log){return new QueueAiProvider(properties,actual,log);}
    }
    static class QueueAiProvider extends AiProviderService{
        final ArrayDeque<String> answers=new ArrayDeque<>();final List<AiModelRequest> requests=new ArrayList<>();
        QueueAiProvider(AiProviderProperties properties,AiModelProvider provider,AiCallLogWriter log){super(properties,provider,log);}
        void reset(){answers.clear();requests.clear();}void answer(String value){answers.add(value);}
        @Override public AiModelResult generate(AiModelRequest request){requests.add(request);return new AiModelResult("fake-deepseek","deepseek-v4-flash",answers.removeFirst(),new AiTokenUsage(50,100,150),"stop");}
    }
}
