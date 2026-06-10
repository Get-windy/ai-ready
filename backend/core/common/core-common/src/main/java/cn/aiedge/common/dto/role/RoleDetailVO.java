package cn.aiedge.common.dto.role;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色响应VO
 */
@Data
public class RoleDetailVO {

    private Long id;

    private String roleCode;

    private String roleName;

    private String roleType;

    /**
     * 角色作用域：PLATFORM-平台级 TENANT-租户级
     */
    private String scope;

    private Integer dataScope;

    private Long parentId;

    private String parentName;

    private Integer sort;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String remark;

    /**
     * 角色权限列表
     */
    private List<PermissionVO> permissions;

    /**
     * 权限ID列表
     */
    private List<Long> permissionIds;
}
