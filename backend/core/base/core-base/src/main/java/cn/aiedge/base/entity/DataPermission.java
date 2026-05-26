package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 数据权限实体
 * 用于细粒度的数据访问控制
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("sys_data_permission")
public class DataPermission {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限编码
     */
    private String permissionCode;

    /**
     * 数据权限类型（0-全部 1-本部门 2-本部门及以下 3-仅本人 4-自定义规则）
     */
    private Integer dataScope;

    /**
     * 数据权限规则（JSON格式存储具体规则）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object permissionRule;

    /**
     * 关联的角色ID
     */
    private Long roleId;

    /**
     * 关联的用户ID
     */
    private Long userId;

    /**
     * 作用域类型（1-角色级 2-用户级 3-全局）
     */
    private Integer scopeType;

    /**
     * 状态（0-启用 1-禁用）
     */
    private Integer status;

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