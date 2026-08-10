package com.neu.riketiku.tiku.daoru;

import static org.assertj.core.api.Assertions.assertThat;

import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import com.neu.riketiku.tiku.admin.QuestionAdminService;
import com.neu.riketiku.tiku.admin.QuestionDtos;
import com.neu.riketiku.xueshenglianxi.StudentPracticeDtos;
import com.neu.riketiku.xueshenglianxi.StudentPracticeService;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class Golden30ImportIntegrationTest extends AdminQuestionIntegrationTestSupport {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize().getParent();
    private static final Path SOURCE_ROOT = PROJECT_ROOT.resolve("题库");
    private static final Path STORAGE_ROOT = Path.of(System.getProperty("java.io.tmpdir"), "rike-tiku-golden30-storage-" + UUID.randomUUID());
    private static final Map<String, Path> CANDIDATES = Map.of(
            "物理", PROJECT_ROOT.resolve("题库/理综/测试结果/物理/待审核_清洗版.xlsx"),
            "化学", PROJECT_ROOT.resolve("题库/理综/测试结果/化学/待审核_清洗版.xlsx"),
            "生物", PROJECT_ROOT.resolve("题库/理综/测试结果/生物/待审核_清洗版.xlsx"));
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private QuestionImportService importer;
    @Autowired private QuestionAdminService adminQuestions;
    @Autowired private StudentPracticeService practice;

    @DynamicPropertySource
    static void golden30Properties(DynamicPropertyRegistry registry) {
        registry.add("rike.tiku.question-import.source-root", () -> SOURCE_ROOT.toString());
        registry.add("rike.tiku.attachment.storage-root", () -> STORAGE_ROOT.toString());
    }

    @Test
    void importsPublishesAndPracticesTheThirtyCandidateQuestions() throws Exception {
        seedKnowledgePoints();
        long adminId = user("golden30_admin", "ADMIN");
        List<Long> questionIds = new ArrayList<>();
        for (String subject : List.of("物理", "化学", "生物")) {
            MockMultipartFile upload = normalizedWorkbook(subject);
            var preview = importer.preview(upload);
            assertThat(preview.totalCount()).as(subject).isEqualTo(10);
            assertThat(preview.validCount()).as(() -> subject + ":" + preview.rows()).isEqualTo(10);
            var confirmation = importer.confirm(upload, preview.fileHash(), adminId);
            questionIds.addAll(jdbc.queryForList("SELECT q.id FROM ti_mu q JOIN dao_ru_pi_ci b ON b.id=q.dao_ru_pi_ci_id WHERE b.pi_ci_bian_hao=? ORDER BY q.id", Long.class, confirmation.batchCode()));
        }
        assertThat(questionIds).hasSize(30);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu_fu_jian WHERE ti_mu_id IN (" + marks(questionIds.size()) + ") AND fu_jian_lei_xing='IMAGE' AND zhuang_tai='ACTIVE'", Integer.class, questionIds.toArray()))
                .isGreaterThan(0);

        for (long questionId : questionIds) {
            adminQuestions.updateSourceRights(questionId, new QuestionDtos.SourceRightsUpdate("USER_PROVIDED", "本地演示题库：用户提供，仅用于独立验收数据库"), adminId);
            adminQuestions.transition(questionId, "APPROVED", "PENDING", "PUBLISHED", null, adminId);
        }

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu WHERE id IN (" + marks(questionIds.size()) + ") AND zhuang_tai='PUBLISHED'", Integer.class, questionIds.toArray())).isEqualTo(30);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu_jie_xi WHERE ti_mu_id IN (" + marks(questionIds.size()) + ") AND jie_xi_lei_xing='STANDARD' AND zhuang_tai='PUBLISHED' AND yi_shan_chu=0", Integer.class, questionIds.toArray())).isEqualTo(30);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu_zhi_shi_dian WHERE ti_mu_id IN (" + marks(questionIds.size()) + ")", Integer.class, questionIds.toArray())).isGreaterThanOrEqualTo(30);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu_lai_yuan WHERE ti_mu_id IN (" + marks(questionIds.size()) + ") AND quan_li_zhuang_tai='USER_PROVIDED'", Integer.class, questionIds.toArray())).isEqualTo(90);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu WHERE id IN (" + marks(questionIds.size()) + ") AND shi_fou_ke_zi_dong_pan_fen=1", Integer.class, questionIds.toArray())).isEqualTo(29);

        long studentId = user("golden30_student", "STUDENT");
        for (long subjectId : List.of(1L, 2L, 3L)) {
            int practiceCount = subjectId == 1L ? 9 : 10;
            var session = practice.create(studentId, new StudentPracticeDtos.CreateRequest(subjectId, null,
                    List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "FILL_BLANK"), null, practiceCount));
            assertThat(session.questions()).hasSize(practiceCount);
            assertThat(session.questions().stream().flatMap(question -> question.attachments().stream())
                    .noneMatch(attachment -> "STANDARD_ANALYSIS".equals(attachment.position()))).isTrue();
            var answers = session.questions().stream().map(question -> new StudentPracticeDtos.Answer(
                    question.practiceQuestionId(), safeAnswer(question), 0)).toList();
            practice.submit(studentId, session.id(), new StudentPracticeDtos.SubmitRequest(answers));
            var result = practice.result(studentId, session.id());
            assertThat(result.questions()).hasSize(practiceCount);
            assertThat(result.questions()).allSatisfy(item -> {
                assertThat(item.standardAnalysis()).isNotBlank();
                assertThat(item.question().attachments()).allSatisfy(attachment -> {
                    assertThat(attachment.renderStatus()).isEqualTo("AVAILABLE");
                    assertThat(attachment.contentUrl()).isNotBlank();
                });
            });
        }
    }

    private void seedKnowledgePoints() throws Exception {
        for (Path candidate : CANDIDATES.values()) {
            try (var workbook = WorkbookFactory.create(Files.newInputStream(candidate))) {
                var sheet = workbook.getSheet("题目检查");
                var formatter = new DataFormatter();
                for (int row = 2; row <= sheet.getLastRowNum(); row++) {
                    var current = sheet.getRow(row);
                    if (current == null) continue;
                    String subject = formatter.formatCellValue(current.getCell(0));
                    String subjectCode = Map.of("物理", "PHYSICS", "化学", "CHEMISTRY", "生物", "BIOLOGY").get(subject);
                    long subjectId = jdbc.queryForObject("SELECT id FROM ke_mu WHERE ke_mu_dai_ma=?", Long.class, subjectCode);
                    for (String path : formatter.formatCellValue(current.getCell(13)).split("[；;]")) {
                        String normalized = path.trim().replaceAll("\\s*[>＞]\\s*", ">");
                        if (!normalized.isBlank()) jdbc.update("INSERT IGNORE INTO zhi_shi_dian(ke_mu_id,zhi_shi_dian_ming_cheng,wan_zheng_lu_jing,ceng_ji,pai_xu,zhuang_tai) VALUES (?,?,?,1,999,'ACTIVE')",
                                subjectId, normalized.substring(normalized.lastIndexOf('>') + 1), normalized);
                    }
                }
            }
        }
    }

    private MockMultipartFile normalizedWorkbook(String subject) throws Exception {
        Path candidate = CANDIDATES.get(subject);
        try (var workbook = new XSSFWorkbook(Files.newInputStream(candidate)); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.getSheet("题目检查");
            var formatter = new DataFormatter();
            boolean imageAdded = false;
            for (int row = 2; row <= sheet.getLastRowNum(); row++) {
                var current = sheet.getRow(row);
                if (current == null) continue;
                String questionNumber = formatter.formatCellValue(current.getCell(4));
                for (int column : List.of(6, 7, 8, 9)) {
                    var cell = current.getCell(column);
                    if (cell == null || cell.getCellType() != org.apache.poi.ss.usermodel.CellType.STRING) continue;
                    String value = cell.getStringCellValue().replaceAll("〔(?:图片|公式)对象 [IF]\\d{3}〕", column == 7 ? "图示" : "");
                    cell.setCellValue(value);
                }
                current.getCell(10).setCellValue(0);
                current.getCell(11).setCellValue(0);
                if ("物理".equals(subject) && "14".equals(questionNumber) && !imageAdded) {
                    var stem = current.getCell(6);
                    stem.setCellValue(stem.getStringCellValue() + "〔图片对象 I126〕");
                    current.getCell(10).setCellValue(1);
                    imageAdded = true;
                }
            }
            assertThat(imageAdded || !"物理".equals(subject)).isTrue();
            workbook.write(output);
            return new MockMultipartFile("file", subject + "-golden30.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }

    private long user(String username, String role) {
        jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao,shi_fou_shou_ci_deng_lu) VALUES (?,?,0)", username, passwordEncoder.encode("a1234567"));
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        long roleId = jdbc.queryForObject("SELECT id FROM jiao_se WHERE jiao_se_dai_ma=?", Long.class, role);
        jdbc.update("INSERT INTO yong_hu_jiao_se(yong_hu_id,jiao_se_id) VALUES (?,?)", id, roleId);
        if ("STUDENT".equals(role)) jdbc.update("INSERT INTO xue_sheng_dang_an(yong_hu_id,xue_hao,xing_ming,nian_ji) VALUES (?,?,?,?)", id, "GOLDEN30", "Golden30 学生", "高一");
        return id;
    }

    private JsonNode safeAnswer(StudentPracticeDtos.SessionQuestion question) {
        return switch (question.questionType()) {
            case "SINGLE_CHOICE" -> JSON.getNodeFactory().textNode(question.options().getFirst().label());
            case "MULTIPLE_CHOICE" -> JSON.valueToTree(question.options().stream().limit(2).map(StudentPracticeDtos.Option::label).toList());
            case "FILL_BLANK" -> JSON.valueToTree(new ArrayList<>(java.util.Collections.nCopies(question.blankCount(), "")));
            default -> JSON.getNodeFactory().textNode("");
        };
    }

    private String marks(int count) { return String.join(",", java.util.Collections.nCopies(count, "?")); }
}
