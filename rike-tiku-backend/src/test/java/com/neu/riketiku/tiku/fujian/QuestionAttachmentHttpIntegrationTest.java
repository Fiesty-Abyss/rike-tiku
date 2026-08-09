package com.neu.riketiku.tiku.fujian;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import com.neu.riketiku.xueshenglianxi.StudentPracticeDtos;
import com.neu.riketiku.xueshenglianxi.StudentPracticeService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.node.JsonNodeFactory;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuestionAttachmentHttpIntegrationTest extends AdminQuestionIntegrationTestSupport {
    private static final String PASSWORD = "a1234567";
    private static final Path STORAGE_ROOT = Path.of(System.getProperty("java.io.tmpdir"),
            "rike-tiku-attachment-http-" + UUID.randomUUID());

    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private QuestionAttachmentStorage storage;
    @Autowired
    private StudentPracticeService practice;

    @DynamicPropertySource
    static void attachmentProperties(DynamicPropertyRegistry registry) {
        registry.add("rike.tiku.attachment.storage-root", () -> STORAGE_ROOT.toString());
    }

    @AfterEach
    void cleanTestData() {
        List<Long> studentIds = jdbc.queryForList("SELECT id FROM xue_sheng_dang_an WHERE yong_hu_id IN (SELECT id FROM yong_hu WHERE yong_hu_ming IN ('attachment_owner','attachment_other'))", Long.class);
        List<Long> sessionIds = studentIds.isEmpty() ? List.of() : jdbc.queryForList("SELECT id FROM lian_xi_hui_hua WHERE xue_sheng_id IN (" + marks(studentIds.size()) + ")", Long.class, studentIds.toArray());
        List<Long> questionIds = jdbc.queryForList("SELECT id FROM ti_mu WHERE ti_gan IN ('题干〔图片对象 I001〕','其他题干〔图片对象 I001〕')", Long.class);
        if (!studentIds.isEmpty()) {
            String studentMarks = marks(studentIds.size());
            jdbc.update("DELETE FROM cuo_ti_ji_lu WHERE xue_sheng_id IN (" + studentMarks + ")", studentIds.toArray());
            jdbc.update("DELETE FROM xue_sheng_da_ti WHERE xue_sheng_id IN (" + studentMarks + ")", studentIds.toArray());
        }
        if (!sessionIds.isEmpty()) {
            String sessionMarks = marks(sessionIds.size());
            jdbc.update("DELETE FROM xue_xi_jie_guo WHERE lian_xi_hui_hua_id IN (" + sessionMarks + ")", sessionIds.toArray());
            jdbc.update("DELETE FROM lian_xi_ti_mu WHERE lian_xi_hui_hua_id IN (" + sessionMarks + ")", sessionIds.toArray());
            jdbc.update("DELETE FROM lian_xi_hui_hua WHERE id IN (" + sessionMarks + ")", sessionIds.toArray());
        }
        if (!questionIds.isEmpty()) {
            String questionMarks = marks(questionIds.size());
            jdbc.update("DELETE FROM ti_mu_fu_jian WHERE ti_mu_id IN (" + questionMarks + ")", questionIds.toArray());
            jdbc.update("DELETE FROM ti_mu_zhi_shi_dian WHERE ti_mu_id IN (" + questionMarks + ")", questionIds.toArray());
            jdbc.update("DELETE FROM ti_mu_jie_xi WHERE ti_mu_id IN (" + questionMarks + ")", questionIds.toArray());
            jdbc.update("DELETE FROM ti_mu_xuan_xiang WHERE ti_mu_id IN (" + questionMarks + ")", questionIds.toArray());
            jdbc.update("DELETE FROM ti_mu WHERE id IN (" + questionMarks + ")", questionIds.toArray());
        }
        jdbc.update("DELETE FROM xue_sheng_dang_an WHERE yong_hu_id IN (SELECT id FROM yong_hu WHERE yong_hu_ming IN ('attachment_owner','attachment_other'))");
        jdbc.update("DELETE FROM yong_hu_jiao_se WHERE yong_hu_id IN (SELECT id FROM yong_hu WHERE yong_hu_ming IN ('attachment_admin','attachment_owner','attachment_other'))");
        jdbc.update("DELETE FROM yong_hu WHERE yong_hu_ming IN ('attachment_admin','attachment_owner','attachment_other')");
    }

    @Test
    void enforcesRoleAndQuestionContextOverRealHttp() throws Exception {
        long adminId = user("attachment_admin", "ADMIN");
        long ownerId = student("attachment_owner");
        long otherId = student("attachment_other");
        long questionId = question("题干〔图片对象 I001〕", "解析〔图片对象 I002〕");
        long analysisId = jdbc.queryForObject("SELECT id FROM ti_mu_jie_xi WHERE ti_mu_id=? AND jie_xi_lei_xing='STANDARD'", Long.class, questionId);
        var image = storage.store("force.png", png());
        long questionAttachmentId = attachment(questionId, null, "QUESTION", image, "I001");
        long analysisAttachmentId = attachment(questionId, analysisId, "STANDARD_ANALYSIS", image, "I002");

        var session = practice.create(ownerId, new StudentPracticeDtos.CreateRequest(1L, null, List.of("SINGLE_CHOICE"), null, 1));
        long otherQuestionId = question("其他题干〔图片对象 I001〕", "其他解析〔图片对象 I002〕");
        long otherAnalysisId = jdbc.queryForObject("SELECT id FROM ti_mu_jie_xi WHERE ti_mu_id=? AND jie_xi_lei_xing='STANDARD'", Long.class, otherQuestionId);
        long otherAttachmentId = attachment(otherQuestionId, null, "QUESTION", image, "I001");
        attachment(otherQuestionId, otherAnalysisId, "STANDARD_ANALYSIS", image, "I002");

        String adminToken = login("attachment_admin", "ADMIN");
        String ownerToken = login("attachment_owner", "STUDENT");
        String otherToken = login("attachment_other", "STUDENT");
        String adminPath = "/api/v1/admin/question-attachments/" + questionAttachmentId + "/content";
        String practicePath = "/api/v1/student/practice-sessions/" + session.id() + "/attachments/" + questionAttachmentId + "/content";
        String analysisPracticePath = "/api/v1/student/practice-sessions/" + session.id() + "/attachments/" + analysisAttachmentId + "/content";

        assertThat(get(adminPath, null).statusCode()).isEqualTo(401);
        assertThat(get(adminPath, ownerToken).statusCode()).isEqualTo(403);
        assertThat(get(adminPath, adminToken).statusCode()).isEqualTo(200);
        assertThat(get(practicePath, ownerToken).statusCode()).isEqualTo(200);
        assertThat(get(practicePath, otherToken).statusCode()).isEqualTo(404);
        assertThat(get(analysisPracticePath, ownerToken).statusCode()).isEqualTo(404);

        practice.submit(ownerId, session.id(), new StudentPracticeDtos.SubmitRequest(List.of(
                new StudentPracticeDtos.Answer(session.questions().getFirst().practiceQuestionId(), JsonNodeFactory.instance.textNode("B"), 2))));
        assertThat(get(analysisPracticePath, ownerToken).statusCode()).isEqualTo(200);
        assertThat(get("/api/v1/student/wrong-questions/" + questionId + "/attachments/" + questionAttachmentId + "/content", ownerToken).statusCode()).isEqualTo(200);
        assertThat(get("/api/v1/student/wrong-questions/" + questionId + "/attachments/" + otherAttachmentId + "/content", ownerToken).statusCode()).isEqualTo(404);
        assertThat(get("/api/v1/student/wrong-questions/" + questionId + "/attachments/" + questionAttachmentId + "/content", otherToken).statusCode()).isEqualTo(404);
        assertThat(get("/api/v1/admin/question-attachments/" + analysisAttachmentId + "/content", adminToken).statusCode()).isEqualTo(200);
        assertThat(adminId).isPositive();
    }

    private HttpResponse<byte[]> get(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private String login(String username, String role) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> challenge = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/auth/captcha-challenge"))
                .GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = "{\"username\":\"%s\",\"password\":\"%s\",\"expectedRole\":\"%s\",\"challengeId\":\"%s\",\"captchaCode\":\"%s\"}"
                .formatted(username, PASSWORD, role, JsonPath.read(challenge.body(), "$.challengeId"), JsonPath.read(challenge.body(), "$.testCode"));
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).isEqualTo(200);
        return JsonPath.read(response.body(), "$.accessToken");
    }

    private long user(String prefix, String role) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao,shi_fou_shou_ci_deng_lu) VALUES (?,?,0)",
                prefix + "_" + suffix, passwordEncoder.encode(PASSWORD));
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        long roleId = jdbc.queryForObject("SELECT id FROM jiao_se WHERE jiao_se_dai_ma=?", Long.class, role);
        jdbc.update("INSERT INTO yong_hu_jiao_se(yong_hu_id,jiao_se_id) VALUES (?,?)", id, roleId);
        jdbc.update("UPDATE yong_hu SET yong_hu_ming=? WHERE id=?", prefix, id);
        return id;
    }

    private long student(String username) {
        long id = user(username, "STUDENT");
        jdbc.update("INSERT INTO xue_sheng_dang_an(yong_hu_id,xue_hao,xing_ming,nian_ji) VALUES (?,?,?,?)",
                id, "ATTACH_" + username, "附件学生", "高一");
        return id;
    }

    private long question(String stem, String analysis) {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        jdbc.update("""
                INSERT INTO ti_mu(ke_mu_id,ti_mu_lei_xing,shi_yong_mo_shi,ti_gan,zheng_que_da_an,nan_du,shi_fou_ke_zi_dong_pan_fen,zhuang_tai,nei_rong_ha_xi)
                VALUES (1,'SINGLE_CHOICE','ONLINE_PRACTICE',?,?,1,1,'PUBLISHED',?)
                """, stem, "{\"schemaVersion\":1,\"type\":\"SINGLE_CHOICE\",\"optionLabels\":[\"A\"]}", suffix);
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO ti_mu_xuan_xiang(ti_mu_id,xuan_xiang_biao_shi,xuan_xiang_nei_rong,shi_fou_zheng_que,pai_xu) VALUES (?, 'A','正确选项',1,1),(?, 'B','错误选项',0,2)", id, id);
        jdbc.update("INSERT INTO ti_mu_jie_xi(ti_mu_id,jie_xi_lei_xing,jie_xi_nei_rong,ban_ben_hao,zhuang_tai) VALUES (?,'STANDARD',?,1,'PUBLISHED')", id, analysis);
        long pointId = jdbc.queryForObject("SELECT id FROM zhi_shi_dian WHERE ke_mu_id=1 AND zhuang_tai='ACTIVE' LIMIT 1", Long.class);
        jdbc.update("INSERT INTO ti_mu_zhi_shi_dian(ti_mu_id,zhi_shi_dian_id,shi_fou_zhu_yao,pai_xu) VALUES (?,?,1,1)", id, pointId);
        return id;
    }

    private long attachment(long questionId, Long analysisId, String position, QuestionAttachmentStorage.StoredImage image, String marker) {
        jdbc.update("""
                INSERT INTO ti_mu_fu_jian(ti_mu_id,ti_mu_jie_xi_id,guan_lian_wei_zhi,fu_jian_lei_xing,yuan_shi_wen_jian_ming,xiang_dui_lu_jing,nei_rong_ha_xi,dui_xiang_biao_shi,zheng_wen_zi_fu_wei_zhi,pai_xu,zhuang_tai)
                VALUES (?,?,?,'IMAGE','force.png',?,?,?,?,1,'ACTIVE')
                """, questionId, analysisId, position, image.relativePath(), image.hash(), marker, 1);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }

    private String marks(int count) {
        return String.join(",", java.util.Collections.nCopies(count, "?"));
    }
}
