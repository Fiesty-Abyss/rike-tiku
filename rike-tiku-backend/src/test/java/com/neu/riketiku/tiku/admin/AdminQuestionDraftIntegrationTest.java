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
class AdminQuestionDraftIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private QuestionAdminService service;
    @Autowired private JdbcTemplate jdbc;

    @Test @Transactional
    void createsSingleChoiceDraftWithAllRelations() {
        var detail = service.create(request("SINGLE_CHOICE", "ONLINE_PRACTICE", true, "AUTHORIZED"));
        assertThat(detail.question().status()).isEqualTo("DRAFT");
        assertThat(detail.options()).hasSize(2);
        assertThat(detail.knowledgePoints()).isNotEmpty();
        assertThat(detail.sources()).hasSize(3);
    }

    @Test @Transactional
    void rejectsInvalidChoiceAndDuplicateContent() {
        var valid = request("SINGLE_CHOICE", "ONLINE_PRACTICE", true, "AUTHORIZED");
        service.create(valid);
        assertThatThrownBy(() -> service.create(valid)).isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("重复");
        var invalid = request("SINGLE_CHOICE", "ONLINE_PRACTICE", false, "AUTHORIZED");
        assertThatThrownBy(() -> service.create(invalid)).isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("正确");
    }

    @Test @Transactional
    void supportsSubjectiveTopicLearningOnly() {
        var detail = service.create(request("SUBJECTIVE", "TOPIC_LEARNING", false, "AUTHORIZED"));
        assertThat(detail.question().questionType()).isEqualTo("SUBJECTIVE");
        assertThatThrownBy(() -> service.create(request("SUBJECTIVE", "ONLINE_PRACTICE", false, "AUTHORIZED"))).isInstanceOf(RenZhengYeWuYiChang.class);
    }

    @Test @Transactional
    void supportsMultipleChoiceAndFillBlankDrafts() {
        var multiple = request("MULTIPLE_CHOICE", "ONLINE_PRACTICE", true, "AUTHORIZED");
        multiple = new QuestionDtos.Save(multiple.subjectId(), multiple.questionType(), multiple.usageMode(), multiple.stem(),
                "{\"schemaVersion\":1,\"type\":\"MULTIPLE_CHOICE\",\"optionLabels\":[\"A\",\"B\"]}", multiple.difficulty(), multiple.difficultyDescription(), multiple.autoGradable(),
                List.of(new QuestionDtos.Option("A", "选项A", true), new QuestionDtos.Option("B", "选项B", true)), multiple.standardAnalysis(), multiple.knowledgePointIds(), multiple.sources());
        assertThat(service.create(multiple).options()).extracting(QuestionDtos.Option::correct).containsExactly(true, true);
        assertThat(service.create(request("FILL_BLANK", "ONLINE_PRACTICE", true, "AUTHORIZED")).question().questionType()).isEqualTo("FILL_BLANK");
    }

    @Test @Transactional
    void rejectsInvalidAnswerAndKnowledgePoint() {
        var baseAnswer = request("SINGLE_CHOICE", "ONLINE_PRACTICE", true, "AUTHORIZED");
        var invalidAnswer = new QuestionDtos.Save(baseAnswer.subjectId(), baseAnswer.questionType(), baseAnswer.usageMode(), baseAnswer.stem(),
                "{\"schemaVersion\":1,\"type\":\"SINGLE_CHOICE\",\"optionLabels\":[\"Z\"]}", baseAnswer.difficulty(), baseAnswer.difficultyDescription(), baseAnswer.autoGradable(), baseAnswer.options(), baseAnswer.standardAnalysis(), baseAnswer.knowledgePointIds(), baseAnswer.sources());
        assertThatThrownBy(() -> service.create(invalidAnswer)).isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("正确答案");
        var basePoint = request("SINGLE_CHOICE", "ONLINE_PRACTICE", true, "AUTHORIZED");
        var invalidPoint = new QuestionDtos.Save(basePoint.subjectId(), basePoint.questionType(), basePoint.usageMode(), basePoint.stem(), basePoint.correctAnswer(), basePoint.difficulty(), basePoint.difficultyDescription(), basePoint.autoGradable(), basePoint.options(), basePoint.standardAnalysis(), List.of(999999L), basePoint.sources());
        assertThatThrownBy(() -> service.create(invalidPoint)).isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("知识点");
    }

    @Test @Transactional
    void updatesOnlyDraftAndRecalculatesHash() {
        var created = service.create(request("SINGLE_CHOICE", "ONLINE_PRACTICE", true, "AUTHORIZED"));
        String before = jdbc.queryForObject("SELECT nei_rong_ha_xi FROM ti_mu WHERE id=?", String.class, created.question().id());
        var baseChanged = request("SINGLE_CHOICE", "ONLINE_PRACTICE", true, "AUTHORIZED");
        var changed = new QuestionDtos.Save(baseChanged.subjectId(), baseChanged.questionType(), baseChanged.usageMode(), "更新后题干" + UUID.randomUUID(), baseChanged.correctAnswer(), baseChanged.difficulty(), baseChanged.difficultyDescription(), baseChanged.autoGradable(), baseChanged.options(), baseChanged.standardAnalysis(), baseChanged.knowledgePointIds(), baseChanged.sources());
        service.update(created.question().id(), changed);
        assertThat(jdbc.queryForObject("SELECT nei_rong_ha_xi FROM ti_mu WHERE id=?", String.class, created.question().id())).isNotEqualTo(before);
        service.transition(created.question().id(), "SUBMITTED", "DRAFT", "PENDING", null, null);
        assertThatThrownBy(() -> service.update(created.question().id(), changed)).isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("状态");
    }

    private QuestionDtos.Save request(String type, String mode, boolean firstCorrect, String rights) {
        String suffix = UUID.randomUUID().toString();
        Long pointId = jdbc.queryForObject("SELECT id FROM zhi_shi_dian WHERE ke_mu_id=1 AND zhuang_tai='ACTIVE' LIMIT 1", Long.class);
        String answer = type.equals("FILL_BLANK")
                ? "{\"schemaVersion\":1,\"blanks\":[{\"acceptedAnswers\":[\"答案\"]}]}"
                : type.equals("SUBJECTIVE")
                ? "{\"schemaVersion\":1,\"type\":\"SUBJECTIVE\"}"
                : "{\"schemaVersion\":1,\"type\":\"" + type + "\",\"optionLabels\":[\"A\"]}";
        return new QuestionDtos.Save(1L, type, mode, "测试题干" + suffix, answer, 2, "测试", !"SUBJECTIVE".equals(type),
                type.equals("SUBJECTIVE") ? List.of() : List.of(new QuestionDtos.Option("A", "选项A", firstCorrect), new QuestionDtos.Option("B", "选项B", !firstCorrect && type.equals("MULTIPLE_CHOICE"))),
                "标准解析", List.of(pointId), sources(rights));
    }
    private List<QuestionDtos.Source> sources(String rights) { return List.of("QUESTION", "ANSWER", "STANDARD_ANALYSIS").stream().map(part -> new QuestionDtos.Source(part, "TEACHER_CREATED", "匿名测试", rights, null, null, null, null, null, "测试授权")).toList(); }
}
