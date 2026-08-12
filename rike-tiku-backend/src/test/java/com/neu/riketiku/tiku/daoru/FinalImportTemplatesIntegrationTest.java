package com.neu.riketiku.tiku.daoru;

import static org.assertj.core.api.Assertions.assertThat;

import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import com.neu.riketiku.xueshengdaoru.StudentImportConfirmService;
import com.neu.riketiku.xueshengdaoru.StudentImportService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class FinalImportTemplatesIntegrationTest extends AdminQuestionIntegrationTestSupport {
    private static final Path REPOSITORY = Path.of("..").toAbsolutePath().normalize();

    @Autowired private StudentImportService studentImport;
    @Autowired private StudentImportConfirmService studentConfirm;
    @Autowired private QuestionImportService questionImport;
    @Autowired private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void templateProperties(DynamicPropertyRegistry registry) {
        registry.add("rike.tiku.question-import.source-root", () -> REPOSITORY.resolve("题库").toString());
    }

    @Test
    void publishedTemplatesPassPreviewAndConfirmAgainstDisposableDatabase() throws Exception {
        jdbc.update("INSERT INTO ban_ji(ban_ji_bian_ma,ban_ji_ming_cheng,nian_ji,ru_xue_nian_fen,zhuang_tai) VALUES ('CLASS_TEMPLATE','模板验证班','高三',2023,'ACTIVE')");

        MockMultipartFile students = workbook("student-import-template.xlsx");
        var studentPreview = studentImport.preview(students);
        assertThat(studentPreview.totalCount()).isEqualTo(2);
        assertThat(studentPreview.invalidCount()).isZero();
        var studentResult = studentConfirm.confirm(students);
        assertThat(studentResult.importedCount()).isEqualTo(2);

        String reviewer = "template_admin_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao,zhang_hao_zhuang_tai,shi_fou_shou_ci_deng_lu) VALUES (?,?,'ENABLED',0)", reviewer, "$2a$10$abcdefghijklmnopqrstuvABCDEFGHIJKLMNOPQRSTUVabcdefghijklmno");
        Long reviewerId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        MockMultipartFile questions = workbook("question-import-template.xlsx");
        var questionPreview = questionImport.preview(questions);
        assertThat(questionPreview.totalCount()).isOne();
        assertThat(questionPreview.invalidCount()).isZero();
        var questionResult = questionImport.confirm(questions, questionPreview.fileHash(), reviewerId);
        assertThat(questionResult.importedCount()).isOne();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu q JOIN dao_ru_pi_ci b ON b.id=q.dao_ru_pi_ci_id WHERE b.pi_ci_bian_hao=? AND q.zhuang_tai='PENDING'", Integer.class, questionResult.batchCode())).isOne();
    }

    private MockMultipartFile workbook(String name) throws Exception {
        Path path = REPOSITORY.resolve("docs/templates").resolve(name);
        return new MockMultipartFile("file", name,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", Files.readAllBytes(path));
    }
}
