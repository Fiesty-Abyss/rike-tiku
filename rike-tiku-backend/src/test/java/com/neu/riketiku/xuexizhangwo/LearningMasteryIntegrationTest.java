package com.neu.riketiku.xuexizhangwo;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.neu.riketiku.demo.DemoDataService;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LearningMasteryIntegrationTest extends AdminQuestionIntegrationTestSupport {
    private final HttpClient http = HttpClient.newHttpClient();

    @Autowired private DemoDataService demo;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;
    @LocalServerPort private int port;

    @Test
    void noDataInsufficientSubjectAndStudentIsolationWork() throws Exception {
        demo.seed();
        useNonDefaultPasswords();
        try {
            long student = student("DEMO_199_01");
            long otherStudent = student("DEMO_199_02");
            long point = point(1, 0);
            String token = login("demo_199_01", "STUDENT");
            String otherToken = login("demo_199_02", "STUDENT");
            String teacherToken = login("demo_physics_admin", "TEACHER");

            String empty = get("/api/v1/student/learning-summary?subjectId=1", token).body();
            assertThat(empty).contains("\"totalAnsweredCount\":0", "\"overallAccuracy\":null", "NOT_STARTED");
            Integer activePointCount = jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM zhi_shi_dian
                    WHERE ke_mu_id = 1
                      AND zhuang_tai = 'ACTIVE'
                      AND yi_shan_chu = 0
                    """, Integer.class);
            assertThat(JsonPath.<Number>read(empty, "$.overall.totalKnowledgePointCount").intValue())
                    .isEqualTo(activePointCount);
            assertThat(JsonPath.<List<?>>read(empty, "$.recommendations")).hasSize(3);
            assertThat(empty).contains("该知识点尚未开始练习。", "\"count\":5", "\"subjectId\":1");
            assertThat(JsonPath.<List<Number>>read(empty, "$.recommendations[*].knowledgePointId"))
                    .extracting(Number::longValue)
                    .containsExactlyInAnyOrder(point(1, 0), point(1, 1), point(1, 2));

            addAnswer(student, 1, point, question(point, 0), true, true);
            addAnswer(student, 1, point, question(point, 1), true, true);
            String insufficient = get("/api/v1/student/learning-summary?subjectId=1", token).body();
            assertPoint(insufficient, point, 2, 2, "INSUFFICIENT");
            assertThat(insufficient).contains("当前练习样本较少，建议继续练习以确认掌握情况。");

            assertThat(get("/api/v1/student/learning-summary?subjectId=1", otherToken).body())
                    .contains("\"totalAnsweredCount\":0");
            assertThat(get("/api/v1/student/learning-summary?subjectId=2", token).body())
                    .contains("\"totalAnsweredCount\":0").doesNotContain("\"knowledgePointId\":" + point + ",");
            assertThat(get("/api/v1/student/learning-summary?subjectId=1", teacherToken).statusCode()).isEqualTo(403);
            assertThat(otherStudent).isPositive();
        } finally {
            demo.clean();
        }
    }

    @Test
    void masteryLevelsWrongStatesAndRecommendationPriorityAreDeterministic() throws Exception {
        demo.seed();
        useNonDefaultPasswords();
        try {
            long student = student("DEMO_199_01");
            long weakPoint = point(1, 0);
            long improvingPoint = point(1, 1);
            long masteredPoint = point(1, 2);
            long activeWrongAnswer = 0;

            for (int index = 0; index < 4; index++) {
                long answer = addAnswer(student, 1, weakPoint, question(weakPoint, index), index == 0, true);
                if (index == 1) activeWrongAnswer = answer;
            }
            upsertWrong(student, question(weakPoint, 1), activeWrongAnswer, "NEW");
            for (int index = 0; index < 4; index++) {
                addAnswer(student, 1, improvingPoint, question(improvingPoint, index), index < 3, true);
            }
            long masteredWrongAnswer = 0;
            for (int index = 0; index < 5; index++) {
                long answer = addAnswer(student, 1, masteredPoint, question(masteredPoint, index), index < 4, true);
                if (index == 4) masteredWrongAnswer = answer;
            }
            upsertWrong(student, question(masteredPoint, 4), masteredWrongAnswer, "MASTERED");

            String token = login("demo_199_01", "STUDENT");
            String summary = get("/api/v1/student/learning-summary?subjectId=1", token).body();
            assertPoint(summary, weakPoint, 4, 1, "WEAK");
            assertPoint(summary, improvingPoint, 4, 3, "IMPROVING");
            assertPoint(summary, masteredPoint, 5, 4, "MASTERED");
            assertThat(JsonPath.<List<?>>read(summary, "$.recommendations")).hasSizeLessThanOrEqualTo(3);
            assertThat(JsonPath.<Number>read(summary, "$.recommendations[0].knowledgePointId").longValue())
                    .isEqualTo(weakPoint);
            assertThat(JsonPath.<String>read(summary, "$.recommendations[0].reason"))
                    .isEqualTo("该知识点仍有未完成复习的错题。");

            jdbc.update("UPDATE cuo_ti_ji_lu SET zhuang_tai='NEW' WHERE xue_sheng_id=? AND ti_mu_id=?",
                    student, question(masteredPoint, 4));
            assertPoint(get("/api/v1/student/learning-summary?subjectId=1", token).body(), masteredPoint, 5, 4, "IMPROVING");
            jdbc.update("UPDATE cuo_ti_ji_lu SET zhuang_tai='REVIEWING' WHERE xue_sheng_id=? AND ti_mu_id=?",
                    student, question(masteredPoint, 4));
            assertPoint(get("/api/v1/student/learning-summary?subjectId=1", token).body(), masteredPoint, 5, 4, "IMPROVING");
            jdbc.update("UPDATE cuo_ti_ji_lu SET zhuang_tai='MASTERED' WHERE xue_sheng_id=? AND ti_mu_id=?",
                    student, question(masteredPoint, 4));
            assertPoint(get("/api/v1/student/learning-summary?subjectId=1", token).body(), masteredPoint, 5, 4, "MASTERED");
        } finally {
            demo.clean();
        }
    }

    @Test
    void unsubmittedAnswersAreIgnoredAndNewCompletedFactsAppearImmediately() throws Exception {
        demo.seed();
        useNonDefaultPasswords();
        try {
            long student = student("DEMO_199_03");
            long point = point(1, 0);
            long session = addAnswer(student, 1, point, question(point, 0), false, false);
            String token = login("demo_199_03", "STUDENT");
            assertThat(get("/api/v1/student/learning-summary?subjectId=1", token).body())
                    .contains("\"totalAnsweredCount\":0");

            long conversationId = jdbc.queryForObject("""
                    SELECT lt.lian_xi_hui_hua_id FROM xue_sheng_da_ti da
                    JOIN lian_xi_ti_mu lt ON lt.id=da.lian_xi_ti_mu_id WHERE da.id=?
                    """, Long.class, session);
            jdbc.update("UPDATE lian_xi_hui_hua SET zhuang_tai='SUBMITTED',ti_jiao_shi_jian=CURRENT_TIMESTAMP(3) WHERE id=?",
                    conversationId);
            jdbc.update("INSERT INTO xue_xi_jie_guo(lian_xi_hui_hua_id,zong_ti_shu,zheng_que_shu,zong_de_fen,ti_jiao_shi_jian) VALUES (?,1,0,0,CURRENT_TIMESTAMP(3))",
                    conversationId);
            String refreshed = get("/api/v1/student/learning-summary?subjectId=1", token).body();
            assertPoint(refreshed, point, 1, 0, "INSUFFICIENT");
            assertThat(refreshed).contains("\"totalAnsweredCount\":1", "\"totalCorrectCount\":0");
        } finally {
            demo.clean();
        }
    }

    @Test
    void teacherOnlyReadsOwnActiveScopeClassAndSubject() throws Exception {
        demo.seed();
        useNonDefaultPasswords();
        try {
            long student = student("DEMO_199_01");
            long physicsPoint = point(1, 0);
            addAnswer(student, 1, physicsPoint, question(physicsPoint, 0), true, true);
            long physics199 = scope("DEMO_T_PHYSICS", "DEMO_CLASS_199", 1);
            long biology199 = scope("DEMO_T_BIOLOGY", "DEMO_CLASS_199", 3);
            String physicsToken = login("demo_physics_admin", "TEACHER");
            String biologyToken = login("demo_biology_teacher", "TEACHER");
            String studentToken = login("demo_199_01", "STUDENT");

            HttpResponse<String> allowed = get("/api/v1/teacher/scopes/" + physics199 + "/learning-summary", physicsToken);
            assertThat(allowed.statusCode()).isEqualTo(200);
            assertThat(allowed.body()).contains("199班", "物理", "199班学生01", "\"answeredCount\":1")
                    .doesNotContain("200班学生01");
            assertThat(get("/api/v1/teacher/scopes/" + biology199 + "/learning-summary", physicsToken).statusCode())
                    .isEqualTo(403);
            assertThat(get("/api/v1/teacher/scopes/" + physics199 + "/learning-summary", biologyToken).statusCode())
                    .isEqualTo(403);
            assertThat(get("/api/v1/teacher/scopes/" + physics199 + "/learning-summary", studentToken).statusCode())
                    .isEqualTo(403);

            jdbc.update("UPDATE ren_ke_guan_xi SET zhuang_tai='DISABLED' WHERE id=?", physics199);
            assertThat(get("/api/v1/teacher/scopes/" + physics199 + "/learning-summary", physicsToken).statusCode())
                    .isEqualTo(403);
        } finally {
            demo.clean();
        }
    }

    @Test
    void activePointWithHistoryRemainsInMasteryButCannotRecommendWhenFewerThanFiveQuestions() throws Exception {
        demo.seed();
        useNonDefaultPasswords();
        try {
            long student = student("DEMO_199_01");
            long sourcePoint = point(1, 0);
            long limitedPoint = createLimitedPoint(sourcePoint, 3);
            jdbc.update("UPDATE zhi_shi_dian SET zhuang_tai='DISABLED' WHERE ke_mu_id=1 AND id<>?", limitedPoint);
            addAnswer(student, 1, limitedPoint, question(sourcePoint, 0), true, true);
            addAnswer(student, 1, limitedPoint, question(sourcePoint, 1), false, true);
            addAnswer(student, 1, limitedPoint, question(sourcePoint, 2), false, true);

            String token = login("demo_199_01", "STUDENT");
            String summary = get("/api/v1/student/learning-summary?subjectId=1", token).body();
            assertPoint(summary, limitedPoint, 3, 1, "WEAK");
            assertThat(JsonPath.<Number>read(summary, "$.overall.totalKnowledgePointCount").intValue()).isEqualTo(1);
            assertThat(JsonPath.<Number>read(summary, "$.overall.weakKnowledgePointCount").intValue()).isEqualTo(1);
            assertThat(JsonPath.<List<?>>read(summary, "$.recommendations")).isEmpty();
            assertThat(summary).contains("当前暂无题量充足的知识点可生成5题巩固练习，可以先进行综合练习。")
                    .doesNotContain(GOOD_PERFORMANCE_MESSAGE);

            long physics199 = scope("DEMO_T_PHYSICS", "DEMO_CLASS_199", 1);
            String teacherSummary = get("/api/v1/teacher/scopes/" + physics199 + "/learning-summary",
                    login("demo_physics_admin", "TEACHER")).body();
            assertThat(teacherSummary).contains("199班学生01", "\"answeredCount\":3",
                    "\"weakKnowledgePointCount\":1");
        } finally {
            demo.clean();
        }
    }

    @Test
    void allMasteredPointsKeepGoodPerformanceMessageWithoutReinforcementRecommendation() throws Exception {
        demo.seed();
        useNonDefaultPasswords();
        try {
            long student = student("DEMO_199_02");
            long masteredPoint = point(1, 0);
            jdbc.update("UPDATE zhi_shi_dian SET zhuang_tai='DISABLED' WHERE ke_mu_id=1 AND id<>?", masteredPoint);
            for (int index = 0; index < 3; index++) {
                addAnswer(student, 1, masteredPoint, question(masteredPoint, index), true, true);
            }

            String summary = get("/api/v1/student/learning-summary?subjectId=1",
                    login("demo_199_02", "STUDENT")).body();
            assertPoint(summary, masteredPoint, 3, 3, "MASTERED");
            assertThat(JsonPath.<List<?>>read(summary, "$.recommendations")).isEmpty();
            assertThat(summary).contains(GOOD_PERFORMANCE_MESSAGE)
                    .doesNotContain("当前暂无题量充足的知识点可生成5题巩固练习");
        } finally {
            demo.clean();
        }
    }

    private long addAnswer(long studentId, long subjectId, long pointId, long questionId, boolean correct, boolean submitted) {
        jdbc.update("INSERT INTO lian_xi_hui_hua(xue_sheng_id,ke_mu_id,zhuang_tai,ti_mu_shu,ti_jiao_shi_jian) VALUES (?,?,?,1,IF(?='SUBMITTED',CURRENT_TIMESTAMP(3),NULL))",
                studentId, subjectId, submitted ? "SUBMITTED" : "CREATED", submitted ? "SUBMITTED" : "CREATED");
        long conversationId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        Map<String, Object> point = jdbc.queryForMap("SELECT zhi_shi_dian_ming_cheng name,wan_zheng_lu_jing path FROM zhi_shi_dian WHERE id=?", pointId);
        jdbc.update("""
                INSERT INTO lian_xi_ti_mu(lian_xi_hui_hua_id,ti_mu_id,ti_mu_shun_xu,fen_zhi,ti_mu_lei_xing,
                    nan_du_kuai_zhao,ti_gan_kuai_zhao,zheng_que_da_an_kuai_zhao,biao_zhun_jie_xi_kuai_zhao,zhi_shi_dian_kuai_zhao)
                SELECT ?,q.id,1,1,q.ti_mu_lei_xing,q.nan_du,q.ti_gan,q.zheng_que_da_an,'测试解析',
                    JSON_ARRAY(JSON_OBJECT('id',?,'name',?,'path',?))
                FROM ti_mu q WHERE q.id=?
                """, conversationId, pointId, point.get("name"), point.get("path"), questionId);
        long practiceQuestionId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("""
                INSERT INTO xue_sheng_da_ti(lian_xi_ti_mu_id,xue_sheng_id,xue_sheng_da_an,shi_fou_zheng_que,de_fen,ti_jiao_shi_jian)
                VALUES (?,?,JSON_OBJECT('test',true),?,?,CURRENT_TIMESTAMP(3))
                """, practiceQuestionId, studentId, correct ? 1 : 0, correct ? 1 : 0);
        long answerId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (submitted) {
            jdbc.update("INSERT INTO xue_xi_jie_guo(lian_xi_hui_hua_id,zong_ti_shu,zheng_que_shu,zong_de_fen,ti_jiao_shi_jian) VALUES (?,1,?,?,CURRENT_TIMESTAMP(3))",
                    conversationId, correct ? 1 : 0, correct ? 1 : 0);
        }
        return answerId;
    }

    private void upsertWrong(long studentId, long questionId, long answerId, String status) {
        jdbc.update("""
                INSERT INTO cuo_ti_ji_lu(xue_sheng_id,ti_mu_id,cuo_wu_ci_shu,lian_xu_zheng_que_ci_shu,zhuang_tai,zui_jin_da_ti_id,zui_jin_cuo_wu_shi_jian)
                VALUES (?,?,1,0,?,?,CURRENT_TIMESTAMP(3))
                ON DUPLICATE KEY UPDATE zhuang_tai=VALUES(zhuang_tai),zui_jin_da_ti_id=VALUES(zui_jin_da_ti_id)
                """, studentId, questionId, status, answerId);
    }

    private void assertPoint(String body, long pointId, int answered, int correct, String level) {
        List<Map<String, Object>> points = JsonPath.read(body, "$.knowledgePoints[?(@.knowledgePointId==" + pointId + ")]");
        assertThat(points).singleElement().satisfies(point -> {
            assertThat(((Number) point.get("answeredCount")).intValue()).isEqualTo(answered);
            assertThat(((Number) point.get("correctCount")).intValue()).isEqualTo(correct);
            assertThat(point.get("masteryLevel")).isEqualTo(level);
        });
    }

    private long student(String number) {
        return jdbc.queryForObject("SELECT id FROM xue_sheng_dang_an WHERE xue_hao=?", Long.class, number);
    }

    private long point(long subjectId, int offset) {
        return jdbc.queryForObject("""
                SELECT k.id FROM zhi_shi_dian k
                JOIN ti_mu_zhi_shi_dian qk ON qk.zhi_shi_dian_id=k.id AND qk.yi_shan_chu=0
                JOIN ti_mu q ON q.id=qk.ti_mu_id AND q.ti_gan LIKE '【演示】%'
                WHERE k.ke_mu_id=? GROUP BY k.id HAVING COUNT(DISTINCT q.id)>=5 ORDER BY k.id LIMIT 1 OFFSET
                """ + " " + offset, Long.class, subjectId);
    }

    private long question(long pointId, int offset) {
        return jdbc.queryForObject("""
                SELECT q.id FROM ti_mu q JOIN ti_mu_zhi_shi_dian qk ON qk.ti_mu_id=q.id AND qk.yi_shan_chu=0
                WHERE qk.zhi_shi_dian_id=? AND q.ti_gan LIKE '【演示】%' ORDER BY q.id LIMIT 1 OFFSET
                """ + " " + offset, Long.class, pointId);
    }

    private long createLimitedPoint(long sourcePointId, int questionCount) {
        jdbc.update("""
                INSERT INTO zhi_shi_dian(ke_mu_id,zhi_shi_dian_ming_cheng,wan_zheng_lu_jing,ceng_ji,pai_xu)
                VALUES (1,'限量练习知识点','物理>限量练习知识点',1,99)
                """);
        long pointId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        for (int index = 0; index < questionCount; index++) {
            jdbc.update("""
                    INSERT INTO ti_mu_zhi_shi_dian(ti_mu_id,zhi_shi_dian_id,shi_fou_zhu_yao,pai_xu)
                    VALUES (?,?,0,99)
                    """, question(sourcePointId, index), pointId);
        }
        return pointId;
    }

    private static final String GOOD_PERFORMANCE_MESSAGE =
            "当前已练习知识点整体表现良好，可以进行综合随机练习。";

    private long scope(String teacherNumber, String classCode, long subjectId) {
        return jdbc.queryForObject("""
                SELECT r.id FROM ren_ke_guan_xi r
                JOIN jiao_shi_dang_an t ON t.id=r.jiao_shi_id JOIN ban_ji b ON b.id=r.ban_ji_id
                WHERE t.gong_hao=? AND b.ban_ji_bian_ma=? AND r.ke_mu_id=?
                """, Long.class, teacherNumber, classCode, subjectId);
    }

    private String login(String username, String role) throws Exception {
        HttpResponse<String> challenge = get("/api/v1/auth/captcha-challenge", null);
        String challengeId = JsonPath.read(challenge.body(), "$.challengeId");
        String captchaCode = JsonPath.read(challenge.body(), "$.testCode");
        HttpResponse<String> response = post("/api/v1/auth/login", null, "{\"username\":\"" + username
                + "\",\"password\":\"MasteryPass1\",\"expectedRole\":\"" + role + "\",\"challengeId\":\""
                + challengeId + "\",\"captchaCode\":\"" + captchaCode + "\"}");
        assertThat(response.statusCode()).isEqualTo(200);
        return JsonPath.read(response.body(), "$.accessToken");
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).GET();
        if (token != null) request.header("Authorization", "Bearer " + token);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> post(String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        return http.send(request.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void useNonDefaultPasswords(){ for(String username: new String[]{"demo_199_01","demo_199_02","demo_199_03","demo_physics_admin","demo_biology_teacher"}) jdbc.update("UPDATE yong_hu SET mi_ma_zhai_yao=?,shi_fou_shou_ci_deng_lu=0 WHERE yong_hu_ming=?",passwordEncoder.encode("MasteryPass1"),username); }
}
