package com.neu.riketiku.tiku.daoru;

import static org.assertj.core.api.Assertions.assertThat;

import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class Mvp30WorkbookReadIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private QuestionImportService service;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void readsAllThreeUnmodifiedMvp30WorkbooksAgainstPureV1ToV6Baseline() throws Exception {
        Map<String, Expected> expected = Map.of(
                "物理", new Expected("PHYSICS", 10, 0, 10),
                "化学", new Expected("CHEMISTRY", 10, 1, 9),
                "生物", new Expected("BIOLOGY", 10, 1, 9));
        Map<String, Integer> before = businessTableCounts();
        var errorCodes = new java.util.ArrayList<String>();
        for (String subject : List.of("物理", "化学", "生物")) {
            Map.Entry<String, Expected> entry = Map.entry(subject, expected.get(subject));
            Path path = Path.of("..", "题库", "理综", "测试结果", entry.getKey(), "待审核_清洗版.xlsx");
            byte[] content = Files.readAllBytes(path);
            var preview = service.preview(new MockMultipartFile("file", path.getFileName().toString(),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content));
            assertThat(preview.subjectCode()).isEqualTo(entry.getValue().subjectCode());
            assertThat(preview.totalCount()).isEqualTo(entry.getValue().total());
            assertThat(preview.validCount()).as(subject + " pure V1-V6 valid count").isEqualTo(entry.getValue().valid());
            assertThat(preview.invalidCount()).isEqualTo(entry.getValue().invalid());
            errorCodes.addAll(preview.rows().stream().flatMap(row -> row.errors().stream()).map(error -> error.code()).toList());
        }
        assertThat(errorCodes).contains("KNOWLEDGE_POINT_NOT_FOUND");
        assertThat(errorCodes).anyMatch(code -> code.startsWith("ATTACHMENT_"));
        assertThat(businessTableCounts()).isEqualTo(before);
    }

    @Test
    @Transactional
    void isolatesAttachmentIntegrityAfterPreseedingWorkbookKnowledgePoints() throws Exception {
        Map<String, Expected> expected = Map.of(
                "物理", new Expected("PHYSICS", 10, 2, 8),
                "化学", new Expected("CHEMISTRY", 10, 1, 9),
                "生物", new Expected("BIOLOGY", 10, 6, 4));
        Map<String, Integer> before = businessTableCounts();
        for (String subject : List.of("物理", "化学", "生物")) {
            Map.Entry<String, Expected> entry = Map.entry(subject, expected.get(subject));
            Path path = Path.of("..", "题库", "理综", "测试结果", entry.getKey(), "待审核_清洗版.xlsx");
            seedKnowledgePoints(path, entry.getValue().subjectCode());
            byte[] content = Files.readAllBytes(path);
            var preview = service.preview(new MockMultipartFile("file", path.getFileName().toString(),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content));
            assertThat(preview.subjectCode()).isEqualTo(entry.getValue().subjectCode());
            assertThat(preview.totalCount()).isEqualTo(entry.getValue().total());
            assertThat(preview.validCount()).as(() -> preview.rows().stream().map(row -> row.rowNumber() + ":" + row.errors().stream().map(error -> error.code()).toList()).toList().toString()).isEqualTo(entry.getValue().valid());
            assertThat(preview.invalidCount()).isEqualTo(entry.getValue().invalid());
            assertThat(preview.fileHash()).hasSize(64);
            if (entry.getValue().invalid() > 0) assertThat(preview.rows().stream().flatMap(row -> row.errors().stream()).map(error -> error.code()).toList())
                    .anyMatch(code -> code.startsWith("ATTACHMENT_"));
        }
        assertThat(businessTableCounts()).isEqualTo(before);
    }

    private int count(String sql) { return jdbc.queryForObject(sql, Integer.class); }
    private Map<String, Integer> businessTableCounts() {
        return Map.of(
                "dao_ru_pi_ci", count("SELECT COUNT(*) FROM dao_ru_pi_ci"),
                "ti_mu", count("SELECT COUNT(*) FROM ti_mu"),
                "ti_mu_xuan_xiang", count("SELECT COUNT(*) FROM ti_mu_xuan_xiang"),
                "ti_mu_jie_xi", count("SELECT COUNT(*) FROM ti_mu_jie_xi"),
                "ti_mu_zhi_shi_dian", count("SELECT COUNT(*) FROM ti_mu_zhi_shi_dian"),
                "ti_mu_fu_jian", count("SELECT COUNT(*) FROM ti_mu_fu_jian"),
                "ti_mu_lai_yuan", count("SELECT COUNT(*) FROM ti_mu_lai_yuan"),
                "ti_mu_shen_he_ji_lu", count("SELECT COUNT(*) FROM ti_mu_shen_he_ji_lu"));
    }
    private void seedKnowledgePoints(Path path, String subjectCode) throws Exception {
        Long subjectId = jdbc.queryForObject("SELECT id FROM ke_mu WHERE ke_mu_dai_ma=?", Long.class, subjectCode);
        try (var workbook = WorkbookFactory.create(Files.newInputStream(path))) {
            var sheet = workbook.getSheet("题目检查");
            var formatter = new DataFormatter();
            for (int index = 2; index <= sheet.getLastRowNum(); index++) {
                var cell = sheet.getRow(index).getCell(13);
                String paths = formatter.formatCellValue(cell);
                for (String pointPath : paths.split("[；;]")) {
                    String normalized = pointPath.trim().replaceAll("\\s*[>＞]\\s*", ">");
                    if (!normalized.isBlank()) jdbc.update("INSERT IGNORE INTO zhi_shi_dian(ke_mu_id,zhi_shi_dian_ming_cheng,wan_zheng_lu_jing,ceng_ji,pai_xu,zhuang_tai) VALUES (?,?,?,1,999,'ACTIVE')",
                            subjectId, normalized.substring(normalized.lastIndexOf('>') + 1), normalized);
                }
            }
        }
    }
    private record Expected(String subjectCode, int total, int valid, int invalid) { }
}
