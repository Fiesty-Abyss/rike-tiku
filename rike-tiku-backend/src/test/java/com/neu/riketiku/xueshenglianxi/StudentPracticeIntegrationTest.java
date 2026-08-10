package com.neu.riketiku.xueshenglianxi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import tools.jackson.databind.node.JsonNodeFactory;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import com.neu.riketiku.tiku.fujian.QuestionAttachmentContentService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class StudentPracticeIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private StudentPracticeService service;
    @Autowired private QuestionAttachmentContentService attachmentContentService;
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
    void shufflesEligiblePoolBeforeSelectingPracticeQuestions() {
        long userId = student("random_pool");
        for (int index = 0; index < 8; index++) {
            question("SINGLE_CHOICE", "PUBLISHED", 1, "A");
        }

        Set<Set<Long>> selections = new HashSet<>();
        for (int attempt = 0; attempt < 8; attempt++) {
            var session = service.create(userId, new StudentPracticeDtos.CreateRequest(
                    1L, null, List.of("SINGLE_CHOICE"), 1, 5));
            List<Long> questionIds = jdbc.queryForList("""
                    SELECT ti_mu_id FROM lian_xi_ti_mu
                    WHERE lian_xi_hui_hua_id=? ORDER BY ti_mu_shun_xu
                    """, Long.class, session.id());
            assertThat(questionIds).hasSize(5).doesNotHaveDuplicates();
            selections.add(Set.copyOf(questionIds));
        }

        assertThat(selections).hasSizeGreaterThan(1);
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

    @Test
    @Transactional
    void skipsUnfreezableCandidatesAndUsesLaterCompleteQuestions() {
        long userId = student("pool");
        long validOne = question("SINGLE_CHOICE", "PUBLISHED", 1, "A");
        long validTwo = question("SINGLE_CHOICE", "PUBLISHED", 1, "A");
        long missingAnalysis = question("SINGLE_CHOICE", "PUBLISHED", 1, "A");
        long disabledKnowledgePoint = question("SINGLE_CHOICE", "PUBLISHED", 1, "A");
        long attached = question("SINGLE_CHOICE", "PUBLISHED", 1, "A");
        long marker = question("SINGLE_CHOICE", "PUBLISHED", 1, "A");
        long uniquePoint = uniqueKnowledgePoint();
        for (long questionId : List.of(validOne, validTwo, missingAnalysis, disabledKnowledgePoint, attached, marker)) {
            jdbc.update("DELETE FROM ti_mu_zhi_shi_dian WHERE ti_mu_id=?", questionId);
            jdbc.update("INSERT INTO ti_mu_zhi_shi_dian(ti_mu_id,zhi_shi_dian_id,shi_fou_zhu_yao,pai_xu) VALUES (?,?,1,1)", questionId, uniquePoint);
        }
        jdbc.update("UPDATE ti_mu_jie_xi SET zhuang_tai='DRAFT' WHERE ti_mu_id=?", missingAnalysis);
        jdbc.update("UPDATE ti_mu_zhi_shi_dian SET yi_shan_chu=1 WHERE ti_mu_id=?", disabledKnowledgePoint);
        jdbc.update("""
                INSERT INTO ti_mu_fu_jian(ti_mu_id,guan_lian_wei_zhi,fu_jian_lei_xing,yuan_shi_wen_jian_ming,xiang_dui_lu_jing,
                    nei_rong_ha_xi,pai_xu,zhuang_tai)
                VALUES (?,'QUESTION','IMAGE','anonymous.png','sources/anonymous.png',?,1,'ACTIVE')
                """, attached, "a".repeat(64));
        jdbc.update("UPDATE ti_mu SET ti_gan=? WHERE id=?", "含〔图片对象 I001〕标记", marker);

        var session = service.create(userId, new StudentPracticeDtos.CreateRequest(1L, List.of(uniquePoint), List.of("SINGLE_CHOICE"), 1, 2));

        assertThat(session.questions()).extracting(question -> jdbc.queryForObject(
                "SELECT ti_mu_id FROM lian_xi_ti_mu WHERE id=?", Long.class, question.practiceQuestionId()))
                .containsExactlyInAnyOrder(validOne, validTwo);
        assertThatThrownBy(() -> service.create(userId, new StudentPracticeDtos.CreateRequest(1L, List.of(uniquePoint),
                List.of("SINGLE_CHOICE"), 1, 3))).isInstanceOf(RenZhengYeWuYiChang.class)
                .hasMessageContaining("已发布题目不足");
    }

    @Test
    @Transactional
    void protectsUnsubmittedResultsAndCrossStudentWrongQuestionDetails() {
        long owner = student("detail_owner");
        long other = student("detail_other");
        long questionId = question("SINGLE_CHOICE", "PUBLISHED", 1, "A");
        var session = service.create(owner, new StudentPracticeDtos.CreateRequest(1L, null, List.of("SINGLE_CHOICE"), null, 1));

        assertThatThrownBy(() -> service.result(owner, session.id())).isInstanceOf(RenZhengYeWuYiChang.class)
                .hasMessageContaining("尚未提交");
        service.submit(owner, session.id(), new StudentPracticeDtos.SubmitRequest(List.of(
                new StudentPracticeDtos.Answer(session.questions().getFirst().practiceQuestionId(), JsonNodeFactory.instance.textNode(" B "), 13))));
        assertThatThrownBy(() -> service.result(other, session.id())).isInstanceOf(RenZhengYeWuYiChang.class)
                .hasMessageContaining("不属于当前学生");
        assertThatThrownBy(() -> service.wrongQuestion(other, questionId)).isInstanceOf(RenZhengYeWuYiChang.class)
                .hasMessageContaining("错题不存在");
    }

    @Test
    @Transactional
    void rejectsWrongQuestionAttachmentFromAnotherQuestion() {
        long userId = student("wrong_attachment_scope");
        long ownedQuestionId = question("SINGLE_CHOICE", "PUBLISHED", 1, "A");
        var session = service.create(userId, new StudentPracticeDtos.CreateRequest(1L, null, List.of("SINGLE_CHOICE"), null, 1));
        long otherQuestionId = question("SINGLE_CHOICE", "PUBLISHED", 1, "A");
        var selected = session.questions().getFirst();
        service.submit(userId, session.id(), new StudentPracticeDtos.SubmitRequest(List.of(
                new StudentPracticeDtos.Answer(selected.practiceQuestionId(), JsonNodeFactory.instance.textNode("B"), 1))));
        jdbc.update("""
                INSERT INTO ti_mu_fu_jian(ti_mu_id,guan_lian_wei_zhi,fu_jian_lei_xing,yuan_shi_wen_jian_ming,xiang_dui_lu_jing,nei_rong_ha_xi,pai_xu,zhuang_tai)
                VALUES (?,'QUESTION','IMAGE','other.png','images/other.png',?,1,'ACTIVE')
                """, otherQuestionId, "a".repeat(64));
        long otherAttachmentId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        assertThatThrownBy(() -> attachmentContentService.wrongQuestion(userId, ownedQuestionId, otherAttachmentId))
                .isInstanceOf(RenZhengYeWuYiChang.class)
                .hasMessageContaining("不存在、不可用或无访问权限");
    }

    @Test
    void rollsBackAnswerFactsAndWrongQuestionWhenResultInsertFails() {
        long userId = student("result_rollback");
        long questionId = question("SINGLE_CHOICE", "PUBLISHED", 1, "A");
        var session = service.create(userId, new StudentPracticeDtos.CreateRequest(1L, null, List.of("SINGLE_CHOICE"), null, 1));
        try {
            jdbc.update("INSERT INTO xue_xi_jie_guo(lian_xi_hui_hua_id,zong_ti_shu,zheng_que_shu,zong_de_fen,ti_jiao_shi_jian) VALUES (?,1,0,0,NOW(3))", session.id());
            assertThatThrownBy(() -> service.submit(userId, session.id(), new StudentPracticeDtos.SubmitRequest(List.of(
                    new StudentPracticeDtos.Answer(session.questions().getFirst().practiceQuestionId(), JsonNodeFactory.instance.textNode("B"), 1)))))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM xue_sheng_da_ti da JOIN lian_xi_ti_mu lt ON lt.id=da.lian_xi_ti_mu_id WHERE lt.lian_xi_hui_hua_id=?", Integer.class, session.id())).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuo_ti_ji_lu WHERE zui_jin_da_ti_id IN (SELECT da.id FROM xue_sheng_da_ti da JOIN lian_xi_ti_mu lt ON lt.id=da.lian_xi_ti_mu_id WHERE lt.lian_xi_hui_hua_id=?)", Integer.class, session.id())).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM xue_xi_jie_guo WHERE lian_xi_hui_hua_id=?", Integer.class, session.id())).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT zhuang_tai FROM lian_xi_hui_hua WHERE id=?", String.class, session.id())).isEqualTo("CREATED");
        } finally {
            jdbc.update("DELETE FROM xue_xi_jie_guo WHERE lian_xi_hui_hua_id=?", session.id());
            jdbc.update("DELETE FROM lian_xi_ti_mu WHERE lian_xi_hui_hua_id=?", session.id());
            jdbc.update("DELETE FROM lian_xi_hui_hua WHERE id=?", session.id());
            jdbc.update("DELETE FROM ti_mu_zhi_shi_dian WHERE ti_mu_id=?", questionId);
            jdbc.update("DELETE FROM ti_mu_jie_xi WHERE ti_mu_id=?", questionId);
            jdbc.update("DELETE FROM ti_mu_xuan_xiang WHERE ti_mu_id=?", questionId);
            jdbc.update("DELETE FROM ti_mu WHERE id=?", questionId);
            jdbc.update("DELETE FROM xue_sheng_dang_an WHERE yong_hu_id=?", userId);
            jdbc.update("DELETE FROM yong_hu WHERE id=?", userId);
        }
    }

    @Test
    @Transactional
    void enforcesAnswerNormalizationAndElapsedTimeBoundary() {
        long userId = student("answer_rules");
        question("SINGLE_CHOICE", "PUBLISHED", 1, "A");
        question("MULTIPLE_CHOICE", "PUBLISHED", 1, "AB");
        question("FILL_BLANK", "PUBLISHED", 1, "填空答案");
        var session = service.create(userId, new StudentPracticeDtos.CreateRequest(1L, null,
                List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "FILL_BLANK"), null, 3));
        var answers = session.questions().stream().map(item -> switch (item.questionType()) {
            case "SINGLE_CHOICE" -> new StudentPracticeDtos.Answer(item.practiceQuestionId(), JsonNodeFactory.instance.textNode(" a "), 2);
            case "MULTIPLE_CHOICE" -> new StudentPracticeDtos.Answer(item.practiceQuestionId(), JsonNodeFactory.instance.arrayNode().add(" b ").add("A").add("A"), 3);
            default -> new StudentPracticeDtos.Answer(item.practiceQuestionId(), JsonNodeFactory.instance.arrayNode().add("填空答案"), 4);
        }).toList();
        assertThat(service.submit(userId, session.id(), new StudentPracticeDtos.SubmitRequest(answers)).correctCount()).isEqualTo(3);

        var overLimit = service.create(userId, new StudentPracticeDtos.CreateRequest(1L, null, List.of("SINGLE_CHOICE"), null, 1));
        assertThatThrownBy(() -> service.submit(userId, overLimit.id(), new StudentPracticeDtos.SubmitRequest(List.of(
                new StudentPracticeDtos.Answer(overLimit.questions().getFirst().practiceQuestionId(), JsonNodeFactory.instance.textNode("A"), 86401)))))
                .isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("86400");
    }

    @Test
    @Transactional
    void gradesMultiBlankCaseSensitiveAndFullWidthAnswersWithoutChangingBlankOrder() {
        long userId = student("blank_rules");
        long questionId = question("FILL_BLANK", "PUBLISHED", 1, "填空答案");
        jdbc.update("UPDATE ti_mu SET zheng_que_da_an=? WHERE id=?", """
                {"schemaVersion":1,"type":"FILL_BLANK","blanks":[
                  {"index":1,"acceptedAnswers":["Ａ，Ｂ","A,B"]},
                  {"index":2,"acceptedAnswers":["Case"],"caseSensitive":true}
                ]}
                """, questionId);
        var session = service.create(userId, new StudentPracticeDtos.CreateRequest(1L, null, List.of("FILL_BLANK"), null, 1));
        var valid = new StudentPracticeDtos.Answer(session.questions().getFirst().practiceQuestionId(),
                JsonNodeFactory.instance.arrayNode().add("A，B").add("Case"), 5);
        assertThat(service.submit(userId, session.id(), new StudentPracticeDtos.SubmitRequest(List.of(valid))).correctCount()).isEqualTo(1);

        var wrongCase = service.create(userId, new StudentPracticeDtos.CreateRequest(1L, null, List.of("FILL_BLANK"), null, 1));
        var wrong = new StudentPracticeDtos.Answer(wrongCase.questions().getFirst().practiceQuestionId(),
                JsonNodeFactory.instance.arrayNode().add("A,B").add("case"), 5);
        assertThat(service.submit(userId, wrongCase.id(), new StudentPracticeDtos.SubmitRequest(List.of(wrong))).correctCount()).isZero();
    }

    @Test
    @Transactional
    void rejectsUserWithoutActiveStudentProfile() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao,shi_fou_shou_ci_deng_lu) VALUES (?,?,0)",
                "no_profile_" + suffix, "x".repeat(60));
        long userId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        assertThatThrownBy(() -> service.options(userId, null)).isInstanceOf(RenZhengYeWuYiChang.class)
                .hasMessageContaining("没有有效学生档案");
    }

    @Test
    @Transactional
    void reportsAvailabilityAndCreatesRuleBasedSimilarPracticeWithoutCurrentQuestion() {
        long userId = student("similar");
        long point = uniqueKnowledgePoint();
        long reference = question("MULTIPLE_CHOICE", "PUBLISHED", 2, "AB");
        long sameType = question("MULTIPLE_CHOICE", "PUBLISHED", 3, "AB");
        long otherType = question("SINGLE_CHOICE", "PUBLISHED", 2, "A");
        for (long questionId : List.of(reference, sameType, otherType)) {
            jdbc.update("DELETE FROM ti_mu_zhi_shi_dian WHERE ti_mu_id=?", questionId);
            jdbc.update("INSERT INTO ti_mu_zhi_shi_dian(ti_mu_id,zhi_shi_dian_id,shi_fou_zhu_yao,pai_xu) VALUES (?,?,1,1)", questionId, point);
        }

        var request = new StudentPracticeDtos.CreateRequest(1L, List.of(point), null, null, 1, reference);
        assertThat(service.availability(userId, request).availableCount()).isEqualTo(2);
        var session = service.create(userId, request);

        assertThat(session.questions().getFirst().questionId()).isEqualTo(sameType);
        assertThat(session.questions().getFirst().questionId()).isNotEqualTo(reference);
    }

    @Test
    @Transactional
    void filtersNewWrongQuestionByRealSubjectCodeWithoutDependingOnDatabaseIds() {
        long userId = student("wrong_subject");
        long biologyQuestion = questionForSubject(3L, "SINGLE_CHOICE", 1);
        long biologyPoint = jdbc.queryForObject("SELECT zhi_shi_dian_id FROM ti_mu_zhi_shi_dian WHERE ti_mu_id=?", Long.class, biologyQuestion);
        var session = service.create(userId, new StudentPracticeDtos.CreateRequest(3L, List.of(biologyPoint), List.of("SINGLE_CHOICE"), 1, 1));
        service.submit(userId, session.id(), new StudentPracticeDtos.SubmitRequest(List.of(
                new StudentPracticeDtos.Answer(session.questions().getFirst().practiceQuestionId(), JsonNodeFactory.instance.textNode("B"), 1))));

        assertThat(service.wrongQuestions(userId, "BIOLOGY")).extracting(StudentPracticeDtos.WrongQuestionItem::questionId)
                .contains(biologyQuestion);
        assertThat(service.wrongQuestions(userId, "PHYSICS")).isEmpty();
        assertThat(service.wrongQuestions(userId, null)).extracting(StudentPracticeDtos.WrongQuestionItem::subjectCode)
                .containsOnly("BIOLOGY");
    }

    private void submitSingle(long userId, String label) {
        var session = service.create(userId, new StudentPracticeDtos.CreateRequest(1L, null, List.of("SINGLE_CHOICE"), null, 1));
        var answer = new StudentPracticeDtos.Answer(session.questions().getFirst().practiceQuestionId(), JsonNodeFactory.instance.textNode(label), 1);
        service.submit(userId, session.id(), new StudentPracticeDtos.SubmitRequest(List.of(answer)));
    }

    private long uniqueKnowledgePoint() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("""
                INSERT INTO zhi_shi_dian(ke_mu_id,zhi_shi_dian_ming_cheng,wan_zheng_lu_jing,ceng_ji,pai_xu,zhuang_tai)
                VALUES (1,?,?,1,999,'ACTIVE')
                """, "练习过滤" + suffix, "匿名练习>" + suffix);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private tools.jackson.databind.JsonNode answerFor(String type) {
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

    private long questionForSubject(long subjectId, String type, int difficulty) {
        long id = question(type, "PUBLISHED", difficulty, "A");
        jdbc.update("DELETE FROM ti_mu_zhi_shi_dian WHERE ti_mu_id=?", id);
        jdbc.update("UPDATE ti_mu SET ke_mu_id=? WHERE id=?", subjectId, id);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO zhi_shi_dian(ke_mu_id,zhi_shi_dian_ming_cheng,wan_zheng_lu_jing,ceng_ji,pai_xu,zhuang_tai) VALUES (?,?,?,1,999,'ACTIVE')",
                subjectId, "错题筛选" + suffix, "错题筛选>" + suffix);
        long point = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO ti_mu_zhi_shi_dian(ti_mu_id,zhi_shi_dian_id,shi_fou_zhu_yao,pai_xu) VALUES (?,?,1,1)", id, point);
        return id;
    }
}
