package com.aiready.permission.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 组织机构实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_organization")
public class Organization {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 父组织ID
     */
    private Long parentId;
    
    /**
     * 组织编码
     */
    private String orgCode;
    
    /**
     * 组织名称
     */
    private String orgName;
    
    /**
     * 组织类型（1：公司 2：部门 3：小组）
     */
    private Integer orgType;
    
    /**
     * 组织层级
     */
    private Integer level;
    
    /**
     * 排序
     */
    private Integer sortOrder;
    
    /**
     * 负责人ID
     */
    private Long leaderId;
    
    /**
     * 联系电话
     */
    private String phone;
    
    /**
     * 邮箱
     */
    private String email;
    
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
    
    /**
     * 子组织列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<Organization> children;
    
    /**
     * 父组织名称（非数据库字段）
     */
    @TableField(exist = false)
    private String parentName;
}
