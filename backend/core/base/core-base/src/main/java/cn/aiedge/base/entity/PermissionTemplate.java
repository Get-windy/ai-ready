package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 权限模板实体
 * 用于快速分配预设权限组合
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("sys_permission_template")
public class PermissionTemplate {

    /**
     * 模板ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID（多租户支持）
     */
    private Long tenantId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板编码（唯一）
     */
    private String templateCode;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 权限配置（JSON格式存储权限ID列表）
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object permissionConfig;

    /**
     * 模板类型（1-系统默认 2-自定义）
     */
    private Integer templateType;

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