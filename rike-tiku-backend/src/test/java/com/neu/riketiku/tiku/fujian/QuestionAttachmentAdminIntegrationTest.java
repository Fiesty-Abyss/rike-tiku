package com.neu.riketiku.tiku.fujian;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

        HttpResponse<String> stemUpload = multipart("POST", "/api/v1/admin/questions/" + questionId + "/attachments?position=QUESTION",
                "file", "stem.png", png(4, 4), token);
        assertThat(stemUpload.statusCode()).isEqualTo(200);
        long stemAttachmentId = ((Number) JsonPath.read(stemUpload.body(), "$.id")).longValue();
        assertThat((String) JsonPath.read(stemUpload.body(), "$.objectMarker")).isEqualTo("I001");
        assertThat((String) JsonPath.read(stemUpload.body(), "$.renderStatus")).isEqualTo("AVAILABLE");
        assertThat((String) JsonPath.read(stemUpload.body(), "$.contentUrl"))
                .startsWith("/api/v1/admin/question-attachments/")
                .doesNotContain("E:\\");

        HttpResponse<String> analysisUpload = multipart("POST", "/api/v1/admin/questions/" + questionId + "/attachments?position=STANDARD_ANALYSIS",
                "file", "analysis.jpg", jpeg(), token);
        assertThat(analysisUpload.statusCode()).isEqualTo(200);
        long analysisAttachmentId = ((Number) JsonPath.read(analysisUpload.body(), "$.id")).longValue();
        assertThat((String) JsonPath.read(analysisUpload.body(), "$.objectMarker")).isEqualTo("I002");

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

        HttpResponse<Void> deleted = delete("/api/v1/admin/questions/" + questionId + "/attachments/" + analysisAttachmentId, token);
        assertThat(deleted.statusCode()).isEqualTo(204);
        assertThat(getBytes("/api/v1/admin/question-attachments/" + analysisAttachmentId + "/content", token).statusCode()).isEqualTo(404);
        assertThat(get("/api/v1/admin/questions/" + questionId, token).body()).doesNotContain("〔图片对象 I002〕");
        assertThat(adminId).isPositive();
    }

    private long user() {
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
        jdbc.update("INSERT INTO ti_mu_jie_xi(ti_mu_id,jie_xi_lei_xing,jie_xi_nei_rong,ban_ben_hao,zhuang_tai) VALUES (?,'STANDARD','原始解析',1,'DRAFT')", id);
        return id;
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
