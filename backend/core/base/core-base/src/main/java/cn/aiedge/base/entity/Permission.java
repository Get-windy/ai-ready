package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限/菜单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class Permission extends BaseEntity {

    /**
     * 权限编码（如：user:create、user:update）
     */
    private String permissionCode;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限类型（menu-菜单、button-按钮、api-API接口）
     */
    private String permissionType;

    /**
     * 父权限ID
     */
    private Long parentId;

    /**
     * 菜单路径
     */
    private String path;

    /**
     * 菜单组件
     */
    private String component;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 是否可见
     */
    private Boolean visible;

    /**
     * 状态 0-禁用 1-启用
     */
    private Integer status;

    /**
     * API路径（用于API权限）
     */
    private String apiPath;

    /**
     * 请求方法（GET、POST、PUT、DELETE等）
     */
    private String method;

    /**
     * 备注
     */
    private String remark;
}
