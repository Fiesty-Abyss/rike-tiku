package com.neu.riketiku.jiaoshi;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.neu.riketiku.demo.DemoDataService;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JiaoShiGaoPinKaoDianIntegrationTest extends AdminQuestionIntegrationTestSupport {
    private final HttpClient http = HttpClient.newHttpClient();

    @Autowired private DemoDataService demo;
    @Autowired private JdbcTemplate jdbc;
    @LocalServerPort private int port;

    @Test
    void teacherAndStudentHighFrequencyPermissionsAreSeparated() throws Exception {
        demo.seed();
        try {
            long physics199 = scope("DEMO_T_PHYSICS", "DEMO_CLASS_199", 1);
            long biology199 = scope("DEMO_T_BIOLOGY", "DEMO_CLASS_199", 3);
            String physicsToken = login("demo_physics_admin", "TEACHER");
            String studentToken = login("demo_199_01", "STUDENT");

            HttpResponse<String> workspace = get("/api/v1/teacher/scopes/" + physics199, physicsToken);
            assertThat(workspace.statusCode()).isEqualTo(200);
            assertThat(workspace.body()).contains("199班", "studentCount", "牛顿第二定律受力分析");
            assertThat(get("/api/v1/teacher/scopes/" + biology199, physicsToken).statusCode()).isEqualTo(403);
            assertThat(get("/api/v1/teacher/scopes/" + physics199, studentToken).statusCode()).isEqualTo(403);

            String createBody = "{\"knowledgePointId\":1,\"title\":\"接口新增考点\",\"content\":\"接口测试正文\",\"sortOrder\":9}";
            HttpResponse<String> created = post("/api/v1/teacher/scopes/" + physics199 + "/high-frequency-points", physicsToken, createBody);
            assertThat(created.statusCode()).isEqualTo(200);
            long pointId = ((Number) JsonPath.read(created.body(), "$.id")).longValue();
            assertThat(put("/api/v1/teacher/high-frequency-points/" + pointId, physicsToken,
                    "{\"title\":\"接口修改考点\",\"content\":\"修改后的正文\",\"sortOrder\":10}").statusCode()).isEqualTo(200);
            assertThat(post("/api/v1/teacher/high-frequency-points/" + pointId + "/status", physicsToken,
                    "{\"status\":\"DISABLED\"}").statusCode()).isEqualTo(200);

            HttpResponse<String> studentPoints = get("/api/v1/student/high-frequency-points?subjectId=1", studentToken);
            assertThat(studentPoints.statusCode()).isEqualTo(200);
            assertThat(studentPoints.body()).contains("牛顿第二定律受力分析").doesNotContain("接口修改考点");
            assertThat(get("/api/v1/student/high-frequency-points?subjectId=1", physicsToken).statusCode()).isEqualTo(403);
        } finally {
            demo.clean();
        }
    }

    private long scope(String teacherNumber, String classCode, long subjectId) {
        return jdbc.queryForObject("""
                SELECT r.id FROM ren_ke_guan_xi r
                JOIN jiao_shi_dang_an t ON t.id=r.jiao_shi_id
                JOIN ban_ji b ON b.id=r.ban_ji_id
                WHERE t.gong_hao=? AND b.ban_ji_bian_ma=? AND r.ke_mu_id=?
                """, Long.class, teacherNumber, classCode, subjectId);
    }

    private String login(String username, String role) throws Exception {
        HttpResponse<String> challenge = get("/api/v1/auth/captcha-challenge", null);
        String challengeId = JsonPath.read(challenge.body(), "$.challengeId");
        String captchaCode = JsonPath.read(challenge.body(), "$.testCode");
        HttpResponse<String> response = post("/api/v1/auth/login", null, "{\"username\":\"" + username
                + "\",\"password\":\"a1234567\",\"expectedRole\":\"" + role + "\",\"challengeId\":\""
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

    private HttpResponse<String> put(String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + token);
        return http.send(request.PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
