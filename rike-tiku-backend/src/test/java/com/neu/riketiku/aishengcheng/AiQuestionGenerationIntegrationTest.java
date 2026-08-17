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
import com.neu.riketiku.aixuesheng.StudentAiService;
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
    @Autowired JdbcTemplate jdbc; @Autowired QuestionAdminService questions; @Autowired StudentAiService studentAi;
    @BeforeEach void reset(){provider.reset();}

    @Test @Transactional
    void generatesOnlyPendingThenRequiresQualityReviewBeforePublishing(){
        long admin=user("admin");long point=point();long mother=mother("牛顿第二定律母题");
        String motherStem=jdbc.queryForObject("SELECT ti_gan FROM ti_mu WHERE id=?",String.class,mother);
        provider.answer(candidate("空间站货物运输中依据合力和加速度判断运动状态",point,"SCENARIO_TRANSFER"));
        var request=request(mother,point,"SCENARIO_TRANSFER",1);
        var task=service.generate(admin,"ADMIN",request);
        assertThat(task.status()).isEqualTo("SUCCESS");assertThat(task.generatedCount()).isEqualTo(1);
        var candidate=task.candidates().getFirst();
        assertThat(candidate.status()).isEqualTo("PENDING");assertThat(candidate.duplicateWarning()).isEqualTo("NONE");
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
        assertThatThrownBy(()->service.generate(admin,"ADMIN",new AiQuestionGenerationDtos.Generate(mother,"SINGLE_CHOICE",List.of(point),2,"SCENARIO_TRANSFER",4))).isInstanceOf(RenZhengYeWuYiChang.class);
        provider.answer("{bad");provider.answer("{still-bad");
        assertThatThrownBy(()->service.generate(admin,"ADMIN",request(mother,point,"SCENARIO_TRANSFER",1))).isInstanceOf(RenZhengYeWuYiChang.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu WHERE fu_ti_mu_id=?",Integer.class,mother)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_sheng_cheng_ren_wu WHERE zhuang_tai='FAILED'",Integer.class)).isEqualTo(1);

        provider.answer(candidate("电路母题增加电阻条件后的分析",point,"DISTRACTOR_REDESIGN"));
        var first=service.generate(admin,"ADMIN",request(mother,point,"DISTRACTOR_REDESIGN",1));
        provider.answer(candidate("电路母题增加电阻条件后的分析",point,"COMBINED"));
        assertThatThrownBy(()->service.generate(admin,"ADMIN",request(mother,point,"COMBINED",1))).isInstanceOfSatisfying(RenZhengYeWuYiChang.class,e->assertThat(e.getCode()).isEqualTo("QUESTION_DUPLICATE"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu WHERE fu_ti_mu_id=?",Integer.class,mother)).isEqualTo(1);
        seedPending(mother,admin,5);
        assertThatThrownBy(()->service.generate(admin,"ADMIN",request(mother,point,"CONDITION_RECOMBINATION",1))).isInstanceOfSatisfying(RenZhengYeWuYiChang.class,e->assertThat(e.getCode()).isEqualTo("AI_PENDING_LIMIT_REACHED"));
        assertThat(first.candidates()).hasSize(1);
    }

    @Test @Transactional
    void enforcesPublishedMotherAndTeacherSubjectScope(){
        long teacher=user("teacher");long point=point();long published=mother("物理母题");long draft=mother("未发布母题");jdbc.update("UPDATE ti_mu SET zhuang_tai='DRAFT' WHERE id=?",draft);
        assertThatThrownBy(()->service.generate(teacher,"TEACHER",request(published,point,"SCENARIO_TRANSFER",1))).isInstanceOfSatisfying(RenZhengYeWuYiChang.class,e->assertThat(e.getStatus().value()).isEqualTo(403));
        assertThatThrownBy(()->service.generate(teacher,"TEACHER",request(draft,point,"SCENARIO_TRANSFER",1))).isInstanceOfSatisfying(RenZhengYeWuYiChang.class,e->assertThat(e.getCode()).isEqualTo("AI_MOTHER_QUESTION_UNAVAILABLE"));
        jdbc.update("INSERT INTO jiao_shi_dang_an(yong_hu_id,gong_hao,xing_ming,zhuang_tai) VALUES (?,?,?,'ACTIVE')",teacher,"T"+teacher,"匿名教师");long teacherId=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        jdbc.update("INSERT INTO ban_ji(ban_ji_bian_ma,ban_ji_ming_cheng,nian_ji,ru_xue_nian_fen) VALUES (?,?,?,2025)","AIG"+teacher,"AI测试班","高二");long classId=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        jdbc.update("INSERT INTO ren_ke_guan_xi(jiao_shi_id,ban_ji_id,ke_mu_id,shi_fou_zhu_ren_ke,zhuang_tai,kai_shi_shi_jian) VALUES (?,?,1,0,'ACTIVE',CURRENT_TIMESTAMP(3))",teacherId,classId);
        assertThat(service.knowledgePoints(teacher,"TEACHER",1)).extracting(AiQuestionGenerationDtos.KnowledgePointOption::id).contains(point);
        long chemistryPoint=point(2);long chemistryMother=mother(2,"化学母题");
        assertThatThrownBy(()->service.knowledgePoints(teacher,"TEACHER",2)).isInstanceOfSatisfying(RenZhengYeWuYiChang.class,e->assertThat(e.getStatus().value()).isEqualTo(403));
        assertThatThrownBy(()->service.generate(teacher,"TEACHER",request(chemistryMother,chemistryPoint,"SCENARIO_TRANSFER",1))).isInstanceOfSatisfying(RenZhengYeWuYiChang.class,e->assertThat(e.getStatus().value()).isEqualTo(403));
        provider.answer(candidate("物理母题更换实验情境后的判断",point,"SCENARIO_TRANSFER"));
        assertThat(service.generate(teacher,"TEACHER",request(published,point,"SCENARIO_TRANSFER",1)).status()).isEqualTo("SUCCESS");
    }

    @Test @Transactional
    void topicVisualFlagChangesGenerationBehaviorInsteadOfSilentlyFallingBack(){
        long student=user("topic_student");
        String suffix=UUID.randomUUID().toString().replace("-","").substring(0,10);
        jdbc.update("INSERT INTO xue_sheng_dang_an(yong_hu_id,xue_hao,xing_ming,nian_ji) VALUES (?,?,?,?)",student,"TOP"+suffix,"匿名专题学生","高二");
        long point=point();
        long mother=mother("无图片专题母题");
        jdbc.update("UPDATE ti_mu SET ti_mu_lei_xing='SUBJECTIVE',shi_yong_mo_shi='TOPIC_LEARNING',shi_fou_ke_zi_dong_pan_fen=0,zheng_que_da_an=CAST(? AS JSON) WHERE id=?",
                "{\"schemaVersion\":1,\"type\":\"SUBJECTIVE\"}",mother);
        provider.answer("""
                {"schemaVersion":2,"candidates":[{"stem":"在全新实验材料中分析两个独立条件并写出完整推理过程","questionType":"SUBJECTIVE","difficulty":3,"options":[],"correctAnswer":{"schemaVersion":1,"type":"SUBJECTIVE"},"standardAnalysis":"先识别实验变量，再分别分析两个条件，最后综合得到结论。","variationMode":"COMBINED","variationSummary":"更换实验场景并重组推理条件","changedDimensions":["SCENARIO","CONDITION","REASONING_PATH"]}]}
                """);
        var textOnly=new AiQuestionGenerationDtos.Generate(mother,"SUBJECTIVE",List.of(point),3,"COMBINED",1);
        var generated=service.generateTopic(student,textOnly,false,true);
        assertThat(generated.status()).isEqualTo("SUCCESS");
        assertThat(generated.visionUsed()).isFalse();

        var visualRequired=new AiQuestionGenerationDtos.Generate(mother,"SUBJECTIVE",List.of(point),3,"SCENARIO_TRANSFER",1);
        assertThatThrownBy(() -> service.generateTopic(student,visualRequired,true,true))
                .isInstanceOfSatisfying(RenZhengYeWuYiChang.class,error -> assertThat(error.getCode()).isEqualTo("AI_VISION_UNAVAILABLE"));
        assertThat(jdbc.queryForObject("SELECT zhuang_tai FROM ai_sheng_cheng_ren_wu WHERE mu_ti_mu_id=? AND bian_shi_fang_shi='SCENARIO_TRANSFER'",String.class,mother)).isEqualTo("FAILED");
    }

    @Test @Transactional
    void studentTopicCandidateStaysPrivateUntilExplicitTeacherSubmission(){
        long student=user("topic_private_student");
        String suffix=UUID.randomUUID().toString().replace("-","").substring(0,10);
        jdbc.update("INSERT INTO xue_sheng_dang_an(yong_hu_id,xue_hao,xing_ming,nian_ji) VALUES (?,?,?,?)",student,"TP"+suffix,"匿名专题学生","高二");
        long point=point(); long mother=mother("学生专题候选母题");
        jdbc.update("UPDATE ti_mu SET ti_mu_lei_xing='SUBJECTIVE',shi_yong_mo_shi='TOPIC_LEARNING',shi_fou_ke_zi_dong_pan_fen=0,zheng_que_da_an=CAST(? AS JSON) WHERE id=?",
                "{\"schemaVersion\":1,\"type\":\"SUBJECTIVE\"}",mother);
        provider.answer("""
                {"schemaVersion":2,"candidates":[{"stem":"在新的实验材料中分析两个条件并写出完整推理过程","questionType":"SUBJECTIVE","difficulty":3,"options":[],"correctAnswer":{"schemaVersion":1,"type":"SUBJECTIVE"},"standardAnalysis":"先识别变量，再分别分析条件，最后综合结论。","variationMode":"COMBINED","variationSummary":"重组专题情境与推理条件","changedDimensions":["SCENARIO","CONDITION","REASONING_PATH"]}]}
                """);
        var generated=service.generateTopic(student,new AiQuestionGenerationDtos.Generate(mother,"SUBJECTIVE",List.of(point),3,"COMBINED",1),false,true);
        long candidate=generated.candidates().getFirst().questionId();
        assertThat(generated.candidates().getFirst().status()).isEqualTo("DRAFT");
        var tutor=studentAi.createConversation(student,null,candidate,null,"TOPIC_QUESTION",null,"STANDARD",false);
        assertThat(tutor.contextType()).isEqualTo("TOPIC_QUESTION");
        assertThat(tutor.messages()).isEmpty();

        long teacher=user("topic_private_teacher");
        jdbc.update("INSERT INTO jiao_shi_dang_an(yong_hu_id,gong_hao,xing_ming,zhuang_tai) VALUES (?,?,?,'ACTIVE')",teacher,"TP"+suffix,"匿名教师");
        long teacherProfile=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        jdbc.update("INSERT INTO ban_ji(ban_ji_bian_ma,ban_ji_ming_cheng,nian_ji,ru_xue_nian_fen) VALUES (?,?,?,2025)","TP"+suffix,"专题审核班","高二");
        long classId=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
        jdbc.update("INSERT INTO ren_ke_guan_xi(jiao_shi_id,ban_ji_id,ke_mu_id,shi_fou_zhu_ren_ke,zhuang_tai,kai_shi_shi_jian) VALUES (?,?,1,0,'ACTIVE',CURRENT_TIMESTAMP(3))",teacherProfile,classId);

        assertThat(service.tasks(teacher,"TEACHER")).noneMatch(task -> task.id()==generated.id());
        assertThatThrownBy(() -> service.task(generated.id(),teacher,"TEACHER"))
                .isInstanceOfSatisfying(RenZhengYeWuYiChang.class,error -> assertThat(error.getCode()).isEqualTo("AI_STUDENT_CANDIDATE_PRIVATE"));
        assertThatThrownBy(() -> service.review(teacher,"TEACHER",candidate,new AiQuestionGenerationDtos.Review(1,1,1,1,1,"APPROVED",3,"不应直审")))
                .isInstanceOfSatisfying(RenZhengYeWuYiChang.class,error -> assertThat(error.getCode()).isEqualTo("AI_STUDENT_CANDIDATE_PRIVATE"));

        var submitted=service.submitStudentTopicVariant(student,candidate);
        assertThat(submitted.candidates().getFirst().status()).isEqualTo("PENDING");
        assertThat(service.task(generated.id(),teacher,"TEACHER").candidates()).hasSize(1);
        var approved=service.review(teacher,"TEACHER",candidate,new AiQuestionGenerationDtos.Review(1,1,1,1,1,"APPROVED",3,"人工审核通过"));
        assertThat(approved.status()).isEqualTo("PUBLISHED");
    }

    @Test
    void rollsBackWholeCandidateBatchWhenSecondQualityInsertFails(){
        long admin=user("atomic");long point=point();long mother=mother("批次原子性母题");
        String originalStem=jdbc.queryForObject("SELECT ti_gan FROM ti_mu WHERE id=?",String.class,mother);
        String originalStandard=jdbc.queryForObject("SELECT jie_xi_nei_rong FROM ti_mu_jie_xi WHERE ti_mu_id=? AND jie_xi_lei_xing='STANDARD'",String.class,mother);
        Long taskId=null;
        jdbc.execute("DROP TRIGGER IF EXISTS trg_ai_candidate_atomic_failure");
        jdbc.execute("""
                CREATE TRIGGER trg_ai_candidate_atomic_failure
                BEFORE INSERT ON ai_hou_xuan_ti_zhi_liang_ping_jia FOR EACH ROW
                BEGIN
                  IF NEW.bian_shi_zhai_yao='FORCE_ATOMIC_FAILURE' THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='forced second candidate failure';
                  END IF;
                END
                """);
        try{
            provider.answer(candidateBatch(point));
            var request=request(mother,point,"COMBINED",2);
            assertThatThrownBy(()->service.generate(admin,"ADMIN",request))
                    .isInstanceOfSatisfying(RenZhengYeWuYiChang.class,e->assertThat(e.getCode()).isEqualTo("AI_GENERATION_FAILED"));
            taskId=jdbc.queryForObject("SELECT id FROM ai_sheng_cheng_ren_wu WHERE mu_ti_mu_id=?",Long.class,mother);
            assertThat(jdbc.queryForObject("SELECT zhuang_tai FROM ai_sheng_cheng_ren_wu WHERE id=?",String.class,taskId)).isEqualTo("FAILED");
            assertThat(jdbc.queryForObject("SELECT yi_sheng_cheng_shu_liang FROM ai_sheng_cheng_ren_wu WHERE id=?",Integer.class,taskId)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_hou_xuan_ti_zhi_liang_ping_jia WHERE ai_sheng_cheng_ren_wu_id=?",Integer.class,taskId)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu WHERE fu_ti_mu_id=?",Integer.class,mother)).isZero();
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ti_mu q JOIN ti_mu_lai_yuan s ON s.ti_mu_id=q.id
                    WHERE q.fu_ti_mu_id=? AND q.zhuang_tai='PENDING' AND s.lai_yuan_lei_xing='AI_GENERATED'
                    """,Integer.class,mother)).isZero();
            assertThat(jdbc.queryForObject("SELECT ti_gan FROM ti_mu WHERE id=?",String.class,mother)).isEqualTo(originalStem);
            assertThat(jdbc.queryForObject("SELECT jie_xi_nei_rong FROM ti_mu_jie_xi WHERE ti_mu_id=? AND jie_xi_lei_xing='STANDARD'",String.class,mother)).isEqualTo(originalStandard);
        }finally{
            jdbc.execute("DROP TRIGGER IF EXISTS trg_ai_candidate_atomic_failure");
            if(taskId!=null)jdbc.update("DELETE FROM ai_sheng_cheng_ren_wu WHERE id=?",taskId);
            jdbc.update("DELETE FROM ti_mu_xuan_xiang WHERE ti_mu_id=?",mother);
            jdbc.update("DELETE FROM ti_mu_jie_xi WHERE ti_mu_id=?",mother);
            jdbc.update("DELETE FROM ti_mu WHERE id=?",mother);
            jdbc.update("DELETE FROM yong_hu WHERE id=?",admin);
        }
    }

    @Test @Transactional
    void safelyReusesFailedRequestTaskButStillRejectsEffectiveDuplicate(){
        long admin=user("retry");long point=point();long mother=mother("动量守恒母题");
        var request=request(mother,point,"REPRESENTATION_SWITCH",1);
        provider.answer("{bad");provider.answer("{still-bad");
        assertThatThrownBy(()->service.generate(admin,"ADMIN",request)).isInstanceOf(RenZhengYeWuYiChang.class);
        long failedTask=jdbc.queryForObject("SELECT id FROM ai_sheng_cheng_ren_wu WHERE zhuang_tai='FAILED'",Long.class);
        provider.answer(candidate("冰面碰撞实验中比较相互作用前后的总动量",point,"REPRESENTATION_SWITCH"));
        var retried=service.generate(admin,"ADMIN",request);
        assertThat(retried.id()).isEqualTo(failedTask);assertThat(retried.status()).isEqualTo("SUCCESS");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_sheng_cheng_ren_wu WHERE mu_ti_mu_id=?",Integer.class,mother)).isEqualTo(1);
        assertThatThrownBy(()->service.generate(admin,"ADMIN",request)).isInstanceOfSatisfying(RenZhengYeWuYiChang.class,e->assertThat(e.getCode()).isEqualTo("DUPLICATE_GENERATION_REQUEST"));
    }

    private AiQuestionGenerationDtos.Generate request(long mother,long point,String mode,int count){return new AiQuestionGenerationDtos.Generate(mother,"SINGLE_CHOICE",List.of(point),2,mode,count);}
    private String candidate(String stem,long point,String mode){return "{\"schemaVersion\":2,\"candidates\":["+candidateJson(stem,point,"改变情境并重组条件",mode)+"]}";}
    private String candidateBatch(long point){return "{\"schemaVersion\":2,\"candidates\":["+candidateJson("太空舱实验中依据加速度与合力关系选择结论",point,"第一候选已进入持久化路径","COMBINED")+","+candidateJson("传送带实验中依据受力变化选择正确结论",point,"FORCE_ATOMIC_FAILURE","COMBINED")+"]}";}
    private String candidateJson(String stem,long point,String summary,String mode){String dimensions=switch(mode){case "SCENARIO_TRANSFER"->"[\"SCENARIO\",\"CONDITION\"]";case "CONDITION_RECOMBINATION"->"[\"CONDITION\",\"DATA\"]";case "REPRESENTATION_SWITCH"->"[\"REPRESENTATION\",\"REASONING_PATH\"]";case "DISTRACTOR_REDESIGN"->"[\"DISTRACTOR\",\"REASONING_PATH\"]";case "COMBINED"->"[\"SCENARIO\",\"CONDITION\",\"REASONING_PATH\"]";default->"[\"SCENARIO\",\"CONDITION\"]";};return "{\"stem\":\""+stem+"\",\"questionType\":\"SINGLE_CHOICE\",\"difficulty\":2,\"options\":[{\"label\":\"A\",\"content\":\"合力方向与加速度方向一致\",\"correct\":true},{\"label\":\"B\",\"content\":\"速度方向始终等于合力方向\",\"correct\":false}],\"correctAnswer\":{\"schemaVersion\":1,\"type\":\"SINGLE_CHOICE\",\"optionLabels\":[\"A\"]},\"standardAnalysis\":\"先分析新的受力情境，再由动力学关系逐步判断。\",\"variationMode\":\""+mode+"\",\"variationSummary\":\""+summary+"\",\"changedDimensions\":"+dimensions+"}";}
    private long user(String prefix){String name=prefix+UUID.randomUUID().toString().replace("-","").substring(0,10);jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao,shi_fou_shou_ci_deng_lu) VALUES (?,?,0)",name,"x".repeat(60));return jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
    private long point(){return point(1);}
    private long point(long subjectId){return jdbc.queryForObject("SELECT id FROM zhi_shi_dian WHERE ke_mu_id=? AND zhuang_tai='ACTIVE' LIMIT 1",Long.class,subjectId);}
    private long mother(String stem){return mother(1,stem);}
    private long mother(long subjectId,String stem){String hash=UUID.randomUUID().toString().replace("-","");jdbc.update("INSERT INTO ti_mu(ke_mu_id,ti_mu_lei_xing,shi_yong_mo_shi,ti_gan,zheng_que_da_an,nan_du,shi_fou_ke_zi_dong_pan_fen,zhuang_tai,nei_rong_ha_xi) VALUES (?,'SINGLE_CHOICE','ONLINE_PRACTICE',?,'{\"schemaVersion\":1,\"type\":\"SINGLE_CHOICE\",\"optionLabels\":[\"A\"]}',2,1,'PUBLISHED',?)",subjectId,stem+hash.substring(0,5),hash);long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);jdbc.update("INSERT INTO ti_mu_xuan_xiang(ti_mu_id,xuan_xiang_biao_shi,xuan_xiang_nei_rong,shi_fou_zheng_que,pai_xu) VALUES (?,'A','正确',1,1),(?,'B','错误',0,2)",id,id);jdbc.update("INSERT INTO ti_mu_jie_xi(ti_mu_id,jie_xi_lei_xing,jie_xi_nei_rong,ban_ben_hao,zhuang_tai) VALUES (?,'STANDARD','母题 STANDARD 不可修改',1,'PUBLISHED')",id);return id;}
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
