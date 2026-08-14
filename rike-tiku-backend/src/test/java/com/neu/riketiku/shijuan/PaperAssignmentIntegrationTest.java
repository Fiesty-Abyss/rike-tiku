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

    private long id(String sql) { return jdbc.queryForObject(sql, Long.class); }
}
