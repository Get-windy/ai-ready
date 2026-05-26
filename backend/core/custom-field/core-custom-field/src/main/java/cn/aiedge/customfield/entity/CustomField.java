package cn.aiedge.customfield.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_custom_field")
public class CustomField {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fieldCode;

    private String modelName;

    private String fieldName;

    private String fieldType;

    private String fieldLabel;

    private Boolean required;

    private Boolean readonly;

    private Boolean searchable;

    private Boolean sortable;

    private String defaultValue;

    private String selectionValues;

    private String validationRule;

    private String helpText;

    private String placeholder;

    private Integer sortOrder;

    private String groupCode;

    private String groupName;

    private Boolean active;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}