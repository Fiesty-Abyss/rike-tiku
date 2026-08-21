package com.neu.riketiku.tiku.daoru;

import static org.assertj.core.api.Assertions.assertThat;

import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import com.neu.riketiku.xueshengdaoru.StudentImportConfirmService;
import com.neu.riketiku.xueshengdaoru.StudentImportService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** Validates the committed demonstration workbooks without importing them into either live schema. */
@SpringBootTest
@Transactional
class FinalDemoImportWorkbooksIntegrationTest extends AdminQuestionIntegrationTestSupport {
    private static final Path REPOSITORY = Path.of("..").toAbsolutePath().normalize();
    private static final Path DEMO_IMPORTS = REPOSITORY.resolve("docs/demo-import");

    @Autowired private StudentImportService studentImport;
    @Autowired private StudentImportConfirmService studentConfirm;
    @Autowired private QuestionImportService questionImport;
    @Autowired private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void importProperties(DynamicPropertyRegistry registry) {
        registry.add("rike.tiku.question-import.source-root", () -> REPOSITORY.resolve("题库").toString());
    }

    @Test
    void committed203StudentAndScienceWorkbooksPreviewAndConfirmInDisposableSchema() throws Exception {
        jdbc.update("INSERT INTO ban_ji(ban_ji_bian_ma,ban_ji_ming_cheng,nian_ji,ru_xue_nian_fen,zhuang_tai) VALUES ('CLASS_203','203班','高三',2023,'ACTIVE')");
        seedDemoKnowledgeCatalog();

        MockMultipartFile students = workbook("学生导入_203班_演示.xlsx");
        var studentPreview = studentImport.preview(students);
        assertThat(studentPreview.totalCount()).isEqualTo(2);
        assertThat(studentPreview.invalidCount()).isZero();
        var studentConfirmResult = studentConfirm.confirm(students);
        assertThat(studentConfirmResult.importedCount()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM xue_sheng_dang_an WHERE xue_hao IN ('S20360001','S20360002')", Integer.class)).isEqualTo(2);

        String reviewer = "final_demo_import_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao,zhang_hao_zhuang_tai,shi_fou_shou_ci_deng_lu) VALUES (?,?,'ENABLED',0)",
                reviewer, "$2a$10$abcdefghijklmnopqrstuvABCDEFGHIJKLMNOPQRSTUVabcdefghijklmno");
        Long reviewerId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        for (String workbook : List.of("题库导入_物理_演示.xlsx", "题库导入_化学_演示.xlsx", "题库导入_生物_演示.xlsx")) {
            MockMultipartFile questions = workbook(workbook);
            var preview = questionImport.preview(questions);
            assertThat(preview.totalCount()).isEqualTo(3);
            assertThat(preview.invalidCount()).withFailMessage(() -> preview.rows().toString()).isZero();
            assertThat(preview.duplicateCount()).isZero();
            assertThat(preview.alreadyImported()).isFalse();

            var confirmed = questionImport.confirm(questions, preview.fileHash(), reviewerId);
            assertThat(confirmed.importedCount()).isEqualTo(3);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu q JOIN dao_ru_pi_ci b ON b.id=q.dao_ru_pi_ci_id WHERE b.pi_ci_bian_hao=? AND q.zhuang_tai='PENDING'", Integer.class, confirmed.batchCode())).isEqualTo(3);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ti_mu_jie_xi a JOIN ti_mu q ON q.id=a.ti_mu_id WHERE a.jie_xi_lei_xing='STANDARD' AND a.zhuang_tai='PENDING' AND q.dao_ru_pi_ci_id IS NOT NULL", Integer.class)).isGreaterThanOrEqualTo(9);
    }

    private MockMultipartFile workbook(String name) throws Exception {
        Path path = DEMO_IMPORTS.resolve(name);
        return new MockMultipartFile("file", name,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", Files.readAllBytes(path));
    }

    private void seedDemoKnowledgeCatalog() {
        seedPoint("PHYSICS", "力学", "功和能", "机械能守恒");
        seedPoint("PHYSICS", "物理实验", "数据处理", "不确定度与图像");
        seedPoint("CHEMISTRY", "化学反应原理", "水溶液", "酸碱与pH");
        seedPoint("CHEMISTRY", "化学反应原理", "电化学", "原电池与电解池");
        seedPoint("CHEMISTRY", "化学基本概念", "物质的量", "摩尔计算");
        seedPoint("BIOLOGY", "生物实验", "科学探究", "变量控制与数据分析");
        seedPoint("BIOLOGY", "遗传与进化", "遗传规律", "分离定律");
        seedPoint("BIOLOGY", "生物与环境", "生态系统", "能量流动与物质循环");
    }

    private void seedPoint(String subjectCode, String... labels) {
        Long subjectId = jdbc.queryForObject("SELECT id FROM ke_mu WHERE ke_mu_dai_ma=?", Long.class, subjectCode);
        Long parentId = null;
        String path = "";
        for (int index = 0; index < labels.length; index++) {
            path = path.isEmpty() ? labels[index] : path + ">" + labels[index];
            Long existing = jdbc.query("SELECT id FROM zhi_shi_dian WHERE ke_mu_id=? AND wan_zheng_lu_jing=?",
                    result -> result.next() ? result.getLong(1) : null, subjectId, path);
            if (existing == null) {
                jdbc.update("INSERT INTO zhi_shi_dian(ke_mu_id,fu_zhi_shi_dian_id,zhi_shi_dian_ming_cheng,wan_zheng_lu_jing,ceng_ji,pai_xu,zhuang_tai) VALUES (?,?,?,?,?,100,'ACTIVE')",
                        subjectId, parentId, labels[index], path, index + 1);
                existing = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            }
            parentId = existing;
        }
    }
}
