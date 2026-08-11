package com.neu.riketiku.tiku.fujian;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import com.neu.riketiku.tiku.admin.QuestionAdminService;
import com.neu.riketiku.tiku.admin.QuestionContentHashService;
import com.neu.riketiku.tiku.admin.QuestionDtos;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuestionAttachmentAdminIntegrationTest extends AdminQuestionIntegrationTestSupport {
    private static final String PASSWORD = "a1234567";
    private static final Path STORAGE_ROOT = Path.of(System.getProperty("java.io.tmpdir"),
            "rike-tiku-admin-attachment-" + UUID.randomUUID());
    private static final String USERNAME = "admin_attachment_upload";
    private static long questionId;

    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private QuestionContentHashService contentHashService;
    @Autowired
    private QuestionAdminService questionAdminService;

    @DynamicPropertySource
    static void attachmentProperties(DynamicPropertyRegistry registry) {
        registry.add("rike.tiku.attachment.storage-root", () -> STORAGE_ROOT.toString());
    }

    @BeforeAll
    static void noOpBeforeAll() {
        // The database and data are created per test instance after Spring starts.
    }

    @AfterAll
    static void cleanupStorage() throws Exception {
        if (Files.exists(STORAGE_ROOT)) {
            try (var paths = Files.walk(STORAGE_ROOT)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) { }
                });
            }
        }
    }

    @Test
    void uploadsPreviewsReplacesAndDeletesDraftQuestionImages() throws Exception {
        long adminId = user();
        questionId = question();
        String token = login();
        String originalHash = jdbc.queryForObject("SELECT nei_rong_ha_xi FROM ti_mu WHERE id=?", String.class, questionId);

        HttpResponse<String> stemUpload = multipart("POST", "/api/v1/admin/questions/" + questionId + "/attachments?position=QUESTION",
                "file", "stem.png", png(4, 4), token);
        assertThat(stemUpload.statusCode()).isEqualTo(200);
        long stemAttachmentId = ((Number) JsonPath.read(stemUpload.body(), "$.id")).longValue();
        String originalPath = jdbc.queryForObject("SELECT xiang_dui_lu_jing FROM ti_mu_fu_jian WHERE id=?", String.class, stemAttachmentId);
        assertThat(STORAGE_ROOT.resolve(originalPath)).exists();
        assertThat((String) JsonPath.read(stemUpload.body(), "$.objectMarker")).isEqualTo("I001");
        assertThat((String) JsonPath.read(stemUpload.body(), "$.renderStatus")).isEqualTo("AVAILABLE");
        assertThat((String) JsonPath.read(stemUpload.body(), "$.contentUrl"))
                .startsWith("/api/v1/admin/question-attachments/")
                .doesNotContain("E:\\");
        String stemWithMarker = jdbc.queryForObject("SELECT ti_gan FROM ti_mu WHERE id=?", String.class, questionId);
        assertThat(jdbc.queryForObject("SELECT nei_rong_ha_xi FROM ti_mu WHERE id=?", String.class, questionId))
                .isEqualTo(expectedHash(stemWithMarker));
        assertThat(jdbc.queryForObject("SELECT nei_rong_ha_xi FROM ti_mu WHERE id=?", String.class, questionId))
                .isNotEqualTo(originalHash);

        HttpResponse<String> analysisUpload = multipart("POST", "/api/v1/admin/questions/" + questionId + "/attachments?position=STANDARD_ANALYSIS",
                "file", "analysis.jpg", jpeg(), token);
        assertThat(analysisUpload.statusCode()).isEqualTo(200);
        long analysisAttachmentId = ((Number) JsonPath.read(analysisUpload.body(), "$.id")).longValue();
        assertThat((String) JsonPath.read(analysisUpload.body(), "$.objectMarker")).isEqualTo("I002");
        assertThat(jdbc.queryForObject("SELECT nei_rong_ha_xi FROM ti_mu WHERE id=?", String.class, questionId))
                .isEqualTo(expectedHash(stemWithMarker));

        HttpResponse<String> detail = get("/api/v1/admin/questions/" + questionId, token);
        assertThat(detail.statusCode()).isEqualTo(200);
        assertThat(detail.body()).contains("〔图片对象 I001〕", "〔图片对象 I002〕");
        assertThat(detail.body()).doesNotContain("xiang_dui_lu_jing", STORAGE_ROOT.toString());

        HttpResponse<byte[]> content = getBytes("/api/v1/admin/question-attachments/" + stemAttachmentId + "/content", token);
        assertThat(content.statusCode()).isEqualTo(200);
        assertThat(content.body()).containsExactly(png(4, 4));

        HttpResponse<String> replacement = multipart("PUT", "/api/v1/admin/questions/" + questionId + "/attachments/" + stemAttachmentId,
                "file", "replacement.png", png(6, 6), token);
        assertThat(replacement.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(replacement.body(), "$.objectMarker")).isEqualTo("I001");
        assertThat(getBytes("/api/v1/admin/question-attachments/" + stemAttachmentId + "/content", token).body())
                .containsExactly(png(6, 6));
        String replacementPath = jdbc.queryForObject("SELECT xiang_dui_lu_jing FROM ti_mu_fu_jian WHERE id=?", String.class, stemAttachmentId);
        assertThat(STORAGE_ROOT.resolve(replacementPath)).exists();
        assertThat(STORAGE_ROOT.resolve(originalPath)).doesNotExist();
        assertThat(jdbc.queryForObject("SELECT nei_rong_ha_xi FROM ti_mu WHERE id=?", String.class, questionId))
                .isEqualTo(expectedHash(stemWithMarker));

        HttpResponse<Void> deleted = delete("/api/v1/admin/questions/" + questionId + "/attachments/" + analysisAttachmentId, token);
        assertThat(deleted.statusCode()).isEqualTo(204);
        assertThat(getBytes("/api/v1/admin/question-attachments/" + analysisAttachmentId + "/content", token).statusCode()).isEqualTo(404);
        assertThat(get("/api/v1/admin/questions/" + questionId, token).body()).doesNotContain("〔图片对象 I002〕");
        assertThat(jdbc.queryForObject("SELECT nei_rong_ha_xi FROM ti_mu WHERE id=?", String.class, questionId))
                .isEqualTo(expectedHash(stemWithMarker));
        HttpResponse<Void> deletedStem = delete("/api/v1/admin/questions/" + questionId + "/attachments/" + stemAttachmentId, token);
        assertThat(deletedStem.statusCode()).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT nei_rong_ha_xi FROM ti_mu WHERE id=?", String.class, questionId))
                .isEqualTo(expectedHash("原始题干"));
        assertThat(getBytes("/api/v1/admin/question-attachments/" + stemAttachmentId + "/content", token).statusCode()).isEqualTo(404);
        assertThat(STORAGE_ROOT.resolve(replacementPath)).doesNotExist();
        assertThat(adminId).isPositive();
    }

    @Test
    void keepsStandardAnalysisAttachmentReferenceStableAcrossDraftUpdates() throws Exception {
        long adminId = user();
        long stableQuestionId = question();
        String token = login();
        long originalAnalysisId = jdbc.queryForObject("SELECT id FROM ti_mu_jie_xi WHERE ti_mu_id=? AND jie_xi_lei_xing='STANDARD'", Long.class, stableQuestionId);

        byte[] originalImage = png(4, 4);
        HttpResponse<String> upload = multipart("POST", "/api/v1/admin/questions/" + stableQuestionId + "/attachments?position=STANDARD_ANALYSIS",
                "file", "analysis.png", originalImage, token);
        assertThat(upload.statusCode()).isEqualTo(200);
        long attachmentId = ((Number) JsonPath.read(upload.body(), "$.id")).longValue();
        assertThat(jdbc.queryForObject("SELECT ti_mu_jie_xi_id FROM ti_mu_fu_jian WHERE id=?", Long.class, attachmentId))
                .isEqualTo(originalAnalysisId);
        String originalPath = jdbc.queryForObject("SELECT xiang_dui_lu_jing FROM ti_mu_fu_jian WHERE id=?", String.class, attachmentId);
        assertThat(STORAGE_ROOT.resolve(originalPath)).exists();

        String analysisWithMarker = jdbc.queryForObject("SELECT jie_xi_nei_rong FROM ti_mu_jie_xi WHERE id=?", String.class, originalAnalysisId);
        questionAdminService.update(stableQuestionId, updateRequest("第一次更新题干", analysisWithMarker + "\n第一次更新解析"));
        assertThat(jdbc.queryForObject("SELECT id FROM ti_mu_jie_xi WHERE ti_mu_id=? AND jie_xi_lei_xing='STANDARD'", Long.class, stableQuestionId))
                .isEqualTo(originalAnalysisId);
        assertThat(jdbc.queryForObject("SELECT ti_mu_jie_xi_id FROM ti_mu_fu_jian WHERE id=?", Long.class, attachmentId))
                .isEqualTo(originalAnalysisId);
        assertThat(getBytes("/api/v1/admin/question-attachments/" + attachmentId + "/content", token).body()).containsExactly(originalImage);

        byte[] replacementImage = png(6, 6);
        HttpResponse<String> replacement = multipart("PUT", "/api/v1/admin/questions/" + stableQuestionId + "/attachments/" + attachmentId,
                "file", "replacement.png", replacementImage, token);
        assertThat(replacement.statusCode()).isEqualTo(200);
        assertThat(getBytes("/api/v1/admin/question-attachments/" + attachmentId + "/content", token).body()).containsExactly(replacementImage);
        String replacementPath = jdbc.queryForObject("SELECT xiang_dui_lu_jing FROM ti_mu_fu_jian WHERE id=?", String.class, attachmentId);
        assertThat(STORAGE_ROOT.resolve(originalPath)).doesNotExist();

        String changedAnalysis = jdbc.queryForObject("SELECT jie_xi_nei_rong FROM ti_mu_jie_xi WHERE id=?", String.class, originalAnalysisId);
        questionAdminService.update(stableQuestionId, updateRequest("第二次更新题干", changedAnalysis + "\n第二次更新解析"));
        assertThat(jdbc.queryForObject("SELECT id FROM ti_mu_jie_xi WHERE ti_mu_id=? AND jie_xi_lei_xing='STANDARD'", Long.class, stableQuestionId))
                .isEqualTo(originalAnalysisId);

        HttpResponse<Void> deleted = delete("/api/v1/admin/questions/" + stableQuestionId + "/attachments/" + attachmentId, token);
        assertThat(deleted.statusCode()).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT ti_mu_jie_xi_id FROM ti_mu_fu_jian WHERE id=?", Long.class, attachmentId))
                .isEqualTo(originalAnalysisId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu_fu_jian WHERE id=? AND zhuang_tai='DISABLED' AND yi_shan_chu=1", Integer.class, attachmentId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu_jie_xi WHERE id=?", Integer.class, originalAnalysisId)).isEqualTo(1);
        assertThat(getBytes("/api/v1/admin/question-attachments/" + attachmentId + "/content", token).statusCode()).isEqualTo(404);
        assertThat(STORAGE_ROOT.resolve(replacementPath)).doesNotExist();
        assertThat(adminId).isPositive();
    }

    private long user() {
        List<Long> existing = jdbc.query("SELECT id FROM yong_hu WHERE yong_hu_ming=?", (rs, row) -> rs.getLong(1), USERNAME);
        if (!existing.isEmpty()) return existing.getFirst();
        jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao,shi_fou_shou_ci_deng_lu) VALUES (?,?,0)",
                USERNAME, passwordEncoder.encode(PASSWORD));
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO yong_hu_jiao_se(yong_hu_id,jiao_se_id) SELECT ?,id FROM jiao_se WHERE jiao_se_dai_ma='ADMIN'", id);
        return id;
    }

    private long question() {
        jdbc.update("""
                INSERT INTO ti_mu(ke_mu_id,ti_mu_lei_xing,shi_yong_mo_shi,ti_gan,zheng_que_da_an,nan_du,shi_fou_ke_zi_dong_pan_fen,zhuang_tai,nei_rong_ha_xi)
                VALUES (1,'SINGLE_CHOICE','ONLINE_PRACTICE','原始题干','{"schemaVersion":1,"type":"SINGLE_CHOICE","optionLabels":["A"]}',1,1,'DRAFT',?)
                """, UUID.randomUUID().toString().replace("-", ""));
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO ti_mu_xuan_xiang(ti_mu_id,xuan_xiang_biao_shi,xuan_xiang_nei_rong,shi_fou_zheng_que,pai_xu) VALUES (?,?,?,?,?)",
                id, "A", "选项A", true, 1);
        jdbc.update("INSERT INTO ti_mu_xuan_xiang(ti_mu_id,xuan_xiang_biao_shi,xuan_xiang_nei_rong,shi_fou_zheng_que,pai_xu) VALUES (?,?,?,?,?)",
                id, "B", "选项B", false, 2);
        jdbc.update("INSERT INTO ti_mu_jie_xi(ti_mu_id,jie_xi_lei_xing,jie_xi_nei_rong,ban_ben_hao,zhuang_tai) VALUES (?,'STANDARD','原始解析',1,'DRAFT')", id);
        jdbc.update("UPDATE ti_mu SET nei_rong_ha_xi=? WHERE id=?", expectedHash("原始题干", id), id);
        return id;
    }

    private String expectedHash(String stem) {
        return expectedHash(stem, questionId);
    }

    private String expectedHash(String stem, long id) {
        var options = jdbc.query("SELECT xuan_xiang_biao_shi,xuan_xiang_nei_rong FROM ti_mu_xuan_xiang WHERE ti_mu_id=? AND yi_shan_chu=0 ORDER BY pai_xu",
                (rs, row) -> new QuestionContentHashService.OptionContent(rs.getString(1), rs.getString(2)), id);
        return contentHashService.calculate(stem, options);
    }

    private QuestionDtos.Save updateRequest(String stem, String standardAnalysis) {
        Long pointId = jdbc.queryForObject("SELECT id FROM zhi_shi_dian WHERE ke_mu_id=1 AND zhuang_tai='ACTIVE' LIMIT 1", Long.class);
        return new QuestionDtos.Save(1L, "SINGLE_CHOICE", "ONLINE_PRACTICE", stem,
                "{\"schemaVersion\":1,\"type\":\"SINGLE_CHOICE\",\"optionLabels\":[\"A\"]}", 1, "测试", true,
                List.of(new QuestionDtos.Option("A", "选项A", true), new QuestionDtos.Option("B", "选项B", false)), standardAnalysis,
                List.of(pointId), List.of("QUESTION", "ANSWER", "STANDARD_ANALYSIS").stream()
                        .map(part -> new QuestionDtos.Source(part, "TEACHER_CREATED", "附件回归测试", "AUTHORIZED", null, null, null, null, null, "测试授权")).toList());
    }

    private String login() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> challenge = client.send(HttpRequest.newBuilder()
                .uri(URI.create(base() + "/api/v1/auth/captcha-challenge")).GET().build(), HttpResponse.BodyHandlers.ofString());
        String body = "{\"username\":\"%s\",\"password\":\"%s\",\"expectedRole\":\"ADMIN\",\"challengeId\":\"%s\",\"captchaCode\":\"%s\"}"
                .formatted(USERNAME, PASSWORD, JsonPath.read(challenge.body(), "$.challengeId"), JsonPath.read(challenge.body(), "$.testCode"));
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create(base() + "/api/v1/auth/login")).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return JsonPath.read(response.body(), "$.accessToken");
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        return HttpClient.newHttpClient().send(request("GET", path, token).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<byte[]> getBytes(String path, String token) throws Exception {
        return HttpClient.newHttpClient().send(request("GET", path, token).build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private HttpResponse<Void> delete(String path, String token) throws Exception {
        return HttpClient.newHttpClient().send(request("DELETE", path, token).build(), HttpResponse.BodyHandlers.discarding());
    }

    private HttpRequest.Builder request(String method, String path, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(base() + path)).method(method, HttpRequest.BodyPublishers.noBody());
        return builder.header("Authorization", "Bearer " + token);
    }

    private HttpResponse<String> multipart(String method, String path, String field, String fileName, byte[] bytes, String token) throws Exception {
        String boundary = "----Codex" + UUID.randomUUID();
        byte[] prefix = ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + field + "\"; filename=\"" + fileName
                + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[prefix.length + bytes.length + suffix.length];
        System.arraycopy(prefix, 0, body, 0, prefix.length);
        System.arraycopy(bytes, 0, body, prefix.length, bytes.length);
        System.arraycopy(suffix, 0, body, prefix.length + bytes.length, suffix.length);
        HttpRequest.Builder request = HttpRequest.newBuilder().uri(URI.create(base() + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .method(method, HttpRequest.BodyPublishers.ofByteArray(body));
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String base() { return "http://localhost:" + port; }

    private byte[] png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }

    private byte[] jpeg() throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpeg", output);
            return output.toByteArray();
        }
    }
}
