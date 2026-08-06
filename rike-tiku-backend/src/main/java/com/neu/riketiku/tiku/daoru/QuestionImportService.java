package com.neu.riketiku.tiku.daoru;

import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class QuestionImportService {
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final int MAX_ROWS = 100;
    private static final String SHEET_NAME = "题目检查";
    private static final List<String> HEADERS = List.of("学科", "年份", "区域", "试卷来源", "题号", "题型", "题干", "选项", "答案", "标准解析", "题干图片数", "解析图片数", "一级知识点", "知识点", "难度", "难度说明", "答案解析审核状态", "审核状态", "来源文件");
    private static final Pattern OPTION = Pattern.compile("^\\s*([A-Z])(?:[.．、])\\s*(.+?)\\s*$");
    private static final Pattern MARKER = Pattern.compile("〔(图片|公式)对象\\s+([IF]\\d{3})〕");
    private static final Pattern BLANK = Pattern.compile("(?=[①②③④⑤⑥⑦⑧⑨⑩])");
    private final JdbcTemplate jdbc;
    private final Path sourceRoot;

    public QuestionImportService(JdbcTemplate jdbc,
                                 @Value("${rike.tiku.question-import.source-root:../题库}") String sourceRoot) {
        this.jdbc = jdbc;
        this.sourceRoot = Path.of(sourceRoot).toAbsolutePath().normalize();
    }

    public QuestionImportDtos.Preview preview(MultipartFile file) {
        ValidationResult result = validate(file);
        return toPreview(result);
    }

    @Transactional
    public QuestionImportDtos.Confirm confirm(MultipartFile file, String previewFileHash, Long reviewerId) {
        ValidationResult result = validate(file);
        if (previewFileHash == null || previewFileHash.isBlank() || !result.fileHash().equals(previewFileHash)) {
            fail("IMPORT_FILE_CHANGED", "确认文件与预检查文件不一致，请重新预检查", HttpStatus.CONFLICT);
        }
        if (result.alreadyImported()) {
            fail("IMPORT_ALREADY_CONFIRMED", "该文件已经成功导入，不能重复确认", HttpStatus.CONFLICT);
        }
        if (result.rows().isEmpty() || result.rows().stream().anyMatch(row -> !row.errors().isEmpty())) {
            fail("IMPORT_VALIDATION_FAILED", "文件存在无效或重复题目，整批不能确认导入", HttpStatus.BAD_REQUEST);
        }
        String batchCode = "QUESTION_" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + "_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO dao_ru_pi_ci(pi_ci_bian_hao,dao_ru_lei_xing,yuan_shi_wen_jian_ming,yuan_shi_wen_jian_lu_jing,wen_jian_ha_xi,zong_ji_lu_shu,cheng_gong_shu,shi_bai_shu,zhuang_tai,bei_zhu) VALUES (?,?,?,?,?,?,?,?,?,?)",
                batchCode, "QUESTION", result.fileName(), controlledFilePath(result.subjectCode(), result.fileName()), result.fileHash(),
                result.rows().size(), result.rows().size(), 0, "IMPORTED", "MVP30 管理员题库导入；全部进入 PENDING");
        Long batchId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        for (ImportedRow row : result.rows()) {
            writeRow(batchId, row, reviewerId);
        }
        return new QuestionImportDtos.Confirm(batchCode, result.rows().size(), result.rows().size());
    }

    private ValidationResult validate(MultipartFile file) {
        validateFile(file);
        String fileHash = sha256(bytes(file));
        List<RawRow> rawRows = readRows(file);
        String subjectCode = rawRows.isEmpty() ? null : subjectCode(rawRows.getFirst().subject());
        Map<String, Integer> seenHashes = new HashMap<>();
        List<ImportedRow> rows = new ArrayList<>();
        for (RawRow raw : rawRows) {
            rows.add(validateRow(raw, seenHashes));
        }
        boolean alreadyImported = count("SELECT COUNT(*) FROM dao_ru_pi_ci WHERE dao_ru_lei_xing='QUESTION' AND wen_jian_ha_xi=? AND zhuang_tai='IMPORTED'", fileHash) > 0;
        return new ValidationResult(file.getOriginalFilename(), fileHash, subjectCode, alreadyImported, List.copyOf(rows));
    }

    private ImportedRow validateRow(RawRow raw, Map<String, Integer> seenHashes) {
        List<QuestionImportDtos.Error> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String subjectCode = subjectCode(raw.subject());
        if (subjectCode == null) {
            error(errors, "subject", "SUBJECT_INVALID", "学科只允许物理、化学或生物");
        }
        Mapping mapping = mapType(raw.questionType());
        if (mapping == null) {
            error(errors, "questionType", "QUESTION_TYPE_UNSUPPORTED", "题型无法安全映射，请人工处理");
        }
        required(raw.stem(), "stem", "STEM_REQUIRED", "题干不能为空", errors);
        required(raw.standardAnalysis(), "standardAnalysis", "STANDARD_ANALYSIS_REQUIRED", "标准解析不能为空", errors);
        required(raw.paperName(), "paperName", "SOURCE_REQUIRED", "试卷来源不能为空", errors);
        required(raw.sourceFile(), "sourceFile", "SOURCE_REQUIRED", "来源文件不能为空", errors);
        if (raw.year() == null) error(errors, "year", "SOURCE_YEAR_INVALID", "来源年份必须为整数");
        required(raw.region(), "region", "SOURCE_REQUIRED", "来源区域不能为空", errors);
        required(raw.questionNumber(), "questionNumber", "SOURCE_REQUIRED", "来源题号不能为空", errors);
        Integer difficulty = mapDifficulty(raw.difficulty());
        if (difficulty == null) error(errors, "difficulty", "DIFFICULTY_INVALID", "难度只支持 easy、medium、hard");
        List<OptionDraft> options = mapping != null && mapping.choice() ? parseOptions(raw.options(), errors) : List.of();
        String answer = mapping == null ? "{}" : parseAnswer(raw.answer(), mapping, options, errors);
        List<String> pointPaths = parsePointPaths(raw.knowledgePoints());
        if (pointPaths.isEmpty()) {
            error(errors, "knowledgePoints", "KNOWLEDGE_POINT_REQUIRED", "至少需要一个知识点路径");
        }
        List<Long> pointIds = new ArrayList<>();
        if (subjectCode != null) {
            Long subjectId = jdbc.query("SELECT id FROM ke_mu WHERE ke_mu_dai_ma=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0",
                    rs -> rs.next() ? rs.getLong(1) : null, subjectCode);
            if (subjectId == null) {
                error(errors, "subject", "SUBJECT_UNAVAILABLE", "学科不存在或已停用");
            } else {
                for (String path : pointPaths) {
                    Long pointId = jdbc.query("SELECT id FROM zhi_shi_dian WHERE ke_mu_id=? AND wan_zheng_lu_jing=? AND zhuang_tai='ACTIVE' AND yi_shan_chu=0",
                            rs -> rs.next() ? rs.getLong(1) : null, subjectId, path);
                    if (pointId == null) error(errors, "knowledgePoints", "KNOWLEDGE_POINT_NOT_FOUND", "知识点路径未在当前学科精确匹配：" + path);
                    else pointIds.add(pointId);
                }
            }
        }
        String hash = subjectCode == null || mapping == null ? "" : contentHash(raw.stem(), options);
        if (!hash.isEmpty()) {
            if (seenHashes.putIfAbsent(hash, raw.rowNumber()) != null) error(errors, "content", "CONTENT_DUPLICATE_IN_FILE", "与当前文件中的其他题目内容完全重复");
            if (count("SELECT COUNT(*) FROM ti_mu WHERE nei_rong_ha_xi=? AND yi_shan_chu=0", hash) > 0) error(errors, "content", "CONTENT_DUPLICATE_IN_DATABASE", "与数据库现有题目内容完全重复");
        }
        List<AttachmentDraft> attachments = attachments(raw, subjectCode, options, errors, warnings);
        if (!validImageCount(raw, attachments, "QUESTION") || !validImageCount(raw, attachments, "STANDARD_ANALYSIS")) {
            warnings.add("Excel图片数量仅用于复核；实际关联以正文对象标记和精确对象文件为准");
        }
        return new ImportedRow(raw, subjectCode, mapping, difficulty, options, answer, List.copyOf(pointPaths), List.copyOf(pointIds),
                List.copyOf(attachments), hash, List.copyOf(errors), List.copyOf(warnings));
    }

    private boolean validImageCount(RawRow raw, List<AttachmentDraft> attachments, String position) {
        int declared = "QUESTION".equals(position) ? raw.questionImageCount() : raw.analysisImageCount();
        int actual = (int) attachments.stream().filter(item -> item.position().equals(position) && item.type().equals("IMAGE")).count();
        return declared == actual;
    }

    private List<AttachmentDraft> attachments(RawRow raw, String subjectCode, List<OptionDraft> options,
                                               List<QuestionImportDtos.Error> errors, List<String> warnings) {
        if (subjectCode == null) return List.of();
        List<TextPart> textParts = new ArrayList<>();
        textParts.add(new TextPart("QUESTION", null, raw.stem()));
        for (int index = 0; index < options.size(); index++) textParts.add(new TextPart("OPTION", index, options.get(index).content()));
        textParts.add(new TextPart("ANSWER", null, raw.answer()));
        textParts.add(new TextPart("STANDARD_ANALYSIS", null, raw.standardAnalysis()));
        List<AttachmentDraft> result = new ArrayList<>();
        for (TextPart part : textParts) {
            Matcher matcher = MARKER.matcher(part.text());
            int order = 1;
            while (matcher.find()) {
                String marker = matcher.group(2);
                String type = marker.startsWith("I") ? "IMAGE" : "FORMULA";
                Path file = findObjectFile(subjectCode, raw.questionNumber(), marker, type, errors, raw.rowNumber());
                if (file != null) {
                    result.add(new AttachmentDraft(part.position(), part.optionIndex(), type, marker, file.getFileName().toString(),
                            controlledRelativePath(file), sha256(file), matcher.start() + 1, order++));
                }
            }
        }
        if (result.isEmpty() && (raw.questionImageCount() > 0 || raw.analysisImageCount() > 0)) {
            warnings.add("Excel声明存在图片对象，但正文没有可解析的对象标记");
        }
        return result;
    }

    private Path findObjectFile(String subjectCode, String questionNumber, String marker, String type,
                                List<QuestionImportDtos.Error> errors, int rowNumber) {
        String subject = Map.of("PHYSICS", "物理", "CHEMISTRY", "化学", "BIOLOGY", "生物").get(subjectCode);
        List<Path> roots = List.of(
                sourceRoot.resolve("理综").resolve("测试结果").resolve(subject).resolve("images").normalize(),
                sourceRoot.resolve(subject).resolve("母题库").resolve("images").normalize());
        String numeric = marker.substring(1);
        String expected = "(?i)^q" + Pattern.quote(questionNumber) + "_.+_"
                + ("IMAGE".equals(type) ? "image" : "formula") + "_" + numeric + "\\.[a-z0-9]+$";
        try {
            for (Path root : roots) {
                if (!root.startsWith(sourceRoot) || !Files.isDirectory(root)) continue;
                try (var files = Files.walk(root)) {
                    List<Path> matches = files.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().matches(expected)).toList();
                    if (matches.size() == 1) return matches.getFirst();
                    if (matches.size() > 1) {
                        error(errors, "attachments", "ATTACHMENT_OBJECT_AMBIGUOUS", "第" + rowNumber + "行对象" + marker + "匹配到多个附件");
                        return null;
                    }
                }
            }
            error(errors, "attachments", "ATTACHMENT_OBJECT_MISSING", "第" + rowNumber + "行对象" + marker + "未找到精确附件");
            return null;
        } catch (IOException exception) {
            error(errors, "attachments", "ATTACHMENT_DIRECTORY_UNAVAILABLE", "附件对象目录无法读取");
            return null;
        }
    }

    private void writeRow(Long batchId, ImportedRow row, Long reviewerId) {
        Long subjectId = jdbc.queryForObject("SELECT id FROM ke_mu WHERE ke_mu_dai_ma=?", Long.class, row.subjectCode());
        jdbc.update("INSERT INTO ti_mu(ke_mu_id,dao_ru_pi_ci_id,ti_mu_lei_xing,shi_yong_mo_shi,ti_gan,zheng_que_da_an,nan_du,nan_du_shuo_ming,shi_fou_ke_zi_dong_pan_fen,zhuang_tai,nei_rong_ha_xi) VALUES (?,?,?,?,?,CAST(? AS JSON),?,?,?,'PENDING',?)",
                subjectId, batchId, row.mapping().type(), row.mapping().usageMode(), row.raw().stem(), row.answer(), row.difficulty(), blank(row.raw().difficultyReason()), row.mapping().autoGradable(), row.contentHash());
        Long questionId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        Map<Integer, Long> optionIds = new HashMap<>();
        int order = 1;
        for (OptionDraft option : row.options()) {
            jdbc.update("INSERT INTO ti_mu_xuan_xiang(ti_mu_id,xuan_xiang_biao_shi,xuan_xiang_nei_rong,shi_fou_zheng_que,pai_xu) VALUES (?,?,?,?,?)",
                    questionId, option.label(), option.content(), option.correct(), order);
            optionIds.put(order++, jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
        }
        jdbc.update("INSERT INTO ti_mu_jie_xi(ti_mu_id,jie_xi_lei_xing,jie_xi_nei_rong,ban_ben_hao,zhuang_tai) VALUES (?,'STANDARD',?,1,'PENDING')", questionId, row.raw().standardAnalysis());
        Long analysisId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        order = 1;
        for (Long pointId : row.pointIds()) jdbc.update("INSERT INTO ti_mu_zhi_shi_dian(ti_mu_id,zhi_shi_dian_id,shi_fou_zhu_yao,pai_xu) VALUES (?,?,?,?)", questionId, pointId, order == 1, order++);
        for (String contentType : List.of("QUESTION", "ANSWER", "STANDARD_ANALYSIS")) {
            jdbc.update("INSERT INTO ti_mu_lai_yuan(ti_mu_id,nei_rong_lei_xing,lai_yuan_lei_xing,lai_yuan_ming_cheng,lai_yuan_di_zhi,nian_fen,di_qu,shi_juan_ming_cheng,ti_hao,quan_li_zhuang_tai,quan_li_yi_ju) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    questionId, contentType, "REAL_EXAM", row.raw().paperName(), controlledSourceAddress(row.raw().sourceFile()), row.raw().year(), blank(row.raw().region()), row.raw().paperName(), row.raw().questionNumber(), "COPYRIGHT_UNKNOWN", "MVP30 本地候选文件未提供可发布权利依据");
        }
        for (AttachmentDraft attachment : row.attachments()) {
            Long optionId = "OPTION".equals(attachment.position()) ? optionIds.get(attachment.optionIndex() + 1) : null;
            Long relatedAnalysisId = "STANDARD_ANALYSIS".equals(attachment.position()) ? analysisId : null;
            jdbc.update("INSERT INTO ti_mu_fu_jian(ti_mu_id,ti_mu_xuan_xiang_id,ti_mu_jie_xi_id,guan_lian_wei_zhi,fu_jian_lei_xing,yuan_shi_wen_jian_ming,xiang_dui_lu_jing,nei_rong_ha_xi,dui_xiang_biao_shi,zheng_wen_zi_fu_wei_zhi,pai_xu,zhuang_tai) VALUES (?,?,?,?,?,?,?,?,?,?,?, 'ACTIVE')",
                    questionId, optionId, relatedAnalysisId, attachment.position(), attachment.type(), attachment.fileName(), attachment.relativePath(), attachment.hash(), attachment.marker(), attachment.characterPosition(), attachment.order());
        }
        jdbc.update("INSERT INTO ti_mu_shen_he_ji_lu(ti_mu_id,shen_he_dong_zuo,yuan_zhuang_tai,mu_biao_zhuang_tai,shen_he_ren_id,shen_he_yi_jian) VALUES (?,'SUBMITTED','DRAFT','PENDING',?,?)",
                questionId, reviewerId, "管理员导入，等待人工审核");
    }

    private List<RawRow> readRows(MultipartFile file) {
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) fail("SHEET_NOT_FOUND", "未找到“题目检查”工作表", HttpStatus.BAD_REQUEST);
            validateHeaders(sheet.getRow(1));
            List<RawRow> rows = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();
            for (int index = 2; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null || blankRow(row, formatter)) continue;
                if (rows.size() >= MAX_ROWS) fail("ROW_LIMIT_EXCEEDED", "题目数据行不能超过100行", HttpStatus.BAD_REQUEST);
                List<String> values = new ArrayList<>();
                for (int column = 0; column < HEADERS.size(); column++) values.add(text(row, column, formatter));
                rows.add(new RawRow(index + 1, values));
            }
            return List.copyOf(rows);
        } catch (RenZhengYeWuYiChang exception) {
            throw exception;
        } catch (Exception exception) {
            fail("WORKBOOK_INVALID", "Excel文件无法读取或已损坏", HttpStatus.BAD_REQUEST);
            return List.of();
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) fail("FILE_EMPTY", "上传文件不能为空", HttpStatus.BAD_REQUEST);
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) fail("FILE_TYPE_INVALID", "只允许上传 .xlsx 文件", HttpStatus.BAD_REQUEST);
        if (file.getSize() > MAX_FILE_SIZE) fail("FILE_TOO_LARGE", "文件不能超过10MB", HttpStatus.BAD_REQUEST);
    }

    private void validateHeaders(Row row) {
        if (row == null) fail("WORKBOOK_INVALID", "Excel缺少第2行题目表头", HttpStatus.BAD_REQUEST);
        DataFormatter formatter = new DataFormatter();
        for (int index = 0; index < HEADERS.size(); index++) {
            if (!HEADERS.get(index).equals(text(row, index, formatter))) fail("WORKBOOK_INVALID", "题目检查Sheet表头不符合MVP30格式", HttpStatus.BAD_REQUEST);
        }
    }

    private String text(Row row, int column, DataFormatter formatter) {
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        if (cell.getCellType() == CellType.FORMULA) fail("FORMULA_CELL_NOT_ALLOWED", "题目检查数据区域不允许公式单元格", HttpStatus.BAD_REQUEST);
        return formatter.formatCellValue(cell).trim();
    }

    private boolean blankRow(Row row, DataFormatter formatter) {
        for (int index = 0; index < HEADERS.size(); index++) if (!text(row, index, formatter).isBlank()) return false;
        return true;
    }

    private Mapping mapType(String value) {
        String normalized = blank(value);
        if (normalized == null) return null;
        return switch (normalized) {
            case "单选题" -> new Mapping("SINGLE_CHOICE", "ONLINE_PRACTICE", true, true);
            case "多选题" -> new Mapping("MULTIPLE_CHOICE", "ONLINE_PRACTICE", true, true);
            case "实验填空题" -> new Mapping("FILL_BLANK", "ONLINE_PRACTICE", true, false);
            case "解答题" -> new Mapping("SUBJECTIVE", "TOPIC_LEARNING", false, false);
            default -> null;
        };
    }

    private List<OptionDraft> parseOptions(String text, List<QuestionImportDtos.Error> errors) {
        List<OptionDraft> options = new ArrayList<>();
        for (String line : text.split("\\R")) {
            Matcher matcher = OPTION.matcher(line);
            if (!matcher.matches()) {
                error(errors, "options", "OPTION_FORMAT_INVALID", "选择题选项必须使用 A. 选项内容 的逐行格式");
                return List.of();
            }
            options.add(new OptionDraft(matcher.group(1), matcher.group(2), false));
        }
        if (options.size() < 2 || new HashSet<>(options.stream().map(OptionDraft::label).toList()).size() != options.size()) error(errors, "options", "OPTION_INVALID", "选择题至少两个且选项标识不能重复");
        return options;
    }

    private String parseAnswer(String raw, Mapping mapping, List<OptionDraft> options, List<QuestionImportDtos.Error> errors) {
        if (mapping.type().equals("SUBJECTIVE")) return "{\"schemaVersion\":1,\"type\":\"SUBJECTIVE\"}";
        if (mapping.type().equals("FILL_BLANK")) {
            List<String> answers = new ArrayList<>();
            for (String segment : BLANK.split(raw)) {
                String cleaned = segment.replaceFirst("^[①②③④⑤⑥⑦⑧⑨⑩]\\.\\s*", "").trim();
                if (!cleaned.isBlank()) answers.add(cleaned);
            }
            if (answers.isEmpty()) error(errors, "answer", "FILL_BLANK_ANSWER_INVALID", "实验填空题答案无法安全拆分为多个空位");
            StringBuilder result = new StringBuilder("{\"schemaVersion\":1,\"type\":\"FILL_BLANK\",\"blanks\":[");
            for (int index = 0; index < answers.size(); index++) { if (index > 0) result.append(','); result.append("{\"acceptedAnswers\":[").append(json(answers.get(index))).append("]}"); }
            return result.append("]}").toString();
        }
        List<String> labels = new ArrayList<>();
        String normalized = raw.toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        if (normalized.matches("[A-Z]+")) {
            for (int index = 0; index < normalized.length(); index++) labels.add(String.valueOf(normalized.charAt(index)));
        } else {
            for (String item : normalized.split("[、,，]+")) if (!item.isBlank()) labels.add(item);
        }
        Set<String> available = new HashSet<>(options.stream().map(OptionDraft::label).toList());
        boolean valid = !labels.isEmpty() && labels.stream().allMatch(available::contains);
        if (mapping.type().equals("SINGLE_CHOICE") && labels.size() != 1) valid = false;
        if (mapping.type().equals("MULTIPLE_CHOICE") && labels.size() < 2) valid = false;
        if (!valid) error(errors, "answer", "CHOICE_ANSWER_INVALID", "正确答案与选择题选项不一致");
        List<OptionDraft> corrected = new ArrayList<>();
        for (OptionDraft option : options) corrected.add(new OptionDraft(option.label(), option.content(), labels.contains(option.label())));
        options.clear(); options.addAll(corrected);
        return "{\"schemaVersion\":1,\"type\":" + json(mapping.type()) + ",\"optionLabels\":[" + labels.stream().map(this::json).reduce((left, right) -> left + "," + right).orElse("") + "]}";
    }

    private List<String> parsePointPaths(String text) {
        List<String> result = new ArrayList<>();
        for (String item : text.split("[；;]")) {
            String path = item.trim().replaceAll("\\s*[>＞]\\s*", ">");
            if (!path.isBlank()) result.add(path);
        }
        return List.copyOf(result);
    }

    private String subjectCode(String subject) {
        return Map.of("物理", "PHYSICS", "化学", "CHEMISTRY", "生物", "BIOLOGY").get(blank(subject));
    }

    private Integer mapDifficulty(String value) {
        String normalized = blank(value);
        if (normalized == null) return null;
        return Map.of("easy", 1, "medium", 2, "hard", 3).get(normalized.toLowerCase(Locale.ROOT));
    }

    private QuestionImportDtos.Preview toPreview(ValidationResult result) {
        List<QuestionImportDtos.Row> rows = result.rows().stream().map(row -> new QuestionImportDtos.Row(row.raw().rowNumber(), row.subjectCode(), row.mapping() == null ? null : row.mapping().type(), row.mapping() == null ? null : row.mapping().usageMode(),
                summary(row.raw().stem()), row.pointPaths(), row.attachments().size(), row.contentHash(), row.errors().isEmpty() ? "VALID" : "INVALID", row.errors(), row.warnings())).toList();
        int valid = (int) rows.stream().filter(row -> row.status().equals("VALID")).count();
        int duplicates = (int) rows.stream().filter(row -> row.errors().stream().anyMatch(error -> error.code().contains("DUPLICATE"))).count();
        return new QuestionImportDtos.Preview(result.fileName(), result.fileHash(), result.subjectCode(), rows.size(), valid, rows.size() - valid, duplicates, result.alreadyImported(), rows);
    }

    private String contentHash(String stem, List<OptionDraft> options) {
        StringBuilder value = new StringBuilder(stem.replaceAll("\\s+", ""));
        for (OptionDraft option : options) value.append('|').append(option.label().trim()).append(':').append(option.content().replaceAll("\\s+", ""));
        return sha256(value.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String normalize(String value) { return value.replace("\r\n", "\n").replace('\r', '\n').trim(); }
    private String summary(String value) { String normalized = normalize(value); return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "…"; }
    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void required(String value, String field, String code, String message, List<QuestionImportDtos.Error> errors) { if (blank(value) == null) error(errors, field, code, message); }
    private void error(List<QuestionImportDtos.Error> errors, String field, String code, String message) { errors.add(new QuestionImportDtos.Error(field, code, message)); }
    private int count(String sql, Object... args) { return jdbc.queryForObject(sql, Integer.class, args); }
    private byte[] bytes(MultipartFile file) { try { return file.getBytes(); } catch (IOException exception) { fail("WORKBOOK_INVALID", "Excel文件无法读取", HttpStatus.BAD_REQUEST); return new byte[0]; } }
    private String sha256(Path file) { try { return sha256(Files.readAllBytes(file)); } catch (IOException exception) { throw new IllegalStateException("无法计算附件哈希", exception); } }
    private String sha256(byte[] content) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)); } catch (Exception exception) { throw new IllegalStateException(exception); } }
    private String controlledRelativePath(Path path) { return sourceRoot.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/'); }
    private String controlledFilePath(String subjectCode, String fileName) { String subject = Map.of("PHYSICS", "物理", "CHEMISTRY", "化学", "BIOLOGY", "生物").get(subjectCode); return "理综/测试结果/" + subject + "/" + fileName; }
    private String controlledSourceAddress(String value) { if (value == null) return null; String name = value.replace('\\', '/'); int index = name.lastIndexOf('/'); return index >= 0 ? "理综/" + name.substring(index + 1) : "理综/来源文件"; }
    private String json(String value) { return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""; }
    private void fail(String code, String message, HttpStatus status) { throw new RenZhengYeWuYiChang(code, message, status); }

    private record ValidationResult(String fileName, String fileHash, String subjectCode, boolean alreadyImported, List<ImportedRow> rows) {
    }
    private record Mapping(String type, String usageMode, boolean autoGradable, boolean choice) {
    }
    private record OptionDraft(String label, String content, boolean correct) {
    }
    private record AttachmentDraft(String position, Integer optionIndex, String type, String marker, String fileName,
                                   String relativePath, String hash, int characterPosition, int order) {
    }
    private record TextPart(String position, Integer optionIndex, String text) {
    }
    private record ImportedRow(RawRow raw, String subjectCode, Mapping mapping, Integer difficulty, List<OptionDraft> options,
                               String answer, List<String> pointPaths, List<Long> pointIds, List<AttachmentDraft> attachments,
                               String contentHash, List<QuestionImportDtos.Error> errors, List<String> warnings) {
    }
    private record RawRow(int rowNumber, List<String> values) {
        String subject() { return values.get(0); }
        Integer year() { try { return values.get(1).isBlank() ? null : Integer.valueOf(values.get(1)); } catch (NumberFormatException exception) { return null; } }
        String region() { return values.get(2); }
        String paperName() { return values.get(3); }
        String questionNumber() { return values.get(4); }
        String questionType() { return values.get(5); }
        String stem() { return values.get(6); }
        String options() { return values.get(7); }
        String answer() { return values.get(8); }
        String standardAnalysis() { return values.get(9); }
        int questionImageCount() { return number(10); }
        int analysisImageCount() { return number(11); }
        String knowledgePoints() { return values.get(13); }
        String difficulty() { return values.get(14); }
        String difficultyReason() { return values.get(15); }
        String sourceFile() { return values.get(18); }
        private int number(int index) { try { return Integer.parseInt(values.get(index)); } catch (NumberFormatException exception) { return 0; } }
    }
}
