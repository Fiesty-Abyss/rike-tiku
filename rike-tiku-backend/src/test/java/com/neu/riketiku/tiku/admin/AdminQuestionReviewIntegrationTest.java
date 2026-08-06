package com.neu.riketiku.tiku.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class AdminQuestionReviewIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private QuestionAdminService service;
    @Autowired private JdbcTemplate jdbc;

    @Test @Transactional
    void supportsAllFiveStateTransitionsAndKeepsReviewHistory() {
        var question = service.create(request("AUTHORIZED"));
        Long id = question.question().id();
        service.transition(id, "SUBMITTED", "DRAFT", "PENDING", null, null);
        assertStatuses(id, "PENDING");
        service.transition(id, "APPROVED", "PENDING", "PUBLISHED", null, null);
        assertStatuses(id, "PUBLISHED");
        service.transition(id, "DISABLED", "PUBLISHED", "DISABLED", null, null);
        assertStatuses(id, "DISABLED");
        var republished = service.transition(id, "APPROVED", "DISABLED", "PUBLISHED", null, null);
        assertThat(republished.question().status()).isEqualTo("PUBLISHED");
        assertStatuses(id, "PUBLISHED");
        assertThat(republished.reviews()).extracting(QuestionDtos.Review::action).containsExactly("SUBMITTED", "APPROVED", "DISABLED", "APPROVED");
    }

    @Test @Transactional
    void returnRequiresOpinionAndMovesBackToDraft() {
        var question = service.create(request("AUTHORIZED"));
        service.transition(question.question().id(), "SUBMITTED", "DRAFT", "PENDING", null, null);
        assertThatThrownBy(() -> service.transition(question.question().id(), "REJECTED", "PENDING", "DRAFT", " ", null)).isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("意见");
        assertThat(service.transition(question.question().id(), "REJECTED", "PENDING", "DRAFT", "请补齐依据", null).question().status()).isEqualTo("DRAFT");
        assertStatuses(question.question().id(), "DRAFT");
    }

    @Test @Transactional
    void unknownOrRestrictedRightsCannotBePublished() {
        for (String rights : List.of("COPYRIGHT_UNKNOWN", "RESTRICTED")) {
            var question = service.create(request(rights));
            service.transition(question.question().id(), "SUBMITTED", "DRAFT", "PENDING", null, null);
            assertThatThrownBy(() -> service.transition(question.question().id(), "APPROVED", "PENDING", "PUBLISHED", null, null)).isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("权利");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu_shen_he_ji_lu WHERE ti_mu_id=? AND shen_he_dong_zuo='APPROVED'", Integer.class, question.question().id())).isZero();
            assertStatuses(question.question().id(), "PENDING");
        }
    }

    @Test
    void reviewRecordFailureRollsBackQuestionAndAnalysisStatus() {
        var question = service.create(request("AUTHORIZED"));
        service.transition(question.question().id(), "SUBMITTED", "DRAFT", "PENDING", null, null);
        assertThatThrownBy(() -> service.transition(question.question().id(), "APPROVED", "PENDING", "PUBLISHED", null, 999999L))
                .isInstanceOf(Exception.class);
        assertStatuses(question.question().id(), "PENDING");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu_shen_he_ji_lu WHERE ti_mu_id=? AND shen_he_dong_zuo='APPROVED'", Integer.class, question.question().id())).isZero();
    }

    private void assertStatuses(Long questionId, String expected) {
        assertThat(jdbc.queryForObject("SELECT zhuang_tai FROM ti_mu WHERE id=?", String.class, questionId)).isEqualTo(expected);
        assertThat(jdbc.queryForObject("SELECT zhuang_tai FROM ti_mu_jie_xi WHERE ti_mu_id=? AND jie_xi_lei_xing='STANDARD' AND ban_ben_hao=1 AND yi_shan_chu=0", String.class, questionId)).isEqualTo(expected);
    }

    private QuestionDtos.Save request(String rights) {
        String suffix = UUID.randomUUID().toString();
        Long pointId = jdbc.queryForObject("SELECT id FROM zhi_shi_dian WHERE ke_mu_id=1 AND zhuang_tai='ACTIVE' LIMIT 1", Long.class);
        List<QuestionDtos.Source> sources = List.of("QUESTION", "ANSWER", "STANDARD_ANALYSIS").stream().map(part -> new QuestionDtos.Source(part, "TEACHER_CREATED", "匿名测试", rights, null, null, null, null, null, "测试授权")).toList();
        return new QuestionDtos.Save(1L, "SINGLE_CHOICE", "ONLINE_PRACTICE", "审核流程题" + suffix, "{\"schemaVersion\":1,\"type\":\"SINGLE_CHOICE\",\"optionLabels\":[\"A\"]}", 2, null, true, List.of(new QuestionDtos.Option("A", "正确", true), new QuestionDtos.Option("B", "错误", false)), "标准解析", List.of(pointId), sources);
    }
}
