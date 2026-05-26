package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 权限实体
 * 菜单/API权限定义
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("sys_permission")
public class SysPermission {

    /**
     * 权限ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID（多租户支持）
     */
    private Long tenantId;

    /**
     * 父级权限ID
     */
    private Long parentId;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限编码（唯一）
     */
    private String permissionCode;

    /**
     * 权限类型（1-菜单 2-按钮 3-API）
     */
    private Integer permissionType;

    /**
     * 菜单路径（类型为菜单时）
     */
    private String path;

    /**
     * 菜单组件（类型为菜单时）
     */
    private String component;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * API路径（类型为API时）
     */
    private String apiPath;

    /**
     * 请求方法（类型为API时）
     */
    private String method;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 是否可见（0-隐藏 1-显示）
     */
    private Integer visible;

    /**
     * 状态（0-正常 1-禁用）
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
}