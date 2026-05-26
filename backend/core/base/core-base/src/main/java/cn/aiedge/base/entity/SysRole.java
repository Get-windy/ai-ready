package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 角色实体
 * RBAC角色定义
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("sys_role")
public class SysRole {

    /**
     * 角色ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID（多租户支持）
     */
    private Long tenantId;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色编码（唯一）
     */
    private String roleCode;

    /**
     * 角色类型（0-系统角色 1-自定义角色）
     */
    private Integer roleType;

    /**
     * 数据权限类型（0-全部 1-本部门 2-本部门及以下 3-仅本人 4-自定义）
     */
    private Integer dataScope;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 状态（0-正常 1-禁用）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否删除（0-未删除 1-已删除）
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}