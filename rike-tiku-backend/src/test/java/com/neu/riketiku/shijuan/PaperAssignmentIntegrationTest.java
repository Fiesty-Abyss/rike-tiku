package com.neu.riketiku.shijuan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neu.riketiku.demo.DemoDataService;
import com.neu.riketiku.ai.AiProviderService;
import com.neu.riketiku.ai.provider.AiModelResult;
import com.neu.riketiku.ai.provider.AiTokenUsage;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class PaperAssignmentIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private DemoDataService demo;
    @Autowired private PaperService papers;
    @Autowired private PaperAssignmentService assignments;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private AiProviderService aiProvider;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @Transactional
    void publishesFrozenPaperOnlyTo199AndGradesDeterministicallyAndIdempotently() {
        demo.seed();
        long teacherUser = id("SELECT id FROM yong_hu WHERE yong_hu_ming='demo_physics_admin'");
        long student199User = id("SELECT id FROM yong_hu WHERE yong_hu_ming='demo_199_01'");
        long student200User = id("SELECT id FROM yong_hu WHERE yong_hu_ming='demo_200_01'");
        long subject = id("SELECT id FROM ke_mu WHERE ke_mu_dai_ma='PHYSICS'");
        long scope199 = id("SELECT r.id FROM ren_ke_guan_xi r JOIN ban_ji b ON b.id=r.ban_ji_id JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id WHERE b.ban_ji_bian_ma='DEMO_CLASS_199' AND j.yong_hu_id=" + teacherUser + " AND r.ke_mu_id=" + subject);
        long question = id("SELECT id FROM ti_mu WHERE ke_mu_id=" + subject + " AND ti_mu_lei_xing='SINGLE_CHOICE' AND zhuang_tai='PUBLISHED' AND yi_shan_chu=0 ORDER BY id LIMIT 1");
        PaperDtos.Paper paper = papers.save(teacherUser, new PaperDtos.Save(subject, "199班确定性判分测试", "MANUAL",
                List.of(new PaperDtos.ItemInput(question, new BigDecimal("10")))));
        PaperAssignmentDtos.Release release = assignments.publish(teacherUser, paper.id(),
                new PaperAssignmentDtos.Publish(scope199, LocalDateTime.now().plusHours(2)));

        assertThat(assignments.studentList(student199User)).extracting(PaperAssignmentDtos.Release::id).contains(release.id());
        assertThat(assignments.studentList(student200User)).extracting(PaperAssignmentDtos.Release::id).doesNotContain(release.id());
        assertThatThrownBy(() -> assignments.studentDetail(student200User, release.id()))
                .isInstanceOf(RenZhengYeWuYiChang.class);

        PaperAssignmentDtos.Detail before = assignments.studentDetail(student199User, release.id());
        assertThat(before.answersVisible()).isFalse();
        assertThat(before.questions().getFirst().correctAnswer()).isNull();
        String correctJson = jdbc.queryForObject("SELECT CAST(zheng_que_da_an_kuai_zhao AS CHAR) FROM shi_juan_fa_bu_ti_mu WHERE id=?", String.class, before.questions().getFirst().itemId());
        JsonNode submitted = mapper.readTree(correctJson).path("optionLabels").get(0);
        PaperAssignmentDtos.Submit request = new PaperAssignmentDtos.Submit(List.of(
                new PaperAssignmentDtos.DraftAnswer(before.questions().getFirst().itemId(), submitted)));
        PaperAssignmentDtos.SubmitResult first = assignments.submit(student199User, release.id(), request);
        PaperAssignmentDtos.SubmitResult second = assignments.submit(student199User, release.id(), request);
        assertThat(first.objectiveScore()).isEqualByComparingTo("10");
        assertThat(second).isEqualTo(first);
        PaperAssignmentDtos.Detail after = assignments.studentDetail(student199User, release.id());
        assertThat(after.answersVisible()).isTrue();
        assertThat(after.questions().getFirst().correct()).isTrue();
        assertThat(after.questions().getFirst().standardAnalysis()).isNotBlank();
        assertThat(assignments.classStats(teacherUser, release.id()).submitted()).isEqualTo(1);
        assertThat(assignments.quality(teacherUser, paper.id()).notice()).contains("不代替教师审核");
        when(aiProvider.generate(any())).thenReturn(new AiModelResult("fake-deepseek", "paper-test-model",
                "覆盖范围可核对；难度与题量仍需教师复核；不会自动换题、改分或发布。", new AiTokenUsage(10, 8, 18), "stop"));
        PaperAssignmentDtos.AiQualityAssessment ai = assignments.aiQuality(teacherUser, paper.id());
        assertThat(ai.status()).isEqualTo("AI_ADVICE_READY");
        assertThat(ai.notice()).contains("不代替教师审核");
        assertThat(ai.content()).contains("不会自动换题");
    }

    @Test
    @Transactional
    void studentProfileIsRestrictedToTheCurrentTeachersActiveClassAndSubjectScope() {
        demo.seed();
        long physicsTeacher=id("SELECT id FROM yong_hu WHERE yong_hu_ming='demo_physics_admin'");
        long chemistryTeacher=id("SELECT id FROM yong_hu WHERE yong_hu_ming='demo_chemistry_teacher'");
        long student199User=id("SELECT id FROM yong_hu WHERE yong_hu_ming='demo_199_01'");
        long student199=id("SELECT id FROM xue_sheng_dang_an WHERE yong_hu_id="+student199User);
        long student200=id("SELECT s.id FROM xue_sheng_dang_an s JOIN yong_hu u ON u.id=s.yong_hu_id WHERE u.yong_hu_ming='demo_200_01'");

        var physics=publishAndSubmit(physicsTeacher,student199User,"PHYSICS","物理画像边界");
        var chemistry=publishAndSubmit(chemistryTeacher,student199User,"CHEMISTRY","化学画像边界");

        var profile=assignments.studentProfile(physicsTeacher,physics.id(),student199);
        assertThat(profile.trend()).extracting(PaperAssignmentDtos.StudentTrend::releaseId)
                .contains(physics.id()).doesNotContain(chemistry.id());
        assertThatThrownBy(() -> assignments.studentProfile(physicsTeacher,physics.id(),student200))
                .isInstanceOfSatisfying(RenZhengYeWuYiChang.class,error -> assertThat(error.getCode()).isEqualTo("PAPER_STUDENT_FORBIDDEN"));
        assertThatThrownBy(() -> assignments.studentProfile(chemistryTeacher,physics.id(),student199))
                .isInstanceOfSatisfying(RenZhengYeWuYiChang.class,error -> assertThat(error.getCode()).isEqualTo("PAPER_RELEASE_NOT_FOUND"));
    }

    @Test
    @Transactional
    void publishesTopicSubjectiveQuestionWithFrozenAttachmentAndNeverAutoGradesIt() {
        demo.seed();
        long teacherUser = id("SELECT id FROM yong_hu WHERE yong_hu_ming='demo_physics_admin'");
        long studentUser = id("SELECT id FROM yong_hu WHERE yong_hu_ming='demo_199_01'");
        long subject = id("SELECT id FROM ke_mu WHERE ke_mu_dai_ma='PHYSICS'");
        long scope = id("SELECT r.id FROM ren_ke_guan_xi r JOIN ban_ji b ON b.id=r.ban_ji_id JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id WHERE b.ban_ji_bian_ma='DEMO_CLASS_199' AND j.yong_hu_id=" + teacherUser + " AND r.ke_mu_id=" + subject);

        var searchable = papers.questions(teacherUser, subject, null, "SUBJECTIVE", null, null);
        assertThat(searchable).isNotEmpty().allSatisfy(question -> assertThat(question.type()).isEqualTo("SUBJECTIVE"));
        long subjective = searchable.stream().filter(question -> !question.stemAttachments().isEmpty()).findFirst().orElseThrow().id();
        var paper = papers.save(teacherUser, new PaperDtos.Save(subject, "专题主观题发布", "MANUAL",
                List.of(new PaperDtos.ItemInput(subjective, new BigDecimal("20")))));
        var release = assignments.publish(teacherUser, paper.id(), new PaperAssignmentDtos.Publish(scope, LocalDateTime.now().plusHours(2)));
        var detail = assignments.studentDetail(studentUser, release.id());
        var question = detail.questions().getFirst();
        assertThat(question.type()).isEqualTo("SUBJECTIVE");
        assertThat(question.stemAttachments()).isNotEmpty();
        assertThat(assignments.studentAttachment(studentUser, release.id(), question.itemId(),
                question.stemAttachments().getFirst().id()).bytes()).isNotEmpty();
        assertThat(assignments.quality(teacherUser, paper.id()).coverage()).noneMatch(value -> value.contains("{SUBJECTIVE="));

        var result = assignments.submit(studentUser, release.id(), new PaperAssignmentDtos.Submit(List.of(
                new PaperAssignmentDtos.DraftAnswer(question.itemId(), mapper.readTree("\"分步骤作答\"")))));
        assertThat(result.objectiveScore()).isEqualByComparingTo("0");
        assertThat(result.objectiveTotal()).isEqualByComparingTo("0");
        assertThat(result.subjectivePendingCount()).isEqualTo(1);
        assertThat(id("SELECT COUNT(*) FROM shi_juan_xue_sheng_da_ti WHERE shi_juan_ti_jiao_id=" + result.submissionId() + " AND zhuang_tai='SUBJECTIVE_PENDING'"))
                .isEqualTo(1);
    }

    @Test
    @Transactional
    void teacherQuestionSearchDoesNotLeakAnotherTeachersPrivateTopicQuestion() {
        demo.seed();
        long teacher199 = id("SELECT id FROM yong_hu WHERE yong_hu_ming='demo_physics_admin'");
        long subject = id("SELECT id FROM ke_mu WHERE ke_mu_dai_ma='PHYSICS'");
        long scope199 = id("SELECT r.id FROM ren_ke_guan_xi r JOIN ban_ji b ON b.id=r.ban_ji_id JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id WHERE b.ban_ji_bian_ma='DEMO_CLASS_199' AND j.yong_hu_id=" + teacher199 + " AND r.ke_mu_id=" + subject);
        jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao) VALUES (?,?)", "topic_scope_other", "x".repeat(60));
        long teacher200 = id("SELECT LAST_INSERT_ID()");
        jdbc.update("INSERT INTO jiao_shi_dang_an(yong_hu_id,gong_hao,xing_ming) VALUES (?,?,?)", teacher200, "TOPIC_OTHER", "范围外教师");
        long profile200 = id("SELECT LAST_INSERT_ID()");
        jdbc.update("INSERT INTO ban_ji(ban_ji_bian_ma,ban_ji_ming_cheng,nian_ji,ru_xue_nian_fen) VALUES ('TOPIC_SCOPE_OTHER','范围外测试班','高二',2025)");
        long class200 = id("SELECT LAST_INSERT_ID()");
        jdbc.update("INSERT INTO ren_ke_guan_xi(jiao_shi_id,ban_ji_id,ke_mu_id,zhuang_tai,kai_shi_shi_jian) VALUES (?,?,?,'ACTIVE',CURRENT_DATE)", profile200, class200, subject);
        long privateQuestion = id("SELECT q.id FROM ti_mu q WHERE q.ke_mu_id=" + subject + " AND q.ti_mu_lei_xing='SUBJECTIVE' AND q.shi_yong_mo_shi='TOPIC_LEARNING' ORDER BY q.id LIMIT 1");
        jdbc.update("UPDATE ti_mu SET ke_jian_fan_wei='TEACHING_SCOPE_PRIVATE',ren_ke_guan_xi_id=?,chuang_jian_ren_id=? WHERE id=?", scope199, teacher199, privateQuestion);

        assertThat(papers.questions(teacher199, subject, null, "SUBJECTIVE", null, null)).extracting(PaperDtos.QuestionOption::id).contains(privateQuestion);
        assertThat(papers.questions(teacher200, subject, null, "SUBJECTIVE", null, null)).extracting(PaperDtos.QuestionOption::id).doesNotContain(privateQuestion);
        assertThatThrownBy(() -> papers.save(teacher200, new PaperDtos.Save(subject, "越权专题题", "MANUAL",
                List.of(new PaperDtos.ItemInput(privateQuestion, new BigDecimal("10"))))))
                .isInstanceOf(RenZhengYeWuYiChang.class);
    }

    @Test
    @Transactional
    void softDeleteHidesPaperButKeepsCancelledReleaseAndSubmissionHistory() {
        demo.seed();
        long teacher = id("SELECT id FROM yong_hu WHERE yong_hu_ming='demo_physics_admin'");
        long student = id("SELECT id FROM yong_hu WHERE yong_hu_ming='demo_199_01'");
        long subject = id("SELECT id FROM ke_mu WHERE ke_mu_dai_ma='PHYSICS'");
        long scope = id("SELECT r.id FROM ren_ke_guan_xi r JOIN ban_ji b ON b.id=r.ban_ji_id JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id WHERE b.ban_ji_bian_ma='DEMO_CLASS_199' AND j.yong_hu_id=" + teacher + " AND r.ke_mu_id=" + subject);
        long question = id("SELECT id FROM ti_mu WHERE ke_mu_id=" + subject + " AND ti_mu_lei_xing='SINGLE_CHOICE' AND zhuang_tai='PUBLISHED' AND yi_shan_chu=0 ORDER BY id LIMIT 1");
        var paper = papers.save(teacher, new PaperDtos.Save(subject, "软删除历史保留", "MANUAL", List.of(new PaperDtos.ItemInput(question, new BigDecimal("10")))));
        var release = assignments.publish(teacher, paper.id(), new PaperAssignmentDtos.Publish(scope, LocalDateTime.now().plusHours(2)));
        assertThatThrownBy(() -> papers.softDelete(teacher, paper.id())).isInstanceOfSatisfying(RenZhengYeWuYiChang.class,
                error -> assertThat(error.getCode()).isEqualTo("PAPER_DELETE_ACTIVE_RELEASE"));
        var detail = assignments.studentDetail(student, release.id());
        JsonNode answer = mapper.readTree(jdbc.queryForObject("SELECT CAST(zheng_que_da_an_kuai_zhao AS CHAR) FROM shi_juan_fa_bu_ti_mu WHERE id=?", String.class, detail.questions().getFirst().itemId())).path("optionLabels").get(0);
        assignments.submit(student, release.id(), new PaperAssignmentDtos.Submit(List.of(new PaperAssignmentDtos.DraftAnswer(detail.questions().getFirst().itemId(), answer))));
        assignments.cancel(teacher, release.id());
        papers.softDelete(teacher, paper.id());
        assertThat(papers.list(teacher)).extracting(PaperDtos.ListItem::id).doesNotContain(paper.id());
        assertThat(assignments.teacherReleases(teacher, paper.id())).extracting(PaperAssignmentDtos.Release::status).contains("CANCELLED");
        assertThat(assignments.submissions(teacher, release.id())).extracting(PaperAssignmentDtos.SubmissionRow::status).contains("SUBMITTED");
        assertThat(assignments.teacherReleaseOverview(teacher, scope, "CANCELLED", "软删除", 1, 20).items())
                .extracting(PaperAssignmentDtos.ReleaseOverview::paperId).contains(paper.id());
    }

    private PaperAssignmentDtos.Release publishAndSubmit(long teacherUser,long studentUser,String subjectCode,String name) {
        long subject=id("SELECT id FROM ke_mu WHERE ke_mu_dai_ma='"+subjectCode+"'");
        long scope=id("SELECT r.id FROM ren_ke_guan_xi r JOIN ban_ji b ON b.id=r.ban_ji_id JOIN jiao_shi_dang_an j ON j.id=r.jiao_shi_id WHERE b.ban_ji_bian_ma='DEMO_CLASS_199' AND j.yong_hu_id="+teacherUser+" AND r.ke_mu_id="+subject+" AND r.zhuang_tai='ACTIVE'");
        long question=id("SELECT id FROM ti_mu WHERE ke_mu_id="+subject+" AND ti_mu_lei_xing='SINGLE_CHOICE' AND zhuang_tai='PUBLISHED' AND yi_shan_chu=0 ORDER BY id LIMIT 1");
        PaperDtos.Paper paper=papers.save(teacherUser,new PaperDtos.Save(subject,name,"MANUAL",List.of(new PaperDtos.ItemInput(question,new BigDecimal("10")))));
        PaperAssignmentDtos.Release release=assignments.publish(teacherUser,paper.id(),new PaperAssignmentDtos.Publish(scope,LocalDateTime.now().plusHours(2)));
        PaperAssignmentDtos.Detail detail=assignments.studentDetail(studentUser,release.id());
        String answer=jdbc.queryForObject("SELECT CAST(zheng_que_da_an_kuai_zhao AS CHAR) FROM shi_juan_fa_bu_ti_mu WHERE id=?",String.class,detail.questions().getFirst().itemId());
        assignments.submit(studentUser,release.id(),new PaperAssignmentDtos.Submit(List.of(new PaperAssignmentDtos.DraftAnswer(
                detail.questions().getFirst().itemId(),mapper.readTree(answer).path("optionLabels").get(0)))));
        return release;
    }

    private long id(String sql) { return jdbc.queryForObject(sql, Long.class); }
}
