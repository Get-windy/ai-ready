package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 角色继承关系实体
 * 支持角色之间的继承关系
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("sys_role_inheritance")
public class RoleInheritance {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 父角色ID
     */
    private Long parentRoleId;

    /**
     * 子角色ID
     */
    private Long childRoleId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 继承类型（1-完全继承 2-部分继承）
     */
    private Integer inheritanceType;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
}