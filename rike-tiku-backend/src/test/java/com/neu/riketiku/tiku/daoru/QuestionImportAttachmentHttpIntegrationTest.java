package com.neu.riketiku.tiku.daoru;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import com.neu.riketiku.tiku.admin.QuestionAdminService;
import com.neu.riketiku.tiku.admin.QuestionDtos;
import com.neu.riketiku.xueshenglianxi.StudentPracticeDtos;
import com.neu.riketiku.xueshenglianxi.StudentPracticeService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuestionImportAttachmentHttpIntegrationTest extends AdminQuestionIntegrationTestSupport {
    private static final String PASSWORD = "a1234567";
    private static final Path SOURCE_ROOT = Path.of(System.getProperty("java.io.tmpdir"),
            "rike-tiku-import-attachment-source-" + UUID.randomUUID());
    private static final Path STORAGE_ROOT = Path.of(System.getProperty("java.io.tmpdir"),
            "rike-tiku-import-attachment-storage-" + UUID.randomUUID());

    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private QuestionImportService importer;
    @Autowired
    private QuestionAdminService adminQuestions;
    @Autowired
    private StudentPracticeService practice;

    @DynamicPropertySource
    static void importProperties(DynamicPropertyRegistry registry) {
        registry.add("rike.tiku.question-import.source-root", () -> SOURCE_ROOT.toString());
        registry.add("rike.tiku.attachment.storage-root", () -> STORAGE_ROOT.toString());
    }

    @AfterEach
    void cleanImportedChain() {
        List<Long> studentIds = jdbc.queryForList("SELECT id FROM xue_sheng_dang_an WHERE yong_hu_id IN (SELECT id FROM yong_hu WHERE yong_hu_ming IN ('import_chain_student'))", Long.class);
        List<Long> sessionIds = studentIds.isEmpty() ? List.of() : jdbc.queryForList("SELECT id FROM lian_xi_hui_hua WHERE xue_sheng_id IN (" + marks(studentIds.size()) + ")", Long.class, studentIds.toArray());
        List<Long> questionIds = jdbc.queryForList("SELECT id FROM ti_mu WHERE ti_gan LIKE '导入题干%'", Long.class);
        List<Long> batchIds = questionIds.isEmpty() ? List.of() : jdbc.queryForList("SELECT DISTINCT dao_ru_pi_ci_id FROM ti_mu WHERE id IN (" + marks(questionIds.size()) + ") AND dao_ru_pi_ci_id IS NOT NULL", Long.class, questionIds.toArray());
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
            jdbc.update("DELETE FROM ti_mu_shen_he_ji_lu WHERE ti_mu_id IN (" + questionMarks + ")", questionIds.toArray());
            jdbc.update("DELETE FROM ti_mu_lai_yuan WHERE ti_mu_id IN (" + questionMarks + ")", questionIds.toArray());
            jdbc.update("DELETE FROM ti_mu_zhi_shi_dian WHERE ti_mu_id IN (" + questionMarks + ")", questionIds.toArray());
            jdbc.update("DELETE FROM ti_mu_jie_xi WHERE ti_mu_id IN (" + questionMarks + ")", questionIds.toArray());
            jdbc.update("DELETE FROM ti_mu_xuan_xiang WHERE ti_mu_id IN (" + questionMarks + ")", questionIds.toArray());
            jdbc.update("DELETE FROM ti_mu WHERE id IN (" + questionMarks + ")", questionIds.toArray());
        }
        if (!batchIds.isEmpty()) {
            jdbc.update("DELETE FROM dao_ru_pi_ci WHERE id IN (" + marks(batchIds.size()) + ")", batchIds.toArray());
        }
        jdbc.update("DELETE FROM xue_sheng_dang_an WHERE yong_hu_id IN (SELECT id FROM yong_hu WHERE yong_hu_ming='import_chain_student')");
        jdbc.update("DELETE FROM yong_hu_jiao_se WHERE yong_hu_id IN (SELECT id FROM yong_hu WHERE yong_hu_ming IN ('import_chain_admin','import_chain_student'))");
        jdbc.update("DELETE FROM yong_hu WHERE yong_hu_ming IN ('import_chain_admin','import_chain_student')");
    }

    @Test
    void importsShortObjectMarkersThroughAdminDetailContentAndStudentPractice() throws Exception {
        prepareSourceFiles();
        MockMultipartFile upload = file("import-attachment.xlsx", workbook(new String[]{
                "物理", "2023", "全国", "真实导入附件测试卷", "31", "单选题",
                "导入题干〔图片对象 I001〕", "A. 正确\nB. 错误", "A", "导入解析〔图片对象 I002〕",
                "1", "1", "力学", "力学>机械振动与机械波>波速、波长与频率", "easy", "测试难度",
                "待审核", "待审核", "试题文件：sources/test-paper.doc\n答案解析文件：sources/test-answer.doc"
        }));

        QuestionImportDtos.Preview preview = importer.preview(upload);
        assertThat(preview.validCount()).isOne();
        long adminId = adminUser();
        QuestionImportDtos.Confirm confirmation = importer.confirm(upload, preview.fileHash(), adminId);
        long questionId = jdbc.queryForObject("SELECT q.id FROM ti_mu q JOIN dao_ru_pi_ci b ON b.id=q.dao_ru_pi_ci_id WHERE b.pi_ci_bian_hao=?", Long.class, confirmation.batchCode());

        assertThat(jdbc.queryForList("SELECT dui_xiang_biao_shi FROM ti_mu_fu_jian WHERE ti_mu_id=? ORDER BY id", String.class, questionId))
                .containsExactly("I001", "I002");
        QuestionDtos.Detail detail = adminQuestions.detail(questionId);
        assertThat(detail.stem()).contains("〔图片对象 I001〕");
        assertThat(detail.attachments()).extracting(QuestionDtos.Attachment::objectMarker).containsExactly("I001", "I002");
        assertThat(detail.attachments()).allSatisfy(attachment -> {
            assertThat(attachment.renderStatus()).isEqualTo("AVAILABLE");
            assertThat(attachment.contentUrl()).isNotBlank();
        });

        String adminToken = login("import_chain_admin", "ADMIN");
        HttpResponse<byte[]> detailResponse = get("/api/v1/admin/questions/" + questionId, adminToken);
        String detailBody = new String(detailResponse.body(), StandardCharsets.UTF_8);
        assertThat(detailResponse.statusCode()).isEqualTo(200);
        assertThat(detailBody).contains("I001", "I002").doesNotContain("xiangDuiLuJing", "relativePath");
        QuestionDtos.Attachment questionAttachment = detail.attachments().stream()
                .filter(attachment -> "QUESTION".equals(attachment.position())).findFirst().orElseThrow();
        HttpResponse<byte[]> adminContent = get(questionAttachment.contentUrl(), adminToken);
        assertThat(adminContent.statusCode()).isEqualTo(200);
        assertThat(adminContent.headers().firstValue("content-type").orElse("")).startsWith("image/png");
        assertThat(ImageIO.read(new ByteArrayInputStream(adminContent.body()))).isNotNull();

        adminQuestions.updateSourceRights(questionId,
                new QuestionDtos.SourceRightsUpdate("USER_PROVIDED", "测试附件由专项提供，仅用于隔离测试"), adminId);
        adminQuestions.transition(questionId, "APPROVED", "PENDING", "PUBLISHED", null, adminId);

        long studentId = studentUser();
        long importedKnowledgePointId = jdbc.queryForObject("SELECT zhi_shi_dian_id FROM ti_mu_zhi_shi_dian WHERE ti_mu_id=? ORDER BY pai_xu,id LIMIT 1", Long.class, questionId);
        StudentPracticeDtos.Session session = practice.create(studentId,
                new StudentPracticeDtos.CreateRequest(1L, List.of(importedKnowledgePointId), List.of("SINGLE_CHOICE"), 1, 1));
        StudentPracticeDtos.SessionQuestion sessionQuestion = session.questions().getFirst();
        assertThat(sessionQuestion.stem()).contains("〔图片对象 I001〕");
        assertThat(sessionQuestion.attachments()).extracting(StudentPracticeDtos.Attachment::objectMarker).containsExactly("I001");
        StudentPracticeDtos.Attachment studentStem = sessionQuestion.attachments().stream()
                .filter(attachment -> "QUESTION".equals(attachment.position())).findFirst().orElseThrow();
        long analysisAttachmentId = jdbc.queryForObject("SELECT id FROM ti_mu_fu_jian WHERE ti_mu_id=? AND guan_lian_wei_zhi='STANDARD_ANALYSIS'", Long.class, questionId);
        String analysisUrl = "/api/v1/student/practice-sessions/" + session.id() + "/attachments/" + analysisAttachmentId + "/content";

        String studentToken = login("import_chain_student", "STUDENT");
        assertThat(get(studentStem.contentUrl(), studentToken).statusCode()).isEqualTo(200);
        assertThat(get(analysisUrl, studentToken).statusCode()).isEqualTo(404);
        practice.submit(studentId, session.id(), new StudentPracticeDtos.SubmitRequest(List.of(
                new StudentPracticeDtos.Answer(sessionQuestion.practiceQuestionId(), text("B"), 1))));
        StudentPracticeDtos.Result result = practice.result(studentId, session.id());
        assertThat(result.questions().getFirst().question().attachments())
                .extracting(StudentPracticeDtos.Attachment::objectMarker).containsExactly("I001", "I002");
        assertThat(get(analysisUrl, studentToken).statusCode()).isEqualTo(200);
    }

    private void prepareSourceFiles() throws Exception {
        Files.createDirectories(SOURCE_ROOT.resolve("物理/母题库/images"));
        Files.createDirectories(SOURCE_ROOT.resolve("sources"));
        Files.write(SOURCE_ROOT.resolve("物理/母题库/images/q31_stem_image_001.png"), png());
        Files.write(SOURCE_ROOT.resolve("物理/母题库/images/q31_analysis_image_002.png"), png());
        Files.writeString(SOURCE_ROOT.resolve("sources/test-paper.doc"), "fixture-paper");
        Files.writeString(SOURCE_ROOT.resolve("sources/test-answer.doc"), "fixture-answer");
    }

    private long adminUser() {
        jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao,shi_fou_shou_ci_deng_lu) VALUES (?,?,0)",
                "import_chain_admin", passwordEncoder.encode(PASSWORD));
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        long roleId = jdbc.queryForObject("SELECT id FROM jiao_se WHERE jiao_se_dai_ma='ADMIN'", Long.class);
        jdbc.update("INSERT INTO yong_hu_jiao_se(yong_hu_id,jiao_se_id) VALUES (?,?)", id, roleId);
        return id;
    }

    private long studentUser() {
        jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao,shi_fou_shou_ci_deng_lu) VALUES (?,?,0)",
                "import_chain_student", passwordEncoder.encode(PASSWORD));
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        long roleId = jdbc.queryForObject("SELECT id FROM jiao_se WHERE jiao_se_dai_ma='STUDENT'", Long.class);
        jdbc.update("INSERT INTO yong_hu_jiao_se(yong_hu_id,jiao_se_id) VALUES (?,?)", id, roleId);
        jdbc.update("INSERT INTO xue_sheng_dang_an(yong_hu_id,xue_hao,xing_ming,nian_ji) VALUES (?,?,?,?)",
                id, "IMPORT_CHAIN_STUDENT", "导入链学生", "高一");
        return id;
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

    private HttpResponse<byte[]> get(String path, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private MockMultipartFile file(String name, byte[] body) {
        return new MockMultipartFile("file", name, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", body);
    }

    private byte[] workbook(String[] row) throws Exception {
        List<String> headers = List.of("学科", "年份", "区域", "试卷来源", "题号", "题型", "题干", "选项", "答案", "标准解析", "题干图片数", "解析图片数", "一级知识点", "知识点", "难度", "难度说明", "答案解析审核状态", "审核状态", "来源文件");
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("题目检查");
            sheet.createRow(0).createCell(0).setCellValue("真实附件导入专项");
            var header = sheet.createRow(1);
            for (int index = 0; index < headers.size(); index++) header.createCell(index).setCellValue(headers.get(index));
            var values = sheet.createRow(2);
            for (int index = 0; index < row.length; index++) values.createCell(index).setCellValue(row[index]);
            workbook.createSheet("质量统计").createRow(0).createCell(0).setCellFormula("1+1");
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private byte[] png() throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", output);
            return output.toByteArray();
        }
    }

    private String marks(int count) {
        return String.join(",", java.util.Collections.nCopies(count, "?"));
    }

    private tools.jackson.databind.JsonNode text(String value) {
        return new tools.jackson.databind.ObjectMapper().getNodeFactory().textNode(value);
    }
}
