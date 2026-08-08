package com.neu.riketiku.xueshengguanli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import com.neu.riketiku.renzheng.JwtLingPaiFuWu;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.renzheng.RenZhengYongHu;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.StudentCreateRequest;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.StudentTransferRequest;
import com.neu.riketiku.xueshengguanli.dto.StudentManagementDtos.StudentUpdateRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StudentManagementIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private StudentManagementService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtLingPaiFuWu jwt;
    @LocalServerPort private int port;

    @Test
    void adminCanPageAndFilterStudentsWithoutInternalSecrets() throws Exception {
        long class199 = createClass("STU-LIST-199", "199班", "高三", "ACTIVE");
        service.create(new StudentCreateRequest("S-LIST-01", "列表学生甲", "student_list_01", "高三", class199));
        service.create(new StudentCreateRequest("S-LIST-02", "列表学生乙", "student_list_02", "高三", class199));

        assertThat(service.page(1, 10, "S-LIST-01", null, null, null, null, null, null).records()).hasSize(1);
        assertThat(service.page(1, 10, null, "学生乙", null, null, null, null, null).records()).hasSize(1);
        assertThat(service.page(1, 10, null, null, "list_01", null, null, null, null).records()).hasSize(1);
        assertThat(service.page(1, 10, null, null, null, class199, "高三", "ENABLED", "ACTIVE").records()).hasSize(2);

        String adminToken = token("list-admin", List.of("ADMIN"));
        HttpResponse<String> response = get("/api/v1/admin/students?studentNumber=S-LIST-01", adminToken);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("S-LIST-01", "199班", "ENABLED", "ACTIVE")
                .doesNotContain("mi_ma_zhai_yao", "passwordHash", "accessToken", "yi_shan_chu");
    }

    @Test
    void createIsAtomicAndRejectsDuplicateOrUnavailableData() {
        long activeClass = createClass("STU-CREATE-A", "创建班", "高三", "ACTIVE");
        long disabledClass = createClass("STU-CREATE-D", "停用班", "高三", "DISABLED");
        service.create(new StudentCreateRequest("S-CREATE-01", "创建学生", "student_create_01", "高三", activeClass));

        assertCode(() -> service.create(new StudentCreateRequest("S-CREATE-01", "重复学号", "student_create_02", "高三", activeClass)), "STUDENT_NUMBER_EXISTS");
        assertCode(() -> service.create(new StudentCreateRequest("S-CREATE-02", "重复用户名", "student_create_01", "高三", activeClass)), "USERNAME_EXISTS");
        assertCode(() -> service.create(new StudentCreateRequest("S-CREATE-03", "无班级", "student_create_03", "高三", 999999L)), "CLASS_UNAVAILABLE");
        assertCode(() -> service.create(new StudentCreateRequest("S-CREATE-04", "停用班级", "student_create_04", "高三", disabledClass)), "CLASS_UNAVAILABLE");

        assertThat(count("SELECT COUNT(*) FROM yong_hu WHERE yong_hu_ming IN ('student_create_03','student_create_04')")).isZero();
        assertThat(count("SELECT COUNT(*) FROM xue_sheng_dang_an WHERE xue_hao IN ('S-CREATE-03','S-CREATE-04')")).isZero();
    }

    @Test
    void adminCanEditDisableAndEnableStudentWithoutDeletingLearningData() throws Exception {
        long classId = createClass("STU-EDIT", "编辑班", "高二", "ACTIVE");
        var created = service.create(new StudentCreateRequest("S-EDIT-01", "编辑前", "student_edit_01", "高二", classId));
        long studentId = created.student().student().id();
        completeInitialPassword("student_edit_01", created.initialPassword(), "StudentEditPass2");

        var disabled = service.update(studentId, new StudentUpdateRequest("编辑后", "高三", "DISABLED", "ACTIVE"));
        assertThat(disabled.student().name()).isEqualTo("编辑后");
        assertThat(disabled.student().grade()).isEqualTo("高三");
        assertThat(login("student_edit_01", "StudentEditPass2").statusCode()).isEqualTo(403);

        service.update(studentId, new StudentUpdateRequest("编辑后", "高三", "ENABLED", "ACTIVE"));
        assertThat(login("student_edit_01", "StudentEditPass2").statusCode()).isEqualTo(200);
        assertThat(count("SELECT COUNT(*) FROM yong_hu WHERE yong_hu_ming='student_edit_01' AND yi_shan_chu=0")).isEqualTo(1);
    }

    @Test
    void transferPreservesHistoryAndOnlyOneActiveMainClass() {
        long class199 = createClass("STU-TRANSFER-199", "199班", "高三", "ACTIVE");
        long class200 = createClass("STU-TRANSFER-200", "200班", "高三", "ACTIVE");
        var created = service.create(new StudentCreateRequest("S-TRANSFER", "调班学生", "student_transfer", "高三", class199));
        long studentId = created.student().student().id();

        var transferred = service.transfer(studentId, new StudentTransferRequest(class200));
        assertThat(transferred.student().currentClass().id()).isEqualTo(class200);
        assertThat(transferred.classHistory()).hasSize(2);
        assertThat(transferred.classHistory()).anySatisfy(item -> {
            assertThat(item.classId()).isEqualTo(class199);
            assertThat(item.current()).isFalse();
            assertThat(item.exitedAt()).isNotNull();
        });
        assertThat(count("SELECT COUNT(*) FROM ban_ji_xue_sheng WHERE xue_sheng_id=" + studentId + " AND shi_fou_zhu_ban_ji=1 AND zhuang_tai='ACTIVE' AND tui_chu_shi_jian IS NULL")).isEqualTo(1);
        assertCode(() -> service.transfer(studentId, new StudentTransferRequest(class200)), "STUDENT_ALREADY_IN_CLASS");
    }

    @Test
    void passwordResetInvalidatesOldPasswordAndRequiresInitialChange() throws Exception {
        long classId = createClass("STU-RESET", "重置班", "高三", "ACTIVE");
        var created = service.create(new StudentCreateRequest("S-RESET", "重置学生", "student_reset", "高三", classId));
        completeInitialPassword("student_reset", created.initialPassword(), "StudentOldPass2");
        long studentId = created.student().student().id();

        String resetPassword = service.resetPassword(studentId).initialPassword();
        assertThat(resetPassword).hasSize(12).doesNotContain("StudentOldPass2");
        assertThat(login("student_reset", "StudentOldPass2").statusCode()).isEqualTo(401);
        HttpResponse<String> login = login("student_reset", resetPassword);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(login.body(), "$.mustChangePassword")).isTrue();
        String digest = jdbc.queryForObject("SELECT mi_ma_zhai_yao FROM yong_hu WHERE yong_hu_ming='student_reset'", String.class);
        assertThat(digest).doesNotContain(resetPassword);
        assertThat(passwordEncoder.matches(resetPassword, digest)).isTrue();
    }

    @Test
    void studentAndTeacherCannotAccessAdminStudentEndpoints() throws Exception {
        assertThat(get("/api/v1/admin/students", token("blocked-student", List.of("STUDENT"))).statusCode()).isEqualTo(403);
        assertThat(get("/api/v1/admin/students", token("blocked-teacher", List.of("TEACHER"))).statusCode()).isEqualTo(403);
        assertThat(get("/api/v1/admin/students", null).statusCode()).isEqualTo(401);
        assertThat(get("/api/v1/admin/students", token("allowed-admin", List.of("ADMIN"))).statusCode()).isEqualTo(200);
    }

    private long createClass(String code, String name, String grade, String status) {
        jdbc.update("INSERT INTO ban_ji(ban_ji_bian_ma,ban_ji_ming_cheng,nian_ji,ru_xue_nian_fen,zhuang_tai) VALUES (?,?,?,?,?)",
                code, name, grade, Math.max(2000, Year.now().getValue() - 3), status);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void completeInitialPassword(String username, String oldPassword, String newPassword) throws Exception {
        HttpResponse<String> login = login(username, oldPassword);
        String accessToken = JsonPath.read(login.body(), "$.accessToken");
        HttpRequest request = HttpRequest.newBuilder(uri("/api/v1/auth/change-initial-password"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"oldPassword\":\"" + oldPassword
                        + "\",\"newPassword\":\"" + newPassword + "\",\"confirmPassword\":\"" + newPassword + "\"}"))
                .build();
        assertThat(HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> login(String username, String password) throws Exception {
        HttpResponse<String> challenge = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(uri("/api/v1/auth/captcha-challenge")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password
                + "\",\"expectedRole\":\"STUDENT\",\"challengeId\":\""
                + JsonPath.read(challenge.body(), "$.challengeId") + "\",\"captchaCode\":\""
                + JsonPath.read(challenge.body(), "$.testCode") + "\"}";
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> get(String path, String accessToken) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        if (accessToken != null) request.header("Authorization", "Bearer " + accessToken);
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String token(String username, List<String> roles) {
        return jwt.shengChengLingPai(new RenZhengYongHu(9999L, username, roles, false));
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(RenZhengYeWuYiChang.class,
                exception -> assertThat(exception.getCode()).isEqualTo(code));
    }
}
