package cn.aiedge.common.dto.role;

import lombok.Data;

/**
 * 权限简要信息VO
 */
@Data
public class PermissionVO {

    private Long id;

    private String permissionCode;

    private String permissionName;

    private String permissionType;
}
