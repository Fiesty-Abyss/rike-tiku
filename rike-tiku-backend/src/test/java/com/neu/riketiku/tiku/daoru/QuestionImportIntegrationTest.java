package com.neu.riketiku.tiku.daoru;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.tiku.admin.AdminQuestionIntegrationTestSupport;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class QuestionImportIntegrationTest extends AdminQuestionIntegrationTestSupport {
    private static final Path ROOT = Path.of("target", "question-import-fixture-" + UUID.randomUUID()).toAbsolutePath();
    private static final List<String> HEADERS = List.of("学科", "年份", "区域", "试卷来源", "题号", "题型", "题干", "选项", "答案", "标准解析", "题干图片数", "解析图片数", "一级知识点", "知识点", "难度", "难度说明", "答案解析审核状态", "审核状态", "来源文件");

    @Autowired private QuestionImportService service;
    @Autowired private JdbcTemplate jdbc;

    @BeforeAll
    static void prepareFixtureRoot() throws Exception {
        Files.createDirectories(ROOT.resolve("物理/母题库/images"));
        Files.createDirectories(ROOT.resolve("物理/母题库/attachments"));
        Files.createDirectories(ROOT.resolve("sources"));
        Files.writeString(ROOT.resolve("物理/母题库/images/q3_题干_formula_001.png"), "fixture-formula");
        Files.writeString(ROOT.resolve("sources/test-paper.doc"), "fixture-paper");
        Files.writeString(ROOT.resolve("sources/test-answer.doc"), "fixture-answer");
    }

    @DynamicPropertySource
    static void importProperties(DynamicPropertyRegistry registry) {
        registry.add("rike.tiku.question-import.source-root", ROOT::toString);
    }

    @Test
    void previewMapsChoiceFillBlankAndSubjectiveWithoutWritingBusinessTables() throws Exception {
        int before = count("SELECT COUNT(*) FROM ti_mu WHERE dao_ru_pi_ci_id IS NOT NULL");
        var preview = service.preview(file("types.xlsx", workbook(
                row("1", "单选题", "单选题干", "A. 甲\nB. 乙", "A", "解析", "easy"),
                row("2", "多选题", "多选题干", "A. 甲\nB. 乙\nC. 丙", "A、C", "解析", "medium"),
                row("3", "实验填空题", "填空题干〔公式对象 F001〕", "", "①. 正极 ②. 负极", "解析", "hard"),
                row("4", "解答题", "主观题干", "", "任意原文答案", "解析", "medium"))));
        assertThat(preview.totalCount()).isEqualTo(4);
        assertThat(preview.validCount()).isEqualTo(4);
        assertThat(preview.rows()).extracting(QuestionImportDtos.Row::questionType)
                .containsExactly("SINGLE_CHOICE", "MULTIPLE_CHOICE", "FILL_BLANK", "SUBJECTIVE");
        assertThat(preview.rows().get(2).attachmentCount()).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM ti_mu WHERE dao_ru_pi_ci_id IS NOT NULL")).isEqualTo(before);
    }

    @Test
    void rejectsBrokenHeadersFormulaAndDuplicateContent() throws Exception {
        assertThatThrownBy(() -> service.preview(file("bad.xlsx", badWorkbook())))
                .isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("表头");
        assertThatThrownBy(() -> service.preview(file("formula.xlsx", formulaWorkbook())))
                .isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("公式");
        var preview = service.preview(file("duplicate.xlsx", workbook(
                row("5", "单选题", "重复题干", "A. 甲\nB. 乙", "A", "解析", "easy"),
                row("6", "单选题", "重复题干", "A. 甲\nB. 乙", "A", "解析", "easy"))));
        assertThat(preview.invalidCount()).isEqualTo(1);
        assertThat(preview.rows().get(1).errors()).extracting(QuestionImportDtos.Error::code).contains("CONTENT_DUPLICATE_IN_FILE");
    }

    @Test
    void confirmWritesBatchQuestionRelationsPendingAnalysisAndSubmittedReview() throws Exception {
        var upload = file("confirm.xlsx", workbook(row("7", "单选题", "确认入库题", "A. 甲\nB. 乙", "A", "标准解析", "easy")));
        var preview = service.preview(upload);
        Long reviewerId = reviewer();
        var response = service.confirm(upload, preview.fileHash(), reviewerId);
        assertThat(response.importedCount()).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM dao_ru_pi_ci WHERE pi_ci_bian_hao=?", response.batchCode())).isOne();
        assertThat(jdbc.queryForObject("SELECT yuan_shi_wen_jian_lu_jing FROM dao_ru_pi_ci WHERE pi_ci_bian_hao=?", String.class, response.batchCode())).isNull();
        assertThat(count("SELECT COUNT(*) FROM ti_mu q JOIN dao_ru_pi_ci b ON b.id=q.dao_ru_pi_ci_id WHERE b.pi_ci_bian_hao=? AND q.zhuang_tai='PENDING'", response.batchCode())).isOne();
        assertThat(count("SELECT COUNT(*) FROM ti_mu_jie_xi x JOIN ti_mu q ON q.id=x.ti_mu_id JOIN dao_ru_pi_ci b ON b.id=q.dao_ru_pi_ci_id WHERE b.pi_ci_bian_hao=? AND x.zhuang_tai='PENDING'", response.batchCode())).isOne();
        assertThat(count("SELECT COUNT(*) FROM ti_mu_lai_yuan x JOIN ti_mu q ON q.id=x.ti_mu_id JOIN dao_ru_pi_ci b ON b.id=q.dao_ru_pi_ci_id WHERE b.pi_ci_bian_hao=?", response.batchCode())).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM ti_mu_shen_he_ji_lu x JOIN ti_mu q ON q.id=x.ti_mu_id JOIN dao_ru_pi_ci b ON b.id=q.dao_ru_pi_ci_id WHERE b.pi_ci_bian_hao=? AND x.shen_he_dong_zuo='SUBMITTED' AND x.shen_he_ren_id=?", response.batchCode(), reviewerId)).isOne();
        assertThatThrownBy(() -> service.confirm(upload, preview.fileHash(), reviewerId))
                .isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("已经成功导入");
    }

    @Test
    void confirmationFailureRollsBackEveryImportedTable() throws Exception {
        var upload = file("rollback.xlsx", workbook(row("8", "单选题", "回滚题", "A. 甲\nB. 乙", "A", "标准解析", "easy")));
        var preview = service.preview(upload);
        int batches = count("SELECT COUNT(*) FROM dao_ru_pi_ci");
        int questions = count("SELECT COUNT(*) FROM ti_mu");
        int options = count("SELECT COUNT(*) FROM ti_mu_xuan_xiang");
        int analyses = count("SELECT COUNT(*) FROM ti_mu_jie_xi");
        int points = count("SELECT COUNT(*) FROM ti_mu_zhi_shi_dian");
        int attachments = count("SELECT COUNT(*) FROM ti_mu_fu_jian");
        int sources = count("SELECT COUNT(*) FROM ti_mu_lai_yuan");
        int reviews = count("SELECT COUNT(*) FROM ti_mu_shen_he_ji_lu");
        assertThatThrownBy(() -> service.confirm(upload, preview.fileHash(), 999999L)).isInstanceOf(Exception.class);
        assertThat(count("SELECT COUNT(*) FROM dao_ru_pi_ci")).isEqualTo(batches);
        assertThat(count("SELECT COUNT(*) FROM ti_mu")).isEqualTo(questions);
        assertThat(count("SELECT COUNT(*) FROM ti_mu_xuan_xiang")).isEqualTo(options);
        assertThat(count("SELECT COUNT(*) FROM ti_mu_jie_xi")).isEqualTo(analyses);
        assertThat(count("SELECT COUNT(*) FROM ti_mu_zhi_shi_dian")).isEqualTo(points);
        assertThat(count("SELECT COUNT(*) FROM ti_mu_fu_jian")).isEqualTo(attachments);
        assertThat(count("SELECT COUNT(*) FROM ti_mu_lai_yuan")).isEqualTo(sources);
        assertThat(count("SELECT COUNT(*) FROM ti_mu_shen_he_ji_lu")).isEqualTo(reviews);
    }

    @Test
    void blocksDeclaredImageProblemsDuplicateObjectsAndAmbiguousRoots() throws Exception {
        Files.writeString(ROOT.resolve("物理/母题库/images/q11_题干_image_001.png"), "unique-image");
        var unique = service.preview(file("unique.xlsx", workbook(with(row("11", "单选题", "题干〔图片对象 I001〕", "A. 甲\nB. 乙", "A", "解析", "easy"), 10, "1"))));
        assertThat(unique.validCount()).isOne();

        var noMarker = service.preview(file("no-marker.xlsx", workbook(with(row("12", "单选题", "没有对象标记", "A. 甲\nB. 乙", "A", "解析", "easy"), 10, "1"))));
        assertThat(noMarker.rows().getFirst().errors()).extracting(QuestionImportDtos.Error::code).contains("ATTACHMENT_MARKER_MISSING", "ATTACHMENT_IMAGE_COUNT_MISMATCH");

        var mismatch = service.preview(file("mismatch.xlsx", workbook(with(row("11", "单选题", "题干〔图片对象 I001〕", "A. 甲\nB. 乙", "A", "解析", "easy"), 10, "2"))));
        assertThat(mismatch.rows().getFirst().errors()).extracting(QuestionImportDtos.Error::code).contains("ATTACHMENT_IMAGE_COUNT_MISMATCH");

        var duplicate = service.preview(file("duplicate-object.xlsx", workbook(with(row("11", "单选题", "题干〔图片对象 I001〕〔图片对象 I001〕", "A. 甲\nB. 乙", "A", "解析", "easy"), 10, "1"))));
        assertThat(duplicate.rows().getFirst().errors()).extracting(QuestionImportDtos.Error::code).contains("ATTACHMENT_OBJECT_DUPLICATE");

        Files.writeString(ROOT.resolve("物理/母题库/attachments/q13_题干_image_001.png"), "second-root-image");
        Files.writeString(ROOT.resolve("物理/母题库/images/q13_题干_image_001.png"), "first-root-image");
        var ambiguous = service.preview(file("ambiguous.xlsx", workbook(with(row("13", "单选题", "题干〔图片对象 I001〕", "A. 甲\nB. 乙", "A", "解析", "easy"), 10, "1"))));
        assertThat(ambiguous.rows().getFirst().errors()).extracting(QuestionImportDtos.Error::code).contains("ATTACHMENT_OBJECT_AMBIGUOUS");
    }

    @Test
    void enforcesSingleSubjectAndKeepsSubjectiveAndFillBlankAnswers() throws Exception {
        var blankSubject = service.preview(file("blank-subject.xlsx", workbook(with(row("14", "单选题", "空学科", "A. 甲\nB. 乙", "A", "解析", "easy"), 0, ""))));
        assertThat(blankSubject.rows().getFirst().errors()).extracting(QuestionImportDtos.Error::code).contains("SUBJECT_INVALID");

        String[] chemistry = with(row("15", "单选题", "混合学科", "A. 甲\nB. 乙", "A", "解析", "easy"), 0, "化学");
        chemistry[13] = "化学基本概念>化学与社会>材料和文物保护";
        var mixed = service.preview(file("mixed.xlsx", workbook(row("16", "单选题", "物理题", "A. 甲\nB. 乙", "A", "解析", "easy"), chemistry)));
        assertThat(mixed.subjectCode()).isNull();
        assertThat(mixed.rows()).allSatisfy(value -> assertThat(value.errors()).extracting(QuestionImportDtos.Error::code).contains("SUBJECT_MIXED_FILE"));

        Files.writeString(ROOT.resolve("物理/母题库/images/q17_答案_image_002.png"), "answer-image");
        String[] subjective = with(row("17", "解答题", "主观题干", "", "原始参考答案〔图片对象 I002〕", "解析", "medium"), 10, "0");
        var upload = file("subjective.xlsx", workbook(subjective));
        var preview = service.preview(upload);
        var response = service.confirm(upload, preview.fileHash(), reviewer());
        String answer = jdbc.queryForObject("SELECT CAST(q.zheng_que_da_an AS CHAR) FROM ti_mu q JOIN dao_ru_pi_ci b ON b.id=q.dao_ru_pi_ci_id WHERE b.pi_ci_bian_hao=?", String.class, response.batchCode());
        assertThat(answer).contains("SUBJECTIVE", "referenceAnswer", "原始参考答案");
        assertThat(count("SELECT COUNT(*) FROM ti_mu_fu_jian a JOIN ti_mu q ON q.id=a.ti_mu_id JOIN dao_ru_pi_ci b ON b.id=q.dao_ru_pi_ci_id WHERE b.pi_ci_bian_hao=? AND a.guan_lian_wei_zhi='ANSWER' AND a.zheng_wen_zi_fu_wei_zhi > 0", response.batchCode())).isOne();

        var fillUpload = file("fill.xlsx", workbook(row("18", "实验填空题", "填空题", "", "①. 原答案一 ②. 原答案二", "解析", "hard")));
        var fill = service.preview(fillUpload);
        assertThat(fill.rows().getFirst().status()).isEqualTo("VALID");
        var fillResponse = service.confirm(fillUpload, fill.fileHash(), reviewer());
        String fillAnswer = jdbc.queryForObject("SELECT CAST(q.zheng_que_da_an AS CHAR) FROM ti_mu q JOIN dao_ru_pi_ci b ON b.id=q.dao_ru_pi_ci_id WHERE b.pi_ci_bian_hao=?", String.class, fillResponse.batchCode());
        assertThat(fillAnswer).contains("\"index\": 1", "\"index\": 2", "原答案一", "原答案二");
    }

    @Test
    void rejectsUnsafeOrMissingSourcesAndChangedPreviewHash() throws Exception {
        String[] missing = with(row("20", "单选题", "缺少来源", "A. 甲\nB. 乙", "A", "解析", "easy"), 18,
                "试题文件：sources/missing.doc\n答案解析文件：sources/test-answer.doc");
        var missingPreview = service.preview(file("missing-source.xlsx", workbook(missing)));
        assertThat(missingPreview.rows().getFirst().errors()).extracting(QuestionImportDtos.Error::code).contains("SOURCE_FILE_MISSING");

        String[] outside = with(row("21", "单选题", "越界来源", "A. 甲\nB. 乙", "A", "解析", "easy"), 18,
                "试题文件：../outside.doc\n答案解析文件：sources/test-answer.doc");
        var outsidePreview = service.preview(file("outside-source.xlsx", workbook(outside)));
        assertThat(outsidePreview.rows().getFirst().errors()).extracting(QuestionImportDtos.Error::code).contains("SOURCE_PATH_OUTSIDE_ROOT");

        var upload = file("changed.xlsx", workbook(row("22", "单选题", "哈希改变", "A. 甲\nB. 乙", "A", "解析", "easy")));
        assertThatThrownBy(() -> service.confirm(upload, "not-the-preview-hash", reviewer()))
                .isInstanceOf(RenZhengYeWuYiChang.class).hasMessageContaining("不一致");
    }

    private Long reviewer() {
        String name = "import_admin_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO yong_hu(yong_hu_ming,mi_ma_zhai_yao,zhang_hao_zhuang_tai,shi_fou_shou_ci_deng_lu) VALUES (?,?,'ENABLED',0)", name, "$2a$10$abcdefghijklmnopqrstuvABCDEFGHIJKLMNOPQRSTUVabcdefghijklmno");
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private int count(String sql, Object... arguments) {
        return jdbc.queryForObject(sql, Integer.class, arguments);
    }

    private MockMultipartFile file(String name, byte[] body) {
        return new MockMultipartFile("file", name, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", body);
    }

    private String[] row(String number, String type, String stem, String options, String answer, String analysis, String difficulty) {
        return new String[] {"物理", "2023", "全国", "匿名测试卷", number, type, stem, options, answer, analysis, "0", "0", "力学", "力学>机械振动与机械波>波速、波长与频率", difficulty, "测试难度", "待审核", "待审核", "试题文件：sources/test-paper.doc\n答案解析文件：sources/test-answer.doc"};
    }

    private String[] with(String[] source, int column, String value) {
        String[] copy = source.clone();
        copy[column] = value;
        return copy;
    }

    private byte[] workbook(String[]... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("题目检查");
            sheet.createRow(0).createCell(0).setCellValue("MVP30题目检查");
            var header = sheet.createRow(1);
            for (int column = 0; column < HEADERS.size(); column++) header.createCell(column).setCellValue(HEADERS.get(column));
            for (int index = 0; index < rows.length; index++) {
                var row = sheet.createRow(index + 2);
                for (int column = 0; column < rows[index].length; column++) row.createCell(column).setCellValue(rows[index][column]);
            }
            workbook.createSheet("质量统计").createRow(0).createCell(0).setCellFormula("1+1");
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private byte[] badWorkbook() throws Exception {
        byte[] body = workbook(row("9", "单选题", "错误表头", "A. 甲\nB. 乙", "A", "解析", "easy"));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(body)); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.getSheet("题目检查").getRow(1).getCell(0).setCellValue("错误列");
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private byte[] formulaWorkbook() throws Exception {
        byte[] body = workbook(row("10", "单选题", "公式题干", "A. 甲\nB. 乙", "A", "解析", "easy"));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(body)); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.getSheet("题目检查").getRow(2).getCell(6).setCellFormula("1+1");
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
