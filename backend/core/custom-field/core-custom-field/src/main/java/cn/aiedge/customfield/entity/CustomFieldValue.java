package cn.aiedge.customfield.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_custom_field_value")
public class CustomFieldValue {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fieldId;

    private String modelName;

    private Long recordId;

    private String valueString;

    private Integer valueInteger;

    private Long valueLong;

    private Double valueDouble;

    private Boolean valueBoolean;

    private LocalDateTime valueDate;

    private String valueText;

    private String valueJson;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}