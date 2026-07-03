package cn.aiedge.integration.model;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 同步字段映射配置
 *
 * 定义外部系统字段与本地系统字段的对应关系，支持转换规则。
 */
@Data
@Accessors(chain = true)
@TableName("sync_field_mapping")
public class SyncFieldMapping {

    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 租户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tenantId;

    /** 关联的同步配置ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceConfigId;

    /** 单据类型编码（601=客户, 604=供应商 等） */
    private String billType;

    /** 外部系统字段名 */
    private String sourceField;

    /** 外部系统字段显示名 */
    private String sourceLabel;

    /** 本地系统字段名 */
    private String targetField;

    /** 本地系统字段显示名 */
    private String targetLabel;

    /** 转换类型: direct=直接映射, enum=枚举转换, formula=公式, default=默认值 */
    private String transformType;

    /** 转换规则（JSON 或表达式） */
    private String transformRule;

    /** 默认值 */
    private String defaultValue;

    /** 是否必填 */
    private Boolean required;

    /** 排序 */
    private Integer sortOrder;

    /** 状态: 1=启用, 0=禁用 */
    private Integer status;

    /** 逻辑删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
