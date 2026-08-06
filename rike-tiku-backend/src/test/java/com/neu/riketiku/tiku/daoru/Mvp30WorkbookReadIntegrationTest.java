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

@SpringBootTest
class Mvp30WorkbookReadIntegrationTest extends AdminQuestionIntegrationTestSupport {
    @Autowired private QuestionImportService service;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void readsAllThreeUnmodifiedMvp30WorkbooksWithoutWritingBusinessRows() throws Exception {
        int beforeBatches = count("SELECT COUNT(*) FROM dao_ru_pi_ci");
        int beforeQuestions = count("SELECT COUNT(*) FROM ti_mu");
        Map<String, Expected> expected = Map.of(
                "物理", new Expected("PHYSICS", 10, 2, 8),
                "化学", new Expected("CHEMISTRY", 10, 1, 9),
                "生物", new Expected("BIOLOGY", 10, 6, 4));
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
        assertThat(count("SELECT COUNT(*) FROM dao_ru_pi_ci")).isEqualTo(beforeBatches);
        assertThat(count("SELECT COUNT(*) FROM ti_mu")).isEqualTo(beforeQuestions);
    }

    private int count(String sql) { return jdbc.queryForObject(sql, Integer.class); }
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
