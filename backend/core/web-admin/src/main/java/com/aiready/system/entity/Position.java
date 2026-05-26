package com.aiready.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 岗位实体类
 * 用于管理系统中的岗位信息
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_position")
public class Position {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 岗位名称
     */
    private String positionName;

    /**
     * 岗位编码
     */
    private String positionCode;

    /**
     * 所属部门ID
     */
    private Long deptId;

    /**
     * 所属部门名称（冗余字段）
     */
    @TableField(exist = false)
    private String deptName;

    /**
     * 岗位类型（1：管理岗 2：技术岗 3：销售岗 4：运营岗 5：其他）
     */
    private Integer positionType;

    /**
     * 岗位级别（1-5，数字越大级别越高）
     */
    private Integer positionLevel;

    /**
     * 岗位描述
     */
    private String description;

    /**
     * 工作职责
     */
    private String responsibilities;

    /**
     * 任职要求
     */
    private String requirements;

    /**
     * 编制人数
     */
    private Integer headcount;

    /**
     * 在职人数
     */
    private Integer currentCount;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态（0：停用 1：正常）
     */
    private Integer status;

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
