package com.neu.riketiku.xueshengdaoru;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.riketiku.jiaoxue.entity.BanJi;
import com.neu.riketiku.jiaoxue.mapper.BanJiMapper;
import com.neu.riketiku.renzheng.RenZhengYeWuYiChang;
import com.neu.riketiku.xueshengdaoru.response.StudentImportError;
import com.neu.riketiku.xueshengdaoru.response.StudentImportPreviewResponse;
import com.neu.riketiku.xueshengdaoru.response.StudentImportRowResponse;
import com.neu.riketiku.zhanghao.entity.YongHu;
import com.neu.riketiku.zhanghao.mapper.YongHuMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StudentImportService {
    private static final int MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final int MAX_ROWS = 500;
    private final StudentExcelTemplate template;
    private final BanJiMapper banJiMapper;
    private final YongHuMapper yongHuMapper;
    private final JdbcTemplate jdbcTemplate;

    public StudentImportService(StudentExcelTemplate template, BanJiMapper banJiMapper,
            YongHuMapper yongHuMapper, JdbcTemplate jdbcTemplate) {
        this.template = template;
        this.banJiMapper = banJiMapper;
        this.yongHuMapper = yongHuMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void writeTemplate(OutputStream output) throws IOException {
        template.write(output);
    }

    public StudentImportPreviewResponse preview(MultipartFile file) {
        List<ValidatedRow> validatedRows = validateRows(file);
        List<StudentImportRowResponse> rows = validatedRows.stream().map(ValidatedRow::response).toList();
        int validCount = (int) rows.stream().filter(row -> "VALID".equals(row.status())).count();
        return new StudentImportPreviewResponse(file.getOriginalFilename(), rows.size(), validCount,
                rows.size() - validCount, rows);
    }

    List<ValidatedRow> validateRows(MultipartFile file) {
        validateFile(file);
        List<ParsedRow> parsedRows = readRows(file);
        Map<String, Integer> studentNumbers = new HashMap<>();
        Map<String, Integer> usernames = new HashMap<>();
        List<ValidatedRow> rows = new ArrayList<>();
        for (ParsedRow row : parsedRows) {
            List<StudentImportError> errors = validateRow(row, studentNumbers, usernames);
            rows.add(new ValidatedRow(row, toResponse(row, errors)));
        }
        return List.copyOf(rows);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            fail("FILE_EMPTY", "上传文件不能为空", HttpStatus.BAD_REQUEST);
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            fail("FILE_TYPE_INVALID", "只允许上传.xlsx文件", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            fail("FILE_TOO_LARGE", "文件不能超过5MB", HttpStatus.BAD_REQUEST);
        }
    }

    private List<ParsedRow> readRows(MultipartFile file) {
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheet("学生导入");
            if (sheet == null) {
                fail("SHEET_NOT_FOUND", "未找到“学生导入”工作表", HttpStatus.BAD_REQUEST);
            }
            validateHeaders(sheet.getRow(0));
            List<ParsedRow> rows = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }
                if (rows.size() >= MAX_ROWS) {
                    fail("ROW_LIMIT_EXCEEDED", "数据行不能超过500行", HttpStatus.BAD_REQUEST);
                }
                rows.add(new ParsedRow(index + 1,
                        text(row, 0, formatter), text(row, 1, formatter), text(row, 2, formatter),
                        text(row, 3, formatter), text(row, 4, formatter), text(row, 5, formatter),
                        text(row, 6, formatter)));
            }
            return rows;
        } catch (RenZhengYeWuYiChang exception) {
            throw exception;
        } catch (Exception exception) {
            fail("WORKBOOK_INVALID", "Excel文件无法读取或已损坏", HttpStatus.BAD_REQUEST);
            return List.of();
        }
    }

    private void validateHeaders(Row header) {
        if (header == null) {
            fail("WORKBOOK_INVALID", "Excel缺少表头", HttpStatus.BAD_REQUEST);
        }
        DataFormatter formatter = new DataFormatter();
        for (int index = 0; index < StudentExcelTemplate.HEADERS.size(); index++) {
            if (!StudentExcelTemplate.HEADERS.get(index).equals(text(header, index, formatter))) {
                fail("WORKBOOK_INVALID", "Excel表头不符合学生导入模板", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (int index = 0; index < StudentExcelTemplate.HEADERS.size(); index++) {
            if (!text(row, index, formatter).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String text(Row row, int index, DataFormatter formatter) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.FORMULA) {
            fail("FORMULA_CELL_NOT_ALLOWED", "不允许使用公式单元格", HttpStatus.BAD_REQUEST);
        }
        return formatter.formatCellValue(cell).trim();
    }

    private List<StudentImportError> validateRow(ParsedRow row, Map<String, Integer> studentNumbers,
            Map<String, Integer> usernames) {
        List<StudentImportError> errors = new ArrayList<>();
        required(row.studentNumber(), "studentNumber", "STUDENT_NUMBER_REQUIRED", "学号不能为空", errors);
        if (!row.studentNumber().isEmpty()) {
            if (row.studentNumber().length() > 64) {
                add(errors, "studentNumber", "STUDENT_NUMBER_REQUIRED", "学号长度不能超过64字符");
            }
            duplicate(row.studentNumber(), studentNumbers, "studentNumber", "STUDENT_NUMBER_DUPLICATE_IN_FILE", "学号在当前文件中重复", errors);
            if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM xue_sheng_dang_an WHERE xue_hao=? AND yi_shan_chu=0",
                    Integer.class, row.studentNumber()) > 0) {
                add(errors, "studentNumber", "STUDENT_NUMBER_ALREADY_EXISTS", "学号已存在");
            }
        }
        required(row.name(), "name", "NAME_REQUIRED", "姓名不能为空", errors);
        if (!row.name().isEmpty() && (row.name().length() < 2 || row.name().length() > 32)) {
            add(errors, "name", "NAME_INVALID", "姓名长度应为2至32字符");
        }
        required(row.classCode(), "classCode", "CLASS_CODE_REQUIRED", "班级编码不能为空", errors);
        required(row.grade(), "grade", "GRADE_REQUIRED", "年级不能为空", errors);
        BanJi banJi = findClass(row.classCode(), errors);
        if (banJi != null && !banJi.getNianJi().equals(row.grade())) {
            add(errors, "grade", "GRADE_CLASS_MISMATCH", "年级与班级不一致");
        }
        String username = row.username().isEmpty() ? row.studentNumber() : row.username();
        row.username(username);
        if (!username.matches("[A-Za-z0-9._-]{1,64}")) {
            add(errors, "username", "USERNAME_INVALID", "用户名只能包含字母、数字、点、下划线或连字符，长度1至64字符");
        } else {
            duplicate(username, usernames, "username", "USERNAME_DUPLICATE_IN_FILE", "用户名在当前文件中重复", errors);
            if (yongHuMapper.selectCount(new LambdaQueryWrapper<YongHu>().eq(YongHu::getYongHuMing, username)) > 0) {
                add(errors, "username", "USERNAME_ALREADY_EXISTS", "用户名已存在");
            }
        }
        if (!row.password().isEmpty() && !validPassword(row.password())) {
            add(errors, "initialPassword", "PASSWORD_POLICY_VIOLATION", "初始密码应为8至64位，且同时包含字母和数字");
        }
        if (row.accountStatus().isEmpty()) {
            row.accountStatus("ENABLED");
        }
        if (!Set.of("ENABLED", "DISABLED").contains(row.accountStatus())) {
            add(errors, "accountStatus", "INVALID_ACCOUNT_STATUS", "账号状态只允许ENABLED或DISABLED");
        }
        return errors;
    }

    private BanJi findClass(String classCode, List<StudentImportError> errors) {
        if (classCode.isEmpty()) {
            return null;
        }
        BanJi banJi = banJiMapper.selectOne(new LambdaQueryWrapper<BanJi>().eq(BanJi::getBanJiBianMa, classCode));
        if (banJi == null) {
            add(errors, "classCode", "CLASS_NOT_FOUND", "班级不存在");
        } else if (!"ACTIVE".equals(banJi.getZhuangTai())) {
            add(errors, "classCode", "CLASS_NOT_ACTIVE", "班级不是ACTIVE状态");
        }
        return banJi;
    }

    private void duplicate(String value, Map<String, Integer> seen, String field, String code, String message,
            List<StudentImportError> errors) {
        if (seen.putIfAbsent(value, 1) != null) {
            add(errors, field, code, message);
        }
    }

    private void required(String value, String field, String code, String message, List<StudentImportError> errors) {
        if (value.isEmpty()) {
            add(errors, field, code, message);
        }
    }

    private boolean validPassword(String value) {
        return value.length() >= 8 && value.length() <= 64 && !value.isBlank()
                && value.matches(".*[A-Za-z].*") && value.matches(".*[0-9].*");
    }

    private StudentImportRowResponse toResponse(ParsedRow row, List<StudentImportError> errors) {
        return new StudentImportRowResponse(row.rowNumber(), row.studentNumber(), row.name(), row.classCode(),
                row.grade(), row.username(), row.accountStatus(), !row.password().isEmpty(), row.password().isEmpty(),
                errors.isEmpty() ? "VALID" : "INVALID", List.copyOf(errors));
    }

    private void add(List<StudentImportError> errors, String field, String code, String message) {
        errors.add(new StudentImportError(field, code, message));
    }

    private void fail(String code, String message, HttpStatus status) {
        throw new RenZhengYeWuYiChang(code, message, status);
    }

    static final class ParsedRow {
        private final int rowNumber;
        private final String studentNumber;
        private final String name;
        private final String classCode;
        private final String grade;
        private String username;
        private final String password;
        private String accountStatus;

        private ParsedRow(int rowNumber, String studentNumber, String name, String classCode, String grade,
                String username, String password, String accountStatus) {
            this.rowNumber = rowNumber;
            this.studentNumber = studentNumber;
            this.name = name;
            this.classCode = classCode;
            this.grade = grade;
            this.username = username;
            this.password = password;
            this.accountStatus = accountStatus;
        }
        int rowNumber() { return rowNumber; }
        String studentNumber() { return studentNumber; }
        String name() { return name; }
        String classCode() { return classCode; }
        String grade() { return grade; }
        String username() { return username; }
        void username(String value) { username = value; }
        String password() { return password; }
        String accountStatus() { return accountStatus; }
        void accountStatus(String value) { accountStatus = value; }
    }

    record ValidatedRow(ParsedRow row, StudentImportRowResponse response) {
    }
}
