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

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM yong_hu WHERE yong_hu_ming LIKE 'demo_%'", Integer.class)).isEqualTo(3);
        String digest = jdbc.queryForObject("SELECT mi_ma_zhai_yao FROM yong_hu WHERE yong_hu_ming='demo_student'", String.class);
        assertThat(digest).doesNotContain(DemoDataService.DEMO_PASSWORD);
        assertThat(passwordEncoder.matches(DemoDataService.DEMO_PASSWORD, digest)).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ren_ke_guan_xi r JOIN jiao_shi_dang_an t ON t.id=r.jiao_shi_id WHERE t.gong_hao='DEMO_T001'", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu WHERE ti_gan LIKE '【演示】%'", Integer.class)).isEqualTo(18);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu_lai_yuan s JOIN ti_mu q ON q.id=s.ti_mu_id WHERE q.ti_gan LIKE '【演示】%' AND s.quan_li_zhuang_tai='USER_PROVIDED'", Integer.class)).isEqualTo(54);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu_shen_he_ji_lu r JOIN ti_mu q ON q.id=r.ti_mu_id WHERE q.ti_gan LIKE '【演示】%'", Integer.class)).isEqualTo(36);
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
        assertThat(jdbc.queryForObject("SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success=1", Integer.class)).isEqualTo(7);
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
                .contains("RIKE_TIKU_BACKEND_PORT", "RIKE_TIKU_CORS_ALLOWED_ORIGINS", "http://localhost:18081/api/v1")
                .doesNotContain("RIKE_TIKU_SERVER_PORT", "RIKE_TIKU_CORS_ALLOWED_ORIGIN =");
    }

    private void assertHttpLogin(String username, String role, int status, String expectedBody) throws Exception {
        HttpResponse<String> challenge = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/auth/slider-challenge")).GET().build(), HttpResponse.BodyHandlers.ofString());
        String body = "{\"username\":\"" + username + "\",\"password\":\"a1234567\",\"expectedRole\":\"" + role
                + "\",\"challengeId\":\"" + JsonPath.read(challenge.body(), "$.challengeId") + "\",\"sliderOffset\":"
                + ((Number) JsonPath.read(challenge.body(), "$.targetDisplayOffset")).intValue() + "}";
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
}
