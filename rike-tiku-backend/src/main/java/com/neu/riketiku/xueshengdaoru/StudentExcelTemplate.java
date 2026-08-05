package com.neu.riketiku.xueshengdaoru;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class StudentExcelTemplate {
    static final List<String> HEADERS = List.of(
            "xue_hao", "xing_ming", "ban_ji_bian_ma", "nian_ji", "yong_hu_ming", "chu_shi_mi_ma", "zhang_hao_zhuang_tai");

    void write(OutputStream output) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet data = workbook.createSheet("学生导入");
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            Row header = data.createRow(0);
            for (int column = 0; column < HEADERS.size(); column++) {
                Cell cell = header.createCell(column);
                cell.setCellValue(HEADERS.get(column));
                cell.setCellStyle(headerStyle);
                data.setColumnWidth(column, column == 1 ? 18 * 256 : 16 * 256);
            }
            addExample(data, 1, "20260001", "学生甲", "199", "高三", "20260001", "", "ENABLED");
            addExample(data, 2, "20260002", "学生乙", "199", "高三", "", "", "");
            data.createFreezePane(0, 1);
            DataValidationHelper helper = data.getDataValidationHelper();
            DataValidationConstraint constraint = helper.createExplicitListConstraint(new String[] {"ENABLED", "DISABLED"});
            DataValidation validation = helper.createValidation(constraint, new CellRangeAddressList(1, 500, 6, 6));
            validation.setSuppressDropDownArrow(true);
            data.addValidationData(validation);

            Sheet guide = workbook.createSheet("填写说明");
            String[][] instructions = {
                    {"字段", "必填", "规则/默认值"},
                    {"xue_hao", "是", "字符串，1至64字符；保留前导零；全局唯一"},
                    {"xing_ming", "是", "2至32字符，不能纯空白"},
                    {"ban_ji_bian_ma", "是", "班级必须存在且为ACTIVE"},
                    {"nian_ji", "是", "必须与班级年级一致"},
                    {"yong_hu_ming", "否", "为空时默认等于学号；1至64字符且唯一"},
                    {"chu_shi_mi_ma", "否", "为空时确认导入阶段随机生成；预检查不回显密码"},
                    {"zhang_hao_zhuang_tai", "否", "默认ENABLED；可选ENABLED或DISABLED"},
                    {"注意", "", "预检查不会入库；禁止填写真实敏感信息；不使用公式或宏"}
            };
            for (int rowIndex = 0; rowIndex < instructions.length; rowIndex++) {
                Row row = guide.createRow(rowIndex);
                for (int column = 0; column < instructions[rowIndex].length; column++) {
                    Cell cell = row.createCell(column);
                    cell.setCellValue(instructions[rowIndex][column]);
                    if (rowIndex == 0) {
                        cell.setCellStyle(headerStyle);
                    }
                }
            }
            guide.setColumnWidth(0, 22 * 256);
            guide.setColumnWidth(1, 10 * 256);
            guide.setColumnWidth(2, 70 * 256);
            guide.createFreezePane(0, 1);
            workbook.write(output);
        }
    }

    private void addExample(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int column = 0; column < values.length; column++) {
            row.createCell(column).setCellValue(values[column]);
        }
    }
}
