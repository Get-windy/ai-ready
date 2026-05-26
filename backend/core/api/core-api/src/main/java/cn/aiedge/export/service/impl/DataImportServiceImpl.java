package cn.aiedge.export.service.impl;

import cn.aiedge.export.service.DataImportService;
import cn.aiedge.cache.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 数据导入服务实现
 * 
 * 支持Excel和CSV格式的数据导入
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportServiceImpl implements DataImportService {

    private final CacheService cacheService;

    // 数据类型字段配置缓存Key
    private static final String FIELD_CONFIG_KEY = "import:fields:";

    // 字段定义配置
    private static final Map<String, List<FieldDefinition>> FIELD_DEFINITIONS = new HashMap<>();

    static {
        // 客户导入字段定义
        FIELD_DEFINITIONS.put("customer", List.of(
            new FieldDefinition("customerName", "客户名称", "string", true, 100, null, "客户全称", null),
            new FieldDefinition("customerCode", "客户编码", "string", false, 50, "^[A-Z0-9]+$", "唯一编码，大写字母和数字", null),
            new FieldDefinition("contact", "联系人", "string", false, 50, null, "主要联系人姓名", null),
            new FieldDefinition("phone", "联系电话", "string", false, 20, "^1[3-9]\\d{9}$", "手机号码", null),
            new FieldDefinition("email", "邮箱", "string", false, 100, "^[\\w.-]+@[\\w.-]+\\.\\w+$", "电子邮箱", null),
            new FieldDefinition("address", "地址", "string", false, 200, null, "详细地址", null),
            new FieldDefinition("remark", "备注", "string", false, 500, null, "备注信息", null)
        ));

        // 产品导入字段定义
        FIELD_DEFINITIONS.put("product", List.of(
            new FieldDefinition("productName", "产品名称", "string", true, 100, null, "产品名称", null),
            new FieldDefinition("productCode", "产品编码", "string", true, 50, "^[A-Z0-9]+$", "唯一编码", null),
            new FieldDefinition("category", "分类", "string", false, 50, null, "产品分类", null),
            new FieldDefinition("price", "价格", "decimal", true, 0, "^\\d+(\\.\\d{1,2})?$", "销售价格", null),
            new FieldDefinition("stock", "库存", "integer", false, 0, "^\\d+$", "库存数量", null),
            new FieldDefinition("unit", "单位", "string", false, 20, null, "计量单位", 
                List.of(new FieldValue("件", "件"), new FieldValue("个", "个"), new FieldValue("台", "台"))),
            new FieldDefinition("status", "状态", "string", false, 20, null, "产品状态",
                List.of(new FieldValue("active", "在售"), new FieldValue("inactive", "停售")))
        ));

        // 订单导入字段定义
        FIELD_DEFINITIONS.put("order", List.of(
            new FieldDefinition("orderNo", "订单号", "string", true, 50, null, "订单编号", null),
            new FieldDefinition("customerName", "客户名称", "string", true, 100, null, "客户名称", null),
            new FieldDefinition("productName", "产品名称", "string", true, 100, null, "产品名称", null),
            new FieldDefinition("quantity", "数量", "integer", true, 0, "^\\d+$", "订购数量", null),
            new FieldDefinition("price", "单价", "decimal", true, 0, "^\\d+(\\.\\d{1,2})?$", "销售单价", null),
            new FieldDefinition("orderDate", "订单日期", "date", true, 0, "^\\d{4}-\\d{2}-\\d{2}$", "订单日期", null)
        ));

        // 用户导入字段定义
        FIELD_DEFINITIONS.put("user", List.of(
            new FieldDefinition("username", "用户名", "string", true, 50, "^[a-zA-Z][a-zA-Z0-9_]{2,}$", "登录用户名", null),
            new FieldDefinition("realName", "真实姓名", "string", true, 50, null, "用户真实姓名", null),
            new FieldDefinition("email", "邮箱", "string", true, 100, "^[\\w.-]+@[\\w.-]+\\.\\w+$", "电子邮箱", null),
            new FieldDefinition("phone", "手机号", "string", false, 20, "^1[3-9]\\d{9}$", "手机号码", null),
            new FieldDefinition("deptName", "部门", "string", false, 100, null, "所属部门", null),
            new FieldDefinition("role", "角色", "string", false, 50, null, "用户角色",
                List.of(new FieldValue("admin", "管理员"), new FieldValue("user", "普通用户")))
        ));
    }

    @Override
    public ImportResult importExcel(InputStream inputStream, String dataType, Long tenantId) {
        log.info("开始Excel导入: dataType={}, tenantId={}", dataType, tenantId);
        
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            List<FieldDefinition> fields = getFieldDefinitions(dataType);
            if (fields.isEmpty()) {
                return ImportResult.failure("不支持的数据类型: " + dataType);
            }

            // 读取表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return ImportResult.failure("文件格式错误：缺少表头行");
            }

            Map<Integer, String> headerMapping = parseHeaders(headerRow, fields);
            if (headerMapping.isEmpty()) {
                return ImportResult.failure("文件格式错误：无法识别表头");
            }

            // 读取数据行
            List<Map<String, Object>> dataList = new ArrayList<>();
            List<ImportError> errors = new ArrayList<>();
            int totalRows = sheet.getLastRowNum();

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, Object> rowData = new HashMap<>();
                boolean rowValid = true;

                for (Map.Entry<Integer, String> entry : headerMapping.entrySet()) {
                    Cell cell = row.getCell(entry.getKey());
                    Object value = getCellValue(cell);
                    rowData.put(entry.getValue(), value);
                }

                // 基本校验
                for (FieldDefinition field : fields) {
                    Object value = rowData.get(field.field());
                    if (field.required() && (value == null || value.toString().isEmpty())) {
                        errors.add(new ImportError(i + 1, field.field(), 
                            value != null ? value.toString() : "", 
                            field.label() + "不能为空"));
                        rowValid = false;
                    }
                }

                if (rowValid) {
                    dataList.add(rowData);
                }
            }

            // 详细校验
            ValidateResult validateResult = validate(dataList, dataType);
            
            int successCount = validateResult.validRows();
            int failureCount = validateResult.invalidRows() + errors.size();

            if (failureCount > 0) {
                for (ValidateError ve : validateResult.errors()) {
                    errors.add(new ImportError(ve.rowIndex(), ve.field(), ve.value(), ve.message()));
                }
                return ImportResult.partial(totalRows, successCount, failureCount, errors);
            }

            log.info("Excel导入完成: 成功{}条, 失败{}条", successCount, failureCount);
            return ImportResult.success(totalRows, successCount);

        } catch (Exception e) {
            log.error("Excel导入失败", e);
            return ImportResult.failure("导入失败: " + e.getMessage());
        }
    }

    @Override
    public ImportResult importCsv(InputStream inputStream, String dataType, Long tenantId) {
        log.info("开始CSV导入: dataType={}, tenantId={}", dataType, tenantId);
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withTrim()
                .parse(reader)) {

            List<FieldDefinition> fields = getFieldDefinitions(dataType);
            if (fields.isEmpty()) {
                return ImportResult.failure("不支持的数据类型: " + dataType);
            }

            // 获取CSV表头
            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            if (headerMap.isEmpty()) {
                return ImportResult.failure("文件格式错误：缺少表头");
            }

            // 字段映射
            Map<String, String> fieldMapping = new HashMap<>();
            for (FieldDefinition field : fields) {
                String label = field.label();
                if (headerMap.containsKey(label)) {
                    fieldMapping.put(label, field.field());
                } else if (headerMap.containsKey(field.field())) {
                    fieldMapping.put(field.field(), field.field());
                }
            }

            List<Map<String, Object>> dataList = new ArrayList<>();
            List<ImportError> errors = new ArrayList<>();
            int rowIndex = 1;

            for (CSVRecord record : csvParser) {
                rowIndex++;
                Map<String, Object> rowData = new HashMap<>();
                boolean rowValid = true;

                for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                    String headerName = entry.getKey();
                    String fieldName = fieldMapping.getOrDefault(headerName, headerName);
                    String value = record.get(entry.getValue());
                    rowData.put(fieldName, value);
                }

                // 基本校验
                for (FieldDefinition field : fields) {
                    Object value = rowData.get(field.field());
                    if (field.required() && (value == null || value.toString().isEmpty())) {
                        errors.add(new ImportError(rowIndex, field.field(), 
                            value != null ? value.toString() : "", 
                            field.label() + "不能为空"));
                        rowValid = false;
                    }
                }

                if (rowValid) {
                    dataList.add(rowData);
                }
            }

            // 详细校验
            ValidateResult validateResult = validate(dataList, dataType);
            
            int successCount = validateResult.validRows();
            int failureCount = validateResult.invalidRows() + errors.size();

            if (failureCount > 0) {
                for (ValidateError ve : validateResult.errors()) {
                    errors.add(new ImportError(ve.rowIndex(), ve.field(), ve.value(), ve.message()));
                }
                return ImportResult.partial(rowIndex - 1, successCount, failureCount, errors);
            }

            log.info("CSV导入完成: 成功{}条, 失败{}条", successCount, failureCount);
            return ImportResult.success(rowIndex - 1, successCount);

        } catch (Exception e) {
            log.error("CSV导入失败", e);
            return ImportResult.failure("导入失败: " + e.getMessage());
        }
    }

    @Override
    public PreviewResult preview(InputStream inputStream, String dataType, int maxRows) {
        log.info("预览导入数据: dataType={}, maxRows={}", dataType, maxRows);
        
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // 读取表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return new PreviewResult(List.of(), List.of(), 0, 0);
            }

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValue(cell) != null ? cell.toString() : "");
            }

            // 读取预览数据
            List<Map<String, Object>> rows = new ArrayList<>();
            int totalRows = sheet.getLastRowNum();
            int previewRows = Math.min(maxRows, totalRows);

            for (int i = 1; i <= previewRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, Object> rowData = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    rowData.put(headers.get(j), getCellValue(cell));
                }
                rows.add(rowData);
            }

            return new PreviewResult(headers, rows, totalRows, previewRows);

        } catch (Exception e) {
            log.error("预览数据失败", e);
            return new PreviewResult(List.of(), List.of(), 0, 0);
        }
    }

    @Override
    public ValidateResult validate(List<Map<String, Object>> dataList, String dataType) {
        if (dataList == null || dataList.isEmpty()) {
            return ValidateResult.valid(0);
        }

        List<FieldDefinition> fields = getFieldDefinitions(dataType);
        List<ValidateError> errors = new ArrayList<>();
        int validRows = 0;
        int invalidRows = 0;

        for (int i = 0; i < dataList.size(); i++) {
            Map<String, Object> rowData = dataList.get(i);
            int rowIndex = i + 2; // Excel行号从2开始（跳过表头）
            boolean rowValid = true;

            for (FieldDefinition field : fields) {
                Object value = rowData.get(field.field());
                String strValue = value != null ? value.toString() : "";

                // 必填校验
                if (field.required() && strValue.isEmpty()) {
                    errors.add(new ValidateError(rowIndex, field.field(), strValue, 
                        "required", field.label() + "不能为空"));
                    rowValid = false;
                    continue;
                }

                // 长度校验
                if (!strValue.isEmpty() && field.maxLength() > 0 && 
                    strValue.length() > field.maxLength()) {
                    errors.add(new ValidateError(rowIndex, field.field(), strValue,
                        "maxLength", field.label() + "长度不能超过" + field.maxLength()));
                    rowValid = false;
                }

                // 格式校验
                if (!strValue.isEmpty() && field.pattern() != null && 
                    !Pattern.matches(field.pattern(), strValue)) {
                    errors.add(new ValidateError(rowIndex, field.field(), strValue,
                        "pattern", field.label() + "格式不正确"));
                    rowValid = false;
                }

                // 枚举值校验
                if (!strValue.isEmpty() && field.options() != null && !field.options().isEmpty()) {
                    boolean validOption = field.options().stream()
                        .anyMatch(opt -> opt.value().equals(strValue) || opt.label().equals(strValue));
                    if (!validOption) {
                        errors.add(new ValidateError(rowIndex, field.field(), strValue,
                            "options", field.label() + "值不在可选范围内"));
                        rowValid = false;
                    }
                }
            }

            if (rowValid) {
                validRows++;
            } else {
                invalidRows++;
            }
        }

        if (invalidRows > 0) {
            return ValidateResult.invalid(dataList.size(), validRows, invalidRows, errors);
        }
        return ValidateResult.valid(dataList.size());
    }

    @Override
    public List<FieldDefinition> getFieldDefinitions(String dataType) {
        return FIELD_DEFINITIONS.getOrDefault(dataType, List.of());
    }

    // ==================== 辅助方法 ====================

    /**
     * 解析表头并映射到字段
     */
    private Map<Integer, String> parseHeaders(Row headerRow, List<FieldDefinition> fields) {
        Map<Integer, String> mapping = new HashMap<>();
        Map<String, String> labelToField = new HashMap<>();
        
        for (FieldDefinition field : fields) {
            labelToField.put(field.label(), field.field());
            labelToField.put(field.field(), field.field());
        }

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            String headerName = cell != null ? cell.toString().trim() : "";
            
            String fieldName = labelToField.get(headerName);
            if (fieldName != null) {
                mapping.put(i, fieldName);
            }
        }

        return mapping;
    }

    /**
     * 获取单元格值
     */
    private Object getCellValue(Cell cell) {
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                double num = cell.getNumericCellValue();
                if (num == (long) num) {
                    yield (long) num;
                }
                yield num;
            }
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }
}
