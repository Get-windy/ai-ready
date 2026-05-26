package com.aiready.permission.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_role")
public class Role {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 角色编码
     */
    private String roleCode;
    
    /**
     * 角色名称
     */
    private String roleName;
    
    /**
     * 角色描述
     */
    private String description;
    
    /**
     * 数据权限范围（1：全部数据 2：本部门数据 3：本部门及子部门数据 4：仅本人数据）
     */
    private Integer dataScope;
    
    /**
     * 状态（0：禁用 1：启用）
     */
    private Integer status;
    
    /**
     * 排序
     */
    private Integer sortOrder;
    
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
    
    /**
     * 权限列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<Permission> permissions;
}
