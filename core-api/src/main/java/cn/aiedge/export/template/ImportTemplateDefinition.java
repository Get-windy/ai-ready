package cn.aiedge.export.template;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 导入模板定义
 * 支持字段类型、校验规则、下拉选项、示例数据等
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportTemplateDefinition {

    /**
     * 模板ID
     */
    private String templateId;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 模板版本
     */
    private String version;

    /**
     * 字段列表
     */
    private List<TemplateField> fields;

    /**
     * 示例数据行数
     */
    private int sampleRowCount;

    /**
     * 最大导入行数
     */
    private int maxImportRows;

    /**
     * 是否启用严格校验
     */
    private boolean strictValidation;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 模板字段定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateField {
        
        /**
         * 字段名（英文）
         */
        private String fieldName;
        
        /**
         * 字段标题（中文）
         */
        private String fieldTitle;
        
        /**
         * 字段类型
         */
        private FieldType fieldType;
        
        /**
         * 是否必填
         */
        private boolean required;
        
        /**
         * 最大长度
         */
        private Integer maxLength;
        
        /**
         * 最小值（数值类型）
         */
        private Double minValue;
        
        /**
         * 最大值（数值类型）
         */
        private Double maxValue;
        
        /**
         * 正则表达式校验
         */
        private String regexPattern;
        
        /**
         * 下拉选项（Map<value, label>）
         */
        private Map<String, String> dropdownOptions;
        
        /**
         * 默认值
         */
        private String defaultValue;
        
        /**
         * 示例值
         */
        private String sampleValue;
        
        /**
         * 字段描述
         */
        private String description;
        
        /**
         * 列顺序
         */
        private int order;
        
        /**
         * 是否唯一
         */
        private boolean unique;
        
        /**
         * 引用数据类型（如部门ID引用dept表）
         */
        private String referenceType;
        
        /**
         * 自定义校验器类名
         */
        private String customValidator;
    }

    /**
     * 字段类型枚举
     */
    public enum FieldType {
        STRING,         // 字符串
        INTEGER,        // 整数
        DECIMAL,        // 小数
        DATE,           // 日期
        DATETIME,       // 日期时间
        BOOLEAN,        // 布尔
        ENUM,           // 枚举
        EMAIL,          // 邮箱
        PHONE,          // 手机号
        ID_CARD,        // 身份证
        URL,            // 网址
        REFERENCE       // 引用
    }

    /**
     * 创建模板副本
     */
    public ImportTemplateDefinition copy() {
        return ImportTemplateDefinition.builder()
                .templateId(this.templateId + "_copy")
                .dataType(this.dataType)
                .templateName(this.templateName + "（副本）")
                .description(this.description)
                .version(this.version)
                .fields(this.fields)
                .sampleRowCount(this.sampleRowCount)
                .maxImportRows(this.maxImportRows)
                .strictValidation(this.strictValidation)
                .createTime(LocalDateTime.now())
                .build();
    }
}
