package com.neu.riketiku.xueshenglianxi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class StudentPracticeIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private StudentPracticeService service;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @Transactional
    void createsFrozenPublishedQuestionsWithoutLeakingAnswers() {
        long userId = student("freeze");
        question("SINGLE_CHOICE", "PUBLISHED", 1, "A");
        var session = service.create(userId, new StudentPracticeDtos.CreateRequest(1L, null, List.of("SINGLE_CHOICE"), null, 1));

        assertThat(session.status()).isEqualTo("CREATED");
        assertThat(session.questions()).hasSize(1);
        assertThat(session.questions().getFirst().options()).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lian_xi_ti_mu WHERE lian_xi_hui_hua_id=?", Integer.class, session.id())).isEqualTo(1);
        assertThat(session.questions().getFirst().toString()).doesNotContain("correctAnswer", "标准解析");
    }

    @Test
    @Transactional
    void gradesSingleMultipleAndFillBlankThenCreatesWrongQuestion() {
        long userId = student("mixed");
        question("SINGLE_CHOICE", "PUBLISHED", 1, "A");
        question("MULTIPLE_CHOICE", "PUBLISHED", 2, "AB");
        question("FILL_BLANK", "PUBLISHED", 3, "填空答案");
        var session = service.create(userId, new StudentPracticeDtos.CreateRequest(1L, null,
                List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "FILL_BLANK"), null, 3));
        var answers = session.questions().stream().map(question -> new StudentPracticeDtos.Answer(question.practiceQuestionId(), answerFor(question.questionType()), 10)).toList();

        var result = service.submit(userId, session.id(), new StudentPracticeDtos.SubmitRequest(answers));
        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.correctCount()).isEqualTo(2);
        assertThat(result.questions()).extracting(StudentPracticeDtos.ResultQuestion::standardAnalysis).allMatch(value -> value.contains("标准解析"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuo_ti_ji_lu", Integer.class)).isEqualTo(1);
    }

    @Test
    @Transactional
    void rejectsInvalidOptionAndRollsBackWholeSubmission() {
        long userId = student("rollback");
        question("MULTIPLE_CHOICE", "PUBLISHED", 2, "AB");
        var session = service.create(userId, new StudentPracticeDtos.CreateRequest(1L, null, List.of("MULTIPLE_CHOICE"), null, 1));
        var answer = new StudentPracticeDtos.Answer(session.questions().getFirst().practiceQuestionId(), JsonNodeFactory.instance.arrayNode().add("Z"), null);

        assertThatThrownBy(() -> service.submit(userId, session.id(), new StudentPracticeDtos.SubmitRequest(List.of(answer))))
                .isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("无效选项");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM xue_sheng_da_ti", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT zhuang_tai FROM lian_xi_hui_hua WHERE id=?", String.class, session.id())).isEqualTo("CREATED");
    }

    @Test
    @Transactional
    void rejectsDuplicateSubmissionAndCrossStudentAccess() {
        long owner = student("owner");
        long other = student("other");
        question("SINGLE_CHOICE", "PUBLISHED", 1, "A");
        var session = service.create(owner, new StudentPracticeDtos.CreateRequest(1L, null, List.of("SINGLE_CHOICE"), null, 1));
        var answer = new StudentPracticeDtos.Answer(session.questions().getFirst().practiceQuestionId(), JsonNodeFactory.instance.textNode("A"), null);
        service.submit(owner, session.id(), new StudentPracticeDtos.SubmitRequest(List.of(answer)));

        assertThatThrownBy(() -> service.submit(owner, session.id(), new StudentPracticeDtos.SubmitRequest(List.of(answer))))
                .isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("已经提交");
        assertThatThrownBy(() -> service.session(other, session.id()))
                .isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("不属于当前学生");
    }

    @Test
    @Transactional
    void accumulatesWrongQuestionAndMarksMasteredAfterTwoCorrectAttempts() {
        long userId = student("wrong");
        long questionId = question("SINGLE_CHOICE", "PUBLISHED", 1, "A");
        submitSingle(userId, "B");
        submitSingle(userId, "A");
        submitSingle(userId, "A");

        assertThat(jdbc.queryForObject("SELECT cuo_wu_ci_shu FROM cuo_ti_ji_lu WHERE xue_sheng_id=(SELECT id FROM xue_sheng_dang_an WHERE yong_hu_id=?) AND ti_mu_id=?", Integer.class, userId, questionId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT lian_xu_zheng_que_ci_shu FROM cuo_ti_ji_lu WHERE ti_mu_id=?", Integer.class, questionId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT zhuang_tai FROM cuo_ti_ji_lu WHERE ti_mu_id=?", String.class, questionId)).isEqualTo("MASTERED");
    }

    @Test
    @Transactional
    void appliesSubjectKnowledgePointDifficultyAndPublishedFilters() {
        long userId = student("filter");
        long point = jdbc.queryForObject("SELECT id FROM zhi_shi_dian WHERE ke_mu_id=1 AND zhuang_tai='ACTIVE' LIMIT 1", Long.class);
        question("SINGLE_CHOICE", "PUBLISHED", 1, "A");
        question("SINGLE_CHOICE", "PENDING", 1, "A");
        assertThat(service.create(userId, new StudentPracticeDtos.CreateRequest(1L, List.of(point), List.of("SINGLE_CHOICE"), 1, 1)).questions()).hasSize(1);
        assertThatThrownBy(() -> service.create(userId, new StudentPracticeDtos.CreateRequest(1L, null, List.of("SUBJECTIVE"), null, 1)))
                .isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("只支持");
    }

    private void submitSingle(long userId, String label) {
        var session = service.create(userId, new StudentPracticeDtos.CreateRequest(1L, null, List.of("SINGLE_CHOICE"), null, 1));
        var answer = new StudentPracticeDtos.Answer(session.questions().getFirst().practiceQuestionId(), JsonNodeFactory.instance.textNode(label), 1);
        service.submit(userId, session.id(), new StudentPracticeDtos.SubmitRequest(List.of(answer)));
    }

    private com.fasterxml.jackson.databind.JsonNode answerFor(String type) {
        return switch (type) {
            case "SINGLE_CHOICE" -> JsonNodeFactory.instance.textNode("B");
            case "MULTIPLE_CHOICE" -> JsonNodeFactory.instance.arrayNode().add("B").add("A");
            default -> JsonNodeFactory.instance.arrayNode().add("填空答案");
        };
    }

    private long student(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao,shi_fou_shou_ci_deng_lu) VALUES (?,?,0)",
                prefix + "_" + suffix, "x".repeat(60));
        long userId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO xue_sheng_dang_an(yong_hu_id,xue_hao,xing_ming,nian_ji) VALUES (?,?,?,?)",
                userId, "S" + suffix, "匿名学生", "高一");
        return userId;
    }

    private long question(String type, String status, int difficulty, String answer) {
        String suffix = UUID.randomUUID().toString();
        String correctAnswer = switch (type) {
            case "MULTIPLE_CHOICE" -> "{\"schemaVersion\":1,\"type\":\"MULTIPLE_CHOICE\",\"optionLabels\":[\"A\",\"B\"]}";
            case "FILL_BLANK" -> "{\"schemaVersion\":1,\"type\":\"FILL_BLANK\",\"blanks\":[{\"index\":1,\"acceptedAnswers\":[\"填空答案\"]}]}";
            default -> "{\"schemaVersion\":1,\"type\":\"SINGLE_CHOICE\",\"optionLabels\":[\"A\"]}";
        };
        jdbc.update("""
                INSERT INTO ti_mu(ke_mu_id,ti_mu_lei_xing,shi_yong_mo_shi,ti_gan,zheng_que_da_an,nan_du,shi_fou_ke_zi_dong_pan_fen,zhuang_tai,nei_rong_ha_xi)
                VALUES (1,?,'ONLINE_PRACTICE',?,?,?,1,?,?)
                """, type, "练习测试题" + suffix, correctAnswer, difficulty, status, UUID.randomUUID().toString().replace("-", ""));
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (!"FILL_BLANK".equals(type)) {
            jdbc.update("INSERT INTO ti_mu_xuan_xiang(ti_mu_id,xuan_xiang_biao_shi,xuan_xiang_nei_rong,shi_fou_zheng_que,pai_xu) VALUES (?, 'A','选项A',1,1),(?, 'B','选项B',0,2)", id, id);
        }
        jdbc.update("INSERT INTO ti_mu_jie_xi(ti_mu_id,jie_xi_lei_xing,jie_xi_nei_rong,ban_ben_hao,zhuang_tai) VALUES (?,'STANDARD','标准解析',1,?)", id, status);
        long point = jdbc.queryForObject("SELECT id FROM zhi_shi_dian WHERE ke_mu_id=1 AND zhuang_tai='ACTIVE' LIMIT 1", Long.class);
        jdbc.update("INSERT INTO ti_mu_zhi_shi_dian(ti_mu_id,zhi_shi_dian_id,shi_fou_zhu_yao,pai_xu) VALUES (?,?,1,1)", id, point);
        return id;
    }
}
