package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class Role extends BaseEntity {

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色类型（admin-管理员、user-普通用户等）
     */
    private String roleType;

    /**
     * 数据权限类型（1-全部 2-本部门 3-本部门及子部门 4-仅本人）
     */
    private Integer dataScope;

    /**
     * 父角色ID
     */
    private Long parentId;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 状态 0-禁用 1-启用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
