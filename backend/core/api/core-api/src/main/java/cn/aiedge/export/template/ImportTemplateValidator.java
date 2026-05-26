package cn.aiedge.export.template;

import cn.aiedge.export.template.ImportTemplateDefinition.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 导入模板校验器
 * 根据模板定义校验导入数据
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImportTemplateValidator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("yyyy.MM.dd"),
        DateTimeFormatter.ofPattern("yyyyMMdd")
    };
    
    private static final DateTimeFormatter[] DATETIME_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyyMMdd HHmmss")
    };
    
    // 常用正则表达式
    private static final Map<String, Pattern> COMMON_PATTERNS = Map.of(
        "email", Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"),
        "phone", Pattern.compile("^1[3-9]\\d{9}$"),
        "idCard", Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$"),
        "url", Pattern.compile("^(https?://)?[\\w.-]+\\.[a-zA-Z]{2,}(/\\S*)?$")
    );

    /**
     * 校验数据
     *
     * @param template 模板定义
     * @param data 数据列表
     * @return 校验结果
     */
    public ValidationResult validate(ImportTemplateDefinition template, 
                                      List<Map<String, Object>> data) {
        List<FieldError> errors = new ArrayList<>();
        Map<String, Set<Object>> uniqueValues = new HashMap<>();
        
        // 初始化唯一值追踪
        for (TemplateField field : template.getFields()) {
            if (field.isUnique()) {
                uniqueValues.put(field.getFieldName(), new HashSet<>());
            }
        }
        
        // 逐行校验
        for (int i = 0; i < data.size(); i++) {
            int rowNum = i + 2; // Excel行号从2开始（第1行是标题）
            Map<String, Object> rowData = data.get(i);
            
            for (TemplateField field : template.getFields()) {
                String fieldName = field.getFieldName();
                Object value = rowData.get(fieldName);
                
                // 必填校验
                if (field.isRequired() && isEmpty(value)) {
                    errors.add(new FieldError(rowNum, fieldName, "必填字段不能为空"));
                    continue;
                }
                
                // 空值跳过后续校验
                if (isEmpty(value)) {
                    continue;
                }
                
                String strValue = String.valueOf(value);
                
                // 类型校验
                FieldError typeError = validateType(field, strValue, rowNum);
                if (typeError != null) {
                    errors.add(typeError);
                    continue;
                }
                
                // 长度校验
                if (field.getMaxLength() != null && strValue.length() > field.getMaxLength()) {
                    errors.add(new FieldError(rowNum, fieldName, 
                        "长度超过最大限制" + field.getMaxLength()));
                }
                
                // 数值范围校验
                if (field.getFieldType() == FieldType.INTEGER || 
                    field.getFieldType() == FieldType.DECIMAL) {
                    FieldError rangeError = validateRange(field, strValue, rowNum);
                    if (rangeError != null) {
                        errors.add(rangeError);
                    }
                }
                
                // 正则表达式校验
                if (field.getRegexPattern() != null) {
                    FieldError regexError = validateRegex(field, strValue, rowNum);
                    if (regexError != null) {
                        errors.add(regexError);
                    }
                }
                
                // 枚举值校验
                if (field.getFieldType() == FieldType.ENUM || 
                    field.getDropdownOptions() != null) {
                    FieldError enumError = validateEnum(field, strValue, rowNum);
                    if (enumError != null) {
                        errors.add(enumError);
                    }
                }
                
                // 特殊类型校验
                FieldError specialError = validateSpecialType(field, strValue, rowNum);
                if (specialError != null) {
                    errors.add(specialError);
                }
                
                // 唯一性校验
                if (field.isUnique()) {
                    Set<Object> seen = uniqueValues.get(fieldName);
                    if (seen.contains(strValue)) {
                        errors.add(new FieldError(rowNum, fieldName, 
                            "值重复：" + strValue));
                    } else {
                        seen.add(strValue);
                    }
                }
            }
        }
        
        // 检查数据量
        if (data.size() > template.getMaxImportRows()) {
            errors.add(0, new FieldError(0, "_system", 
                "数据量超过最大限制：" + template.getMaxImportRows()));
        }
        
        return new ValidationResult(errors.isEmpty(), errors, data.size(), errors.size());
    }

    /**
     * 校验单行数据
     */
    public RowValidationResult validateRow(ImportTemplateDefinition template,
                                            Map<String, Object> rowData, int rowNum) {
        List<FieldError> errors = new ArrayList<>();
        
        for (TemplateField field : template.getFields()) {
            String fieldName = field.getFieldName();
            Object value = rowData.get(fieldName);
            
            FieldError error = validateField(field, value, rowNum);
            if (error != null) {
                errors.add(error);
            }
        }
        
        return new RowValidationResult(errors.isEmpty(), errors, rowNum);
    }

    /**
     * 校验单个字段
     */
    public FieldError validateField(TemplateField field, Object value, int rowNum) {
        String fieldName = field.getFieldName();
        
        // 必填校验
        if (field.isRequired() && isEmpty(value)) {
            return new FieldError(rowNum, fieldName, "必填字段不能为空");
        }
        
        if (isEmpty(value)) {
            return null;
        }
        
        String strValue = String.valueOf(value);
        
        // 类型校验
        FieldError typeError = validateType(field, strValue, rowNum);
        if (typeError != null) return typeError;
        
        // 长度校验
        if (field.getMaxLength() != null && strValue.length() > field.getMaxLength()) {
            return new FieldError(rowNum, fieldName, "长度超过最大限制" + field.getMaxLength());
        }
        
        // 数值范围校验
        if (field.getFieldType() == FieldType.INTEGER || 
            field.getFieldType() == FieldType.DECIMAL) {
            FieldError rangeError = validateRange(field, strValue, rowNum);
            if (rangeError != null) return rangeError;
        }
        
        // 正则校验
        if (field.getRegexPattern() != null) {
            FieldError regexError = validateRegex(field, strValue, rowNum);
            if (regexError != null) return regexError;
        }
        
        // 枚举校验
        if (field.getDropdownOptions() != null && !field.getDropdownOptions().isEmpty()) {
            FieldError enumError = validateEnum(field, strValue, rowNum);
            if (enumError != null) return enumError;
        }
        
        // 特殊类型校验
        return validateSpecialType(field, strValue, rowNum);
    }

    // ==================== 私有方法 ====================

    private boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof String str) return str.trim().isEmpty();
        return false;
    }

    private FieldError validateType(TemplateField field, String value, int rowNum) {
        try {
            switch (field.getFieldType()) {
                case INTEGER:
                    Integer.parseInt(value);
                    break;
                case DECIMAL:
                    new BigDecimal(value);
                    break;
                case DATE:
                    parseDate(value);
                    break;
                case DATETIME:
                    parseDateTime(value);
                    break;
                case BOOLEAN:
                    parseBoolean(value);
                    break;
                default:
                    break;
            }
            return null;
        } catch (Exception e) {
            return new FieldError(rowNum, field.getFieldName(), 
                "类型错误：期望" + field.getFieldType().name() + "类型");
        }
    }

    private FieldError validateRange(TemplateField field, String value, int rowNum) {
        try {
            BigDecimal decimal = new BigDecimal(value);
            
            if (field.getMinValue() != null && decimal.compareTo(BigDecimal.valueOf(field.getMinValue())) < 0) {
                return new FieldError(rowNum, field.getFieldName(), 
                    "值小于最小值" + field.getMinValue());
            }
            
            if (field.getMaxValue() != null && decimal.compareTo(BigDecimal.valueOf(field.getMaxValue())) > 0) {
                return new FieldError(rowNum, field.getFieldName(), 
                    "值大于最大值" + field.getMaxValue());
            }
            
            return null;
        } catch (NumberFormatException e) {
            return new FieldError(rowNum, field.getFieldName(), "数值格式错误");
        }
    }

    private FieldError validateRegex(TemplateField field, String value, int rowNum) {
        Pattern pattern = Pattern.compile(field.getRegexPattern());
        if (!pattern.matcher(value).matches()) {
            return new FieldError(rowNum, field.getFieldName(), "格式不正确");
        }
        return null;
    }

    private FieldError validateEnum(TemplateField field, String value, int rowNum) {
        Map<String, String> options = field.getDropdownOptions();
        if (options == null || options.isEmpty()) {
            return null;
        }
        
        // 检查值是否在选项中
        boolean valid = options.containsKey(value) || options.containsValue(value);
        if (!valid) {
            return new FieldError(rowNum, field.getFieldName(), 
                "无效的选项值，有效值为：" + String.join(", ", options.values()));
        }
        return null;
    }

    private FieldError validateSpecialType(TemplateField field, String value, int rowNum) {
        Pattern pattern = null;
        String typeName = "";
        
        switch (field.getFieldType()) {
            case EMAIL:
                pattern = COMMON_PATTERNS.get("email");
                typeName = "邮箱";
                break;
            case PHONE:
                pattern = COMMON_PATTERNS.get("phone");
                typeName = "手机号";
                break;
            case ID_CARD:
                pattern = COMMON_PATTERNS.get("idCard");
                typeName = "身份证号";
                break;
            case URL:
                pattern = COMMON_PATTERNS.get("url");
                typeName = "网址";
                break;
            default:
                return null;
        }
        
        if (pattern != null && !pattern.matcher(value).matches()) {
            return new FieldError(rowNum, field.getFieldName(), 
                "无效的" + typeName + "格式");
        }
        
        return null;
    }

    private LocalDate parseDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("无法解析日期: " + value);
    }

    private LocalDateTime parseDateTime(String value) {
        for (DateTimeFormatter formatter : DATETIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("无法解析日期时间: " + value);
    }

    private boolean parseBoolean(String value) {
        return switch (value.toLowerCase()) {
            case "true", "是", "yes", "1", "y" -> true;
            case "false", "否", "no", "0", "n" -> false;
            default -> throw new IllegalArgumentException("无法解析布尔值: " + value);
        };
    }

    // ==================== DTO ====================

    /**
     * 校验结果
     */
    public record ValidationResult(
        boolean valid,
        List<FieldError> errors,
        int totalRows,
        int errorCount
    ) {
        public static ValidationResult success(int totalRows) {
            return new ValidationResult(true, Collections.emptyList(), totalRows, 0);
        }
        
        public static ValidationResult failure(List<FieldError> errors, int totalRows) {
            return new ValidationResult(false, errors, totalRows, errors.size());
        }
    }

    /**
     * 行校验结果
     */
    public record RowValidationResult(
        boolean valid,
        List<FieldError> errors,
        int rowNum
    ) {}

    /**
     * 字段错误
     */
    public record FieldError(
        int rowNum,
        String fieldName,
        String message
    ) {
        @Override
        public String toString() {
            return String.format("行%d, 字段'%s': %s", rowNum, fieldName, message);
        }
    }
}
