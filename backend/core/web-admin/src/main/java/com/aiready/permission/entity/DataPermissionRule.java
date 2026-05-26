package com.aiready.permission.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 数据权限规则实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_data_permission_rule")
public class DataPermissionRule {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 规则名称
     */
    private String ruleName;
    
    /**
     * 规则编码
     */
    private String ruleCode;
    
    /**
     * 数据表名
     */
    private String tableName;
    
    /**
     * 部门字段名
     */
    private String deptColumn;
    
    /**
     * 用户字段名
     */
    private String userColumn;
    
    /**
     * 自定义SQL条件
     */
    private String customSql;
    
    /**
     * 状态（0：禁用 1：启用）
     */
    private Integer status;
    
    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 删除标志（0：未删除 1：已删除）
     */
    @TableLogic
    private Integer deleted;
}
