package com.neu.riketiku.renzheng;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RenZhengJiChengTest {
    private static final String DATABASE_PASSWORD = requiredEnvironment("RIKE_TIKU_DB_PASSWORD");
    private static final String DATABASE_USERNAME = environment("RIKE_TIKU_DB_USERNAME", "root");
    private static final String DATABASE_HOST = environment("RIKE_TIKU_DB_HOST", "localhost");
    private static final String DATABASE_PORT = environment("RIKE_TIKU_DB_PORT", "3306");
    private static final String TEST_JWT_SECRET =
            "automated-test-only-jwt-secret-with-more-than-32-bytes";
    private static final String SCHEMA =
            "rike_tiku_auth_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final String JDBC_OPTIONS =
            "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai";
    private static final String ADMIN_URL =
            "jdbc:mysql://" + DATABASE_HOST + ":" + DATABASE_PORT + "/mysql" + JDBC_OPTIONS;
    private static final String TEST_URL =
            "jdbc:mysql://" + DATABASE_HOST + ":" + DATABASE_PORT + "/" + SCHEMA + JDBC_OPTIONS;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);
    private static final String DEFAULT_PASSWORD = "a1234567";

    static {
        try (Connection connection = DriverManager.getConnection(ADMIN_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + SCHEMA
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TEST_URL);
        registry.add("spring.datasource.username", () -> DATABASE_USERNAME);
        registry.add("spring.datasource.password", () -> DATABASE_PASSWORD);
        registry.add("app.jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("app.jwt.expiration-seconds", () -> 7200L);
    }

    @AfterAll
    static void dropTemporaryDatabase() throws Exception {
        try (Connection connection = DriverManager.getConnection(ADMIN_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS `" + SCHEMA + "`");
        }
    }

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TuXingYanZhengMaFuWu captchaService;

    @Test
    void threeRolesAndMultipleRolesShouldComeOnlyFromDatabase() throws Exception {
        long student = insertUser("student", "StudentPass1", false, "ENABLED", "STUDENT");
        insertStudentProfile(student, "学生甲");
        insertUser("teacher", "TeacherPass1", false, "ENABLED", "TEACHER");
        insertUser("admin", "AdminPass1", false, "ENABLED", "ADMIN");
        long multiRole = insertUser("multi", "MultiRolePass1", false, "ENABLED", "STUDENT");
        addRole(multiRole, "TEACHER", "ACTIVE");

        assertLoginRole("student", "StudentPass1", "STUDENT", "STUDENT");
        assertLoginRole("teacher", "TeacherPass1", "TEACHER", "TEACHER");
        assertLoginRole("admin", "AdminPass1", "ADMIN", "ADMIN");

        TestResponse multi = login("multi", "MultiRolePass1", "STUDENT");
        assertThat(multi.status()).isEqualTo(200);
        List<?> roles = JsonPath.read(multi.body(), "$.user.roles");
        assertThat(asStrings(roles)).containsExactly("STUDENT", "TEACHER");

        TestResponse escalation = login("student", "StudentPass1", "ADMIN");
        assertError(escalation, 403, "ROLE_MISMATCH");
        Integer adminRelations = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM yong_hu_jiao_se yhjs
                JOIN jiao_se js ON js.id=yhjs.jiao_se_id
                WHERE yhjs.yong_hu_id=? AND js.jiao_se_dai_ma='ADMIN'
                """, Integer.class, student);
        assertThat(adminRelations).isZero();
    }

    @Test
    void adminQuestionEndpointsRequireAuthenticatedNonInitialAdminRole() throws Exception {
        String adminToken = token(login("question_admin", "AdminPass1", "ADMIN", false));
        String studentToken = token(login("question_student", "StudentPass1", "STUDENT", false));
        String teacherToken = token(login("question_teacher", "TeacherPass1", "TEACHER", false));
        String initialAdminToken = token(login("question_initial_admin", "AdminPass1", "ADMIN", true));

        assertThat(get("/api/v1/admin/questions?page=1&size=10", null).status()).isEqualTo(401);
        assertThat(get("/api/v1/admin/questions?page=1&size=10", studentToken).status()).isEqualTo(403);
        assertThat(get("/api/v1/admin/questions?page=1&size=10", teacherToken).status()).isEqualTo(403);
        assertError(get("/api/v1/admin/questions?page=1&size=10", initialAdminToken), 403, "MUST_CHANGE_PASSWORD");
        assertThat(get("/api/v1/admin/questions?page=1&size=10", adminToken).status()).isEqualTo(200);
    }

    @Test
    void adminQuestionImportEndpointsRequireAuthenticatedNonInitialAdminRole() throws Exception {
        String adminToken = token(login("question_import_admin", "AdminPass1", "ADMIN", false));
        String studentToken = token(login("question_import_student", "StudentPass1", "STUDENT", false));
        String teacherToken = token(login("question_import_teacher", "TeacherPass1", "TEACHER", false));
        String initialAdminToken = token(login("question_import_initial_admin", "AdminPass1", "ADMIN", true));
        byte[] invalidWorkbook = "not-a-workbook".getBytes(StandardCharsets.UTF_8);

        assertError(uploadPath("/api/v1/admin/question-import/preview", invalidWorkbook, "questions.xlsx", null), 401, "UNAUTHENTICATED");
        assertError(uploadPath("/api/v1/admin/question-import/preview", invalidWorkbook, "questions.xlsx", studentToken), 403, "ACCESS_DENIED");
        assertError(uploadPath("/api/v1/admin/question-import/preview", invalidWorkbook, "questions.xlsx", teacherToken), 403, "ACCESS_DENIED");
        assertError(uploadPath("/api/v1/admin/question-import/preview", invalidWorkbook, "questions.xlsx", initialAdminToken), 403, "MUST_CHANGE_PASSWORD");
        assertError(uploadPath("/api/v1/admin/question-import/preview", invalidWorkbook, "questions.xlsx", adminToken), 400, "WORKBOOK_INVALID");
    }

    @Test
    void adminOperationLogQueryIsRestrictedToActiveAdmin() throws Exception {
        String adminToken = token(login("operation_log_admin", "AdminPass1", "ADMIN", false));
        String studentToken = token(login("operation_log_student", "StudentPass1", "STUDENT", false));
        String initialAdminToken = token(login("operation_log_initial_admin", "AdminPass1", "ADMIN", true));

        assertError(get("/api/v1/admin/operation-logs?page=1&size=10", null), 401, "UNAUTHENTICATED");
        assertError(get("/api/v1/admin/operation-logs?page=1&size=10", studentToken), 403, "ACCESS_DENIED");
        assertError(get("/api/v1/admin/operation-logs?page=1&size=10", initialAdminToken), 403, "MUST_CHANGE_PASSWORD");
        assertThat(get("/api/v1/admin/operation-logs?page=1&size=10", adminToken).status()).isEqualTo(200);
    }

    @Test
    void studentPracticeEndpointsRequireActiveStudentRoleAndProfile() throws Exception {
        long studentId = insertUser("practice_student", "StudentPass1", false, "ENABLED", "STUDENT");
        insertStudentProfile(studentId, "练习学生");
        String studentToken = token(login("practice_student", "StudentPass1", "STUDENT"));
        String teacherToken = token(login("practice_teacher", "TeacherPass1", "TEACHER", false));
        String adminToken = token(login("practice_admin", "AdminPass1", "ADMIN", false));
        long initialStudentId = insertUser("practice_initial_student", "StudentPass1", true, "ENABLED", "STUDENT");
        insertStudentProfile(initialStudentId, "首次登录学生");
        String initialStudentToken = token(login("practice_initial_student", "StudentPass1", "STUDENT"));

        assertError(get("/api/v1/student/practice-options", null), 401, "UNAUTHENTICATED");
        assertError(get("/api/v1/student/practice-options", teacherToken), 403, "ACCESS_DENIED");
        assertError(get("/api/v1/student/practice-options", adminToken), 403, "ACCESS_DENIED");
        assertError(get("/api/v1/student/practice-options", initialStudentToken), 403, "MUST_CHANGE_PASSWORD");
        assertThat(get("/api/v1/student/practice-options", studentToken).status()).isEqualTo(200);
        assertError(get("/api/v1/student/ai/analyses/999999", null), 401, "UNAUTHENTICATED");
        assertError(get("/api/v1/student/ai/analyses/999999", teacherToken), 403, "ACCESS_DENIED");
        assertError(get("/api/v1/student/ai/analyses/999999", adminToken), 403, "ACCESS_DENIED");
        assertError(get("/api/v1/student/ai/analyses/999999", initialStudentToken), 403, "MUST_CHANGE_PASSWORD");
        assertError(get("/api/v1/student/ai/analyses/999999", studentToken), 404, "STUDENT_AI_RESOURCE_NOT_FOUND");
    }

    @Test
    void studentPracticeSessionResponseDoesNotLeakAnswersBeforeSubmission() throws Exception {
        long userId = insertUser("practice_safe_session", "StudentPass1", false, "ENABLED", "STUDENT");
        insertStudentProfile(userId, "练习安全学生");
        insertPublishedPracticeQuestion();
        String accessToken = token(login("practice_safe_session", "StudentPass1", "STUDENT"));

        TestResponse created = post("/api/v1/student/practice-sessions", """
                {"subjectId":1,"questionTypes":["SINGLE_CHOICE"],"count":1}
                """, accessToken);
        assertThat(created.status()).isEqualTo(200);
        assertThat(created.body()).doesNotContain("correctAnswer", "standardAnalysis", "zheng_que_da_an", "shi_fou_zheng_que");
        Number sessionId = JsonPath.read(created.body(), "$.id");
        TestResponse session = get("/api/v1/student/practice-sessions/" + sessionId.longValue(), accessToken);
        assertThat(session.status()).isEqualTo(200);
        assertThat(session.body()).doesNotContain("correctAnswer", "standardAnalysis", "zheng_que_da_an", "shi_fou_zheng_que");
        assertError(get("/api/v1/student/practice-sessions/" + sessionId.longValue() + "/result", accessToken),
                409, "PRACTICE_NOT_SUBMITTED");
    }

    @Test
    void studentPracticeSubmitEndpointDeserializesAnswersAndReturnsResult() throws Exception {
        long userId = insertUser("practice_submit_http", "StudentPass1", false, "ENABLED", "STUDENT");
        insertStudentProfile(userId, "练习提交学生");
        insertPublishedPracticeQuestion();
        String accessToken = token(login("practice_submit_http", "StudentPass1", "STUDENT"));

        TestResponse created = post("/api/v1/student/practice-sessions", """
                {"subjectId":1,"questionTypes":["SINGLE_CHOICE"],"count":1}
                """, accessToken);
        Number sessionId = JsonPath.read(created.body(), "$.id");
        Number practiceQuestionId = JsonPath.read(created.body(), "$.questions[0].practiceQuestionId");

        TestResponse submitted = post("/api/v1/student/practice-sessions/" + sessionId.longValue() + "/submit", """
                {"answers":[{"practiceQuestionId":%d,"answer":"A","elapsedSeconds":1}]}
                """.formatted(practiceQuestionId.longValue()), accessToken);

        assertThat(submitted.status()).isEqualTo(200);
        assertThat(submitted.body()).contains("correctAnswer", "standardAnalysis");
        assertThat(get("/api/v1/student/practice-sessions/" + sessionId.longValue() + "/result", accessToken).status()).isEqualTo(200);
    }

    @Test
    void emptyOrUnreadablePracticeSubmitBodyReturnsBadRequest() throws Exception {
        long userId = insertUser("practice_empty_body", "StudentPass1", false, "ENABLED", "STUDENT");
        insertStudentProfile(userId, "空请求体学生");
        insertPublishedPracticeQuestion();
        String accessToken = token(login("practice_empty_body", "StudentPass1", "STUDENT"));

        Number sessionId = JsonPath.read(post("/api/v1/student/practice-sessions", """
                {"subjectId":1,"questionTypes":["SINGLE_CHOICE"],"count":1}
                """, accessToken).body(), "$.id");

        assertError(post("/api/v1/student/practice-sessions/" + sessionId.longValue() + "/submit", "", accessToken),
                400, "INVALID_REQUEST");
        assertError(post("/api/v1/student/practice-sessions/" + sessionId.longValue() + "/submit", "not-json", accessToken),
                400, "INVALID_REQUEST");
    }

    @Test
    void invalidCredentialsAndUnavailableAccountsShouldBeRejected() throws Exception {
        insertUser("valid", "ValidPass1", false, "ENABLED", "STUDENT");
        insertUser("disabled", "DisabledPass1", false, "DISABLED", "STUDENT");
        insertUser("locked", "LockedPass1", false, "LOCKED", "STUDENT");
        insertUserWithoutRole("no_role", "NoRolePass1", "ENABLED");
        long disabledRelation = insertUserWithoutRole("disabled_relation", "RelationPass1", "ENABLED");
        addRole(disabledRelation, "STUDENT", "DISABLED");
        long deletedUser = insertUser("deleted", "DeletedPass1", false, "ENABLED", "STUDENT");
        jdbcTemplate.update("UPDATE yong_hu SET yi_shan_chu=1 WHERE id=?", deletedUser);

        assertError(login("missing", "SomePass1", "STUDENT"), 401, "INVALID_CREDENTIALS");
        assertError(login("valid", "WrongPass1", "STUDENT"), 401, "INVALID_CREDENTIALS");
        assertError(login("disabled", "DisabledPass1", "STUDENT"), 403, "ACCOUNT_DISABLED");
        assertError(login("locked", "LockedPass1", "STUDENT"), 403, "ACCOUNT_LOCKED");
        assertError(login("no_role", "NoRolePass1", "STUDENT"), 403, "ACCOUNT_HAS_NO_ROLE");
        assertError(login("disabled_relation", "RelationPass1", "STUDENT"),
                403, "ACCOUNT_HAS_NO_ROLE");
        assertError(login("deleted", "DeletedPass1", "STUDENT"), 401, "INVALID_CREDENTIALS");

        insertUser("disabled_role", "DisabledRolePass1", false, "ENABLED", "ADMIN");
        jdbcTemplate.update("UPDATE jiao_se SET zhuang_tai='DISABLED' WHERE jiao_se_dai_ma='ADMIN'");
        try {
            assertError(login("disabled_role", "DisabledRolePass1", "ADMIN"),
                    403, "ACCOUNT_HAS_NO_ROLE");
        } finally {
            jdbcTemplate.update("UPDATE jiao_se SET zhuang_tai='ACTIVE' WHERE jiao_se_dai_ma='ADMIN'");
        }
    }

    @Test
    void jwtShouldAuthenticateAndEnforceRoleBoundaries() throws Exception {
        long studentId = insertUser("role_student", "StudentPass1", false, "ENABLED", "STUDENT");
        insertStudentProfile(studentId, "学生甲");
        insertUser("role_teacher", "TeacherPass1", false, "ENABLED", "TEACHER");
        insertUser("role_admin", "AdminPass1", false, "ENABLED", "ADMIN");

        String studentToken = token(login("role_student", "StudentPass1", "STUDENT"));
        String teacherToken = token(login("role_teacher", "TeacherPass1", "TEACHER"));
        String adminToken = token(login("role_admin", "AdminPass1", "ADMIN"));

        assertThat(get("/api/v1/test/student", studentToken).status()).isEqualTo(200);
        assertError(get("/api/v1/test/teacher", studentToken), 403, "ACCESS_DENIED");
        assertError(get("/api/v1/test/admin", studentToken), 403, "ACCESS_DENIED");
        assertError(get("/api/v1/test/admin", teacherToken), 403, "ACCESS_DENIED");
        assertThat(get("/api/v1/test/admin", adminToken).status()).isEqualTo(200);
        assertError(get("/api/v1/test/student", null), 401, "UNAUTHENTICATED");

        TestResponse me = get("/api/v1/auth/me", studentToken);
        assertThat(me.status()).isEqualTo(200);
        assertThat((String) JsonPath.read(me.body(), "$.username")).isEqualTo("role_student");
        assertThat((String) JsonPath.read(me.body(), "$.displayName")).isEqualTo("学生甲");
        List<?> meRoles = JsonPath.read(me.body(), "$.roles");
        assertThat(asStrings(meRoles)).containsExactly("STUDENT");
    }

    @Test
    void expiredAndTamperedTokensShouldBeRejectedWithoutLeakingToken() throws Exception {
        long userId = insertUser("token_user", "TokenPass1", false, "ENABLED", "STUDENT");
        String validToken = token(login("token_user", "TokenPass1", "STUDENT"));
        String[] tokenParts = validToken.split("\\.");
        String signature = tokenParts[2];
        String tampered = tokenParts[0] + "." + tokenParts[1] + "."
                + (signature.startsWith("a") ? "b" : "a") + signature.substring(1);
        TestResponse tamperedResponse = get("/api/v1/auth/me", tampered);
        assertError(tamperedResponse, 401, "TOKEN_INVALID");
        assertThat(tamperedResponse.body()).doesNotContain(tampered);

        Instant now = Instant.now();
        String expired = Jwts.builder()
                .subject("token_user")
                .claim("uid", userId)
                .claim("roles", List.of("STUDENT"))
                .claim("mustChangePassword", false)
                .issuedAt(Date.from(now.minusSeconds(60)))
                .expiration(Date.from(now.minusSeconds(120)))
                .signWith(Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
        assertError(get("/api/v1/auth/me", expired), 401, "TOKEN_EXPIRED");
    }

    @Test
    void firstLoginGateMustBlockNormalBusinessUntilInitialPasswordChanges() throws Exception {
        long userId = insertUser("first_gate", "InitialPass1", true, "ENABLED", "STUDENT");
        insertStudentProfile(userId, "学生甲");
        TestResponse login = login("first_gate", "InitialPass1", "STUDENT");
        assertThat(login.status()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(login.body(), "$.mustChangePassword")).isTrue();
        String accessToken = token(login);

        assertThat(get("/api/v1/auth/me", accessToken).status()).isEqualTo(200);
        assertError(get("/api/v1/profile", accessToken), 403, "MUST_CHANGE_PASSWORD");

        TestResponse changed = changePassword(
                accessToken, "InitialPass1", "ChangedPass2", "ChangedPass2");
        String changedToken = token(changed);
        assertThat(get("/api/v1/profile", changedToken).status()).isEqualTo(200);
    }

    @Test
    void defaultPasswordMustTriggerGateEvenWhenFirstLoginFlagDrifted() throws Exception {
        insertUser("default_flag_true", DEFAULT_PASSWORD, true, "ENABLED", "STUDENT");
        insertUser("custom_flag_true", "CustomPass1", true, "ENABLED", "STUDENT");
        long defaultFlagFalse = insertUser("default_flag_false", DEFAULT_PASSWORD, false, "ENABLED", "STUDENT");
        insertUser("custom_flag_false", "CustomPass2", false, "ENABLED", "STUDENT");

        assertThat((Boolean) JsonPath.read(login("default_flag_true", DEFAULT_PASSWORD, "STUDENT").body(), "$.mustChangePassword")).isTrue();
        assertThat((Boolean) JsonPath.read(login("custom_flag_true", "CustomPass1", "STUDENT").body(), "$.mustChangePassword")).isTrue();
        TestResponse driftedLogin = login("default_flag_false", DEFAULT_PASSWORD, "STUDENT");
        assertThat((Boolean) JsonPath.read(driftedLogin.body(), "$.mustChangePassword")).isTrue();
        String driftedToken = token(driftedLogin);
        assertError(get("/api/v1/test/student", driftedToken), 403, "MUST_CHANGE_PASSWORD");
        assertThat((Boolean) JsonPath.read(get("/api/v1/auth/me", driftedToken).body(), "$.mustChangePassword")).isTrue();
        assertThat((Boolean) JsonPath.read(login("custom_flag_false", "CustomPass2", "STUDENT").body(), "$.mustChangePassword")).isFalse();

        assertError(changePassword(driftedToken, DEFAULT_PASSWORD, DEFAULT_PASSWORD, DEFAULT_PASSWORD),
                400, "PASSWORD_MUST_NOT_BE_DEFAULT");
        TestResponse changed = changePassword(driftedToken, DEFAULT_PASSWORD, "Abcd12345", "Abcd12345");
        assertThat(changed.status()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(changed.body(), "$.mustChangePassword")).isFalse();
        assertThat(jdbcTemplate.queryForObject("SELECT shi_fou_shou_ci_deng_lu FROM yong_hu WHERE id=?", Boolean.class, defaultFlagFalse)).isFalse();
        assertThat(PASSWORD_ENCODER.matches(DEFAULT_PASSWORD, passwordHash(defaultFlagFalse))).isFalse();
        String changedToken = token(changed);
        assertThat(get("/api/v1/test/student", changedToken).status()).isEqualTo(200);

        String ordinaryToken = token(login("custom_flag_false", "CustomPass2", "STUDENT"));
        assertError(post("/api/v1/auth/change-password", """
                {"oldPassword":"CustomPass2","newPassword":"a1234567","confirmPassword":"a1234567"}
                """, ordinaryToken), 400, "PASSWORD_MUST_NOT_BE_DEFAULT");
    }

    @Test
    void passwordChangeShouldValidateInputAndReplaceHashAtomically() throws Exception {
        long userId = insertUser("change_user", "InitialPass1", true, "ENABLED", "STUDENT");
        String initialHash = passwordHash(userId);
        String initialToken = token(login("change_user", "InitialPass1", "STUDENT"));

        assertError(changePassword(initialToken, "WrongPass1", "NewPassword2", "NewPassword2"),
                400, "OLD_PASSWORD_INCORRECT");
        assertError(changePassword(initialToken, "InitialPass1", "NewPassword2", "DifferentPass2"),
                400, "PASSWORD_CONFIRMATION_MISMATCH");
        assertError(changePassword(initialToken, "InitialPass1", "InitialPass1", "InitialPass1"),
                400, "PASSWORD_UNCHANGED");
        assertError(changePassword(initialToken, "InitialPass1", "onlyletters", "onlyletters"),
                400, "PASSWORD_POLICY_VIOLATION");

        TestResponse changed = changePassword(
                initialToken, "InitialPass1", "NewPassword2", "NewPassword2");
        assertThat(changed.status()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(changed.body(), "$.mustChangePassword")).isFalse();
        String newToken = token(changed);

        String changedHash = passwordHash(userId);
        assertThat(changedHash).isNotEqualTo(initialHash);
        assertThat(PASSWORD_ENCODER.matches("NewPassword2", changedHash)).isTrue();
        Boolean firstLogin = jdbcTemplate.queryForObject(
                "SELECT shi_fou_shou_ci_deng_lu FROM yong_hu WHERE id=?", Boolean.class, userId);
        LocalDateTime changedAt = jdbcTemplate.queryForObject(
                "SELECT mi_ma_xiu_gai_shi_jian FROM yong_hu WHERE id=?", LocalDateTime.class, userId);
        assertThat(firstLogin).isFalse();
        assertThat(changedAt).isNotNull();
        assertError(login("change_user", "InitialPass1", "STUDENT"), 401, "INVALID_CREDENTIALS");
        assertThat(login("change_user", "NewPassword2", "STUDENT").status()).isEqualTo(200);
        assertThat(get("/api/v1/test/student", newToken).status()).isEqualTo(200);
    }

    @Test
    void successfulLoginShouldUpdateAuditTimeAndHealthShouldRemainPublic() throws Exception {
        long userId = insertUser("audit_user", "AuditPass1", false, "ENABLED", "ADMIN");
        assertThat(login("audit_user", "AuditPass1", "ADMIN").status()).isEqualTo(200);
        LocalDateTime loginAt = jdbcTemplate.queryForObject(
                "SELECT zui_hou_deng_lu_shi_jian FROM yong_hu WHERE id=?", LocalDateTime.class, userId);
        assertThat(loginAt).isNotNull();

        TestResponse health = get("/api/v1/health", null);
        assertThat(health.status()).isEqualTo(200);
        assertThat(health.body()).contains("\"status\":\"UP\"", "\"database\":\"UP\"");
        assertError(post("/api/v1/auth/login", "{}", null), 400, "INVALID_REQUEST");
    }

    @Test
    void adminClassManagementShouldValidateRulesAndAccess() throws Exception {
        String adminToken = token(login("class_admin", "AdminPass1", "ADMIN", false));
        String studentToken = token(login("class_student", "StudentPass1", "STUDENT", false));
        String teacherToken = token(login("class_teacher", "TeacherPass1", "TEACHER", false));
        String firstAdminToken = token(login("class_first_admin", "AdminPass1", "ADMIN", true));

        assertError(get("/api/v1/admin/classes", null), 401, "UNAUTHENTICATED");
        assertError(get("/api/v1/admin/classes", studentToken), 403, "ACCESS_DENIED");
        assertError(get("/api/v1/admin/classes", teacherToken), 403, "ACCESS_DENIED");
        assertError(get("/api/v1/admin/classes", firstAdminToken), 403, "MUST_CHANGE_PASSWORD");

        TestResponse created = json("POST", "/api/v1/admin/classes", """
                {"classCode":" G1-A ","className":" 一班 ","grade":" 高一 ","enrollmentYear":2025}
                """, adminToken);
        assertThat(created.status()).isEqualTo(200);
        assertThat((String) JsonPath.read(created.body(), "$.classCode")).isEqualTo("G1-A");
        assertThat((String) JsonPath.read(created.body(), "$.status")).isEqualTo("ACTIVE");
        Integer id = JsonPath.read(created.body(), "$.id");

        assertError(json("POST", "/api/v1/admin/classes", """
                {"classCode":"G1-A","className":"重复","grade":"高一","enrollmentYear":2025}
                """, adminToken), 409, "CLASS_CODE_EXISTS");
        assertError(json("POST", "/api/v1/admin/classes", """
                {"classCode":" ","className":"一班","grade":"高一","enrollmentYear":2025}
                """, adminToken), 400, "INVALID_REQUEST");
        assertError(json("POST", "/api/v1/admin/classes", """
                {"classCode":"G1-B","className":" ","grade":"高一","enrollmentYear":2025}
                """, adminToken), 400, "INVALID_REQUEST");
        assertError(json("POST", "/api/v1/admin/classes", """
                {"classCode":"G1-B","className":"二班","grade":"高一","enrollmentYear":1999}
                """, adminToken), 400, "INVALID_REQUEST");
        assertThat(json("POST", "/api/v1/admin/classes", """
                {"classCode":"G2-B","className":"二班","grade":"高二","enrollmentYear":2024}
                """, adminToken).status()).isEqualTo(200);

        assertThat(get("/api/v1/admin/classes?page=1&size=1&code=G1-A", adminToken).body()).contains("\"total\":1");
        assertThat(get("/api/v1/admin/classes?code=G1-A", adminToken).body()).contains("G1-A");
        assertThat(get("/api/v1/admin/classes?name=二班", adminToken).body()).contains("G2-B");
        assertThat(get("/api/v1/admin/classes?grade=高二", adminToken).body()).contains("G2-B");
        assertThat(get("/api/v1/admin/classes?status=ACTIVE", adminToken).body()).contains("G1-A", "G2-B");
        assertThat(get("/api/v1/admin/classes/" + id, adminToken).status()).isEqualTo(200);
        assertError(get("/api/v1/admin/classes/999999", adminToken), 404, "CLASS_NOT_FOUND");

        TestResponse updated = json("PUT", "/api/v1/admin/classes/" + id, """
                {"className":"一班（调整）","grade":"高二","enrollmentYear":2024}
                """, adminToken);
        assertThat(updated.body()).contains("一班（调整）", "高二", "G1-A");
        assertThat(json("PATCH", "/api/v1/admin/classes/" + id + "/status", "{\"status\":\"DISABLED\"}", adminToken).body()).contains("DISABLED");
        assertThat(json("PATCH", "/api/v1/admin/classes/" + id + "/status", "{\"status\":\"GRADUATED\"}", adminToken).body()).contains("GRADUATED");
        assertError(json("PATCH", "/api/v1/admin/classes/" + id + "/status", "{\"status\":\"UNKNOWN\"}", adminToken), 400, "INVALID_CLASS_STATUS");
    }

    @Test
    void teacherManagementShouldRequireAdminAndRejectDuplicateTriple() throws Exception {
        String adminToken = token(login("teacher_admin", "AdminPass1", "ADMIN", false));
        String studentToken = token(login("teacher_student", "StudentPass1", "STUDENT", false));
        String teacherToken = token(login("teacher_teacher", "TeacherPass1", "TEACHER", false));
        String firstAdminToken = token(login("teacher_first_admin", "AdminPass1", "ADMIN", true));
        assertError(get("/api/v1/admin/teachers", null), 401, "UNAUTHENTICATED");
        assertError(get("/api/v1/admin/teachers", studentToken), 403, "ACCESS_DENIED");
        assertError(get("/api/v1/admin/teachers", teacherToken), 403, "ACCESS_DENIED");
        assertError(get("/api/v1/admin/teachers", firstAdminToken), 403, "MUST_CHANGE_PASSWORD");
        TestResponse created = json("POST", "/api/v1/admin/teachers", """
                {"employeeNumber":"T-API-01","name":"教师接口甲","username":"teacher_created","initialPassword":"TeacherPass1","accountStatus":"ENABLED"}
                """, adminToken);
        assertThat(created.status()).isEqualTo(200); assertThat(created.body()).contains("initialPassword");
        Integer teacherId = JsonPath.read(created.body(), "$.teacher.id");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM yong_hu_jiao_se ur JOIN jiao_se r ON r.id=ur.jiao_se_id
                JOIN jiao_shi_dang_an p ON p.yong_hu_id=ur.yong_hu_id
                WHERE p.id=? AND r.jiao_se_dai_ma='TEACHER' AND ur.zhuang_tai='ACTIVE'
                """, Integer.class, teacherId)).isEqualTo(1);
        long classId = createClass("T-API-C", "教师接口班", "高一", "ACTIVE");
        TestResponse relation = json("POST", "/api/v1/admin/teachers/" + teacherId + "/teaching-assignments", """
                {"classId":%d,"subjectId":1,"primary":true,"startTime":"2026-08-05T08:00:00"}
                """.formatted(classId), adminToken);
        assertThat(relation.status()).isEqualTo(200);
        assertError(json("POST", "/api/v1/admin/teachers/" + teacherId + "/teaching-assignments", """
                {"classId":%d,"subjectId":1,"primary":false,"startTime":"2026-08-05T08:00:00"}
                """.formatted(classId), adminToken), 409, "TEACHING_ASSIGNMENT_EXISTS");
    }

    @Test
    void studentImportTemplateAndPreviewShouldValidateWithoutWritingStudents() throws Exception {
        String adminToken = token(login("import_admin", "AdminPass1", "ADMIN", false));
        String studentToken = token(login("import_student", "StudentPass1", "STUDENT", false));
        String teacherToken = token(login("import_teacher", "TeacherPass1", "TEACHER", false));
        String firstAdminToken = token(login("import_first_admin", "AdminPass1", "ADMIN", true));
        long activeClass = createClass("199", "高三一班", "高三", "ACTIVE");
        createClass("DISABLED-CLASS", "停用班", "高三", "DISABLED");
        createClass("GRADUATED-CLASS", "毕业班", "高三", "GRADUATED");
        long existingUser = insertUser("existing_username", "StudentPass1", false, "ENABLED", "STUDENT");
        jdbcTemplate.update("INSERT INTO xue_sheng_dang_an(yong_hu_id,xue_hao,xing_ming,nian_ji) VALUES (?,?,?,?)",
                existingUser, "existing_student", "已有学生", "高三");
        int userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM yong_hu", Integer.class);
        int studentCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM xue_sheng_dang_an", Integer.class);
        int relationCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ban_ji_xue_sheng", Integer.class);

        assertThat(get("/api/v1/admin/student-import/template", null).status()).isEqualTo(401);
        assertThat(get("/api/v1/admin/student-import/template", studentToken).status()).isEqualTo(403);
        assertThat(get("/api/v1/admin/student-import/template", teacherToken).status()).isEqualTo(403);
        assertError(get("/api/v1/admin/student-import/template", firstAdminToken), 403, "MUST_CHANGE_PASSWORD");
        byte[] template = getBytes("/api/v1/admin/student-import/template", adminToken).body();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(template))) {
            assertThat(workbook.getSheet("学生导入")).isNotNull();
            assertThat(workbook.getSheet("填写说明")).isNotNull();
            assertThat(workbook.getSheet("学生导入").getRow(0).getCell(0).getStringCellValue()).isEqualTo("xue_hao");
            assertThat(workbook.getSheet("学生导入").getDataValidations()).isNotEmpty();
        }

        byte[] valid = workbookBytes(List.of(
                new String[] {"20260001", "学生甲", "199", "高三", "explicit_user", "", "ENABLED"},
                new String[] {"20260002", "学生乙", "199", "高三", "", "", ""}), false);
        assertError(upload(valid, "student-import-valid.xlsx", null), 401, "UNAUTHENTICATED");
        assertThat(upload(valid, "student-import-valid.xlsx", studentToken).status()).isEqualTo(403);
        assertThat(upload(valid, "student-import-valid.xlsx", teacherToken).status()).isEqualTo(403);
        assertError(upload(valid, "student-import-valid.xlsx", firstAdminToken), 403, "MUST_CHANGE_PASSWORD");
        TestResponse validPreview = upload(valid, "student-import-valid.xlsx", adminToken);
        assertThat(validPreview.status()).isEqualTo(200);
        assertThat(validPreview.body()).contains("\"validCount\":2", "explicit_user", "\"passwordWillGenerate\":true", "\"accountStatus\":\"ENABLED\"");

        byte[] invalid = workbookBytes(List.of(
                new String[] {"", "学生甲", "199", "高三", "", "", ""},
                new String[] {"20260003", "", "199", "高三", "", "", ""},
                new String[] {"20260004", "学生丁", "", "高三", "", "", ""},
                new String[] {"20260005", "学生戊", "NO-CLASS", "高三", "", "", ""},
                new String[] {"20260006", "学生己", "DISABLED-CLASS", "高三", "", "", ""},
                new String[] {"20260007", "学生庚", "GRADUATED-CLASS", "高三", "", "", ""},
                new String[] {"20260008", "学生辛", "199", "高二", "", "", "LOCKED"},
                new String[] {"existing_student", "学生壬", "199", "高三", "existing_username", "onlyletters", ""},
                new String[] {"20260008", "学生癸", "199", "高三", "existing_username", "", ""}), false);
        TestResponse invalidPreview = upload(invalid, "student-import-invalid.xlsx", adminToken);
        assertThat(invalidPreview.status()).isEqualTo(200);
        assertThat(invalidPreview.body()).contains("STUDENT_NUMBER_REQUIRED", "NAME_REQUIRED", "CLASS_CODE_REQUIRED",
                "CLASS_NOT_FOUND", "CLASS_NOT_ACTIVE", "GRADE_CLASS_MISMATCH", "INVALID_ACCOUNT_STATUS",
                "STUDENT_NUMBER_ALREADY_EXISTS", "USERNAME_ALREADY_EXISTS", "PASSWORD_POLICY_VIOLATION",
                "STUDENT_NUMBER_DUPLICATE_IN_FILE", "USERNAME_DUPLICATE_IN_FILE");
        assertError(upload(new byte[0], "empty.xlsx", adminToken), 400, "FILE_EMPTY");
        assertError(upload("not-xlsx".getBytes(), "fake.txt", adminToken), 400, "FILE_TYPE_INVALID");
        assertError(upload("not-xlsx".getBytes(), "broken.xlsx", adminToken), 400, "WORKBOOK_INVALID");
        assertError(upload(workbookBytes(List.<String[]>of(new String[] {"20260009", "学生甲", "199", "高三", "", "", ""}), true),
                "formula.xlsx", adminToken), 400, "FORMULA_CELL_NOT_ALLOWED");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM yong_hu", Integer.class)).isEqualTo(userCount);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM xue_sheng_dang_an", Integer.class)).isEqualTo(studentCount);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ban_ji_xue_sheng", Integer.class)).isEqualTo(relationCount);
    }

    @Test
    void studentImportConfirmShouldWriteFourTablesAtomically() throws Exception {
        String adminToken = token(login("confirm_admin", "AdminPass1", "ADMIN", false));
        String studentToken = token(login("confirm_student", "StudentPass1", "STUDENT", false));
        String teacherToken = token(login("confirm_teacher", "TeacherPass1", "TEACHER", false));
        String initialAdminToken = token(login("confirm_initial_admin", "AdminPass1", "ADMIN", true));
        createClass("CONFIRM-CLASS", "确认导入班", "高三", "ACTIVE");
        byte[] workbook = workbookBytes(List.of(
                new String[] {"20260101", "学生甲", "CONFIRM-CLASS", "高三", "", "", ""},
                new String[] {"20260102", "学生乙", "CONFIRM-CLASS", "高三", "confirm_two", "CustomPass1", "DISABLED"}), false);
        assertThat(uploadPath("/api/v1/admin/student-import/confirm", workbook, "confirm.xlsx", null).status()).isEqualTo(401);
        assertThat(uploadPath("/api/v1/admin/student-import/confirm", workbook, "confirm.xlsx", studentToken).status()).isEqualTo(403);
        assertThat(uploadPath("/api/v1/admin/student-import/confirm", workbook, "confirm.xlsx", teacherToken).status()).isEqualTo(403);
        TestResponse response = uploadPath("/api/v1/admin/student-import/confirm", workbook, "confirm.xlsx", adminToken);
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body()).contains("\"importedCount\":2", "CustomPass1", "\"mustChangePassword\":true");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM yong_hu WHERE yong_hu_ming IN ('20260101','confirm_two')", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM xue_sheng_dang_an WHERE xue_hao IN ('20260101','20260102')", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ban_ji_xue_sheng WHERE zhuang_tai='ACTIVE'", Integer.class)).isGreaterThanOrEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT shi_fou_shou_ci_deng_lu FROM yong_hu WHERE yong_hu_ming='20260101'", Boolean.class)).isTrue();
        assertThat(PASSWORD_ENCODER.matches("CustomPass1", jdbcTemplate.queryForObject("SELECT mi_ma_zhai_yao FROM yong_hu WHERE yong_hu_ming='confirm_two'", String.class))).isTrue();
        assertThat(uploadPath("/api/v1/admin/student-import/confirm", workbook, "confirm.xlsx", adminToken).status()).isEqualTo(400);
    }

    @Test
    void captchaShouldRequireCorrectCodeAndAllowUnifiedMultiRoleLogin() throws Exception {
        long userId = insertUser("captcha_multi", "CaptchaPass1", false, "ENABLED", "STUDENT");
        addRole(userId, "TEACHER", "ACTIVE");
        assertError(post("/api/v1/auth/login", "{\"username\":\"captcha_multi\",\"password\":\"CaptchaPass1\"}", null), 400, "CAPTCHA_CHALLENGE_REQUIRED");
        TestResponse challenge = get("/api/v1/auth/captcha-challenge", null);
        String id = JsonPath.read(challenge.body(), "$.challengeId");
        String code = JsonPath.read(challenge.body(), "$.testCode");
        assertThat((String) JsonPath.read(challenge.body(), "$.image")).startsWith("data:image/png;base64,");
        assertError(post("/api/v1/auth/login", """
                {"username":"captcha_multi","password":"CaptchaPass1","challengeId":"%s","captchaCode":"WRONG"}
                """.formatted(id), null), 400, "CAPTCHA_INCORRECT");
        assertError(post("/api/v1/auth/login", """
                {"username":"captcha_multi","password":"CaptchaPass1","challengeId":"%s","captchaCode":"%s"}
                """.formatted(id, code), null), 400, "CAPTCHA_CHALLENGE_REUSED");
        TestResponse unified = loginWithoutExpectedRole("captcha_multi", "CaptchaPass1");
        assertThat(unified.status()).isEqualTo(200);
        assertThat(asStrings(JsonPath.read(unified.body(), "$.user.roles"))).containsExactly("STUDENT", "TEACHER");
    }

    @Test
    void captchaShouldBeCaseInsensitiveAndOneTimeAfterSuccessfulLogin() throws Exception {
        insertUser("captcha_case", "CaptchaPass1", false, "ENABLED", "STUDENT");
        TestResponse challenge = get("/api/v1/auth/captcha-challenge", null);
        String id = JsonPath.read(challenge.body(), "$.challengeId");
        String code = JsonPath.read(challenge.body(), "$.testCode");
        TestResponse success = post("/api/v1/auth/login", """
                {"username":"captcha_case","password":"CaptchaPass1","challengeId":"%s","captchaCode":"%s"}
                """.formatted(id, code.toLowerCase()), null);
        assertThat(success.status()).isEqualTo(200);
        assertError(post("/api/v1/auth/login", """
                {"username":"captcha_case","password":"CaptchaPass1","challengeId":"%s","captchaCode":"%s"}
                """.formatted(id, code), null), 400, "CAPTCHA_CHALLENGE_REUSED");
    }

    @Test
    void captchaRefreshShouldInvalidatePreviousChallengeAndExpiredChallengeShouldBeRejected() throws Exception {
        insertUser("captcha_refresh", "CaptchaPass1", false, "ENABLED", "STUDENT");
        TestResponse first = get("/api/v1/auth/captcha-challenge", null);
        String firstId = JsonPath.read(first.body(), "$.challengeId");
        String firstCode = JsonPath.read(first.body(), "$.testCode");
        TestResponse second = get("/api/v1/auth/captcha-challenge?previousChallengeId=" + firstId, null);
        String secondId = JsonPath.read(second.body(), "$.challengeId");
        String secondCode = JsonPath.read(second.body(), "$.testCode");
        assertThat(secondId).isNotEqualTo(firstId);
        assertError(post("/api/v1/auth/login", """
                {"username":"captcha_refresh","password":"CaptchaPass1","challengeId":"%s","captchaCode":"%s"}
                """.formatted(firstId, firstCode), null), 400, "CAPTCHA_CHALLENGE_REUSED");

        captchaService.expireForTest(secondId);
        assertError(post("/api/v1/auth/login", """
                {"username":"captcha_refresh","password":"CaptchaPass1","challengeId":"%s","captchaCode":"%s"}
                """.formatted(secondId, secondCode), null), 400, "CAPTCHA_CHALLENGE_EXPIRED");
    }

    @Test
    void ordinaryPasswordChangeShouldReplacePasswordAndRejectOldOne() throws Exception {
        insertUser("regular_password", "OldPass1", false, "ENABLED", "STUDENT");
        String token = token(login("regular_password", "OldPass1", "STUDENT"));
        TestResponse changed = post("/api/v1/auth/change-password", """
                {"oldPassword":"OldPass1","newPassword":"NewPass2","confirmPassword":"NewPass2"}
                """, token);
        assertThat(changed.status()).isEqualTo(200);
        assertError(login("regular_password", "OldPass1", "STUDENT"), 401, "INVALID_CREDENTIALS");
        assertThat(login("regular_password", "NewPass2", "STUDENT").status()).isEqualTo(200);
    }

    @Test
    void teacherTeachingScopesShouldOnlyReturnCurrentTeacherTriples() throws Exception {
        long teacherUser = insertUser("scope_teacher", "TeacherPass1", false, "ENABLED", "TEACHER");
        long otherUser = insertUser("scope_other", "TeacherPass1", false, "ENABLED", "TEACHER");
        long classId = createClass("SCOPE-1", "范围班", "高二", "ACTIVE");
        jdbcTemplate.update("INSERT INTO jiao_shi_dang_an(yong_hu_id,gong_hao,xing_ming) VALUES (?,?,?)", teacherUser, "SCOPE-T1", "范围教师");
        jdbcTemplate.update("INSERT INTO jiao_shi_dang_an(yong_hu_id,gong_hao,xing_ming) VALUES (?,?,?)", otherUser, "SCOPE-T2", "其他教师");
        long teacherId = jdbcTemplate.queryForObject("SELECT id FROM jiao_shi_dang_an WHERE yong_hu_id=?", Long.class, teacherUser);
        long otherId = jdbcTemplate.queryForObject("SELECT id FROM jiao_shi_dang_an WHERE yong_hu_id=?", Long.class, otherUser);
        jdbcTemplate.update("INSERT INTO ren_ke_guan_xi(jiao_shi_id,ban_ji_id,ke_mu_id,shi_fou_zhu_ren_ke,zhuang_tai,kai_shi_shi_jian) VALUES (?,?,1,1,'ACTIVE',NOW())", teacherId, classId);
        jdbcTemplate.update("INSERT INTO ren_ke_guan_xi(jiao_shi_id,ban_ji_id,ke_mu_id,shi_fou_zhu_ren_ke,zhuang_tai,kai_shi_shi_jian) VALUES (?,?,2,0,'ACTIVE',NOW())", otherId, classId);
        String teacherToken = token(login("scope_teacher", "TeacherPass1", "TEACHER"));
        assertThat(get("/api/v1/teacher/teaching-scopes", teacherToken).body()).contains("范围班", "PHYSICS").doesNotContain("CHEMISTRY");
        String studentToken = token(login("scope_student", "StudentPass1", "STUDENT", false));
        assertError(get("/api/v1/teacher/teaching-scopes", studentToken), 403, "ACCESS_DENIED");
    }

    private long createClass(String code, String name, String grade, String status) {
        jdbcTemplate.update("INSERT INTO ban_ji(ban_ji_bian_ma,ban_ji_ming_cheng,nian_ji,ru_xue_nian_fen,zhuang_tai) VALUES (?,?,?,?,?)",
                code, name, grade, 2025, status);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void insertPublishedPracticeQuestion() {
        String hash = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("""
                INSERT INTO ti_mu(ke_mu_id,ti_mu_lei_xing,shi_yong_mo_shi,ti_gan,zheng_que_da_an,nan_du,
                    shi_fou_ke_zi_dong_pan_fen,zhuang_tai,nei_rong_ha_xi)
                VALUES (1,'SINGLE_CHOICE','ONLINE_PRACTICE','HTTP安全题',?,1,1,'PUBLISHED',?)
                """, "{\"schemaVersion\":1,\"type\":\"SINGLE_CHOICE\",\"optionLabels\":[\"A\"]}", hash);
        long questionId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("""
                INSERT INTO ti_mu_xuan_xiang(ti_mu_id,xuan_xiang_biao_shi,xuan_xiang_nei_rong,shi_fou_zheng_que,pai_xu)
                VALUES (?,'A','选项A',1,1),(?,'B','选项B',0,2)
                """, questionId, questionId);
        jdbcTemplate.update("""
                INSERT INTO ti_mu_jie_xi(ti_mu_id,jie_xi_lei_xing,jie_xi_nei_rong,ban_ben_hao,zhuang_tai)
                VALUES (?,'STANDARD','HTTP标准解析',1,'PUBLISHED')
                """, questionId);
        long pointId = jdbcTemplate.queryForObject("SELECT id FROM zhi_shi_dian WHERE ke_mu_id=1 AND zhuang_tai='ACTIVE' LIMIT 1", Long.class);
        jdbcTemplate.update("INSERT INTO ti_mu_zhi_shi_dian(ti_mu_id,zhi_shi_dian_id,shi_fou_zhu_yao,pai_xu) VALUES (?,?,1,1)", questionId, pointId);
    }

    private byte[] workbookBytes(List<String[]> rows, boolean formula) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("学生导入");
            String[] headers = {"xue_hao", "xing_ming", "ban_ji_bian_ma", "nian_ji", "yong_hu_ming", "chu_shi_mi_ma", "zhang_hao_zhuang_tai"};
            var header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                var row = sheet.createRow(rowIndex + 1);
                for (int column = 0; column < rows.get(rowIndex).length; column++) row.createCell(column).setCellValue(rows.get(rowIndex)[column]);
            }
            if (formula) sheet.getRow(1).getCell(0).setCellFormula("1+1");
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private TestResponse upload(byte[] bytes, String filename, String accessToken) throws Exception {
        return uploadPath("/api/v1/admin/student-import/preview", bytes, filename, accessToken);
    }

    private TestResponse uploadPath(String path, byte[] bytes, String filename, String accessToken) throws Exception {
        String boundary = "----test" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + filename
                + "\"\r\nContent-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n").getBytes());
        body.write(bytes);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes());
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(baseUrl() + path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()));
        addAuthorization(builder, accessToken);
        return send(builder.build());
    }

    private ByteResponse getBytes(String path, String accessToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(baseUrl() + path)).GET();
        addAuthorization(builder, accessToken);
        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        return new ByteResponse(response.statusCode(), response.body());
    }

    private void assertLoginRole(String username, String password, String expectedRole, String returnedRole)
            throws Exception {
        TestResponse response = login(username, password, expectedRole);
        assertThat(response.status()).isEqualTo(200);
        assertThat((String) JsonPath.read(response.body(), "$.tokenType")).isEqualTo("Bearer");
        assertThat((Integer) JsonPath.read(response.body(), "$.expiresIn")).isEqualTo(7200);
        List<?> responseRoles = JsonPath.read(response.body(), "$.user.roles");
        assertThat(asStrings(responseRoles)).contains(returnedRole);
        assertThat((String) JsonPath.read(response.body(), "$.accessToken")).isNotBlank();
    }

    private TestResponse login(String username, String password, String expectedRole) throws Exception {
        TestResponse challenge = get("/api/v1/auth/captcha-challenge", null);
        String challengeId = JsonPath.read(challenge.body(), "$.challengeId");
        String captchaCode = JsonPath.read(challenge.body(), "$.testCode");
        return post("/api/v1/auth/login", """
                {"username":"%s","password":"%s","expectedRole":"%s","challengeId":"%s","captchaCode":"%s"}
                """.formatted(username, password, expectedRole, challengeId, captchaCode), null);
    }

    private TestResponse loginWithoutExpectedRole(String username, String password) throws Exception {
        TestResponse challenge = get("/api/v1/auth/captcha-challenge", null);
        return post("/api/v1/auth/login", """
                {"username":"%s","password":"%s","challengeId":"%s","captchaCode":"%s"}
                """.formatted(username, password, JsonPath.read(challenge.body(), "$.challengeId"),
                JsonPath.read(challenge.body(), "$.testCode")), null);
    }

    private TestResponse changePassword(
            String accessToken,
            String oldPassword,
            String newPassword,
            String confirmPassword) throws Exception {
        return post("/api/v1/auth/change-initial-password", """
                {"oldPassword":"%s","newPassword":"%s","confirmPassword":"%s"}
                """.formatted(oldPassword, newPassword, confirmPassword), accessToken);
    }

    private TestResponse get(String path, String accessToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(baseUrl() + path)).GET();
        addAuthorization(builder, accessToken);
        return send(builder.build());
    }

    private TestResponse post(String path, String body, String accessToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        addAuthorization(builder, accessToken);
        return send(builder.build());
    }

    private TestResponse json(String method, String path, String body, String accessToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(baseUrl() + path))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body));
        addAuthorization(builder, accessToken);
        return send(builder.build());
    }

    private TestResponse login(String username, String password, String role, boolean firstLogin) throws Exception {
        insertUser(username, password, firstLogin, "ENABLED", role);
        return login(username, password, role);
    }

    private void addAuthorization(HttpRequest.Builder builder, String accessToken) {
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
    }

    private TestResponse send(HttpRequest request) throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        return new TestResponse(response.statusCode(), response.body());
    }

    private String token(TestResponse response) {
        assertThat(response.status()).isEqualTo(200);
        return JsonPath.read(response.body(), "$.accessToken");
    }

    private void assertError(TestResponse response, int status, String code) {
        assertThat(response.status()).isEqualTo(status);
        assertThat((String) JsonPath.read(response.body(), "$.code")).isEqualTo(code);
        assertThat(response.body()).doesNotContain("SQLException", "SELECT ", "java.");
    }

    private long insertUser(
            String username,
            String password,
            boolean firstLogin,
            String status,
            String role) {
        long userId = insertUserWithoutRole(username, password, status, firstLogin);
        addRole(userId, role, "ACTIVE");
        return userId;
    }

    private long insertUserWithoutRole(String username, String password, String status) {
        return insertUserWithoutRole(username, password, status, false);
    }

    private long insertUserWithoutRole(
            String username,
            String password,
            String status,
            boolean firstLogin) {
        jdbcTemplate.update("""
                INSERT INTO yong_hu(
                    yong_hu_ming,mi_ma_zhai_yao,zhang_hao_zhuang_tai,shi_fou_shou_ci_deng_lu
                ) VALUES (?,?,?,?)
                """, username, PASSWORD_ENCODER.encode(password), status, firstLogin);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void addRole(long userId, String role, String relationStatus) {
        jdbcTemplate.update("""
                INSERT INTO yong_hu_jiao_se(yong_hu_id,jiao_se_id,zhuang_tai)
                SELECT ?,id,? FROM jiao_se WHERE jiao_se_dai_ma=?
                """, userId, relationStatus, role);
    }

    private void insertStudentProfile(long userId, String name) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        jdbcTemplate.update("""
                INSERT INTO xue_sheng_dang_an(yong_hu_id,xue_hao,xing_ming,nian_ji)
                VALUES (?,?,?,'高一')
                """, userId, "S" + suffix, name);
    }

    private String passwordHash(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT mi_ma_zhai_yao FROM yong_hu WHERE id=?", String.class, userId);
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private List<String> asStrings(List<?> values) {
        return values.stream().map(String::valueOf).toList();
    }

    private static String environment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for MySQL integration tests");
        }
        return value;
    }

    private record TestResponse(int status, String body) {
    }

    private record ByteResponse(int status, byte[] body) {
    }
}
