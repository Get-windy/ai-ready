package cn.aiedge.export.template;

import cn.aiedge.export.template.ImportTemplateDefinition.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 导入模板生成器
 * 生成带有数据验证和示例数据的Excel模板
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImportTemplateGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 样式颜色
    private static final short HEADER_BG_COLOR = IndexedColors.LIGHT_BLUE.getIndex();
    private static final short REQUIRED_BG_COLOR = IndexedColors.PALE_BLUE.getIndex();
    private static final short SAMPLE_BG_COLOR = IndexedColors.LIGHT_YELLOW.getIndex();

    /**
     * 生成Excel模板文件
     *
     * @param template 模板定义
     * @return Excel文件字节数组
     */
    public byte[] generateTemplate(ImportTemplateDefinition template) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("导入数据");

            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle requiredStyle = createRequiredStyle(workbook);
            CellStyle sampleStyle = createSampleStyle(workbook);

            List<TemplateField> fields = template.getFields();

            // 1. 创建标题行
            Row headerRow = sheet.createRow(0);
            for (TemplateField field : fields) {
                Cell cell = headerRow.createCell(field.getOrder() - 1);
                String headerText = field.getFieldTitle();
                if (field.isRequired()) {
                    headerText += " *";
                }
                cell.setCellValue(headerText);
                cell.setCellStyle(headerStyle);
            }

            // 2. 创建字段说明行
            Row descRow = sheet.createRow(1);
            for (TemplateField field : fields) {
                Cell cell = descRow.createCell(field.getOrder() - 1);
                cell.setCellValue(getFieldDescription(field));
                CellStyle descStyle = workbook.createCellStyle();
                Font descFont = workbook.createFont();
                descFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
                descFont.setFontHeightInPoints((short) 9);
                descStyle.setFont(descFont);
                cell.setCellStyle(descStyle);
            }

            // 3. 添加示例数据
            int sampleRows = Math.min(template.getSampleRowCount(), 3);
            for (int i = 0; i < sampleRows; i++) {
                Row sampleRow = sheet.createRow(2 + i);
                for (TemplateField field : fields) {
                    Cell cell = sampleRow.createCell(field.getOrder() - 1);
                    setSampleValue(cell, field);
                    cell.setCellStyle(sampleStyle);
                }
            }

            // 4. 设置数据验证（下拉列表、数值范围等）
            int firstDataRow = 2 + sampleRows;
            int lastDataRow = firstDataRow + template.getMaxImportRows();
            for (TemplateField field : fields) {
                addDataValidation(sheet, field, firstDataRow, lastDataRow);
            }

            // 5. 设置列宽
            for (TemplateField field : fields) {
                int columnIndex = field.getOrder() - 1;
                int width = calculateColumnWidth(field);
                sheet.setColumnWidth(columnIndex, width);
            }

            // 6. 冻结首行
            sheet.createFreezePane(0, 1);

            // 7. 创建说明Sheet
            createInstructionSheet(workbook, template);

            // 输出
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 生成带错误信息的导入结果文件
     */
    public byte[] generateErrorFile(List<Map<String, Object>> data, 
                                     List<ValidationError> errors,
                                     ImportTemplateDefinition template) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("导入结果");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle errorStyle = createErrorStyle(workbook);

            List<TemplateField> fields = template.getFields();

            // 标题行
            Row headerRow = sheet.createRow(0);
            int colIndex = 0;
            for (TemplateField field : fields) {
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(field.getFieldTitle());
                cell.setCellStyle(headerStyle);
            }
            // 错误信息列
            Cell errorHeaderCell = headerRow.createCell(colIndex);
            errorHeaderCell.setCellValue("错误信息");
            errorHeaderCell.setCellStyle(headerStyle);

            // 数据行
            for (int i = 0; i < data.size(); i++) {
                Row dataRow = sheet.createRow(i + 1);
                Map<String, Object> rowData = data.get(i);
                
                colIndex = 0;
                for (TemplateField field : fields) {
                    Cell cell = dataRow.createCell(colIndex++);
                    Object value = rowData.get(field.getFieldName());
                    setCellValue(cell, value, field);
                }
                
                // 错误信息
                ValidationError rowError = findError(errors, i + 1);
                if (rowError != null) {
                    Cell errorCell = dataRow.createCell(colIndex);
                    errorCell.setCellValue(rowError.getMessage());
                    errorCell.setCellStyle(errorStyle);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ==================== 私有方法 ====================

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(HEADER_BG_COLOR);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        
        return style;
    }

    private CellStyle createRequiredStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(REQUIRED_BG_COLOR);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createSampleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(SAMPLE_BG_COLOR);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        
        return style;
    }

    private CellStyle createErrorStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.RED.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        
        return style;
    }

    private String getFieldDescription(TemplateField field) {
        StringBuilder desc = new StringBuilder();
        
        desc.append(field.getFieldType().name().toLowerCase());
        
        if (field.isRequired()) {
            desc.append(" | 必填");
        }
        
        if (field.getMaxLength() != null) {
            desc.append(" | 最大").append(field.getMaxLength()).append("字符");
        }
        
        if (field.getMinValue() != null || field.getMaxValue() != null) {
            desc.append(" | 范围:");
            if (field.getMinValue() != null) {
                desc.append(field.getMinValue());
            }
            desc.append("-");
            if (field.getMaxValue() != null) {
                desc.append(field.getMaxValue());
            }
        }
        
        return desc.toString();
    }

    private void setSampleValue(Cell cell, TemplateField field) {
        String sampleValue = field.getSampleValue();
        if (sampleValue == null || sampleValue.isEmpty()) {
            sampleValue = generateSampleValue(field);
        }
        
        switch (field.getFieldType()) {
            case INTEGER:
                try {
                    cell.setCellValue(Integer.parseInt(sampleValue));
                } catch (NumberFormatException e) {
                    cell.setCellValue(sampleValue);
                }
                break;
            case DECIMAL:
                try {
                    cell.setCellValue(Double.parseDouble(sampleValue));
                } catch (NumberFormatException e) {
                    cell.setCellValue(sampleValue);
                }
                break;
            case BOOLEAN:
                cell.setCellValue(Boolean.parseBoolean(sampleValue));
                break;
            default:
                cell.setCellValue(sampleValue);
        }
    }

    private String generateSampleValue(TemplateField field) {
        return switch (field.getFieldType()) {
            case STRING -> "示例文本";
            case INTEGER -> "100";
            case DECIMAL -> "99.99";
            case DATE -> LocalDate.now().format(DATE_FORMATTER);
            case DATETIME -> LocalDateTime.now().format(DATETIME_FORMATTER);
            case BOOLEAN -> "是";
            case EMAIL -> "example@test.com";
            case PHONE -> "13800138000";
            case ID_CARD -> "110101199001011234";
            case URL -> "https://example.com";
            default -> "";
        };
    }

    private void setCellValue(Cell cell, Object value, TemplateField field) {
        if (value == null) {
            return;
        }
        
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime.format(DATETIME_FORMATTER));
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(date.format(DATE_FORMATTER));
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private void addDataValidation(XSSFSheet sheet, TemplateField field, 
                                    int firstRow, int lastRow) {
        int columnIndex = field.getOrder() - 1;
        XSSFDataValidationHelper helper = new XSSFDataValidationHelper(sheet);
        
        // 下拉列表验证
        if (field.getDropdownOptions() != null && !field.getDropdownOptions().isEmpty()) {
            String[] options = field.getDropdownOptions().values().toArray(new String[0]);
            String formula = "\"" + String.join(",", options) + "\"";
            
            CellRangeAddressList addressList = new CellRangeAddressList(
                firstRow, lastRow, columnIndex, columnIndex);
            
            DataValidationConstraint constraint = helper.createFormulaListConstraint(formula);
            DataValidation validation = helper.createValidation(constraint, addressList);
            validation.setShowErrorBox(true);
            validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
            validation.createErrorBox("输入错误", "请从下拉列表中选择有效选项");
            
            sheet.addValidationData(validation);
        }
        
        // 数值范围验证
        if (field.getFieldType() == FieldType.INTEGER || field.getFieldType() == FieldType.DECIMAL) {
            CellRangeAddressList addressList = new CellRangeAddressList(
                firstRow, lastRow, columnIndex, columnIndex);
            
            DataValidationConstraint constraint;
            if (field.getMinValue() != null && field.getMaxValue() != null) {
                constraint = helper.createNumericConstraint(
                    DataValidationConstraint.ValidationType.DECIMAL,
                    DataValidationConstraint.OperatorType.BETWEEN,
                    String.valueOf(field.getMinValue()),
                    String.valueOf(field.getMaxValue()));
            } else if (field.getMinValue() != null) {
                constraint = helper.createNumericConstraint(
                    DataValidationConstraint.ValidationType.DECIMAL,
                    DataValidationConstraint.OperatorType.GREATER_OR_EQUAL,
                    String.valueOf(field.getMinValue()),
                    null);
            } else if (field.getMaxValue() != null) {
                constraint = helper.createNumericConstraint(
                    DataValidationConstraint.ValidationType.DECIMAL,
                    DataValidationConstraint.OperatorType.LESS_OR_EQUAL,
                    null,
                    String.valueOf(field.getMaxValue()));
            } else {
                constraint = helper.createNumericConstraint(
                    DataValidationConstraint.ValidationType.DECIMAL,
                    DataValidationConstraint.OperatorType.IGNORE,
                    null, null);
            }
            
            DataValidation validation = helper.createValidation(constraint, addressList);
            validation.setShowErrorBox(true);
            validation.createErrorBox("数值范围错误", "请输入有效范围内的数值");
            sheet.addValidationData(validation);
        }
        
        // 文本长度验证
        if (field.getMaxLength() != null && field.getFieldType() == FieldType.STRING) {
            CellRangeAddressList addressList = new CellRangeAddressList(
                firstRow, lastRow, columnIndex, columnIndex);
            
            DataValidationConstraint constraint = helper.createTextLengthConstraint(
                DataValidationConstraint.OperatorType.LESS_OR_EQUAL,
                String.valueOf(field.getMaxLength()),
                null);
            
            DataValidation validation = helper.createValidation(constraint, addressList);
            validation.setShowErrorBox(true);
            validation.createErrorBox("文本过长", "文本长度不能超过" + field.getMaxLength() + "个字符");
            sheet.addValidationData(validation);
        }
    }

    private int calculateColumnWidth(TemplateField field) {
        int baseWidth = 256 * 12; // 12个字符宽度
        
        if (field.getFieldTitle() != null) {
            baseWidth = Math.max(baseWidth, 256 * (field.getFieldTitle().length() + 2));
        }
        
        if (field.getFieldType() == FieldType.STRING && field.getMaxLength() != null) {
            baseWidth = Math.max(baseWidth, 256 * Math.min(field.getMaxLength(), 50));
        }
        
        return baseWidth;
    }

    private void createInstructionSheet(Workbook workbook, ImportTemplateDefinition template) {
        Sheet sheet = workbook.createSheet("填写说明");
        
        int rowNum = 0;
        
        // 标题
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(template.getTemplateName() + " - 填写说明");
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleCell.getCellStyle().setFont(titleFont);
        
        rowNum++; // 空行
        
        // 描述
        Row descRow = sheet.createRow(rowNum++);
        descRow.createCell(0).setCellValue("说明：" + template.getDescription());
        
        rowNum++; // 空行
        
        // 字段说明
        Row fieldHeaderRow = sheet.createRow(rowNum++);
        fieldHeaderRow.createCell(0).setCellValue("字段名称");
        fieldHeaderRow.createCell(1).setCellValue("字段类型");
        fieldHeaderRow.createCell(2).setCellValue("是否必填");
        fieldHeaderRow.createCell(3).setCellValue("说明");
        
        for (TemplateField field : template.getFields()) {
            Row fieldRow = sheet.createRow(rowNum++);
            fieldRow.createCell(0).setCellValue(field.getFieldTitle());
            fieldRow.createCell(1).setCellValue(field.getFieldType().name());
            fieldRow.createCell(2).setCellValue(field.isRequired() ? "是" : "否");
            fieldRow.createCell(3).setCellValue(field.getDescription() != null ? field.getDescription() : "");
        }
        
        rowNum++; // 空行
        
        // 注意事项
        Row noteRow = sheet.createRow(rowNum++);
        noteRow.createCell(0).setCellValue("注意事项：");
        
        Row note1 = sheet.createRow(rowNum++);
        note1.createCell(0).setCellValue("1. 带 * 号的字段为必填项");
        
        Row note2 = sheet.createRow(rowNum++);
        note2.createCell(0).setCellValue("2. 日期格式：yyyy-MM-dd");
        
        Row note3 = sheet.createRow(rowNum++);
        note3.createCell(0).setCellValue("3. 最大导入行数：" + template.getMaxImportRows());
        
        // 设置列宽
        sheet.setColumnWidth(0, 256 * 20);
        sheet.setColumnWidth(1, 256 * 15);
        sheet.setColumnWidth(2, 256 * 12);
        sheet.setColumnWidth(3, 256 * 40);
    }

    private ValidationError findError(List<ValidationError> errors, int rowNum) {
        if (errors == null) return null;
        return errors.stream()
            .filter(e -> e.getRowNum() == rowNum)
            .findFirst()
            .orElse(null);
    }

    /**
     * 校验错误
     */
    public record ValidationError(int rowNum, String fieldName, String message) {
        public static ValidationError of(int rowNum, String fieldName, String message) {
            return new ValidationError(rowNum, fieldName, message);
        }
    }
}
