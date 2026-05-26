package com.aiready.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 部门实体类
 * 用于管理系统组织架构中的部门信息
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_department")
public class Department {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 部门编码
     */
    private String deptCode;

    /**
     * 父部门ID
     */
    private Long parentId;

    /**
     * 父部门名称（冗余字段）
     */
    @TableField(exist = false)
    private String parentName;

    /**
     * 层级路径（如：0,1,2,）
     */
    private String ancestors;

    /**
     * 部门负责人ID
     */
    private Long leaderId;

    /**
     * 部门负责人名称
     */
    private String leaderName;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 部门排序
     */
    private Integer sortOrder;

    /**
     * 部门状态（0：停用 1：正常）
     */
    private Integer status;

    /**
     * 部门类型（1：公司 2：部门 3：小组）
     */
    private Integer deptType;

    /**
     * 部门地址
     */
    private String address;

    /**
     * 备注
     */
    private String remark;

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
