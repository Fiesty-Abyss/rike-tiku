package com.neu.riketiku.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import com.neu.riketiku.xueshenglianxi.StudentPracticeDtos;
import com.neu.riketiku.xueshenglianxi.StudentPracticeService;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoDataServiceIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private DemoDataService demo;
    @Autowired private StudentPracticeService practice;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbc;
    @LocalServerPort private int port;

    @Test
    @Transactional
    void demoDataIsDisabledByDefaultAndProtectedDatabaseNamesAreRejected() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM yong_hu WHERE yong_hu_ming LIKE 'demo_%'", Integer.class)).isZero();
        for (String database : List.of("rike_tiku", "mysql", "information_schema", "performance_schema", "sys", "bad-name")) {
            assertThatThrownBy(() -> DemoDataService.guardDatabaseName(database))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("拒绝");
        }
    }

    @Test
    @Transactional
    void seedsAccountsTeachingOrganizationQuestionsSourcesAndReviewsIdempotently() {
        demo.seed();
        demo.seed();
        demo.validateSeed();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM yong_hu WHERE yong_hu_ming LIKE 'demo_%'", Integer.class)).isEqualTo(14);
        String digest = jdbc.queryForObject("SELECT mi_ma_zhai_yao FROM yong_hu WHERE yong_hu_ming='demo_student'", String.class);
        assertThat(digest).doesNotContain(DemoDataService.DEMO_PASSWORD);
        assertThat(passwordEncoder.matches(DemoDataService.DEMO_PASSWORD, digest)).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ren_ke_guan_xi r JOIN jiao_shi_dang_an t ON t.id=r.jiao_shi_id WHERE t.gong_hao='DEMO_T001'", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu WHERE ti_gan LIKE '【演示】%'", Integer.class))
                .isEqualTo(DemoDataService.FINAL_DEMO_QUESTION_COUNT);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu_lai_yuan s JOIN ti_mu q ON q.id=s.ti_mu_id WHERE q.ti_gan LIKE '【演示】%' AND s.quan_li_zhuang_tai='USER_PROVIDED' AND s.lai_yuan_ming_cheng='本科毕业设计自编演示题'", Integer.class))
                .isEqualTo(DemoDataService.FINAL_DEMO_QUESTION_COUNT * 3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu_shen_he_ji_lu r JOIN ti_mu q ON q.id=r.ti_mu_id WHERE q.ti_gan LIKE '【演示】%'", Integer.class))
                .isEqualTo(DemoDataService.FINAL_DEMO_QUESTION_COUNT * 2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ban_ji WHERE ban_ji_bian_ma LIKE 'DEMO_CLASS_%'", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM jiao_shi_dang_an WHERE gong_hao LIKE 'DEMO_T%'", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM xue_sheng_dang_an WHERE xue_hao LIKE 'DEMO_%'", Integer.class)).isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ren_ke_guan_xi r JOIN jiao_shi_dang_an t ON t.id=r.jiao_shi_id WHERE t.gong_hao LIKE 'DEMO_T%' AND r.zhuang_tai='ACTIVE'", Integer.class)).isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM gao_pin_kao_dian h JOIN ren_ke_guan_xi r ON r.id=h.ren_ke_guan_xi_id JOIN ban_ji b ON b.id=r.ban_ji_id WHERE b.ban_ji_bian_ma IN ('DEMO_CLASS_199','DEMO_CLASS_200') AND h.zhuang_tai='ACTIVE'", Integer.class)).isEqualTo(12);
        assertThat(jdbc.queryForList("""
                SELECT a.jie_xi_nei_rong FROM ti_mu_jie_xi a JOIN ti_mu q ON q.id=a.ti_mu_id
                WHERE q.ti_gan LIKE '【演示】%' AND a.jie_xi_lei_xing='STANDARD'
                """, String.class)).allSatisfy(analysis -> assertThat(analysis)
                        .doesNotContain("演示时可用其他选项构造错题", "正确答案为由"));
    }

    @Test
    @Transactional
    void demo90RemainsStableAndFinalBankAddsReviewedVariants() {
        demo.seed();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu WHERE ti_gan LIKE '【演示】%' AND ti_gan NOT LIKE '【演示】变式：%'", Integer.class))
                .isEqualTo(90);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu WHERE ti_gan LIKE '【演示】变式：%'", Integer.class))
                .isEqualTo(DemoVariantQuestionBank.ACCEPTED_COUNT);

        for (String subject : List.of("PHYSICS", "CHEMISTRY", "BIOLOGY")) {
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*) FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id
                    WHERE q.ti_gan LIKE '【演示】%' AND q.ti_gan NOT LIKE '【演示】变式：%' AND s.ke_mu_dai_ma=?
                    """, Integer.class, subject)).isEqualTo(30);
            assertThat(jdbc.queryForList("""
                    SELECT COUNT(*) FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id
                    WHERE q.ti_gan LIKE '【演示】%' AND q.ti_gan NOT LIKE '【演示】变式：%' AND s.ke_mu_dai_ma=?
                    GROUP BY q.ti_mu_lei_xing ORDER BY q.ti_mu_lei_xing
                    """, Integer.class, subject)).containsExactly(10, 10, 10);
            assertThat(jdbc.queryForList("""
                    SELECT COUNT(*) FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id
                    WHERE q.ti_gan LIKE '【演示】%' AND q.ti_gan NOT LIKE '【演示】变式：%' AND s.ke_mu_dai_ma=?
                    GROUP BY q.nan_du ORDER BY q.nan_du
                    """, Integer.class, subject)).containsExactly(10, 10, 10);
            assertThat(jdbc.queryForList("""
                    SELECT COUNT(DISTINCT q.id) FROM ti_mu q
                    JOIN ke_mu s ON s.id=q.ke_mu_id
                    JOIN ti_mu_zhi_shi_dian qk ON qk.ti_mu_id=q.id AND qk.yi_shan_chu=0
                    JOIN zhi_shi_dian k ON k.id=qk.zhi_shi_dian_id AND k.yi_shan_chu=0
                    WHERE q.ti_gan LIKE '【演示】%' AND q.ti_gan NOT LIKE '【演示】变式：%' AND s.ke_mu_dai_ma=?
                    GROUP BY k.id ORDER BY k.id
                    """, Integer.class, subject)).containsExactly(10, 10, 10);
        }
    }

    @Test
    @Transactional
    void acceptedVariantsPassStructureRightsReviewAndDuplicateChecks() {
        assertThat(DemoVariantQuestionBank.CANDIDATE_COUNT).isEqualTo(54);
        assertThat(DemoVariantQuestionBank.ACCEPTED_COUNT).isEqualTo(30);
        assertThat(DemoVariantQuestionBank.REJECTED_COUNT).isEqualTo(24);
        assertThat(DemoVariantQuestionBank.acceptedQuestions()).hasSize(30);
        demo.seed();

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM ti_mu q
                WHERE q.ti_gan LIKE '【演示】变式：%'
                  AND q.zhuang_tai='PUBLISHED' AND q.shi_yong_mo_shi='ONLINE_PRACTICE'
                  AND q.shi_fou_ke_zi_dong_pan_fen=1 AND q.yi_shan_chu=0
                  AND q.ti_mu_lei_xing IN ('SINGLE_CHOICE','MULTIPLE_CHOICE','FILL_BLANK')
                """, Integer.class)).isEqualTo(30);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id
                WHERE q.ti_gan LIKE '【演示】变式：%' AND s.ke_mu_dai_ma='PHYSICS'
                """, Integer.class)).isEqualTo(10);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id
                WHERE q.ti_gan LIKE '【演示】变式：%' AND s.ke_mu_dai_ma='CHEMISTRY'
                """, Integer.class)).isEqualTo(9);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id
                WHERE q.ti_gan LIKE '【演示】变式：%' AND s.ke_mu_dai_ma='BIOLOGY'
                """, Integer.class)).isEqualTo(11);

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM ti_mu q
                WHERE q.ti_gan LIKE '【演示】变式：%'
                  AND (TRIM(q.ti_gan)='' OR q.ti_gan LIKE '%[[I%' OR q.ti_gan LIKE '%[[F%'
                    OR EXISTS (SELECT 1 FROM ti_mu_fu_jian f WHERE f.ti_mu_id=q.id AND f.zhuang_tai='ACTIVE' AND f.yi_shan_chu=0)
                    OR NOT EXISTS (SELECT 1 FROM ti_mu_jie_xi a WHERE a.ti_mu_id=q.id AND a.jie_xi_lei_xing='STANDARD'
                      AND a.zhuang_tai='PUBLISHED' AND a.yi_shan_chu=0 AND TRIM(a.jie_xi_nei_rong)<>'')
                    OR NOT EXISTS (SELECT 1 FROM ti_mu_zhi_shi_dian qk JOIN zhi_shi_dian k ON k.id=qk.zhi_shi_dian_id
                      WHERE qk.ti_mu_id=q.id AND qk.yi_shan_chu=0 AND k.zhuang_tai='ACTIVE' AND k.ke_mu_id=q.ke_mu_id))
                """, Integer.class)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM ti_mu_lai_yuan s JOIN ti_mu q ON q.id=s.ti_mu_id
                WHERE q.ti_gan LIKE '【演示】变式：%' AND s.lai_yuan_lei_xing='TEACHER_CREATED'
                  AND s.lai_yuan_ming_cheng='本科毕业设计自编演示题'
                  AND s.quan_li_zhuang_tai='USER_PROVIDED' AND TRIM(s.quan_li_yi_ju)<>'' AND s.yi_shan_chu=0
                """, Integer.class)).isEqualTo(90);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM ti_mu_shen_he_ji_lu r JOIN ti_mu q ON q.id=r.ti_mu_id
                WHERE q.ti_gan LIKE '【演示】变式：%' AND r.shen_he_dong_zuo IN ('SUBMITTED','APPROVED')
                """, Integer.class)).isEqualTo(60);

        List<String> baselineSourceStems = jdbc.queryForList(
                "SELECT ti_gan FROM ti_mu WHERE ti_gan LIKE '【演示】%' AND ti_gan NOT LIKE '【演示】变式：%'", String.class);
        HashSet<String> baselineStems = new HashSet<>();
        baselineSourceStems.forEach(stem -> assertThat(baselineStems.add(normalizeStem(stem)))
                .as("Demo90内部重复: " + stem).isTrue());
        assertThat(baselineStems).hasSize(DemoDataService.BASE_DEMO_QUESTION_COUNT);
        HashSet<String> variantStems = new HashSet<>();
        jdbc.queryForList("SELECT ti_gan FROM ti_mu WHERE ti_gan LIKE '【演示】变式：%'", String.class).forEach(stem -> {
            String normalized = normalizeStem(stem);
            assertThat(variantStems.add(normalized)).as("变式内部重复: " + stem).isTrue();
            assertThat(baselineStems).as("变式与Demo90重复: " + stem).doesNotContain(normalized);
        });
        assertThat(baselineStems).doesNotContainAnyElementsOf(variantStems);

        List<String> chemistryText = jdbc.queryForList("""
                SELECT CONCAT_WS(' ',q.ti_gan,GROUP_CONCAT(DISTINCT o.xuan_xiang_nei_rong SEPARATOR ' '),
                    GROUP_CONCAT(DISTINCT a.jie_xi_nei_rong SEPARATOR ' '))
                FROM ti_mu q JOIN ke_mu s ON s.id=q.ke_mu_id
                LEFT JOIN ti_mu_xuan_xiang o ON o.ti_mu_id=q.id AND o.yi_shan_chu=0
                LEFT JOIN ti_mu_jie_xi a ON a.ti_mu_id=q.id AND a.yi_shan_chu=0
                WHERE q.ti_gan LIKE '【演示】变式：%' AND s.ke_mu_dai_ma='CHEMISTRY' GROUP BY q.id
                """, String.class);
        assertThat(chemistryText).allSatisfy(text -> assertThat(text)
                .doesNotContain("H2", "O2", "CO2", "H2SO4", "SO4^", "10^", "[[I", "[[F", "\\frac", "\\mathrm"));
    }

    @Test
    @Transactional
    void studentPracticeServiceActuallySelectsAcceptedVariants() {
        demo.seed();
        long userId = jdbc.queryForObject("SELECT id FROM yong_hu WHERE yong_hu_ming='demo_student'", Long.class);
        for (long subjectId : jdbc.queryForList("SELECT id FROM ke_mu WHERE ke_mu_dai_ma IN ('PHYSICS','CHEMISTRY','BIOLOGY') ORDER BY id", Long.class)) {
            long knowledgePointId = jdbc.queryForObject("""
                    SELECT k.id FROM zhi_shi_dian k
                    JOIN ti_mu_zhi_shi_dian qk ON qk.zhi_shi_dian_id=k.id AND qk.yi_shan_chu=0
                    JOIN ti_mu q ON q.id=qk.ti_mu_id AND q.yi_shan_chu=0
                    WHERE k.ke_mu_id=? AND k.zhuang_tai='ACTIVE' AND k.yi_shan_chu=0
                      AND q.ti_gan LIKE '【演示】变式：%'
                    GROUP BY k.id ORDER BY k.id LIMIT 1
                    """, Long.class, subjectId);
            boolean selectedVariant = false;
            HashSet<List<String>> questionSets = new HashSet<>();
            for (int attempt = 0; attempt < 10; attempt++) {
                var session = practice.create(userId,
                        new StudentPracticeDtos.CreateRequest(subjectId, List.of(knowledgePointId), null, null, 5));
                List<String> stems = session.questions().stream().map(StudentPracticeDtos.SessionQuestion::stem).sorted().toList();
                questionSets.add(stems);
                selectedVariant |= stems.stream().anyMatch(stem -> stem.contains("变式："));
            }
            assertThat(selectedVariant).as("subjectId=" + subjectId).isTrue();
            assertThat(questionSets).as("相同知识点多次随机题集应发生变化, subjectId=" + subjectId).hasSizeGreaterThan(1);
        }
    }

    @Test
    @Transactional
    void allSubjectsCanCreateFiveQuestionPracticeFromSeededPool() {
        demo.seed();
        long userId = jdbc.queryForObject("SELECT id FROM yong_hu WHERE yong_hu_ming='demo_student'", Long.class);
        for (long subjectId : jdbc.queryForList("SELECT id FROM ke_mu WHERE ke_mu_dai_ma IN ('PHYSICS','CHEMISTRY','BIOLOGY') ORDER BY id", Long.class)) {
            var session = practice.create(userId, new StudentPracticeDtos.CreateRequest(subjectId, null, null, null, 5));
            assertThat(session.questions()).hasSize(5);
            assertThat(session.questions()).allMatch(question -> !question.stem().contains("[[I") && !question.stem().contains("[[F"));
        }
    }

    @Test
    @Transactional
    void cleanRemovesOnlyDemoRecordsAndLeavesFlywaySamples() {
        int baselineQuestions = jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu", Integer.class);
        demo.seed();
        demo.clean();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM yong_hu WHERE yong_hu_ming LIKE 'demo_%'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu WHERE ti_gan LIKE '【演示】%'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu", Integer.class)).isEqualTo(baselineQuestions);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ke_mu", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success=1", Integer.class)).isEqualTo(11);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM gao_pin_kao_dian", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM si_xin_hui_hua", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM si_xin_xiao_xi", Integer.class)).isZero();
    }

    @Test
    void seededDemoAccountsLoginThroughRealHttpAndWrongEntryIsRejected() throws Exception {
        demo.seed();
        try {
            assertHttpLogin("demo_admin", "ADMIN", 200, "\"ADMIN\"");
            assertHttpLogin("demo_teacher", "TEACHER", 200, "\"TEACHER\"");
            assertHttpLogin("demo_student", "STUDENT", 200, "\"STUDENT\"");
            assertHttpLogin("demo_admin", "STUDENT", 403, "ROLE_MISMATCH");
            String digest = jdbc.queryForObject("SELECT mi_ma_zhai_yao FROM yong_hu WHERE yong_hu_ming='demo_admin'", String.class);
            assertThat(digest).doesNotContain(DemoDataService.DEMO_PASSWORD);
        } finally {
            demo.clean();
        }
    }

    @Test
    void demoScriptUsesExactBackendCorsAndApiEnvironmentContracts() throws Exception {
        String script = Files.readString(Path.of("..", "scripts", "demo-environment.ps1"), StandardCharsets.UTF_8);
        assertThat(script)
                .contains("RIKE_TIKU_BACKEND_PORT", "RIKE_TIKU_CORS_ALLOWED_ORIGINS", "http://localhost:18081/api/v1",
                        "'final-acceptance'", "'smoke-backend'", "$env:RIKE_TIKU_CAPTCHA_EXPOSE_TEST_CODE = 'false'",
                        "$env:RIKE_TIKU_CAPTCHA_EXPOSE_TEST_CODE = 'true'")
                .doesNotContain("RIKE_TIKU_SERVER_PORT", "RIKE_TIKU_CORS_ALLOWED_ORIGIN =");
    }

    @Test
    void variantReviewRecordMatchesSeedInventory() throws Exception {
        String review = Files.readString(Path.of("..", "docs", "DEMO_VARIANT_QUESTION_REVIEW.md"), StandardCharsets.UTF_8);
        assertThat(review)
                .contains("| 物理 | 18 | 10 | 8 |", "| 化学 | 18 | 9 | 9 |", "| 生物 | 18 | 11 | 7 |",
                        "| 合计 | 54 | 30 | 24 |", "开发阶段由 Codex 辅助整理候选不等于系统实现了运行时 AI 出题")
                .doesNotContain("MVP30 已正式入库");
    }

    private void assertHttpLogin(String username, String role, int status, String expectedBody) throws Exception {
        HttpResponse<String> challenge = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/auth/captcha-challenge")).GET().build(), HttpResponse.BodyHandlers.ofString());
        String body = "{\"username\":\"" + username + "\",\"password\":\"a1234567\",\"expectedRole\":\"" + role
                + "\",\"challengeId\":\"" + JsonPath.read(challenge.body(), "$.challengeId") + "\",\"captchaCode\":\""
                + JsonPath.read(challenge.body(), "$.testCode") + "\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.body()).contains(expectedBody);
        if (status == 200) assertThat(response.body()).contains("\"accessToken\"").doesNotContain("a1234567");
    }

    private static String normalizeStem(String stem) {
        return Normalizer.normalize(stem, Normalizer.Form.NFKC)
                .replace("【演示】", "")
                .replaceFirst("^变式：", "")
                .toLowerCase()
                .replaceAll("[\\p{P}\\p{Z}\\s]+", "");
    }
}
