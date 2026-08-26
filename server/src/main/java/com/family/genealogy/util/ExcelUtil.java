package com.family.genealogy.util;

import com.family.genealogy.entity.FamilyMember;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导入导出工具类
 */
public class ExcelUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String[] EXPORT_HEADERS = {
            "ID", "姓名", "性别", "世代", "房支", "父节点ID",
            "配偶ID", "出生日期", "逝世日期", "出生地", "简介", "排序"
    };

    /**
     * 导出族谱成员到 Excel
     */
    public static void exportMembers(List<FamilyMember> members, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("族谱成员");

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 写入表头
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(EXPORT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据样式
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // 写入数据
            for (int i = 0; i < members.size(); i++) {
                FamilyMember member = members.get(i);
                Row row = sheet.createRow(i + 1);

                createCell(row, 0, member.getId() != null ? member.getId().toString() : "", dataStyle);
                createCell(row, 1, member.getName() != null ? member.getName() : "", dataStyle);
                createCell(row, 2, "M".equals(member.getGender()) ? "男" : "女", dataStyle);
                createCell(row, 3, member.getGeneration() != null ? member.getGeneration().toString() : "", dataStyle);
                createCell(row, 4, member.getBranch() != null ? member.getBranch() : "", dataStyle);
                createCell(row, 5, member.getParentId() != null ? member.getParentId().toString() : "", dataStyle);
                createCell(row, 6, member.getSpouseId() != null ? member.getSpouseId().toString() : "", dataStyle);
                createCell(row, 7, member.getBirthDate() != null ? member.getBirthDate().format(DATE_FORMATTER) : "", dataStyle);
                createCell(row, 8, member.getDeathDate() != null ? member.getDeathDate().format(DATE_FORMATTER) : "", dataStyle);
                createCell(row, 9, member.getBirthPlace() != null ? member.getBirthPlace() : "", dataStyle);
                createCell(row, 10, member.getBiography() != null ? member.getBiography() : "", dataStyle);
                createCell(row, 11, member.getSortOrder() != null ? member.getSortOrder().toString() : "0", dataStyle);
            }

            // 自动调整列宽
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
                // 设置最小宽度
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
            }

            workbook.write(outputStream);
        }
    }

    /**
     * 从 Excel 导入族谱成员
     */
    public static List<FamilyMember> importMembers(InputStream inputStream) throws IOException {
        List<FamilyMember> members = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return members;
            }

            // 跳过表头，从第二行开始读取
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                FamilyMember member = new FamilyMember();

                // ID（导入时可能为空，表示新增）
                String idStr = getCellStringValue(row.getCell(0));
                if (!idStr.isEmpty()) {
                    try {
                        member.setId(Long.parseLong(idStr));
                    } catch (NumberFormatException ignored) {
                    }
                }

                // 姓名（必填）
                String name = getCellStringValue(row.getCell(1));
                if (name.isEmpty()) {
                    continue; // 跳过没有姓名的行
                }
                member.setName(name);

                // 性别
                String genderStr = getCellStringValue(row.getCell(2));
                member.setGender(parseGender(genderStr));

                // 世代
                String genStr = getCellStringValue(row.getCell(3));
                if (!genStr.isEmpty()) {
                    try {
                        member.setGeneration(Integer.parseInt(genStr));
                    } catch (NumberFormatException ignored) {
                    }
                }

                // 房支
                member.setBranch(getCellStringValue(row.getCell(4)));

                // 父节点ID
                String parentIdStr = getCellStringValue(row.getCell(5));
                if (!parentIdStr.isEmpty()) {
                    try {
                        member.setParentId(Long.parseLong(parentIdStr));
                    } catch (NumberFormatException ignored) {
                    }
                }

                // 配偶ID
                String spouseIdStr = getCellStringValue(row.getCell(6));
                if (!spouseIdStr.isEmpty()) {
                    try {
                        member.setSpouseId(Long.parseLong(spouseIdStr));
                    } catch (NumberFormatException ignored) {
                    }
                }

                // 出生日期
                member.setBirthDate(parseDate(getCellStringValue(row.getCell(7))));

                // 逝世日期
                member.setDeathDate(parseDate(getCellStringValue(row.getCell(8))));

                // 出生地
                member.setBirthPlace(getCellStringValue(row.getCell(9)));

                // 简介
                member.setBiography(getCellStringValue(row.getCell(10)));

                // 排序
                String sortStr = getCellStringValue(row.getCell(11));
                if (!sortStr.isEmpty()) {
                    try {
                        member.setSortOrder(Integer.parseInt(sortStr));
                    } catch (NumberFormatException ignored) {
                        member.setSortOrder(0);
                    }
                } else {
                    member.setSortOrder(0);
                }

                members.add(member);
            }
        }

        return members;
    }

    /**
     * 导出 CSV 格式
     */
    public static String exportMembersToCsv(List<FamilyMember> members) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", EXPORT_HEADERS)).append("\n");

        for (FamilyMember member : members) {
            sb.append(escapeCsv(member.getId() != null ? member.getId().toString() : "")).append(",");
            sb.append(escapeCsv(member.getName() != null ? member.getName() : "")).append(",");
            sb.append(escapeCsv("M".equals(member.getGender()) ? "男" : "女")).append(",");
            sb.append(escapeCsv(member.getGeneration() != null ? member.getGeneration().toString() : "")).append(",");
            sb.append(escapeCsv(member.getBranch() != null ? member.getBranch() : "")).append(",");
            sb.append(escapeCsv(member.getParentId() != null ? member.getParentId().toString() : "")).append(",");
            sb.append(escapeCsv(member.getSpouseId() != null ? member.getSpouseId().toString() : "")).append(",");
            sb.append(escapeCsv(member.getBirthDate() != null ? member.getBirthDate().format(DATE_FORMATTER) : "")).append(",");
            sb.append(escapeCsv(member.getDeathDate() != null ? member.getDeathDate().format(DATE_FORMATTER) : "")).append(",");
            sb.append(escapeCsv(member.getBirthPlace() != null ? member.getBirthPlace() : "")).append(",");
            sb.append(escapeCsv(member.getBiography() != null ? member.getBiography() : "")).append(",");
            sb.append(escapeCsv(member.getSortOrder() != null ? member.getSortOrder().toString() : "0"));
            sb.append("\n");
        }

        return sb.toString();
    }

    private static void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().format(DATE_FORMATTER);
                }
                double numVal = cell.getNumericCellValue();
                if (numVal == Math.floor(numVal)) {
                    return String.valueOf((long) numVal);
                }
                return String.valueOf(numVal);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private static boolean isRowEmpty(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellStringValue(cell);
                if (!value.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String parseGender(String genderStr) {
        if (genderStr == null || genderStr.isEmpty()) {
            return "M";
        }
        genderStr = genderStr.trim();
        if ("女".equals(genderStr) || "F".equalsIgnoreCase(genderStr) || "female".equalsIgnoreCase(genderStr)) {
            return "F";
        }
        return "M";
    }

    private static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            // 尝试其他格式
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
