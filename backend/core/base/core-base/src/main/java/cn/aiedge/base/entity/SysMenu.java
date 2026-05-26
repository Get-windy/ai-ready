package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 菜单实体
 * 树形结构菜单定义
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("sys_menu")
public class SysMenu {

    /**
     * 菜单ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID（多租户支持）
     */
    private Long tenantId;

    /**
     * 父级菜单ID（0为顶级菜单）
     */
    private Long parentId;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 菜单编码（唯一）
     */
    private String menuCode;

    /**
     * 菜单类型（0-目录 1-菜单 2-按钮）
     */
    private Integer menuType;

    /**
     * 路由路径
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 路由名称
     */
    private String routeName;

    /**
     * 重定向路径
     */
    private String redirect;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 是否外链（0-否 1-是）
     */
    private Integer isExternal;

    /**
     * 是否缓存（0-否 1-是）
     */
    private Integer isCache;

    /**
     * 是否可见（0-隐藏 1-显示）
     */
    private Integer visible;

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
}