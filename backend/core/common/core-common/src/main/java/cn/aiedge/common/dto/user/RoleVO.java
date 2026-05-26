package cn.aiedge.common.dto.user;

import lombok.Data;

/**
 * 角色简要信息VO
 */
@Data
public class RoleVO {

    private Long id;

    private String roleCode;

    private String roleName;
}
